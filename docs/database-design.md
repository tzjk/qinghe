# 数据库设计（M0）

## 2026-07-23 订单生命周期候选增量（未执行）

- 真实 `qh_order` 尚待用户在 DataGrip 执行 `SHOW CREATE TABLE qh_order;` 和 `SHOW INDEX FROM qh_order;` 后确认。本轮不执行数据库 SQL，也不将静态脚本结论表述为已存在的真实字段。
- 基线状态列为 `VARCHAR(20)`，待支付编码固定为 `PENDING_PAY`。统一状态完整集合为 `PENDING_PAY`、`PAID`、`ACCEPTED`、`DELIVERING`、`COMPLETED`、`CANCELLED`；合法流转见 `docs/order-state-machine.md`。
- `backend/src/main/resources/sql/order_lifecycle_schema_increment.sql` 是 DataGrip 人工审核候选：仅补充 `pay_time`、`accepted_time`、`delivery_time`、`pay_expire_time` 和 `(status, pay_expire_time)`。不新增 `goods_amount`，不重复 `cancel_reason`、`cancel_time`、`completed_time`，不删除或重命名字段。
- 当前金额语义不变：`total_amount` 为商品原始总额，`pay_amount` 为最终应付金额。

## 2026-07-20 学生所属校区结构门禁（未改表）

- 已对本机 `qinghe_life` 只读确认：`qh_student_profile.campus_id` 为 `bigint NOT NULL` 且已索引。本轮不生成、不执行迁移 SQL，也不以地址、楼栋、寝室或扫码结果替代该学生所属校区字段。
- `campus_id` 是当前学生资料的归属校区；`student_no` 与 `current_flag` 的既有唯一规则不变。首次建档仅可引用启用 `qh_campus.status=1`，资料建立后学生端不可自行改写该值。
- 校区名称是由 `campus_id` 查询得到的响应展示字段，不持久化或信任客户端提交的名称。入住快照中的校区名称继续服务于入住历史，不能反向成为学生资料归属来源。

## 2026-07-19 学籍异动与批量操作实施约定（未改表）

- 本轮不修改数据库结构、不生成或执行 SQL。`qh_student_profile` 的版本、原因、生效/结束时间和办理管理员字段记录学籍版本；旧当前资料写 `current_flag=NULL/end_time`，新资料写 `current_flag=1`。
- 新写 `student_status` 只有 `ENROLLED`、`SUSPENDED`、`DROPPED`、`GRADUATED`。`TRANSFER_MAJOR` 和 `REINSTATED` 是操作类型而非持久当前状态，扫码入住仅许可当前 `ENROLLED`。
- `qh_dorm_checkin` 始终是历史账本。退宿/毕业/退学处理关闭 `active_flag=1` 记录，填写退宿时间/原因/办理管理员；`qh_asset_set` 仅从该入住关联恢复 `AVAILABLE`，`qh_dorm_bed.status` 不变。二维码停用不清空令牌，轮换不改套装编号。

## 2026-07-18 楼栋备注与目录规则

- 用户已在 DataGrip 手工执行并复核 `qh_building.remark VARCHAR(255) NULL`；应用未生成或执行 SQL。原有 4 条楼栋、索引和外键均保留。
- `qh_building.area` 表示校园区域，`remark` 表示管理员备注；实体分别映射 `area`、`remark`。楼栋编码和名称的唯一范围均为同校区。
- `qh_dorm_room` 同时持有 `campus_id`、`building_id`；新增寝室必须以楼栋真实 `campus_id` 写入。楼栋停用不删除既有寝室、床位、资产套装或入住历史，当前入住仍以 `qh_dorm_checkin.active_flag=1` 判断。
- 资产套装新编号继续为 `building_code + room_no + '-' + bed_no`。已有下级资源的楼栋冻结编码，禁止级联更新、资产编号批量改写和二维码 Token 轮换。

## 2026-07-18 订单与入住历史核验

- `qh_dorm_checkin.previous_checkin_id` 是可空自关联历史字段：换寝的新有效入住写入原入住记录 ID，历史记录保留；它不是 DTO 响应字段。
- 普通订单继续以 `qh_order.total_amount` 表示商品小计，`discount_amount`、`delivery_fee`、`pay_amount` 由订单事务写入；本轮未修改表结构或执行 SQL。

## 1. 运行与安全配置基线

M1 创建后端 `application.yml` 时必须使用以下配置；本轮不创建配置文件、不连接数据库或 Redis。

