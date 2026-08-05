# 08. Agent Redis 扩展性分析

## 当前实现与结论

Agent 没有业务 Redis 客户端或 Key。会话、限流、SSE 数量、会话互斥、Provider 熔断、Token 累计和 Usage 均为有界的进程内状态；公共工具缓存也是可选的 30 秒进程内 LRU，默认关闭。当前本地开发和单实例生产不需要 Redis。

| 能力 | 单实例 | 多实例何时需要 Redis/替代 | 推荐抽象与禁止数据 |
|---|---|---|---|
| 会话摘要 | 进程内 30 分钟/1000 条足够 | 需要跨实例续聊或无粘性负载均衡时 | `ConversationStore` 已为 Protocol；只存已脱敏、长度受限摘要。禁止 Token、完整订单/地址/宿舍 |
| 限流/并发 | `InMemoryLimiter` 有效 | 网关不统一、多个实例需要共享配额时 | `RateLimiter` 接口 + 网关/Redis 原子窗口；不以 client IP 之外数据作隐私画像 |
| 公共查询缓存 | 进程内最简单 | 真实命中率和 Spring 后端压力证明需要共享命中时 | `PublicCache`；只缓存公共 GET 最小化结果。禁止个人查询/Bearer Token 作为 Key 或 Value |
| SSE 状态 | 进程内连接计数正确 | 需要跨节点配额/路由或广播时 | 由网关/连接层管理；不把活动 SSE 或 Token 盲目持久化到 Redis |
| Provider 熔断/Usage/预算 | 进程内适合本地单实例 | 需要全局熔断、租户/会话跨节点预算或全局运营报表时 | `ProviderHealthStore`、`UsageStore`；只存聚合、哈希化会话标识。禁止 API Key、系统 Prompt |
| 分布式锁 | 不需要 | 仅共享的幂等后台任务/精确一次协调有明确对象时 | 优先 DB 唯一约束/条件更新；不要为普通 Agent 请求加 Redisson 锁 |

## 迁移成本与建议

- 现有 `ConversationStore` 已可替换；限流、缓存、熔断、usage 尚需小接口隔离后再接分布式实现，迁移成本为中等。
- 多实例第一个更值得解决的是会话粘性/存储和全局限流，而不是缓存；公共缓存只有测得收益再共享。
- 当前不应接入 Redis，避免把 Agent 与业务 Redis DB 2、权限和 PII 风险耦合。

