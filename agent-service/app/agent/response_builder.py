from dataclasses import dataclass

from app.schemas.tool import ToolCallTrace


@dataclass(frozen=True, slots=True)
class AgentResult:
    answer: str
    intent: str
    tool_calls: list[ToolCallTrace]
    mock: bool
    warnings: list[str]
