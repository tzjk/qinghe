# 青禾校园生活服务系统项目需求

## 1. 项目目标

开发“青禾校园生活服务系统 V1.0”，英文工程名为 qinghe-life-service。

项目采用 Vue 3 与 Spring Boot 前后端分离结构，用于本地运行、功能展示、页面截图和软著源代码整理。

系统面向校园及周边生活场景，提供商铺浏览、商品选择、购物车、订单、优惠券、探店内容、评论互动、个人中心和后台管理功能。

不得复制其他教学项目的源码、包名、表名、接口和页面。

## 2. 技术栈

### 后端

- Java 8
- Spring Boot 2.7.18
- Maven
- MyBatis-Plus 3.5.x
- MySQL 8
- Redis
- Lombok
- Jackson
- Hibernate Validator
- 统一返回结果
- 全局异常处理
- 登录拦截器
- CORS 配置
- 分页查询

### 前端

- Vue 3
- Vite
- Element Plus
- Pinia
- Vue Router
- Axios

### 运行配置

- 后端端口：8090
- 前端端口：5174
- 数据库：qinghe_life
- MySQL 驱动：`com.mysql.cj.jdbc.Driver`
- MySQL URL：`jdbc:mysql://localhost:3306/qinghe_life?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true`
- MySQL 用户名默认值：`root`
- MySQL 密码配置：`${MYSQL_PASSWORD:1234}`
- Redis 主机默认值：`192.168.100.128`
- Redis 端口：6379
- Redis 密码配置：`${REDIS_PASSWORD:123321}`
- Redis 数据库编号：2
- Redis 键前缀：`qh:`
- Redis 连接池：`max-active=10`、`max-idle=10`、`min-idle=1`、`max-wait=1000ms`、`time-between-eviction-runs=10s`
- Token 请求头：Authorization: Bearer <token>

密码仅允许写入后端 `application.yml` 的环境变量占位表达式；不得写入 README.md、API.md、前端页面或软著文案。

## 3. 项目目录

所有内容必须保存在当前本地项目目录：

qinghe-life-service
- backend
- frontend
- docs
- deploy
- AGENTS.md
- PROJECT_PLAN.md
- progress.md
- findings.md
- README.md

SQL 文件路径：

backend/src/main/resources/sql/qinghe_life.sql

接口文档路径：

backend/API.md

软著文案路径：

frontend/src/assets/copyright-text.md

## 4. 数据库

至少建立以下数据表：

- qh_user
- qh_user_address
- qh_admin
- qh_category
- qh_shop
- qh_goods
- qh_cart
- qh_order
- qh_order_item
- qh_coupon
- qh_user_coupon
- qh_blog
- qh_comment
- qh_blog_like
- qh_blog_favorite

其中：

- `qh_shop` 至少包含 `is_featured`、`cover_image`、`sort_order`，用于首页推荐和轮播展示。
- `qh_comment` 至少包含 `user_id`、`shop_id`、`order_id`、`blog_id`、`content`、`score`、`images`、`parent_id`、`status`、`create_time`、`update_time`。

SQL 文件必须包含：

- CREATE DATABASE IF NOT EXISTS
- USE qinghe_life
- 建表语句
- 索引
- 字段注释
- 测试数据

不得包含 DROP DATABASE。

SQL 与 Java 实体类字段必须一致。

## 5. 用户端功能

### 用户模块

- 获取开发验证码
- 手机号登录
- Token 登录状态
- 查询当前用户
- 修改个人资料
- 地址管理
- 退出登录

地址接口：

- GET /api/addresses
- POST /api/addresses
- PUT /api/addresses/{id}
- DELETE /api/addresses/{id}
- PUT /api/addresses/{id}/default

验证码固定为 123456，可写入 Redis。

登录成功后生成 UUID Token，将用户登录信息保存到 Redis。

### 首页模块

- 轮播内容
- 服务分类
- 推荐商铺
- 热门商品
- 优惠券入口
- 探店内容入口

`GET /api/home/summary` 固定返回：`banners`、`categories`、`recommendedShops`、`hotGoods`、`availableCoupons`、`featuredBlogs`。

不增加轮播表；`banners` 使用 `qh_shop` 中 `is_featured=1` 且 `status=1` 的推荐商铺数据生成。

### 商铺模块

- 商铺分页查询
- 分类筛选
- 商铺详情
- 商铺商品查询
- 商铺评论查询
- Redis 商铺缓存

### 商品模块

- 商品分页查询
- 商品详情
- 按商铺查询
- 上架与下架状态

### 购物车模块

- 加入购物车
- 查询购物车
- 修改数量
- 删除商品
- 清空购物车
- 金额计算

### 订单模块

- 从购物车创建订单
- 查询我的订单
- 查询订单详情
- 取消订单
- 模拟支付
- 确认完成
- 发布评价

