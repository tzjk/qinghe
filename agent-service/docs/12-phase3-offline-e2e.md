# Phase 3 完全离线端到端模拟

本阶段的唯一后端是测试内存中的 `FakeQingheBackend`。它通过 HTTPX `MockTransport` 返回虚构的青禾 `Result(code, message, data)` 包络，并记录发送给它的请求；只有 `offline.backend` 可被该传输接受，因此不会产生真实网络连接。

完整链路为：FastAPI 请求 → Header 请求上下文 → 注入与越权检查 → 确定性 Provider 选择 → 静态工具白名单 → Pydantic 参数校验 → Spring 只读工具 → MockTransport → `Result` 解析 → `ToolResult` → Provider 最终中文回答 → FastAPI 响应。

覆盖的模拟分支包括成功、业务 401/403/404、HTTP 401/403/404/429/500、非 JSON、字段缺失、`data=null`、超时、连接失败、空数据与工具失败。所有模拟数据均为虚构数据。

`DeterministicToolCallingProvider` 不读取 Key，不调用模型，也不接收请求上下文。它仅接收用户问题、已识别意图、公开工具元数据和标准化工具结果。编排器最多执行两个工具；未知工具、参数非法、认证失败和后端错误均被标准化为失败轨迹，不能触发循环或编造业务数据。
