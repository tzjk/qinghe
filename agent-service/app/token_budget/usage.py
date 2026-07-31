from app.token_budget.models import TokenUsage


def provider_usage(value: object | None, *, input_tokens: int, output_tokens: int) -> TokenUsage:
    if isinstance(value, dict):
        prompt = value.get("prompt_tokens", value.get("input_tokens"))
        completion = value.get("completion_tokens", value.get("output_tokens"))
        cached = value.get("cached_tokens", 0)
        if all(isinstance(item, int) and item >= 0 for item in (prompt, completion, cached)):
            return TokenUsage(prompt, completion, cached, estimated=False)
    return TokenUsage(input_tokens, output_tokens, 0, estimated=True)
