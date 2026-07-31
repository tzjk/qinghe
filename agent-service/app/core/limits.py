"""Process-local limits. They are deliberately replaceable by a distributed implementation later."""

import asyncio
import time
from collections import defaultdict, deque
from contextlib import asynccontextmanager
from typing import AsyncIterator

from app.core.errors import AgentError


class InMemoryLimiter:
    def __init__(
        self,
        *,
        per_minute: int,
        max_requests: int,
        max_model_calls: int,
        max_tool_calls: int,
        max_sse_connections: int,
    ) -> None:
        self._per_minute = per_minute
        self._request_times: dict[str, deque[float]] = defaultdict(deque)
        self._request_sem = asyncio.Semaphore(max_requests)
        self._model_sem = asyncio.Semaphore(max_model_calls)
        self._tool_sem = asyncio.Semaphore(max_tool_calls)
        self._conversation_locks: set[str] = set()
        self._sse_count = 0
        self._max_sse_connections = max_sse_connections
        self._guard = asyncio.Lock()

    async def admit_ip(self, client_ip: str) -> None:
        now = time.monotonic()
        async with self._guard:
            entries = self._request_times[client_ip]
            while entries and now - entries[0] >= 60:
                entries.popleft()
            if len(entries) >= self._per_minute:
                raise AgentError("AGENT_RATE_LIMITED", "请求过于频繁，请稍后再试。", True, 429)
            entries.append(now)

    @asynccontextmanager
    async def request_slot(self) -> AsyncIterator[None]:
        if self._request_sem.locked() and self._request_sem._value <= 0:  # noqa: SLF001 - deterministic local guard
            raise AgentError("AGENT_BUSY", "校园助手当前较忙，请稍后再试。", True, 429)
        await self._request_sem.acquire()
        try:
            yield
        finally:
            self._request_sem.release()

    @asynccontextmanager
    async def conversation_slot(self, conversation_id: str) -> AsyncIterator[None]:
        async with self._guard:
            if conversation_id in self._conversation_locks:
                raise AgentError("AGENT_CONVERSATION_BUSY", "该会话正在处理上一条消息，请稍后再试。", True, 429)
            self._conversation_locks.add(conversation_id)
        try:
            yield
        finally:
            async with self._guard:
                self._conversation_locks.discard(conversation_id)

    @asynccontextmanager
    async def model_slot(self) -> AsyncIterator[None]:
        if self._model_sem.locked() and self._model_sem._value <= 0:  # noqa: SLF001
            raise AgentError("AGENT_MODEL_BUSY", "回答生成繁忙，请稍后再试。", True, 429)
        await self._model_sem.acquire()
        try:
            yield
        finally:
            self._model_sem.release()

    @asynccontextmanager
    async def tool_slot(self) -> AsyncIterator[None]:
        if self._tool_sem.locked() and self._tool_sem._value <= 0:  # noqa: SLF001
            raise AgentError("AGENT_TOOL_BUSY", "查询服务繁忙，请稍后再试。", True, 429)
        await self._tool_sem.acquire()
        try:
            yield
        finally:
            self._tool_sem.release()

    @asynccontextmanager
    async def sse_slot(self) -> AsyncIterator[None]:
        await self.acquire_sse()
        try:
            yield
        finally:
            await self.release_sse()

    async def acquire_sse(self) -> None:
        async with self._guard:
            if self._sse_count >= self._max_sse_connections:
                raise AgentError("AGENT_SSE_BUSY", "当前对话连接较多，请稍后再试。", True, 429)
            self._sse_count += 1

    async def release_sse(self) -> None:
        async with self._guard:
            self._sse_count = max(0, self._sse_count - 1)

    @property
    def sse_count(self) -> int:
        return self._sse_count
