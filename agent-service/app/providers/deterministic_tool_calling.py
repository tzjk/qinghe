import re
from typing import Any

from app.agent.provider_types import ProviderToolCall
from app.conversation import ConversationContext
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
        self, *, intent: str, message: str, tool_results: list[dict[str, Any]], conversation_context: ConversationContext | None = None
    ) -> str:
        if not tool_results:
            if intent == "greeting":
                return "你好呀，我是清禾校园小助手。可以帮你查优惠、找店铺、看订单和宿舍信息，也能回答一些校园生活中的常见问题。今天想了解什么？"
            if intent == "help":
                return "我可以帮你查优惠、找店铺、看订单和宿舍信息，也能聊聊学习和校园生活中的常见问题。你可以直接说，例如“今天有什么优惠”或“推荐一家晚餐店”。"
            if intent == "clarification":
                return "我想帮你查得更准一些。你是想了解优惠、店铺，还是校园生活里的其他问题？"
            if intent == "contextual_clarification":
                return "我明白，你还在比较刚才那家店。你想看看它更实惠的商品，还是想换一家店对比？"
            if intent == "general_chat":
                return "当然可以，我们可以先聊聊这个问题。要是需要校园里的实时信息，我会帮你查询已接入的内容。"
            if intent == "unsupported":
                return "这个请求我暂时不能直接帮你处理。你可以问我校园优惠、店铺、订单、宿舍，或校园生活中的常见问题。"
            if intent == "nearby_shop_query":
                return "还差一点位置信息。你可以提供临时经度和纬度，我就能帮你找附近的店铺；这些坐标不会被保存。"
            if intent == "discount_query":
                return "我目前能确认的是优惠券信息，暂时不能判断某件商品是不是正在打折。你可以问我今天有哪些优惠券，或者想找哪类店铺。"
            return "我暂时没有对应的查询入口，不过可以帮你看看校园优惠、店铺、订单或宿舍信息。"
        failed = next((item for item in tool_results if not item.get("success")), None)
        if failed:
            reason = str(failed.get("user_message") or "这次查询没有拿到结果")
            return f"这次没能帮你查到，{reason}。你可以稍后再试，或换一种问法告诉我想查什么。"
        first = tool_results[0]
        data = first.get("data") or {}
        if intent in {"promotion_query", "active_coupons"}:
            records = _sorted_records(data, _coupon_rank)
            if not records:
                return "暂时没查到可领取的优惠券。你可以晚点再看看，或者让我帮你找找附近店铺。"
            if any(word in message for word in ("最值得", "推荐", "划算", "最优")):
                best = records[0]
                return (
                    f"如果按当前公开的优惠力度和使用门槛看，我更推荐优先考虑“{best.get('name', '这张优惠券')}”："
                    f"{_coupon_summary(best)}。是否最适合你，还要看你的消费金额和领取条件。"
                )
            prefix = "今天有这些优惠可以看看：" if intent == "promotion_query" else "目前有这些优惠可以看看："
            return prefix + "；".join(_coupon_summary(item) for item in records)
        if intent == "shop_recommendation":
            records = _sorted_records(data, _shop_rank)
            if not records:
                return "暂时没找到合适的店铺。你可以告诉我想吃什么，或希望离哪里近一些。"
            best = records[0]
            name = str(best.get("name", "这家店"))
            score = best.get("score")
            reason = f"评分{score}" if score is not None else "公开信息比较完整"
            alternatives = [str(item.get("name", "未命名商铺")) for item in records[1:3]]
            extra = f"另外也可以看看{'、'.join(alternatives)}。" if alternatives else ""
            meal_hint = "晚饭" if any(word in message for word in ("晚饭", "吃饭")) else "校园用餐"
            return f"我更推荐你先看看{name}，它在当前{meal_hint}结果里{reason}。{extra}想继续的话，我可以帮你查这家店有什么商品。"
        if intent == "shop_detail":
            if isinstance(data, dict):
                return f"商铺详情：{data.get('name', '未命名商铺')}；地址：{data.get('address', '暂无')}；评分：{data.get('score', '暂无')}。"
            return "暂时没查到这家店的详情。你可以换个店铺名称，或让我推荐几家店。"
        if intent == "shop_goods":
            records = data.get("records", []) if isinstance(data, dict) else []
            if not records:
                return "这家店暂时没有可展示的在售商品。你可以看看其他店铺，或换一家店再问我。"
            return "这家店目前有这些商品：" + "、".join(_goods_summary(item) for item in records if isinstance(item, dict))
        if intent == "hot_explore_query":
            records = data.get("records", []) if isinstance(data, dict) else []
            return "最近大家在看：" + "、".join(str(item.get("title", item.get("shopName", "未命名内容"))) for item in records if isinstance(item, dict)) if records else "暂时还没有热门探店内容。你也可以让我推荐一家店。"
        if intent == "nearby_shop_query":
            records = data.get("records", []) if isinstance(data, dict) else []
            return "附近可以看看：" + "、".join(_nearby_summary(item) for item in records if isinstance(item, dict)) if records else "这附近暂时没找到合适的店铺。可以试着把搜索范围调大一点。"
        if intent == "dorm_query":
            if isinstance(data, dict):
                return f"你的宿舍：{data.get('campus', '')}{data.get('building', '')}{data.get('room', '')}室{data.get('bed', '')}床。"
        if intent in {"order_query", "order_detail"}:
            records = data.get("records", []) if isinstance(data, dict) else []
            if intent == "order_query" and not records:
                return "你目前还没有可展示的订单记录。之后想查订单状态时，直接告诉我就行。"
            if intent == "order_query":
                return "我的订单：" + "；".join(_order_summary(item) for item in records if isinstance(item, dict)) + "。模拟支付状态不代表真实付款。"
            return "订单详情：" + _order_summary(data) + "。这里的支付状态仅用于模拟展示。" if isinstance(data, dict) else "暂时没查到这笔订单的详情。你可以确认一下订单号后再问我。"
        if intent == "coupon_wallet":
            return "已经帮你整理好优惠券列表了，过期券会按实际状态显示。"
        if intent == "profile_query":
            return "已经帮你查到当前账号的基本资料了。"
        if intent == "student_profile":
            return "已经帮你查到学生档案摘要了。"
        if intent == "address_query":
            return "已经帮你查到地址摘要了。"
        return "查到了，我已经根据最新结果帮你整理好了。"

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


def _sorted_records(data: Any, rank) -> list[dict[str, Any]]:
    records = data.get("records", []) if isinstance(data, dict) else []
    normalized = [item for item in records if isinstance(item, dict)]
    return sorted(normalized, key=rank, reverse=True)


def _coupon_rank(item: dict[str, Any]) -> tuple[int, float, float]:
    amount = _as_float(item.get("discountAmount"))
    threshold = _as_float(item.get("thresholdAmount"))
    if amount is not None:
        return (2, amount, -(threshold or 0.0))
    rate = _as_float(item.get("discountRate"))
    if rate is not None:
        return (1, rate, -(threshold or 0.0))
    return (0, 0.0, -(threshold or 0.0))


def _shop_rank(item: dict[str, Any]) -> tuple[int, float]:
    score = _as_float(item.get("score"))
    return (1 if score is not None else 0, score or 0.0)


def _as_float(value: Any) -> float | None:
    try:
        return float(value) if value is not None else None
    except (TypeError, ValueError):
        return None


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
