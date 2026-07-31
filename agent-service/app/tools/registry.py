from typing import Any

from pydantic import BaseModel

from app.core.errors import AUTH_REQUIRED, AgentError
from app.core.request_context import get_request_context
from app.schemas.tool import ToolMetadata, ToolResult
from app.tools.base import BaseTool


class ToolRegistry:
    def __init__(self) -> None:
        self._tools: dict[str, BaseTool] = {}

    def register(self, tool: BaseTool) -> None:
        if tool.name in self._tools:
            raise ValueError(f"重复工具注册：{tool.name}")
        self._tools[tool.name] = tool

    def get(self, name: str) -> BaseTool:
        try:
            return self._tools[name]
        except KeyError as exc:
            raise AgentError(
                code="AGENT_TOOL_NOT_FOUND",
                message="请求的工具未注册。",
                retryable=False,
                status_code=400,
            ) from exc

    async def execute(self, name: str, payload: dict[str, Any] | None = None) -> ToolResult:
        tool = self.get(name)
        context = get_request_context()
        if tool.requires_auth and not (context and context.authorization_token):
            raise AUTH_REQUIRED
        validated: BaseModel = tool.input_model.model_validate(payload or {})
        result = await tool.execute(validated)
        if isinstance(result, ToolResult):
            return result
        return ToolResult(
            success=True,
            tool_name=tool.name,
            data=result,
            data_source=tool.data_source,
        )

    def metadata(self) -> list[ToolMetadata]:
        return [
            ToolMetadata(
                name=tool.name,
                description=tool.description,
                requires_auth=tool.requires_auth,
                data_source=tool.data_source,
                timeout_seconds=tool.timeout_seconds,
                input_schema=tool.input_model.model_json_schema(),
            )
            for tool in self._tools.values()
        ]

    @property
    def count(self) -> int:
        return len(self._tools)
