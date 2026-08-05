import asyncio
from datetime import datetime, timedelta, timezone

import pytest

from app.conversation import ConversationTurn, InMemoryConversationStore
from app.core.config import Settings
from app.core.errors import AgentError
from app.core.limits import InMemoryLimiter
from app.schemas.tool import ToolResult
from app.tools.result_minimizer import minimize_for_provider


def turn() -> ConversationTurn:
    return ConversationTurn(message="我的订单", answer_summary="仅摘要", intent="order_query", tool_names=("get_my_recent_orders",))


def test_conversation_ttl_capacity_and_sensitive_exclusion() -> None:
    store = InMemoryConversationStore(max_turns=2, ttl_minutes=1, max_count=2)
    store.append("a", turn())
    store.append("a", turn())
    store.append("a", turn())
    assert len(store.get("a")) == 2
    store.append("b", turn())
    store.append("c", turn())
    assert store.count == 2 and not store.exists("a")
    store._items["b"].last_active = datetime.now(timezone.utc) - timedelta(minutes=2)  # controlled local clock state
    assert store.cleanup() == 1


def test_minimal_provider_result_removes_credentials_internal_and_address_detail() -> None:
    minimized = minimize_for_provider(ToolResult(success=True, tool_name="get_my_recent_orders", data_source="spring", backend_code=200, backend_request_id="internal", data={"orderNo": "QH1", "token": "secret", "phone": "123", "detailAddress": "room", "class": "Java"}))
    assert minimized == {"success": True, "tool_name": "get_my_recent_orders", "data_source": "spring", "data": {"orderNo": "QH1"}}


async def test_limiter_rate_concurrency_and_sse_release() -> None:
    limiter = InMemoryLimiter(per_minute=1, max_requests=1, max_model_calls=1, max_tool_calls=1, max_sse_connections=1)
    await limiter.admit_ip("127.0.0.1")
    with pytest.raises(AgentError) as rate:
        await limiter.admit_ip("127.0.0.1")
    assert rate.value.status_code == 429
    async with limiter.request_slot():
        with pytest.raises(AgentError):
            async with limiter.request_slot():
                pass
    await limiter.acquire_sse()
    with pytest.raises(AgentError):
        await limiter.acquire_sse()
    await limiter.release_sse()
    assert limiter.sse_count == 0


def test_production_rejects_silent_provider_fallback() -> None:
    with pytest.raises(ValueError, match="production"):
        Settings(agent_env="production", agent_mock_mode=False, llm_provider="openai_compatible", llm_api_key="x", llm_base_url="http://fake", llm_model="fake", agent_provider_fallback_enabled=True)
