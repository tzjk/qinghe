import json
from pathlib import Path

from app.agent.intent import classify_intent
from app.core.config import Settings
from app.providers.deterministic_tool_calling import DeterministicToolCallingProvider
from app.tools.spring import create_spring_registry
from evals.metrics import rate
from tests.fakes.fake_qinghe_backend import FakeQingheBackend

ROOT = Path(__file__).resolve().parent

async def run() -> dict[str, object]:
    cases = json.loads((ROOT / "cases.json").read_text(encoding="utf-8"))
    fake_backend = FakeQingheBackend()
    settings = Settings(agent_env="test", agent_mock_mode=False, agent_tool_mode="spring", llm_provider="deterministic", qinghe_backend_enabled=True, qinghe_backend_base_url="http://offline.backend")
    registry, client = create_spring_registry(settings, transport=fake_backend.transport, data_source="spring_simulated")
    provider = DeterministicToolCallingProvider()
    scores = {key: 0 for key in ("intent_accuracy", "tool_selection_accuracy", "auth_gate_accuracy", "safety_block_accuracy", "unsupported_accuracy", "hallucination_free_rate", "max_tool_calls_compliance", "response_schema_valid_rate")}
    for case in cases:
        result = classify_intent(case["message"])
        calls = [] if not result.safe else await provider.select_tools(intent=result.name, message=case["message"], max_tools=2, tools=registry.metadata())
        scores["intent_accuracy"] += result.name == case["intent"]
        scores["tool_selection_accuracy"] += (not case.get("tool")) or bool(calls and calls[0].name == case["tool"])
        scores["safety_block_accuracy"] += bool(case.get("blocked")) == (not result.safe)
        scores["unsupported_accuracy"] += (case["intent"] != "unsupported") or (result.name == "unsupported")
        scores["max_tool_calls_compliance"] += len(calls) <= 2
        scores["hallucination_free_rate"] += all(call.name in {tool.name for tool in registry.metadata()} for call in calls)
        scores["response_schema_valid_rate"] += isinstance(calls, list)
        scores["auth_gate_accuracy"] += (not case.get("auth")) or bool(calls and registry.get(calls[0].name).requires_auth)
    await client.close()
    report = {"case_count": len(cases), "metrics": {key: rate(value, len(cases)) for key, value in scores.items()}, "offline": True, "contains_credentials": False}
    (ROOT / "latest-report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    return report

if __name__ == "__main__":
    import asyncio
    report = asyncio.run(run())
    print(f"Offline agent evaluation: {report['case_count']} cases")
    for key, value in report["metrics"].items(): print(f"- {key}: {value:.0%}")
    print(f"JSON report: {ROOT / 'latest-report.json'}")
