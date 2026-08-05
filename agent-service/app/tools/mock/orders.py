from typing import ClassVar

from pydantic import BaseModel, ConfigDict

from app.tools.base import BaseTool, EmptyInput


class OrdersOutput(BaseModel):
    model_config = ConfigDict(extra="forbid")

    data_source: str
    orders: list[dict[str, str]]


class MyRecentOrdersTool(BaseTool):
    name: ClassVar[str] = "get_my_recent_orders"
    description: ClassVar[str] = "返回虚构的当前用户订单摘要，不接收身份参数。"
    input_model: ClassVar[type[BaseModel]] = EmptyInput
    output_model: ClassVar[type[BaseModel]] = OrdersOutput
    requires_auth: ClassVar[bool] = True

    async def execute(self, payload: EmptyInput) -> dict[str, object]:
        return OrdersOutput(
            data_source="mock",
            orders=[
                {"order_no": "MOCK-20260730-001", "status": "配送中"},
                {"order_no": "MOCK-20260729-002", "status": "已完成"},
            ],
        ).model_dump()


def create_mock_registry() -> "ToolRegistry":
    from app.tools.mock.contract_tools import create_contract_mock_tools
    from app.tools.registry import ToolRegistry

    registry = ToolRegistry()
    for tool in create_contract_mock_tools():
        registry.register(tool)
    return registry
