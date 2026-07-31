# 关注 Feed 设计

Feed Key 为 `feed:{userId}`，member 是 postId、score 是发布时间毫秒。发布成功提交后向作者粉丝同步写入；新关注只回填目标作者最近配置数量的 `PUBLISHED` 内容。

读取使用 `ZREVRANGEBYSCORE` 的 `maxTime + offset` 游标，按 Redis 顺序批量取帖子并过滤删除、隐藏、非公开作者及已取消关注作者。无效索引惰性删除。Redis 故障回退 MySQL：先限制关注人数，再按时间读取公开帖子，绝不返回伪造空 Feed。

取消关注后停止后续投递、仅清理目标作者最近有限帖子，并由读时事实校验处理遗留索引。每次写入后仅保留最新 `max-size`，裁剪只删派生索引，不删帖子。当前是 following feed，不是偏好推荐或机器学习推荐；超大作者混合推拉仅为后续预留。
