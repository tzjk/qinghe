from typing import Any, ClassVar

from pydantic import BaseModel

from app.clients.endpoint_registry import EndpointName
from app.tools.spring.base import SpringReadTool, SpringToolOutput
from app.tools.spring.models import PositiveIdInput, ShopGoodsInput, ShopSearchInput


class SearchShopsTool(SpringReadTool):
    name: ClassVar[str] = "search_shops"
    description: ClassVar[str] = "按现有分类、关键词或排序条件查询商铺。"
    input_model: ClassVar[type[BaseModel]] = ShopSearchInput
    output_model: ClassVar[type[BaseModel]] = SpringToolOutput
    requires_auth: ClassVar[bool] = False
    endpoint = EndpointName.SHOPS

    def request_values(self, payload: ShopSearchInput):
        return payload.model_dump(exclude_none=True), {}

    def adapt_data(self, data: Any) -> Any:
        return _shop_page(data)


class GetShopDetailTool(SpringReadTool):
    name: ClassVar[str] = "get_shop_detail"
    description: ClassVar[str] = "按商铺 ID 查询公开详情。"
    input_model: ClassVar[type[BaseModel]] = PositiveIdInput
    output_model: ClassVar[type[BaseModel]] = SpringToolOutput
    requires_auth: ClassVar[bool] = False
    endpoint = EndpointName.SHOP_DETAIL

    def request_values(self, payload: PositiveIdInput):
        return {}, {"id": payload.id}

    def adapt_data(self, data: Any) -> Any:
        if not isinstance(data, dict):
            return None
        return {key: data.get(key) for key in ("id", "categoryId", "name", "address", "score", "coverImage") if key in data}


class GetShopGoodsTool(SpringReadTool):
    name: ClassVar[str] = "get_shop_goods"
    description: ClassVar[str] = "按商铺 ID 查询公开在售商品，不推断商品级优惠。"
    input_model: ClassVar[type[BaseModel]] = ShopGoodsInput
    output_model: ClassVar[type[BaseModel]] = SpringToolOutput
    requires_auth: ClassVar[bool] = False
    endpoint = EndpointName.SHOP_GOODS

    def request_values(self, payload: ShopGoodsInput):
        values = payload.model_dump(exclude_none=True)
        shop_id = values.pop("id")
        return values, {"id": shop_id}

    def adapt_data(self, data: Any) -> Any:
        if not isinstance(data, dict):
            return {"records": [], "total": 0, "page": 1, "size": 0}
        records = data.get("records") if isinstance(data.get("records"), list) else []
        return {
            "records": [
                {key: item.get(key) for key in ("id", "shopId", "categoryId", "name", "price", "stock", "saleStatus", "coverImage") if key in item}
                for item in records if isinstance(item, dict)
            ],
            "total": data.get("total", len(records)), "page": data.get("page", 1), "size": data.get("size", len(records)),
        }


def _shop_page(data: Any) -> Any:
    if data is None:
        return None
    if not isinstance(data, dict):
        return {"records": [], "total": 0, "page": 1, "size": 0}
    records = data.get("records") if isinstance(data.get("records"), list) else []
    return {
        "records": [
            {key: item.get(key) for key in ("id", "categoryId", "name", "address", "score", "coverImage") if key in item}
            for item in records if isinstance(item, dict)
        ],
        "total": data.get("total", len(records)), "page": data.get("page", 1), "size": data.get("size", len(records)),
    }
