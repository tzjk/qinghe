from dataclasses import dataclass


@dataclass(slots=True)
class AgentError(Exception):
    code: str
    message: str
    retryable: bool = False
    status_code: int = 400


PROMPT_INJECTION = AgentError(
    code="AGENT_PROMPT_INJECTION_BLOCKED",
    message="该请求包含不安全指令，无法处理。",
    retryable=False,
    status_code=400,
)

AUTH_REQUIRED = AgentError(
    code="AGENT_AUTH_REQUIRED",
    message="请先登录后再查询个人信息。",
    retryable=False,
    status_code=401,
)
