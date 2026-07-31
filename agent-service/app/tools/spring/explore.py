from typing import Any, ClassVar

from pydantic import BaseModel

from app.clients.endpoint_registry import EndpointName
from app.tools.spring.base import SpringReadTool, SpringToolOutput
from app.tools.spring.models import ExplorePostsInput, NearbyShopsInput


class GetNearbyShopsTool(SpringReadTool):
    name: ClassVar[str] = "get_nearby_shops"
    description: ClassVar[str] = "按临时经纬度、半径和分类查询附近商铺，不保存坐标。"
    input_model: ClassVar[type[BaseModel]] = NearbyShopsInput
    output_model: ClassVar[type[BaseModel]] = SpringToolOutput
    requires_auth: ClassVar[bool] = False
    endpoint = EndpointName.NEARBY_SHOPS

    def adapt_data(self, data: Any) -> Any:
        return _page(data, ("id", "categoryId", "name", "distance", "score"))


class GetHotExplorePostsTool(SpringReadTool):
    name: ClassVar[str] = "get_hot_explore_posts"
    description: ClassVar[str] = "按后端 hot 排序查询热门探店内容。"
    input_model: ClassVar[type[BaseModel]] = ExplorePostsInput
    output_model: ClassVar[type[BaseModel]] = SpringToolOutput
    requires_auth: ClassVar[bool] = False
    endpoint = EndpointName.EXPLORE_POSTS

    def adapt_data(self, data: Any) -> Any:
        return _page(data, ("id", "shopId", "shopName", "authorName", "title", "content", "likeCount", "commentCount", "createdAt"))


def _page(data: Any, fields: tuple[str, ...]) -> Any:
    if data is None:
        return None
    if not isinstance(data, dict):
        return {"records": [], "total": 0, "page": 1, "size": 0}
    records = data.get("records") if isinstance(data.get("records"), list) else []
    return {
        "records": [{key: item.get(key) for key in fields if key in item} for item in records if isinstance(item, dict)],
        "total": data.get("total", len(records)), "page": data.get("page", 1), "size": data.get("size", len(records)),
    }
