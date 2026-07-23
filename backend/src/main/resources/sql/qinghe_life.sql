CREATE DATABASE IF NOT EXISTS qinghe_life DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE qinghe_life;

CREATE TABLE IF NOT EXISTS qh_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    phone VARCHAR(20) NOT NULL COMMENT '手机号',
    username VARCHAR(32) DEFAULT NULL COMMENT '账号用户名，旧手机号用户未绑定时为空',
    password_hash VARCHAR(100) DEFAULT NULL COMMENT 'BCrypt 密码哈希，旧手机号用户未绑定时为空',
    nickname VARCHAR(64) NOT NULL COMMENT '昵称',
    avatar_url VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
    gender TINYINT NOT NULL DEFAULT 0 COMMENT '性别 0未知 1男 2女',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_qh_user_phone (phone),
    UNIQUE KEY uk_qh_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS qh_campus (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    campus_code VARCHAR(32) DEFAULT NULL COMMENT '校区编码',
    campus_name VARCHAR(100) NOT NULL COMMENT '校区名称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_qh_campus_code (campus_code),
    UNIQUE KEY uk_qh_campus_name (campus_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校园校区目录表';

CREATE TABLE IF NOT EXISTS qh_building (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    campus_id BIGINT NOT NULL COMMENT '所属校区ID',
    area VARCHAR(64) DEFAULT NULL COMMENT '校园区域',
    building_type VARCHAR(32) DEFAULT NULL COMMENT '楼栋类型',
    building_name VARCHAR(100) NOT NULL COMMENT '楼栋名称或编号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_qh_building_campus_name (campus_id, building_name),
    KEY idx_qh_building_campus_area (campus_id, area),
    CONSTRAINT fk_qh_building_campus FOREIGN KEY (campus_id) REFERENCES qh_campus(id) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校园楼栋目录表';

CREATE TABLE IF NOT EXISTS qh_user_address (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    receiver_name VARCHAR(64) NOT NULL COMMENT '收件人',
    receiver_phone VARCHAR(20) NOT NULL COMMENT '收件电话',
    province VARCHAR(64) DEFAULT NULL COMMENT '历史省字段，校园地址可为空',
    city VARCHAR(64) DEFAULT NULL COMMENT '历史市字段，校园地址可为空',
    district VARCHAR(64) DEFAULT NULL COMMENT '历史区字段，校园地址可为空',
    detail_address VARCHAR(255) DEFAULT NULL COMMENT '历史详细地址字段，校园地址可为空',
    campus_id BIGINT DEFAULT NULL COMMENT '校区ID',
    area VARCHAR(64) DEFAULT NULL COMMENT '校园区域快照',
    building_id BIGINT DEFAULT NULL COMMENT '楼栋ID',
    building_type VARCHAR(32) DEFAULT NULL COMMENT '楼栋类型快照',
    building_name VARCHAR(100) DEFAULT NULL COMMENT '楼栋名称或编号快照',
    floor VARCHAR(20) DEFAULT NULL COMMENT '楼层',
    room_no VARCHAR(64) DEFAULT NULL COMMENT '房间号或宿舍号',
    delivery_point VARCHAR(128) DEFAULT NULL COMMENT '配送点或自提点',
    detail VARCHAR(500) DEFAULT NULL COMMENT '详细位置说明',
    label VARCHAR(32) DEFAULT NULL COMMENT '地址标签',
    remark VARCHAR(255) DEFAULT NULL COMMENT '配送备注',
    address_type VARCHAR(16) DEFAULT NULL COMMENT '地址类型 CAMPUS或HISTORICAL',
    is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认 1是 0否',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_qh_address_user (user_id),
    KEY idx_qh_address_user_default (user_id, is_default),
    KEY idx_qh_address_campus (campus_id),
    KEY idx_qh_address_building (building_id),
    CONSTRAINT fk_qh_address_campus FOREIGN KEY (campus_id) REFERENCES qh_campus(id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_qh_address_building FOREIGN KEY (building_id) REFERENCES qh_building(id) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户地址表';

CREATE TABLE IF NOT EXISTS qh_admin (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    username VARCHAR(64) NOT NULL COMMENT '管理员账号',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
    display_name VARCHAR(64) NOT NULL COMMENT '显示名称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_qh_admin_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

CREATE TABLE IF NOT EXISTS qh_category (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    name VARCHAR(64) NOT NULL COMMENT '分类名称',
    icon_url VARCHAR(255) DEFAULT NULL COMMENT '图标地址',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_qh_category_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类表';

CREATE TABLE IF NOT EXISTS qh_shop (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    name VARCHAR(100) NOT NULL COMMENT '商铺名称',
    address VARCHAR(255) NOT NULL COMMENT '商铺地址',
    phone VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    score DECIMAL(3,2) NOT NULL DEFAULT 5.00 COMMENT '评分',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    is_featured TINYINT NOT NULL DEFAULT 0 COMMENT '是否推荐 1是 0否',
    cover_image VARCHAR(255) DEFAULT NULL COMMENT '封面地址',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_qh_shop_category (category_id),
    KEY idx_qh_shop_featured (is_featured, status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商铺表';

CREATE TABLE IF NOT EXISTS qh_goods (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    shop_id BIGINT NOT NULL COMMENT '商铺ID',
    name VARCHAR(100) NOT NULL COMMENT '商品名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '商品描述',
    price DECIMAL(10,2) NOT NULL COMMENT '售价',
    stock INT NOT NULL DEFAULT 0 COMMENT '库存',
    sales_count INT NOT NULL DEFAULT 0 COMMENT '销量',
    sale_status VARCHAR(16) NOT NULL DEFAULT 'ON_SALE' COMMENT '上架状态',
    cover_image VARCHAR(255) DEFAULT NULL COMMENT '封面地址',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_qh_goods_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

CREATE TABLE IF NOT EXISTS qh_cart (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    shop_id BIGINT NOT NULL COMMENT '商铺ID',
    goods_id BIGINT NOT NULL COMMENT '商品ID',
    quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
    selected TINYINT NOT NULL DEFAULT 1 COMMENT '是否选中 1是 0否',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_qh_cart_user_goods (user_id, goods_id),
    KEY idx_qh_cart_user_selected (user_id, selected)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

CREATE TABLE IF NOT EXISTS qh_order (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_no VARCHAR(32) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    shop_id BIGINT NOT NULL COMMENT '商铺ID',
    address_id BIGINT NOT NULL COMMENT '地址ID',
    receiver_name VARCHAR(64) NOT NULL COMMENT '收件人快照',
    receiver_phone VARCHAR(20) NOT NULL COMMENT '收件电话快照',
    delivery_address VARCHAR(1000) NOT NULL COMMENT '配送地址快照',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '商品总金额',
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
    pay_amount DECIMAL(10,2) NOT NULL COMMENT '应付金额',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAY' COMMENT '订单状态',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_qh_order_no (order_no),
    KEY idx_qh_order_user (user_id),
    KEY idx_qh_order_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE TABLE IF NOT EXISTS qh_order_item (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    goods_id BIGINT NOT NULL COMMENT '商品ID',
    goods_name VARCHAR(100) NOT NULL COMMENT '商品名称快照',
    goods_image VARCHAR(255) DEFAULT NULL COMMENT '商品图片快照',
    goods_price DECIMAL(10,2) NOT NULL COMMENT '商品价格快照',
    quantity INT NOT NULL COMMENT '数量',
    subtotal DECIMAL(10,2) NOT NULL COMMENT '小计金额',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_qh_order_item_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

CREATE TABLE IF NOT EXISTS qh_coupon (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    name VARCHAR(100) NOT NULL COMMENT '优惠券名称',
    coupon_type VARCHAR(32) NOT NULL COMMENT '优惠类型',
    discount_amount DECIMAL(10,2) NOT NULL COMMENT '优惠金额',
    threshold_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '使用门槛',
    total_stock INT NOT NULL DEFAULT 0 COMMENT '总库存',
    claimed_count INT NOT NULL DEFAULT 0 COMMENT '已领数量',
    coupon_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '券状态',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券表';

CREATE TABLE IF NOT EXISTS qh_user_coupon (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    coupon_id BIGINT NOT NULL COMMENT '优惠券ID',
    order_id BIGINT DEFAULT NULL COMMENT '使用订单ID',
    coupon_status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT '用户券状态',
    claim_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    use_time DATETIME DEFAULT NULL COMMENT '使用时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_qh_user_coupon (user_id, coupon_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券表';

CREATE TABLE IF NOT EXISTS qh_blog (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '发布用户ID',
    shop_id BIGINT DEFAULT NULL COMMENT '关联商铺ID',
    title VARCHAR(150) NOT NULL COMMENT '标题',
    content TEXT NOT NULL COMMENT '内容',
    cover_image VARCHAR(255) DEFAULT NULL COMMENT '封面地址',
    like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    favorite_count INT NOT NULL DEFAULT 0 COMMENT '收藏数',
    blog_status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' COMMENT '内容状态',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_qh_blog_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='探店内容表';

CREATE TABLE IF NOT EXISTS qh_comment (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    shop_id BIGINT DEFAULT NULL COMMENT '商铺ID',
    order_id BIGINT DEFAULT NULL COMMENT '订单ID',
    blog_id BIGINT DEFAULT NULL COMMENT '探店ID',
    comment_type VARCHAR(20) NOT NULL COMMENT '评论类型',
    content VARCHAR(1000) NOT NULL COMMENT '评论内容',
    score INT DEFAULT NULL COMMENT '评分 1至5',
    images TEXT DEFAULT NULL COMMENT '图片地址JSON',
    parent_id BIGINT DEFAULT NULL COMMENT '父评论ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1可见 0隐藏',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_qh_comment_order (order_id),
    KEY idx_qh_comment_shop (shop_id, status, create_time),
    KEY idx_qh_comment_blog (blog_id, status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一评论表';

CREATE TABLE IF NOT EXISTS qh_blog_like (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    blog_id BIGINT NOT NULL COMMENT '探店ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_qh_blog_like (user_id, blog_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='探店点赞表';

CREATE TABLE IF NOT EXISTS qh_blog_favorite (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    blog_id BIGINT NOT NULL COMMENT '探店ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_qh_blog_favorite (user_id, blog_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='探店收藏表';

CREATE TABLE IF NOT EXISTS qh_operate_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT DEFAULT NULL COMMENT '操作用户ID',
    module VARCHAR(64) NOT NULL COMMENT '业务模块',
    action VARCHAR(64) NOT NULL COMMENT '操作类型',
    controller_class VARCHAR(160) NOT NULL COMMENT 'Controller类名',
    controller_method VARCHAR(120) NOT NULL COMMENT 'Controller方法名',
    request_path VARCHAR(255) NOT NULL COMMENT '请求路径',
    http_method VARCHAR(16) NOT NULL COMMENT 'HTTP方法',
    request_summary TEXT DEFAULT NULL COMMENT '请求参数脱敏摘要',
    response_summary TEXT DEFAULT NULL COMMENT '返回结果脱敏摘要',
    success TINYINT NOT NULL COMMENT '执行结果 1成功 0失败',
    exception_summary VARCHAR(500) DEFAULT NULL COMMENT '异常脱敏摘要',
    duration_ms BIGINT NOT NULL COMMENT '执行耗时毫秒',
    ip VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
    operate_time DATETIME NOT NULL COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_qh_operate_log_user_time (user_id, operate_time),
    KEY idx_qh_operate_log_module_time (module, operate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

INSERT INTO qh_user (phone, nickname, status) VALUES
('13800000001', '青禾同学', 1);

INSERT INTO qh_admin (username, password_hash, display_name, status) VALUES
('admin', '$2a$10$M1skeletonPasswordHashOnlyForDemo', '系统管理员', 1);

INSERT INTO qh_category (name, icon_url, sort_order, status) VALUES
('美食', '/images/category-food.png', 1, 1),
('便利服务', '/images/category-service.png', 2, 1);

INSERT INTO qh_shop (category_id, name, address, phone, score, status, is_featured, cover_image, sort_order) VALUES
(1, '青禾食堂', '校园生活区一层', '010-80000001', 4.80, 1, 1, '/images/shop-canteen.png', 1),
(2, '时光便利', '校园生活区东侧', '010-80000002', 4.60, 1, 1, '/images/shop-store.png', 2);

INSERT INTO qh_goods (shop_id, name, description, price, stock, sales_count, sale_status, cover_image) VALUES
(1, '招牌套餐', '适合午间用餐的组合套餐', 18.00, 100, 28, 'ON_SALE', '/images/goods-meal.png'),
(2, '日常用品包', '常用生活用品组合', 12.50, 80, 16, 'ON_SALE', '/images/goods-kit.png');

INSERT INTO qh_coupon (name, coupon_type, discount_amount, threshold_amount, total_stock, claimed_count, coupon_status, start_time, end_time) VALUES
('新用户立减券', 'CASH', 3.00, 20.00, 100, 0, 'PUBLISHED', '2026-01-01 00:00:00', '2026-12-31 23:59:59');

INSERT INTO qh_blog (user_id, shop_id, title, content, cover_image, like_count, favorite_count, blog_status) VALUES
(1, 1, '午餐推荐', '分享校园内的用餐体验。', '/images/blog-lunch.png', 3, 1, 'PUBLISHED');
