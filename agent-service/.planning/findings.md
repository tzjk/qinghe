# Phase 1 发现记录

- Phase 0 八份设计文档已存在于 `agent-service/docs/`；本阶段保留其中“完全独立、Mock 默认、无业务数据访问”的边界。
- 当前可用解释器为 `D:\anaconda3\python.exe`，版本 `3.12.4`，满足“Python 3.11 或兼容版本”的要求。
- `agent-service/` 启动时仅有 docs；不存在应用代码、依赖清单、虚拟环境或 `.env`。
- 本阶段的所有测试应使用 FastAPI/HTTPX 进程内测试客户端，不连接任何实际服务或网络。

## 独立验证发现

- 初次 venv 创建因 Anaconda 的 `ensurepip` 子进程失败而未带 pip；不删除任何文件的 `venv --upgrade` 修复后可在项目内安装依赖。该问题不涉及项目源码或业务服务。
- pytest 实测 18 passed；默认 Mock Provider 和工具无 HTTPX/Socket 外部调用路径。HTTPX 仅安装为开发测试依赖，并仅用于 ASGI 进程内 API 客户端。
- Uvicorn CLI 在当前受限沙箱中因 Anaconda `ctypes` DLL 访问被拒绝而无法加载 Click；`import app.main` 成功，故问题发生在外层 CLI 依赖初始化。待受控本地启动验证，不将进程内 pytest 结果误写为 CLI 启动验证。

## Phase 1 收口

- 受控本地运行证明 Uvicorn 可在 `127.0.0.1:8100` 启动；health、ready 和优惠聊天端点均返回预期 Mock 结果。PowerShell 直接包含中文的 JSON 在该终端链路中出现编码差异，使用 JSON Unicode 转义后得到正确 `promotion_query`，应用内 pytest 的中文意图测试也通过。
- 运行时不含 Spring Boot Client 或任何数据库、Redis、OSS、外部 LLM Client；`QINGHE_BACKEND_ENABLED=false` 只是后续占位配置。HTTPX 被刻意限制为 pytest 的 ASGI 进程内客户端。
- Phase 1 可继续独立运行，但真实模型配置仍留空且不会被读取；后续接入必须由用户单独授权并从 Mock Provider/工具替换点开始。
