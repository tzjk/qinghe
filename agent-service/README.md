# 青禾校园智能助手

## Phase 8

Phase 8 adds Agent-only Token Budget, deterministic context compression, dynamic (at most four) tool Schema selection, logical model profiles/routing, Provider circuit/usage decorators, optional public-only process cache and safe usage/tracing seams without changing public API/SSE or Spring read-only contracts. Run `\.venv-clean\Scripts\python.exe -m benchmarks.report` with the existing offline checks; it uses the 50 local evaluation cases and writes `benchmarks/results/` without a network/model request. See `docs/29-agent-core-optimization.md` through `docs/33-resume-and-interview-highlights.md`.

## Phase 5–6

Phase 5–6 adds a fake-transport-tested OpenAI-compatible adapter, bounded in-memory sessions and limits, 50-case offline evaluation, and the Vue `/assistant` page. Default `.env.example` remains fully offline Mock mode. Run `\.venv-clean\Scripts\python.exe -m pytest -m "not real_backend"`, `\.venv-clean\Scripts\python.exe -m evals.runner`, and the frontend production build without starting a service. See `docs/20-phase5-provider-productionization.md` through `docs/24-manual-acceptance-checklist.md`.

## Phase 3：完全离线端到端模拟

Phase 3 使用确定性的 `DeterministicToolCallingProvider`、内存 `InMemoryConversationStore` 与 HTTPX `MockTransport` 完成 FastAPI、请求上下文、安全检查、工具白名单与参数校验、Spring `Result` 解析、标准化 `ToolResult`、回答与结构化响应的完整离线链路。测试 Fake Backend 只接受 `offline.backend`，不会连接真实 Spring Boot、8090、5174、MySQL、Redis、OSS、互联网或模型。

`POST /api/v1/chat` 保持兼容，并补充 `tool_mode`、`provider`、`duration_ms` 和安全 `warnings`。`POST /api/v1/chat/stream` 以 SSE 返回 `conversation.started`、`intent.detected`、`tool.started`、`tool.completed`、`answer.delta`、`answer.completed` 或 `error`；所有事件均不含 Token、原始 Header、内部 URL 或堆栈。`DELETE /api/v1/conversations/{conversation_id}` 仅清除 Agent 的内存会话。

离线验证：

```powershell
.\.venv-clean\Scripts\python.exe -m pytest
.\.venv-clean\Scripts\python.exe -m compileall app
```

当前仍未连接真实系统，也没有真实模型 Key。真实联调的进入条件见 `docs/14-real-integration-entry-criteria.md`。

## Phase 2 状态

本目录是与青禾主系统隔离的 FastAPI Agent 服务。Phase 2 已实现 OpenAI 兼容 Provider 适配层和 Spring Boot 只读工具层；本阶段未启动服务、未配置真实 Key、未访问任何网络或主系统服务。

默认配置完全离线：`AGENT_MOCK_MODE=true`、`AGENT_TOOL_MODE=mock`、`LLM_PROVIDER=mock`、`QINGHE_BACKEND_ENABLED=false`。它使用确定性的 Mock Provider 与虚构工具数据，不需要 `LLM_API_KEY`。

## Provider 和工具模式

- `mock`：注册 Phase 1 Mock 工具，返回明确标记的虚构演示数据。
- `deterministic`：本地确定性工具选择与中文回答，不读取 Key、不创建 OpenAI Provider，也不发起模型网络请求；可与 Spring 只读工具组合。
- `spring`：只注册 Spring Boot 只读工具；要求 `AGENT_MOCK_MODE=false` 和 `QINGHE_BACKEND_ENABLED=true`。
- `mock` Provider 不发起网络请求。
- `openai_compatible` Provider 使用 OpenAI Chat Completions 兼容接口，支持普通文本与结构化工具调用；仅在明确选择该 Provider 时校验 Key、Base URL 和模型名。

真实 Spring + 本地确定性联调应使用 `.venv-clean` 与 `scripts/start_agent.ps1`；它仅在子进程中清理 Anaconda PATH 条目并设置以下配置：

```dotenv
AGENT_MOCK_MODE=false
AGENT_TOOL_MODE=spring
LLM_PROVIDER=deterministic
LLM_API_KEY=
QINGHE_BACKEND_ENABLED=true
QINGHE_BACKEND_BASE_URL=http://127.0.0.1:8090
```

`.env.example` 仍保持完全离线 Mock 默认值。不要创建或提交包含真实 Key 或 Token 的 `.env`；`.venv-clean` 是本机 Anaconda `ctypes` 冲突的隔离修复环境，原 `.venv` 保留但不再推荐用于运行服务。

## Token 透传与安全边界

`POST /api/v1/chat` 保留原请求体，并允许 `Authorization: Bearer <token>` 请求头。Token 只能从该 Header 获取：不能来自 `message`、`metadata`、工具参数或 `userId`。FastAPI 不解析 Token，不传给模型，也不记录到日志、公开响应或会话记忆；请求结束即释放请求上下文。Spring Boot 仍是身份、授权和实时业务事实的唯一判定方。

个人工具在 Spring 模式没有 Token 时返回 `AGENT_AUTH_REQUIRED`。401 映射为登录失效，403 映射为无权限。管理员 Token 不会被转换为普通用户身份，`/api/admin/**` 永不进入白名单。

所有后端调用仅能使用固定注册表中的 GET 路径，禁止完整 URL、动态路径、重定向、SQL、Shell、代码执行与写操作。工具输入使用 `extra="forbid"`、分页/ID/经纬度/半径边界；每轮最多执行两个工具，不存在自主循环。

## Spring 只读工具

公共工具：

- `get_today_promotions`：基于优惠券状态、领取窗口和库存筛选可领取优惠券。
- `get_active_coupons`
- `search_shops`
- `get_shop_detail`
- `get_shop_goods`
- `get_nearby_shops`
- `get_hot_explore_posts`

当前用户工具：

- `get_my_profile`
- `get_my_dorm_info`
- `get_my_coupon_wallet`
- `get_my_recent_orders`
- `get_my_order_detail`
- `get_my_default_address`

商品关键字搜索（作为商品搜索）、商品优惠价、促销开始/结束时间、商品级优惠适用关系及店铺优惠商品聚合尚不可用。当前后端没有可靠的商品级促销数据，因此不会把普通商品价格描述为折扣价，也不会注册 `search_goods` 或 `get_discounted_goods`。

## 后端错误映射

后端 `Result(code, message, data)` 会转换为内部 `ToolResult`。HTTP/业务 401、403、404、409、429、5xx、超时、连接失败、JSON 异常和响应结构异常都映射为安全的 `AGENT_*` 结果；不会回传 Python 堆栈或后端原始异常。仅连接和超时等瞬时传输错误最多重试一次；401 与 403 从不重试。

## 后续首次真实联调

1. 使用 `scripts/run_tests.ps1` 运行离线回归；真实公共测试须显式传入 `-RealBackend`。
2. 使用 `scripts/start_agent.ps1` 显式启动 `127.0.0.1:8100` 的确定性 Spring 只读模式。
3. 确认 Spring Boot 已由用户按既有方式运行；公共工具不需要 Token。
4. 当前用户工具仅在用户临时提供普通 Bearer Token 后验证，Token 不落盘。
5. 真实模型是后续单独阶段；本阶段不配置或调用模型服务。
