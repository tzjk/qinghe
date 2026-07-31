from collections import Counter
from decimal import Decimal

from app.token_budget.models import TokenUsage


class InMemoryMetrics:
    """仅用于 Phase 1 进程内计数；不写 Redis 或数据库。"""

    def __init__(self) -> None:
        self._intents: Counter[str] = Counter()
        self._tool_calls = 0
        self._requests = 0
        self._profiles: Counter[str] = Counter()
        self._fallbacks = 0
        self._cache_hits = 0
        self._tokens = 0
        self._cost = Decimal("0")

    def record(self, *, intent: str, tool_count: int) -> None:
        self._intents[intent] += 1
        self._tool_calls += tool_count

    def record_usage(self, *, profile: str, usage: TokenUsage, cost: Decimal, fallback_used: bool, cache_hit: bool) -> None:
        self._requests += 1
        self._profiles[profile] += 1
        self._fallbacks += int(fallback_used)
        self._cache_hits += int(cache_hit)
        self._tokens += usage.total_tokens
        self._cost += cost

    def snapshot(self) -> dict[str, object]:
        return {"intents": dict(self._intents), "tool_calls": self._tool_calls}

    def usage_summary(self) -> dict[str, object]:
        return {"request_count": self._requests, "total_tokens": self._tokens, "average_tokens": round(self._tokens / self._requests, 2) if self._requests else 0, "model_profiles": dict(self._profiles), "fallback_count": self._fallbacks, "cache_hits": self._cache_hits, "tool_calls": self._tool_calls, "estimated_cost": str(self._cost)}
