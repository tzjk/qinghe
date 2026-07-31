import re
from typing import Any

from app.agent.provider_types import ProviderToolCall
from app.providers.base import BaseLLMProvider
from app.schemas.tool import ToolMetadata


class DeterministicToolCallingProvider(BaseLLMProvider):
    """Offline-only provider used by tests and the local Mock mode.

    It receives only the user message, intent, tool metadata, and normalized results.
    Request context and credentials are deliberately outside this interface.
    """

    name = "deterministic"

    def __init__(self, scripted_calls: dict[str, object] | None = None) -> None:
        self.scripted_calls = scripted_calls or {}
        self.observed_inputs: list[dict[str, object]] = []

    async def select_tools(
        self, *, intent: str, message: str, max_tools: int, tools: list[ToolMetadata]
    ) -> list[ProviderToolCall]:
        self.observed_inputs.append(
            {"intent": intent, "message": message, "tool_names": [tool.name for tool in tools]}
        )
        scripted = self.scripted_calls.get(message)
        if scripted is not None:
            return scripted  # type: ignore[return-value]
        if intent == "promotion_query":
            return [ProviderToolCall("get_today_promotions", {})]
        if intent == "active_coupons":
            return [ProviderToolCall("get_active_coupons", {})]
        if intent == "coupon_wallet":
            return [ProviderToolCall("get_my_coupon_wallet", {})]
        if intent == "shop_recommendation":
            keyword = "晚饭" if any(word in message for word in ("晚饭", "吃饭")) else None
            return [ProviderToolCall("search_shops", {"keyword": keyword} if keyword else {})]
        if intent == "shop_detail":
            shop_id = _first_positive_integer(message)
            return [ProviderToolCall("get_shop_detail", {"id": shop_id})] if shop_id else []
        if intent == "shop_goods":
            shop_id = _first_positive_integer(message)
            return [ProviderToolCall("get_shop_goods", {"id": shop_id})] if shop_id else []
        if intent == "hot_explore_query":
            return [ProviderToolCall("get_hot_explore_posts", {})]
        if intent == "profile_query":
            return [ProviderToolCall("get_my_profile", {})]
        if intent == "student_profile":
            return [ProviderToolCall("get_my_student_profile", {})]
        if intent == "address_query":
            return [ProviderToolCall("get_my_addresses", {})]
        if intent == "nearby_shop_query":
            longitude = _number_after(message, "经度")
            latitude = _number_after(message, "纬度")
            if longitude is None or latitude is None:
                return []
            arguments: dict[str, float] = {"longitude": longitude, "latitude": latitude}
            radius = _number_after(message, "半径")
            if radius is not None:
                arguments["radius"] = radius
            return [ProviderToolCall("get_nearby_shops", arguments)]
        if intent == "dorm_query":
            return [ProviderToolCall("get_my_dorm_info", {})]
        if intent == "order_detail":
            order_id = _first_positive_integer(message)
            return [ProviderToolCall("get_my_order_detail", {"order_id": order_id})] if order_id else []
        if intent == "order_query":
            return [ProviderToolCall("get_my_recent_orders", {})]
        return []

    async def generate_response(
        self, *, intent: str, message: str, tool_results: list[dict[str, Any]]
    ) -> str:
        if not tool_results:
            if intent == "nearby_shop_query":
                return "请提供临时经度和纬度后再查询附近商铺；坐标不会被保存。"
            if intent == "discount_query":
                return "当前系统只有优惠券的实时依据，不能把普通商品价格描述为折扣价格。"
            return "该问题当前没有可调用的只读工具。"
        failed = next((item for item in tool_results if not item.get("success")), None)
        if failed:
            return str(failed.get("user_message") or "查询未完成，未返回任何业务数据。")
        first = tool_results[0]
        data = first.get("data") or {}
        if intent in {"promotion_query", "active_coupons"}:
            records = data.get("records", []) if isinstance(data, dict) else []
            if not records:
                return "目前没有可领取的优惠券。"
            prefix = "今天可领取的优惠券：" if intent == "promotion_query" else "当前可领取的优惠券："
            return prefix + "；".join(_coupon_summary(item) for item in records if isinstance(item, dict))
        if intent == "shop_recommendation":
            records = data.get("records", []) if isinstance(data, dict) else []
            prefix = "适合晚饭的商铺：" if any(word in message for word in ("晚饭", "吃饭")) else "商铺："
            return prefix + "、".join(str(item.get("name", "未命名商铺")) for item in records) if records else "没有符合条件的商铺。"
        if intent == "shop_detail":
            if isinstance(data, dict):
                return f"商铺详情：{data.get('name', '未命名商铺')}；地址：{data.get('address', '暂无')}；评分：{data.get('score', '暂无')}。"
            return "未查询到商铺详情。"
        if intent == "shop_goods":
            records = data.get("records", []) if isinstance(data, dict) else []
            if not records:
                return "该商铺当前没有可展示的在售商品。"
            return "商铺商品：" + "、".join(_goods_summary(item) for item in records if isinstance(item, dict))
        if intent == "hot_explore_query":
            records = data.get("records", []) if isinstance(data, dict) else []
            return "热门探店：" + "、".join(str(item.get("title", item.get("shopName", "未命名内容"))) for item in records if isinstance(item, dict)) if records else "目前没有热门探店内容。"
        if intent == "nearby_shop_query":
            records = data.get("records", []) if isinstance(data, dict) else []
            return "附近商铺：" + "、".join(_nearby_summary(item) for item in records if isinstance(item, dict)) if records else "附近没有符合条件的商铺。"
        if intent == "dorm_query":
            if isinstance(data, dict):
                return f"你的宿舍：{data.get('campus', '')}{data.get('building', '')}{data.get('room', '')}室{data.get('bed', '')}床。"
        if intent in {"order_query", "order_detail"}:
            records = data.get("records", []) if isinstance(data, dict) else []
            if intent == "order_query" and not records:
                return "目前没有订单记录。"
            if intent == "order_query":
                return "我的订单：" + "；".join(_order_summary(item) for item in records if isinstance(item, dict)) + "。模拟支付状态不代表真实付款。"
            return "订单详情：" + _order_summary(data) + "。模拟支付状态不代表真实付款。" if isinstance(data, dict) else "未查询到订单详情。"
        if intent == "coupon_wallet":
            return "已返回你的优惠券列表；过期券会按后端状态展示。"
        if intent == "profile_query":
            return "已返回当前登录用户的必要资料。"
        if intent == "student_profile":
            return "已返回当前登录用户的学生档案摘要。"
        if intent == "address_query":
            return "已返回当前登录用户的地址摘要。"
        return "已根据只读工具结果完成查询。"

    async def health_check(self) -> bool:
        return True


