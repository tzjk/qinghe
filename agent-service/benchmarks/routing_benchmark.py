from collections import Counter

from app.agent.intent import classify_intent
from app.core.config import Settings
from app.model_routing.registry import ModelRegistry
from app.model_routing.router import ModelRouter


def route(cases: list[dict[str, object]]) -> dict[str, int]:
    router = ModelRouter(ModelRegistry(Settings(agent_env="test")))
    result: Counter[str] = Counter()
    for case in cases:
        intent = classify_intent(str(case["message"])).name
        result[router.decide(intent=intent, expected_tool_count=1 if case.get("tool") else 0, estimated_input_tokens=100).selected_profile] += 1
    return dict(result)
