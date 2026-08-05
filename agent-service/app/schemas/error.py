from pydantic import BaseModel, ConfigDict


class ErrorDetail(BaseModel):
    model_config = ConfigDict(extra="forbid")

    code: str
    message: str
    retryable: bool


class ErrorResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    request_id: str
    error: ErrorDetail
