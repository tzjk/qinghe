# Phase 8 Agent 核心架构优化

## 审计结论

原实现已有 FastAPI 单一 HTTP 中间件、CORS、请求 ID、请求体/IP/并发限流、安全意图识别、Token Header 短生命周期透传、工具白名单/Pydantic 校验、两工具上限、Spring GET-only Client、ToolResult 字段最小化、十轮会话、Provider 超时/一次重试、显式开发降级及基础 intent/tool metrics。

原缺口是：工具选择向 Provider 发送全量 Schema；没有统一输入/输出预算、模型窗口、会话累计、usage/cost；原始近期 message 可留在内存；Provider 韧性没有断路器；路由没有 Profile 决策；指标没有安全 usage 聚合；核心编排集中于 `AgentOrchestrator`。

## 三层固定顺序

```text
HTTP: Cors -> RequestId -> BodyLimit -> RateLimit -> AccessLog -> ExceptionMapping
Agent: Safety -> Auth -> TokenBudget -> ModelRouting -> ToolGuard -> ToolResultCompression -> ResponseSanitization -> CostAccounting
Provider: Timeout -> Retry -> CircuitBreaker -> UsageCapture -> Fallback
```

HTTP 层不做模型路由；Provider 层不进入 FastAPI 路由。中间件名称、顺序和纯粹的组件定义见 `app/middleware/`，Provider decorator 位于 `app/providers/middleware.py`。

## 状态编排决定

继续使用自定义明确状态机，而不迁移 LangGraph。当前图只有 `START → safety → auth → budget → routing → tool_selection → tool_execution → result_compaction → answer_generation → usage_record → END`，没有 checkpoint、人工恢复或长任务故障恢复需求；迁移只会增加依赖与重复适配，不能简化现有 API 或测试。`AgentGraph` 保留为稳定门面，后续有真实 checkpoint 需求时才重新评估。
