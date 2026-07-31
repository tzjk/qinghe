"""Opt-in real Spring Boot checks. Never run without both explicit guards."""

import os

import httpx
import pytest
from httpx import ASGITransport, AsyncClient

from app.clients.endpoint_registry import EndpointName
from app.clients.qinghe_client import QingheClient
from app.core.config import Settings
from app.main import create_app


pytestmark = pytest.mark.real_backend


def _real_enabled() -> bool:
    return os.getenv("QINGHE_REAL_BACKEND_TESTS", "").strip().lower() == "true"


@pytest.fixture(autouse=True)
def guard_real_backend() -> None:
    if not _real_enabled():
        pytest.skip("set QINGHE_REAL_BACKEND_TESTS=true to allow real backend access")


@pytest.fixture
def settings() -> Settings:
    return Settings(
        agent_env="test",
        agent_log_level="WARNING",
        agent_mock_mode=False,
        agent_tool_mode="spring",
        llm_provider="deterministic",
        qinghe_backend_enabled=True,
        qinghe_backend_base_url="http://127.0.0.1:8090",
        qinghe_max_retries=0,
    )


async def test_public_read_only_contracts(settings: Settings) -> None:
    client = QingheClient(settings)
    try:
        coupons = await client.get(EndpointName.COUPONS)
        shops = await client.get(EndpointName.SHOPS, params={"page": 1, "size": 10})
        posts = await client.get(EndpointName.EXPLORE_POSTS, params={"page": 1, "size": 10, "sort": "hot"})
        nearby = await client.get(
            EndpointName.NEARBY_SHOPS,
            params={"longitude": 120.0, "latitude": 30.0, "radius": 5, "page": 1, "size": 10},
        )
        assert isinstance(coupons.data, dict)
        assert isinstance(shops.data, dict)
        assert isinstance(posts.data, dict)
        assert isinstance(nearby.data, dict)
        records = shops.data.get("records", [])
        if records and isinstance(records[0], dict) and isinstance(records[0].get("id"), int):
            shop_id = records[0]["id"]
            assert (await client.get(EndpointName.SHOP_DETAIL, path_values={"id": shop_id})).backend_code == 200
            assert (await client.get(EndpointName.SHOP_GOODS, path_values={"id": shop_id}, params={"page": 1, "size": 10})).backend_code == 200
    finally:
        await client.close()


async def test_fastapi_public_chat_and_missing_or_invalid_token(settings: Settings) -> None:
    app = create_app(settings)
    transport = ASGITransport(app=app, raise_app_exceptions=False)
    try:
        async with AsyncClient(transport=transport, base_url="http://testserver") as client:
            promotion = await client.post("/api/v1/chat", json={"message": "今天有什么优惠？"})
            assert promotion.status_code == 200
            assert promotion.json()["tool_calls"][0]["tool_name"] == "get_today_promotions"
            missing = await client.post("/api/v1/chat", json={"message": "我的资料"})
            assert missing.status_code == 200
            assert missing.json()["tool_calls"][0]["status"] == "failed"
            invalid = await client.post(
                "/api/v1/chat", json={"message": "我的资料"}, headers={"Authorization": "Bearer invalid-token"}
            )
            assert invalid.status_code == 200
            assert invalid.json()["tool_calls"][0]["status"] == "failed"
            assert "invalid-token" not in invalid.text
    finally:
        await app.state.services.backend_client.close()


async def test_personal_tools_with_user_supplied_token_only(settings: Settings) -> None:
    token = os.getenv("QINGHE_TEST_USER_TOKEN", "")
    if not token:
        pytest.skip("QINGHE_TEST_USER_TOKEN was not supplied")
    app = create_app(settings)
    transport = ASGITransport(app=app, raise_app_exceptions=False)
    headers = {"Authorization": f"Bearer {token}"}
    try:
        async with AsyncClient(transport=transport, base_url="http://testserver") as client:
            for message, tool_name in (
                ("我的资料", "get_my_profile"),
                ("我的学生档案", "get_my_student_profile"),
                ("我的宿舍", "get_my_dorm_info"),
                ("我的优惠券", "get_my_coupon_wallet"),
                ("我的订单", "get_my_recent_orders"),
                ("我的地址", "get_my_addresses"),
            ):
                response = await client.post("/api/v1/chat", json={"message": message}, headers=headers)
                assert response.status_code == 200
                assert response.json()["tool_calls"][0]["tool_name"] == tool_name
                assert token not in response.text
    finally:
        await app.state.services.backend_client.close()
