# Agent Service Progress

## 2026-07-30 - Phase 5–6 completed, awaiting user review

- Implemented Agent-only provider productionization: OpenAI-compatible protocol parser, capped tool calls, safe retry/error/cancellation behavior, injected local fake transport tests, optional development/test deterministic fallback, and no provider initialization network call.
- Added final provider-result minimization; 10-turn/TTL/max-count in-memory conversations; process-local IP, request, conversation, model, tool, and SSE limits; and release-on-completion safeguards. No Redis was added.
- Added `evals/` with 50 deterministic/Fake Backend cases, readable runner output, and aggregate JSON report. Core success metrics are all 100%: auth gate, safety block, max tool calls, response schema; intent/tool-selection metrics are 98%.
- Added Vue `/assistant`, a plain-text responsive chat UI, quick questions, login hint, status labels, stop/clear actions, POST SSE parser (`fetch`/`ReadableStream`/`TextDecoder`/`AbortController`), ordinary-token-only Authorization forwarding, and sessionStorage-only conversation id cleanup on logout/account change. The Agent remains isolated from all other Qinghe pages.
- Added Agent configuration and manual acceptance documents 20–24, plus safe frontend Agent base URL example. No key, Token, real model call, write/admin request, backend/database/Redis/OSS change, service start/stop, Git command, commit, push, or pull request occurred.
- Verification succeeded: `python -m compileall app`; offline pytest `60 passed, 3 deselected` (one pre-existing non-fatal pytest cache permission warning); `python -m evals.runner` 50 cases; `D:/develop/NodeJS/npm.cmd run build` success (1755 modules). Frontend build retained existing Rollup annotation/large-chunk warnings.

## 2026-07-31 — Phase 8 audit in progress

- Restored the Agent Phase 5–6 plan/progress/findings, root records and Phase 0–7 document inventory. Phase 8 is restricted to `agent-service/`; no network, model, local service, database, Redis, OSS or Git operation has been issued.
- Audited the FastAPI app/core/API/schema/conversation/metrics path, Agent intent/orchestrator/provider contracts, tool registry/result minimizer and client allow-list. Current baseline has a combined HTTP middleware, safety/auth/tool gates, fixed two-tool cap, simple conversation turn cap, OpenAI retry and explicit development fallback; it lacks formal pipeline middleware, token budgets, dynamic schema selection, profile routing, circuit breaker, usage cost aggregates and safe tracing.

## 2026-07-31 — Phase 8 completed, awaiting user review

- Added Agent-only `token_budget/`, `model_routing/`, `middleware/`, `cache/`, Provider middleware and observability usage/tracing modules. The custom state machine now applies deterministic Schema selection, pre/post Token budgeting, safe conversation context, Profile decision, process-local public exact cache and aggregate cost/usage recording; existing response/SSE schemas remain unchanged.
- Added 8 Phase 8 offline tests (68 selected total) for budgets, Schema selection, context/clear, cache isolation, routing/order, circuit states, safe tracer and usage summary. Existing 50-case evaluation stayed at its prior metric levels.
- Final commands passed with the known non-fatal pytest-cache permission warning: compileall; `68 passed, 3 deselected`; 50 offline evals; Token benchmark. No external/network/service request occurred.

## 2026-07-30 - Phase 5–6 baseline and plan

- Restored the Phase 4.5 Agent planning records and created the Phase 5–6 integrated plan section. Scope is constrained to Agent and directly related Vue files; no service control, Git, backend/database/Redis/OSS modification, real-model call, public-network access, or live Qinghe endpoint access is authorized.
- Confirmed current provider/configuration, orchestration, conversation, request middleware, ordinary/admin token separation, and user-layout logout contracts. The next implementation phase is provider productionization with in-process fake transport coverage; no production provider call will be made.

## 2026-07-30 - Phase 4.5 started

