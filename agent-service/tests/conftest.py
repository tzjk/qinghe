import pytest
from fastapi import FastAPI
from httpx import ASGITransport, AsyncClient

from app.core.config import Settings
from app.main import create_app
from app.providers.deterministic_tool_calling import DeterministicToolCallingProvider
from tests.fakes.fake_qinghe_backend import FakeQingheBackend


@pytest.fixture
def fake_backend() -> FakeQingheBackend:
    return FakeQingheBackend()


@pytest.fixture
def provider() -> DeterministicToolCallingProvider:
    return DeterministicToolCallingProvider()


@pytest.fixture
def app(fake_backend: FakeQingheBackend, provider: DeterministicToolCallingProvider) -> FastAPI:
    settings = Settings(
        agent_env="test",
        agent_log_level="WARNING",
        agent_mock_mode=False,
        agent_tool_mode="spring",
        qinghe_backend_enabled=True,
        qinghe_backend_base_url="http://offline.backend",
        qinghe_max_retries=0,
    )
    return create_app(
        settings,
        provider=provider,
        backend_transport=fake_backend.transport,
        backend_data_source="spring_simulated",
    )


@pytest.fixture
async def client(app: FastAPI):
    transport = ASGITransport(app=app, raise_app_exceptions=False)
    async with AsyncClient(transport=transport, base_url="http://testserver") as test_client:
        yield test_client