| 配置项 | 确认值 |
|---|---|
| Spring Boot 端口 | `8090` |
| MySQL 驱动 | `com.mysql.cj.jdbc.Driver` |
| MySQL URL | `jdbc:mysql://localhost:3306/qinghe_life?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true` |
| MySQL 用户名 | `root` |
| MySQL 密码 | `${MYSQL_PASSWORD:1234}` |
| Redis 主机 | `192.168.100.128` |
| Redis 端口/库 | `6379` / `2` |
| Redis 密码 | `${REDIS_PASSWORD:123321}` |
| Redis 连接池 | `max-active=10`、`max-idle=10`、`min-idle=1`、`max-wait=1000ms`、`time-between-eviction-runs=10s` |
| Redis Key | 必须以 `qh:` 开头 |

密码不得写入 README.md、API.md、前端页面或软著文案。`application.yml` 仅保留上述环境变量占位表达式，不写明文固定密码。

## 2. 设计约定

- 数据库：`qinghe_life`；字符集建议 `utf8mb4`；所有业务表以 `qh_` 为前缀。
- 主键使用 `BIGINT`，Java 对应 `Long`；金额使用 `DECIMAL(10,2)` 与 Java `BigDecimal`；时间使用 `DATETIME` 与 Java `LocalDateTime`。
- 审计字段统一使用 `create_time`、`update_time`，与 `qh_comment` 的必需字段保持一致。
- 通用启停字段 `status` 使用 `TINYINT`：`1` 表示启用/可见，`0` 表示停用/隐藏。首页轮播仅选取 `qh_shop.status=1`。
- SQL 将在 M1 生成至 `backend/src/main/resources/sql/qinghe_life.sql`，含建库、建表、索引、注释和测试数据；不得包含 `DROP DATABASE`，且本轮不生成或执行 SQL。

## 3. 表、实体与关键字段

| 表名 | Java 实体 | 用途 | 关键字段与约束 |
|---|---|---|
| `qh_user` | `User` | 用户账户与资料 | `phone` 唯一；`username` 唯一；`password_hash` 仅保存 BCrypt 密文；`nickname`、`avatar_url`、`gender`、`status`。账号字段对旧手机号验证码用户可为空，绑定账号或新注册时必须同时写入。 |
| `qh_campus` | `Campus` | 校区目录 | `campus_code`、`campus_name` 均唯一；`status`、`sort_order`；仅提供启用校区只读查询。 |
| `qh_building` | `Building` | 校园楼栋目录 | `campus_id` 外键关联 `qh_campus`；同一校区内 `building_name` 唯一；`area`、`building_type`、`status`、`sort_order`；仅提供按校区的启用楼栋只读查询。 |
| `qh_user_address` | `UserAddress` | 用户配送地址 | 旧字段保留；校园字段保存目录关联和配送快照。新建/更新仅写校园字段并固定 `CAMPUS`，旧记录保持 `HISTORICAL` 直到被用户编辑转换。 |
| `qh_admin` | `Admin` | 管理员账户 | `username` 唯一；`password_hash`、`display_name`、`status`。 |
| `qh_category` | `Category` | 服务分类 | `name` 唯一；`icon_url`、`sort_order`、`status`。 |
| `qh_shop` | `Shop` | 商铺与首页推荐来源 | `category_id`、`name`、`address`、`phone`、`score`、`status`、`is_featured`、`cover_image`、`sort_order`。 |
| `qh_goods` | `Goods` | 商铺商品 | `shop_id`、`name`、`price`、`stock`、`sales_count`、`sale_status`。 |
| `qh_cart` | `Cart` | 购物车项 | `user_id`、`goods_id` 联合唯一；`(user_id, shop_id)` 与 `(user_id, selected)` 查询索引；`shop_id`、`quantity`、`selected`。 |
| `qh_order` | `Order` | 用户订单 | `order_no` 唯一；`user_id`、`shop_id`、`address_id`、`receiver_name`、`receiver_phone`、`delivery_address` 快照、金额字段、`status`、`remark`。 |
| `qh_order_item` | `OrderItem` | 订单商品明细 | `order_id` 索引；`goods_id`、名称/图片/价格快照、`quantity`、`subtotal`。 |
| `qh_coupon` | `Coupon` | 优惠券定义 | `name`、`coupon_type`、`discount_amount`、`threshold_amount`、`total_stock`、`claimed_count`、`coupon_status`、有效期。 |
| `qh_user_coupon` | `UserCoupon` | 用户领券记录 | `user_id`、`coupon_id` 联合唯一；`coupon_status`、`order_id`、领取/使用时间。 |
| `qh_blog` | `Blog` | 探店内容 | `user_id`、`shop_id`、标题、内容、封面、`like_count`、`favorite_count`、`blog_status`。 |
| `qh_comment` | `Comment` | 订单评价、商铺评论与探店评论 | `user_id`、`shop_id`、`order_id`、`blog_id`、`content`、`score`、`images`、`parent_id`、`status`、`create_time`、`update_time`；另有 `comment_type` 区分用途。 |
| `qh_blog_like` | `BlogLike` | 探店点赞关系 | `user_id`、`blog_id` 联合唯一。 |
| `qh_blog_favorite` | `BlogFavorite` | 探店收藏关系 | `user_id`、`blog_id` 联合唯一。 |
| `qh_operate_log` | `OperateLog` | 重要写操作审计记录 | `user_id`、模块、操作类型、Controller类/方法、请求路径/方法、脱敏请求与返回摘要、成功状态、异常摘要、耗时、IP、操作时间；索引为 `(user_id, operate_time)` 与 `(module, operate_time)`。 |

