# Phase R1 实施记录

## 已真实实现（源码编译通过）

- `coupon-seckill-claim.lua` 在任何写入前校验参数和五个 Key 类型；成功路径创建带 TTL 的 reservation Hash 与主 Stream 消息。
- `XADD` 使用 Lua `pcall`；失败时仅在本次 `SADD` 成功的条件下 `SREM` 并 `INCR` 回滚，删除 reservation，返回内部码 9，Java 不返回受理成功。
- 新增 reservation、每消息 retry、DLQ Stream 和原消息 ID 的 DLQ 去重索引 Key；均经 `RedisKeys` 集中生成。
- 主 Stream 不自动裁剪；配置仅定义建议长度、告警阈值与保留期，避免删除 Pending。
- 显式 lease 的三把锁改为有限 wait + Redisson watchdog；订单扫描限制每轮批次数。

## 离线验证与限制

主源码 `mvn "-Dmaven.repo.local=C:/Users/28402/.m2/repository" -DskipTests compile` 已通过。未连接 Redis/MySQL；Lua 真实 WRONGTYPE、OOM、XADD 和 XCLAIM 行为仍须隔离 Redis 联调。
