# Final model configuration

Do not place a key in tracked files. When the user is ready for a manual first model check, create ignored `agent-service/.env.local`:

```dotenv
AGENT_MOCK_MODE=false
AGENT_TOOL_MODE=spring
QINGHE_BACKEND_ENABLED=true
QINGHE_BACKEND_BASE_URL=http://127.0.0.1:8090
LLM_PROVIDER=openai_compatible
LLM_API_KEY=<user supplied>
LLM_BASE_URL=<user supplied>
LLM_MODEL=<user supplied>
LLM_TEMPERATURE=0.2
LLM_TIMEOUT_SECONDS=30
LLM_MAX_RETRIES=1
AGENT_ALLOWED_ORIGINS=http://localhost:5174
```

This milestone does not validate that key, start a service, or contact a model. For the first manual acceptance, start only user-controlled dependencies, ask one public read-only question, verify no token appears in output/logs, then separately test an ordinary logged-in user personal query. Do not use an administrator token.
