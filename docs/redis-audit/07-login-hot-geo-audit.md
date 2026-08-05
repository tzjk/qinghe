# 07. 登录、热榜与 GEO 审计

## 登录与 Token

- Token 为 `UUID.randomUUID()` 去横线的 32 位十六进制，不可预测；普通/管理员 Key 分离，初始和滑动 TTL 均为 30 分钟，登出显式删除，`afterCompletion` 清理 User/Admin ThreadLocal。
- 会话 Hash 全部由 `StringRedisTemplate` 写字符串，未见 Long Hash 引发序列化类型异常；日志只输出掩码手机号，未发现 Token 日志。
- P1：普通会话保存 `realName`、`studentNo`，超出认证恢复所需最小集合；Redis 访问控制、TLS、备份保留策略没有仓库证据。应删去不必要 PII，并由部署保证访问隔离。
- P2：每个请求和 WebSocket 握手续期，属于滑动会话；没有绝对最大会话年龄、多端数量和强制下线策略。是否新增取决于安全要求。
- Redis 不可用时应拒绝认证/受保护 API/握手而非弱化身份校验，当前实际行为符合该原则。

## 点赞与热榜

- 点赞事实在 MySQL：`ExploreLikeMapper#insertIgnore/deleteByPostAndUser` 与数据库唯一约束 `uk_qh_blog_like`；LikeCount 也由事务内 DB 条件更新维护。Redis 不保存用户点赞状态，不存在双写点赞记录。
- `qh:zset:explore:hot` 仅保存 postId→likeCount，在事务 `afterCommit` 更新。Redis 丢失/空集合会读取 MySQL 前 500 条重建；查询异常按 MySQL like/time 排序回退。
- P2：ZSet 无 TTL/容量监控，重建使用删后逐条写，期间读会回退；没有历史淘汰/批量原子切换。它可恢复，适合当前规模；不需要把点赞改为异步落库。

## GEO

- `qh:geo:shop` 为全局商铺坐标索引；只保存 shopId 和店铺经纬度，不保存用户实时坐标。创建、更新经纬度、变更状态均在数据库提交后 add/remove；禁用、坐标缺失会移除。
- 附近查询限制半径 0.1-20km，返回后再按 DB 状态/分类过滤；Redis 类型错误/连接异常回退 MySQL 距离计算。分类变化不需迁移 Key，因为分类不在 GEO Key 分片维度中。
- P2：没有应用启动重建/管理端全量重建，Redis 丢失后会持续 MySQL 全扫描；规模变大时需受控重建任务和索引/分页基准。全局 GEO Key 是热点候选，应观测而非提前按分类拆 Key。

