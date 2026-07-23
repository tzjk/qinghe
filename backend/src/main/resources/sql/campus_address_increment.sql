-- ============================================================
-- 青禾校园生活服务系统
-- 校园地址模型一次性增量迁移脚本
--
-- 适用数据库：MySQL 8.0.34
-- 目标数据库：qinghe_life
--
-- 本脚本将：
-- 1. 创建校区目录表 qh_campus
-- 2. 创建楼栋目录表 qh_building
-- 3. 扩展 qh_user_address 校园地址字段
-- 4. 为校园地址字段创建索引和外键
-- 5. 将现有省市区地址标记为 HISTORICAL
-- 6. 将旧地址内容回填至 detail
--
-- 本脚本不会：
-- 1. 删除原有地址
-- 2. 删除原有字段
-- 3. 清空任何表
-- 4. 修改用户、购物车、订单数据
-- 5. 插入校区和楼栋基础数据
--
-- 注意：本脚本按“一次性执行”设计，请勿重复完整执行。
-- ============================================================


-- 1. 指定目标数据库
USE qinghe_life;

-- 必须返回 qinghe_life
SELECT DATABASE() AS current_database;


-- ============================================================
-- 2. 执行前数据检查
-- ============================================================

-- 查看当前地址总数，执行后应保持一致
SELECT COUNT(*) AS address_count_before
FROM qh_user_address;

-- 查看迁移前地址数据
SELECT *
FROM qh_user_address
ORDER BY id;

-- 查看迁移前地址表结构
SHOW COLUMNS FROM qh_user_address;

-- 查看迁移前索引
SHOW INDEX FROM qh_user_address;


-- ============================================================
-- 3. 创建校区目录表
-- ============================================================

