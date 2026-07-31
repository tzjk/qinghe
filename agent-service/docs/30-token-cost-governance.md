# Token 与成本治理

`TokenBudgetManager` 在调用前对 System Prompt、安全规则、确定性会话摘要、滑动窗口、当前问题、所选工具 Schema 和最小 ToolResult 作保守估算；未知 tokenizer 始终标记 `estimated=true`，不冒充精确值。它校验 context window、输入硬上限、输出预留和会话累计上限，并以 `AGENT_TOKEN_BUDGET_EXCEEDED` 返回结构化安全错误。

调用后优先记录 Provider `usage`（`input_tokens`、`output_tokens`、`cached_tokens`、`total_tokens`）；Provider 未返回时使用估算。成本由 Profile 的无秘密配置决定，默认 0，仓库不写真实模型价格。

会话只保留安全 message/answer 摘要、intent 和工具名。旧轮转为确定性摘要；原始工具结果不保存，Token/API Key、完整宿舍/订单/地址不得进入摘要。清除会话同时清除累计 Token；新的 conversation_id 不会复用摘要。
