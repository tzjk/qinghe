def fallback_warning(profile: str) -> str:
    return f"模型 Provider 不可用，已使用 {profile} 安全降级；未编造实时业务数据。"
