"""Small ASGI middleware units. The FastAPI composition remains API-contract neutral."""
import time
from collections.abc import Awaitable, Callable
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import Response
from fastapi.middleware.cors import CORSMiddleware as CorsMiddleware

from app.core.errors import AgentError
from app.core.request_context import bind_request_context, extract_bearer_token, new_request_id, reset_request_context


class RequestIdMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        request_id = new_request_id()
        request.state.request_id = request_id
        token = bind_request_context(request_id, authorization_token=extract_bearer_token(request.headers.get("Authorization")))
        try:
            response = await call_next(request)
            response.headers["X-Request-ID"] = request_id
            return response
        finally:
            reset_request_context(token)


class BodyLimitMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        if request.method == "POST":
            maximum = request.app.state.settings.agent_max_request_bytes
            length = request.headers.get("content-length")
            if length and int(length) > maximum:
                raise AgentError("AGENT_VALIDATION_ERROR", "请求体过大，请缩短后重试。", False, 413)
            if len(await request.body()) > maximum:
                raise AgentError("AGENT_VALIDATION_ERROR", "请求体过大，请缩短后重试。", False, 413)
        return await call_next(request)


class RateLimitMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        if request.method == "POST" and request.url.path in {"/api/v1/chat", "/api/v1/chat/stream"}:
            await request.app.state.services.limits.admit_ip(request.client.host if request.client else "unknown")
        return await call_next(request)


class AccessLogMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        started = time.perf_counter()
        response = await call_next(request)
        # Deliberately no message/header/body logging.
        with request.app.state.services.tracer.span("agent.request", request_id=getattr(request.state, "request_id", "unknown"), success=response.status_code < 500, duration_ms=int((time.perf_counter() - started) * 1000)):
            pass
        return response


class ExceptionMappingMiddleware(BaseHTTPMiddleware):
    def __init__(self, app, mapper: Callable[[AgentError, Request], Response] | None = None) -> None:
        super().__init__(app)
        self._mapper = mapper

    async def dispatch(self, request: Request, call_next):
        try:
            return await call_next(request)
        except AgentError as error:
            if self._mapper is None:
                raise
            return self._mapper(error, request)
