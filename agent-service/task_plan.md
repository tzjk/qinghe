# Phase 4.5 - Python Runtime Isolation and Controlled Real Spring Boot Read-Only Integration

## Goal

With the user manually operating Spring Boot, isolate the Agent's Python child process from Anaconda contamination and verify the isolated FastAPI Agent's read-only path using a deterministic local provider and the real Qinghe Spring Boot API at `127.0.0.1:8090`. The Agent must remain isolated from direct MySQL/Redis access, preserve offline defaults and the 41-case offline suite, and never require or call a real-model key.

## Scope boundaries

- All writes remain under `agent-service/`; `backend/`, `frontend/`, root business files, database/Redis data, SQL, authentication logic, Spring controllers, and Vue pages are read-only.
- Only allow-listed `GET` Spring API calls are permitted. No admin route, administrator token, business write request, SQL, or real model configuration/call is allowed.
- The user controls Spring Boot, MySQL, Redis, and Vue. The Agent may start FastAPI temporarily on `127.0.0.1:8100` and must stop it after validation; it must not start or stop any Qinghe dependency.
- Environment repair is process-local only: no Anaconda installation, system/user environment variable, PowerShell profile, registry, or global-package change. A fresh `.venv-clean` is permitted only if the current `.venv` is demonstrably contaminated and a non-Anaconda CPython is available.
- Public verification precedes authenticated personal verification. Personal verification needs a user-supplied ordinary Bearer token passed only in the live request header; it must never be persisted, logged, shown, or put in test data.
- Default configuration remains fully offline. Spring integration requires explicit enablement.

## Phases

1. **Restore context and define Phase 4 plan** — `complete`
2. **Re-audit actual Spring GET contracts and Agent compatibility** — `complete`
3. **Implement minimal deterministic-provider plus Spring-tool configuration support** — `complete`
4. **Preserve offline regression and create private-config guidance** — `complete`
5. **Diagnose interpreter, ctypes, Conda variables, PATH, and Uvicorn command resolution** — `complete`
6. **Identify a non-Anaconda CPython eligible for a clean local virtual environment** — `complete`
7. **Implement the smallest process-local launcher/test helper and update local documentation** — `complete`
8. **Run explicit-interpreter offline regression without real-network access** — `complete`
9. **Run the double-gated public real-backend tests and record exact counts** — `complete`
10. **FastAPI health, deterministic Spring chat, SSE, errors, isolation, and process cleanup** — `complete`
11. **Authenticated personal-tool verification, only after user supplies a normal token** — `deferred: awaiting user-provided ordinary token`

## Required verification

- Re-run the existing offline pytest and compile checks after code changes.
- First test `127.0.0.1:8090` connectivity, then use an already-confirmed public GET endpoint because this backend does not expose Actuator health. If unavailable, stop real integration without modifying the Qinghe system.
- Use the Agent virtual-environment interpreter explicitly for all Python checks. Never use bare `uvicorn` for the repaired launch path.
- Verify the FastAPI public chain for promotions, coupons, shop search/detail/goods, nearby shops, and hot explore posts. Verify real socket SSE event order and sanitized output. The only personal checks without a normal Token are safe rejection/mapping and no-leak behavior.
- Record direct-public, FastAPI-public, personal-token, error, empty-data, SSE, interpreter/isolation, and process-cleanup evidence truthfully.

## Phase 4.5 acceptance conditions

- Default pytest excludes all `real_backend` cases. They can access `127.0.0.1:8090` only when both `-m real_backend` and `QINGHE_REAL_BACKEND_TESTS=true` are supplied.
- A normal user token is optional and only arrives through a live `Authorization` header or the transient `QINGHE_TEST_USER_TOKEN` environment variable. Missing-token personal checks must skip or return the documented rejection; they must never invent credentials.
- Phase 4.5 public acceptance is complete: all offline checks, seven public FastAPI chains, real SSE, auth rejection, Result parsing, isolated Python startup, and process cleanup have evidence. Personal-tool success remains deferred until the user supplies an ordinary token.

## Errors encountered

