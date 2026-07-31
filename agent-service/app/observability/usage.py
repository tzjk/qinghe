from dataclasses import dataclass
from datetime import datetime, timezone
from decimal import Decimal


@dataclass(frozen=True, slots=True)
class ModelUsageRecord:
    request_id: str
    conversation_id: str
    provider: str
    model_profile: str
    model: str
    input_tokens: int
    output_tokens: int
    cached_tokens: int
    total_tokens: int
    estimated_cost: Decimal
    estimated: bool
    tool_count: int
    fallback_used: bool
    duration_ms: int
    timestamp: datetime

    @classmethod
    def now(cls, **values: object) -> "ModelUsageRecord":
        return cls(timestamp=datetime.now(timezone.utc), **values)  # type: ignore[arg-type]
