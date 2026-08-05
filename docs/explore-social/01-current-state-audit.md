# 当前实现审计

- 探店事实表：`qh_explore_post`；作者字段为 `user_id`，用户可见状态为 `PUBLISHED`。
- 点赞事实表：`qh_explore_like(post_id,user_id,created_at)`，已存在联合唯一键；无需增加点赞字段。
- 项目没有既有签到、关注或共同关注实现。`UserContext.getUserId()` 是本项目的登录身份来源。
- Redis namespace 由 `RedisKeys` 运行时拼接，默认开发命名空间为 `qh:dev:`；不得硬编码此前缀。
- 本阶段未执行 SQL、真实 Redis/MySQL 或真实业务写入。

## Phase 2 审查结论

- 已修复关注列表全量读取后内存分页，改由 MyBatis-Plus 数据库分页。
- 已修复 Feed 预取后按预取末项推进游标导致跳过帖子的风险；过滤后的页面可能少于请求数量，但游标只跨越实际扫描的 Redis 项。
- 已修复点赞同一 `created_at` 时 ZSet 无法按点赞记录 ID 稳定排序的问题：派生成员使用点赞 ID 的固定宽度前缀，score 仍为首次点赞时间。
