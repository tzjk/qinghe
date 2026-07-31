# Phase 1 进度记录

## 2026-07-30 启动

- 已恢复 Phase 0 架构、安全、工具和待确认问题文档。
- 已确认 Python 3.12.4；尚未创建虚拟环境或安装任何依赖。
- 已建立仅位于 `agent-service/.planning/` 的阶段计划、发现和进度记录。

## 2026-07-30 骨架：配置与边界模型

- 已创建 `app/core`、`app/schemas`、`app/prompts` 的初始模块：Settings、统一 AgentError、request_id 上下文、脱敏日志辅助、聊天/健康/错误/工具 Pydantic 模型和静态安全提示词。
- 配置默认 Mock 模式、端口 8100、后端禁用、两工具上限和 16KB 请求上限；尚未创建 Provider、工具、路由、虚拟环境或安装依赖。

## 2026-07-30 骨架：Provider 与 Mock 工具

- 已实现抽象 `BaseLLMProvider`、确定性的 `MockLLMProvider` 和仅支持 Mock 的 Provider Factory；非 Mock 配置会返回统一 Provider 错误，绝不发起真实模型网络请求。
- 已实现静态 Tool Registry 和四个集中管理的虚构工具：今日优惠、商铺搜索、我的宿舍、最近订单。个人工具均使用空输入模型并拒绝 `userId`、`studentId`、学号和手机号等额外字段。

## 2026-07-30 骨架：受限编排与路由

- 已实现确定性意图分类、基础注入/越权识别、两工具硬上限、工具超时映射、Provider 错误映射、进程内指标和工具调用轨迹。
- 已实现 `/health`、`/ready`、`/api/v1/chat`、开发环境 `/api/v1/tools` 路由；应用工厂与全局异常处理尚待完成，尚未创建虚拟环境或安装依赖。

## 2026-07-30 骨架：应用、文档与测试

- 已完成应用工厂、CORS、16KB 请求体限制、统一错误响应、请求日志、README、`.env.example`、依赖清单、`.gitignore` 和 unit/integration pytest 用例。
- 配置读取路径已限定为 `agent-service/.env`（本阶段未创建该文件），避免意外读取项目根目录环境文件。下一步仅会对新源码做编译检查，再在 `agent-service/.venv` 中安装依赖并执行测试。

## 2026-07-30 虚拟环境安装阻断

- `python -m compileall -q app tests` 成功，覆盖 40 个 Python 文件。
- 已按预告命令在 `agent-service/.venv` 创建虚拟环境，但 Anaconda Python 的 `ensurepip` 子进程以退出码 1 失败，导致 `.venv` 内没有 `pip`，后续升级和项目依赖安装均未执行。未修改系统 Python、未使用管理员权限、未连接业务系统或外部模型。
- 下一步将先只读诊断基础解释器是否带 `pip` 或现有 `virtualenv`，不重复同一 venv 创建命令；若无安全的项目内替代方案，将保留源码并如实报告验证阻断。

## 2026-07-30 独立依赖与测试验证

- 基础解释器的 pip 初始化受 DLL 访问限制；未删除虚拟环境的受控修复成功，`agent-service/.venv` 已独立安装本项目及开发依赖。
- 已运行 `agent-service/.venv/Scripts/python.exe -m pytest`：18 passed，0 failed，耗时 0.15s。测试使用 HTTPX ASGITransport，不依赖网络、Spring Boot、Vue3、MySQL、Redis、OSS 或真实 LLM。
- 下一步将短暂启动仅绑定 `127.0.0.1:8100` 的 Agent Uvicorn 进程，检查 health、ready、chat 三个本地端点后停止该 Agent 进程。

## 2026-07-30 Uvicorn 首次启动错误

- 启动前已检查 8100 无监听进程；独立 Uvicorn 子进程随即以退出码 1 结束，尚未发出任何 HTTP 请求或触及外部服务。由于未保留进程输出，下一步改为直接导入 `app.main` 获取明确的本地异常，不重复同一后台启动方式。
- `app.main` 可直接导入；随后直接运行 Uvicorn CLI 得到根因：`click` 导入 Anaconda 基础 `ctypes` 时因沙箱 DLL 访问被拒绝。该异常发生在 Uvicorn 启动前，不是应用路由或依赖版本错误。将按受控权限重试仅本地 8100 的启动与端点验证；若仍失败，不再重试。

## 2026-07-30 本地进程验证（部分）

- 受控本地启动成功：`GET /health` 返回 `ok/mock=true`，`GET /ready` 返回 `ready/provider=mock/tool_count=4`；验证结束后已停止该 Agent 自身的 8100 进程。
- 首次 PowerShell 中文 JSON 字符串在该终端链路中被服务端识别为 `unsupported`，与 pytest 中的确定性优惠意图断言不一致。该结果疑似命令行编码而非应用逻辑；下一次将使用 JSON Unicode 转义字面量验证同一公开本地端点，不修改应用代码。

## 2026-07-30 本地进程验证收口

- 使用 JSON Unicode 转义重试后，`POST /api/v1/chat` 返回 `intent=promotion_query`、`tool=get_today_promotions`、`mock=true` 和原 conversation ID；再次停止该 Agent 自身进程。
- 已补充 development 环境 `/api/v1/tools` 安全元数据测试；将重新运行完整 pytest 作为最终测试证据。

## 2026-07-30 Phase 1 完成

- 最终命令 `.venv/Scripts/python.exe -m pytest`：19 passed，0 failed，0.26s；静态文件检查确认必需模块齐全、未创建 `.env`、应用目录不含 HTTPX/数据库/Redis/OSS 客户端。
- 依赖安装阶段访问了配置的 PyPI 镜像；该行为仅为用户授权的项目内依赖安装。应用、pytest 和 Uvicorn 本地验证没有访问外部 LLM、Spring Boot 8090、Vue3 5174、MySQL、Redis 或 OSS。
- 本阶段只写入 `agent-service/`：应用、测试、README、依赖/示例配置、`.venv` 与 `.planning`。未写 backend、frontend、根目录文档、数据库脚本、Git 配置或现有环境变量；未执行 Git。
