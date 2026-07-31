from typing import ClassVar

from pydantic import BaseModel, ConfigDict

from app.tools.base import BaseTool, EmptyInput


class PromotionsOutput(BaseModel):
    model_config = ConfigDict(extra="forbid")

    data_source: str
    campaigns: list[dict[str, str]]


class TodayPromotionsTool(BaseTool):
    name: ClassVar[str] = "get_today_promotions"
    description: ClassVar[str] = "返回虚构的今日优惠活动。"
    input_model: ClassVar[type[BaseModel]] = EmptyInput
    output_model: ClassVar[type[BaseModel]] = PromotionsOutput
    requires_auth: ClassVar[bool] = False

    async def execute(self, payload: EmptyInput) -> dict[str, object]:
        return PromotionsOutput(
            data_source="mock",
            campaigns=[
                {"name": "青禾午餐满减券", "benefit": "满30元减5元"},
                {"name": "晚间简餐折扣", "benefit": "指定简餐九折"},
            ],
        ).model_dump()
