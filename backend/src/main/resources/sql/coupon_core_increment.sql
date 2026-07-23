-- 普通优惠券核心增量；仅供 DataGrip 人工审核后执行一次，禁止由应用自动导入。
-- 现有 qh_user_coupon(user_id,coupon_id) 唯一约束保留，因此第一版普通券仅允许 per_user_limit=1。
-- 执行前须确认下列列和索引均尚不存在；已有券的新时间/范围列保持 NULL，需人工补齐后才可发布。
ALTER TABLE qh_coupon
    ADD COLUMN applicable_scope VARCHAR(20) DEFAULT NULL COMMENT '适用范围 ALL或SHOP' AFTER threshold_amount,
    ADD COLUMN applicable_shop_id BIGINT DEFAULT NULL COMMENT '适用店铺ID；ALL范围为空' AFTER applicable_scope,
    ADD COLUMN discount_rate DECIMAL(5,4) DEFAULT NULL COMMENT '折扣率；固定金额券为空' AFTER discount_amount,
    ADD COLUMN per_user_limit INT DEFAULT NULL COMMENT '每人限领数量；第一版只能为1' AFTER total_stock,
    ADD COLUMN claim_start_time DATETIME DEFAULT NULL COMMENT '领取开始时间' AFTER end_time,
    ADD COLUMN claim_end_time DATETIME DEFAULT NULL COMMENT '领取结束时间' AFTER claim_start_time,
    ADD COLUMN use_start_time DATETIME DEFAULT NULL COMMENT '使用开始时间' AFTER claim_end_time,
    ADD COLUMN use_end_time DATETIME DEFAULT NULL COMMENT '使用结束时间' AFTER use_start_time,
    ADD INDEX idx_qh_coupon_publish_claim (coupon_status, claim_start_time, claim_end_time),
    ADD INDEX idx_qh_coupon_scope_shop (applicable_scope, applicable_shop_id);

ALTER TABLE qh_user_coupon
    ADD COLUMN expire_time DATETIME DEFAULT NULL COMMENT '用户券过期时间快照' AFTER use_time,
    ADD COLUMN source_type VARCHAR(20) DEFAULT NULL COMMENT '来源 NORMAL或SECKILL' AFTER coupon_status,
    ADD COLUMN source_activity_id BIGINT DEFAULT NULL COMMENT '秒杀活动ID；普通券为空' AFTER source_type,
    ADD COLUMN returned_time DATETIME DEFAULT NULL COMMENT '待支付订单取消后的退券时间' AFTER expire_time,
    ADD INDEX idx_qh_user_coupon_status_expire (user_id, coupon_status, expire_time),
    ADD INDEX idx_qh_user_coupon_order (order_id),
    ADD INDEX idx_qh_user_coupon_activity (source_activity_id);
