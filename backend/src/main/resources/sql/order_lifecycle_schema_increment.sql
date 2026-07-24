-- 订单生命周期候选增量；执行前需核验真实表结构。
-- 本项目通过 DataGrip 人工审核并手工执行 SQL，不使用 Flyway 或其他自动迁移。
--
-- 执行前必须在目标库手工运行并审核：
--   SHOW CREATE TABLE qh_order;
--   SHOW INDEX FROM qh_order;
-- 确认 pay_time、accepted_time、delivery_time、pay_expire_time 及
-- idx_qh_order_status_pay_expire_time 均不存在后，才可一次性执行本脚本。
-- 同时确认既有 cancel_reason、cancel_time、completed_time 不在本脚本中重复添加。
-- 本脚本不得由应用自动导入；未核验前不得向 Order 实体映射下列候选列。

ALTER TABLE qh_order
    ADD COLUMN pay_time DATETIME NULL COMMENT '支付完成时间' AFTER pay_amount,
    ADD COLUMN accepted_time DATETIME NULL COMMENT '管理员接单时间' AFTER pay_time,
    ADD COLUMN delivery_time DATETIME NULL COMMENT '开始配送时间' AFTER accepted_time,
    ADD COLUMN pay_expire_time DATETIME NULL COMMENT '待支付截止时间' AFTER delivery_time,
    ADD INDEX idx_qh_order_status_pay_expire_time (status, pay_expire_time);
