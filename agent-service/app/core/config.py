from functools import lru_cache
from pathlib import Path

from pydantic import Field, model_validator, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


AGENT_SERVICE_ROOT = Path(__file__).resolve().parents[2]


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=AGENT_SERVICE_ROOT / ".env", env_file_encoding="utf-8", extra="ignore"
    )

    agent_app_name: str = "Qinghe Campus Assistant"
    agent_env: str = "development"
    agent_host: str = "127.0.0.1"
    agent_port: int = Field(default=8100, ge=1, le=65535)
    agent_log_level: str = "INFO"
    agent_mock_mode: bool = True
    agent_tool_mode: str = "mock"
    agent_allowed_origins: str = "http://localhost:5174"
    agent_max_request_bytes: int = Field(default=16_384, ge=1_024, le=1_048_576)
    agent_max_tool_calls: int = Field(default=2, ge=1, le=2)

    # A real-model credential must be supplied through the local runtime environment.
    # Keep source defaults offline and credential-free.
    llm_provider: str = "mock"
    llm_api_key: str = ""
    llm_base_url: str = ""
    llm_model: str = ""
    llm_temperature: float = Field(default=0.2, ge=0, le=2)
    llm_timeout_seconds: int = Field(default=30, ge=1, le=120)
    llm_max_retries: int = Field(default=1, ge=0, le=1)
    llm_max_tokens: int = Field(default=1024, ge=64, le=4096)
    agent_provider_fallback_enabled: bool = False
    agent_provider_fallback: str = "deterministic"

    agent_conversation_max_turns: int = Field(default=10, ge=1, le=10)
    agent_conversation_ttl_minutes: int = Field(default=30, ge=1, le=1440)
    agent_conversation_max_count: int = Field(default=1000, ge=1, le=10_000)
    agent_rate_limit_per_minute: int = Field(default=30, ge=1, le=600)
    agent_max_concurrent_requests: int = Field(default=20, ge=1, le=200)
    agent_max_concurrent_model_calls: int = Field(default=5, ge=1, le=50)
    agent_max_concurrent_tool_calls: int = Field(default=10, ge=1, le=100)
    agent_max_sse_connections: int = Field(default=20, ge=1, le=200)

    agent_token_budget_enabled: bool = True
    agent_default_context_limit: int = Field(default=8192, ge=1024, le=131_072)
    agent_max_input_tokens: int = Field(default=6000, ge=256, le=131_072)
    agent_max_output_tokens: int = Field(default=800, ge=64, le=16_384)
    agent_history_max_turns: int = Field(default=6, ge=1, le=10)
    agent_tool_schema_max_count: int = Field(default=4, ge=0, le=15)
    agent_tool_result_max_tokens: int = Field(default=1500, ge=64, le=16_384)
    agent_conversation_max_tokens: int = Field(default=30_000, ge=1000, le=1_000_000)
    agent_token_budget_warning_ratio: float = Field(default=0.8, ge=0.1, le=1.0)
    agent_fast_profile_enabled: bool = True
    agent_standard_profile_enabled: bool = True
    agent_circuit_breaker_failure_threshold: int = Field(default=3, ge=1, le=20)
    agent_circuit_breaker_recovery_seconds: int = Field(default=30, ge=1, le=3600)
    agent_circuit_breaker_half_open_max_calls: int = Field(default=1, ge=1, le=10)
    agent_public_cache_enabled: bool = False
    agent_public_cache_ttl_seconds: int = Field(default=30, ge=1, le=3600)
    agent_public_cache_max_entries: int = Field(default=500, ge=1, le=10_000)

    qinghe_backend_enabled: bool = False
    qinghe_backend_base_url: str = "http://127.0.0.1:8090"
    qinghe_connect_timeout_seconds: int = Field(default=2, ge=1, le=30)
    qinghe_read_timeout_seconds: int = Field(default=5, ge=1, le=60)
    qinghe_write_timeout_seconds: int = Field(default=5, ge=1, le=60)
    qinghe_pool_timeout_seconds: int = Field(default=2, ge=1, le=30)
    qinghe_max_retries: int = Field(default=1, ge=0, le=1)

    @field_validator("agent_env")
    @classmethod
    def validate_environment(cls, value: str) -> str:
        normalized = value.strip().lower()
        if normalized not in {"development", "test", "production"}:
            raise ValueError("AGENT_ENV 仅支持 development、test 或 production")
        return normalized

    @field_validator("agent_tool_mode")
    @classmethod
    def validate_tool_mode(cls, value: str) -> str:
        normalized = value.strip().lower()
        if normalized not in {"mock", "spring"}:
            raise ValueError("AGENT_TOOL_MODE 仅支持 mock 或 spring")
        return normalized

    @field_validator("llm_provider")
    @classmethod
    def validate_provider(cls, value: str) -> str:
        normalized = value.strip().lower()
        if normalized not in {"mock", "deterministic", "openai_compatible"}:
            raise ValueError("LLM_PROVIDER 仅支持 mock、deterministic 或 openai_compatible")
        return normalized

    @field_validator("agent_provider_fallback")
    @classmethod
    def validate_fallback_provider(cls, value: str) -> str:
        normalized = value.strip().lower()
        if normalized != "deterministic":
            raise ValueError("AGENT_PROVIDER_FALLBACK must be deterministic")
        return normalized

    @model_validator(mode="after")
    def validate_mode_combination(self) -> "Settings":
        if self.agent_mock_mode and self.agent_tool_mode != "mock":
            raise ValueError("AGENT_MOCK_MODE=true 时 AGENT_TOOL_MODE 必须为 mock")
        if self.agent_mock_mode and self.llm_provider != "mock":
            raise ValueError("AGENT_MOCK_MODE=true 时 LLM_PROVIDER 必须为 mock")
        if self.agent_tool_mode == "spring" and not self.qinghe_backend_enabled:
            raise ValueError("AGENT_TOOL_MODE=spring 要求 QINGHE_BACKEND_ENABLED=true")
        if self.llm_provider == "openai_compatible" and not self.llm_api_key.strip():
            raise ValueError("LLM_PROVIDER=openai_compatible 时必须配置 LLM_API_KEY")
        if self.llm_provider == "openai_compatible" and not self.llm_base_url.strip():
            raise ValueError("LLM_PROVIDER=openai_compatible 时必须配置 LLM_BASE_URL")
        if self.llm_provider == "openai_compatible" and not self.llm_model.strip():
            raise ValueError("LLM_PROVIDER=openai_compatible 时必须配置 LLM_MODEL")
        if self.agent_provider_fallback_enabled and self.llm_provider != "openai_compatible":
            raise ValueError("AGENT_PROVIDER_FALLBACK_ENABLED requires openai_compatible")
        if self.agent_env == "production" and self.agent_provider_fallback_enabled:
            raise ValueError("production does not allow silent provider fallback")
        return self

    @property
    def allowed_origins(self) -> list[str]:
        return [origin.strip() for origin in self.agent_allowed_origins.split(",") if origin.strip()]


@lru_cache
def get_settings() -> Settings:
    return Settings()