## 4. 关联、索引与评论规则

| 关系/规则 | 设计 |
|---|---|
| 用户—地址 | 为 `qh_user_address.user_id` 建索引；同一用户设置默认地址时，服务层先清除该用户其他地址的 `is_default`。 |
| 校区—楼栋—地址 | `qh_building.campus_id` 外键关联校区；地址以可空 `campus_id`、`building_id` 分别关联校区和楼栋，并建命名索引。后续服务层必须校验所选楼栋属于所选校区；目录改名不应抹去地址行已保存的区域、楼栋类型和楼栋名称快照。 |
| 分类—商铺—商品 | 为 `qh_shop.category_id`、`qh_goods.shop_id` 建索引；首页轮播查询按 `(is_featured, status, sort_order)` 排序。 |
| 用户—购物车—商品 | `qh_cart(user_id, goods_id)` 唯一索引，保证同一用户同一商品只有一条购物车项；另建 `(user_id, shop_id)` 和 `(user_id, selected)` 查询索引；`shop_id` 由商品归属商铺生成，`selected` 表示本次是否选中。 |
| 订单—明细 | `qh_order.order_no` 唯一；为订单用户、商铺和 `qh_order_item.order_id` 建索引；订单保存 `receiver_name`、`receiver_phone`、`delivery_address` 快照，明细保存商品名称/图片/价格快照。 |
| 用户—优惠券 | `qh_user_coupon(user_id, coupon_id)` 唯一索引，辅助重复领取校验。 |
| 探店互动 | `qh_blog_like(user_id, blog_id)`、`qh_blog_favorite(user_id, blog_id)` 唯一索引，保证互动幂等。 |
| 评论查询 | 建立 `qh_comment(shop_id, status, create_time)`、`qh_comment(blog_id, status, create_time)` 索引。 |
| 订单评价 | `order_id` 唯一索引；订单评价写入时同时保存订单所属 `shop_id`，`score` 必须为 1 至 5。 |

`qh_comment.comment_type` 使用 `ORDER_REVIEW`、`SHOP_COMMENT`、`BLOG_COMMENT`：

- `ORDER_REVIEW`：`order_id` 与 `shop_id` 非空，`blog_id` 为空，`score` 必填。
- `SHOP_COMMENT`：`shop_id` 非空，`order_id`、`blog_id` 为空，`score` 可选。
- `BLOG_COMMENT`：`blog_id` 非空，`shop_id`、`order_id` 为空，`score` 为空。

`parent_id` 为可空自关联；回复必须与父评论指向同一业务目标。`images` 存储图片地址 JSON 文本；本项目不接入云存储。

## 5. 状态值登记

| 字段 | 值 | 适用表 | 规则 |
|---|---|---|---|
| `status` | `1` / `0` | `qh_user`、`qh_admin`、`qh_category`、`qh_shop`、`qh_comment` | 分别表示启用/停用或可见/隐藏。`qh_shop` 首页轮播条件固定为 `status=1`。 |
| `is_featured` | `1` / `0` | `qh_shop` | 分别表示推荐/普通；仅 `is_featured=1` 的启用商铺进入 `banners`。 |
| `sale_status` | `ON_SALE` / `OFF_SALE` | `qh_goods` | 仅 `ON_SALE` 商品可加入购物车。 |
| `status` | `PENDING_PAY`、`PAID`、`CANCELLED`、`COMPLETED` | `qh_order` | M3A 只创建 `PENDING_PAY`；支付、取消、完成流转留待后续阶段。 |
| `coupon_status` | `DRAFT`、`PUBLISHED`、`STOPPED` | `qh_coupon` | 仅已发布且处于有效期的券可领取。 |
| `coupon_status` | `AVAILABLE`、`USED`、`EXPIRED` | `qh_user_coupon` | Java 属性命名为 `userCouponStatus` 或使用独立枚举，避免与券定义状态混用。 |
| `blog_status` | `PUBLISHED`、`HIDDEN` | `qh_blog` | 用户端只显示 `PUBLISHED`。 |

## 6. Java 分层命名登记

