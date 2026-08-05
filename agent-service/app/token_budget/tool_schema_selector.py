from dataclasses import dataclass

from app.schemas.tool import ToolMetadata


_INTENT_TOOLS: dict[str, tuple[str, ...]] = {
    "promotion_query": ("get_today_promotions",),
    "active_coupons": ("get_active_coupons", "get_today_promotions"),
    "coupon_wallet": ("get_my_coupon_wallet",),
    "shop_recommendation": ("search_shops",),
    "shop_detail": ("get_shop_detail", "search_shops"),
    "shop_goods": ("get_shop_goods",),
    "nearby_shop_query": ("get_nearby_shops", "search_shops"),
    "hot_explore_query": ("get_hot_explore_posts",),
    "dorm_query": ("get_my_dorm_info",),
    "order_query": ("get_my_recent_orders", "get_my_order_detail"),
    "order_detail": ("get_my_order_detail", "get_my_recent_orders"),
    "profile_query": ("get_my_profile",),
    "student_profile": ("get_my_student_profile",),
    "address_query": ("get_my_addresses", "get_my_default_address"),
}


@dataclass(frozen=True, slots=True)
class SchemaSelection:
    tools: tuple[ToolMetadata, ...]
    total_available: int
    excluded: tuple[str, ...]
    reason: str


class ToolSchemaSelector:
    def __init__(self, max_count: int = 4) -> None:
        self._max_count = max_count

    def select(self, *, intent: str, tools: list[ToolMetadata], authenticated: bool, recent_tool_names: tuple[str, ...] = ()) -> SchemaSelection:
        wanted = _INTENT_TOOLS.get(intent, ())
        if intent in {"greeting", "help", "clarification", "contextual_clarification", "general_chat", "unsupported", "discount_query"}:
            return SchemaSelection((), len(tools), tuple(item.name for item in tools), "no_tool_intent")
        by_name = {item.name: item for item in tools}
        selected: list[ToolMetadata] = []
        for name in wanted or recent_tool_names:
            item = by_name.get(name)
            if item and (authenticated or not item.requires_auth) and item not in selected:
                selected.append(item)
            if len(selected) >= self._max_count:
                break
        excluded = tuple(item.name for item in tools if item not in selected)
        return SchemaSelection(tuple(selected), len(tools), excluded, f"intent:{intent};auth:{authenticated}")
