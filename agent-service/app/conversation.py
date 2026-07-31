from dataclasses import dataclass, field
from datetime import datetime, timedelta, timezone
from typing import Protocol
import re


_CREDENTIAL = re.compile(r"(?i)bearer\s+[^\s,;]+")


@dataclass(frozen=True, slots=True)
class ConversationTurn:
    message: str
    answer_summary: str
    intent: str
    tool_names: tuple[str, ...]


@dataclass(frozen=True, slots=True)
class ConversationContext:
    summary: str
    recent_turns: tuple[ConversationTurn, ...]


class ConversationStore(Protocol):
    def get(self, conversation_id: str) -> tuple[ConversationTurn, ...]: ...

    def append(self, conversation_id: str, turn: ConversationTurn) -> None: ...

    def clear(self, conversation_id: str) -> bool: ...

    def exists(self, conversation_id: str) -> bool: ...

    def context(self, conversation_id: str, max_recent_turns: int) -> ConversationContext: ...


class InMemoryConversationStore:
    """Stores only user-visible summaries; credentials and raw tool results are excluded."""

    def __init__(self, max_turns: int = 10, ttl_minutes: int = 30, max_count: int = 1000) -> None:
        self._max_turns = max_turns
        self._ttl = timedelta(minutes=ttl_minutes)
        self._max_count = max_count
        self._items: dict[str, _ConversationRecord] = {}

    def _cleanup_expired(self, now: datetime | None = None) -> int:
        current = now or datetime.now(timezone.utc)
        expired = [key for key, record in self._items.items() if current - record.last_active > self._ttl]
        for key in expired:
            self._items.pop(key, None)
        return len(expired)

    def get(self, conversation_id: str) -> tuple[ConversationTurn, ...]:
        self._cleanup_expired()
        record = self._items.get(conversation_id)
        if record is None:
            return ()
        record.last_active = datetime.now(timezone.utc)
        return tuple(record.turns)

    def append(self, conversation_id: str, turn: ConversationTurn) -> None:
        self._cleanup_expired()
        if conversation_id not in self._items and len(self._items) >= self._max_count:
            oldest = min(self._items, key=lambda key: self._items[key].last_active)
            self._items.pop(oldest, None)
        turn = ConversationTurn(
            message=_safe_turn_text(turn.message),
            answer_summary=_safe_turn_text(turn.answer_summary),
            intent=turn.intent,
            tool_names=turn.tool_names,
        )
        record = self._items.setdefault(conversation_id, _ConversationRecord())
        record.turns.append(turn)
        while len(record.turns) > self._max_turns:
            old = record.turns.pop(0)
            record.summary = _append_summary(record.summary, old)
        record.last_active = datetime.now(timezone.utc)

    def clear(self, conversation_id: str) -> bool:
        return self._items.pop(conversation_id, None) is not None

    def exists(self, conversation_id: str) -> bool:
        self._cleanup_expired()
        return conversation_id in self._items

    def context(self, conversation_id: str, max_recent_turns: int) -> ConversationContext:
        self._cleanup_expired()
        record = self._items.get(conversation_id)
        if record is None:
            return ConversationContext(summary="", recent_turns=())
        record.last_active = datetime.now(timezone.utc)
        older = record.turns[:-max_recent_turns]
        summary = record.summary
        for turn in older:
            summary = _append_summary(summary, turn)
        return ConversationContext(summary=summary, recent_turns=tuple(record.turns[-max_recent_turns:]))

    def cleanup(self) -> int:
        return self._cleanup_expired()

    @property
    def count(self) -> int:
        self._cleanup_expired()
        return len(self._items)


@dataclass(slots=True)
class _ConversationRecord:
    turns: list[ConversationTurn] = field(default_factory=list)
    summary: str = ""
    last_active: datetime = field(default_factory=lambda: datetime.now(timezone.utc))


def _append_summary(existing: str, turn: ConversationTurn) -> str:
    tools = "、".join(turn.tool_names) if turn.tool_names else "无工具"
    item = f"意图={turn.intent};工具={tools};结果={turn.answer_summary[:96]}"
    return (existing + " | " + item).strip(" | ")[-800:]


def _safe_turn_text(value: str) -> str:
    return _CREDENTIAL.sub("[credential redacted]", value).replace("\n", " ").strip()[:240]
