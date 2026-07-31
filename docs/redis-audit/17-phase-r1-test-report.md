# Phase R1 测试报告

## 已通过（不足以完成全部验收）

- 主源码离线编译：成功，286 个源文件。
- 新增 `RedisHardeningUnitTest`：7 个纯离线用例已通过，覆盖会话 PII、namespace、指标低基数、错误分类、两份 Lua 的静态安全结构和保留策略配置。

## 未执行/待真实 Redis

本阶段没有连接 Redis、MySQL、Spring Boot 或网络，因此未执行真实 Lua、消费、ACK、DLQ、Pending 接管和数据库唯一约束集成测试。真实测试必须使用独立 namespace，设置 `QINGHE_REAL_REDIS_TESTS=true` 后再获授权执行，且不得清空任何非测试 Redis。

用户要求的 20 类行为测试尚未全部由可控 mock 覆盖；本报告不能作为 Phase R1 全量测试验收通过的依据。
