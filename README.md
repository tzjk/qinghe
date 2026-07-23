# 青禾校园生活服务系统 V1.0

青禾校园生活服务系统是一个本地演示项目，采用 Spring Boot 2.7.18、MyBatis-Plus、MySQL、Redis、Vue 3 与 Vite。项目不接入真实支付、短信或云存储；SQL 仅供人工确认后执行。

## 目录

- `backend`：Java / Maven 后端
- `frontend`：Vue 3 / Vite 前端
- `deploy`：本地部署说明
- `docs`：接口、设计、协作与发布文档

## 本地开发

后端端口为 `8090`，前端端口为 `5174`。本机通过环境变量或未跟踪的本地配置提供 MySQL、Redis、OSS 等连接信息；绝不将密码、Token 或密钥提交到仓库。

在依赖已安装的环境中，基础构建命令为：

```bash
cd backend && mvn -DskipTests package
cd frontend && npm ci && npm run build
```

这些命令不替代需要真实 MySQL/Redis 的本机集成测试。构建产物不会进入 Git。

## Git 与 GitHub

采用轻量分支模型：`main` 用于稳定可演示版本，`develop` 用于日常集成，日常工作从 `feature/*`、`fix/*`、`docs/*` 等短期分支开始。禁止长期直接在 `main` 开发。

- 工作流与提交规范：[docs/GIT_WORKFLOW.md](docs/GIT_WORKFLOW.md)
- 贡献与自检：[CONTRIBUTING.md](CONTRIBUTING.md)
- 发布与版本标签：[docs/RELEASE_PROCESS.md](docs/RELEASE_PROCESS.md)
- 变更记录：[CHANGELOG.md](CHANGELOG.md)

使用 Conventional Commits，例如 `feat(order): implement atomic stock deduction`。每次变更应带上适用的测试、构建和文档记录。

GitHub Actions 的 CI 位于 `.github/workflows/ci.yml`：后端仅编译/打包，前端执行 `npm ci` 和生产构建；依赖局域网 Redis 的完整集成测试仍由本机执行。请在 GitHub 的 Actions 和 Pull Requests 页面查看状态，并为 `main`、`develop` 配置分支保护。

首次初始化、提交和推送必须先完成敏感信息审计，并获得明确授权；远程仓库优先使用 SSH，具体命令和安全要求见工作流文档。不要在命令、配置或文档中写入 PAT、SSH 私钥或其他凭据。

## 安全与忽略规则

根目录 `.gitignore` 排除本地环境文件、密钥、构建输出、日志、数据库/Redis dump、IDE 个人配置和 Codex 临时输出。`.env.example` 可以提交，但只能包含占位符。已被 Git 跟踪的文件不会因新增忽略规则而自动变安全，发现此类问题时必须停止推送并先轮换凭据。
