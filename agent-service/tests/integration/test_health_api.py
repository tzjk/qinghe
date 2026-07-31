from httpx import ASGITransport, AsyncClient

from app.core.config import Settings
from app.main import create_app


async def test_health_returns_independent_mock_status(client: AsyncClient) -> None:
    response = await client.get("/health")

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "ok"
    assert body["mock_mode"] is False
    assert body["environment"] == "test"
    assert response.headers["x-request-id"]


async def test_ready_checks_only_local_components(client: AsyncClient) -> None:
    response = await client.get("/ready")

    assert response.status_code == 200
    assert response.json() == {
        "service": "Qinghe Campus Assistant",
        "status": "ready",
        "provider": "deterministic",
        "tool_count": 15,
        "mock_mode": False,
    }


async def test_tools_is_hidden_outside_development(client: AsyncClient) -> None:
    response = await client.get("/api/v1/tools")

    assert response.status_code == 404
    assert response.json()["error"]["code"] == "AGENT_TOOL_NOT_FOUND"


async def test_tools_returns_safe_metadata_in_development() -> None:
    app = create_app(Settings(agent_env="development", agent_log_level="WARNING"))
    async with AsyncClient(
        transport=ASGITransport(app=app), base_url="http://testserver"
    ) as client:
        response = await client.get("/api/v1/tools")

    assert response.status_code == 200
    tools = response.json()["tools"]
    assert {item["name"] for item in tools} == {
        "get_today_promotions",
        "search_shops",
        "get_my_dorm_info",
        "get_my_recent_orders",
    }
    assert "LLM_API_KEY" not in response.text


async def test_usage_summary_is_development_only_and_aggregate(client) -> None:
    await client.post("/api/v1/chat", json={"message": "今天有什么优惠？"})
    response = await client.get("/api/v1/metrics/usage-summary")
    assert response.status_code == 404

    app = create_app(Settings(agent_env="development", agent_log_level="WARNING"))
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://testserver") as development_client:
        summary = await development_client.get("/api/v1/metrics/usage-summary")
    assert summary.status_code == 200
    assert set(summary.json()) == {"request_count", "total_tokens", "average_tokens", "model_profiles", "fallback_count", "cache_hits", "tool_calls", "estimated_cost"}
