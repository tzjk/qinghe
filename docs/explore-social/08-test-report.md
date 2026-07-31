# 离线测试报告

新增社交单元测试应使用 Mockito 模拟 Mapper、Redis Template、Redisson 与 `UserContext`，默认不连接真实 MySQL/Redis。覆盖签到幂等/月份/失败、关注约束/缓存/共同关注回退、点赞最早五人及 Feed 游标/过滤/回填/裁剪。

真实 Redis/MySQL 联调只可在 `QINGHE_REAL_SOCIAL_TESTS=true` 的独立 namespace 下人工启用，默认跳过，不清理开发 Redis。本轮未执行真实联调；后端编译受本地 Maven JAR 访问拒绝阻断，结果必须以后续可读依赖环境的实际输出为准。

Phase 2 新增 `ExploreSocialPhase2StaticSafetyTest`：以 JUnit DynamicTest 检查 SQL 非破坏性、签到边界、事务/缓存约束、Feed 游标/批次/过滤、VO 脱敏及前端防重和竞态代码路径；测试只读取本仓库文件和纯值对象。`RealSocialIntegrationReservedTest` 默认禁用，不创建 Spring 上下文。

最终离线结果为 127 run、126 passed、0 failed、0 errors、1 skipped：Mockito 业务行为为 59 passed，非 Mockito 业务行为为 0 passed，DynamicTest 静态审计为 65 passed，另有 2 项普通静态 Key/VO 契约测试通过；`RealSocialIntegrationReservedTest` 为唯一受控真实入口且默认 skipped。DynamicTest 与普通静态契约均不计入 45 项业务行为目标。

因此离线测试门槛已经满足，可在人工授权的隔离 Redis/MySQL 环境进入真实联调；本报告不把该结论描述为真实联调已完成。
