# 模型路由、熔断与降级

`ModelRegistry` 注册 `deterministic`、`fast`、`standard` 和 `fallback` 四个逻辑 Profile，含 provider、model、工具/流式能力、窗口、最大输出、成本层、成本字段和 enabled。默认不提供真实价格或真实 Key。

`ModelRouter` 输出 `ModelRoutingDecision`：greeting/help/safety/unsupported 使用 deterministic；单一查询走 fast；双工具/多意图走 standard；断路器打开走 fallback。用户消息不能指定 Profile。认证仍先由 AuthGate/工具门禁处理，后端数据失败不会换模型编造数据。

Provider middleware 将超时/429/5xx 作为可计失败，参数/安全拒绝不记健康失败。状态为 closed/open/half_open，阈值、恢复时间与半开探测数配置化。fallback 必含 warning；生产配置拒绝静默 fallback；确定性回答只能基于成功工具结果或安全错误。
