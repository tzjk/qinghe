from app.core.errors import AgentError


def ensure_fallback_allowed(*, production: bool, enabled: bool) -> None:
    if production and enabled:
        raise AgentError("AGENT_PROVIDER_FALLBACK_BLOCKED", "生产环境未启用静默模型降级。", False, 503)
