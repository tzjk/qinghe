from app.agent.intent import classify_intent
from app.token_budget.estimator import SafeTokenEstimator
from app.token_budget.tool_schema_selector import ToolSchemaSelector


def estimate(case: dict[str, object], tools: list[object]) -> dict[str, int]:
    estimator = SafeTokenEstimator()
    intent = classify_intent(str(case["message"])).name
    selection = ToolSchemaSelector(4).select(intent=intent, tools=tools, authenticated=bool(case.get("auth")))
    history = [{"user": "安全摘要", "assistant": "安全结果摘要"} for _ in range(6)]
    schemas = [{"name": tool.name, "description": tool.description, "parameters": tool.input_schema} for tool in selection.tools]
    result = {"records": [{"orderNo": "SIM-001", "status": "DELIVERING"}]}
    return {"input_tokens": estimator.estimate_object([case["message"], history, schemas, result]).tokens, "schema_tokens": estimator.estimate_object(schemas).tokens, "history_tokens": estimator.estimate_object(history).tokens, "result_tokens": estimator.estimate_object(result).tokens}
