from app.providers.deterministic_tool_calling import DeterministicToolCallingProvider


class MockLLMProvider(DeterministicToolCallingProvider):
    """完全确定性的本地 Provider，不读取 Key，也不发起网络请求。"""

    name = "mock"
