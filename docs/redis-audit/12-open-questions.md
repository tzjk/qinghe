# 12. 开放问题与后续验证清单

以下问题无法从静态源码确认，本轮未连接 Redis/MySQL、未启动服务，也没有执行写操作。

1. Redis 实例的 AOF/RDB、fsync、复制、备份、TLS、ACL、淘汰策略、最大内存、慢日志和命令超时配置。
2. 开发、测试、生产是否实际共享 `192.168.100.128:6379` 的 DB 2；是否存在环境 namespace、独立实例或网络隔离。
3. 目录缓存和锁的 P95/P99 重建时长、缓存命中率、商铺商品最大数量/序列化大小、热点 QPS。
4. 秒杀预热由哪个管理端入口触发；活动结束后是否有人工清理、对账、退款/补偿和用户查询状态。
5. Stream consumer-name 在多实例部署时如何唯一化；历史 Stream/Group 是否已有遗留消息；失败 Hash 是否有人工处理渠道。
6. `qh_user_coupon` 的唯一索引和订单超时索引是否已在目标 MySQL 实际执行；SQL 文件仅为候选并非 live schema 证明。
7. 订单超时扫描的最大积压、单批耗时和多实例数量；55 秒 lease 是否由事实数据支持。
8. GEO/热榜 Redis 丢失后允许的恢复时间和 MySQL 回退容量；是否需要受控重建入口。
9. Agent 生产是否会水平扩展、是否有粘性会话或网关限流；只有这些条件成立才评估 Redis Agent state。
10. Redis 连接超时/NOAUTH 的真实运行环境是否已修复；不得通过在源码中恢复默认密码或绕过认证来“验证”。

## 后续验证方法（需单独授权）

- 只读：`INFO`、`CONFIG GET`（若权限允许）、`SLOWLOG GET`、`XINFO STREAM/GROUPS/CONSUMERS`、`XPENDING`、显式 Key 的 `TYPE/TTL/MEMORY USAGE`；不使用 `KEYS`、`FLUSH*`。
- 隔离集成：显式前缀数据，验证 Lua 失败、ACK 丢失、消费者宕机、Pending 接管、DLQ、锁租约、事务回滚和 Redis 故障。
- 压测：只在隔离 Redis/MySQL、获准的服务实例和受控测试账号下执行；记录数据再决定 Big Key、TTL、锁和 Agent Redis 化。

