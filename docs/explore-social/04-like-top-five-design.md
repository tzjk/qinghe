# 点赞最早五人设计

`explore:likers:{postId}` 是独立点赞用户 ZSet，member 为 userId，score 为首次点赞时间毫秒。它与热度 ZSet 完全分离。

数据库点赞插入/删除成功后，提交后分别以 `ZADD NX`/`ZREM` 维护索引。读取时按 score 升序取前五个仍存在的点赞用户，完整 `likeCount` 始终来自帖子计数。缓存不存在时按 `qh_explore_like.created_at ASC` 重建；校园规模保留完整 ZSet，UI 仅展示五人不代表只存五人。