| 资源域 | 实体 | 请求 DTO 示例 | 响应 VO 示例 |
|---|---|---|---|
| 用户/地址 | `User`、`UserAddress` | `UserLoginRequest`、`UserProfileUpdateRequest`、`AddressCreateDTO`、`AddressUpdateDTO` | `UserVO`、`AddressVO`、`LoginVO` |
| 管理员 | `Admin` | `AdminLoginRequest`、`AdminStatusUpdateRequest` | `AdminVO`、`AdminLoginVO` |
| 分类/商铺/商品 | `Category`、`Shop`、`Goods` | `CategorySaveRequest`、`ShopSaveRequest`、`GoodsSaveRequest`、`ResourceStatusUpdateRequest` | `CategoryVO`、`ShopVO`、`GoodsVO` |
| 购物车 | `Cart` | `CartItemCreateRequest`、`CartItemUpdateRequest` | `CartItemVO`、`CartSummaryVO` |
| 订单/评价 | `Order`、`OrderItem`、`Comment` | `OrderCreateRequest`、`OrderCancelRequest`、`OrderReviewRequest` | `OrderVO`、`OrderDetailVO`、`CommentVO` |
| 优惠券 | `Coupon`、`UserCoupon` | `CouponSaveRequest` | `CouponVO`、`UserCouponVO` |
| 探店/评论 | `Blog`、`Comment`、`BlogLike`、`BlogFavorite` | `BlogCreateRequest`、`BlogCommentCreateRequest` | `BlogVO`、`CommentVO` |

禁止使用含义模糊的 `CommonDTO`、`DataVO`、`ResultDTO` 作为业务对象；统一返回体为 `ApiResponse<T>`，分页体为 `PageResult<T>`。

## 7. M1 一致性核对清单

1. SQL、实体和 DTO/VO 必须使用本文件登记的表名、字段名、状态名和接口语义。
2. `Order` 的 SQL 映射必须指向 `qh_order`，不得使用无前缀的 `order` 表。
3. 金额不得使用 `double`；库存、数量、计数不得使用金额类型。
4. 前端只将状态值映射为展示文案，不自行定义未登记的状态。
5. 后端配置只可将密码写入 `application.yml` 的环境变量占位表达式；不得将密码传播到文档或前端。

## 8. 账号注册字段迁移（待人工执行）

- 基线 `qh_user` 已定义 `username VARCHAR(32) DEFAULT NULL`、`password_hash VARCHAR(100) DEFAULT NULL` 和 `uk_qh_user_username(username)`；手机号继续由 `uk_qh_user_phone(phone)` 唯一约束保护。
- 字段允许 `NULL` 是旧手机号验证码用户兼容策略：旧用户未绑定账号时两个字段均为空；后续注册服务只允许在原用户记录上补齐用户名和 BCrypt 密文，不创建第二个手机号用户，也不改变 id、地址、购物车或历史数据。
- 新注册或绑定账号时，服务层必须同时校验并保存用户名和 BCrypt `password_hash`，禁止保存明文密码、`password` 明文字段、MD5 或 SHA1。
- 一次性脚本位于 `backend/src/main/resources/sql/account_register_increment.sql`，只含 `ALTER TABLE` 与 `CREATE UNIQUE INDEX`，必须由用户在 DataGrip 审核并手工执行；本项目不会自动导入。执行后需复核列与索引，再实施注册接口和真实集成测试。

## 9. 操作日志

- 用户已在 DataGrip 手工执行 `backend/src/main/resources/sql/operate_log_increment.sql`；本轮只读复核确认 `qh_operate_log` 的 15 个字段、主键与 `(user_id, operate_time)`、`(module, operate_time)` 两个复合索引均与 `OperateLog` 实体一致。
- `@OperateLog` 只标注关键写操作，`OperateLogAspect` 使用 Spring 注入的 `ObjectMapper` 和现有 `UserContext.getUserId()` 生成审计记录；项目没有 `UserHolder` 类，不创建并行用户容器。
- `OperateLogServiceImpl.save` 使用 `REQUIRES_NEW` 独立事务。目标方法成功或抛出异常都会记录；日志入库异常只写无敏感内容的 error 日志，不影响业务返回或原异常继续交给 `GlobalExceptionHandler`。
- 请求和返回摘要使用递归脱敏 JSON，总长度最多 2000 字符；掩盖 `password`、`confirmPassword`、`passwordHash`、`code`、`Authorization`、任意 Token、Redis/数据库密码与完整手机号，且省略文件二进制、上传文件、Servlet 请求/响应及流对象。

## 10. 校园地址模型与兼容实现（迁移已人工执行）

### 10.1 审计结论与目标结构

- 实际 MySQL 8.0.34 已有启用校区和楼栋，`qh_user_address` 已含 12 个校园字段、索引及外键，且仍保留 2 条 `HISTORICAL` 地址。
- `Campus`、`Building` 与 `UserAddress` 已映射实际表。目录 ID 是关联依据，地址行的区域/楼栋字段是配送展示快照；校园名称由关联校区读取后随地址 VO 返回。
- `receiver_name`、`receiver_phone`、`is_default`、`user_id` 继续沿用。手机号继续只在界面展示时脱敏；查询、修改、删除、设默认均必须以当前登录用户 ID 过滤。首地址默认、默认切换、删除默认后自动补选不变。

