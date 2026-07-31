from typing import Any, ClassVar

from pydantic import BaseModel

from app.clients.endpoint_registry import EndpointName
from app.tools.spring.base import SpringReadTool, SpringToolOutput
from app.tools.spring.models import CouponsInput


class GetActiveCouponsTool(SpringReadTool):
    name: ClassVar[str] = "get_active_coupons"
    description: ClassVar[str] = "查询当前可领取的公开优惠券。"
    input_model: ClassVar[type[BaseModel]] = CouponsInput
    output_model: ClassVar[type[BaseModel]] = SpringToolOutput
    requires_auth: ClassVar[bool] = False
    endpoint = EndpointName.COUPONS

    def adapt_data(self, data: Any) -> Any:
        return _coupon_page(data, mine=False)


class GetMyCouponWalletTool(SpringReadTool):
    name: ClassVar[str] = "get_my_coupon_wallet"
    description: ClassVar[str] = "查询当前登录用户的优惠券，不接收身份参数。"
    input_model: ClassVar[type[BaseModel]] = CouponsInput
    output_model: ClassVar[type[BaseModel]] = SpringToolOutput
    requires_auth: ClassVar[bool] = True
    endpoint = EndpointName.COUPONS_MINE

    def adapt_data(self, data: Any) -> Any:
        return _coupon_page(data, mine=True)


def _coupon_page(data: Any, *, mine: bool) -> Any:
    if data is None:
        return None
    if not isinstance(data, dict):
        return {"records": [], "total": 0, "page": 1, "size": 0}
    records = data.get("records")
    if not isinstance(records, list):
        records = []
    fields = (
        "name", "couponType", "discountAmount", "discountRate", "thresholdAmount",
        "useStartTime", "useEndTime", "status",
    )
    if mine:
        fields += ("expireTime",)
    else:
        fields += ("availableStock", "receiveStartTime", "receiveEndTime")
    return {
        "records": [{key: item.get(key) for key in fields if key in item} for item in records if isinstance(item, dict)],
        "total": data.get("total", len(records)),
        "page": data.get("page", 1),
        "size": data.get("size", len(records)),
    }
