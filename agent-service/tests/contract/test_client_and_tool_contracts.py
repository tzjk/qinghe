import httpx
import pytest

from app.clients.endpoint_registry import EndpointName
from app.clients.qinghe_client import QingheClient
from app.core.config import Settings
from app.core.errors import AgentError
from app.tools.contracts import assert_compatible_contracts
from app.tools.mock.orders import create_mock_registry
from app.tools.spring import create_spring_registry
from tests.fakes.fake_qinghe_backend import FakeQingheBackend, result


def spring_settings() -> Settings:
    return Settings(agent_mock_mode=False, agent_tool_mode="spring", qinghe_backend_enabled=True, qinghe_backend_base_url="http://offline.backend", qinghe_max_retries=0)


async def test_qinghe_client_success_and_data_null_are_parsed() -> None:
    fake = FakeQingheBackend()
    client = QingheClient(spring_settings(), transport=fake.transport)
    try:
        parsed = await client.get(EndpointName.COUPONS)
        assert parsed.backend_code == 200
        assert parsed.data["records"]
        assert parsed.java_http_duration_ms >= 0
    finally:
        await client.close()


async def test_data_null_is_valid_result_envelope() -> None:
    fake = FakeQingheBackend(responses={"/api/coupons": httpx.Response(200, json=result(200, None))})
    client = QingheClient(spring_settings(), transport=fake.transport)
    try:
        assert (await client.get(EndpointName.COUPONS)).data is None
    finally:
        await client.close()


@pytest.mark.parametrize(
    ("status_code", "code", "expected"),
    [(200, 401, "AGENT_AUTH_EXPIRED"), (200, 403, "AGENT_FORBIDDEN"), (200, 404, "AGENT_NOT_FOUND"), (401, 200, "AGENT_AUTH_EXPIRED"), (403, 200, "AGENT_FORBIDDEN"), (404, 200, "AGENT_NOT_FOUND"), (429, 200, "AGENT_BACKEND_RATE_LIMITED"), (500, 200, "AGENT_BACKEND_UNAVAILABLE")],
)
async def test_qinghe_client_maps_business_and_http_errors(status_code, code, expected) -> None:
    fake = FakeQingheBackend(responses={"/api/coupons": httpx.Response(status_code, json=result(code, None))})
    client = QingheClient(spring_settings(), transport=fake.transport)
    try:
        with pytest.raises(AgentError) as captured:
            await client.get(EndpointName.COUPONS)
        assert captured.value.code == expected
    finally:
        await client.close()


async def test_non_json_and_missing_data_are_safe_errors() -> None:
    for response in (httpx.Response(200, text="not json"), httpx.Response(200, json={"code": 200, "message": "ok"})):
        fake = FakeQingheBackend(responses={"/api/coupons": response})
        client = QingheClient(spring_settings(), transport=fake.transport)
        try:
            with pytest.raises(AgentError) as captured:
                await client.get(EndpointName.COUPONS)
            assert captured.value.code == "AGENT_BACKEND_RESPONSE_INVALID"
        finally:
            await client.close()


def test_mock_and_spring_contracts_match() -> None:
    fake = FakeQingheBackend()
    mock_registry = create_mock_registry()
    spring_registry, _ = create_spring_registry(spring_settings(), transport=fake.transport, data_source="spring_simulated")
    assert_compatible_contracts(mock_registry, spring_registry, ("get_today_promotions", "search_shops", "get_my_dorm_info", "get_my_recent_orders"))


@pytest.mark.parametrize(("error", "expected"), [(httpx.ReadTimeout("timeout"), "AGENT_BACKEND_TIMEOUT"), (httpx.ConnectError("offline"), "AGENT_BACKEND_UNAVAILABLE")])
async def test_transport_failures_never_escape(error, expected) -> None:
    async def failing_handler(_request):
        raise error

    client = QingheClient(spring_settings(), transport=httpx.MockTransport(failing_handler))
    try:
        with pytest.raises(AgentError) as captured:
            await client.get(EndpointName.COUPONS)
        assert captured.value.code == expected
    finally:
        await client.close()
