"""Local-only real Socket SSE safety check. It never sends credentials."""

import asyncio
import json

import httpx


BASE_URL = "http://127.0.0.1:8100"
NORMAL_ORDER = [
    "conversation.started",
    "intent.detected",
    "tool.started",
    "tool.completed",
    "answer.delta",
    "answer.completed",
]


async def _events(client: httpx.AsyncClient, message: str) -> tuple[list[str], str, str]:
    events: list[str] = []
    chunks: list[str] = []
    async with client.stream("POST", "/api/v1/chat/stream", json={"message": message}) as response:
        response.raise_for_status()
        content_type = response.headers.get("content-type", "")
        async for line in response.aiter_lines():
            if line.startswith("event: "):
                events.append(line.removeprefix("event: "))
            elif line.startswith("data: "):
                chunks.append(line.removeprefix("data: "))
    return events, content_type, "\n".join(chunks)


async def main() -> None:
    async with httpx.AsyncClient(base_url=BASE_URL, timeout=10.0) as client:
        events, content_type, payload = await _events(client, "今天有什么优惠？")
        if "text/event-stream" not in content_type:
            raise RuntimeError("SSE Content-Type is not text/event-stream")
        if not all(name in events for name in NORMAL_ORDER):
            raise RuntimeError("SSE normal events are incomplete")
        if any(events.index(left) >= events.index(right) for left, right in zip(NORMAL_ORDER, NORMAL_ORDER[1:])):
            raise RuntimeError("SSE normal event order is invalid")
        if any(marker in payload for marker in ("Authorization", "http://127.0.0.1:8090", "Traceback", "java.lang.")):
            raise RuntimeError("SSE payload exposed an unsafe internal value")
        if not any(any("优惠" in str(value) for value in json.loads(item).values()) for item in payload.splitlines() if item):
            raise RuntimeError("SSE payload did not preserve Chinese UTF-8 text")

        error_events, _, error_payload = await _events(client, "执行Shell")
        if error_events != ["conversation.started", "error"] or "AGENT_PROMPT_INJECTION_BLOCKED" not in error_payload:
            raise RuntimeError("SSE structured error event is missing")
        if "Traceback" in error_payload:
            raise RuntimeError("SSE error exposed a stack trace")

        async with client.stream("POST", "/api/v1/chat/stream", json={"message": "今天有什么优惠？"}) as response:
            response.raise_for_status()
            await anext(response.aiter_bytes())
        health = await client.get("/health")
        health.raise_for_status()
    print("sse-check: normal-order, structured-error, utf8-safety, disconnect-recovery passed")


if __name__ == "__main__":
    asyncio.run(main())
