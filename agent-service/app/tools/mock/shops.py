from typing import ClassVar

from pydantic import BaseModel, ConfigDict, Field

from app.tools.base import BaseTool


class ShopSearchInput(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)

    keyword: str = Field(default="晚饭", min_length=1, max_length=80)


class ShopsOutput(BaseModel):
    model_config = ConfigDict(extra="forbid")

    data_source: str
    shops: list[dict[str, str]]


class SearchShopsTool(BaseTool):
    name: ClassVar[str] = "search_shops"
    description: ClassVar[str] = "按场景或关键词返回虚构商铺。"
    input_model: ClassVar[type[BaseModel]] = ShopSearchInput
    output_model: ClassVar[type[BaseModel]] = ShopsOutput
    requires_auth: ClassVar[bool] = False

    async def execute(self, payload: ShopSearchInput) -> dict[str, object]:
        return ShopsOutput(
            data_source="mock",
            shops=[
                {"name": "湖畔简餐", "reason": f"适合{payload.keyword}，出餐较快"},
                {"name": "松风面馆", "reason": "提供虚构的热食套餐"},
            ],
        ).model_dump()
