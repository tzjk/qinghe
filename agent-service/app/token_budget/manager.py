from app.core.config import Settings
from app.token_budget.context_window import validate_context
from app.token_budget.estimator import SafeTokenEstimator
from app.token_budget.models import ContextLayers, TokenBudgetPlan, TokenUsage


class TokenBudgetManager:
    def __init__(self, settings: Settings, estimator: SafeTokenEstimator | None = None) -> None:
        self._settings = settings
        self._estimator = estimator or SafeTokenEstimator()
        self._conversation_totals: dict[str, int] = {}

    def plan(self, layers: ContextLayers, *, context_limit: int, max_output_tokens: int) -> TokenBudgetPlan:
        system = self._estimator.estimate_text(layers.system_prompt + "\n" + layers.safety_rules).tokens
        history = self._estimator.estimate_object([layers.conversation_summary, *layers.recent_turns, layers.user_message]).tokens
        schemas = self._estimator.estimate_object(layers.tool_definitions).tokens if layers.tool_definitions else 0
        results = self._estimator.estimate_object(layers.tool_results).tokens if layers.tool_results else 0
        input_tokens = system + history + schemas + results
        reserve = min(max_output_tokens, self._settings.agent_max_output_tokens)
        if self._settings.agent_token_budget_enabled:
            validate_context(input_tokens=input_tokens, reserved_output_tokens=reserve, context_limit=context_limit, max_input_tokens=self._settings.agent_max_input_tokens)
        return TokenBudgetPlan(input_tokens, reserve, context_limit, system, history, schemas, results, ("tokenizer_estimate",))

    def record(self, conversation_id: str, usage: TokenUsage) -> None:
        total = self._conversation_totals.get(conversation_id, 0) + usage.total_tokens
        if total > self._settings.agent_conversation_max_tokens:
            # Do not retain an unbounded total: the current request already completed, next request is rejected.
            total = self._settings.agent_conversation_max_tokens
        self._conversation_totals[conversation_id] = total

    def ensure_conversation_available(self, conversation_id: str) -> None:
        if self._conversation_totals.get(conversation_id, 0) >= self._settings.agent_conversation_max_tokens:
            from app.token_budget.models import TokenBudgetExceeded
            raise TokenBudgetExceeded("会话累计 Token 预算已用尽，请清除会话后继续。", input_tokens=self._conversation_totals[conversation_id], limit=self._settings.agent_conversation_max_tokens)

    def clear(self, conversation_id: str) -> None:
        self._conversation_totals.pop(conversation_id, None)
