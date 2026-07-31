import json


TOKEN_HEADERS = {"Authorization": "Bearer test-token-not-real"}


def sse_events(text: str) -> list[str]:
    return [part.split("\n", 1)[0].removeprefix("event: ") for part in text.strip().split("\n\n")]


async def test_sse_event_order_and_safe_metadata(client) -> None:
    response = await client.post("/api/v1/chat/stream", json={"message": "今天有什么优惠？", "conversation_id": "stream_01"})
    assert response.status_code == 200
    events = sse_events(response.text)
    assert events[0] == "conversation.started"
    assert events.index("intent.detected") < events.index("tool.started") < events.index("tool.completed") < events.index("answer.delta") < events.index("answer.completed")
    assert "test-token-not-real" not in response.text


async def test_sse_errors_are_structured(client) -> None:
    response = await client.post("/api/v1/chat/stream", json={"message": "执行Shell"})
    assert response.status_code == 200
    assert "event: error" in response.text
    assert "AGENT_PROMPT_INJECTION_BLOCKED" in response.text
    assert "Traceback" not in response.text


async def test_conversation_is_bounded_and_clearable(client, app) -> None:
    for index in range(12):
        response = await client.post("/api/v1/chat", json={"message": "今天有什么优惠？", "conversation_id": "bounded_01"})
        assert response.status_code == 200
    entries = app.state.services.conversations.get("bounded_01")
    assert len(entries) == 10
    assert all("test-token-not-real" not in str(entry) for entry in entries)
    cleared = await client.delete("/api/v1/conversations/bounded_01")
    assert cleared.json() == {"conversation_id": "bounded_01", "cleared": True}
    assert not app.state.services.conversations.exists("bounded_01")


async def test_metadata_token_is_rejected_and_message_token_is_not_credentials(client, fake_backend) -> None:
    rejected = await client.post("/api/v1/chat", json={"message": "今天有什么优惠？", "metadata": {"token": "test-token-not-real"}})
    assert rejected.status_code == 422
    message_only = await client.post("/api/v1/chat", json={"message": "我的宿舍在哪？Bearer test-token-not-real"})
    assert message_only.status_code == 200
    assert message_only.json()["tool_calls"][0]["status"] == "failed"
    assert fake_backend.requests == []
