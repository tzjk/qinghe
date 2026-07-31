# 单向关注关系设计

候选表 `qh_follow` 以 `user_id -> follow_user_id` 表示单向关系，联合唯一键是最终防重保障。应用拒绝自己关注自己，且目标必须是启用用户。MySQL 为事实源，Redis 只作派生索引。

关注/取消关注在数据库事务中完成；提交后才同步 `followings:{userId}` 和 `followers:{userId}`。提交后 Redis 失败只记录低基数指标和日志，不回滚已成功事务。

公开用户响应只使用 `PublicUserSummaryVO`，不含手机、实名、学号、宿舍、地址、密码或 Token。
