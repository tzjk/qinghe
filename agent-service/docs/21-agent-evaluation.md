# Agent evaluation

Run `python -m evals.runner` from `agent-service`. The runner uses the deterministic provider and in-memory Fake Backend transport only. It loads 50 cases covering public queries, personal-query gates, prompt injection, admin/Shell/SQL requests, missing discount data, ambiguity, tool limits, and unsupported questions.

It prints a readable summary and writes `evals/latest-report.json`. Reports contain aggregate metrics only: intent/tool selection, auth gate, safety block, unsupported, hallucination-free, tool-call limit, and response schema rates. They contain no token, key, address detail, order detail, or raw model payload.
