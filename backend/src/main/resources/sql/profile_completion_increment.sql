-- 青禾校园生活服务系统：首次资料完成状态增量脚本
-- 仅供用户在 DataGrip 审核后手工执行；本项目不会自动导入此脚本。
-- 执行前请确认目标库为 qinghe_life、目标表为 qh_user，且现有数据已完成备份。
-- 该变更仅新增可用于可靠后端状态判断的字段；不删除、不修改既有用户、地址、Token 或日志数据。

ALTER TABLE qh_user
    ADD COLUMN IF NOT EXISTS profile_completed TINYINT(1) NOT NULL DEFAULT 0
    COMMENT '是否已完成首次资料和校园地址：0否，1是'
    AFTER password_hash;

-- 执行后请只读复核：
-- SELECT column_name, column_type, is_nullable, column_default
-- FROM information_schema.columns
-- WHERE table_schema = 'qinghe_life' AND table_name = 'qh_user' AND column_name = 'profile_completed';
