from abc import ABC, abstractmethod
from typing import Any, ClassVar

from pydantic import BaseModel, ConfigDict


class EmptyInput(BaseModel):
    model_config = ConfigDict(extra="forbid")


class BaseTool(ABC):
    name: ClassVar[str]
    description: ClassVar[str]
    input_model: ClassVar[type[BaseModel]]
    output_model: ClassVar[type[BaseModel]]
    requires_auth: ClassVar[bool]
    data_source: ClassVar[str] = "mock"
    timeout_seconds: ClassVar[int] = 2

    @abstractmethod
    async def execute(self, payload: BaseModel) -> dict[str, Any]:
        """仅返回本工具的确定性 Mock 数据。"""
