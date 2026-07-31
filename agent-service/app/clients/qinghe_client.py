import asyncio
import time
from typing import Any
from urllib.parse import urlsplit

import httpx

from app.clients.endpoint_registry import ENDPOINTS, EndpointName
from app.clients.error_mapper import map_transport_error
from app.clients.response_parser import ParsedBackendResponse, parse_backend_response
from app.core.config import Settings
from app.core.errors import AUTH_REQUIRED, AgentError


class QingheClient:
    """GET-only client. Tools can select only a registry key and typed query values."""

    def __init__(
        self,
        settings: Settings,
        *,
        transport: httpx.AsyncBaseTransport | None = None,
        data_source: str = "spring",
    ) -> None:
        if not settings.qinghe_backend_enabled:
            raise ValueError("QINGHE_BACKEND_ENABLED=false 时不能创建 QingheClient")
        parsed = urlsplit(settings.qinghe_backend_base_url)
        if parsed.scheme not in {"http", "https"} or not parsed.netloc or parsed.username or parsed.password:
            raise ValueError("QINGHE_BACKEND_BASE_URL 必须是无凭据的 HTTP(S) 基础地址")
        self._settings = settings
        self.data_source = data_source
        self._client = httpx.AsyncClient(
            base_url=settings.qinghe_backend_base_url.rstrip("/") + "/",
            timeout=httpx.Timeout(
                connect=settings.qinghe_connect_timeout_seconds,
                read=settings.qinghe_read_timeout_seconds,
                write=settings.qinghe_write_timeout_seconds,
                pool=settings.qinghe_pool_timeout_seconds,
            ),
            follow_redirects=False,
            transport=transport,
        )

    async def close(self) -> None:
        await self._client.aclose()

    async def get(
        self,
        endpoint_name: EndpointName,
        *,
        params: dict[str, Any] | None = None,
        path_values: dict[str, int] | None = None,
        authorization_token: str | None = None,
    ) -> ParsedBackendResponse:
        endpoint = ENDPOINTS[endpoint_name]
        if endpoint.method != "GET":
            raise AgentError("AGENT_TOOL_FORBIDDEN", "仅允许只读查询工具。", False, 400)
        if endpoint.requires_auth and not authorization_token:
            raise AUTH_REQUIRED
        headers = {"Authorization": f"Bearer {authorization_token}"} if authorization_token else {}
        retries = self._settings.qinghe_max_retries
        for attempt in range(retries + 1):
            started = time.perf_counter()
            try:
                response = await self._client.get(
                    endpoint.path(**(path_values or {})), params=params or {}, headers=headers
                )
                payload = response.json()
                parsed = parse_backend_response(
                    status_code=response.status_code,
                    payload=payload,
                    request_id=response.headers.get("X-Request-ID"),
                )
                return parsed
            except (httpx.TimeoutException, httpx.ConnectError) as exc:
                if attempt < retries:
                    await asyncio.sleep(0)
                    continue
                raise map_transport_error("timeout" if isinstance(exc, httpx.TimeoutException) else "connect") from exc
            except httpx.RequestError as exc:
                raise map_transport_error("connect") from exc
            except ValueError as exc:
                raise AgentError("AGENT_BACKEND_RESPONSE_INVALID", "后端返回格式异常。", True, 502) from exc
            finally:
                _ = int((time.perf_counter() - started) * 1000)
