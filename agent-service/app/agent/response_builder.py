from dataclasses import dataclass

from app.schemas.tool import ToolCallTrace


@dataclass(frozen=True, slots=True)
class AgentResult:
    answer: str
    intent: str
    tool_calls: list[ToolCallTrace]
    mock: bool
    warnings: list[str]
    shop_references: tuple[tuple[int, str], ...] = ()
    tool_duration_ms: int = 0
    java_http_duration_ms: int = 0
    provider_duration_ms: int = 0
    first_output_ms: int | None = None
