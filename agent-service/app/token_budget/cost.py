from decimal import Decimal

from app.token_budget.models import TokenUsage


def estimate_cost(usage: TokenUsage, *, input_cost_per_million: Decimal, output_cost_per_million: Decimal, cached_input_cost_per_million: Decimal) -> Decimal:
    return (Decimal(usage.input_tokens - usage.cached_tokens) * input_cost_per_million + Decimal(usage.cached_tokens) * cached_input_cost_per_million + Decimal(usage.output_tokens) * output_cost_per_million) / Decimal(1_000_000)
