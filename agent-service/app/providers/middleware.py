"""Provider-layer decorators; none depends on FastAPI routes."""
import asyncio
import time
from typing import Any

from app.agent.provider_types import ProviderToolCall
from app.core.config import Settings
from app.core.errors import AgentError
from app.model_routing.health import CircuitState, ProviderHealth
from app.providers.base import BaseLLMProvider
from app.schemas.tool import ToolMetadata


class ProviderCircuitBreakerMiddleware:
    def __init__(self, settings: Settings) -> None:
        self._threshold = settings.agent_circuit_breaker_failure_threshold
        self._recovery = settings.agent_circuit_breaker_recovery_seconds
        self._half_max = settings.agent_circuit_breaker_half_open_max_calls
        self.health = ProviderHealth()

    def before_call(self) -> None:
        now = time.monotonic()
        if self.health.state is CircuitState.OPEN:
            if self.health.opened_at is None or now - self.health.opened_at < self._recovery:
                raise AgentError("AGENT_PROVIDER_CIRCUIT_OPEN", "模型服务暂不可用，请稍后重试。", True, 503)
            self.health.state, self.health.half_open_calls = CircuitState.HALF_OPEN, 0
        if self.health.state is CircuitState.HALF_OPEN:
            if self.health.half_open_calls >= self._half_max:
                raise AgentError("AGENT_PROVIDER_CIRCUIT_OPEN", "模型服务正在恢复，请稍后重试。", True, 503)
            self.health.half_open_calls += 1

    def success(self) -> None:
        self.health = ProviderHealth()

    def failure(self, error: AgentError) -> None:
        if not error.retryable:
            return
        self.health.failures += 1
        if self.health.state is CircuitState.HALF_OPEN or self.health.failures >= self._threshold:
            self.health.state, self.health.opened_at = CircuitState.OPEN, time.monotonic()


class ProviderRetryMiddleware:
    """Reusable bounded retry policy for providers without a built-in protocol retry."""
    def __init__(self, attempts: int) -> None:
        self._attempts = attempts

    async def run(self, call):
        last: AgentError | None = None
        for index in range(self._attempts + 1):
            try:
                return await call()
            except AgentError as error:
                last = error
                if not error.retryable or index >= self._attempts:
                    raise
                await asyncio.sleep(0)
        raise last or AgentError("AGENT_PROVIDER_ERROR", "模型服务暂不可用，请稍后重试。", True, 503)


class ProviderUsageCaptureMiddleware(BaseLLMProvider):
    def __init__(self, primary: BaseLLMProvider, settings: Settings) -> None:
        self._primary, self.name = primary, primary.name
        self._breaker = ProviderCircuitBreakerMiddleware(settings)
        self._retry = ProviderRetryMiddleware(0)  # OpenAI adapter already owns its protocol retry.
        self.last_usage: dict[str, int] | None = None

    async def _run(self, call):
        self._breaker.before_call()
        try:
            result = await self._retry.run(call)
            self.last_usage = getattr(self._primary, "last_usage", None)
            self._breaker.success()
            return result
        except AgentError as error:
            self._breaker.failure(error)
            raise

    async def select_tools(self, *, intent: str, message: str, max_tools: int, tools: list[ToolMetadata]) -> list[ProviderToolCall]:
        return await self._run(lambda: self._primary.select_tools(intent=intent, message=message, max_tools=max_tools, tools=tools))

    async def generate_response(self, *, intent: str, message: str, tool_results: list[dict[str, Any]]) -> str:
        return await self._run(lambda: self._primary.generate_response(intent=intent, message=message, tool_results=tool_results))

    async def health_check(self) -> bool:
        return await self._primary.health_check()
