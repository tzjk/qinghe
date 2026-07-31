import pytest

from app.core.config import Settings
from app.providers.deterministic_tool_calling import DeterministicToolCallingProvider
from app.providers.factory import create_provider


def test_deterministic_provider_and_spring_tools_are_independent() -> None:
    settings = Settings(
        agent_mock_mode=False,
        agent_tool_mode="spring",
        llm_provider="deterministic",
        qinghe_backend_enabled=True,
    )
    assert isinstance(create_provider(settings), DeterministicToolCallingProvider)
    assert settings.llm_api_key == ""


def test_spring_tools_still_require_explicit_backend_enablement() -> None:
    with pytest.raises(ValueError, match="QINGHE_BACKEND_ENABLED"):
        Settings(agent_mock_mode=False, agent_tool_mode="spring", llm_provider="deterministic")
