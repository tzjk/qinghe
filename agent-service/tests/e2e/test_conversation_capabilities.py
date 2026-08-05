import httpx


async def ask(client, message: str, *, conversation_id: str = "conversation_capabilities"):
    return await client.post("/api/v1/chat", json={"message": message, "conversation_id": conversation_id})


async def test_local_general_tool_and_contextual_paths(client, fake_backend) -> None:
    fake_backend.responses["/api/shops"] = httpx.Response(
        200,
        json={"code": 200, "message": "success", "data": {"records": [
            {"id": 2, "name": "知味简餐", "score": 4.6},
            {"id": 1, "name": "晴川面馆", "score": 4.9},
        ], "total": 2, "page": 1, "size": 10}},
    )
    fake_backend.responses["/api/coupons"] = httpx.Response(
        200,
        json={"code": 200, "message": "success", "data": {"records": [
            {"name": "满20减3券", "discountAmount": 3, "thresholdAmount": 20},
            {"name": "满30减8券", "discountAmount": 8, "thresholdAmount": 30},
        ], "total": 2, "page": 1, "size": 10}},
    )

    greeting = await ask(client, "你好")
    help_response = await ask(client, "你可以帮我做什么")
    general = await ask(client, "最近学习有点累，给我一些建议")
    promotion = await ask(client, "今天有什么优惠")
    recommendation = await ask(client, "推荐一家校园店铺")
    follow_up = await ask(client, "第一家有什么商品")
    pronoun_follow_up = await ask(client, "刚才那个有什么商品")
    price_follow_up = await ask(client, "有没有更便宜的")
    coupon_recommendation = await ask(client, "哪张优惠券最值得领取")

    assert greeting.json()["intent"] == "greeting" and greeting.json()["tool_calls"] == []
    assert help_response.json()["intent"] == "help" and help_response.json()["tool_calls"] == []
    assert general.json()["intent"] == "general_chat" and general.json()["tool_calls"] == []
    assert promotion.json()["tool_calls"][0]["tool_name"] == "get_today_promotions"
    assert "满30减8券" in promotion.json()["answer"]
    assert recommendation.json()["tool_calls"][0]["tool_name"] == "search_shops"
    assert "晴川面馆" in recommendation.json()["answer"]
    assert follow_up.json()["intent"] == "shop_goods"
    assert follow_up.json()["tool_calls"][0]["tool_name"] == "get_shop_goods"
    assert pronoun_follow_up.json()["intent"] == "shop_goods"
    assert pronoun_follow_up.json()["tool_calls"][0]["tool_name"] == "get_shop_goods"
    assert price_follow_up.json()["intent"] == "contextual_clarification"
    assert price_follow_up.json()["tool_calls"] == []
    assert coupon_recommendation.json()["intent"] == "promotion_query"
    assert coupon_recommendation.json()["tool_calls"][0]["tool_name"] == "get_today_promotions"
    assert "满30减8券" in coupon_recommendation.json()["answer"]


async def test_context_free_reference_clarifies_and_writes_are_rejected(client, fake_backend) -> None:
    clarification = await ask(client, "第一家有什么商品", conversation_id="no_reference")
    price_clarification = await ask(client, "有没有更便宜的", conversation_id="no_price_reference")
    rejected = await ask(client, "帮我删除最近订单", conversation_id="write_request")

    assert clarification.json()["intent"] == "clarification"
    assert clarification.json()["tool_calls"] == []
    assert price_clarification.json()["intent"] == "clarification"
    assert price_clarification.json()["tool_calls"] == []
    assert rejected.status_code == 400
    assert rejected.json()["error"]["code"] == "AGENT_PROMPT_INJECTION_BLOCKED"
    assert fake_backend.requests == []
