-- 商品店内分类增量迁移（仅供授权人员在 DataGrip 审核后手工执行）
-- 目标库：qinghe_life；本脚本不应由应用、部署脚本或自动化流程执行。
-- 执行前请确认 qh_goods_category 尚不存在，且 qh_goods.category_id 尚不存在。

CREATE TABLE IF NOT EXISTS qh_goods_category (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    shop_id BIGINT NOT NULL COMMENT '所属店铺ID',
    name VARCHAR(64) NOT NULL COMMENT '店内商品分类名称',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_qh_goods_category_shop_name (shop_id, name),
    KEY idx_qh_goods_category_shop_status_sort (shop_id, status, sort_order),
    CONSTRAINT fk_qh_goods_category_shop FOREIGN KEY (shop_id) REFERENCES qh_shop(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店内商品分类表';

ALTER TABLE qh_goods
    ADD COLUMN IF NOT EXISTS category_id BIGINT DEFAULT NULL COMMENT '店内商品分类ID，历史商品可为空' AFTER shop_id,
    ADD KEY idx_qh_goods_category (category_id),
    ADD CONSTRAINT fk_qh_goods_category FOREIGN KEY (category_id) REFERENCES qh_goods_category(id);
