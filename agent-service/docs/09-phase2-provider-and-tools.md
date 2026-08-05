# Phase 2 Provider and Tool Architecture

## Offline default

Phase 2 defaults to `AGENT_MOCK_MODE=true`, `AGENT_TOOL_MODE=mock`, `LLM_PROVIDER=mock`, and `QINGHE_BACKEND_ENABLED=false`. This constructs only the deterministic Mock provider and Mock registry; it opens no Spring or model connection.

`Settings` rejects invalid combinations. Mock mode requires Mock provider/tools. Spring tools require `AGENT_TOOL_MODE=spring` plus `QINGHE_BACKEND_ENABLED=true`. The OpenAI-compatible provider validates Key, Base URL, and model only when selected.

## Provider design

`BaseLLMProvider` has bounded tool selection and final-answer methods. `OpenAICompatibleProvider` uses an OpenAI Chat Completions compatible endpoint, vendor-neutral model/base URL configuration, structured function definitions, finite timeouts, and at most one retry for connection/timeout failures. It neither calls Spring Boot nor accesses MySQL, Redis, Shell, SQL, or generated code.

The orchestration sequence is fixed: safety check, deterministic intent, provider candidate tools, registry whitelist, Pydantic validation, at most two tool executions, normalized results, then final answer. Tool-result text is untrusted data, never instructions.

## Spring client design

`QingheClient` owns an `httpx.AsyncClient` only in Spring tool mode. It has a single configured base address, static GET endpoint registry, disabled redirects, discrete connect/read/write/pool timeouts, and safe closing in application lifespan. Tools cannot provide URLs or paths.

Bearer Token is extracted only from the incoming `Authorization` header and passed only to the Spring request for auth-required endpoints. It is not a Pydantic public field, log field, model prompt value, or session value.
