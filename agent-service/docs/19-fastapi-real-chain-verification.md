# Phase 4.5 FastAPI 真实公共链路验证

## 运行模式

临时 FastAPI 仅绑定 `127.0.0.1:8100`，进程配置为 `AGENT_MOCK_MODE=false`、`AGENT_TOOL_MODE=spring`、`QINGHE_BACKEND_ENABLED=true`、`QINGHE_BACKEND_BASE_URL=http://127.0.0.1:8090`、`LLM_PROVIDER=deterministic` 和空 `LLM_API_KEY`。确定性 Provider 不创建 OpenAI 兼容 Provider，也不会发出外部模型请求。

## 结果

| 验证 | 实际结果 |
|---|---|
| 编译与离线回归 | `compileall app` 通过；43 passed、3 deselected。 |
| 双门禁真实 pytest | `QINGHE_REAL_BACKEND_TESTS=true` 后为 2 passed、1 skipped、43 deselected；跳过的是无普通用户 Token 的个人成功测试。 |
| FastAPI 检查 | `/health`、`/ready`、`/api/v1/tools` 通过；准备状态为 deterministic、15 工具。 |
| 公共聊天完整链路 | 优惠、可领取券、商铺、商铺详情、商铺商品、热门探店、附近商铺共 7 项通过。 |
| 错误行为 | 无 Token 个人工具、固定无效 Token、不存在商铺、坐标/半径越界和商品打折问题均安全处理；不编造商品促销。 |
| SSE 真实 Socket | 正常流含 `conversation.started`、`intent.detected`、`tool.started`、`tool.completed`、`answer.delta`、`answer.completed`；注入阻断流为 `conversation.started` 后 `error`。中文 UTF-8、`text/event-stream`、安全字段、主动断开后健康恢复均通过。 |

## 安全与清理

- 没有普通用户 Token、模型 Key 或外部模型请求。Token 未进入 Provider、会话、响应、文档或脚本。
- 没有调用写接口、`/api/admin/**`、MySQL、Redis 或 OSS；没有修改 backend、frontend、SQL 或根目录文件。
- 验证后已精确停止本轮 Uvicorn 进程，并确认 8100 不再监听；Spring Boot、MySQL 和 Redis 保持运行。
- 个人成功工具、无效真实 Token、订单越权及依赖特定自然数据状态的空/业务失败结果仍等待普通用户 Token 或后续受控条件。
