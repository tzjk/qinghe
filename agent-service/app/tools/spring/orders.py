from typing import Any, ClassVar

from pydantic import BaseModel

from app.clients.endpoint_registry import EndpointName
from app.tools.spring.base import SpringReadTool, SpringToolOutput
from app.tools.spring.models import OrderDetailInput, RecentOrdersInput


class GetMyRecentOrdersTool(SpringReadTool):
    name: ClassVar[str] = "get_my_recent_orders"
    description: ClassVar[str] = "查询当前登录用户最近订单，不接收身份参数。"
    input_model: ClassVar[type[BaseModel]] = RecentOrdersInput
    output_model: ClassVar[type[BaseModel]] = SpringToolOutput
    requires_auth: ClassVar[bool] = True
    endpoint = EndpointName.ORDERS

    def adapt_data(self, data: Any) -> Any:
        return _order_page(data)


class GetMyOrderDetailTool(SpringReadTool):
    name: ClassVar[str] = "get_my_order_detail"
    description: ClassVar[str] = "按订单 ID 查询当前登录用户订单详情。"
    input_model: ClassVar[type[BaseModel]] = OrderDetailInput
    output_model: ClassVar[type[BaseModel]] = SpringToolOutput
    requires_auth: ClassVar[bool] = True
    endpoint = EndpointName.ORDER_DETAIL

    def request_values(self, payload: OrderDetailInput):
        return {}, {"orderId": payload.order_id}

    def adapt_data(self, data: Any) -> Any:
        return _order_summary(data)


def _order_page(data: Any) -> Any:
    if data is None:
        return None
    if not isinstance(data, dict):
        return {"records": [], "total": 0, "page": 1, "size": 0}
    records = data.get("records") if isinstance(data.get("records"), list) else []
    return {"records": [_order_summary(item) for item in records], "total": data.get("total", len(records)), "page": data.get("page", 1), "size": data.get("size", len(records))}


def _order_summary(data: Any) -> Any:
    if not isinstance(data, dict):
        return None
    return {key: data.get(key) for key in ("orderNo", "shopName", "totalAmount", "payAmount", "status", "statusName", "createTime") if key in data}
