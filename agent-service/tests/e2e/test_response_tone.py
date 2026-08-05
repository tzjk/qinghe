import httpx

from tests.fakes.fake_qinghe_backend import result


async def ask(client, message: str):
    return await client.post("/api/v1/chat", json={"message": message})


async def test_greeting_and_help_use_a_warm_direct_style(client) -> None:
    greeting = await ask(client, "你好")
    help_response = await ask(client, "你可以做什么")

    assert greeting.status_code == 200
    assert greeting.json()["answer"].startswith("你好呀，我是清禾校园小助手")
    assert "今天想了解什么" in greeting.json()["answer"]
    assert help_response.status_code == 200
    assert "今天有什么优惠" in help_response.json()["answer"]


async def test_promotion_answers_are_helpful_with_or_without_results(client, fake_backend) -> None:
    available = await ask(client, "今天有什么优惠")
    assert available.status_code == 200
    assert available.json()["answer"].startswith("今天有这些优惠可以看看：")

    fake_backend.responses["/api/coupons"] = httpx.Response(
        200,
        json=result(200, {"records": [], "total": 0, "page": 1, "size": 10}),
    )
    empty = await ask(client, "今天有什么优惠")
    assert empty.status_code == 200
    assert "暂时没查到可领取的优惠券" in empty.json()["answer"]
    assert "可以" in empty.json()["answer"]


async def test_delete_order_remains_rejected(client) -> None:
    response = await ask(client, "帮我删除订单")

    assert response.status_code == 400
    assert response.json()["error"]["code"] == "AGENT_PROMPT_INJECTION_BLOCKED"
