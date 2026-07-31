# 人工联调清单

1. 先人工审核并执行 `explore_social_increment.sql`，确认 `qh_follow` 唯一键和两个索引。
2. 在独立测试用户下验证首次/重复签到、当前月/历史月/未来月。
3. 验证关注、取消关注、共同关注、关注/粉丝公开安全字段。
4. 验证点赞最早五人、取消点赞和 Redis 索引重建。
5. 验证发布投递、同时间戳游标、隐藏/删除过滤、回填、取消关注后惰性清理与容量裁剪。
6. Redis 不可用时确认签到快速失败，关注/点赞数据库成功且 Feed/共同关注走真实回退；不得 FLUSHDB 或写真实业务数据。

## SQL 人工执行清单

1. 先完成可恢复备份，并确认当前库为 `qinghe_life`。
2. 执行 `SHOW TABLES LIKE 'qh_follow'`；若已存在，停止并人工比对结构，不重复执行脚本。
3. 人工审阅 `backend/src/main/resources/sql/explore_social_increment.sql` 后只执行一次；脚本是 `CREATE TABLE IF NOT EXISTS`，但不会为错误的既有同名表补建缺失索引。
4. 执行 `SHOW CREATE TABLE qh_follow` 与 `SHOW INDEX FROM qh_follow`，确认唯一键和两条时间索引；不要插入真实测试数据。
5. 回滚优先采用应用功能开关/停止使用该表及恢复备份；`DROP TABLE` 不是默认回滚方案。

## 离线门禁结果（2026-07-31）

- 已通过 59 个 Mockito 业务行为场景、65 个 DynamicTest 静态审计以及后端 compile 和前端 build。
- 本轮没有执行本清单中的 SQL、Redis/MySQL/HTTP 步骤；真实入口仍默认 skipped。
- 只有在用户提供隔离 namespace、测试用户/数据和明确执行授权后，才可开始本清单第 1 项；不得把离线通过替代人工联调证据。