### 10.2 字段映射与历史数据策略

| 旧字段 | 新字段/策略 | 说明 |
|---|---|---|
| `province`、`city`、`district` | 保留并改为可空 | 不删除、不覆盖；校园新地址不再依赖这三项。 |
| `detail_address` | 保留并改为可空 | 原始旧详细地址永久保留。 |
| 旧四段地址 | `detail` | 迁移时以省、市、区、详细地址拼接写入 `detail`，不改写源字段。 |
| 无校区/楼栋 ID 的旧记录 | `address_type='HISTORICAL'` | 旧记录仍可查询、编辑和设默认，直至用户主动按后续新表单补齐校园信息。 |
| 新校园记录 | `address_type='CAMPUS'` | 服务必须校验必填的 `campus_id`、`building_id` 与目录归属；`room_no` 与 `delivery_point` 至少一项非空，并保存楼栋可读快照。 |

### 10.3 增量脚本与订单快照边界

- 一次性脚本为 `backend/src/main/resources/sql/campus_address_increment.sql`；本项目不会自动导入。它只建目录表、扩展地址表、创建命名索引/外键并安全迁移现有地址，不包含删除、清表或业务数据改写。
- 当前 `qh_order` 和 `qh_cart` 均不在本轮迁移范围。订单当前已有 `address_id`、`receiver_name`、`receiver_phone`、`delivery_address`，但尚无订单业务实现。
- 获准实现订单时，创建订单必须先验证地址归属当前用户，再写不可变快照：校区 ID/名称、区域、楼栋 ID/类型/名称、楼层、房间号、配送点、详细说明、标签、配送备注、收件人和脱敏前电话。`delivery_address` 继续保存可读格式化文本；如需结构化检索，届时以单独、经审核的迁移增加不可变 `delivery_snapshot`，不得回读可编辑地址替代订单快照。
## 宿舍入住与资产二维码管理（待人工执行迁移）

> **2026-07-17 实名资料迁移准备：** 真实 `qinghe_life.qh_student_profile` 仍缺 `real_name`、`contact_phone`，但当前行为 0 行。已生成仅供 DataGrip 人工执行的 `backend/src/main/resources/sql/student_profile_identity_increment.sql`：新增两个无默认值的 `NOT NULL` 字段，不改 `qh_user`、现有索引、外键或版本模型。执行并只读复核成功前，学生实名资料与扫码入住仍未实施，且不得使用昵称或登录手机号替代。

### 模块边界

- 复用 `qh_campus`、`qh_building`、`qh_user`、`qh_admin` 与唯一 `qh_operate_log`；不创建重复校区、楼栋或模块日志表。
- `qh_building` 新增可空 `building_code`，仅作为宿舍资产套装编号来源，不回填或改写既有楼栋数据。后续只有已配置非空楼栋编码的楼栋可以创建寝室、床位和资产套装。
- 本轮生成 `backend/src/main/resources/sql/dorm_asset_increment.sql`，只供 DataGrip 人工审核和执行；当前未执行 SQL、未写入数据、未实现业务代码。

### 新增表与关系

| 表 | 职责 | 关键约束与历史策略 |
|---|---|---|
| `qh_dorm_room` | 校区/楼栋下的寝室目录 | 外键复用校区、楼栋；`(building_id, room_no)` 唯一；停用使用 `status=0`。 |
| `qh_dorm_bed` | 寝室床位目录 | 外键关联寝室；`(dorm_room_id, bed_no)` 唯一；床位可停用，不以入住状态覆盖目录状态。 |
| `qh_student_profile` | 学生资料与学籍版本 | 当前版本 `current_flag=1`，历史版本设为 `NULL`；以 `(user_id, current_flag)` 和 `(student_no, current_flag)` 保证每人、每学号仅一条当前资料。 |
| `qh_asset_set` | 一张床位对应的一套资产和二维码令牌 | 床位、套装编号、`qr_token` 均唯一；记录二维码、套装状态，不保存学生信息。 |
| `qh_asset` | 套装明细 | `(asset_set_id, asset_type)` 唯一；每套由服务层一次性建立床、床板、书桌、衣柜、凳子五项。 |
| `qh_dorm_checkin` | 入住、换寝、退宿、批量退宿历史 | 通过 `active_flag=1` 保证同一学生和同一床位各只有一条有效入住；结束记录改为 `NULL`，保留全部历史与位置快照。 |

`qh_dorm_room.campus_id` 与 `qh_dorm_room.building_id` 同时保留是为了校园范围查询；服务层必须校验楼栋实际属于该校区。`qh_dorm_checkin` 保存学号、学籍状态、校区/楼栋/寝室/床位、资产套装编号快照，避免目录或资料后续改名破坏历史可读性。

