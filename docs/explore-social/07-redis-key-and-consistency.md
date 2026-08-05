# Redis Key 与一致性

统一业务后缀：`sign:`、`followings:`、`followers:`、对应 `:loaded:`、`explore:likers:`、`feed:`。`RedisKeys` 自动拼接 namespace，Key 不放手机号、昵称、Token、宿舍或地址。

关注 Set 与点赞 ZSet 使用 loaded 标识、随机 TTL 和按 userId/postId 的细粒度 Redisson 锁防击穿；空集合同样写 loaded。共同关注 Redis 失败时做 MySQL 交集。

Redis 是派生层：关注、点赞与发帖的事实写入均先提交 MySQL，再执行 Redis 操作。指标不使用用户、帖子或 Token 作为标签，名称包括 follow、liker、feed 和 sign-in 的低基数计数。

点赞用户缓存升级为 `explore:likers:v2:`，旧派生 Key 不会被自动删除。成员编码只在缓存内部以点赞 ID 保证同时间的稳定顺序，接口仍只返回 `userId`、昵称和头像。发布 Feed 以每批 200 个粉丝 Pipeline 写入 `postId`，同时用 `ZREMRANGEBYRANK` 保留最新 `max-size` 条；作者不写入自己的关注 Feed。
