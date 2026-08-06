-- 限量优惠券秒杀订单增量脚本：仅供人工审核后在 qinghe_life 执行，应用不会自动导入。
-- qh_order 是普通商品订单，本表独立保存秒杀优惠券的异步受理结果。
-- 当前项目使用 qh_coupon.available_stock 作为数据库事实库存；不得把此脚本改为先查询再普通 UPDATE。

CREATE TABLE IF NOT EXISTS qh_seckill_coupon_order (
    id BIGINT NOT NULL COMMENT 'Redis 全局唯一秒杀订单 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    coupon_id BIGINT NOT NULL COMMENT '优惠券 ID',
    status VARCHAR(20) NOT NULL COMMENT 'ACCEPTED PROCESSING SUCCESS FAILED',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_qh_seckill_coupon_order_user_coupon (user_id, coupon_id),
    KEY idx_qh_seckill_coupon_order_user_time (user_id, create_time),
    KEY idx_qh_seckill_coupon_order_status_time (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='限量优惠券秒杀异步订单表';

-- 数据库消费者必须使用这一种条件更新，受影响行数不为 1 即视为库存不足：
-- UPDATE qh_coupon
-- SET available_stock = available_stock - 1, claimed_count = claimed_count + 1
-- WHERE id = ? AND status = 'ENABLED' AND available_stock > 0;
