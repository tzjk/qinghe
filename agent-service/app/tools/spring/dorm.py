from typing import Any, ClassVar

from pydantic import BaseModel

from app.clients.endpoint_registry import EndpointName
from app.tools.spring.base import SpringReadTool, SpringToolOutput
from app.tools.spring.models import EmptyToolInput


class GetMyDormInfoTool(SpringReadTool):
    name: ClassVar[str] = "get_my_dorm_info"
    description: ClassVar[str] = "查询当前登录用户的宿舍信息，不接收身份或学生参数。"
    input_model: ClassVar[type[BaseModel]] = EmptyToolInput
    output_model: ClassVar[type[BaseModel]] = SpringToolOutput
    requires_auth: ClassVar[bool] = True
    endpoint = EndpointName.STUDENT_DORM_ME

    def adapt_data(self, data: Any) -> Any:
        if not isinstance(data, dict):
            return None
        aliases = {"campusName": "campus", "buildingName": "building", "roomNo": "room", "bedNo": "bed"}
        return {
            output: data.get(source, data.get(output))
            for source, output in aliases.items()
            if source in data or output in data
        } | ({"checkInStatus": data["checkInStatus"]} if "checkInStatus" in data else {})