### 学生实名资料字段迁移规则（待人工执行）

- `real_name VARCHAR(50) NOT NULL` 保存学生真实姓名；不得复用 `qh_user.nickname`。
- `contact_phone VARCHAR(20) NOT NULL` 保存入住及学籍联系电话；不得复用 `qh_user.phone`，且不建立唯一索引。
- 本轮实测目标表为 0 行，所以可无默认值直接新增 `NOT NULL` 字段。若人工执行前数据已出现，必须停止使用该脚本：先新增可空字段，人工补齐旧记录，再以独立审核操作收紧约束；不得伪造或批量更新姓名、手机号。
- 迁移脚本内已以注释提供执行前的数据库、建表和行数复核 SQL，以及执行后的字段元数据复核 SQL。本项目不会自动导入脚本。

### 资产编号与二维码

- 套装编号在服务层由 `building_code + room_no + '-' + bed_no` 组装，例如楼栋编码 `JA`、寝室号 `101`、床位号 `01` 生成 `JA101-01`；数据库以 `uk_qh_asset_set_no` 防止重复。
- `qr_token` 必须由应用以 `SecureRandom` 生成至少 32 字节随机值并编码为 64 位十六进制；二维码载荷仅包含版本标记和该令牌，例如 `QH-DORM-V1:{qr_token}`，不得出现学号、姓名、手机号、用户 ID、床位真实关系或可推断个人信息。
- 使用 ZXing 在请求时动态生成二维码 PNG/SVG 响应；默认不上传 OSS，也不在数据库保存二维码图片 URL。令牌轮换后旧令牌立即失效，套装记录保留轮换时间。

### 状态与不可变历史

- 学籍状态仅为 `ENROLLED`、`TRANSFER_MAJOR`、`SUSPENDED`、`DROPPED_OUT`、`REINSTATED`、`GRADUATED`。每次学籍异动关闭旧资料版本并新建当前版本，记录生效时间、原因与办理管理员；不硬删除。
- 有效入住仅允许学籍为 `ENROLLED`、`TRANSFER_MAJOR` 或 `REINSTATED` 的学生确认；`SUSPENDED`、`DROPPED_OUT`、`GRADUATED` 的异动办理必须在同一事务中完成退宿和资产释放，并保留原因。
- 入住、换寝、退宿与批量退宿只改变有效记录的状态、结束时间与 `active_flag`，不删除历史行。管理员批量清除定义为批量退宿并将相应资产套装置为 `AVAILABLE`，必须填写原因。
- 下一阶段所有管理写操作继续使用 `@OperateLog` 与 `qh_operate_log`。现有审计表的 `user_id` 对管理员请求可为空，域表的 `operator_admin_id` 保留实际办理管理员；不得新增日志表。
## 宿舍资产已实施约定

本轮未修改任何数据库结构；实现复用已人工执行迁移中的 `qh_building.building_code`、宿舍/床位、资产套装与资产表。`qh_asset_set` 的床位、套装号、二维码令牌唯一约束及 `qh_asset(asset_set_id, asset_type)` 唯一约束共同防止重复生成。

### 2026-07-18 宿舍资源维护规则

- 实时库确认宿舍楼类型值为 `宿舍楼`；宿舍资源查询仅使用该值，教学楼、图书馆等保留在通用楼栋目录。
- `qh_dorm_room(building_id,room_no)` 与 `qh_dorm_bed(dorm_room_id,bed_no)` 分别保证目录编号唯一。`qh_dorm_checkin.active_flag=1` 是唯一的当前入住判断，不能用寝室或床位 `status` 表示占用。
- 资产状态只使用实际表注释定义的 `NORMAL`、`REPAIR`、`SCRAPPED`；资产套装状态保留 `AVAILABLE`、`OCCUPIED`、`MAINTENANCE`、`RETIRED`。本轮不新增列、不修改 `asset_set_no`、`qr_token` 或历史入住行。

## 店铺后台维护与封面字段复核（2026-07-17，已完成并验证）

- 真实 `qh_shop` 已只读复核为：`id`、`category_id`、`name`、`address`、可空 `phone`、`score decimal(3,2)`、`status`、`is_featured`、可空 `cover_image varchar(255)`、`sort_order` 及时间列；索引为主键、`idx_qh_shop_category(category_id)`、`idx_qh_shop_featured(is_featured,status,sort_order)`。
- `cover_image` 保存单张封面 URL，不新建图片表；`status` 同时承担启用/停用展示状态，不存在简介、营业时间或独立营业状态字段。本轮未执行 SQL、未修改表结构，也未迁移或批量重写既有图片 URL。

## 商品后台维护与主图字段复核（2026-07-17，已完成并验证）

