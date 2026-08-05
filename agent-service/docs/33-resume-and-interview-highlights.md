# 青禾校园智能助手：简历与面试材料

## 1. 项目简介

青禾校园智能助手是独立 FastAPI 只读工具代理：用户 Token 只透传到 Spring GET 白名单，模型不接触身份或业务写接口。

## 2. 技术架构

FastAPI + Pydantic + HTTPX MockTransport/Fake Provider + 自定义状态机 + Token Budget + Profile Router + 进程内安全缓存 + usage/tracing seam。

## 3. 简历项目描述

设计并实现可观测、成本可控的校园智能助手编排核心：分层安全门禁、动态工具 Schema、Token 预算、确定性上下文压缩、模型路由与 Provider 熔断降级；保持 SSE 和 Spring 只读工具契约。

## 4. 5 条可量化技术亮点

仅在完成离线基准后填写：当前 50 项评测、pytest 数量、Schema/历史/结果 Token 降幅、fast path 避免调用数和缓存命中数均以 `benchmarks/results` 与命令输出为唯一来源。

## 5. 面试 30 秒介绍

我把校园助手从“能调用工具”提升为可治理编排：请求先经过安全、认证、Token 预算与路由，只发送需要的 Schema；工具结果和会话都做确定性最小化；Provider 失败通过断路器和受控降级处理，并用脱敏 usage 指标证明成本变化。

## 6. 面试 3 分钟介绍

先说明 Spring 是唯一业务事实与授权方，Agent 不解析 Token。再说明三层中间件如何把 HTTP 保护、Agent 决策和 Provider 韧性拆开；Token Budget 为系统/历史/Schema/结果分别分配预算，不能精确分词时如实标为估算；动态 Schema 将工具空间限制在意图相关的最多四个。最后说明只用离线 Fake Provider 与 MockTransport 验证，真实模型仍需受控验收。

## 7. Token 优化原理

全量工具、长历史和原始结果是输入成本主因；选择 Schema、滑窗+摘要和结果压缩分别缩减三部分，输出预留和窗口校验避免超限。

## 8. 模型路由原理

安全/问候走 deterministic，单工具走 fast，双工具或多意图走 standard；主 Provider 连续失败进入 fallback，且不允许用模型猜测后端数据。

## 9. 中间件分层原理

HTTP 只负责协议保护；Agent Pipeline 负责业务安全与决策；Provider decorators 负责调用韧性与 usage，避免把路由、重试和限额散落在路由函数。

## 10. 工具安全原理

静态 GET 白名单、无 userId 参数、Pydantic 输入、AuthGate、最多两工具、结果脱敏与 Token 不入模型/缓存/日志。

## 11. 常见追问

- 为什么不用 LangGraph？当前没有 checkpoint/recovery，显式自定义状态机更小、更易测试。
- 如何保证 Token 数准确？Provider usage 优先；不支持 tokenizer 时保守估算并标记 estimated。
- fallback 会不会幻觉？只允许模板化安全错误或真实成功工具结果，后端失败不猜测。

## 12. 已真实实现

离线 Token Budget、动态 Schema 选择、确定性摘要、Profile 决策、进程内公共缓存隔离、Provider circuit/retry/usage decorator、usage 聚合、SafeTracer seam、离线评测和基准脚本。

## 13. 仅预留，禁止写入简历

真实模型价格/Tokenizer 精确计数、真实模型调用、生产 OTel exporter、Redis 缓存、LangGraph checkpoint、真实模型故障演练和生产成本数据均未实施或验收。
