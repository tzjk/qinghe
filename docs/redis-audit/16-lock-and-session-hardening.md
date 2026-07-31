# 锁、会话与环境硬化

Redisson 3.27.2 的显式 lease 不会启用 watchdog，因此目录重建、订单超时、Pending 恢复均改为有限等待的 watchdog 获取方式。订单锁仍只协调多实例调度；数据库以 `PENDING_PAY + pay_expire_time` 条件更新防止已支付订单取消，且单轮最多 10 批。Pending 锁只覆盖一轮有界查询/claim，不代替消费者组与数据库幂等。

普通登录 Hash 只保存 id、username、nickname、avatarUrl 和必要状态；实名、学号、手机号掩码不再写入。旧 Token 可读，但下次登录/同步会覆盖为最小字段；业务资料继续按 userId 查询数据库。

`spring.redis.database`、host、port 和 `qinghe.redis.namespace` 可环境变量覆盖。默认 namespace 为 `qh:dev:`；切换 namespace 会使缓存和登录 Token 自然失效、要求重新登录，但不会删除旧 Key。