- 实时只读复核的 `qh_goods` 包含 `id`、`shop_id`、`name`、可空 `description`、`price decimal(10,2)`、`stock int default 0`、`sales_count`、`sale_status varchar(16) default ON_SALE`、可空 `cover_image varchar(255)` 及时间列；索引为 `PRIMARY(id)` 与 `idx_qh_goods_shop(shop_id)`。
- `shop_id` 提供店铺关联，`cover_image` 提供单张主图，价格、库存和销售状态满足本轮及后续订单前置的最小字段要求。`qh_goods` 没有直接 `category_id` 或 `sort_order`：分类经 `qh_shop.category_id` 关联，界面不提供虚构的商品排序维护。
- 本轮无需迁移，未生成或执行 SQL，未新增商品图片表或日志表。主图 URL 持久化在现有 `cover_image`，数据库是当前商品资料的真实来源。

## 商品店内分类模型（2026-07-17，待人工迁移）

### 现状语义与迁移结论

- 实时只读复核确认，`qh_category` 是全局名称唯一的店铺类型目录：`qh_shop.category_id` 直接引用其 ID，商品管理页的“分类”筛选仅经所属店铺反查。它不是、也不得复用为店内商品分类。
- `qh_goods` 只有 `shop_id`，没有 `category_id`；因此不能表达“热门推荐、零食、饮料、日用品”等同一店铺内的商品分组。需要先由授权人员在 DataGrip 审核并手工执行 `backend/src/main/resources/sql/goods_category_increment.sql`，再进入业务实现。

### 目标表和关联

| 对象 | 字段/约束 | 说明 |
|---|---|---|
| `qh_goods_category` | `id`、`shop_id`、`name`、`sort_order`、`status`、创建/更新时间 | 店内商品分类；`shop_id` 外键关联 `qh_shop.id`。 |
| 店内名称唯一性 | `UNIQUE(shop_id, name)` | 同一店铺不得有重名分类；不同店铺可使用相同名称。 |
| 查询索引 | `INDEX(shop_id, status, sort_order)` | 支持某店铺的启用分类导航及稳定排序。 |
| `qh_goods.category_id` | 可空 `BIGINT` 与普通索引 | 第一版每个商品至多关联一个店内分类；历史商品保持 `NULL`，不创建默认或未知分类。 |

`qh_goods.category_id` 的外键只能证明分类记录存在，不能单独证明其 `shop_id` 与商品的 `shop_id` 相等。因此后续所有商品新建、编辑、分类调整和店铺调整都必须由服务端在同一事务中读取分类并验证 `goodsCategory.shopId == goods.shopId`；商品更换店铺时必须把 `categoryId` 清空，或同时提交新店铺下的有效分类 ID。客户端只能提交分类 ID，服务端不得依据分类名称创建或猜测分类。

### 执行与兼容边界

- 脚本只新增表、列、索引和约束；不修改或删除现有 `qh_category`、不新增商品图片表或日志表，也不包含 `DROP`、`TRUNCATE`、`DELETE` 或数据回填。
- 已有商品保持 `category_id = NULL`；后续由管理员先创建店内分类，再逐件为旧商品分配，禁止将旧商品自动写入“默认分类”或“未知分类”。

## 学生入住实现约定（2026-07-17）

- 学生资料继续使用真实 `college_name`、`major_name`、`student_status`、`current_flag`，其中 `1` 是当前版本、`NULL` 是历史版本；首次资料使用既有 `ENROLLED` 状态。
- `qh_dorm_bed.status` 只表示床位目录启停，不以入住覆盖；床位占用由 `qh_dorm_checkin.active_flag=1` 的唯一约束表示，资产套装状态在入住后为 `OCCUPIED`。未修改数据库结构或执行迁移。
# 店内商品分类（已人工迁移复核，2026-07-17）

本轮未执行 SQL 或调整结构。真实库已只读复核：`qh_category` 保持全局店铺类型目录；`qh_goods_category` 为店内商品分类，含 `shop_id`、`name`、`sort_order`、`status`，具有 `UNIQUE(shop_id,name)` 与 `INDEX(shop_id,status,sort_order)`；`qh_goods.category_id` 可为 NULL，并分别由商品分类和店铺外键保证关联一致性。

## 订单、优惠券与限时秒杀迁移准备（2026-07-17）

