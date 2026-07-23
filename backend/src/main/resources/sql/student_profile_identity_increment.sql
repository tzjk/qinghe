-- 学生实名资料字段增量（仅供 DataGrip 人工审核和执行；本项目不会自动执行）
-- 适用前提：目标库为 qinghe_life，qh_student_profile 当前无历史数据。
-- 本次不修改 qh_user，不更新历史数据，不调整学籍版本、索引或外键。
--
-- 执行前人工复核：
-- SELECT DATABASE();
-- SHOW CREATE TABLE qh_student_profile;
-- SELECT COUNT(*) FROM qh_student_profile;
--
-- 仅当上述计数为 0 且 real_name/contact_phone 尚不存在时执行下方 ALTER。
-- 若已有资料记录，不得填充虚假姓名或联系电话；应改用独立的可空字段迁移，
-- 人工补齐后再以单独、经审核的操作收紧 NOT NULL。
ALTER TABLE qh_student_profile
    ADD COLUMN real_name VARCHAR(50) NOT NULL COMMENT '学生真实姓名' AFTER user_id,
    ADD COLUMN contact_phone VARCHAR(20) NOT NULL COMMENT '学生入住及学籍联系电话' AFTER class_name;

-- 执行后人工复核：
-- SELECT
--     column_name,
--     column_type,
--     is_nullable,
--     column_default,
--     column_comment
-- FROM information_schema.columns
-- WHERE table_schema = 'qinghe_life'
--   AND table_name = 'qh_student_profile'
--   AND column_name IN ('real_name', 'contact_phone');