| Error | Attempt | Resolution |
|---|---:|---|
| First planning-skill catch-up invocation used the Agent working directory for a project-root script path | 1 | Re-resolved the script from the project root. |
| Second catch-up invocation accessed `PathInfo.Parent` incorrectly in PowerShell | 2 | Used `Split-Path -Parent (Get-Location).Path`; the catch-up completed with no unsynced-context output. |
| Offline regression retained the old 12-tool readiness assertion after two audited personal read tools were added | 1 | Updated the exact readiness contract to 14 rather than weakening it. |
| Uvicorn socket startup could not import Click because this local runtime resolved Anaconda `ctypes` and `_ctypes` DLL access was denied | 1 | Recorded the external runtime blocker; no service/configuration/dependency change was attempted. |
| PowerShell `Get-ChildItem Env:CONDA*` threw a duplicate-key exception while enumerating environment variables | 1 | Treat the failed enumeration as inconclusive; query only the named requested variables next. |
| Parent process could not execute the discovered independent CPython under the sandbox | 1 | Requested a one-time elevated read-only verification; it successfully proved the interpreter's own `ctypes` imports. |
| A combined code/document patch used Markdown inline-code markers for fenced PowerShell command lines and could not match README content | 1 | Split the patch by concern and matched the literal fenced-code lines; the Agent-only edits then applied. |
| The sandbox could not execute `.venv-clean` because its venv launcher points to the verified local CPython outside the workspace | 1 | Requested scoped permission for the approved checks; the clean interpreter ran successfully. |
| Background PowerShell launcher did not expose FastAPI health on 8100 within 15 seconds | 1 | No chat or SSE request was sent; inspect only the launcher/port state, then switch to a foreground controlled launch if absent. |
| Agent launcher scripts were saved as UTF-8 without BOM; the locally invoked Windows PowerShell parsed their quoted strings incorrectly despite the visible source being valid | 2 | Normalize only the two Agent-local PowerShell helper encodings to UTF-8 with BOM, then retry the foreground launcher once. |
| Stopping the foreground PowerShell launcher did not stop its Uvicorn child; port 8100 remained owned by a single process | 1 | Verify the exact port owner, stop only that temporary FastAPI process as authorized, and confirm the port is free before restart. |

# Phase 5–6 - Provider Productionization, Safety Evaluation, and Vue Integration

## Goal

Complete one integrated, read-only milestone for the Qinghe Campus Assistant: productionize the dormant OpenAI-compatible provider without making a real model request, harden orchestration/session/rate-limit handling, establish an offline professional evaluation suite, and add a weakly coupled Vue 3 user-side assistant page using POST SSE. Default runtime remains fully offline Mock mode; deterministic plus Spring tooling remains supported without a model key.

## Non-negotiable boundaries

- Writes are limited to `agent-service/`, `frontend/src/`, `frontend/.env.example`, and directly related frontend configuration/tests. `backend/`, database scripts/data, Redis, OSS, and project Git configuration are not modified.
- Do not start or stop Spring Boot, Vue, MySQL, Redis, or any real model service. Do not access public internet, real models, 8090, or 5174 during the default Phase 5–6 test run.
- Tools remain fixed, allow-listed, and read-only. No administrator route/token, direct database/Redis access, write endpoint, purchase, coupon claim, address/dorm/profile mutation, shell/SQL/code execution, Git command, commit, push, or pull request is allowed.
- Tokens are accepted only from an incoming ordinary-user `Authorization` header and are never logged, stored, given to a provider, placed in URLs, or persisted in frontend storage. No real model key is created or used.

## Phases

1. **Restore context, scope, and current contracts** — `completed`
2. **Plan Phase 5–6 implementation and update Agent records** — `completed`
3. **Harden configuration, OpenAI-compatible protocol adapter, and provider tests with local fake transport** — `completed`
4. **Harden orchestrator safety, minimum tool results, conversations, rate limits, and SSE resource cleanup** — `completed`
5. **Create the 50+ offline evaluation suite and report runner** — `completed`
6. **Integrate the Vue user-side assistant route, POST SSE client, session lifecycle, and graceful degradation** — `completed`
7. **Write Phase 5–6 configuration, integration, evaluation, and manual-acceptance documentation** — `completed`
8. **Run offline Agent compilation/tests/evals and the frontend production build; truthfully record results** — `completed`
9. **Update Agent progress/findings and stop for user review** — `completed`

