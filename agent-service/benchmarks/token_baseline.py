from app.token_budget.estimator import SafeTokenEstimator


def estimate(case: dict[str, object], tools: list[object]) -> dict[str, int]:
    estimator = SafeTokenEstimator()
    history = [{"user": "历史校园咨询内容 " * 12, "assistant": "历史回答摘要 " * 12} for _ in range(10)]
    schemas = [{"name": getattr(tool, "name"), "description": getattr(tool, "description"), "parameters": getattr(tool, "input_schema")} for tool in tools]
    result = {"records": [{"orderNo": "SIM-001", "detailAddress": "sensitive-not-retained", "content": "raw tool output " * 30} for _ in range(3)]}
    return {"input_tokens": estimator.estimate_object([case["message"], history, schemas, result]).tokens, "schema_tokens": estimator.estimate_object(schemas).tokens, "history_tokens": estimator.estimate_object(history).tokens, "result_tokens": estimator.estimate_object(result).tokens}
