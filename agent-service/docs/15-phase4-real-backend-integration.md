# Phase 4.5 受控真实 Spring Boot 只读联调

## 启用条件与安全边界

- 默认仍为完全离线 Mock；真实模式必须显式设置 `AGENT_MOCK_MODE=false`、`AGENT_TOOL_MODE=spring`、`QINGHE_BACKEND_ENABLED=true`、`QINGHE_BACKEND_BASE_URL=http://127.0.0.1:8090` 和 `LLM_PROVIDER=deterministic`。
- `deterministic` 是本地规则 Provider，不要求或调用 `LLM_API_KEY`。Provider 模式与工具模式彼此独立。
- 只允许注册表中的 GET 路径；不会调用写接口或 `/api/admin/**`，不会直连 MySQL、Redis 或 OSS。
- Bearer Token 只可临时出现在到 Agent 的请求 Header 或 `QINGHE_TEST_USER_TOKEN` 进程变量中；不会写入文件、日志、回答、测试样例或文档。

## 当前验证状态（2026-07-30）

| 分层 | 状态 | 证据 |
|---|---|---|
| 离线回归 | 通过 | `.venv-clean` 下 `pytest -m "not real_backend"`：43 passed、3 deselected；`compileall app` 通过。 |
| 后端可达性 | 通过（无 Actuator） | `GET /actuator/health` 为 HTTP 404；随后公共 GET 探测证明 8090 可达。 |
| 阶段 A：公共直连 | 通过 | 6 个白名单 GET 均为 HTTP 200、业务 code 200。 |
| 阶段 B：FastAPI 公共工具 | 通过 | 8100 使用 deterministic + Spring 模式；7 个公共工具均完成 FastAPI → Provider → 8090 → ToolResult → 中文回答链路。 |
| 阶段 C：个人接口 | 等待普通用户 Token | 无 Token 与固定无效 Token 已安全拒绝；个人成功路径仍未请求、未伪造、未读取浏览器/数据库/Redis Token。 |
| 阶段 D：真实 SSE 与安全错误 | 通过（可达后端场景） | 真实 Socket 验证了正常事件顺序、结构化 error、UTF-8、安全字段、主动断开后服务继续响应。 |

## 真实后端测试门禁

真实测试文件位于 `tests/real_backend/`，带有 `@pytest.mark.real_backend`。

- 默认 `python -m pytest` 不应访问 8090；需要用 `-m "not real_backend"` 运行离线回归。
- 只有同时提供 `QINGHE_REAL_BACKEND_TESTS=true` 和命令 `python -m pytest -m real_backend` 时，才会尝试真实 8090。本轮结果为 2 passed、1 skipped、43 deselected；跳过项仅为没有 `QINGHE_TEST_USER_TOKEN` 的个人成功用例。
- 无 `QINGHE_TEST_USER_TOKEN` 时，个人接口测试自动跳过；公共测试与无 Token/固定无效 Token 的只读拒绝测试不伪造真实用户身份。

## 后续模型联调条件

已满足公共真实模型联调的前置条件：隔离运行时、离线/公共真实回归、FastAPI 公共工具链路、SSE 和无 Token 拒绝均已完成。个人成功工具仍等待普通用户 Token；本阶段没有配置或调用真实模型。
