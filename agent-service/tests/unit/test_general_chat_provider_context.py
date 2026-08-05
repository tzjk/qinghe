import json

import httpx

from app.conversation import ConversationContext, ConversationTurn
from app.core.config import Settings
from app.providers.openai_compatible import OpenAICompatibleProvider


async def test_general_chat_uses_provider_with_sanitized_recent_context() -> None:
    observed: dict[str, object] = {}

    def handler(request: httpx.Request) -> httpx.Response:
        observed["body"] = json.loads(request.content)
        return httpx.Response(200, json={"choices": [{"message": {"content": "先安排十分钟休息，再选择一件最重要的学习任务。"}, "finish_reason": "stop"}]})

    settings = Settings(agent_mock_mode=False, llm_provider="openai_compatible", llm_api_key="fake-key", llm_base_url="https://model.example/v1", llm_model="fake-model", llm_max_retries=0)
    context = ConversationContext(summary="", recent_turns=(ConversationTurn("推荐一家店", "晴川面馆", "shop_recommendation", ("search_shops",), ((1, "晴川面馆"),)),))
    provider = OpenAICompatibleProvider(settings, transport=httpx.MockTransport(handler))

    answer = await provider.generate_response(intent="general_chat", message="我最近有点累，给我一些学习建议", tool_results=[], conversation_context=context)

    serialized = str(observed["body"])
    assert answer.startswith("先安排")
    assert "最近对话的脱敏参考数据" in serialized
    assert "晴川面馆" in serialized
    assert "fake-key" not in serialized
