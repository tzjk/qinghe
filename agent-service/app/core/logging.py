import hashlib
import logging
from typing import Any


def configure_logging(level: str) -> None:
    logging.basicConfig(
        level=getattr(logging, level.upper(), logging.INFO),
        format="%(asctime)s %(levelname)s %(name)s %(message)s",
        force=True,
    )


def safe_conversation_id(value: str | None) -> str | None:
    if not value:
        return None
    return hashlib.sha256(value.encode("utf-8")).hexdigest()[:12]


def log_event(logger: logging.Logger, event: str, **fields: Any) -> None:
    safe_fields = {key: value for key, value in fields.items() if key.lower() not in {"authorization", "token", "api_key", "message"}}
    logger.info(event, extra={"agent_fields": safe_fields})