CREATE TABLE IF NOT EXISTS qh_campus (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',

    campus_code VARCHAR(32) DEFAULT NULL COMMENT '校区编码',

    campus_name VARCHAR(100) NOT NULL COMMENT '校区名称',

    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',

    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',

    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        COMMENT '创建时间',

    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
        COMMENT '更新时间',

    PRIMARY KEY (id),

    UNIQUE KEY uk_qh_campus_code (campus_code),

    UNIQUE KEY uk_qh_campus_name (campus_name),

    KEY idx_qh_campus_status_sort (status, sort_order)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '校园校区目录表';


-- ============================================================
-- 4. 创建楼栋目录表
-- ============================================================

CREATE TABLE IF NOT EXISTS qh_building (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',

    campus_id BIGINT NOT NULL COMMENT '所属校区ID',

    area VARCHAR(64) DEFAULT NULL COMMENT '校园区域，如生活区、教学区',

    building_type VARCHAR(32) DEFAULT NULL
        COMMENT '楼栋类型，如宿舍楼、教学楼、办公楼、实验楼',

    building_name VARCHAR(100) NOT NULL COMMENT '楼栋名称或编号',

    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',

    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',

    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        COMMENT '创建时间',

    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
        COMMENT '更新时间',

    PRIMARY KEY (id),

    UNIQUE KEY uk_qh_building_campus_name (
        campus_id,
        building_name
    ),

    KEY idx_qh_building_campus_area (
        campus_id,
        area
    ),

    KEY idx_qh_building_status_sort (
        status,
        sort_order
    ),

    CONSTRAINT fk_qh_building_campus
        FOREIGN KEY (campus_id)
        REFERENCES qh_campus (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '校园楼栋目录表';


-- ============================================================
-- 5. 扩展用户地址表
-- ============================================================

ALTER TABLE qh_user_address

    -- 原省市区字段改为可空，仅用于兼容历史数据
    MODIFY COLUMN province VARCHAR(64) DEFAULT NULL
        COMMENT '历史省字段，校园地址可为空',

    MODIFY COLUMN city VARCHAR(64) DEFAULT NULL
        COMMENT '历史市字段，校园地址可为空',

    MODIFY COLUMN district VARCHAR(64) DEFAULT NULL
        COMMENT '历史区字段，校园地址可为空',

    MODIFY COLUMN detail_address VARCHAR(255) DEFAULT NULL
        COMMENT '历史详细地址字段，校园地址可为空',

    -- 校园地址新字段
    ADD COLUMN campus_id BIGINT DEFAULT NULL
        COMMENT '校区ID'
        AFTER receiver_phone,

    ADD COLUMN area VARCHAR(64) DEFAULT NULL
        COMMENT '校园区域快照'
        AFTER campus_id,

    ADD COLUMN building_id BIGINT DEFAULT NULL
        COMMENT '楼栋ID'
        AFTER area,

    ADD COLUMN building_type VARCHAR(32) DEFAULT NULL
        COMMENT '楼栋类型快照'
        AFTER building_id,

    ADD COLUMN building_name VARCHAR(100) DEFAULT NULL
        COMMENT '楼栋名称或编号快照'
        AFTER building_type,

    ADD COLUMN floor VARCHAR(20) DEFAULT NULL
        COMMENT '楼层'
        AFTER building_name,

    ADD COLUMN room_no VARCHAR(64) DEFAULT NULL
        COMMENT '房间号或宿舍号'
        AFTER floor,

    ADD COLUMN delivery_point VARCHAR(128) DEFAULT NULL
        COMMENT '配送点或自提点'
        AFTER room_no,

    ADD COLUMN detail VARCHAR(500) DEFAULT NULL
        COMMENT '详细位置说明'
        AFTER delivery_point,

    ADD COLUMN label VARCHAR(32) DEFAULT NULL
        COMMENT '地址标签，如宿舍、教学楼、办公室'
        AFTER detail,

    ADD COLUMN remark VARCHAR(255) DEFAULT NULL
        COMMENT '配送备注'
        AFTER label,

    ADD COLUMN address_type VARCHAR(16) DEFAULT NULL
        COMMENT '地址类型：CAMPUS校园地址，HISTORICAL历史地址'
        AFTER remark;


-- ============================================================
-- 6. 创建用户地址索引
-- ============================================================

-- 用户默认地址查询索引
CREATE INDEX idx_qh_address_user_default
    ON qh_user_address (user_id, is_default);

-- 校区地址查询索引
CREATE INDEX idx_qh_address_campus
    ON qh_user_address (campus_id);

-- 楼栋地址查询索引
CREATE INDEX idx_qh_address_building
    ON qh_user_address (building_id);

-- 地址类型查询索引
CREATE INDEX idx_qh_address_type
    ON qh_user_address (address_type);


-- ============================================================
-- 7. 安全迁移历史地址
-- ============================================================

-- 将旧省市区地址拼接到新的 detail 字段。
-- 仅处理尚未标记类型、且没有校区和楼栋关联的数据。
-- 不覆盖已有 detail 内容。

UPDATE qh_user_address
SET
    detail = CASE
        WHEN detail IS NULL OR TRIM(detail) = ''
            THEN NULLIF(
                CONCAT_WS(
                    ' ',
                    NULLIF(TRIM(province), ''),
                    NULLIF(TRIM(city), ''),
                    NULLIF(TRIM(district), ''),
                    NULLIF(TRIM(detail_address), '')
                ),
                ''
            )
        ELSE detail
    END,

    address_type = 'HISTORICAL'

WHERE address_type IS NULL
  AND campus_id IS NULL
  AND building_id IS NULL;


-- ============================================================
-- 8. 添加用户地址外键
-- ============================================================

-- 校区外键
ALTER TABLE qh_user_address
    ADD CONSTRAINT fk_qh_address_campus
        FOREIGN KEY (campus_id)
        REFERENCES qh_campus (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT;

-- 楼栋外键
ALTER TABLE qh_user_address
    ADD CONSTRAINT fk_qh_address_building
        FOREIGN KEY (building_id)
        REFERENCES qh_building (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT;


-- ============================================================
-- 9. 执行后结构验证
-- ============================================================

-- 校区表结构
SHOW CREATE TABLE qh_campus;

-- 楼栋表结构
SHOW CREATE TABLE qh_building;

-- 用户地址表结构
SHOW CREATE TABLE qh_user_address;


-- ============================================================
-- 10. 执行后字段验证
-- ============================================================

SHOW COLUMNS FROM qh_user_address;


-- ============================================================
-- 11. 执行后索引验证
-- ============================================================

SHOW INDEX FROM qh_campus;

SHOW INDEX FROM qh_building;

SHOW INDEX FROM qh_user_address;


-- ============================================================
-- 12. 外键验证
-- ============================================================

SELECT
    CONSTRAINT_NAME,
    TABLE_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'qinghe_life'
  AND TABLE_NAME IN (
      'qh_building',
      'qh_user_address'
  )
  AND REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY TABLE_NAME, CONSTRAINT_NAME;


-- ============================================================
-- 13. 历史地址迁移结果验证
-- ============================================================

SELECT
    id,
    user_id,
    receiver_name,
    receiver_phone,
    province,
    city,
    district,
    detail_address,
    campus_id,
    area,
    building_id,
    building_type,
    building_name,
    floor,
    room_no,
    delivery_point,
    detail,
    label,
    remark,
    address_type,
    is_default
FROM qh_user_address
ORDER BY id;


-- ============================================================
-- 14. 数据数量验证
-- ============================================================

-- 地址总数应与执行前保持一致
SELECT COUNT(*) AS address_count_after
FROM qh_user_address;

-- 当前历史地址数量
SELECT COUNT(*) AS historical_address_count
FROM qh_user_address
WHERE address_type = 'HISTORICAL';

-- 当前校园地址数量，迁移后通常暂时为 0
SELECT COUNT(*) AS campus_address_count
FROM qh_user_address
WHERE address_type = 'CAMPUS';

-- 校区和楼栋目录当前通常为空，
-- 后续需要单独插入基础目录数据
SELECT COUNT(*) AS campus_count
FROM qh_campus;

SELECT COUNT(*) AS building_count
FROM qh_building;