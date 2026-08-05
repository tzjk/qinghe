# 中间件与可观测性

公共精确缓存仅在启用时缓存公开优惠、商铺、热门探店和完全一致的附近坐标/半径查询；键由工具名和标准化参数组成，绝不含 Token。个人工具、地址、订单、资料、宿舍和我的优惠券永不缓存；缓存异常不影响主链路。当前实现为进程内 LRU/TTL 接口，未连接 Redis。

`ModelUsageRecord` 的安全聚合记录 request/conversation 关联、Profile、Provider/模型、Token、估算成本、工具数、fallback、时长与时间；聚合接口仅在 development 的 `GET /api/v1/metrics/usage-summary` 开放，不返回对话或个人数据。

`SafeTracer` 是 OpenTelemetry exporter 未配置时的本地/noop 预留。可用 span 为 `agent.request`、`safety.check`、`token.budget`、`model.route`、`llm.call`、`tool.call`、`tool.result.compact`、`conversation.update`、`sse.stream`；属性白名单仅含 request_id、intent、model_profile、tool_name、success、duration_ms、input_tokens、output_tokens、fallback_used、error_code。Authorization、Key、消息全文和个人 ToolResult 一律拒绝。
