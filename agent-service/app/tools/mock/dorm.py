from typing import ClassVar

from pydantic import BaseModel, ConfigDict

from app.tools.base import BaseTool, EmptyInput


class DormOutput(BaseModel):
    model_config = ConfigDict(extra="forbid")

    data_source: str
    resident_name: str
    campus: str
    building: str
    room: str
    bed: str


class MyDormTool(BaseTool):
    name: ClassVar[str] = "get_my_dorm_info"
    description: ClassVar[str] = "返回虚构的当前用户宿舍信息，不接收身份参数。"
    input_model: ClassVar[type[BaseModel]] = EmptyInput
    output_model: ClassVar[type[BaseModel]] = DormOutput
    requires_auth: ClassVar[bool] = True

    async def execute(self, payload: EmptyInput) -> dict[str, object]:
        return DormOutput(
            data_source="mock",
            resident_name="林小禾（虚构）",
            campus="青禾示范校区",
            building="知行楼",
            room="302",
            bed="2",
        ).model_dump()
