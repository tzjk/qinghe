import asyncio
import json

import httpx
import pytest

from app.core.config import Settings
from app.core.errors import AgentError
from app.providers.openai_compatible import OpenAICompatibleProvider
from app.schemas.tool import ToolMetadata


TOOLS = [
    ToolMetadata(name="search_shops", description="shops", requires_auth=False, data_source="mock", timeout_seconds=1, input_schema={"type": "object"}),
    ToolMetadata(name="get_today_promotions", description="promotions", requires_auth=False, data_source="mock", timeout_seconds=1, input_schema={"type": "object"}),
]


def settings(**changes) -> Settings:
    return Settings(agent_mock_mode=False, llm_provider="openai_compatible", llm_api_key="fake-key", llm_base_url="http://fake.model/v1", llm_model="fake-model", llm_max_retries=1, **changes)


def transport(handler):
    return httpx.MockTransport(handler)


def response(message, finish_reason="stop", status=200):
    return httpx.Response(status, json={"choices": [{"message": message, "finish_reason": finish_reason}]})


async def test_text_response_uses_only_model_key_header() -> None:
    observed = {}

    def handler(request: httpx.Request) -> httpx.Response:
        observed["authorization"] = request.headers.get("Authorization")
        observed["body"] = request.content.decode()
        return response({"content": "校园助手回答"})

    provider = OpenAICompatibleProvider(settings(), transport=transport(handler))
    assert await provider.generate_response(intent="greeting", message="你好 Bearer user-token", tool_results=[]) == "校园助手回答"
    assert observed["authorization"] == "Bearer fake-key"
    assert "user-token" not in observed["body"]


async def test_single_and_double_tool_calls_are_parsed_and_capped() -> None:
    def handler(_request: httpx.Request) -> httpx.Response:
        return response({"tool_calls": [
            {"type": "function", "function": {"name": "search_shops", "arguments": '{"keyword":"晚饭"}'}},
            {"type": "function", "function": {"name": "get_today_promotions", "arguments": "{}"}},
        ]}, "tool_calls")

    calls = await OpenAICompatibleProvider(settings(), transport=transport(handler)).select_tools(intent="shop_recommendation", message="推荐", max_tools=2, tools=TOOLS)
    assert [(call.name, call.arguments) for call in calls] == [("search_shops", {"keyword": "晚饭"}), ("get_today_promotions", {})]


@pytest.mark.parametrize("message", [
    {"tool_calls": [{"function": {"name": "missing", "arguments": "{}"}}]},
    {"tool_calls": [{"function": {"name": "search_shops", "arguments": "{"}}]},
    {"tool_calls": [{"function": {"name": "search_shops", "arguments": "[]"}}]},
])
async def test_invalid_tool_name_and_arguments_are_rejected(message) -> None:
    provider = OpenAICompatibleProvider(settings(), transport=transport(lambda _request: response(message, "tool_calls")))
    with pytest.raises(AgentError):
        await provider.select_tools(intent="x", message="x", max_tools=2, tools=TOOLS)


@pytest.mark.parametrize("factory", [
    lambda: httpx.Response(200, content=b"not-json", headers={"content-type": "application/json"}),
    lambda: httpx.Response(200, json={"choices": []}),
    lambda: response({"content": "x"}, "length"),
    lambda: response({}, "stop"),
])
async def test_invalid_model_payloads_are_rejected(factory) -> None:
    provider = OpenAICompatibleProvider(settings(), transport=transport(lambda _request: factory()))
    with pytest.raises(AgentError) as captured:
        await provider.generate_response(intent="x", message="x", tool_results=[])
    assert captured.value.code == "AGENT_PROVIDER_RESPONSE_INVALID"


async def test_429_and_500_are_retried_then_safely_mapped() -> None:
    attempts = 0

    def handler(_request: httpx.Request) -> httpx.Response:
        nonlocal attempts
        attempts += 1
        return httpx.Response(429 if attempts == 1 else 500, json={"error": {}})

    provider = OpenAICompatibleProvider(settings(), transport=transport(handler))
    with pytest.raises(AgentError) as captured:
        await provider.generate_response(intent="x", message="x", tool_results=[])
    assert captured.value.retryable and attempts == 2


async def test_timeout_is_retried_without_network() -> None:
    attempts = 0

    async def handler(_request: httpx.Request) -> httpx.Response:
        nonlocal attempts
        attempts += 1
        raise httpx.ReadTimeout("offline timeout")

    with pytest.raises(AgentError) as captured:
        await OpenAICompatibleProvider(settings(), transport=transport(handler)).generate_response(intent="x", message="x", tool_results=[])
    assert captured.value.retryable and attempts == 2


async def test_streaming_response_and_midstream_failure() -> None:
    stream = 'data: {"choices":[{"delta":{"content":"青禾"}}]}\n\ndata: {"choices":[{"delta":{"content":"助手"}}]}\n\ndata: [DONE]\n\n'.encode("utf-8")
    provider = OpenAICompatibleProvider(settings(), transport=transport(lambda _request: httpx.Response(200, content=stream)))
    assert [part async for part in provider.stream_response(intent="x", message="x", tool_results=[])] == ["青禾", "助手"]
    broken = OpenAICompatibleProvider(settings(), transport=transport(lambda _request: httpx.Response(200, content=b'data: {bad}\n\n')))
    with pytest.raises(AgentError):
        async for _ in broken.stream_response(intent="x", message="x", tool_results=[]):
            pass


async def test_provider_request_can_be_cancelled() -> None:
    started = asyncio.Event()

    async def handler(_request: httpx.Request) -> httpx.Response:
        started.set()
        await asyncio.sleep(10)
        return response({"content": "late"})

    task = asyncio.create_task(OpenAICompatibleProvider(settings(), transport=transport(handler)).generate_response(intent="x", message="x", tool_results=[]))
    await started.wait()
    task.cancel()
    with pytest.raises(asyncio.CancelledError):
        await task
