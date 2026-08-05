import re
from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class IntentResult:
    name: str
    safe: bool
    context_required: bool = False


_INJECTION_TERMS = (
    "忽略之前所有指令",
    "忽略所有指令",
    "ignore previous instructions",
    "system prompt",
    "系统prompt",
    "系统提示词",
    "输出提示词",
    "执行shell",
    "执行 shell",
    "shell命令",
    "数据库密码",
    "database password",
    "select ",
    "insert ",
    "update ",
    "delete ",
    "authorization",
    "/api/admin",
    "管理员工具",
    "访问任意网址",
    "任意网址",
)

_WRITE_OPERATION_TERMS = (
    "删除订单",
    "取消订单",
    "修改订单",
    "创建订单",
    "帮我下单",
    "支付订单",
    "删除地址",
)

_WRITE_OPERATION_PATTERNS = (
    r"(?:删除|取消|修改|创建|支付).{0,8}订单",
    r"删除.{0,8}地址",
)

_SHOP_REFERENCE_TERMS = ("第一家", "第二家", "这家", "那家", "它", "该店", "刚才那个", "刚才那家")


def detect_prompt_injection(message: str) -> bool:
    lowered = message.lower()
    if any(term in lowered for term in _INJECTION_TERMS):
        return True
    if any(term in message for term in _WRITE_OPERATION_TERMS):
        return True
    if any(re.search(pattern, message) for pattern in _WRITE_OPERATION_PATTERNS):
        return True
    if "领取优惠券" in message and not any(term in message for term in ("可领取优惠券", "有哪些可领取", "可领优惠券")):
        return True
    if re.search(r"(?:用户|user)\s*\d+", lowered) and any(
        word in message for word in ("宿舍", "订单", "优惠券")
    ):
        return True
    return ("其他用户" in message or "他人" in message or "别人的" in message) and (
        "宿舍" in message or "订单" in message or "优惠券" in message
    )


def classify_intent(message: str) -> IntentResult:
    if detect_prompt_injection(message):
        return IntentResult(name="unsupported", safe=False)
    text = message.lower()
    has_shop_reference = any(term in message for term in _SHOP_REFERENCE_TERMS)
    has_relative_price = any(marker in text for marker in ("再", "更")) and any(
        word in text for word in ("便宜", "预算", "低价", "更少")
    )
    if has_shop_reference or has_relative_price:
        if any(word in text for word in ("商品", "菜品", "吃什么")):
            return IntentResult(name="shop_goods", safe=True, context_required=True)
        if any(word in text for word in ("便宜", "预算", "低价", "更少")):
            return IntentResult(name="contextual_clarification", safe=True, context_required=True)
    if any(word in text for word in ("打折", "折扣商品", "优惠商品")):
        return IntentResult(name="discount_query", safe=True)
    if "附近" in text and any(word in text for word in ("商铺", "店铺")):
        return IntentResult(name="nearby_shop_query", safe=True)
    if "热门" in text and any(word in text for word in ("探店", "帖子", "内容")):
        return IntentResult(name="hot_explore_query", safe=True)
    if any(word in text for word in ("商品", "菜品")) and any(word in text for word in ("商铺", "店铺", "店")):
        return IntentResult(name="shop_goods", safe=True)
    if any(word in text for word in ("商铺详情", "店铺详情")):
        return IntentResult(name="shop_detail", safe=True)
    if any(word in text for word in ("学生档案", "学籍资料")):
        return IntentResult(name="student_profile", safe=True)
    if any(word in text for word in ("我的资料", "个人资料", "我的信息")):
        return IntentResult(name="profile_query", safe=True)
    if "地址" in text and "我的" in text:
        return IntentResult(name="address_query", safe=True)
    if "我的优惠券" in text or "我的券" in text:
        return IntentResult(name="coupon_wallet", safe=True)
    if any(word in text for word in ("可领取", "可用优惠券", "有哪些优惠券")):
        return IntentResult(name="active_coupons", safe=True)
    if "订单" in text and "详情" in text:
        return IntentResult(name="order_detail", safe=True)
    if any(word in text for word in ("你好", "您好", "嗨", "hello")):
        return IntentResult(name="greeting", safe=True)
    if any(word in text for word in ("优惠", "优惠券", "折扣", "满减")):
        return IntentResult(name="promotion_query", safe=True)
    if any(word in text for word in ("推荐", "商铺", "店铺", "晚饭", "吃饭")):
        return IntentResult(name="shop_recommendation", safe=True)
    if any(word in text for word in ("宿舍", "寝室", "床位")):
        return IntentResult(name="dorm_query", safe=True)
    if "订单" in text:
        return IntentResult(name="order_query", safe=True)
    if any(word in text for word in ("能做什么", "可以帮我做什么", "你可以做什么", "你会什么", "怎么用", "帮助", "功能", "help")):
        return IntentResult(name="help", safe=True)
    if len(message.strip()) <= 2:
        return IntentResult(name="clarification", safe=True)
    return IntentResult(name="general_chat", safe=True)
