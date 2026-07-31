# Future Integration Checklist

1. Keep a private, uncommitted `.env`; fill `LLM_API_KEY`, `LLM_BASE_URL`, and `LLM_MODEL` with user-supplied values only.
2. Set `AGENT_MOCK_MODE=false`, `AGENT_TOOL_MODE=spring`, `QINGHE_BACKEND_ENABLED=true`; select `LLM_PROVIDER=openai_compatible` only when the model values are ready.
3. Start no service from this checklist. In a separately authorized session, confirm the existing Spring Boot deployment and use a normal user Bearer token.
4. Verify public GET responses, then one auth-required GET response, then safe backend error mappings, then provider tool selection.
5. Confirm no admin route, write method, token log, token prompt value, or user-identity parameter has entered the flow.
6. For any failure, retain the safe structured error and do not add fallback data.
7. Roll back by restoring `AGENT_MOCK_MODE=true`, `AGENT_TOOL_MODE=mock`, `LLM_PROVIDER=mock`, and `QINGHE_BACKEND_ENABLED=false`.
