from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator


class ToolInput(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)


class EmptyToolInput(ToolInput):
    pass


class PageInput(ToolInput):
    page: int = Field(default=1, ge=1, le=10_000)
    size: int = Field(default=10, ge=1, le=100)


class NearbyShopsInput(ToolInput):
    longitude: float = Field(ge=-180, le=180)
    latitude: float = Field(ge=-90, le=90)
    radius: float = Field(default=5, ge=0.1, le=20)
    category_id: int | None = Field(default=None, gt=0)
    page: int = Field(default=1, ge=1, le=10_000)
    size: int = Field(default=10, ge=1, le=50)


class ShopSearchInput(PageInput):
    category_id: int | None = Field(default=None, gt=0)
    keyword: str | None = Field(default=None, min_length=1, max_length=80)
    sort: str | None = Field(default=None, min_length=1, max_length=20)

    @field_validator("keyword", "sort")
    @classmethod
    def reject_unsafe_text(cls, value: str | None) -> str | None:
        if value is None:
            return value
        if any(fragment in value for fragment in ("\r", "\n", "..", "/", "\\", "http://", "https://", "select ", "insert ", "update ", "delete ")):
            raise ValueError("查询文本包含不允许的内容")
        return value


class ShopGoodsInput(PageInput):
    id: int = Field(gt=0)
    category_id: int | None = Field(default=None, gt=0)
    keyword: str | None = Field(default=None, min_length=1, max_length=80)

    @field_validator("keyword")
    @classmethod
    def reject_unsafe_keyword(cls, value: str | None) -> str | None:
        if value is None:
            return value
        if any(fragment in value for fragment in ("\r", "\n", "..", "/", "\\", "http://", "https://", "select ", "insert ", "update ", "delete ")):
            raise ValueError("查询文本包含不允许的内容")
        return value


class PositiveIdInput(ToolInput):
    id: int = Field(gt=0)


class OrderDetailInput(ToolInput):
    order_id: int = Field(gt=0)


class RecentOrdersInput(PageInput):
    status: str | None = Field(default=None, min_length=1, max_length=20)

    @field_validator("status")
    @classmethod
    def validate_status(cls, value: str | None) -> str | None:
        if value is not None and any(fragment in value for fragment in ("\r", "\n", "..", "/", "\\", "select ")):
            raise ValueError("订单状态包含不允许的内容")
        return value


class ExplorePostsInput(ToolInput):
    page: int = Field(default=1, ge=1, le=10_000)
    size: int = Field(default=10, ge=1, le=50)
    sort: Literal["hot"] = "hot"


class CouponsInput(PageInput):
    status: str | None = Field(default=None, min_length=1, max_length=20)
