# 05. 秒杀 Lua 审计

## 实际链路

`POST /api/coupons/{couponId}/seckill-claim` → `CouponSeckillServiceImpl#claim` → `coupon-seckill-claim.lua` → Redis String 库存、Set 一人一券、Hash 活动元数据 → Stream `XADD` → 定时消费者 → `CouponSeckillPersistenceService#persist` 的 MySQL 条件减库存和 `qh_user_coupon` 唯一约束 → ACK。

## 判定

| 检查项 | 结论 |
|---|---|
| Lua 原子性 | 正常命令路径在单脚本内串行执行，活动窗口、`SISMEMBER`、库存检查、`DECR`、`SADD`、`XADD` 不会被其他 Redis 命令插入 |
| 负库存/重复请求 | 先判断 `stock <= 0`，同用户先查 Set；正常路径不会负库存或重复受理 |
| Cluster 跨槽 | **不兼容**：四个 Key 没有相同 hash tag；如将来切到 Redis Cluster，`EVAL` 会 CROSSSLOT。当前 `RedissonConfig` 是单机模式，尚未发生但需限制架构声明 |
| Redis 扣减与 XADD | 同一 Lua 调用减少常规中断窗口；但 Redis Lua 不是事务回滚，若在 `DECR/SADD` 后 `XADD` 运行时失败（如 Stream 类型错误/资源错误），前面的预留可能保留，缺乏修复记录 |
| DB 持久化 | 消费端事务先查已有 `UserCoupon`，再条件减 MySQL 库存并插入用户券；`uk_qh_user_coupon(user_id,coupon_id)` 是最终幂等兜底 |
| DB 失败补偿 | 消息保持 Pending，随后可重试；达到 3 次后只写 failure Hash 并 ACK，不回补 Redis库存/Set，导致 Redis 与 MySQL 长期少卖/不一致 |
| 预热与清理 | 有显式 `preheat`，但无活动结束 TTL、无清理任务、无重新对账；重启/Redis 丢失后需手工预热 |
| 入口限流 | 未发现秒杀 endpoint 专用限流；通用登录/业务层也没有 Redis 限流 |

## 风险分级

- **P0：永久失败后 ACK 但不做可追踪死信和库存/资格补偿。** 用户已经收到“accepted”，DB 未发券则 Redis 库存和资格仍被占用；failure Hash 不能重新投递。
- **P1：Stream Key 类型错误或 XADD 失败时 Lua 前置预留不保证回滚。** 需要设计可恢复的 reservation 状态/补偿/对账，不可仅依赖“Lua 原子”。
- **P1：无 TTL 和活动结束清理。** 库存、用户 Set、元数据累积，活动复用和容量风险增大。
- **P2：未来 Cluster 跨槽不兼容。** 若不计划 Cluster，只须在部署约束中写清；若计划 Cluster，四 Key 使用同一 `{couponId}` hash tag 后再迁移。

## 不建议

- 不应在 Lua 外再加全局/用户 Redisson 锁。
- 不应在 Redis 不可用时绕过 Lua 直接用 MySQL 争抢库存；当前快速失败更安全。

