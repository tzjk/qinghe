from app.token_budget.models import TokenBudgetExceeded


def validate_context(*, input_tokens: int, reserved_output_tokens: int, context_limit: int, max_input_tokens: int) -> None:
    if input_tokens > max_input_tokens:
        raise TokenBudgetExceeded("请求上下文超过输入 Token 上限。", input_tokens=input_tokens, limit=max_input_tokens)
    if input_tokens + reserved_output_tokens > context_limit:
        raise TokenBudgetExceeded("请求上下文超过模型窗口。", input_tokens=input_tokens, limit=context_limit)
