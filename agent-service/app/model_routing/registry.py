from app.core.config import Settings
from app.model_routing.profiles import ModelProfile


class ModelRegistry:
    def __init__(self, settings: Settings) -> None:
        self._profiles = {
            "deterministic": ModelProfile("deterministic", "deterministic", "local-rule", True, True, settings.agent_default_context_limit, settings.agent_max_output_tokens, "zero"),
            "fast": ModelProfile("fast", settings.llm_provider, settings.llm_model or "configured-fast", True, True, settings.agent_default_context_limit, settings.agent_max_output_tokens, "low", enabled=settings.agent_fast_profile_enabled),
            "standard": ModelProfile("standard", settings.llm_provider, settings.llm_model or "configured-standard", True, True, settings.agent_default_context_limit, settings.agent_max_output_tokens, "standard", enabled=settings.agent_standard_profile_enabled),
            "fallback": ModelProfile("fallback", "deterministic", "local-rule", True, True, settings.agent_default_context_limit, settings.agent_max_output_tokens, "zero"),
        }

    def get(self, name: str) -> ModelProfile:
        return self._profiles[name]

    def all(self) -> tuple[ModelProfile, ...]:
        return tuple(self._profiles.values())
