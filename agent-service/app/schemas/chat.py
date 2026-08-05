from typing import Any
from uuid import uuid4

from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.schemas.tool import ToolCallTrace


class ChatRequest(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)

    message: str = Field(min_length=1, max_length=1000)
    conversation_id: str | None = Field(default=None, pattern=r"^[A-Za-z0-9][A-Za-z0-9_-]{0,63}$")
    metadata: dict[str, str] = Field(default_factory=dict, max_length=20)

    @field_validator("metadata")
    @classmethod
    def reject_sensitive_metadata(cls, value: dict[str, str]) -> dict[str, str]:
        forbidden = {"authorization", "authorization_token", "token", "bearer", "user_id", "userid"}
        if any(key.lower() in forbidden for key in value):
            raise ValueError("身份凭据只能通过 Authorization Header 传递")
        return value


class ChatResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    request_id: str
    conversation_id: str
    answer: str
    intent: str
    tool_calls: list[ToolCallTrace]
    mock: bool
    tool_mode: str
    provider: str
    duration_ms: int = Field(ge=0)
    warnings: list[str]


class ConversationClearResponse(BaseModel):
    conversation_id: str
    cleared: bool


def ensure_conversation_id(value: str | None) -> str:
    return value or uuid4().hex


class HealthResponse(BaseModel):
    service: str
    status: str
    version: str
    environment: str
    mock_mode: bool
    timestamp: str


class ReadyResponse(BaseModel):
    service: str
    status: str
    provider: str
    tool_count: int
    mock_mode: bool


class ToolsResponse(BaseModel):
    tools: list[dict[str, Any]]
