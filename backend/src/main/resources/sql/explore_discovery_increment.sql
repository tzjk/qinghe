-- 探店与附近店铺人工迁移（MySQL 8）
-- 本脚本只允许人工审核后执行一次；应用不会自动导入。
-- 执行前必须确认：SHOW COLUMNS FROM qh_shop LIKE 'longitude'; 与 latitude 均无结果。
-- 若任一列已存在，请停止，不要重复执行 ALTER TABLE。

ALTER TABLE qh_shop
    ADD COLUMN longitude DECIMAL(9,6) NULL COMMENT '店铺经度',
    ADD COLUMN latitude DECIMAL(8,6) NULL COMMENT '店铺纬度';

CREATE TABLE IF NOT EXISTS qh_explore_post (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '发布用户ID',
    shop_id BIGINT NOT NULL COMMENT '关联店铺ID',
    title VARCHAR(100) NOT NULL COMMENT '标题',
    content TEXT NOT NULL COMMENT '内容',
    images TEXT NULL COMMENT '逗号分隔的既有OSS图片地址',
    like_count INT NOT NULL DEFAULT 0 COMMENT '点赞计数',
    comment_count INT NOT NULL DEFAULT 0 COMMENT '评论计数',
    post_status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' COMMENT 'PUBLISHED/DISABLED/DELETED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_qh_explore_post_public_time (post_status, created_at, id),
    KEY idx_qh_explore_post_shop_time (shop_id, post_status, created_at),
    KEY idx_qh_explore_post_hot (post_status, like_count, created_at),
    KEY idx_qh_explore_post_user_time (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='探店内容';

CREATE TABLE IF NOT EXISTS qh_explore_like (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    post_id BIGINT NOT NULL COMMENT '探店内容ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_qh_explore_like_post_user (post_id, user_id),
    KEY idx_qh_explore_like_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='探店点赞记录';

CREATE TABLE IF NOT EXISTS qh_explore_comment (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    post_id BIGINT NOT NULL COMMENT '探店内容ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    content VARCHAR(500) NOT NULL COMMENT '评论内容',
    comment_status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' COMMENT 'PUBLISHED/DISABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_qh_explore_comment_post_time (post_id, comment_status, created_at),
    KEY idx_qh_explore_comment_user_time (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='探店评论';

-- 执行后人工复核：SHOW CREATE TABLE qh_shop; SHOW CREATE TABLE qh_explore_post;
-- SHOW CREATE TABLE qh_explore_like; SHOW CREATE TABLE qh_explore_comment;
