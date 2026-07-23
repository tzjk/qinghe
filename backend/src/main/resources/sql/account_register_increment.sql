-- 账号注册字段一次性迁移脚本：仅供用户在 DataGrip 审核后手工执行。
-- 执行前确认当前数据库为 qinghe_life，且 qh_user 尚不存在 username、password_hash、uk_qh_user_username。
-- 本脚本不可重复执行；不包含 DROP、TRUNCATE、清表或数据删除操作。

ALTER TABLE qh_user
    ADD COLUMN username VARCHAR(32) DEFAULT NULL COMMENT '账号用户名，旧手机号用户未绑定时为空' AFTER phone,
    ADD COLUMN password_hash VARCHAR(100) DEFAULT NULL COMMENT 'BCrypt 密码哈希，旧手机号用户未绑定时为空' AFTER username;

CREATE UNIQUE INDEX uk_qh_user_username ON qh_user (username);
