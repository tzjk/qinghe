from dataclasses import dataclass
from decimal import Decimal


@dataclass(frozen=True, slots=True)
class ModelProfile:
    profile_name: str
    provider: str
    model: str
    supports_tools: bool
    supports_streaming: bool
    context_limit: int
    max_output_tokens: int
    cost_tier: str
    input_cost_per_million: Decimal = Decimal("0")
    output_cost_per_million: Decimal = Decimal("0")
    cached_input_cost_per_million: Decimal = Decimal("0")
    enabled: bool = True
