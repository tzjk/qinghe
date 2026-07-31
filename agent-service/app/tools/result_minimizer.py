"""Final provider-context minimization; tool adapters never bypass this boundary."""

from typing import Any

from app.schemas.tool import ToolResult

_FORBIDDEN_KEYS = {
    "token", "authorization", "password", "verifycode", "verificationcode", "idcard",
    "phone", "phoneNumber", "detailaddress", "redis", "sql", "class",
    "backendrequestid", "backend_code", "coverimage", "avatarurl", "content",
}


def minimize_for_provider(result: ToolResult) -> dict[str, Any]:
    """Expose the smallest answer-relevant result, never transport/internal metadata."""
    payload: dict[str, Any] = {
        "success": result.success,
        "tool_name": result.tool_name,
        "data_source": result.data_source,
    }
    if result.success:
        payload["data"] = _clean(result.data)
    else:
        payload["error_code"] = result.error_code
        payload["user_message"] = result.user_message
    return payload


def _clean(value: Any) -> Any:
    if isinstance(value, list):
        return [_clean(item) for item in value]
    if not isinstance(value, dict):
        return value
    return {
        key: _clean(item)
        for key, item in value.items()
        if key.replace("_", "").lower() not in {item.replace("_", "").lower() for item in _FORBIDDEN_KEYS}
    }
