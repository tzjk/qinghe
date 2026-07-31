from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class ModelRoutingDecision:
    selected_profile: str
    route_reason: str
    intent: str
    expected_tool_count: int
    estimated_input_tokens: int
    latency_tier: str
    fallback_allowed: bool
    decision_duration_ms: int
