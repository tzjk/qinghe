from typing import Any, ClassVar

from pydantic import BaseModel

from app.schemas.tool import ToolResult
from app.tools.spring.base import SpringReadTool, SpringToolOutput
from app.tools.spring.models import EmptyToolInput
from app.clients.endpoint_registry import EndpointName


class GetMyDefaultAddressTool(SpringReadTool):
    name: ClassVar[str] = "get_my_default_address"
    description: ClassVar[str] = "从当前登录用户地址列表中选择默认地址。"
    input_model: ClassVar[type[BaseModel]] = EmptyToolInput
    output_model: ClassVar[type[BaseModel]] = SpringToolOutput
    requires_auth: ClassVar[bool] = True
    endpoint = EndpointName.ADDRESSES

    async def execute(self, payload: EmptyToolInput) -> ToolResult:
        result = await super().execute(payload)
        if not result.success:
            return result
        addresses = _safe_addresses(result.data)
        default = next(
            (item for item in addresses if isinstance(item, dict) and item.get("isDefault") in {True, 1, "1"}),
            None,
        )
        result.data = {"address": default, "found": default is not None}
        return result


class GetMyAddressesTool(SpringReadTool):
    name: ClassVar[str] = "get_my_addresses"
    description: ClassVar[str] = "查询当前登录用户的地址摘要，不接收身份参数。"
    input_model: ClassVar[type[BaseModel]] = EmptyToolInput
    output_model: ClassVar[type[BaseModel]] = SpringToolOutput
    requires_auth: ClassVar[bool] = True
    endpoint = EndpointName.ADDRESSES

    def adapt_data(self, data: Any) -> Any:
        return _safe_addresses(data)


def _safe_addresses(data: Any) -> list[dict[str, Any]]:
    if not isinstance(data, list):
        return []
    return [
        {key: item.get(key) for key in ("id", "campusName", "buildingName", "roomNo", "formattedAddress", "label", "isDefault") if key in item}
        for item in data if isinstance(item, dict)
    ]
