import json
import logging
import time
from typing import Any

from fastapi import APIRouter, Request
from fastapi.responses import StreamingResponse

from app.conversation import ConversationTurn
from app.core.config import Settings
from app.core.errors import AgentError
from app.core.logging import log_event, safe_conversation_id
from app.core.request_context import (
    bind_conversation_context,
    bind_request_context,
    extract_bearer_token,
    get_request_context,
    reset_request_context,
)
from app.token_budget.history_compactor import safe_text
from app.schemas.chat import (
    ChatRequest,
    ChatResponse,
    ConversationClearResponse,
    ToolsResponse,
    ensure_conversation_id,
)

router = APIRouter(prefix="/api/v1", tags=["agent"])
logger = logging.getLogger(__name__)


def _record_turn(request: Request, conversation_id: str, payload: ChatRequest, result: Any) -> None:
    request.app.state.services.conversations.append(
        conversation_id,
        ConversationTurn(
            message=safe_text(payload.message, 240),
            answer_summary=safe_text(result.answer, 240),
            intent=result.intent,
            tool_names=tuple(item.tool_name for item in result.tool_calls),
        ),
    )


async def _run_chat(request: Request, payload: ChatRequest, event_sink=None) -> tuple[str, Any, int, str]:
    started = time.perf_counter()
    conversation_id = ensure_conversation_id(payload.conversation_id)
    conversation_token = bind_conversation_context(conversation_id)
    context = get_request_context()
    request_id = context.request_id if context else "unknown"
    try:
        limits = request.app.state.services.limits
        async with limits.request_slot(), limits.conversation_slot(conversation_id):
            history = request.app.state.services.conversations.context(conversation_id, request.app.state.settings.agent_history_max_turns)
            result = await request.app.state.services.graph.invoke(payload.message, event_sink=event_sink, conversation_context=history)
            _record_turn(request, conversation_id, payload, result)
    finally:
        reset_request_context(conversation_token)
    return conversation_id, result, int((time.perf_counter() - started) * 1000), request_id


@router.post("/chat", response_model=ChatResponse)
async def chat(payload: ChatRequest, request: Request) -> ChatResponse:
    conversation_id, result, duration_ms, request_id = await _run_chat(request, payload)
    log_event(
        logger,
        "agent_chat_completed",
        request_id=request_id,
        conversation_id=safe_conversation_id(conversation_id),
        endpoint="/api/v1/chat",
        intent=result.intent,
        tool_name=",".join(item.tool_name for item in result.tool_calls),
        tool_duration_ms=sum(item.duration_ms for item in result.tool_calls),
        total_duration_ms=duration_ms,
        result_status="success",
        message_length=len(payload.message),
    )
    return ChatResponse(
        request_id=request_id,
        conversation_id=conversation_id,
        answer=result.answer,
        intent=result.intent,
        tool_calls=result.tool_calls,
        mock=result.mock,
        tool_mode=request.app.state.settings.agent_tool_mode,
        provider=request.app.state.services.provider.name,
        duration_ms=duration_ms,
        warnings=result.warnings,
    )


def _sse_event(event: str, payload: dict[str, object]) -> str:
    return f"event: {event}\ndata: {json.dumps(payload, ensure_ascii=False, separators=(',', ':'))}\n\n"


@router.post("/chat/stream")
async def chat_stream(payload: ChatRequest, request: Request) -> StreamingResponse:
    outer_context = get_request_context()
    request_id = outer_context.request_id if outer_context else "unknown"
    authorization_token = extract_bearer_token(request.headers.get("Authorization"))
    conversation_id = ensure_conversation_id(payload.conversation_id)
    await request.app.state.services.limits.acquire_sse()

    async def event_generator():
        context_token = bind_request_context(
            request_id,
            authorization_token=authorization_token,
            tool_mode=request.app.state.settings.agent_tool_mode,
            mock_mode=request.app.state.settings.agent_mock_mode,
        )
        conversation_token = bind_conversation_context(conversation_id)
        queued_events: list[tuple[str, dict[str, object]]] = []

        async def sink(event: str, data: dict[str, object]) -> None:
            queued_events.append((event, data))

        try:
            limits = request.app.state.services.limits
            async with limits.request_slot(), limits.conversation_slot(conversation_id):
                yield _sse_event("conversation.started", {"request_id": request_id, "conversation_id": conversation_id})
                history = request.app.state.services.conversations.context(conversation_id, request.app.state.settings.agent_history_max_turns)
                result = await request.app.state.services.graph.invoke(payload.message, event_sink=sink, conversation_context=history)
                _record_turn(request, conversation_id, payload, result)
                for event, data in queued_events:
                    if await request.is_disconnected():
                        return
                    yield _sse_event(event, data)
                for index in range(0, len(result.answer), 24):
                    if await request.is_disconnected():
                        return
                    yield _sse_event("answer.delta", {"text": result.answer[index : index + 24]})
                yield _sse_event(
                    "answer.completed",
                    {"intent": result.intent, "tool_count": len(result.tool_calls), "warnings": result.warnings},
                )
        except AgentError as error:
            yield _sse_event("error", {"code": error.code, "message": error.message, "retryable": error.retryable})
        except Exception:
            yield _sse_event(
                "error",
                {"code": "AGENT_INTERNAL_ERROR", "message": "服务暂时不可用，请稍后重试。", "retryable": True},
            )
        finally:
            await request.app.state.services.limits.release_sse()
            reset_request_context(conversation_token)
            reset_request_context(context_token)

    return StreamingResponse(event_generator(), media_type="text/event-stream", headers={"Cache-Control": "no-cache"})


@router.delete("/conversations/{conversation_id}", response_model=ConversationClearResponse)
async def clear_conversation(conversation_id: str, request: Request) -> ConversationClearResponse:
    request.app.state.services.token_budget.clear(conversation_id)
    return ConversationClearResponse(
        conversation_id=conversation_id,
        cleared=request.app.state.services.conversations.clear(conversation_id),
    )


@router.get("/tools", response_model=ToolsResponse)
async def tools(request: Request) -> ToolsResponse:
    settings: Settings = request.app.state.settings
    if settings.agent_env != "development":
        raise AgentError("AGENT_TOOL_NOT_FOUND", "当前环境未开放工具目录。", False, 404)
    return ToolsResponse(tools=[item.model_dump() for item in request.app.state.services.registry.metadata()])


@router.get("/metrics/usage-summary")
async def usage_summary(request: Request) -> dict[str, object]:
    if request.app.state.settings.agent_env != "development":
        raise AgentError("AGENT_TOOL_NOT_FOUND", "当前环境未开放统计信息。", False, 404)
    return request.app.state.services.metrics.usage_summary()
