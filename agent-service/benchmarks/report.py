import json
from pathlib import Path

from app.core.config import Settings
from app.tools.spring import create_spring_registry
from benchmarks import routing_benchmark, token_baseline, token_optimized
from tests.fakes.fake_qinghe_backend import FakeQingheBackend


ROOT = Path(__file__).resolve().parent


def percentile(values: list[int], ratio: float) -> int:
    return sorted(values)[max(0, min(len(values) - 1, int((len(values) - 1) * ratio)))]


async def run() -> dict[str, object]:
    cases = json.loads((ROOT.parent / "evals" / "cases.json").read_text(encoding="utf-8"))
    fake = FakeQingheBackend()
    settings = Settings(agent_env="test", agent_mock_mode=False, agent_tool_mode="spring", llm_provider="deterministic", qinghe_backend_enabled=True, qinghe_backend_base_url="http://offline.backend")
    registry, client = create_spring_registry(settings, transport=fake.transport)
    baseline = [token_baseline.estimate(case, registry.metadata()) for case in cases]
    optimized = [token_optimized.estimate(case, registry.metadata()) for case in cases]
    await client.close()
    avg = lambda rows, field: round(sum(row[field] for row in rows) / len(rows), 2)
    report = {"case_count": len(cases), "baseline": {"average_input_tokens": avg(baseline, "input_tokens"), "p50_input_tokens": percentile([row["input_tokens"] for row in baseline], .5), "p95_input_tokens": percentile([row["input_tokens"] for row in baseline], .95), "average_schema_tokens": avg(baseline, "schema_tokens"), "average_history_tokens": avg(baseline, "history_tokens"), "average_tool_result_tokens": avg(baseline, "result_tokens")}, "optimized": {"average_input_tokens": avg(optimized, "input_tokens"), "p50_input_tokens": percentile([row["input_tokens"] for row in optimized], .5), "p95_input_tokens": percentile([row["input_tokens"] for row in optimized], .95), "average_schema_tokens": avg(optimized, "schema_tokens"), "average_history_tokens": avg(optimized, "history_tokens"), "average_tool_result_tokens": avg(optimized, "result_tokens")}, "model_calls_avoided": sum(1 for case in cases if classify_no_call(case)), "routing_distribution": routing_benchmark.route(cases), "offline": True}
    report["estimated_token_reduction_ratio"] = round(1 - report["optimized"]["average_input_tokens"] / report["baseline"]["average_input_tokens"], 4)
    output = ROOT / "results"; output.mkdir(exist_ok=True)
    (output / "token_optimization_report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    (output / "token_optimization_report.md").write_text("# Token optimization benchmark\n\n" + "\n".join(f"- {key}: {value}" for key, value in report.items()), encoding="utf-8")
    return report


def classify_no_call(case: dict[str, object]) -> bool:
    return not bool(case.get("tool"))


if __name__ == "__main__":
    import asyncio
    print(json.dumps(asyncio.run(run()), ensure_ascii=False, indent=2))
