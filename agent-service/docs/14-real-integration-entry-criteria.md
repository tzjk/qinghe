# 真实联调进入条件

在进入真实联调前，必须逐项确认：

1. Phase 3 离线 pytest 与 `compileall` 均通过，且所有 Fake Backend 路径保持无网络证据。
2. 用户明确授权连接真实 Spring Boot，并确认端口、普通用户测试 Token、最小化测试数据和可接受的运行窗口。
3. 真实 Spring Boot 已由用户启动；先验证一个公共只读 GET，再验证一个当前用户只读 GET。不得接入管理员、写操作、领取券、下单或支付。
4. 真实模型的 Provider、Base URL、模型名和私有 Key 由用户在私有环境配置；不得写入仓库、文档或日志。模型接入前仍须验证 Token 不进入 Provider 输入。
5. 对真实 `Result` 包络、401/403/404/429/5xx、超时、非 JSON、空列表和 `data=null` 逐项复验，并保留实际结果。
6. 再次审计工具白名单、Pydantic Schema、Token Header 透传、SSE 输出与会话数据最小化；任何失败不得被声明为通过。

在用户给出明确书面授权前，Agent 保持离线 MockTransport 与确定性 Provider 模式。
