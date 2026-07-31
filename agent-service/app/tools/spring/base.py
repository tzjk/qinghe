import time
from typing import Any, ClassVar

from pydantic import BaseModel, ConfigDict

from app.clients.endpoint_registry import EndpointName
from app.clients.qinghe_client import QingheClient
from app.core.errors import AgentError
from app.core.request_context import get_request_context
from app.schemas.tool import ToolResult
from app.tools.base import BaseTool


class SpringToolOutput(BaseModel):
    model_config = ConfigDict(extra="forbid")

    data_source: str = "spring"
    payload: Any


class SpringReadTool(BaseTool):
    data_source: ClassVar[str] = "spring"
    endpoint: ClassVar[EndpointName]

    def __init__(self, client: QingheClient) -> None:
        self._client = client

    def request_values(self, payload: BaseModel) -> tuple[dict[str, Any], dict[str, int]]:
        return payload.model_dump(exclude_none=True), {}

    def adapt_data(self, data: Any) -> Any:
        """Return only Agent-safe fields; subclasses understand their audited VO shape."""
        return data

    async def execute(self, payload: BaseModel) -> ToolResult:
        started = time.perf_counter()
        context = get_request_context()
        try:
            params, path_values = self.request_values(payload)
            response = await self._client.get(
                self.endpoint,
                params=params,
                path_values=path_values,
                authorization_token=context.authorization_token if context else None,
            )
            return ToolResult(
                success=True,
                tool_name=self.name,
                data=self.adapt_data(response.data),
                data_source=self._client.data_source,
                backend_code=response.backend_code,
                backend_request_id=response.backend_request_id,
                duration_ms=int((time.perf_counter() - started) * 1000),
            )
        except AgentError as error:
            return ToolResult(
                success=False,
                tool_name=self.name,
                data_source=self._client.data_source,
                error_code=error.code,
                user_message=error.message,
                duration_ms=int((time.perf_counter() - started) * 1000),
                retryable=error.retryable,
            )
