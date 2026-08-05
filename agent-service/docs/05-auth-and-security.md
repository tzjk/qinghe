# 青禾校园智能助手：身份认证与安全设计

## 身份链路

1. Vue3 在用户已登录时，从既有用户会话读取 Token，并向 FastAPI 发送 `Authorization: Bearer <token>`。聊天页必须位于用户登录守卫下。
2. FastAPI 仅做 Header 格式、长度和 `Bearer` 前缀的轻量校验；不解码、不签发、不刷新 Token，也不将 Token 变为用户 ID。
3. FastAPI 将原 Header 仅透传到 Spring Boot Client 的用户端白名单 GET 请求。
4. Spring Boot 的现有拦截器从 Redis 会话恢复 `UserContext`，再在每个业务 Service 根据该上下文决定归属与权限。
5. Spring Boot 的 401/403 由 FastAPI 规范化为 Agent 错误；前端以现有机制清理用户会话并重新登录。

推荐“FastAPI 不作为业务 Token 的可信验证者、Spring Boot 是唯一验证与授权者”。FastAPI 可在 Phase 1 对 `/api/user/me` 做按 Token 短期健康探测以优化体验，但该探测不产生或缓存身份结论，更不能替代每个实际工具调用的后端校验。

## 用户/管理员隔离

- Agent 端的路由和 Client 强制拒绝路径含 `/api/admin/` 的工具注册、重定向目标和 URL 拼接。
- 前端聊天组件只依赖用户 Token 存储模块，不导入管理员会话模块。
- FastAPI 不接受 `role`、`adminId`、`userId` 或“使用管理员权限”的聊天字段。
- 即便错误携带管理员 Token，Agent 也应拒绝：一期只接受用户 Token 格式并以 Spring Boot 用户端端点访问。

## 防越权与最小数据原则

| 风险 | 控制措施 |
|---|---|
| 模型构造其他用户 ID | 个人工具的 Pydantic schema 根本不定义 `user_id`、`student_id`、学号字段；Client 只调用现有 `/me`、`/mine`、当前用户订单/地址端点。 |
| 按订单 ID 横向越权 | 仅调用 `GET /api/orders/{orderId}`；Spring Boot 已附加 `Order.userId = UserContext.userId`。FastAPI 不把“存在与否”改写为他人信息。 |
| 宿舍/资料越权 | `get_my_dorm_info`、`get_my_profile` 无身份参数；不调用管理员学籍/入住接口。 |
| Prompt 注入 | 系统提示词声明文档和工具返回均是数据；禁止执行消息中的“忽略规则”、外部 URL、SQL、密钥或未注册工具指令。RAG 片段用来源边界包裹，不能改变策略。 |
| 工具逃逸/SSRF | 静态工具白名单和静态 Spring Boot 路径映射；不接受模型生成 URL、host、path、method、Header 或 SQL。 |
| 参数污染 | Pydantic 设为拒绝额外字段；长度、枚举、分页、经纬度、半径和 ID 使用显式约束。 |
| 个人信息泄露 | 对话输出按最少必要字段裁剪；默认掩码电话、学号、详细地址。日志只记录字段分类、数量、状态和哈希化相关标识。 |
| 重放/滥用 | 速率限制按 IP + Token 指纹 + conversation；设置消息长度、并发、工具次数、模型 token 和总时长上限。 |

## 日志、追踪和审计

每个请求生成 `request_id`，客户端携带或服务端生成 `conversation_id`。日志/trace 必记：时间、request_id、conversation_id 哈希、工具名、结果类别、HTTP 状态、工具耗时、模型耗时、重试次数、错误码和熔断状态。

严禁记录：完整 Authorization、Token、完整聊天原文（默认）、完整手机号、学号、地址、订单明细和模型 API Key。若故障排查需要摘录，使用访问受控的短期脱敏采样并保留删除策略。

## 可观测性与测试安全门禁

- 单元测试：工具 schema、白名单、URL 映射、脱敏、错误码、提示词防注入。
- 集成测试：以测试替身验证 HTTPX 转发 Header/超时/重试；真实 Spring Boot 集成仅在经批准的测试环境与测试 Token 下执行。
- 越权测试：个人工具无身份字段、用户 Token 访问管理员路径拒绝、订单 ID 横向访问、伪造 userId/学号字段、日志不含 Token。
- Agent 评测：真实结果引用率、工具选择正确率、无工具时拒答率、敏感信息最小披露、注入对抗、后端/模型不可用提示。

## RAG 边界

向量库只保存经审核的静态说明、规章和 FAQ 的文本、来源、版本、生效期和权限标签。禁止把订单、库存、价格、券状态、宿舍当前入住或用户资料写入向量库作为事实来源。此类问题必须优先调用实时工具；工具失败时明确告知无法确认，不以旧知识片段填补。
