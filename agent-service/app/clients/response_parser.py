from dataclasses import dataclass
from typing import Any

from app.clients.error_mapper import map_backend_error
from app.core.errors import AgentError


@dataclass(frozen=True, slots=True)
class ParsedBackendResponse:
    data: Any
    backend_code: int | str | None
    backend_request_id: str | None


def parse_backend_response(*, status_code: int, payload: Any, request_id: str | None) -> ParsedBackendResponse:
    if not isinstance(payload, dict):
        raise AgentError("AGENT_BACKEND_RESPONSE_INVALID", "后端返回格式异常。", True, 502)
    backend_code = payload.get("code")
    backend_message = payload.get("message")
    backend_request_id = payload.get("requestId") or payload.get("request_id") or request_id
    if status_code != 200:
        raise map_backend_error(status_code=status_code, backend_code=backend_code)
    if backend_code != 200:
        error = map_backend_error(status_code=None, backend_code=backend_code)
        raise error
    if "data" not in payload:
        raise AgentError("AGENT_BACKEND_RESPONSE_INVALID", "后端返回格式异常。", True, 502)
    return ParsedBackendResponse(
        data=payload["data"], backend_code=backend_code, backend_request_id=backend_request_id
    )