- 本轮只按仓库 SQL/实体/Mapper 做静态审计，未连接数据库，因此增量脚本必须先由用户在 DataGrip 读取实际列与索引后人工执行；不得由应用导入。
- 普通订单继续使用 `qh_order`/`qh_order_item`：新增项仅补充结构化校园地址快照、`delivery_fee`、`user_coupon_id`、取消原因/时间、完成时间和查询索引。订单号与用户券关联分别由 `uk_qh_order_no` 和新增 `uk_qh_order_user_coupon` 唯一保护。
- 普通券继续使用 `qh_coupon`/`qh_user_coupon`：补充范围、店铺、领取/使用窗口、折扣率、限领、过期/来源/退券时间和索引。既有 `UNIQUE(user_id,coupon_id)` 保留，故第一版普通券和秒杀券均限制每人一张；不得用代码绕过该约束。
- 秒杀券不与普通订单流程混用：新增 `qh_seckill_coupon_activity`（数据库活动库存）和 `qh_seckill_coupon_order`（异步受理订单）。活动号、活动-用户、Stream 消息 ID 和受理订单号都有唯一约束；消费者必须用 `stock > 0` 条件更新。秒杀活动绑定专用 `coupon_id`，不能复用普通已领取券。
- 未执行脚本：`backend/src/main/resources/sql/order_core_increment.sql`、`backend/src/main/resources/sql/coupon_core_increment.sql`、`backend/src/main/resources/sql/seckill_coupon_increment.sql`。脚本只含 `ALTER TABLE ... ADD ...` 或 `CREATE TABLE`，无 `DROP`、`TRUNCATE`、`DELETE` 或历史数据伪造。
# 订单金额字段约定（2026-07-18）

- `qh_order.total_amount` 是订单商品小计，等于订单明细 `subtotal` 合计；不新增 `goods_amount`。
- `discount_amount` 是优惠金额，`delivery_fee` 是配送费，`pay_amount = total_amount - discount_amount + delivery_fee` 是最终应付金额。
- 本阶段普通订单由后端明确写入 `discount_amount=0.00` 与 `delivery_fee=0.00`，所有订单金额使用 `DECIMAL`/Java `BigDecimal`。

## 管理员入住状态约定（2026-07-18，未改表）

- `qh_dorm_checkin.active_flag=1` 仅表示当前有效入住；关闭记录显式写入 `NULL`，并保留退宿时间、原因和原始历史行。
- `qh_dorm_bed.status` 仅表示目录启停，不因入住、退宿或换寝改变；占用由有效入住唯一约束表达。
- `qh_asset_set.status` 使用既有 `AVAILABLE`/`OCCUPIED`。退宿恢复原套装，换寝在同一事务中恢复原套装并占用目标套装。
- 本轮未修改数据库结构、未执行手工 SQL、未新增日志表。
- 本轮测试收口只使用既有唯一约束和事务验证有效入住竞争；没有生成或执行任何迁移 SQL。`previous_checkin_id` 仅保留在历史持久化关系中，不作为管理端响应字段。

## 2026-07-19 学籍异动与批量宿舍操作设计（仅审计，覆盖旧状态枚举说明）

- 统一当前 `student_status` 为 `ENROLLED`、`SUSPENDED`、`DROPPED`、`GRADUATED`。`TRANSFER_MAJOR`、`REINSTATED` 是异动类型而非当前状态；旧文中 `DROPPED_OUT`、`TRANSFER_MAJOR`、`REINSTATED` 的状态枚举不再作为后续写入契约。
- `current_flag=1` 仅为当前学生资料，旧版本关闭为 `NULL` 并写入 `end_time`；转专业、休学、退学、复学、毕业均通过新建当前版本保留历史。`qh_dorm_checkin.student_profile_id` 与快照保留入住当时的资料，不随新版本回写。
- `SUSPENDED` 可由管理员选择保留入住或同事务退宿；`DROPPED`、`GRADUATED` 不得有当前入住，必须同事务关闭入住并将绑定套装恢复 `AVAILABLE`。`qh_dorm_bed.status` 继续只表示目录启停。
- `qh_asset_set.qr_status` 仅控制二维码可用性，不清空 `qr_token`；轮换仅在空闲、无当前入住的套装上更新唯一新令牌和 `qr_rotated_time`，不改 `asset_set_no`。批量释放只允许无当前入住的套装，且不得把 `MAINTENANCE`、`RETIRED` 强制改为可用。
- 现有字段和索引足够支持受上限的全事务批量毕业/退宿；本轮不生成迁移 SQL。未来若要异步、分片和可恢复的大批次，须先单独审计持久化批次模型，不能伪造模块日志表。

## 2026-07-20 宿舍楼按校区管理实现边界

- 本轮未修改表结构、未执行 SQL。管理员宿舍页只读取 `qh_building.building_type='宿舍楼'`；`DORM`、教学楼、图书馆等通用目录记录不属于此页。
- `BuildingMapper.selectDormBuildingStats` 聚合 `qh_dorm_room`、`qh_dorm_bed`、`qh_asset_set`、`qh_dorm_checkin` 的寝室、床位、资产套装、历史入住和当前入住数，避免楼栋列表 N+1；简单 insert/update/delete 继续使用 MyBatis-Plus。
- 条件删除只会删除 `qh_building` 本行，五类关联计数任一非零即拒绝；不得级联删除寝室、床位、资产、二维码或入住历史。
