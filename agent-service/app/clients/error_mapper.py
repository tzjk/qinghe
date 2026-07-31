from app.core.errors import AgentError


def map_backend_error(*, status_code: int | None, backend_code: int | str | None = None) -> AgentError:
    if status_code == 401 or backend_code == 401:
        return AgentError("AGENT_AUTH_EXPIRED", "登录状态已失效，请重新登录。", False, 401)
    if status_code == 403 or backend_code == 403:
        return AgentError("AGENT_FORBIDDEN", "你没有权限查询该信息。", False, 403)
    if status_code == 404 or backend_code == 404:
        return AgentError("AGENT_NOT_FOUND", "未查询到相关信息。", False, 404)
    if status_code == 409 or backend_code == 409:
        return AgentError("AGENT_BACKEND_CONFLICT", "当前查询无法完成，请稍后重试。", False, 409)
    if status_code == 429 or backend_code == 429:
        return AgentError("AGENT_BACKEND_RATE_LIMITED", "服务繁忙，请稍后再试。", True, 429)
    if status_code is not None and status_code >= 500:
        return AgentError("AGENT_BACKEND_UNAVAILABLE", "后端服务暂时不可用，请稍后重试。", True, 503)
    if status_code == 400 or backend_code == 400:
        return AgentError("AGENT_BACKEND_VALIDATION_ERROR", "查询参数不符合要求。", False, 400)
    return AgentError("AGENT_BACKEND_ERROR", "后端查询失败，请稍后重试。", False, 502)


def map_transport_error(error_type: str) -> AgentError:
    if error_type == "timeout":
        return AgentError("AGENT_BACKEND_TIMEOUT", "后端响应超时，请稍后重试。", True, 504)
    return AgentError("AGENT_BACKEND_UNAVAILABLE", "后端服务暂时不可用，请稍后重试。", True, 503)
