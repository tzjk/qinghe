import asyncio
import time
from collections.abc import Awaitable, Callable
from typing import Any

from pydantic import ValidationError

from app.agent.intent import classify_intent
from app.agent.provider_types import ProviderToolCall
from app.agent.response_builder import AgentResult
from app.core.config import Settings
from app.core.errors import PROMPT_INJECTION, AUTH_REQUIRED, AgentError
from app.core.limits import InMemoryLimiter
from app.core.request_context import get_request_context
from app.cache.public import PublicResponseCache
from app.conversation import ConversationContext
from app.model_routing.registry import ModelRegistry
from app.model_routing.router import ModelRouter
from app.observability.metrics import InMemoryMetrics
from app.observability.tracing import SafeTracer
from app.observability.usage import ModelUsageRecord
from app.prompts.safety_prompt import SAFETY_PROMPT
from app.prompts.system_prompt import SYSTEM_PROMPT
from app.providers.base import BaseLLMProvider
from app.schemas.tool import ToolCallTrace, ToolResult
from app.tools.registry import ToolRegistry
from app.tools.result_minimizer import minimize_for_provider
from app.token_budget import TokenBudgetManager, ToolSchemaSelector
from app.token_budget.history_compactor import safe_text
from app.token_budget.models import ContextLayers, TokenBudgetExceeded
from app.token_budget.usage import provider_usage
from app.token_budget.cost import estimate_cost


