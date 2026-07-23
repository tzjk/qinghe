-- 限时秒杀优惠券独立模型；仅供 DataGrip 人工审核后执行，禁止由应用自动导入。
-- 秒杀活动必须绑定专用 coupon_id，不得与普通已领取券共用，以避免 qh_user_coupon 的既有唯一约束冲突。
CREATE TABLE qh_seckill_coupon_activity (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    activity_no VARCHAR(32) NOT NULL COMMENT '活动编号',
    coupon_id BIGINT NOT NULL COMMENT '专用优惠券ID',
    stock INT NOT NULL COMMENT '数据库秒杀库存',
    per_user_limit INT NOT NULL DEFAULT 1 COMMENT '每人限购；第一版必须为1',
    activity_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT PUBLISHED STOPPED ENDED',
    start_time DATETIME NOT NULL COMMENT '秒杀开始时间',
    end_time DATETIME NOT NULL COMMENT '秒杀结束时间',
    seckill_price DECIMAL(10,2) DEFAULT NULL COMMENT '秒杀领取价或展示规则；不接入真实支付',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_qh_seckill_activity_no (activity_no),
    UNIQUE KEY uk_qh_seckill_coupon (coupon_id),
    KEY idx_qh_seckill_status_time (activity_status, start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='限时秒杀优惠券活动表';

CREATE TABLE qh_seckill_coupon_order (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    seckill_order_no VARCHAR(32) NOT NULL COMMENT '秒杀受理订单号',
    activity_id BIGINT NOT NULL COMMENT '秒杀活动ID',
    coupon_id BIGINT NOT NULL COMMENT '优惠券ID快照',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    user_coupon_id BIGINT DEFAULT NULL COMMENT '异步发放后的用户券ID',
    stream_message_id VARCHAR(64) NOT NULL COMMENT 'Redis Stream消息ID',
    order_status VARCHAR(20) NOT NULL DEFAULT 'ACCEPTED' COMMENT 'ACCEPTED ISSUED FAILED',
    failure_reason VARCHAR(255) DEFAULT NULL COMMENT '最终失败原因摘要',
    processed_time DATETIME DEFAULT NULL COMMENT '成功或最终失败处理时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_qh_seckill_order_no (seckill_order_no),
    UNIQUE KEY uk_qh_seckill_activity_user (activity_id, user_id),
    UNIQUE KEY uk_qh_seckill_stream_message (stream_message_id),
    UNIQUE KEY uk_qh_seckill_user_coupon (user_coupon_id),
    KEY idx_qh_seckill_order_user_time (user_id, create_time),
    KEY idx_qh_seckill_order_status_time (order_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='限时秒杀优惠券异步受理订单表';