订单评价接口：

- POST /api/orders/{id}/review

### 优惠券模块

- 查询可领取优惠券
- 领取优惠券
- 查询我的优惠券
- 下单使用优惠券
- 库存校验
- 重复领取校验
- 使用门槛校验
- Redis 库存同步

### 探店模块

- 探店列表
- 探店详情
- 发布探店内容
- 点赞与取消点赞
- 收藏与取消收藏
- 发表评论
- 查询我的探店
- 查询我的收藏

## 6. 管理端功能

- 管理员登录
- 数据看板
- 分类管理
- 商铺管理
- 商品管理
- 订单管理
- 优惠券管理
- 用户查询
- 探店内容管理
- 评论管理

用户端与管理端位于同一个 Vue 项目，通过路由和布局区分。

## 7. 前端页面

至少实现以下八类页面：

- 首页
- 商铺
- 购物车
- 订单
- 优惠券
- 探店
- 我的
- 后台

页面名称不超过五个字。

页面必须包含真实布局和可演示内容，不得只创建空白页面或静态标题。

应包含：

- 导航栏
- 卡片
- 表格
- 表单
- 分页
- 状态标签
- 加载状态
- 空状态
- 错误提示
- 确认弹窗

## 8. 接口规范

接口统一使用 /api 前缀。

用户接口：

- POST /api/user/code
- POST /api/user/login
- GET /api/user/me
- PUT /api/user/profile
- POST /api/user/logout

首页接口：

- GET /api/home/summary

商铺与商品接口：

- GET /api/shops
- GET /api/shops/{id}
- GET /api/shops/{id}/goods
- GET /api/shops/{id}/comments
- GET /api/goods
- GET /api/goods/{id}

购物车接口：

- GET /api/cart
- POST /api/cart
- PUT /api/cart/{id}
- DELETE /api/cart/{id}
- DELETE /api/cart

订单接口：

- POST /api/orders
- GET /api/orders
- GET /api/orders/{id}
- PUT /api/orders/{id}/cancel
- PUT /api/orders/{id}/pay
- PUT /api/orders/{id}/complete

优惠券接口：

- GET /api/coupons
- POST /api/coupons/{id}/claim
- GET /api/coupons/mine

探店接口：

- GET /api/blogs
- GET /api/blogs/{id}
- POST /api/blogs
- PUT /api/blogs/{id}/like
- PUT /api/blogs/{id}/favorite
- POST /api/blogs/{id}/comments

后台接口统一使用：

- /api/admin/**

后台各模块的逐项查询、新增、修改、删除和状态修改接口以 `docs/api-contract.md` 为唯一设计基线。

统一返回结构：

{
  "code": 200,
  "message": "success",
  "data": {}
}

分页结构统一包含：

- records
- total
- page
- size

## 9. 前端登录规则

Pinia 保存用户信息和 Token。

Token 同步保存到 localStorage。

Axios 请求拦截器统一添加：

Authorization: Bearer <token>

收到 401 时清理登录状态并跳转到登录页面。

## 10. 文档和部署文件

必须生成：

- README.md
- backend/API.md
- docs/database-design.md
- docs/api-contract.md
- docs/page-design.md
- docs/test-report.md
- frontend/src/assets/copyright-text.md
- deploy/README.md
- deploy/start-backend.bat
- deploy/start-frontend.bat

部署脚本只用于启动本项目，不得修改系统服务。

## 11. 软著文案

生成约 250 字总体功能描述，以及首页、商铺、购物车、订单、优惠券、探店、我的、后台的页面说明。

软著文案不得出现：

国家法律法规相关主题、AI、智能、专注、聚焦、整合、综合、重要、核心、根本、关键、有效、契合、强调、延伸、大量、呼应、确立、脱离、明确、基础、洞见、支点、要点、深入、框架、内涵、本质和破折号。

尽量避免“XX性”和“XX化”表达。

## 12. 开发里程碑

M0：需求检查、数据库设计、接口设计和页面设计

M1：后端与前端项目骨架、SQL、实体类和公共组件

M2：登录、首页、商铺和商品

M3：购物车、订单和优惠券

M4：探店、评论、点赞、收藏和个人中心

M5：后台管理、联调、测试、部署说明、API 文档和软著文案

每完成一个里程碑后停止，等待用户审核。

## 13. 验收标准

- Maven 编译或测试通过
- 前端 npm run build 通过
- SQL 与实体字段一致
- 前后端接口路径一致
- Token 能由前端传递至后端
- 至少八类用户可见页面
- 主要后端业务模块存在
- README.md 和 API.md 存在
- 部署说明和启动脚本存在
- 没有原教学项目标识
- 不伪造测试结果
- 本项目为本地项目，不要求 Git 仓库；使用 progress.md、findings.md 和文件清单记录变更。