class AgentOrchestrator:
    def __init__(
        self,
        *,
        settings: Settings,
        provider: BaseLLMProvider,
        registry: ToolRegistry,
        metrics: InMemoryMetrics,
        limits: InMemoryLimiter | None = None,
        token_budget: TokenBudgetManager | None = None,
        schema_selector: ToolSchemaSelector | None = None,
        model_router: ModelRouter | None = None,
        model_registry: ModelRegistry | None = None,
        public_cache: PublicResponseCache | None = None,
        tracer: SafeTracer | None = None,
    ) -> None:
        self._settings = settings
        self._provider = provider
        self._registry = registry
        self._metrics = metrics
        self._limits = limits
        self._token_budget = token_budget or TokenBudgetManager(settings)
        self._schema_selector = schema_selector or ToolSchemaSelector(settings.agent_tool_schema_max_count)
        self._model_registry = model_registry or ModelRegistry(settings)
        self._model_router = model_router or ModelRouter(self._model_registry)
        self._public_cache = public_cache or PublicResponseCache(enabled=False, ttl_seconds=1, max_entries=1)
        self._tracer = tracer or SafeTracer()

    async def handle(
        self,
        message: str,
        event_sink: Callable[[str, dict[str, object]], Awaitable[None]] | None = None,
        conversation_context: ConversationContext | None = None,
    ) -> AgentResult:
        with self._tracer.span("safety.check", request_id=(get_request_context().request_id if get_request_context() else "unknown")):
            intent = classify_intent(message)
        if not intent.safe:
            raise PROMPT_INJECTION
        context = get_request_context()
        selection = self._schema_selector.select(
            intent=intent.name,
            tools=self._registry.metadata(),
            authenticated=bool(context and context.authorization_token),
            recent_tool_names=tuple(name for turn in (conversation_context.recent_turns if conversation_context else ()) for name in turn.tool_names)[-2:],
        )
        layers = self._layers(message, conversation_context, selection.tools)
        try:
            budget = self._token_budget.plan(layers, context_limit=self._settings.agent_default_context_limit, max_output_tokens=self._settings.agent_max_output_tokens)
            if context and context.conversation_id:
                self._token_budget.ensure_conversation_available(context.conversation_id)
        except TokenBudgetExceeded as exc:
            raise AgentError("AGENT_TOKEN_BUDGET_EXCEEDED", str(exc), False, 413) from exc
        decision = self._model_router.decide(intent=intent.name, expected_tool_count=min(2, len(selection.tools)), estimated_input_tokens=budget.input_tokens)
        if event_sink:
            await event_sink("intent.detected", {"intent": intent.name})
        with self._tracer.span("model.route", intent=intent.name, model_profile=decision.selected_profile, input_tokens=budget.input_tokens):
            candidates = await self._provider.select_tools(
            intent=intent.name,
            message=message,
            max_tools=self._settings.agent_max_tool_calls,
            tools=list(selection.tools),
            )
        if not isinstance(candidates, list) or not all(
            isinstance(candidate, ProviderToolCall) for candidate in candidates
        ):
            raise AgentError(
                "AGENT_PROVIDER_RESPONSE_INVALID", "模型返回的工具格式无效。", False, 502
            )
        selected_calls = candidates[: self._settings.agent_max_tool_calls]
        tool_results: list[dict[str, Any]] = []
        traces: list[ToolCallTrace] = []

        cache_hit = False
        for candidate in selected_calls:
            started = time.perf_counter()
            tool = None
            try:
                tool = self._registry.get(candidate.name)
                cache_key = self._public_cache.key_for(tool_name=tool.name, arguments=candidate.arguments, requires_auth=tool.requires_auth)
                cached = self._public_cache.get(cache_key)
                if cached is not None:
                    data = ToolResult.model_validate(cached)
                    cache_hit = True
                    raise _CachedToolResult(data)
                if event_sink:
                    await event_sink(
                        "tool.started", {"tool_name": tool.name, "data_source": tool.data_source}
                    )
                if self._limits is None:
                    data = await asyncio.wait_for(self._registry.execute(candidate.name, candidate.arguments), timeout=tool.timeout_seconds)
                else:
                    async with self._limits.tool_slot():
                        data = await asyncio.wait_for(self._registry.execute(candidate.name, candidate.arguments), timeout=tool.timeout_seconds)
                self._public_cache.put(cache_key, data.model_dump() if data.success else None)
            except _CachedToolResult as cached:
                data = cached.result
            except TimeoutError as exc:
                data = self._failed_result(
                    candidate.name, tool, "AGENT_TOOL_TIMEOUT", "查询耗时过长，请稍后重试。", True
                )
            except ValidationError:
                data = self._failed_result(
                    candidate.name, tool, "AGENT_TOOL_VALIDATION_ERROR", "工具参数不符合要求。", False
                )
            except AgentError as error:
                data = self._failed_result(
                    candidate.name, tool, error.code, error.message, error.retryable
                )
            duration_ms = int((time.perf_counter() - started) * 1000)
            data.duration_ms = duration_ms
            tool_results.append(data.model_dump())
            traces.append(
                ToolCallTrace(
                    tool_name=candidate.name,
                    data_source=data.data_source,
                    duration_ms=duration_ms,
                    status="success" if data.success else "failed",
                )
            )
            if event_sink:
                await event_sink(
                    "tool.completed",
                    {
                        "tool_name": candidate.name,
                        "data_source": data.data_source,
                        "status": "success" if data.success else "failed",
                    },
                )

        try:
            provider_results = [minimize_for_provider(ToolResult.model_validate(value)) for value in tool_results]
            result_layers = self._layers(message, conversation_context, selection.tools, provider_results)
            result_budget = self._token_budget.plan(result_layers, context_limit=self._settings.agent_default_context_limit, max_output_tokens=self._settings.agent_max_output_tokens)
            if self._limits is None:
                answer = await self._provider.generate_response(intent=intent.name, message=message, tool_results=provider_results)
            else:
                async with self._limits.model_slot():
                    answer = await self._provider.generate_response(intent=intent.name, message=message, tool_results=provider_results)
        except TokenBudgetExceeded as exc:
            raise AgentError("AGENT_TOKEN_BUDGET_EXCEEDED", str(exc), False, 413) from exc
        except AgentError:
            raise
        except Exception as exc:
            raise AgentError(
                code="AGENT_PROVIDER_ERROR",
                message="模型响应暂不可用，请稍后重试。",
                retryable=True,
                status_code=503,
            ) from exc

        usage = provider_usage(getattr(self._provider, "last_usage", None), input_tokens=result_budget.input_tokens, output_tokens=max(1, len(answer) // 3))
        if context and context.conversation_id:
            self._token_budget.record(context.conversation_id, usage)
        profile = self._model_registry.get(decision.selected_profile)
        fallback_used = bool(getattr(self._provider, "used_fallback", False)) or decision.selected_profile == "fallback"
        self._metrics.record(intent=intent.name, tool_count=len(traces))
        cost = estimate_cost(usage, input_cost_per_million=profile.input_cost_per_million, output_cost_per_million=profile.output_cost_per_million, cached_input_cost_per_million=profile.cached_input_cost_per_million)
        record = ModelUsageRecord.now(request_id=context.request_id if context else "unknown", conversation_id=context.conversation_id if context and context.conversation_id else "unknown", provider=self._provider.name, model_profile=decision.selected_profile, model=profile.model, input_tokens=usage.input_tokens, output_tokens=usage.output_tokens, cached_tokens=usage.cached_tokens, total_tokens=usage.total_tokens, estimated_cost=cost, estimated=usage.estimated, tool_count=len(traces), fallback_used=fallback_used, duration_ms=0)
        self._metrics.record_usage(profile=record.model_profile, usage=usage, cost=record.estimated_cost, fallback_used=record.fallback_used, cache_hit=cache_hit)
        return AgentResult(
            answer=answer,
            intent=intent.name,
            tool_calls=traces,
            mock=self._settings.agent_mock_mode,
            warnings=([
                "开发环境已从模型 Provider 降级为确定性回答。"
            ] if fallback_used else []) + [
                item.user_message or "工具调用未完成。"
                for item in (ToolResult.model_validate(value) for value in tool_results)
                if not item.success
            ],
        )

    @staticmethod
    def _layers(message: str, context: ConversationContext | None, tools: tuple[object, ...], results: list[dict[str, Any]] | None = None) -> ContextLayers:
        turns = () if context is None else tuple({"user": safe_text(turn.message), "assistant": safe_text(turn.answer_summary)} for turn in context.recent_turns)
        definitions = tuple({"name": getattr(tool, "name"), "description": getattr(tool, "description"), "parameters": getattr(tool, "input_schema")} for tool in tools)
        return ContextLayers(SYSTEM_PROMPT, SAFETY_PROMPT, safe_text(context.summary) if context else "", turns, safe_text(message, 1000), definitions, tuple(results or ()))

    @staticmethod
    def _failed_result(
        tool_name: str,
        tool: object | None,
        error_code: str,
        message: str,
        retryable: bool,
    ) -> ToolResult:
        return ToolResult(
            success=False,
            tool_name=tool_name,
            data_source=getattr(tool, "data_source", "not_available"),
            error_code=error_code,
            user_message=message,
            retryable=retryable,
        )


class _CachedToolResult(Exception):
    def __init__(self, result: ToolResult) -> None:
        self.result = result
