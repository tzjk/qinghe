# Redis 专项摘要（Phase R1 后）

R1 已将秒杀的 Lua 写入缺口收敛为预检、reservation 与脚本内 XADD 失败回滚；Stream 具备真正的去重 DLQ、退避与有界 Pending 接管。数据库 `uk_qh_user_coupon` 和条件库存 UPDATE 仍是最终幂等兜底，但仅有 SQL 源码证据，未做 live schema 验证。

Stream 没有自动裁剪，以免删除 Pending；现有配置给出容量与告警阈值。三处 Redisson 长任务锁改为 watchdog，订单扫描有每轮批次上限。用户 Redis 会话已删除实名、学号、手机号掩码。环境 namespace 与 Redis database 现在可配置。

离线编译和 7 个纯单元测试已通过；真实 Redis/MySQL 联调、AOF/RDB、DLQ/XADD 故障注入、XPENDING/XCLAIM 兼容性与生产指标导出仍待授权验证。
