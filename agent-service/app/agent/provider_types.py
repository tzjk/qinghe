from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class ProviderToolCall:
    name: str
    arguments: dict[str, object]
