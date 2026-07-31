from abc import ABC, abstractmethod
from typing import Any

from app.agent.provider_types import ProviderToolCall
from app.schemas.tool import ToolMetadata


class BaseLLMProvider(ABC):
    name: str

    @abstractmethod
    async def select_tools(
        self, *, intent: str, message: str, max_tools: int, tools: list[ToolMetadata]
    ) -> list[ProviderToolCall]:
        """只返回已知工具名称；调用方仍必须经 Registry 白名单验证。"""

    @abstractmethod
    async def generate_response(
        self, *, intent: str, message: str, tool_results: list[dict[str, Any]]
    ) -> str:
        """根据已验证的意图与工具数据生成面向用户的文本。"""

    @abstractmethod
    async def health_check(self) -> bool:
        """不应在健康检查中发起非必要网络请求。"""
