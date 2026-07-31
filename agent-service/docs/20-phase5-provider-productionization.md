# Phase 5 Provider productionization

`openai_compatible` uses the standard Chat Completions path `/chat/completions` only after a chat request. Construction and `/ready` do not send network traffic. It accepts text, function tool calls (at most two), non-streaming responses, and SSE-style model chunks. Invalid JSON, empty choices, invalid finish reasons, illegal tool names, malformed arguments, 429, 5xx, timeout, and cancellation map to safe Agent errors.

The adapter receives no incoming Bearer token, backend URL, raw tool result, SQL, shell, or internal stack. Its local fake-transport tests cover the protocol without a real key or external model call. Development/test fallback is opt-in and produces a warning; production rejects silent fallback configuration.
