from app.agent.orchestrator import AgentOrchestrator
from app.agent.response_builder import AgentResult


class AgentGraph:
    """Phase 1 的有限状态流程门面，后续可替换为 LangGraph 实现。"""

    def __init__(self, orchestrator: AgentOrchestrator) -> None:
        self._orchestrator = orchestrator

    async def invoke(self, message: str, event_sink=None, conversation_context=None) -> AgentResult:
        return await self._orchestrator.handle(message, event_sink=event_sink, conversation_context=conversation_context)
