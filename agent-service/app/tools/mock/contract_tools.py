from collections.abc import Callable
from typing import Any, ClassVar

from pydantic import BaseModel

from app.schemas.tool import ToolResult
from app.tools.base import BaseTool
from app.tools.spring.models import EmptyToolInput, RecentOrdersInput, ShopSearchInput


class ContractMockTool(BaseTool):
    data_source: ClassVar[str] = "contract_mock"

    def __init__(
        self,
        *,
        name: str,
        description: str,
        input_model: type[BaseModel],
        requires_auth: bool,
        response_factory: Callable[[BaseModel], Any],
    ) -> None:
        self.name = name
        self.description = description
        self.input_model = input_model
        self.requires_auth = requires_auth
        self._response_factory = response_factory

    async def execute(self, payload: BaseModel) -> ToolResult:
        return ToolResult(
            success=True,
            tool_name=self.name,
            data=self._response_factory(payload),
            data_source=self.data_source,
        )


def create_contract_mock_tools() -> tuple[BaseTool, ...]:
    from app.tools.spring.dorm import GetMyDormInfoTool
    from app.tools.spring.orders import GetMyRecentOrdersTool
    from app.tools.spring.promotions import GetTodayPromotionsTool
    from app.tools.spring.shops import SearchShopsTool

    return (
        ContractMockTool(
            name=GetTodayPromotionsTool.name,
            description=GetTodayPromotionsTool.description,
            input_model=EmptyToolInput,
            requires_auth=False,
            response_factory=lambda _: {
                "records": [
                    {
                        "name": "晴川晚餐券",
                        "status": "ENABLED",
                        "stock": 18,
                        "receiveStartTime": "2026-01-01T00:00:00Z",
                        "receiveEndTime": "2099-12-31T23:59:59Z",
                    }
                ],
                "total": 1,
                "page": 1,
                "size": 10,
            },
        ),
        ContractMockTool(
            name=SearchShopsTool.name,
            description=SearchShopsTool.description,
            input_model=ShopSearchInput,
            requires_auth=False,
            response_factory=lambda payload: {
                "records": [
                    {"name": "晴川面馆", "category": "餐饮", "keyword": payload.keyword or ""}
                ],
                "total": 1,
                "page": payload.page,
                "size": payload.size,
            },
        ),
        ContractMockTool(
            name=GetMyDormInfoTool.name,
            description=GetMyDormInfoTool.description,
            input_model=EmptyToolInput,
            requires_auth=True,
            response_factory=lambda _: {"campus": "青禾演示校区", "building": "知行楼", "room": "302", "bed": "2"},
        ),
        ContractMockTool(
            name=GetMyRecentOrdersTool.name,
            description=GetMyRecentOrdersTool.description,
            input_model=RecentOrdersInput,
            requires_auth=True,
            response_factory=lambda payload: {
                "records": [{"orderNo": "SIM-001", "status": "DELIVERING"}],
                "total": 1,
                "page": payload.page,
                "size": payload.size,
            },
        ),
    )
