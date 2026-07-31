"""Explicit names/order for Agent pipeline middleware; orchestration remains a custom state machine."""
from dataclasses import dataclass


HTTP_MIDDLEWARE_ORDER = ("CorsMiddleware", "RequestIdMiddleware", "BodyLimitMiddleware", "RateLimitMiddleware", "AccessLogMiddleware", "ExceptionMappingMiddleware")
AGENT_PIPELINE_ORDER = ("SafetyGuardMiddleware", "AuthGateMiddleware", "TokenBudgetMiddleware", "ModelRoutingMiddleware", "ToolGuardMiddleware", "ToolResultCompressionMiddleware", "ResponseSanitizationMiddleware", "CostAccountingMiddleware")
PROVIDER_MIDDLEWARE_ORDER = ("ProviderTimeoutMiddleware", "ProviderRetryMiddleware", "ProviderCircuitBreakerMiddleware", "ProviderUsageCaptureMiddleware", "ProviderFallbackMiddleware")


@dataclass(frozen=True, slots=True)
class PipelineStage:
    name: str
    layer: str


def declared_stages() -> tuple[PipelineStage, ...]:
    return tuple(PipelineStage(name, "agent") for name in AGENT_PIPELINE_ORDER) + tuple(PipelineStage(name, "provider") for name in PROVIDER_MIDDLEWARE_ORDER)


class SafetyGuardMiddleware: pass
class AuthGateMiddleware: pass
class TokenBudgetMiddleware: pass
class ModelRoutingMiddleware: pass
class ToolGuardMiddleware: pass
class ToolResultCompressionMiddleware: pass
class ResponseSanitizationMiddleware: pass
class CostAccountingMiddleware: pass