def _number_after(message: str, label: str) -> float | None:
    matched = re.search(rf"{label}\s*[:：]?\s*(-?\d+(?:\.\d+)?)", message)
    return float(matched.group(1)) if matched else None


def _first_positive_integer(message: str) -> int | None:
    matched = re.search(r"\d+", message)
    value = int(matched.group(0)) if matched else 0
    return value if value > 0 else None


def _coupon_summary(item: dict[str, Any]) -> str:
    name = str(item.get("name", "未命名优惠券"))
    if item.get("discountAmount") is not None:
        benefit = f"减{item['discountAmount']}元"
    elif item.get("discountRate") is not None:
        benefit = f"{item['discountRate']}折"
    else:
        benefit = "优惠方式以券面为准"
    threshold = f"满{item['thresholdAmount']}元可用" if item.get("thresholdAmount") is not None else "无使用门槛"
    validity = f"有效期{item.get('useStartTime', '未提供')}至{item.get('useEndTime', '未提供')}"
    stock = f"剩余{item['availableStock']}张" if item.get("availableStock") is not None else "剩余数量未提供"
    return f"{name}（{benefit}，{threshold}，{validity}，{stock}）"


def _nearby_summary(item: dict[str, Any]) -> str:
    name = str(item.get("name", "未命名商铺"))
    distance = f"，距离{item['distance']}" if item.get("distance") is not None else ""
    category = f"，分类{item['categoryId']}" if item.get("categoryId") is not None else ""
    return f"{name}{distance}{category}"


def _goods_summary(item: dict[str, Any]) -> str:
    name = str(item.get("name", "未命名商品"))
    price = f"，价格{item['price']}" if item.get("price") is not None else ""
    return f"{name}{price}"


def _order_summary(item: dict[str, Any]) -> str:
    return "，".join(
        str(item[key]) for key in ("orderNo", "shopName", "payAmount", "totalAmount", "statusName", "status", "createTime")
        if item.get(key) is not None
    ) or "订单摘要为空"
