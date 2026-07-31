import time

import pytest

from app.cache.public import PublicResponseCache
from app.conversation import ConversationTurn, InMemoryConversationStore
from app.core.config import Settings
from app.core.errors import AgentError
from app.middleware.pipeline import AGENT_PIPELINE_ORDER, HTTP_MIDDLEWARE_ORDER, PROVIDER_MIDDLEWARE_ORDER
from app.model_routing.health import CircuitState
from app.model_routing.registry import ModelRegistry
from app.model_routing.router import ModelRouter
from app.observability.tracing import SafeTracer
from app.providers.middleware import ProviderCircuitBreakerMiddleware
from app.token_budget.manager import TokenBudgetManager
from app.token_budget.models import ContextLayers, TokenBudgetExceeded
from app.token_budget.tool_schema_selector import ToolSchemaSelector
from app.tools.mock.orders import create_mock_registry


def test_token_budget_normal_and_hard_limit() -> None:
    manager = TokenBudgetManager(Settings(agent_max_input_tokens=256, agent_default_context_limit=1024))
    layers = ContextLayers("system", "safety", "", (), "你好", (), ())
    assert manager.plan(layers, context_limit=1024, max_output_tokens=64).input_tokens > 0
    with pytest.raises(TokenBudgetExceeded):
        manager.plan(ContextLayers("x" * 10000, "", "", (), "x", (), ()), context_limit=1024, max_output_tokens=64)


def test_dynamic_tool_schema_selection_excludes_irrelevant_and_caps() -> None:
    tools = create_mock_registry().metadata()
    selector = ToolSchemaSelector(4)
    dorm = selector.select(intent="dorm_query", tools=tools, authenticated=True)
    assert [item.name for item in dorm.tools] == ["get_my_dorm_info"]
    assert selector.select(intent="greeting", tools=tools, authenticated=False).tools == ()


def test_context_uses_sliding_window_and_safe_summary_clear() -> None:
    store = InMemoryConversationStore(max_turns=2)
    for index in range(3):
        store.append("c", ConversationTurn(f"Bearer secret-{index}", "宿舍完整地址", "dorm_query", ("get_my_dorm_info",)))
    context = store.context("c", 1)
    assert len(context.recent_turns) == 1 and "secret" not in context.recent_turns[0].message
    assert context.summary and store.clear("c")
    assert store.context("c", 1).summary == ""


def test_public_cache_never_keys_or_caches_private_tools() -> None:
    cache = PublicResponseCache(enabled=True, ttl_seconds=30, max_entries=2)
    public = cache.key_for(tool_name="search_shops", arguments={"keyword": "面馆"}, requires_auth=False)
    private = cache.key_for(tool_name="get_my_recent_orders", arguments={"token": "no"}, requires_auth=True)
    cache.put(public, {"records": [1]})
    assert cache.get(public) == {"records": [1]} and private is None


def test_router_profiles_and_fixed_middleware_order() -> None:
    router = ModelRouter(ModelRegistry(Settings()))
    assert router.decide(intent="greeting", expected_tool_count=0, estimated_input_tokens=1).selected_profile == "deterministic"
    assert router.decide(intent="promotion_query", expected_tool_count=1, estimated_input_tokens=1).selected_profile == "fast"
    assert router.decide(intent="shop_recommendation", expected_tool_count=2, estimated_input_tokens=1).selected_profile == "standard"
    assert HTTP_MIDDLEWARE_ORDER[0] == "CorsMiddleware" and AGENT_PIPELINE_ORDER[0] == "SafetyGuardMiddleware" and PROVIDER_MIDDLEWARE_ORDER[-1] == "ProviderFallbackMiddleware"


def test_circuit_breaker_closed_open_half_open() -> None:
    breaker = ProviderCircuitBreakerMiddleware(Settings(agent_circuit_breaker_failure_threshold=1, agent_circuit_breaker_recovery_seconds=1))
    breaker.failure(AgentError("x", "x", True, 503))
    assert breaker.health.state is CircuitState.OPEN
    breaker.health.opened_at = time.monotonic() - 2
    breaker.before_call()
    assert breaker.health.state is CircuitState.HALF_OPEN
    breaker.success()
    assert breaker.health.state is CircuitState.CLOSED


def test_safe_tracer_drops_sensitive_attributes() -> None:
    tracer = SafeTracer()
    with tracer.span("llm.call", request_id="r", token="secret", authorization="Bearer secret", input_tokens=1):
        pass
    assert tracer.spans[0].attributes == {"request_id": "r", "input_tokens": 1}
