from contextlib import contextmanager
from dataclasses import dataclass
from typing import Iterator


_ALLOWED = {"request_id", "intent", "model_profile", "tool_name", "success", "duration_ms", "input_tokens", "output_tokens", "fallback_used", "error_code"}
SUPPORTED_SPANS = ("agent.request", "safety.check", "token.budget", "model.route", "llm.call", "tool.call", "tool.result.compact", "conversation.update", "sse.stream")


@dataclass(frozen=True, slots=True)
class SafeSpan:
    name: str
    attributes: dict[str, object]


class SafeTracer:
    """Noop/local tracing seam; exporter wiring is deliberately outside the request path."""
    def __init__(self) -> None:
        self.spans: list[SafeSpan] = []

    @contextmanager
    def span(self, name: str, **attributes: object) -> Iterator[None]:
        if name not in SUPPORTED_SPANS:
            raise ValueError(f"unsupported safe span: {name}")
        self.spans.append(SafeSpan(name, {key: value for key, value in attributes.items() if key in _ALLOWED}))
        yield
