# Phase 1：独立 FastAPI Agent 骨架

**目标：** 在 `agent-service/` 内创建可独立运行、默认 Mock 模式的 FastAPI Agent 原型。严禁访问或修改青禾 Spring Boot、Vue3、MySQL、Redis、OSS 及其端口。

| 阶段 | 状态 | 交付与判定 |
|---|---|---|
| 1. 隔离基线与设计恢复 | completed | 已确认 Phase 0 文档存在，Python 3.12.4 可用，且 `agent-service/` 当前仅有设计文档。 |
| 2. 项目骨架与配置 | completed | 已创建独立包、配置、错误、日志、schemas、Provider、Mock 工具与 API 路由。 |
| 3. 受限 Agent 编排与测试 | completed | 已实现确定性意图、白名单、两工具上限、注入阻断、工具轨迹和 19 项 pytest 用例。 |
| 4. 独立安装与验证 | completed | 已创建 `agent-service/.venv`、安装本项目依赖、pytest 19/0/0，并完成本地 8100 HTTP 验证。 |
| 5. 文档与隔离复核 | completed | README、静态隔离检查和运行记录已完成；等待用户审核。 |

## 固定边界

- `AGENT_MOCK_MODE=true` 为默认值；Mock Provider 与 Mock 工具不发出网络请求。
- 不引入数据库 ORM、MySQL/Redis 客户端、向量库、Celery、Kafka 或真实 LLM SDK。
- HTTPX 只作为测试客户端；应用运行不创建任何外部业务 HTTP Client。
- 只写入 `agent-service/`（包括本目录的规划记录），不写项目根目录的规划文件。
- 不执行 Git、SQL、服务控制，且不访问 8090、5174、MySQL、Redis 或 OSS。

## 已知风险

- Python 3.12.4 高于最低要求，必须使用与其兼容的依赖版本。
- 依赖安装可能受本机网络或权限限制；若阻断，仍完成源码和配置并如实记录，不改变其他项目或系统环境。

## 验证结论

- `python -m compileall -q app tests` 成功；`.venv/Scripts/python.exe -m pytest` 最终结果为 19 passed、0 failed。
- 本地 Uvicorn 在 `127.0.0.1:8100` 成功验证 `/health`、`/ready` 和使用 Unicode 转义 JSON 的 `/api/v1/chat`，验证结束即停止该 Agent 进程。
- 未创建 `.env`，未实现业务 HTTP Client；静态检查显示 HTTPX 仅位于测试和开发依赖，运行应用不使用其访问任何服务。
- pip 安装阶段访问了配置的 PyPI 镜像下载依赖；除此之外，应用、测试和本地 API 验证均未发起外部网络、LLM 或青禾业务请求。
