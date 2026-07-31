# 06. Redis Stream 可靠性审计

## 当前实现

| 能力 | 代码证据 | 状态 |
|---|---|---|
| Stream/Group/Consumer | `qh:stream:coupon:claim`、`coupon-seckill-group`、`COUPON_SECKILL_CONSUMER` 默认 `coupon-seckill-local` | 已实现；多实例若不覆盖 consumer-name 会重名 |
| Group 初始化 | `xGroupCreate(stream, group, ReadOffset.from("0-0"), true)` | 已实现，能创建空 Stream；历史消息从 0-0 可见 |
| 新消息读取 | 每 500ms scheduled，`read(Consumer, StreamReadOptions.empty().count(20), lastConsumed)` | 已实现；无 BLOCK，调度轮询 |
| ACK | `persist` 成功后 ACK；成功后删除 retry 字段 | 事务完成后 ACK，正确 |
| 失败/Pending | 异常不 ACK；每 15 秒以 `pending(...,20)` + `claim(minIdle=30s)` 接管 | 基础 Pending 恢复已实现 |
| 有限重试 | retry Hash 计数，默认 3 次 | 已实现但无退避、无 TTL、无消息级可观测性 |
| 毒消息/死信 | 第 3 次将原因写 failure Hash 后 ACK | **不是死信队列**：没有原消息/重投/消费端/保留策略 |
| 幂等 | `UserCoupon` 先查 + `uk_qh_user_coupon` + DB 条件减库存 | 已实现数据库最终兜底；ACK 丢失后再投递安全 |
| 积压/长度 | 未设置 MAXLEN/XTRIM、未采集 XLEN/XPENDING/最老 idle | 未实现 |
| 重启/宕机 | Redis 持久化策略未在 repo 配置；Pending 可在 Redis 存活时被接管 | RDB/AOF 可靠性未知；不能承诺重启后不丢 |

## 结论

**已使用 Redis Stream，但尚未达到完整可靠消息队列标准。**

它有消费者组、手动 ACK、提交后 ACK、Pending 接管、有限重试和数据库幂等；但没有真正死信 Stream、退避策略、Stream 长度治理、积压监控、唯一 consumer-name 强制校验，且 Redis AOF/RDB 持久化没有配置证据。永久失败会 ACK 原消息并留下不受 TTL 管理的 Hash，而不是可靠死信处理。

## 推荐 R3 目标

1. 成功事务后 ACK 保持不变；失败不 ACK。
2. 对达到上限的消息使用独立 `qh:stream:coupon:claim:dlq` 写入完整最小字段与失败元数据，确认 DLQ 写入成功后才 ACK 原消息；定义保留期与人工回放流程。
3. 为 retry/failure 设活动级 TTL 或转为受控 Stream；引入指数退避/下次重试时间，避免每 15 秒热循环。
4. 配置唯一 consumer name（实例 ID），监控 Stream 长度、Pending 数、最老 Pending、消费速率/失败/死信；只有有保留需求时使用 `MAXLEN ~`。
5. 在部署清单确认 Redis AOF/RDB、复制/备份与重启恢复语义。未确认前只可称“异步处理”。

