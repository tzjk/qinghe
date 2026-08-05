-- 优惠券基础业务结构参考；用户已人工完成本脚本所述真实字段与索引配置，应用和 Codex 均未自动执行本脚本。
-- 本文件保留作已完成结构的审计参考；不得在当前数据库重复执行，亦不删除或重命名旧列。
-- 现有 uk_qh_user_coupon(user_id,coupon_id) 保留，本阶段 per_user_limit 只能为 1。

ALTER TABLE qh_coupon
    ADD COLUMN discount_rate DECIMAL(5,4) DEFAULT NULL COMMENT '折扣率，例如0.8500；固定金额券为空' AFTER discount_amount,
    ADD COLUMN available_stock INT NOT NULL DEFAULT 0 COMMENT '可领取库存' AFTER total_stock,
    ADD COLUMN receive_start_time DATETIME DEFAULT NULL COMMENT '领取开始时间' AFTER available_stock,
    ADD COLUMN receive_end_time DATETIME DEFAULT NULL COMMENT '领取结束时间' AFTER receive_start_time,
    ADD COLUMN use_start_time DATETIME DEFAULT NULL COMMENT '使用开始时间' AFTER receive_end_time,
    ADD COLUMN use_end_time DATETIME DEFAULT NULL COMMENT '使用结束时间' AFTER use_start_time,
    ADD COLUMN shop_id BIGINT DEFAULT NULL COMMENT '适用店铺ID' AFTER use_end_time,
    ADD COLUMN per_user_limit INT NOT NULL DEFAULT 1 COMMENT '每人限领数；当前唯一键限制为1' AFTER shop_id,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'DISABLED' COMMENT 'ENABLED或DISABLED' AFTER per_user_limit,
    ADD INDEX idx_qh_coupon_receive (status, receive_start_time, receive_end_time),
    ADD INDEX idx_qh_coupon_shop (shop_id, status);

-- 人工确认旧券规则后再执行；以下回填仅保留历史库存和旧单一有效期，不会自动启用旧券。
UPDATE qh_coupon
SET available_stock = CASE WHEN total_stock > claimed_count THEN total_stock - claimed_count ELSE 0 END,
    receive_start_time = start_time,
    receive_end_time = end_time,
    use_start_time = start_time,
    use_end_time = end_time
WHERE receive_start_time IS NULL
   OR receive_end_time IS NULL
   OR use_start_time IS NULL
   OR use_end_time IS NULL;

ALTER TABLE qh_user_coupon
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT 'AVAILABLE、LOCKED、USED、EXPIRED' AFTER coupon_status,
    ADD COLUMN receive_time DATETIME DEFAULT NULL COMMENT '领取时间' AFTER status,
    ADD COLUMN lock_time DATETIME DEFAULT NULL COMMENT '订单锁定时间' AFTER receive_time,
    ADD COLUMN expire_time DATETIME DEFAULT NULL COMMENT '使用截止时间快照' AFTER use_time,
    ADD INDEX idx_qh_user_coupon_status_expire (user_id, status, expire_time),
    ADD INDEX idx_qh_user_coupon_order (order_id);

-- 人工核对历史券后再回填；锁定/已使用状态需以历史订单事实为准。
UPDATE qh_user_coupon
SET receive_time = claim_time,
    status = CASE
        WHEN coupon_status = 'USED' THEN 'USED'
        WHEN coupon_status = 'EXPIRED' THEN 'EXPIRED'
        ELSE 'AVAILABLE'
    END
WHERE receive_time IS NULL;
