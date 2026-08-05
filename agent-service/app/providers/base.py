from abc import ABC, abstractmethod
from collections.abc import AsyncIterator
from typing import Any

from app.agent.provider_types import ProviderToolCall
from app.conversation import ConversationContext
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
        self, *, intent: str, message: str, tool_results: list[dict[str, Any]], conversation_context: ConversationContext | None = None
    ) -> str:
        """根据已验证的意图与工具数据生成面向用户的文本。"""

    async def stream_response(
        self, *, intent: str, message: str, tool_results: list[dict[str, Any]], conversation_context: ConversationContext | None = None
    ) -> AsyncIterator[str]:
        """Compatible default for local providers; remote providers override with token streaming."""
        answer = await self.generate_response(
            intent=intent, message=message, tool_results=tool_results, conversation_context=conversation_context
        )
        if answer:
            yield answer

    @abstractmethod
    async def health_check(self) -> bool:
        """不应在健康检查中发起非必要网络请求。"""
