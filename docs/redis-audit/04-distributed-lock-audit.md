# 04. 分布式锁专项审计

## 已实现锁

| 锁 Key | 临界区 | 获取与释放 | 正确性 | 风险与替代 |
|---|---|---|---|---|
| `qh:lock:cache:shop:{id}` | 单商铺缓存重建 | `tryLock(100ms,10s)`；finally + `isHeldByCurrentThread` + `unlock` | Key 粒度正确，未误删其他线程锁，获取失败后重试/回源 | P1：显式 lease 无 watchdog，锁内 DB/序列化可能超过 10s；无须全局锁 |
| `qh:lock:cache:goods:{id}` | 单商品缓存重建 | 同上 | 正确 | 同上 |
| `qh:lock:cache:shop-goods:{id}` | 单店商品列表重建 | 同上 | 正确 | P2：锁内可能加载全店商品，Big Key 会放大租约风险 |
| `qh:lock:order:timeout-cancel` | 一次完整超时扫描循环 | `tryLock(0,55s)`；finally + 当前线程检查 | 多实例不重复运行的设计正确；失败获取直接跳过本轮 | P1：全局锁粒度过大且扫描不设总批次/时长上限，55 秒后可能重入。DB 条件更新是最终防误取消保护 |
| `qh:lock:coupon:seckill:pending-recovery` | Pending 查询、claim、DB 持久化 | `tryLock(0,10s)`；finally + 当前线程检查 | 防止多实例同时接管 Pending | P1：锁内含多条 Redis/DB 操作，10 秒不足时会提前失效；需分批或基于实际 P99 调整 |

## 未实现/不应强加的锁

- 一人一券：未使用锁；Lua 的 `SISMEMBER+DECR+SADD+XADD` 与 MySQL `uk_qh_user_coupon`/条件库存 UPDATE 才是正确主路径。不要加用户全局 Redisson 锁。
- 秒杀消息逐条消费：未使用消费锁；消费者组与数据库唯一约束应承担重复投递安全。应补强 Pending/死信而不是先加锁。
- 订单创建/库存：没有 Redis 锁；数据库条件更新、订单状态机和唯一约束更适合。
- 商铺更新、统计任务、接口防重复提交、Agent：未发现分布式锁实现。Agent 当前是单进程 `asyncio` 会话互斥，不能视为分布式锁；多实例前可抽象接口，不能缓存 Token 或将 SSE 连接锁进 Redis。
- 未发现 `synchronized`、`String.intern()` 被误当成分布式安全。

## 锁与故障/事务结论

- Redisson 与 Spring 使用同一 Redis endpoint/database；Redis 故障时锁获取抛异常，定时任务记录错误并结束，缓存读取回退 MySQL。没有“获取锁失败仍继续执行”的证据。
- 所有现有 `unlock` 都在 finally，并检查 `isHeldByCurrentThread`，未见锁误删。
- 显式传入 `leaseTime` 时 watchdog 不会续期；这是当前主要误用风险。修复顺序应是缩短临界区、限制批量，再决定使用可控 lease 或 watchdog，不能只把数值调大。

