from dataclasses import dataclass, field


@dataclass(frozen=True, slots=True)
class TokenUsage:
    input_tokens: int = 0
    output_tokens: int = 0
    cached_tokens: int = 0
    estimated: bool = True

    @property
    def total_tokens(self) -> int:
        return self.input_tokens + self.output_tokens


@dataclass(frozen=True, slots=True)
class TokenBudgetPlan:
    input_tokens: int
    reserved_output_tokens: int
    context_limit: int
    system_tokens: int
    history_tokens: int
    tool_schema_tokens: int
    tool_result_tokens: int
    warnings: tuple[str, ...] = ()


class TokenBudgetExceeded(Exception):
    def __init__(self, message: str, *, input_tokens: int, limit: int) -> None:
        super().__init__(message)
        self.input_tokens = input_tokens
        self.limit = limit


@dataclass(frozen=True, slots=True)
class ContextLayers:
    system_prompt: str
    safety_rules: str
    conversation_summary: str
    recent_turns: tuple[dict[str, str], ...]
    user_message: str
    tool_definitions: tuple[dict[str, object], ...] = field(default_factory=tuple)
    tool_results: tuple[dict[str, object], ...] = field(default_factory=tuple)