- User authorized only Agent-local Python runtime isolation, offline tests, explicitly gated public Spring read-only tests, and temporary FastAPI/SSE integration. Real models, Keys, personal Tokens, business writes, administrator routes, Qinghe dependency control, Git, and all backend/frontend/root changes remain out of scope.
- Existing Agent planning records were restored before action. The active milestone is now Phase 4.5; diagnosis precedes any dependency installation or fresh-environment creation.
- Diagnosis proved the current `.venv` was created from Anaconda Python 3.12.4 (`pyvenv.cfg` home points to the Anaconda installation). It imports `ctypes` from that base runtime and fails loading `_ctypes` with access denied; both `python -m uvicorn` and the `.venv` `uvicorn.exe` fail at the same Click import. Bare `uvicorn` is absent from the parent `PATH`.
- The current `PATH` includes Anaconda and Anaconda Library paths. The broad `Env:CONDA*` enumeration failed with a PowerShell duplicate-key error, so the named variables must be checked individually before recording their values.
- The named `CONDA_PREFIX`, `CONDA_DEFAULT_ENV`, and `CONDA_PROMPT_MODIFIER` variables are unset. A verified non-Anaconda CPython 3.12.0 was found and its `ctypes` imported successfully. `.venv-clean` was created from it without altering the old `.venv`, Anaconda, or persistent environment settings; the Agent project and declared dev dependencies were installed only into that new environment.
- Added Agent-local `start_agent.ps1`, `run_tests.ps1`, and `check_sse.py`; the PowerShell helpers explicitly invoke `.venv-clean`, remove only their own process’s Conda markers/PATH entries, use deterministic Spring mode, bind only `127.0.0.1`, and do not persist/print Tokens or Keys.
- Added the missing public `get_active_coupons` and `get_shop_goods` chains, bounded typed inputs, minimal public goods fields, deterministic Chinese answers, and offline coverage. The public registry now has 15 tools; the goods tool uses only `GET /api/shops/{id}/goods` and never claims a product discount.
- Clean-environment verification passed: independent CPython `ctypes`, Click, FastAPI, and Uvicorn imported; `python -m uvicorn --version` passed; `python -m compileall app` passed; and `python -m pytest -m "not real_backend"` reported `42 passed, 3 deselected` in 0.58 s. Pytest emitted only a non-fatal warning that its pre-existing `.pytest_cache` path was not writable.
- `Test-NetConnection 127.0.0.1 -Port 8090` reported true. With the required `QINGHE_REAL_BACKEND_TESTS=true` gate, `python -m pytest -m real_backend` reported `2 passed, 1 skipped, 42 deselected` in 1.96 s. The skipped test is exclusively the user-supplied-token personal-success path; no Token was supplied or printed.
- The first hidden background invocation of `scripts/start_agent.ps1` did not become healthy on 8100 within 15 seconds. No FastAPI chat/SSE request followed that failed readiness check; process/port diagnosis is the next changed approach.
- The launcher and port were both absent. Foreground diagnosis exposed a Windows PowerShell source-encoding parse failure in the newly created `.ps1` files; the source text itself was valid when read as UTF-8. The repair is limited to adding a UTF-8 BOM to the two Agent-local PowerShell scripts, not changing their behavior or any system encoding setting.
- With UTF-8 BOM encoding, the FastAPI launcher reached `/health`, `/ready`, and `/api/v1/tools`: health was `ok`, mock mode was false, readiness reported provider `deterministic` and 15 tools. A bounded radius parser and offline rejection test were then added. The launcher-shell termination left one Uvicorn child holding 8100; no request is being sent until that exact temporary process is stopped and the port is confirmed free.
- The 8100 owner was verified as the exact temporary CPython/Uvicorn command launched for this task. Only that PID was stopped; `Get-NetTCPConnection` then confirmed `port_8100_released=true`. Spring Boot, MySQL, and Redis were not stopped or changed.
- Final isolated verification after the radius change passed: `compileall app` passed and the offline suite reported `43 passed, 3 deselected` (the same non-fatal unwritable `.pytest_cache` warning). Final real-backend validation reported `2 passed, 1 skipped, 43 deselected`; clean `uvicorn.exe --version` also passed.
- The final FastAPI public chain script passed all seven required tools through the actual 8100 Socket to 8090. The real SSE script passed normal event ordering, structured injection-blocked error, UTF-8 safety, no internal sensitive markers, deliberate early disconnect, and later health recovery. The final Uvicorn child was verified by command line, stopped, and 8100 was confirmed released.
- Updated Agent-only Phase 4.5 records: docs 15--17 reflect actual evidence and residual risks; docs 18--19 record the runtime repair and real chain results. Phase 4.5 public acceptance is complete. Personal success verification remains deferred pending a user-provided ordinary Token; no Token was requested, acquired, stored, or printed.
- Final helper verification passed: `scripts/run_tests.ps1` executed correctly after its UTF-8 BOM normalization, applied its process-local isolation, and reported `43 passed, 3 deselected` with only the known non-fatal `.pytest_cache` permission warning.

