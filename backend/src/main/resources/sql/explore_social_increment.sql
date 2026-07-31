-- Explore Social Phase candidate migration (MySQL 8). Review and execute manually only.
-- Do not run this script if qh_follow already exists. No existing data is changed.
CREATE TABLE IF NOT EXISTS qh_follow (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '关注发起用户ID',
    follow_user_id BIGINT NOT NULL COMMENT '被关注用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_qh_follow_user_target (user_id, follow_user_id),
    KEY idx_qh_follow_user_time (user_id, created_at),
    KEY idx_qh_follow_target_time (follow_user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户单向关注关系';
-- Application rejects user_id = follow_user_id; verify qh_user IDs before any manual data repair.
