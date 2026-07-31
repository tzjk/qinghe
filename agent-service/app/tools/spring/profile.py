from typing import Any, ClassVar

from pydantic import BaseModel

from app.clients.endpoint_registry import EndpointName
from app.tools.spring.base import SpringReadTool, SpringToolOutput
from app.tools.spring.models import EmptyToolInput


class GetMyProfileTool(SpringReadTool):
    name: ClassVar[str] = "get_my_profile"
    description: ClassVar[str] = "查询当前登录用户资料，不接收身份参数。"
    input_model: ClassVar[type[BaseModel]] = EmptyToolInput
    output_model: ClassVar[type[BaseModel]] = SpringToolOutput
    requires_auth: ClassVar[bool] = True
    endpoint = EndpointName.USER_ME

    def adapt_data(self, data: Any) -> Any:
        return _pick(data, ("username", "nickname", "avatarUrl", "phoneMasked", "profileCompleted", "hasStudentProfile"))


class GetMyStudentProfileTool(SpringReadTool):
    name: ClassVar[str] = "get_my_student_profile"
    description: ClassVar[str] = "查询当前登录用户的学生档案，不接收身份参数。"
    input_model: ClassVar[type[BaseModel]] = EmptyToolInput
    output_model: ClassVar[type[BaseModel]] = SpringToolOutput
    requires_auth: ClassVar[bool] = True
    endpoint = EndpointName.STUDENT_PROFILE

    def adapt_data(self, data: Any) -> Any:
        return _pick(data, ("campusName", "collegeName", "majorName", "className", "studentStatus"))


def _pick(data: Any, fields: tuple[str, ...]) -> Any:
    if not isinstance(data, dict):
        return None
    return {key: data.get(key) for key in fields if key in data}
