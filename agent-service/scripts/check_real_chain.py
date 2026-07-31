"""Local-only public FastAPI -> deterministic -> Spring validation. No credentials are used."""

import asyncio
import json
import re

import httpx


AGENT_URL = "http://127.0.0.1:8100"
BACKEND_URL = "http://127.0.0.1:8090"
SAFE_MARKERS = ("Authorization", "http://127.0.0.1:8090", "Traceback", "java.lang.")


def _assert_safe(value: object) -> None:
    rendered = json.dumps(value, ensure_ascii=False)
    if any(marker in rendered for marker in SAFE_MARKERS):
        raise RuntimeError("response exposed a credential marker, internal URL, or stack trace")


async def _ask(client: httpx.AsyncClient, message: str, expected_tool: str) -> None:
    response = await client.post("/api/v1/chat", json={"message": message})
    response.raise_for_status()
    body = response.json()
    if body.get("provider") != "deterministic" or body.get("tool_mode") != "spring" or body.get("mock") is not False:
        raise RuntimeError("FastAPI is not in deterministic Spring mode")
    if not body.get("tool_calls") or body["tool_calls"][0].get("tool_name") != expected_tool:
        raise RuntimeError(f"unexpected tool for {message}")
    if not isinstance(body.get("answer"), str) or not re.search(r"[\u4e00-\u9fff]", body["answer"]):
        raise RuntimeError("deterministic provider did not return a Chinese answer")
    _assert_safe(body)


async def main() -> None:
    async with httpx.AsyncClient(timeout=10.0) as backend:
        shops = (await backend.get(f"{BACKEND_URL}/api/shops", params={"page": 1, "size": 10})).json()
    records = shops.get("data", {}).get("records", [])
    if not records or not isinstance(records[0].get("id"), int):
        raise RuntimeError("real public shop lookup did not return an id for detail/goods verification")
    shop_id = records[0]["id"]

    async with httpx.AsyncClient(base_url=AGENT_URL, timeout=10.0) as client:
        health = (await client.get("/health")).json()
        ready = (await client.get("/ready")).json()
        tools = (await client.get("/api/v1/tools")).json().get("tools", [])
        if health.get("status") != "ok" or health.get("mock_mode") is not False:
            raise RuntimeError("health response is not deterministic Spring mode")
        if ready.get("provider") != "deterministic" or ready.get("tool_count") != 15:
            raise RuntimeError("ready response is not deterministic with all public tools")
        expected = {"get_today_promotions", "get_active_coupons", "search_shops", "get_shop_detail", "get_shop_goods", "get_nearby_shops", "get_hot_explore_posts"}
        if not expected.issubset({item.get("name") for item in tools}):
            raise RuntimeError("safe tool directory is incomplete")

        for message, tool in (
            ("今天有什么优惠？", "get_today_promotions"),
            ("现在有哪些可领取优惠券？", "get_active_coupons"),
            ("有哪些商铺？", "search_shops"),
            (f"查看商铺详情 {shop_id}", "get_shop_detail"),
            (f"查看商铺 {shop_id} 的商品", "get_shop_goods"),
            ("热门探店有哪些？", "get_hot_explore_posts"),
            ("附近有什么商铺？经度120 纬度30 半径5", "get_nearby_shops"),
        ):
            await _ask(client, message, tool)

        for message, expected_tool in (
            ("我的宿舍", "get_my_dorm_info"),
            (f"查看商铺详情 {shop_id + 1_000_000_000}", "get_shop_detail"),
            ("附近有什么商铺？经度181 纬度30", "get_nearby_shops"),
            ("附近有什么商铺？经度120 纬度30 半径21", "get_nearby_shops"),
        ):
            response = await client.post("/api/v1/chat", json={"message": message})
            response.raise_for_status()
            body = response.json()
            if body["tool_calls"][0].get("tool_name") != expected_tool or body["tool_calls"][0].get("status") != "failed":
                raise RuntimeError("expected safe failed tool result was not returned")
            _assert_safe(body)

        invalid = await client.post("/api/v1/chat", json={"message": "我的资料"}, headers={"Authorization": "Bearer invalid-token"})
        invalid.raise_for_status()
        invalid_body = invalid.json()
        if invalid_body["tool_calls"][0].get("status") != "failed" or "invalid-token" in invalid.text:
            raise RuntimeError("invalid token was not safely mapped")
        _assert_safe(invalid_body)

        discount = (await client.post("/api/v1/chat", json={"message": "今天哪些商品打折？"})).json()
        if "优惠券" not in discount.get("answer", "") or "商品价格" not in discount.get("answer", ""):
            raise RuntimeError("unsupported product-discount question was not answered safely")
        _assert_safe(discount)
    print("real-chain: seven-public-tools, safe-errors, deterministic-no-key passed")


if __name__ == "__main__":
    asyncio.run(main())
