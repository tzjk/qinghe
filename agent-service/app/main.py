import logging
import time
from contextlib import asynccontextmanager
from dataclasses import dataclass

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.agent.graph import AgentGraph
from app.agent.orchestrator import AgentOrchestrator
from app.cache.public import PublicResponseCache
from app.conversation import InMemoryConversationStore
from app.api import chat, health
from app.core.config import Settings, get_settings
from app.core.errors import AgentError
from app.core.logging import configure_logging, log_event
from app.core.limits import InMemoryLimiter
from app.core.request_context import (
    bind_request_context,
    extract_bearer_token,
    new_request_id,
    reset_request_context,
)
from app.middleware.http import AccessLogMiddleware, BodyLimitMiddleware, ExceptionMappingMiddleware, RateLimitMiddleware, RequestIdMiddleware
from app.observability.metrics import InMemoryMetrics
from app.observability.tracing import SafeTracer
from app.providers.factory import create_provider
from app.schemas.error import ErrorDetail, ErrorResponse
from app.tools.mock.orders import create_mock_registry
from app.tools.spring import create_spring_registry
from app.token_budget import TokenBudgetManager, ToolSchemaSelector
from app.model_routing.registry import ModelRegistry
from app.model_routing.router import ModelRouter

logger = logging.getLogger(__name__)


@dataclass(slots=True)
class AgentServices:
    provider: object
    registry: object
    graph: AgentGraph
    metrics: InMemoryMetrics
    backend_client: object | None = None
    conversations: InMemoryConversationStore | None = None
    limits: InMemoryLimiter | None = None
    token_budget: TokenBudgetManager | None = None
    public_cache: PublicResponseCache | None = None
    tracer: SafeTracer | None = None


def error_response(error: AgentError, request_id: str) -> JSONResponse:
    payload = ErrorResponse(
        request_id=request_id,
        error=ErrorDetail(code=error.code, message=error.message, retryable=error.retryable),
    )
    return JSONResponse(status_code=error.status_code, content=payload.model_dump())


def create_app(
    settings: Settings | None = None,
    *,
    provider: object | None = None,
    backend_transport: object | None = None,
    backend_data_source: str = "spring",
) -> FastAPI:
    active_settings = settings or get_settings()
    configure_logging(active_settings.agent_log_level)
    provider = provider or create_provider(active_settings)
    backend_client = None
    if active_settings.agent_tool_mode == "mock":
        registry = create_mock_registry()
    else:
        registry, backend_client = create_spring_registry(
            active_settings, transport=backend_transport, data_source=backend_data_source
        )
    metrics = InMemoryMetrics()
    limits = InMemoryLimiter(
        per_minute=active_settings.agent_rate_limit_per_minute,
        max_requests=active_settings.agent_max_concurrent_requests,
        max_model_calls=active_settings.agent_max_concurrent_model_calls,
        max_tool_calls=active_settings.agent_max_concurrent_tool_calls,
        max_sse_connections=active_settings.agent_max_sse_connections,
    )
    token_budget = TokenBudgetManager(active_settings)
    model_registry = ModelRegistry(active_settings)
    public_cache = PublicResponseCache(enabled=active_settings.agent_public_cache_enabled, ttl_seconds=active_settings.agent_public_cache_ttl_seconds, max_entries=active_settings.agent_public_cache_max_entries)
    tracer = SafeTracer()
    orchestrator = AgentOrchestrator(
        settings=active_settings, provider=provider, registry=registry, metrics=metrics, limits=limits,
        token_budget=token_budget, schema_selector=ToolSchemaSelector(active_settings.agent_tool_schema_max_count),
        model_registry=model_registry, model_router=ModelRouter(model_registry), public_cache=public_cache, tracer=tracer,
    )

    @asynccontextmanager
    async def lifespan(_: FastAPI):
        try:
            yield
        finally:
            if backend_client is not None:
                await backend_client.close()

    app = FastAPI(title=active_settings.agent_app_name, version="0.1.0", lifespan=lifespan)
    app.state.settings = active_settings
    app.state.services = AgentServices(
        provider=provider,
        registry=registry,
        graph=AgentGraph(orchestrator),
        metrics=metrics,
        backend_client=backend_client,
        conversations=InMemoryConversationStore(
            max_turns=active_settings.agent_conversation_max_turns,
            ttl_minutes=active_settings.agent_conversation_ttl_minutes,
            max_count=active_settings.agent_conversation_max_count,
        ),
        limits=limits,
        token_budget=token_budget,
        public_cache=public_cache,
        tracer=tracer,
    )
    app.add_middleware(
        CORSMiddleware,
        allow_origins=active_settings.allowed_origins,
        allow_credentials=False,
        allow_methods=["GET", "POST"],
        allow_headers=["Content-Type", "Authorization", "X-Request-ID"],
    )

    request_id_stage = RequestIdMiddleware(app)
    body_limit_stage = BodyLimitMiddleware(app)
    rate_limit_stage = RateLimitMiddleware(app)
    access_log_stage = AccessLogMiddleware(app)
    exception_stage = ExceptionMappingMiddleware(
        app,
        mapper=lambda error, request: error_response(error, getattr(request.state, "request_id", new_request_id())),
    )

    @app.middleware("http")
    async def request_context_middleware(request: Request, call_next):
        return await request_id_stage.dispatch(
            request,
            lambda current: body_limit_stage.dispatch(
                current,
                lambda limited: rate_limit_stage.dispatch(
                    limited,
                    lambda admitted: access_log_stage.dispatch(
                        admitted,
                        lambda logged: exception_stage.dispatch(logged, call_next),
                    ),
                ),
            ),
        )

    @app.exception_handler(AgentError)
    async def handle_agent_error(_: Request, error: AgentError) -> JSONResponse:
        return error_response(error, new_request_id())

    @app.exception_handler(RequestValidationError)
    async def handle_validation_error(_: Request, __: RequestValidationError) -> JSONResponse:
        return error_response(
            AgentError("AGENT_VALIDATION_ERROR", "请求参数不符合要求。", False, 422),
            new_request_id(),
        )

    @app.exception_handler(Exception)
    async def handle_internal_error(_: Request, error: Exception) -> JSONResponse:
        logger.exception("agent_internal_error", exc_info=error)
        return error_response(
            AgentError("AGENT_INTERNAL_ERROR", "服务暂时不可用，请稍后重试。", True, 500),
            new_request_id(),
        )

    app.include_router(health.router)
    app.include_router(chat.router)
    return app


app = create_app()
