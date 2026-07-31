from fastapi import APIRouter, Request

from app import __version__
from app.core.config import Settings
from app.core.errors import AgentError
from app.schemas.chat import HealthResponse, ReadyResponse
from app.schemas.common import utc_now_iso

router = APIRouter(tags=["health"])


@router.get("/health", response_model=HealthResponse)
async def health(request: Request) -> HealthResponse:
    settings: Settings = request.app.state.settings
    return HealthResponse(
        service=settings.agent_app_name,
        status="ok",
        version=__version__,
        environment=settings.agent_env,
        mock_mode=settings.agent_mock_mode,
        timestamp=utc_now_iso(),
    )


@router.get("/ready", response_model=ReadyResponse)
async def ready(request: Request) -> ReadyResponse:
    services = request.app.state.services
    if not await services.provider.health_check():
        raise AgentError("AGENT_PROVIDER_ERROR", "Mock Provider 暂不可用。", True, 503)
    return ReadyResponse(
        service=request.app.state.settings.agent_app_name,
        status="ready",
        provider=services.provider.name,
        tool_count=services.registry.count,
        mock_mode=request.app.state.settings.agent_mock_mode,
    )
