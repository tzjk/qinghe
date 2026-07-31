from contextvars import ContextVar, Token
from dataclasses import dataclass
from datetime import datetime, timezone
from uuid import uuid4


@dataclass(frozen=True, slots=True)
class RequestContext:
    request_id: str
    conversation_id: str | None = None
    authorization_token: str | None = None
    tool_mode: str = "mock"
    mock_mode: bool = True
    started_at: datetime | None = None


_context: ContextVar[RequestContext | None] = ContextVar("agent_request_context", default=None)


def new_request_id() -> str:
    return uuid4().hex


def bind_request_context(
    request_id: str,
    conversation_id: str | None = None,
    authorization_token: str | None = None,
    tool_mode: str = "mock",
    mock_mode: bool = True,
) -> Token[RequestContext | None]:
    return _context.set(
        RequestContext(
            request_id=request_id,
            conversation_id=conversation_id,
            authorization_token=authorization_token,
            tool_mode=tool_mode,
            mock_mode=mock_mode,
            started_at=datetime.now(timezone.utc),
        )
    )


def bind_conversation_context(conversation_id: str) -> Token[RequestContext | None] | None:
    current = get_request_context()
    if current is None:
        return None
    return _context.set(
        RequestContext(
            request_id=current.request_id,
            conversation_id=conversation_id,
            authorization_token=current.authorization_token,
            tool_mode=current.tool_mode,
            mock_mode=current.mock_mode,
            started_at=current.started_at,
        )
    )


def get_request_context() -> RequestContext | None:
    return _context.get()


def reset_request_context(token: Token[RequestContext | None] | None) -> None:
    if token is not None:
        _context.reset(token)


def extract_bearer_token(value: str | None) -> str | None:
    if not value:
        return None
    scheme, _, token = value.partition(" ")
    if scheme.lower() != "bearer" or not token or token.strip() != token:
        return None
    return token
