# Phase 2 代码审查

- 签到：身份只取 `UserContext`；上海时区、`dayOfMonth - 1`、SETBIT/GETBIT/BITCOUNT/BITFIELD 和未签到连续天数为 0 均已静态复核。可查询当前月及含当前月的最近 12 个月；Redis 异常为业务码 503。
- 关注：MySQL 唯一键是最终兜底；关注/取消关注事务提交后才更新 Redis。空集合依赖 loaded 标记，重建锁按用户；锁失败及 Redis 异常回退数据库。关注/粉丝列表改为数据库分页，缓存失败有低基数指标与日志。
- 点赞：`qh_explore_like` 是事实源；缓存与热榜 Key 分离。首五人按 `created_at ASC, id ASC`，同时间以内部成员前缀稳定排序；缓存失败回退数据库。
- Feed：仅 `PUBLISHED` 帖子在提交后投递；按 200 粉丝批次 Pipeline 写入 `postId`，容量裁剪保留最新项。读取端二次校验帖子状态、作者状态和当前关系，Redis 异常回退受限 MySQL 查询。

已修复的缺陷：内存关注分页、Feed 预取游标跳过、同时间点赞顺序、无批次的大粉丝 Pipeline、前端关注重复点击/页签竞态、浏览器 UTC 月份偏移。
