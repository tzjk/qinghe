import re

from app.conversation import ConversationTurn


_CREDENTIAL = re.compile(r"(?i)bearer\s+[^\s,;]+")


def safe_text(value: str, limit: int = 160) -> str:
    value = _CREDENTIAL.sub("[credential redacted]", value).replace("\n", " ").strip()
    return value[:limit]


class DeterministicHistoryCompactor:
    """No model call: records intent, tool names and safe answer summaries only."""

    def compact(self, turns: tuple[ConversationTurn, ...], existing_summary: str = "") -> str:
        pieces = [existing_summary] if existing_summary else []
        for turn in turns:
            tools = "、".join(turn.tool_names) if turn.tool_names else "无工具"
            pieces.append(f"意图={turn.intent}; 工具={tools}; 结果={safe_text(turn.answer_summary, 96)}")
        return safe_text(" | ".join(piece for piece in pieces if piece), 800)
