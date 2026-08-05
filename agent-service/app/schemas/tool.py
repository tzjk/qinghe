from typing import Any

from pydantic import BaseModel, ConfigDict, Field


class ToolCallTrace(BaseModel):
    model_config = ConfigDict(extra="forbid")

    tool_name: str
    data_source: str
    duration_ms: int = Field(ge=0)
    java_http_duration_ms: int = Field(default=0, ge=0)
    status: str


class ToolResult(BaseModel):
    """Agent internal normalized result. It is never a public response model."""

    model_config = ConfigDict(extra="forbid")

    success: bool
    tool_name: str
    data: Any | None = None
    data_source: str
    error_code: str | None = None
    user_message: str | None = None
    backend_code: int | str | None = None
    backend_request_id: str | None = None
    duration_ms: int = Field(default=0, ge=0)
    java_http_duration_ms: int = Field(default=0, ge=0)
    retryable: bool = False


class ToolMetadata(BaseModel):
    model_config = ConfigDict(extra="forbid")

    name: str
    description: str
    requires_auth: bool
    data_source: str
    timeout_seconds: int
    input_schema: dict[str, Any]
