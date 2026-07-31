from httpx import AsyncClient


TOKEN_HEADERS = {"Authorization": "Bearer test-token-not-real"}


async def ask(client: AsyncClient, message: str, *, token: bool = False, **extra: object):
    return await client.post("/api/v1/chat", json={"message": message, **extra}, headers=TOKEN_HEADERS if token else {})


async def test_today_promotions_complete_offline_chain(client, fake_backend) -> None:
    response = await ask(client, "今天有什么优惠？")
    body = response.json()
    assert response.status_code == 200
    assert body["intent"] == "promotion_query"
    assert body["tool_calls"][0]["tool_name"] == "get_today_promotions"
    assert body["tool_calls"][0]["data_source"] == "spring_simulated"
    assert "晴川晚餐券" in body["answer"]
    assert fake_backend.requests[-1].url.path == "/api/coupons"


async def test_active_coupons_and_shop_goods_complete_offline_chain(client, fake_backend) -> None:
    coupons = await ask(client, "现在有哪些可领取优惠券？")
    goods = await ask(client, "查看商铺 1 的商品")
    assert coupons.json()["tool_calls"][0]["tool_name"] == "get_active_coupons"
    assert goods.json()["tool_calls"][0]["tool_name"] == "get_shop_goods"
    assert "招牌面" in goods.json()["answer"]
    assert fake_backend.requests[-1].url.path == "/api/shops/1/goods"


async def test_shop_recommendation_uses_supported_keyword_only(client) -> None:
    response = await ask(client, "推荐一家适合晚饭的商铺")
    assert response.status_code == 200
    assert response.json()["tool_calls"][0]["tool_name"] == "search_shops"
    assert "晴川面馆" in response.json()["answer"]


async def test_nearby_requires_coordinates_without_backend_call(client, fake_backend) -> None:
    response = await ask(client, "附近有什么商铺？")
    assert response.status_code == 200
    assert response.json()["tool_calls"] == []
    assert "经度" in response.json()["answer"]
    assert fake_backend.requests == []


async def test_nearby_valid_coordinates_are_transient(client, fake_backend) -> None:
    response = await ask(client, "附近有什么商铺？经度120.1 纬度30.2")
    assert response.status_code == 200
    assert response.json()["tool_calls"][0]["tool_name"] == "get_nearby_shops"
    request = fake_backend.requests[-1]
    assert request.url.path == "/api/explore/shops/nearby"
    assert "120.1" in str(request.url)


async def test_nearby_out_of_range_radius_is_rejected_before_backend_call(client, fake_backend) -> None:
    response = await ask(client, "附近有什么商铺？经度120 纬度30 半径21")
    assert response.status_code == 200
    assert response.json()["tool_calls"][0]["tool_name"] == "get_nearby_shops"
    assert response.json()["tool_calls"][0]["status"] == "failed"
    assert fake_backend.requests == []


async def test_my_dorm_needs_header_token_and_forwards_only_to_fake_backend(client, fake_backend, provider) -> None:
    denied = await ask(client, "我的宿舍在哪？")
    assert denied.status_code == 200
    assert denied.json()["warnings"]
    assert denied.json()["tool_calls"][0]["status"] == "failed"
    assert fake_backend.requests == []

    allowed = await ask(client, "我的宿舍在哪？", token=True)
    assert allowed.status_code == 200
    assert "知行楼" in allowed.json()["answer"]
    assert fake_backend.requests[-1].headers["Authorization"] == "Bearer test-token-not-real"
    assert all("test-token-not-real" not in str(item) for item in provider.observed_inputs)
    assert "test-token-not-real" not in allowed.text


async def test_other_user_dorm_is_blocked_before_provider_or_backend(client, fake_backend, provider) -> None:
    response = await ask(client, "查询用户1001的宿舍", token=True)
    assert response.status_code == 400
    assert response.json()["error"]["code"] == "AGENT_PROMPT_INJECTION_BLOCKED"
    assert fake_backend.requests == []
    assert provider.observed_inputs == []


async def test_coupon_wallet_and_recent_orders_need_token(client) -> None:
    coupon = await ask(client, "我的优惠券有哪些？", token=True)
    orders = await ask(client, "我的最近订单", token=True)
    assert coupon.json()["tool_calls"][0]["tool_name"] == "get_my_coupon_wallet"
    assert "过期" in coupon.json()["answer"]
    assert orders.json()["tool_calls"][0]["tool_name"] == "get_my_recent_orders"
    assert "真实付款" in orders.json()["answer"]


async def test_order_detail_uses_positive_id_and_maps_forbidden(client, fake_backend) -> None:
    response = await ask(client, "查看订单123详情", token=True)
    body = response.json()
    assert response.status_code == 200
    assert body["tool_calls"][0]["tool_name"] == "get_my_order_detail"
    assert body["tool_calls"][0]["status"] == "failed"
    assert "权限" in body["answer"]
    assert fake_backend.requests[-1].url.path == "/api/orders/123"


async def test_discount_question_does_not_invent_discount_tool(client, fake_backend) -> None:
    response = await ask(client, "今天哪些商品打折？")
    assert response.status_code == 200
    assert response.json()["tool_calls"] == []
    assert "优惠券" in response.json()["answer"]
    assert fake_backend.requests == []


async def test_public_response_is_safe_and_complete(client) -> None:
    response = await ask(client, "今天有什么优惠？", conversation_id="safe_01")
    body = response.json()
    assert {"request_id", "conversation_id", "answer", "intent", "tool_calls", "mock", "tool_mode", "provider", "duration_ms", "warnings"} == set(body)
    assert "Authorization" not in response.text