## Acceptance conditions

- The three declared runtime combinations validate unambiguously at startup; only `openai_compatible` requires a non-empty local model configuration, and provider construction is network-silent.
- OpenAI-compatible protocol behavior is verified only through HTTPX mock transports or an in-process fake ASGI app, including text, tools, streaming, failures, malformed responses, cancellation, and no-token forwarding.
- Orchestration never runs more than two fixed read-only tools per turn, blocks unsafe/unsupported requests before tools, minimizes tool data before provider context, and returns safe structured errors.
- In-memory session and process-local limiter behavior have bounded, deterministic tests and never require Redis.
- `python -m evals.runner` executes at least 50 non-network cases and reports the required safety metrics without credentials or personal data.
- The Vue `/assistant` page uses `fetch` plus `ReadableStream`, `TextDecoder`, and `AbortController`; it sends only the ordinary user token in the Authorization header, does not render model HTML, and fails independently from the rest of Qinghe.
- Required Agent offline tests and frontend production build are run after implementation. Any unavailable verification is reported as incomplete, never as passing.

## Errors encountered

| Error | Attempt | Resolution |
|---|---:|---|
| Broad initial context read exceeded the command-output budget | 1 | Switched to Agent- and frontend-targeted files and symbol-level inspection before implementation. |
| New local fake-SSE test used a non-ASCII Python bytes literal and failed pytest collection | 1 | Replace the literal with a Unicode string encoded as UTF-8; rerun the same offline suite. |
| Fake non-JSON model response escaped through `response.json()` instead of mapping to an Agent error | 1 | Catch JSON/shape parsing errors in the OpenAI-compatible adapter and map them to `AGENT_PROVIDER_RESPONSE_INVALID`. |
| Evaluation runner used the smaller Mock registry, which did not include every audited personal read-only tool | 1 | Construct the normal Spring registry with the in-memory Fake Backend transport; rerun offline evaluation. |

# Phase 8 — Agent core architecture, token-cost governance, model routing, middleware and observability

## Goal and boundaries

Implement one Agent-only professionalization milestone under `agent-service/`. Preserve all public HTTP and SSE contracts, operate entirely offline with Fake Provider/MockTransport/local data, never call a real model or any network address, and make no changes to backend, frontend, database, SQL, Redis data, or Spring contracts.

## Phases

1. **Audit Phase 0–7 implementation, docs, contracts and reuse points** — `completed`
2. **Design and implement layered middleware, token budget/context compression, tool-schema selection and result compaction** — `completed`
3. **Implement model profiles/router, provider resilience, public cache, usage accounting and safe tracing seams** — `completed`
4. **Add offline tests, 50-case regression coverage, real token benchmarks and Phase 8 documentation** — `completed`
5. **Run prescribed offline compile/test/eval/benchmark commands; record exact results and stop for review** — `completed`

## Fixed decisions

- The orchestration remains a custom explicit state machine unless the audit demonstrates LangGraph is required for a concrete checkpoint/recovery need. This milestone does not add multi-agent orchestration or RAG.
- Token counts are estimates unless a provider supplies usage; unsupported tokenizers must set `estimated=true` and include a safety margin.
- No cache key or telemetry/log record may contain Authorization, API keys, chat text, raw tool results, token values, or personal dorm/order/address content.
- All failure, fallback and benchmark evidence must be derived from local tests; no metrics or resume number may be invented.

## Phase 8 verification and errors

- Final offline command set succeeded: `compileall app`; `pytest -m "not real_backend"` = `68 passed, 3 deselected`; `evals.runner` = 50 local cases; `benchmarks.report` wrote the required JSON/Markdown report. No real provider, 8090/5174, public network, MySQL, Redis or OSS was contacted.
- Benchmark evidence: baseline average estimated input 4372.86; optimized 318.62; reduction 92.71%; model calls avoided 21; route distribution deterministic 17, fast 28, standard 5.
- The first sandboxed `.venv-clean` invocation could not start its external base interpreter; the approved scoped retry succeeded. One test initially used values below Pydantic safe minimums and was corrected without weakening production limits. One patch used an outdated health-test anchor, then source inspection enabled a narrow corrected patch.
