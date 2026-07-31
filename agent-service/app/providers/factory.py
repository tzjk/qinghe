from app.core.config import Settings
from app.core.errors import AgentError
from app.providers.base import BaseLLMProvider
from app.providers.deterministic_tool_calling import DeterministicToolCallingProvider
from app.providers.mock_provider import MockLLMProvider
from app.providers.openai_compatible import OpenAICompatibleProvider
from app.providers.fallback import FallbackProvider
from app.providers.middleware import ProviderUsageCaptureMiddleware


def create_provider(settings: Settings) -> BaseLLMProvider:
    if settings.llm_provider == "mock":
        return MockLLMProvider()
    if settings.llm_provider == "deterministic":
        return DeterministicToolCallingProvider()
    if settings.llm_provider == "openai_compatible":
        primary = ProviderUsageCaptureMiddleware(OpenAICompatibleProvider(settings), settings)
        if settings.agent_provider_fallback_enabled:
            return FallbackProvider(primary, DeterministicToolCallingProvider())
        return primary
    raise AgentError(
        code="AGENT_PROVIDER_ERROR",
        message="当前配置不支持所选模型模式。",
        retryable=False,
        status_code=503,
    )
