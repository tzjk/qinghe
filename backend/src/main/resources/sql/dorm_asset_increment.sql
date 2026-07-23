-- 青禾校园生活服务系统：宿舍入住与资产二维码管理增量结构
-- 适用数据库：MySQL 8.0.34；目标数据库：qinghe_life
-- 本文件仅供人工审核和手工执行，不由应用自动导入。

ALTER TABLE qh_building
    ADD COLUMN IF NOT EXISTS building_code VARCHAR(32) DEFAULT NULL COMMENT '楼栋编码，宿舍资产套装编号来源',
    ADD UNIQUE INDEX uk_qh_building_campus_code (campus_id, building_code);

CREATE TABLE IF NOT EXISTS qh_dorm_room (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    campus_id BIGINT NOT NULL COMMENT '所属校区ID',
    building_id BIGINT NOT NULL COMMENT '所属楼栋ID',
    room_no VARCHAR(32) NOT NULL COMMENT '寝室号',
    floor VARCHAR(20) DEFAULT NULL COMMENT '楼层快照',
    capacity INT NOT NULL DEFAULT 4 COMMENT '设计床位数',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_qh_dorm_room_building_room (building_id, room_no),
    KEY idx_qh_dorm_room_campus_building_status (campus_id, building_id, status),
    CONSTRAINT fk_qh_dorm_room_campus FOREIGN KEY (campus_id) REFERENCES qh_campus (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_qh_dorm_room_building FOREIGN KEY (building_id) REFERENCES qh_building (id) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='宿舍寝室目录';

CREATE TABLE IF NOT EXISTS qh_dorm_bed (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    dorm_room_id BIGINT NOT NULL COMMENT '寝室ID',
    bed_no VARCHAR(16) NOT NULL COMMENT '床位号，如01',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_qh_dorm_bed_room_bed (dorm_room_id, bed_no),
    KEY idx_qh_dorm_bed_room_status (dorm_room_id, status),
    CONSTRAINT fk_qh_dorm_bed_room FOREIGN KEY (dorm_room_id) REFERENCES qh_dorm_room (id) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='宿舍床位目录';

CREATE TABLE IF NOT EXISTS qh_student_profile (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    student_no VARCHAR(32) NOT NULL COMMENT '学号',
    campus_id BIGINT NOT NULL COMMENT '当前所属校区ID',
    college_name VARCHAR(100) DEFAULT NULL COMMENT '学院名称',
    major_name VARCHAR(100) DEFAULT NULL COMMENT '专业名称',
    class_name VARCHAR(100) DEFAULT NULL COMMENT '班级名称',
    student_status VARCHAR(16) NOT NULL COMMENT '学籍状态：ENROLLED、TRANSFER_MAJOR、SUSPENDED、DROPPED_OUT、REINSTATED、GRADUATED',
    effective_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '当前版本生效时间',
    end_time DATETIME DEFAULT NULL COMMENT '版本失效时间',
    current_flag TINYINT DEFAULT 1 COMMENT '当前版本标记：1当前，NULL历史',
    change_reason VARCHAR(255) DEFAULT NULL COMMENT '学籍异动原因',
    operator_admin_id BIGINT DEFAULT NULL COMMENT '办理管理员ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_qh_student_profile_user_current (user_id, current_flag),
    UNIQUE KEY uk_qh_student_profile_no_current (student_no, current_flag),
    KEY idx_qh_student_profile_campus_status (campus_id, student_status),
    KEY idx_qh_student_profile_admin_time (operator_admin_id, effective_time),
    CONSTRAINT fk_qh_student_profile_user FOREIGN KEY (user_id) REFERENCES qh_user (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_qh_student_profile_campus FOREIGN KEY (campus_id) REFERENCES qh_campus (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_qh_student_profile_admin FOREIGN KEY (operator_admin_id) REFERENCES qh_admin (id) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生学籍资料版本';

CREATE TABLE IF NOT EXISTS qh_asset_set (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    dorm_bed_id BIGINT NOT NULL COMMENT '绑定床位ID',
    asset_set_no VARCHAR(64) NOT NULL COMMENT '资产套装编号：楼栋编码加寝室号加床位号',
    qr_token CHAR(64) NOT NULL COMMENT '随机二维码令牌，不含学生信息',
    qr_status TINYINT NOT NULL DEFAULT 1 COMMENT '二维码状态：1启用，0停用',
    status VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE' COMMENT '套装状态：AVAILABLE、OCCUPIED、MAINTENANCE、RETIRED',
    qr_rotated_time DATETIME DEFAULT NULL COMMENT '二维码最近轮换时间',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_qh_asset_set_bed (dorm_bed_id),
    UNIQUE KEY uk_qh_asset_set_no (asset_set_no),
    UNIQUE KEY uk_qh_asset_set_qr_token (qr_token),
    KEY idx_qh_asset_set_status (status, qr_status),
    CONSTRAINT fk_qh_asset_set_bed FOREIGN KEY (dorm_bed_id) REFERENCES qh_dorm_bed (id) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='床位资产套装与二维码令牌';

CREATE TABLE IF NOT EXISTS qh_asset (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    asset_set_id BIGINT NOT NULL COMMENT '资产套装ID',
    asset_type VARCHAR(16) NOT NULL COMMENT '资产类型：BED、BED_BOARD、DESK、WARDROBE、STOOL',
    asset_name VARCHAR(64) NOT NULL COMMENT '资产名称',
    asset_status VARCHAR(16) NOT NULL DEFAULT 'NORMAL' COMMENT '资产状态：NORMAL、REPAIR、SCRAPPED',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_qh_asset_set_type (asset_set_id, asset_type),
    KEY idx_qh_asset_status (asset_status),
    CONSTRAINT fk_qh_asset_set FOREIGN KEY (asset_set_id) REFERENCES qh_asset_set (id) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资产套装明细';

CREATE TABLE IF NOT EXISTS qh_dorm_checkin (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '入住学生用户ID',
    student_profile_id BIGINT NOT NULL COMMENT '入住时学籍资料版本ID',
    dorm_bed_id BIGINT NOT NULL COMMENT '入住床位ID',
    asset_set_id BIGINT NOT NULL COMMENT '入住资产套装ID',
    previous_checkin_id BIGINT DEFAULT NULL COMMENT '换寝来源入住记录ID',
    checkin_source VARCHAR(16) NOT NULL COMMENT '来源：STUDENT_QR、ADMIN_MANUAL、TRANSFER_IN',
    checkin_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE、CHECKED_OUT、TRANSFERRED、CANCELLED',
    active_flag TINYINT DEFAULT 1 COMMENT '有效入住标记：1有效，NULL历史',
    student_no_snapshot VARCHAR(32) NOT NULL COMMENT '学号快照',
    student_status_snapshot VARCHAR(16) NOT NULL COMMENT '学籍状态快照',
    campus_name_snapshot VARCHAR(100) NOT NULL COMMENT '校区名称快照',
    building_code_snapshot VARCHAR(32) NOT NULL COMMENT '楼栋编码快照',
    building_name_snapshot VARCHAR(100) NOT NULL COMMENT '楼栋名称快照',
    room_no_snapshot VARCHAR(32) NOT NULL COMMENT '寝室号快照',
    bed_no_snapshot VARCHAR(16) NOT NULL COMMENT '床位号快照',
    asset_set_no_snapshot VARCHAR(64) NOT NULL COMMENT '资产套装编号快照',
    checkin_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入住确认时间',
    checkout_time DATETIME DEFAULT NULL COMMENT '退宿完成时间',
    checkout_reason VARCHAR(255) DEFAULT NULL COMMENT '退宿、换寝或批量退宿原因',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    operator_admin_id BIGINT DEFAULT NULL COMMENT '办理管理员ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_qh_dorm_checkin_user_active (user_id, active_flag),
    UNIQUE KEY uk_qh_dorm_checkin_bed_active (dorm_bed_id, active_flag),
    KEY idx_qh_dorm_checkin_student_time (student_profile_id, checkin_time),
    KEY idx_qh_dorm_checkin_asset_time (asset_set_id, checkin_time),
    KEY idx_qh_dorm_checkin_admin_time (operator_admin_id, checkin_time),
    CONSTRAINT fk_qh_dorm_checkin_user FOREIGN KEY (user_id) REFERENCES qh_user (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_qh_dorm_checkin_student_profile FOREIGN KEY (student_profile_id) REFERENCES qh_student_profile (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_qh_dorm_checkin_bed FOREIGN KEY (dorm_bed_id) REFERENCES qh_dorm_bed (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_qh_dorm_checkin_asset_set FOREIGN KEY (asset_set_id) REFERENCES qh_asset_set (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_qh_dorm_checkin_previous FOREIGN KEY (previous_checkin_id) REFERENCES qh_dorm_checkin (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_qh_dorm_checkin_admin FOREIGN KEY (operator_admin_id) REFERENCES qh_admin (id) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='宿舍入住与退宿历史';
