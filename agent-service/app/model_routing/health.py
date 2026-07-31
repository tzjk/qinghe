from dataclasses import dataclass
from enum import Enum


class CircuitState(str, Enum):
    CLOSED = "closed"
    OPEN = "open"
    HALF_OPEN = "half_open"


@dataclass(slots=True)
class ProviderHealth:
    state: CircuitState = CircuitState.CLOSED
    failures: int = 0
    opened_at: float | None = None
    half_open_calls: int = 0