## 2026-07-30 - Phase 3 completed

- Added deterministic offline Provider, HTTPX `MockTransport` Fake Qinghe Backend, Mock/Spring contract comparison, bounded orchestration failures, in-memory conversation store, safe public response fields, SSE streaming, and Agent-local conversation deletion.
- Permitted verification passed: `./.venv/Scripts/python.exe -m pytest` reported `39 passed in 0.49s`; `./.venv/Scripts/python.exe -m compileall app` passed.
- No real HTTP request, localhost:8090/5174 access, Spring Boot, Vue, MySQL, Redis, OSS, model, Key, `.env`, SQL, or Git operation occurred.

## 2026-07-30 - Phase 4 planning started

- User authorized only Phase 4 controlled real Spring Boot read-only integration under `agent-service/`, with a deterministic provider and no model key.
- Restored the existing Phase 3 planning records and ran the planning-skill catch-up script successfully after two path-resolution corrections; it reported no unsynced context.
- Confirmed from the current Agent configuration that `LLM_PROVIDER` accepts only `mock` and `openai_compatible`, while Spring mode requires `AGENT_MOCK_MODE=false`; a minimal backward-compatible deterministic-provider configuration adjustment is expected.
- The supplied Phase 4 requirements are truncated after the Stage D heading. No implementation, FastAPI startup, request to `127.0.0.1:8090`, token handling, database/Redis access, SQL, Git, or main-system modification has occurred in Phase 4.

## 2026-07-30 - Phase 4 code, offline verification, and public direct checks

- Added the explicit `deterministic + spring` configuration path without a model key, while retaining fully offline Mock defaults. Added only audited read tools for current-user student profile and address summaries; Spring registry now contains 14 tools.
- Added Agent-side safe VO minimization and aliases for actual Spring dorm/coupon fields, `.env.local.example` with placeholders only, local-env gitignore rules, and double-gated `real_backend` tests.
- `./.venv/Scripts/python.exe -m pytest -m "not real_backend"` passed with `41 passed, 3 deselected`; `./.venv/Scripts/python.exe -m compileall app` passed. No offline test accessed 8090.
- Direct public Stage A checks against user-operated 8090 completed successfully for coupons, shops, one returned shop's detail and goods, hot explore posts, and nearby shops. All reported HTTP 200 and business code 200.
- `/actuator/health` returned 404, so it is not an available health endpoint. The public checks prove backend reachability instead.
- Uvicorn could not start due an external Click/Anaconda `ctypes` DLL-import error. All attempted FastAPI child processes exited; temporary logs were deleted. No Token, personal request, real pytest, write/admin call, MySQL/Redis/OSS access, system configuration edit, backend/frontend/root-file edit, Git, or model call occurred.
- Per the user-required gate, `pytest -m real_backend` is not run yet. It awaits explicit confirmation; personal verification additionally awaits an ordinary user Token supplied only at runtime.
- Default `./.venv/Scripts/python.exe -m pytest` was also checked with the real-backend guard explicitly false: `41 passed, 3 skipped`. This confirms the real suite does not access 8090 by default.
