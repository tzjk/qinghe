# SSE 与 Agent 内存会话

`POST /api/v1/chat/stream` 是原有非流式 `POST /api/v1/chat` 的补充。它返回 `text/event-stream`，事件顺序为 `conversation.started`，随后是 `intent.detected`、零到两个 `tool.started`/`tool.completed`、一个或多个 `answer.delta`，最后为 `answer.completed`。安全或内部失败返回结构化 `error` 后结束；不会返回异常堆栈。

工具事件仅含 `tool_name`、`data_source` 和状态。流、普通响应和日志都不含 Bearer Token、完整请求 Header、内部 URL 或模型隐藏参数。生成器在每次输出前检查客户端断开状态并停止后续输出。

`ConversationStore` 定义 `get`、`append`、`clear` 与 `exists`。当前实现为 `InMemoryConversationStore`：每个会话最多十轮，超出时移除最旧记录；只保存用户问题、助手回答摘要、意图与工具名称，不保存 Token、API Key 或完整个人工具结果。`DELETE /api/v1/conversations/{conversation_id}` 只清理该内存，不影响任何青禾业务数据。
