-- 普通订单核心增量；仅供 DataGrip 人工审核后执行一次，禁止由应用自动导入。
-- 执行前须确认 qh_order 的下列列和索引均尚不存在，并按实际存量订单人工补齐新快照字段。
ALTER TABLE qh_order
    ADD COLUMN campus_id BIGINT DEFAULT NULL COMMENT '下单时校区ID快照' AFTER address_id,
    ADD COLUMN campus_name VARCHAR(100) DEFAULT NULL COMMENT '下单时校区名称快照' AFTER campus_id,
    ADD COLUMN address_area VARCHAR(64) DEFAULT NULL COMMENT '下单时校园区域快照' AFTER campus_name,
    ADD COLUMN building_id BIGINT DEFAULT NULL COMMENT '下单时楼栋ID快照' AFTER address_area,
    ADD COLUMN building_type VARCHAR(32) DEFAULT NULL COMMENT '下单时楼栋类型快照' AFTER building_id,
    ADD COLUMN building_name VARCHAR(100) DEFAULT NULL COMMENT '下单时楼栋名称快照' AFTER building_type,
    ADD COLUMN floor VARCHAR(20) DEFAULT NULL COMMENT '下单时楼层快照' AFTER building_name,
    ADD COLUMN room_no VARCHAR(64) DEFAULT NULL COMMENT '下单时房间号快照' AFTER floor,
    ADD COLUMN delivery_point VARCHAR(128) DEFAULT NULL COMMENT '下单时配送点快照' AFTER room_no,
    ADD COLUMN address_detail VARCHAR(500) DEFAULT NULL COMMENT '下单时详细位置快照' AFTER delivery_point,
    ADD COLUMN delivery_fee DECIMAL(10,2) DEFAULT NULL COMMENT '配送费；新订单必须明确写入金额' AFTER discount_amount,
    ADD COLUMN user_coupon_id BIGINT DEFAULT NULL COMMENT '核销的用户优惠券ID' AFTER pay_amount,
    ADD COLUMN cancel_reason VARCHAR(255) DEFAULT NULL COMMENT '取消原因' AFTER remark,
    ADD COLUMN cancel_time DATETIME DEFAULT NULL COMMENT '取消时间' AFTER cancel_reason,
    ADD COLUMN completed_time DATETIME DEFAULT NULL COMMENT '完成时间' AFTER cancel_time,
    ADD UNIQUE INDEX uk_qh_order_user_coupon (user_coupon_id),
    ADD INDEX idx_qh_order_user_status_time (user_id, status, create_time),
    ADD INDEX idx_qh_order_shop_status_time (shop_id, status, create_time);
