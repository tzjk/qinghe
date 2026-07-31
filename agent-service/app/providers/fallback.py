from typing import Any

from app.agent.provider_types import ProviderToolCall
from app.core.errors import AgentError
from app.providers.base import BaseLLMProvider
from app.schemas.tool import ToolMetadata


class FallbackProvider(BaseLLMProvider):
    """Development-only, explicit fallback. The caller can surface the warning."""

    def __init__(self, primary: BaseLLMProvider, fallback: BaseLLMProvider) -> None:
        self._primary = primary
        self._fallback = fallback
        self.name = primary.name
        self.used_fallback = False

    async def select_tools(self, *, intent: str, message: str, max_tools: int, tools: list[ToolMetadata]) -> list[ProviderToolCall]:
        try:
            return await self._primary.select_tools(intent=intent, message=message, max_tools=max_tools, tools=tools)
        except AgentError as error:
            if not error.retryable:
                raise
            self.used_fallback = True
            return await self._fallback.select_tools(intent=intent, message=message, max_tools=max_tools, tools=tools)

    async def generate_response(self, *, intent: str, message: str, tool_results: list[dict[str, Any]]) -> str:
        try:
            return await self._primary.generate_response(intent=intent, message=message, tool_results=tool_results)
        except AgentError as error:
            if not error.retryable:
                raise
            self.used_fallback = True
            return await self._fallback.generate_response(intent=intent, message=message, tool_results=tool_results)

    async def health_check(self) -> bool:
        return await self._primary.health_check() or await self._fallback.health_check()
