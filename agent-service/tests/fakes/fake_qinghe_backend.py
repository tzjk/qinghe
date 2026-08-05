from dataclasses import dataclass, field
from typing import Any

import httpx


def result(code: int = 200, data: Any = None, message: str = "success") -> dict[str, Any]:
    return {"code": code, "message": message, "data": data}


@dataclass
class FakeQingheBackend:
    """In-memory Qinghe Result-envelope simulator; it never opens a socket."""

    responses: dict[str, httpx.Response] = field(default_factory=dict)
    requests: list[httpx.Request] = field(default_factory=list)

    async def handle(self, request: httpx.Request) -> httpx.Response:
        self.requests.append(request)
        if request.url.host != "offline.backend":
            raise AssertionError(f"unexpected outbound host: {request.url.host}")
        override = self.responses.get(request.url.path)
        if override is not None:
            return override
        if request.method != "GET":
            return httpx.Response(405, json=result(404, None), request=request)
        if request.url.path == "/api/coupons":
            return httpx.Response(
                200,
                json=result(
                    data={
                        "records": [
                            {"name": "晴川晚餐券", "status": "ENABLED", "availableStock": 12, "receiveStartTime": "2020-01-01T00:00:00Z", "receiveEndTime": "2099-01-01T00:00:00Z"},
                            {"name": "已停用券", "status": "DISABLED", "availableStock": 9},
                            {"name": "售罄券", "status": "ENABLED", "availableStock": 0},
                        ],
                        "total": 3,
                        "page": 1,
                        "size": 10,
                    }
                ),
                request=request,
            )
        if request.url.path == "/api/shops":
            return httpx.Response(200, json=result(data={"records": [{"name": "晴川面馆"}], "total": 1, "page": 1, "size": 10}), request=request)
        if request.url.path == "/api/shops/1/goods":
            return httpx.Response(200, json=result(data={"records": [{"id": 11, "name": "招牌面", "price": 12}], "total": 1, "page": 1, "size": 10}), request=request)
        if request.url.path == "/api/explore/shops/nearby":
            return httpx.Response(200, json=result(data={"records": [{"name": "校门口简餐"}], "total": 1, "page": 1, "size": 10}), request=request)
        if request.headers.get("Authorization") != "Bearer test-token-not-real":
            return httpx.Response(200, json=result(401, None, "unauthenticated"), request=request)
        if request.url.path == "/api/student/dorm/me":
            return httpx.Response(200, json=result(data={"campusName": "青禾模拟校区", "buildingName": "知行楼", "roomNo": "302", "bedNo": "2", "checkInStatus": "CHECKED_IN"}), request=request)
        if request.url.path == "/api/coupons/mine":
            return httpx.Response(200, json=result(data={"records": [{"name": "过期演示券", "status": "EXPIRED"}], "total": 1, "page": 1, "size": 10}), request=request)
        if request.url.path == "/api/orders":
            return httpx.Response(200, json=result(data={"records": [{"orderNo": "SIM-001", "status": "DELIVERING"}], "total": 1, "page": 1, "size": 10}), request=request)
        if request.url.path == "/api/orders/123":
            return httpx.Response(200, json=result(403, None, "forbidden"), request=request)
        return httpx.Response(404, json=result(404, None, "not found"), request=request)

    @property
    def transport(self) -> httpx.MockTransport:
        return httpx.MockTransport(self.handle)
