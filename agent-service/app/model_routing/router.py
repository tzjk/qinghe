import time

from app.model_routing.decisions import ModelRoutingDecision
from app.model_routing.registry import ModelRegistry


class ModelRouter:
    def __init__(self, registry: ModelRegistry) -> None:
        self._registry = registry

    def decide(self, *, intent: str, expected_tool_count: int, estimated_input_tokens: int, provider_open: bool = False) -> ModelRoutingDecision:
        started = time.perf_counter()
        if provider_open:
            profile, reason, latency = "fallback", "primary_provider_circuit_open", "fast"
        elif intent in {"greeting", "help", "unsupported", "discount_query"}:
            profile, reason, latency = "deterministic", "safe_or_simple_intent", "instant"
        elif expected_tool_count >= 2 or intent in {"order_detail", "shop_recommendation"}:
            profile, reason, latency = "standard", "multi_intent_or_dual_tool", "standard"
        else:
            profile, reason, latency = "fast", "single_public_or_single_tool", "fast"
        selected = self._registry.get(profile)
        if not selected.enabled and profile in {"fast", "standard"}:
            profile, reason, latency = "deterministic", reason + ";profile_disabled", "instant"
        return ModelRoutingDecision(profile, reason, intent, expected_tool_count, estimated_input_tokens, latency, profile != "deterministic", int((time.perf_counter() - started) * 1000))
