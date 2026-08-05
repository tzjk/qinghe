# qh_follow 增量 SQL 审查

结论：候选脚本仅包含 `CREATE TABLE IF NOT EXISTS qh_follow`，没有 DROP、TRUNCATE、危险 UPDATE、真实数据、密码或连接信息；MySQL 8/InnoDB/utf8mb4 兼容。主键与 `qh_user.id` 一致为 BIGINT AUTO_INCREMENT，两个关系字段均为 BIGINT，时间字段使用本功能既有的 `created_at/updated_at`。

项目没有统一 MyBatis-Plus 逻辑删除字段；`Follow` 实体也未标记逻辑删除，因此不增加 `deleted` 字段。关系物理删除与唯一键共同保证取消后可重新关注。项目现有表未强制外键，脚本延续该风格；应用层验证目标用户存在且启用。

脚本可重复运行但仅在表不存在时创建；若表已存在但定义错误，它不会修复结构。因此按“人工核验后最多执行一次”的迁移处理，不自动执行。
