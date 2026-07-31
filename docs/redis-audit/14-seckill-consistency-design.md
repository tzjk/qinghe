# 秒杀预留一致性设计

请求生成不含个人信息的 `orderId`，Lua 写入 `qh:coupon:seckill:reservation:{orderId}`：`orderId/userId/couponId/status/messageStatus/createdAt/streamMessageId`，默认 7 天 TTL。状态为 `RESERVED -> ENQUEUED -> PERSISTED`，死信为 `DEAD_LETTERED`。

脚本先验证 stock=String、users=Set 或不存在、meta=Hash、Stream=Stream 或不存在、reservation=Hash 或不存在，并验证活动元数据与参数。之后才 `DECR/SADD/HMSET`。`XADD` 出错时脚本内只撤销本次用户资格并恢复一次库存，所以不会对已存在的用户标记误删或重复增库存。

消息绝不先于库存预留写入；只有所有预留写入和 `XADD` 成功才返回 0。Redis 进程崩溃、AOF 策略和 OOM 下的持久化保证不在源码可证明范围，必须以实际 Redis 运维配置和隔离故障注入验证。
