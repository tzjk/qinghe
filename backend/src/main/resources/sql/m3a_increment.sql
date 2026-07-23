USE qinghe_life;

-- 本文件只供 DataGrip 人工审核和执行，不由应用自动导入。
-- 执行前请确认 qh_cart、qh_order、qh_order_item 均为 0 条记录，且仍使用 M2B 基线字段名。
-- 本脚本不可重复执行；不修改已核验一致的 qh_user_address。

ALTER TABLE qh_cart
    ADD COLUMN shop_id BIGINT NOT NULL COMMENT '商铺ID' AFTER user_id,
    ADD COLUMN selected TINYINT NOT NULL DEFAULT 1 COMMENT '是否选中 1是 0否' AFTER quantity,
    DROP COLUMN price_snapshot,
    ADD KEY idx_qh_cart_user_selected (user_id, selected),
    ADD KEY idx_qh_cart_user_shop (user_id, shop_id);

ALTER TABLE qh_order
    ADD COLUMN address_id BIGINT NOT NULL COMMENT '地址ID' AFTER shop_id,
    ADD COLUMN receiver_name VARCHAR(64) NOT NULL COMMENT '收件人快照' AFTER address_id,
    ADD COLUMN receiver_phone VARCHAR(20) NOT NULL COMMENT '收件电话快照' AFTER receiver_name,
    RENAME COLUMN address_snapshot TO delivery_address,
    RENAME COLUMN order_status TO status;

ALTER TABLE qh_order_item
    ADD COLUMN goods_image VARCHAR(255) NULL COMMENT '商品图片快照' AFTER goods_name,
    RENAME COLUMN price_snapshot TO goods_price,
    RENAME COLUMN subtotal_amount TO subtotal;
