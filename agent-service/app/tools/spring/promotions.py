from datetime import datetime, timezone
from typing import Any, ClassVar

from pydantic import BaseModel

from app.schemas.tool import ToolResult
from app.tools.spring.coupons import GetActiveCouponsTool
from app.tools.spring.models import CouponsInput, EmptyToolInput


class GetTodayPromotionsTool(GetActiveCouponsTool):
    name: ClassVar[str] = "get_today_promotions"
    description: ClassVar[str] = "根据当前有效领取窗口、状态和库存查询可领取优惠券；不查询商品折扣。"
    input_model: ClassVar[type[BaseModel]] = EmptyToolInput

    async def execute(self, payload: EmptyToolInput) -> ToolResult:
        result = await super().execute(CouponsInput())
        if not result.success:
            result.tool_name = self.name
            return result
        result.tool_name = self.name
        result.data = self._filter_available_coupons(result.data)
        return result

    @staticmethod
    def _filter_available_coupons(data: Any) -> Any:
        if not isinstance(data, dict):
            return data
        records = data.get("records")
        if not isinstance(records, list):
            return data
        now = datetime.now(timezone.utc)
        filtered = [item for item in records if isinstance(item, dict) and GetTodayPromotionsTool._is_available(item, now)]
        return {**data, "records": filtered}

    @staticmethod
    def _is_available(item: dict[str, Any], now: datetime) -> bool:
        status = item.get("status")
        if isinstance(status, str) and status.upper() not in {"ENABLED", "ACTIVE", "1"}:
            return False
        stock = item.get("availableStock", item.get("stock"))
        if isinstance(stock, (int, float)) and stock <= 0:
            return False
        start = GetTodayPromotionsTool._parse_time(item.get("receiveStartTime") or item.get("receive_start_time"))
        end = GetTodayPromotionsTool._parse_time(item.get("receiveEndTime") or item.get("receive_end_time"))
        return (start is None or start <= now) and (end is None or now <= end)

    @staticmethod
    def _parse_time(value: Any) -> datetime | None:
        if not isinstance(value, str):
            return None
        try:
            parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
            return parsed if parsed.tzinfo else parsed.replace(tzinfo=timezone.utc)
        except ValueError:
            return None
