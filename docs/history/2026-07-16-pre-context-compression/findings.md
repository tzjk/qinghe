# M0 发现与决策记录

## 本轮范围与约束

- 仅执行 M0：需求检查、数据库设计、接口设计、页面设计和命名冲突审查。
- 未创建 Spring Boot 或 Vue 业务代码；未安装依赖；未连接 MySQL 或 Redis；未执行 SQL。
- 仅新增本文件、`PROJECT_PLAN.md`、`progress.md`、`docs/database-design.md`、`docs/api-contract.md`、`docs/page-design.md`。

## 启动检查

| 检查项 | 结果 | 证据/说明 |
|---|---|---|
| 项目目录 | 已定位 | 实际项目目录为 `C:\Users\28402\Desktop\软著\软著项目构建\qinghe-life-service`。 |
| 项目名 | 目录名匹配 | 目录名为 `qinghe-life-service`。 |
| 本地项目规则 | 已确认 | 本项目不使用 Git；无需核验 Git 仓库，且不得执行 Git 命令。 |
| AGENTS.md | 已读取 | 已采用目录边界、技术版本、原创要求与停点规则。 |
| docs/PROJECT_SPEC.md | 已读取 | 已采用 M0 至 M5、数据表、接口、页面和验收要求。 |
| Skills | 可识别 | `.agents/skills/` 下存在 `planning-with-files`、`context-compression`、`context-degradation`，均含 `SKILL.md`。 |

## 已有文件与本轮改动登记

在写入前检查了 `backend`、`frontend`、`docs`、`deploy`：

| 目录 | 写入前内容 | 本轮处理 |
|---|---|---|
| `backend` | 空 | 不修改。 |
| `frontend` | 空 | 不修改。 |
| `docs` | `PROJECT_SPEC.md` | 只读，不覆盖；新增三个设计文档。 |
| `deploy` | 空 | 不修改。 |

本轮拟新增并已创建的文件：`PROJECT_PLAN.md`、`progress.md`、`findings.md`、`docs/database-design.md`、`docs/api-contract.md`、`docs/page-design.md`。没有覆盖已有文件。

## 命名与状态决策

| 范畴 | 决策 | 原因 |
|---|---|---|
| 数据库 | 统一使用 `qh_` 表前缀与蛇形字段名 | 与需求和 Redis `qh:` 前缀保持同一产品命名，但不混用。 |
| Java | 实体使用单数 PascalCase，如 `UserCoupon`；请求 DTO 以 `Request` 结尾；响应 VO 以 `VO` 结尾 | 实体、入参和出参可一眼区分。 |
| 路径 | 用户端资源使用复数名词；后台统一 `/api/admin/`；用户与管理员登录分开 | 避免 `/api/user/login` 与管理员登录冲突。 |
| 状态 | 通用 `status` 统一为 `1/0`；商品、订单、优惠券和探店使用各自命名状态字段 | 与首页 `qh_shop.status=1` 条件一致，避免跨域状态混用。 |
| 页面 | 路由使用英文短横线；显示名称不超过五个汉字 | 同时满足 URL 可维护性和页面名称限制。 |

## 冲突、缺失与待确认项

1. **实体/DTO/VO 未落地：** M0 只可审查命名设计，不能声称已检查实际 Java 类。M1 创建后应按本轮映射复核。
2. **后台接口不完整：已解决。** 已在 `docs/api-contract.md` 逐项登记管理员登录、看板以及九个管理域的查询、新增、修改、删除和状态修改路径；不适用的订单/用户动作也已明示。
3. **地址接口缺失：已解决。** 已统一为 `GET/POST /api/addresses`、`PUT/DELETE /api/addresses/{id}`、`PUT /api/addresses/{id}/default`。
4. **订单评价接口缺失：已解决。** 已确定 `POST /api/orders/{id}/review`，统一写入 `qh_comment`；字段、索引和评论类型均已登记。
5. **首页轮播无专表：已解决。** 不增加表；`qh_shop` 增加 `is_featured`、`cover_image`、`sort_order`，首页从 `is_featured=1 AND status=1` 的商铺生成 `banners`。
6. **状态枚举未指定：已解决。** 通用 `status=1/0` 与各业务状态字段已在数据库设计和接口契约中统一。

## 命名冲突核查结论

- 15 张要求表可一一映射到 15 个 Java 实体，无重复。
- 提议的 DTO、VO 均按资源域前缀命名；`AdminLoginRequest` 与 `UserLoginRequest` 不冲突。
- 用户端与后台端路径前缀隔离；`/api/shops/{id}/comments` 仅查询，订单评价使用 `/api/orders/{id}/review`，探店评论使用 `/api/blogs/{id}/comments`，不重复。
- 页面路由在用户端和后台端使用不同前缀，显示名均不超过五字；“商铺”列表和“商铺详情”采用不同路由但不作为两个八类页面重复计数。
- 尚无实际 Java、接口或前端文件，因此不存在已实现代码冲突，也不能完成实现级验证。

## 过程问题

| 问题 | 处理 |
|---|---|
| 初始工作目录是仓库父目录 | 发现子目录 `qinghe-life-service` 后，将后续只读检查和文件写入切换至该目录。 |
| 首次 PowerShell 检查的变量插值语法错误 | 使用 `${s}` 重试，成功识别三项 Skill。 |
| 最终核验脚本的 `$_` 字符串插值解析错误 | 改用具名循环变量和 PowerShell 格式化字符串后完成全部核验。 |
| 最后状态展示脚本对空值调用 `Trim()` 失败 | 不重试；六个文件存在性和需求覆盖核验均已在此前成功完成。 |

## 最终文件核验

`PROJECT_PLAN.md`、`progress.md`、`findings.md`、`docs/database-design.md`、`docs/api-contract.md`、`docs/page-design.md` 均已存在。核验同时确认：15 张需求表、用户端/后台接口前缀以及首页、商铺、购物车、订单、优惠券、探店、我的、后台八类页面均已写入对应设计文档。

## M1 实施发现

| 项目 | 结论 |
|---|---|
| Java 版本 | 构建环境为 Java 21，但 Maven 使用 `release 8`，生成类文件主版本为 52，满足 Java 8 目标。 |
| SQL 与实体 | 15 张表、15 个实体和 15 个 Mapper 已建立；修正核验脚本后，字段逐表一致。 |
| 前端 npm | PATH 中的 `C:\Windows\System32\npm` 未执行实际安装/构建；已改用 `D:\develop\NodeJS\npm.cmd`，依赖安装和 Vite 构建成功。 |
| 前端构建警告 | Vite 对第三方 `@vueuse/core` 注释位置和单个压缩包超过 500 kB 发出警告，不影响构建成功；后续可在页面业务增长后再进行分包优化。 |
| M1 边界 | 后端未创建具体业务 Controller 或 Service；页面仅有导航、标题、占位卡片、空状态和返回操作，未提前实现 M2 至 M5 业务。 |
| 项目范围 | 所有源码、文档、构建产物和前端依赖均写入当前项目目录；未启动服务、连接数据库/Redis 或执行 SQL。Maven 依赖解析使用工具自身的本地缓存，该缓存不属于项目源文件。 |

## M2A 启动基线

- 已确认本轮只使用 `qinghe_life` 和 Redis database 2；不访问 hmdp 数据库。
- `application.yml` 已使用本地 MySQL/Redis环境变量占位表达式，并将 Redis Key 前缀配置为 `qh:`。
- 现有工程没有具体用户业务 Controller 或 Service，适合从认证模块开始实现。

## M2A 验证结果

- 项目数据源 `SELECT 1` 成功；Redis PING 成功；测试使用的 Redis 连接配置为 database 2，所有测试 Key 均为 `qh:` 前缀。
- 集成测试在 `qinghe_life` 创建并在结束时清理测试手机号用户和本次具体 Redis Key；未访问 hmdp 数据库，未执行危险 SQL 或 Redis 清空命令。
- PATH 中的 `npm` 不可靠，前端构建继续使用 `D:\develop\NodeJS\npm.cmd` 成功完成。
- 已删除四个无实际作用的 `package-info.java` 骨架文件；本轮 Maven 测试和打包未出现该类警告。
- 前端构建仍有第三方依赖注释与大包警告，但不影响成功产物；分包优化留给后续页面业务增长后处理。

## 2026-07-10 M2B 恢复审计

- 根目录、`backend`、`frontend`、`docs` 和 `deploy` 均实际存在；写入探针已在项目根目录完成创建、非空检查、路径范围检查和重读验证，随后已删除。
- 当前代码含 M2A 的 `UserController`、`UserService`、`UserServiceImpl` 和认证集成测试；`progress.md` 的 M2A 完成记录与实际代码一致。`PROJECT_PLAN.md` 的 M2 总状态为 `in_progress`，本次已将当前子阶段更新为 M2B 第一批。
- M2B 所需的 `CategoryController`、`CategoryService`、`CategoryServiceImpl`、`CategoryVO` 及后续首页、商铺、商品模块文件均尚不存在。前端目前也没有 `category.js`、`home.js`、`shop.js`、`goods.js`。
- `qh_category` 的实体和 Mapper 已存在；字段为 `id`、`name`、`icon_url`、`sort_order`、`status`。分类公开接口应查询 `status=1`，按 `sort_order` 升序，并须加入登录拦截器白名单。
- 分类模块已按上述规则落地并通过 Maven 编译；M2B 的首页、商铺、商品、商铺缓存和前端部分仍未实现，因此 M2B 不能标记为完成。
- 首页模块已完成：首页摘要将六个规定区域一次性返回；轮播不新增数据表，而是复用启用的推荐商铺。优惠券查询附加时间窗口与剩余库存条件，避免首页展示不可领取的券。
- 首页首次文件验证未能执行 Maven，原因是命令工作目录已位于 `backend`，却仍使用 `backend/src/...` 路径；改为 `src/...` 后成功完成文件检查与 Maven 编译。后续后端目录内验证统一使用 `src/` 相对路径。
- 商铺详情、其商品列表和评论列表均先验证商铺存在且启用；这避免了通过子资源接口读取停用商铺的数据。商铺商品只查询 `ON_SALE`，评论只查询 `status=1`。

## 2026-07-10 M2B 续作恢复

- 项目根目录为当前 `qinghe-life-service`，必需的根文件及 `backend`、`frontend`、`docs`、`deploy` 均存在。
- `task_plan.md` 当前将首页标为完成，将“商铺、商品与缓存”标为进行中；项目记录尚未确认 M2B 全部完成，符合继续审计而非直接宣告完成的要求。
- 本轮不得重做已验证分类模块，不进入 M3A；后续状态更新必须以实际文件存在性、源码内容和最新构建结果为依据。
- 实际源码比旧计划记录更靠前：商品 Controller/Service/ServiceImpl、商铺缓存、四个前端 API 和三个前端真实页面均已存在且可读取，因此本轮无需重复创建，只需完成最新回归验证与文档收口。
- 最新 `mvn -DskipTests compile` 成功；输出为 “Nothing to compile”，说明现有 72 个源码对应的 class 已是最新。上一轮日志已记录首页批次实际编译 63 个源文件、商品/缓存阶段实际编译 72 个源文件。
- 最新 `mvn test` 已实际连接项目配置的 MySQL 与 Redis 并通过 2 个集成测试；测试只删除本次精确 Token、验证码与商铺缓存 Key，没有执行 Redis 清库命令或危险 SQL。
- M2B 最终构建闭环已完成：Maven 干净打包重编译 72 个源文件并生成 JAR，前端 Vite 构建成功。前端仍有第三方注释和大包警告，但不影响产物与 M2B 验收。
- M2B 已满足全部完成条件；明确未进入 M3A，也未实现地址、购物车、订单、优惠券领取、探店写操作或后台管理。
- 最终只读核验确认 24/24 个指定文件与产物存在且非空；后端 JAR、前端 `dist/index.html` 和 `docs/HANDOFF.md` 均已实际落盘。

## 2026-07-10 M3A 结构审计

- `qh_user_address` 当前为 `receiver_name/receiver_phone`；最终决定以实际数据库为准，Java 使用 `receiverName/receiverPhone`，不再重命名数据库列。
- `qh_cart` 已有 `(user_id, goods_id)` 联合唯一索引，但缺少 `shop_id` 和 `selected`。
- `qh_order.order_no` 已有唯一索引，`user_id`/`shop_id` 已有索引；需要新增 `address_id/contact_name/contact_phone`，并将 `address_snapshot/order_status` 对齐为 `delivery_address/status`。
- `qh_order_item.order_id` 已有索引；需要新增 `goods_image`，并将 `price_snapshot/subtotal_amount` 对齐为 `goods_price/subtotal`。
- `qh_goods`、`qh_shop`、`qh_user` 及其实体/Mapper 可支持后续 M3A 业务；当前第一批只实现地址管理。
- 因目标列尚未由用户人工执行到本地数据库，真实地址 CRUD 集成测试处于明确阻塞状态；可安全执行静态检查和 Maven 编译。
- 地址服务使用 `UserContext.getUserId()` 作为唯一用户来源；所有按编号更新、删除和设默认操作都同时匹配 `id` 与 `user_id`，不会返回或修改其他用户地址。
- 地址新增、设置默认和删除默认地址使用事务；更新 DTO 不直接修改默认状态，默认切换只通过专用接口完成。
- 地址模块及结构对齐代码已通过 Maven 编译；真实地址数据库测试必须等用户人工执行增量 SQL 后再补充。
- M2A/M2B 既有两项集成测试在实体对齐后继续通过，说明本批未发现需要重构已完成模块的真实回归缺陷。
- 边界核验确认未创建购物车、订单、优惠券、探店或后台业务服务；M3A 后续批次尚未开始。

## 2026-07-10 地址实际字段纠偏

- 用户提供的数据库截图确认实际地址列为 `receiver_name/receiver_phone`；此前将目标设计改为 contact 命名的决定与实际数据库冲突，必须纠正。
- 最终方案采用 Java `receiverName/receiverPhone`，依赖 MyBatis-Plus 下划线转驼峰映射，不保留 contact 与 receiver 两套同义字段。
- `m3a_increment.sql` 中不得再包含地址列重命名；本轮只读审计确认其他 M3A 表后才能决定是否进入购物车。
- 只读数据库查询确认当前库为 `qinghe_life`。地址 Java 最终使用 `receiverName/receiverPhone`，由 MyBatis-Plus 映射到 `receiver_name/receiver_phone`，地址领域不再保留 contact 命名。
- `qh_cart` 实际列为 `id,user_id,goods_id,quantity,price_snapshot,create_time,update_time`，与 Java `shopId/selected` 目标实体不一致。
- `qh_order` 实际仍使用 `address_snapshot/order_status`，缺少目标实体的 `addressId/contactName/contactPhone/deliveryAddress/status` 对应列。
- `qh_order_item` 实际仍使用 `price_snapshot/subtotal_amount`，缺少 `goods_image`，与 Java `goodsImage/goodsPrice/subtotal` 不一致。
- 因三张表结构门禁失败，本轮不能执行地址真实测试或开始购物车；需要用户重新审核并只执行 `m3a_increment.sql` 中购物车、订单、订单明细的 ALTER 后再继续。
- 地址纠偏代码已通过 Maven 编译（78 个源文件）；本轮没有运行任何写数据库的测试或 SQL。

## 2026-07-10 M3A 七表只读审计基线

- 七个实体的 `id/createTime/updateTime` 继承自 `BaseEntity`；`id` 使用 `@TableId Long`，时间字段使用 `LocalDateTime`。
- 各实体使用正确的 `@TableName`；未声明 `@TableField`，字段映射完全依赖 `map-underscore-to-camel-case=true`。
- 七个 Mapper 均无自定义 SQL，仅使用 MyBatis-Plus `BaseMapper`；仓库内不存在 Mapper XML。
- 目标实体中金额字段均为 `BigDecimal`，数量/库存/选中状态均为 `Integer`。

## 七表差异矩阵（实际数据库 vs Java）

公共映射：所有实体继承 `BaseEntity`；`id BIGINT` → `Long id`（继承的 `@TableId`），`create_time/update_time DATETIME` → `LocalDateTime createTime/updateTime`（下划线转驼峰）。七个 Mapper 均无自定义 SQL。

### qh_user_address

| 数据库列 | 类型 | NULL/默认/索引 | Java 字段/类型 | 映射 | 一致 | 建议 |
|---|---|---|---|---|---|---|
| `id` | bigint | NO；PK；自增 | `id / Long` | `@TableId` | 是 | 保持 |
| `user_id` | bigint | NO；`idx_qh_address_user` | `userId / Long` | 驼峰 | 是 | 保持 |
| `receiver_name` | varchar(64) | NO | `receiverName / String` | 驼峰 | 是 | 保持 |
| `receiver_phone` | varchar(20) | NO | `receiverPhone / String` | 驼峰 | 是 | 保持 |
| `province` | varchar(64) | NO | `province / String` | 同名 | 是 | 保持 |
| `city` | varchar(64) | NO | `city / String` | 同名 | 是 | 保持 |
| `district` | varchar(64) | NO | `district / String` | 同名 | 是 | 保持 |
| `detail_address` | varchar(255) | NO | `detailAddress / String` | 驼峰 | 是 | 保持 |
| `is_default` | tinyint | NO；默认 0 | `isDefault / Integer` | 驼峰 | 是 | 保持 |
| `create_time` | datetime | NO；CURRENT_TIMESTAMP | `createTime / LocalDateTime` | 驼峰 | 是 | 保持 |
| `update_time` | datetime | NO；自动更新时间 | `updateTime / LocalDateTime` | 驼峰 | 是 | 保持 |

### qh_cart

| 数据库列/目标列 | 类型 | NULL/默认/索引 | Java 字段/类型 | 映射 | 一致 | 建议 |
|---|---|---|---|---|---|---|
| `id` | bigint | NO；PK；自增 | `id / Long` | `@TableId` | 是 | 保持 |
| `user_id` | bigint | NO；联合唯一第 1 列 | `userId / Long` | 驼峰 | 是 | 保持 |
| `shop_id`（缺失） | 目标 bigint | 目标应 NO；应支持用户/商铺查询 | `shopId / Long` | 驼峰 | 否 | 新增非空列并增加合理索引 |
| `goods_id` | bigint | NO；联合唯一第 2 列 | `goodsId / Long` | 驼峰 | 是 | 保持联合唯一索引 |
| `quantity` | int | NO；默认 1 | `quantity / Integer` | 同名 | 是 | 保持 |
| `selected`（缺失） | 目标 tinyint | 目标 NO；默认 1 | `selected / Integer` | 同名 | 否 | 新增非空默认 1 |
| `price_snapshot`（遗留） | decimal(10,2) | NO；无默认 | 无 | 无映射 | 否 | 必须移除或放宽，否则 BaseMapper 插入失败 |
| `create_time` | datetime | NO；CURRENT_TIMESTAMP | `createTime / LocalDateTime` | 驼峰 | 是 | 保持 |
| `update_time` | datetime | NO；自动更新时间 | `updateTime / LocalDateTime` | 驼峰 | 是 | 保持 |

### qh_order

| 数据库列/目标列 | 类型 | NULL/默认/索引 | Java 字段/类型 | 映射 | 一致 | 建议 |
|---|---|---|---|---|---|---|
| `id` | bigint | NO；PK；自增 | `id / Long` | `@TableId` | 是 | 保持 |
| `order_no` | varchar(32) | NO；唯一 `uk_qh_order_no` | `orderNo / String` | 驼峰 | 是 | 保持 |
| `user_id` | bigint | NO；`idx_qh_order_user` | `userId / Long` | 驼峰 | 是 | 保持 |
| `shop_id` | bigint | NO；`idx_qh_order_shop` | `shopId / Long` | 驼峰 | 是 | 保持 |
| `address_id`（缺失） | 目标 bigint | 目标应 NO | `addressId / Long` | 驼峰 | 否 | 表为空，可直接设计为非空 |
| `receiver_name`（缺失） | 目标 varchar(64) | 目标 NO | 当前为 `contactName / String` | 名称不对应 | 否 | 统一 receiver 命名后再执行迁移 |
| `receiver_phone`（缺失） | 目标 varchar(20) | 目标 NO | 当前为 `contactPhone / String` | 名称不对应 | 否 | 统一 receiver 命名后再执行迁移 |
| `address_snapshot`（遗留） | varchar(1000) | NO | 无 | 无映射 | 否 | 重命名为 `delivery_address` |
| `delivery_address`（缺失） | 目标 varchar(1000) | 目标 NO | `deliveryAddress / String` | 驼峰 | 否 | 由旧列重命名 |
| `total_amount` | decimal(10,2) | NO | `totalAmount / BigDecimal` | 驼峰 | 是 | 保持 |
| `discount_amount` | decimal(10,2) | NO；默认 0.00 | `discountAmount / BigDecimal` | 驼峰 | 是 | 保持 |
| `pay_amount` | decimal(10,2) | NO | `payAmount / BigDecimal` | 驼峰 | 是 | 保持 |
| `order_status`（遗留） | varchar(20) | NO；默认 PENDING_PAY | 无（当前实体为 `status`） | 不对应 | 否 | 重命名为 `status` |
| `status`（缺失） | 目标 varchar(20) | 目标 NO；PENDING_PAY | `status / String` | 同名 | 否 | 迁移后类型兼容；开发前补枚举/常量 |
| `remark` | varchar(255) | YES；默认 NULL | `remark / String` | 同名 | 是 | 保持 |
| `create_time/update_time` | datetime | NO；自动时间规则 | `LocalDateTime` | 驼峰 | 是 | 保持 |

当前没有订单状态枚举或常量；数据库 varchar 与 Java String 类型兼容，但订单开发前必须补集中状态定义，不能散落魔法字符串。

### qh_order_item

| 数据库列/目标列 | 类型 | NULL/默认/索引 | Java 字段/类型 | 映射 | 一致 | 建议 |
|---|---|---|---|---|---|---|
| `id` | bigint | NO；PK；自增 | `id / Long` | `@TableId` | 是 | 保持 |
| `order_id` | bigint | NO；`idx_qh_order_item_order` | `orderId / Long` | 驼峰 | 是 | 保持 |
| `goods_id` | bigint | NO | `goodsId / Long` | 驼峰 | 是 | 保持 |
| `goods_name` | varchar(100) | NO | `goodsName / String` | 驼峰 | 是 | 保持 |
| `goods_image`（缺失） | 目标 varchar(255) | 目标可空 | `goodsImage / String` | 驼峰 | 否 | 新增可空列 |
| `price_snapshot`（遗留） | decimal(10,2) | NO | 无（当前为 `goodsPrice`） | 不对应 | 否 | 重命名为 `goods_price` |
| `goods_price`（缺失） | 目标 decimal(10,2) | 目标 NO | `goodsPrice / BigDecimal` | 驼峰 | 否 | 由旧列重命名 |
| `quantity` | int | NO | `quantity / Integer` | 同名 | 是 | 保持 |
| `subtotal_amount`（遗留） | decimal(10,2) | NO | 无（当前为 `subtotal`） | 不对应 | 否 | 重命名为 `subtotal` |
| `subtotal`（缺失） | 目标 decimal(10,2) | 目标 NO | `subtotal / BigDecimal` | 同名 | 否 | 由旧列重命名 |
| `create_time/update_time` | datetime | NO；自动时间规则 | `LocalDateTime` | 驼峰 | 是 | 保持；额外 update_time 不构成问题 |

### qh_goods

| 数据库列 | 类型 | NULL/默认/索引 | Java 字段/类型 | 映射 | 一致 | 建议 |
|---|---|---|---|---|---|---|
| `id` | bigint | NO；PK；自增 | `id / Long` | `@TableId` | 是 | 保持 |
| `shop_id` | bigint | NO；`idx_qh_goods_shop` | `shopId / Long` | 驼峰 | 是 | 保持 |
| `name` | varchar(100) | NO | `name / String` | 同名 | 是 | 保持 |
| `description` | varchar(500) | YES | `description / String` | 同名 | 是 | 保持 |
| `price` | decimal(10,2) | NO | `price / BigDecimal` | 同名 | 是 | 保持 |
| `stock` | int | NO；默认 0 | `stock / Integer` | 同名 | 是 | 保持 |
| `sales_count` | int | NO；默认 0 | `salesCount / Integer` | 驼峰 | 是 | 保持 |
| `sale_status` | varchar(16) | NO；默认 ON_SALE | `saleStatus / String` | 驼峰 | 是 | 保持项目既定状态名 |
| `cover_image` | varchar(255) | YES | `coverImage / String` | 驼峰 | 是 | 保持项目既定图片字段名 |
| `create_time/update_time` | datetime | NO；自动时间规则 | `LocalDateTime` | 驼峰 | 是 | 保持 |

用户给出的依赖字段简写为 `image/status`，项目实际稳定设计为 `cover_image/sale_status`；数据库、实体、文档和 M2 接口内部一致，不构成 M3A 阻塞。

### qh_shop

| 数据库列 | 类型 | NULL/默认/索引 | Java 字段/类型 | 映射 | 一致 | 建议 |
|---|---|---|---|---|---|---|
| `id` | bigint | NO；PK；自增 | `id / Long` | `@TableId` | 是 | 保持 |
| `category_id` | bigint | NO；分类索引 | `categoryId / Long` | 驼峰 | 是 | 保持 |
| `name` | varchar(100) | NO | `name / String` | 同名 | 是 | 保持 |
| `address` | varchar(255) | NO | `address / String` | 同名 | 是 | 保持 |
| `phone` | varchar(20) | YES | `phone / String` | 同名 | 是 | 保持 |
| `score` | decimal(3,2) | NO；默认 5.00 | `score / BigDecimal` | 同名 | 是 | 保持 |
| `status` | tinyint | NO；默认 1；推荐复合索引 | `status / Integer` | 同名 | 是 | 保持 |
| `is_featured` | tinyint | NO；默认 0；复合索引 | `isFeatured / Integer` | 驼峰 | 是 | 保持 |
| `cover_image` | varchar(255) | YES | `coverImage / String` | 驼峰 | 是 | 保持 |
| `sort_order` | int | NO；默认 0；复合索引 | `sortOrder / Integer` | 驼峰 | 是 | 保持 |
| `create_time/update_time` | datetime | NO；自动时间规则 | `LocalDateTime` | 驼峰 | 是 | 保持 |

### qh_user

| 数据库列 | 类型 | NULL/默认/索引 | Java 字段/类型 | 映射 | 一致 | 建议 |
|---|---|---|---|---|---|---|
| `id` | bigint | NO；PK；自增 | `id / Long` | `@TableId` | 是 | 保持 |
| `phone` | varchar(20) | NO；唯一 `uk_qh_user_phone` | `phone / String` | 同名 | 是 | 保持 |
| `nickname` | varchar(64) | NO | `nickname / String` | 同名 | 是 | 保持 |
| `avatar_url` | varchar(255) | YES | `avatarUrl / String` | 驼峰 | 是 | 保持项目既定字段名 |
| `gender` | tinyint | NO；默认 0 | `gender / Integer` | 同名 | 是 | 保持 |
| `status` | tinyint | NO；默认 1 | `status / Integer` | 同名 | 是 | 保持 |
| `create_time/update_time` | datetime | NO；自动时间规则 | `LocalDateTime` | 驼峰 | 是 | 保持 |

用户给出的依赖字段简写为 `avatar`，项目实际为 `avatar_url/avatarUrl`；数据库、实体和既有认证接口一致，不构成 M3A 阻塞。

## m3a_increment.sql 安全审计

| 检查项 | 结论 |
|---|---|
| 数据库与表范围 | `USE qinghe_life`，仅操作 `qh_cart`、`qh_order`、`qh_order_item`，未操作地址和其他库。 |
| 危险语句 | 不含 DROP DATABASE、DROP TABLE、TRUNCATE、DELETE；本轮未执行任何 ALTER。 |
| 地址重复修改 | 已移除地址 ALTER，不会再修改 `qh_user_address`。 |
| 现有数据 | 三张表 `COUNT(*)=0`，数据迁移风险低。 |
| NOT NULL/默认值 | `selected` 有默认 1；contact 快照有空字符串默认；但 `shop_id/address_id` 被设计为可空，与目标非空约束不一致。 |
| 字段重命名 | 订单明细重命名与 Java一致；订单状态/地址重命名与 Java一致；订单 contact 快照与本轮 receiver 目标不一致。 |
| 遗留字段 | 脚本未处理 `qh_cart.price_snapshot`，其仍为 NOT NULL 且无默认，未来 BaseMapper 插入会失败。 |
| 索引 | 不重复现有联合唯一/订单号索引；新增 `(user_id, selected)` 不重复，但缺少合理的 `(user_id, shop_id)` 查询索引。 |
| 幂等性 | 非幂等。第二次执行会因重复字段/索引或旧列已被重命名而失败，只能执行一次。 |
| 最终判断 | 当前版本不建议直接执行；应先修订购物车遗留列、非空约束、订单 receiver 命名和用户/商铺索引。 |

## 阻塞结论

- 地址真实测试：按本轮门禁规则仍被三张主表不一致阻塞；地址表自身已一致。
- 购物车开发：阻塞。实际表缺少 `shop_id/selected`，遗留非空 `price_snapshot`，索引不足。
- 订单开发：阻塞。订单及明细列名与 Java/目标设计不一致，且尚无订单状态枚举/常量。
- 执行前：先修订增量 SQL；确认三张表均为空；明确 receiver 快照命名；将新外键语义列设为合理非空；处理 `price_snapshot`；补用户/商铺索引。
- 执行后：重新核验三表全部列、NULL/default、联合唯一索引、用户/商铺索引、订单号唯一索引和明细 order_id 索引；随后再运行地址真实测试。

## 2026-07-10 M3A 结构门禁修订启动

- 已恢复到 M3A 结构门禁阶段：地址表真实列和 Java 映射一致，待修订范围仅为 `qh_cart`、`qh_order`、`qh_order_item` 的增量 SQL、目标映射与相关设计记录。
- 本轮不读取仓库外附件；项目规则仅允许访问当前仓库。现有 `PROJECT_PLAN.md`、`progress.md`、`findings.md` 与 `task_plan.md` 已提供足够的已审计事实。
- 修订目标为：消除购物车未映射且非空的 `price_snapshot` 插入阻塞，使必要新增关系列满足目标非空语义，统一订单收货快照为 receiver 命名，并补足用户/商铺查询索引；SQL 保持仅生成、需人工审核后才可执行。
- 首次静态门禁把增量 SQL 注释中的 `qh_user_address` 误判为表操作，检查在 Maven 调用前停止；未执行 SQL、未连接数据库，也未运行编译。后续静态检查应剥离注释后再审计实际 DDL。
- 第二次静态检查错误地要求设计文档包含物理索引名 `idx_qh_cart_user_shop`；设计文档已正确记录 `(user_id, shop_id)` 的索引字段组合。后续检查分离物理 SQL 名称与设计语义验证。

## 2026-07-10 M3A 结构门禁修订结果

- `m3a_increment.sql` 已改为只操作 `qh_cart`、`qh_order`、`qh_order_item`：`qh_cart.shop_id` 与 `qh_order.address_id` 为 `NOT NULL`；购物车移除遗留的必填 `price_snapshot`；新增 `(user_id, selected)` 与 `(user_id, shop_id)` 索引。
- 订单的收货快照已统一为数据库 `receiver_name/receiver_phone` 与 Java `receiverName/receiverPhone`；`qinghe_life.sql`、`Order.java` 和 `docs/database-design.md` 已同步，未保留 contact 命名。
- 增量脚本前置条件明确为三张待迁移表均为 0 条记录且仍使用 M2B 旧字段；脚本非幂等，仅能经 DataGrip 人工审核后执行一次。
- 静态门禁已验证所需 ALTER、列重命名、索引、无地址 ALTER、无危险数据库/Redis语句，以及 SQL/实体/设计映射一致；`mvn -DskipTests compile` 成功，重新编译 78 个源文件。
- 未执行 SQL、未写入数据库、未运行地址真实集成测试，也未开始购物车、订单或 M3B 业务代码。下次只能在人工执行后重新进行只读结构审计。

## 2026-07-10 M3A 人工迁移后复核启动

- 用户已明确确认在 DataGrip 中人工执行一次 `backend/src/main/resources/sql/m3a_increment.sql`；当前项目记录仍停留在执行前，因此本轮必须以实际数据库元数据和当前工作区文件重新判定状态。
- 本轮只允许四表只读复核、地址模块真实集成测试、已有回归测试、Maven 打包和指定交接文档更新；四表任一差异都会立即阻止地址测试与后续业务。

## 2026-07-10 M3A 四表实际结构复核

| 表 | 实际结构与索引 | 结果 |
|---|---|---|
| `qh_user_address` | receiver 字段、`user_id` 索引、默认值和时间字段均正确 | 通过 |
| `qh_cart` | 具有必填 `shop_id`、默认值为 1 的 `selected`；无 `price_snapshot`；存在 `(user_id, goods_id)` 唯一、`(user_id, selected)` 与 `(user_id, shop_id)` 索引 | 通过 |
| `qh_order` | receiver 快照、`delivery_address`、`status`、三项 DECIMAL 金额列已落库；无旧列；订单号唯一索引存在 | 通过 |
| `qh_order_item` | 图片、价格与小计列已落库；无旧列；`order_id` 索引存在 | 通过 |

- 只读查询确认当前库为 `qinghe_life`，`SELECT 1` 成功；四张表计数均为 0。未访问 `hmdp`，未执行任何结构或数据修改语句。
- MySQL 实际列与 `UserAddress`、`Cart`、`Order`、`OrderItem`、`qinghe_life.sql`、`database-design.md` 和 `m3a_increment.sql` 的预期结果一致，地址真实测试门禁已放行。

## 2026-07-10 M3A 地址真实集成测试

- 新增 `AddressIntegrationTest`，通过真实 HTTP 登录与地址接口覆盖未登录 401、首地址默认、默认切换、receiver 映射、当前用户隔离、越权更新/删除拒绝、参数校验、删除默认地址补选和请求后的 `UserContext` 清理。
- 测试创建的地址收件人及测试用户昵称均采用 `M3A_ADDR_TEST_` 标识；清理按两个固定测试手机号和该标识精确删除地址、用户、验证码及 Token Key，不执行整表清理。
- 单独运行 `mvn -Dtest=AddressIntegrationTest test` 成功：1 项测试，0 失败，0 错误。

## 2026-07-10 M3A 最终一致性矩阵（实际数据库）

公共映射：`id`、`create_time`、`update_time` 由 `BaseEntity` 提供，分别映射为 `Long`、`LocalDateTime`、`LocalDateTime`；MyBatis-Plus 全局启用下划线转驼峰。`qh_order_item.update_time` 是通用审计字段，目标字段清单虽未单列但与实体和基线 SQL 一致，不构成差异。

| 表 | 数据库列 / 类型 | NULL / 默认值 | 索引 | Java 字段 / 类型 | 映射方式 | 一致性 |
|---|---|---|---|---|---|---|
| `qh_user_address` | `user_id BIGINT`；`receiver_name VARCHAR(64)`；`receiver_phone VARCHAR(20)`；省市区 `VARCHAR(64)`；`detail_address VARCHAR(255)`；`is_default TINYINT` | 业务列均非空；`is_default=0` | PK `id`；`idx_qh_address_user(user_id)` | `userId Long`；`receiverName String`；`receiverPhone String`；地址字段 `String`；`isDefault Integer` | 下划线转驼峰 | 是 |
| `qh_cart` | `user_id/shop_id/goods_id BIGINT`；`quantity INT`；`selected TINYINT` | 所有业务列非空；`quantity=1`、`selected=1`；无 `price_snapshot` | PK；`uk_qh_cart_user_goods(user_id,goods_id)`；`idx_qh_cart_user_selected`；`idx_qh_cart_user_shop` | `userId/shopId/goodsId Long`；`quantity/selected Integer` | 下划线转驼峰 | 是 |
| `qh_order` | `order_no VARCHAR(32)`；用户/商铺/地址 `BIGINT`；receiver 快照 `VARCHAR`；`delivery_address VARCHAR(1000)`；金额 `DECIMAL(10,2)`；`status VARCHAR(20)`；`remark VARCHAR(255)` | 除 `remark` 外非空；`discount_amount=0.00`、`status=PENDING_PAY`；无旧地址/状态列 | PK；`uk_qh_order_no(order_no)`；用户、商铺索引 | 编号/关联字段 `Long`；receiver 与地址 `String`；金额 `BigDecimal`；`status/remark String` | 下划线转驼峰 | 是 |
| `qh_order_item` | `order_id/goods_id BIGINT`；名称/图片 `VARCHAR`；`goods_price/subtotal DECIMAL(10,2)`；`quantity INT` | `goods_image` 可空；其余业务列非空；无旧快照列 | PK；`idx_qh_order_item_order(order_id)` | 关联字段 `Long`；名称/图片 `String`；金额 `BigDecimal`；数量 `Integer` | 下划线转驼峰 | 是 |

- 用户已确认一次人工执行 `m3a_increment.sql`；本轮通过实际 `SHOW COLUMNS`、`SHOW INDEX`、`SELECT COUNT(*)` 和 `SELECT 1` 确认迁移结果。未再次执行脚本，未访问 `hmdp`。
- 完整回归 `mvn test` 通过：3 项测试，0 失败，0 错误；`mvn clean package -DskipTests` 通过。仅新增地址集成测试，未实现购物车、订单、优惠券或 M3B。
- 测试结束只读计数：`M3A_ADDR_TEST_` 用户 0、地址 0，确认未遗留本轮测试数据。
- 最终源码标识检查首次因历史文档保留了检查命令文本而误报；未修改文件。后续检查仅覆盖后端 Java 与前端源代码，避免将记录性文字当作源码。

## 2026-07-10 M3A 购物车后端批次启动

- 当前文件系统显示 M2A、M2B 与 M3A 地址模块已完成并通过回归；`Cart` 实体和基础 Mapper 已存在，但尚无购物车 Controller、Service、DTO、VO 或购物车集成测试。
- 本轮只允许购物车后端、真实测试和文档收口；开始写代码前必须重新确认 `qh_cart`、`qh_goods` 与 `qh_shop` 的实际稳定字段和索引。

## 2026-07-10 M3A 购物车结构门禁

- 当前库为 `qinghe_life`，`SELECT 1` 成功。`qh_cart` 与 `Cart` 完全一致：无 `price_snapshot`，所有业务列非空，`selected` 默认 1，且用户商品唯一、用户选中和用户商铺索引均存在。
- `qh_goods` 的实际稳定字段为 `cover_image`、`sale_status`、`price DECIMAL(10,2)`、`stock`；`qh_shop` 的稳定字段包含 `id`、`name`、`status`。均与现有实体一致，购物车实现不得使用 `image` 等未落库列名。
- 本次只执行连接检查、`SHOW COLUMNS` 与 `SHOW INDEX`；未访问 `hmdp`，未修改数据库或结构。

## 2026-07-10 M3A 购物车后端实现与单测

- 新增购物车 Controller、Service、实现类、三类 DTO、三类 VO 及 `CartIntegrationTest`。所有接口默认受登录拦截器保护，业务用户编号只由 `UserContext` 获取。
- 重复添加采用参数化的条件原子更新：更新语句与当前 `qh_goods`、`qh_shop` 联结，只有商品在售、商铺启用且累加后不超过实时库存才增加数量。无现有记录时以 `INSERT ... SELECT` 从当前商品插入；遇到联合唯一键并发冲突后重试原子更新。因此不删除唯一索引，也不会生成重复记录或超库存数量。
- 查询先一次取当前用户购物车，再批量取相关商品和商铺并用 Map 关联；未修改商铺缓存逻辑，未在循环内逐条查询商品或商铺。金额由当前商品 `BigDecimal price × quantity` 计算，选中统计只累计 `selected=1` 项。
- `mvn -DskipTests compile` 成功（87 个主源码）；`mvn -Dtest=CartIntegrationTest test` 成功（1 项、0 失败、0 错误）。

## 2026-07-10 M3A 购物车批次最终验证

- `mvn test` 成功：`CartIntegrationTest`、`AddressIntegrationTest` 与 `UserAuthenticationIntegrationTest` 共 4 项测试，0 失败、0 错误；既有用户认证、首页、商铺、商品和缓存回归继续通过。
- `mvn clean package -DskipTests` 成功，重新编译 87 个主源码和 3 个测试源码并生成 JAR。
- 只读残留检查结果：`M3A_CART_TEST_` 用户、商铺、商品、购物车均为 0。测试清理仅删除本轮创建的购物车、商品、商铺、用户和精确 Redis Key；未删除两条演示商铺、两条演示商品或现有用户数据。
- 购物车后端批次完成；M3A 仍为 `in_progress`，下一批为地址与购物车前端接入，订单、支付、优惠券和 M3B 均未开始。

## 2026-07-10 M3A 地址与购物车前端批次启动

- 恢复指令列出 `frontend/src/utils/http.js`，但该路径在当前仓库不存在；本轮会定位并复用实际 HTTP 封装，不能仅按任务文字臆造第二个 Axios 实例。

## 2026-07-10 M3A 前端基线

- 实际 HTTP 封装是 `frontend/src/api/http.js`，它已统一附加 Bearer Token 并处理 401 跳转；任务列出的 `frontend/src/utils/http.js` 不存在，此差异不阻塞实现。
- 当前路由已保护 `/cart`、`/profile`，导航已有购物车入口；`CartView` 与 `ProfileView` 仍为骨架，商铺详情使用真实商品接口但尚无加购操作。

## 2026-07-10 M3A 地址与购物车前端实现

- 新增 `frontend/src/api/address.js` 与 `cart.js`，均复用 `api/http.js`；没有第二个 Axios、前端 `userId`、购物车金额提交或订单请求。
- 新增地址页面并接入 `/profile/addresses` 登录路由；地址列表、表单、删除确认、默认地址、必填/手机号校验、加载/空/错误/提交状态均已实现。号码仅在展示时脱敏，编辑仍提交后端提供的原始号码。
- 购物车页面使用真实 `CartSummaryVO`，支持按商铺分组、数量和选择修改、删除、清空、选中汇总及订单未实现提示。失败与成功操作均重新读取服务端状态。
- 商铺详情已使用真实 `addCart`，未登录保留 `redirect` 跳转登录，商品下架或库存为 0 时禁用操作。
- `D:/develop/NodeJS/npm.cmd run build` 成功；保留 `@vueuse/core` 注释位置提示和单个压缩包超过 500 kB 的非阻断警告。

## 2026-07-10 M3A 前端批次静态收口

- 九个前端基线文件均存在且非空：地址/购物车 API、地址/购物车/商铺详情/我的页面、路由、用户导航和现有 HTTP 封装均只有一套。静态检查确认未出现 `contactName`、`contactPhone` 或订单 API 请求。
- 前端生产构建已通过；后端 `mvn test`（4 项、0 失败）和 Maven 打包已在本轮前端代码落盘后通过。
- 浏览器自动联调因浏览器会话异常未完成。本轮禁止重试浏览器或启动新实例；需要用户手动启动前后端并在外部浏览器验证登录、地址新增/编辑/默认/删除、商铺详情加购、购物车数量/选择/删除/清空和刷新后的服务端数据。
- 自动浏览器联调未完成不是业务代码验收通过的替代说明。M3A 仍为 `in_progress`，下一批为订单创建与查询后端；支付、优惠券和 M3B 均未开始。

## 2026-07-10 M3A 前端最终命令行验收启动

- 用户确认后端 JAR 已在 PID 36220/8090 运行，Vite 已在 5174 运行；本轮只对现有进程做 curl 冒烟，不启动、停止或重启任何进程，也不使用浏览器自动化。
- 复核确认实际 HTTP 封装仍为 `frontend/src/api/http.js`，不存在任务文字所列的 `frontend/src/utils/http.js`；前端 API 均复用前者，不存在第二个 Axios 实例。

## 2026-07-10 M3A 前端最终命令行验收

- 新对话已通过仓库持久化文件恢复状态。实际静态核验显示地址 API `address.js`、购物车 API `cart.js`、地址页 `AddressView.vue`、购物车页 `CartView.vue` 各只有一套；它们均从 `frontend/src/api/http.js` 导入同一 HTTP 实例。地址路由 `/profile/addresses` 受用户守卫保护，用户导航包含 `/cart`，Profile 包含地址入口，商铺详情调用真实 `addCart`。
- 全量前端源扫描仅在 `frontend/src/api/http.js` 发现 `axios` 与 `axios.create`；未发现硬编码 `http://localhost:8090`、客户端 `userId`、`contactName/contactPhone`、地址/购物车重复页面或 API、订单 HTTP 请求、支付成功或结算成功逻辑。`/orders` 仅出现在既有用户/后台导航和路由占位，不构成订单接口调用。
- 任务线索称 8090 曾由 JAR 监听，但本轮只读端口检查的实际事实为 8090 无监听；因此没有运行任何 curl，也没有为更新运行实例而操作进程。5174 正在 `::1:5174` 监听。该差异已同步至 HANDOFF，记为“人工联调环境后端未运行”。
- `D:/develop/NodeJS/npm.cmd run build` 成功：1681 个模块、`frontend/dist` 已生成。`@vueuse/core` 两条注释位置提示和 1105.63 kB 主包大小警告均未阻断构建。
- `mvn test` 成功：4 项测试、0 failure、0 error、0 skipped；地址、购物车以及 M2A/M2B 认证回归持续通过。`mvn clean package -DskipTests` 成功，编译 87 个主源码和 3 个测试源码，并生成 JAR。
- `docs/api-contract.md` 发现地址 `PUT` 行仍使用泛称“联系人、电话”，与实际请求和其他契约的 `receiverName/receiverPhone` 不一致；本轮已作最小文档纠正，未改业务代码、数据库结构或迁移脚本。
- 浏览器自动化未执行。前端生产构建和静态核验不能替代用户在外部浏览器执行地址 CRUD、商铺加购、购物车操作、刷新回读和未登录 redirect 的人工联调；M3A 继续保持 `in_progress`，订单创建与查询后端是待审核后的下一批。

## 2026-07-10 M2A 验证码问题初步恢复

- 实际认证接口为匿名 `POST /api/user/code`（JSON `{ "phone": "..." }`）和 `POST /api/user/login`（JSON `{ "phone": "...", "code": "..." }`）；前端 `api/user.js` 与 `UserController` 的路径、方法和 JSON 参数一致。
- 当前 `LoginView.vue` 确实绑定了 `getCode` 并调用 `sendCode`，但请求期间没有 loading 或禁用状态，失败分支为空，定时器只在自然结束时清理，且成功提示直接写入 `123456`。这解释了请求失败时用户看不到明确按钮反馈，也不满足本地开发验证码的环境隔离要求。
- 当前 `UserServiceImpl` 使用 `RedisKeys.code(phone)` 与 5 分钟 TTL 保存验证码，登录读取同一 Key、成功后删除；但验证码常量在所有环境固定为 `123456`，且 Redis 写入异常未转换为明确业务错误。后续仅在该认证链路内最小修复。

## 2026-07-10 M2A 验证码修复与真实回归

- 真实接口和参数没有不一致：匿名 `POST /api/user/code` 接收 JSON `{ "phone": "..." }`，匿名 `POST /api/user/login` 接收 JSON `{ "phone": "...", "code": "..." }`；`LoginInterceptor` 明确将二者置于 POST 匿名白名单。无需新建认证接口或 Axios 实例。
- 前端根因是交互缺口而非路径错误：按钮原先无发送中 loading/禁用、失败分支为空、倒计时无法卸载清理，并在 UI 硬编码 `123456`。现已保留原请求封装，补齐 loading、禁用、60 秒倒计时、成功/错误消息、`native-type="button"` 和 `onBeforeUnmount` 清理。
- 后端根因是环境隔离不足：原逻辑全环境固定 `123456` 且登录必须同时匹配该常量与缓存值。现默认 profile 为 `dev`，`application-dev.yml` 提供本地测试码；只有 `dev/local` 记录带“仅本地开发环境”标记的验证码日志。非开发环境生成随机 6 位数字，登录仅比较 `RedisKeys.code(phone)` 的实际值。响应不包含验证码，生产不会命中该日志分支。
- Redis 约定保持一致：Key 为 `qh:login:code:{phone}`，String 序列化，Redis database 2，TTL 为 5 分钟；发送限流锁为 `qh:login:code:send:{phone}`、60 秒。Redis 写入异常转换为 `code=503` 的“验证码服务暂不可用”，不写 MySQL；成功登录后删除验证码 Key，Token 保持 `qh:login:token:{token}` 30 分钟。
- 真实集成测试通过：`UserAuthenticationIntegrationTest` 3 项、地址 1 项、购物车 1 项，共 5 项，0 failure、0 error。认证测试确认 Key 存在、TYPE=STRING、TTL>0、匿名发送、错误/不存在/过期拒绝、成功登录、验证码一次性使用、Token Hash 和原有 `UserContext` 行为；测试仅精确删除 `M2A_CODE_TEST_` 用户和本次验证码/Token Key。
- 构建通过：Maven 打包生成 JAR；前端构建转换 1681 个模块并生成 `dist`。8090/5174 均未监听，故没有 curl 运行实例证据，也未操作进程或浏览器。

## 2026-07-10 用户报告验证码 404 的调用链核验

- 实际调用链为：登录页 `getCode` → `sendCode(form.phone)` → `http.post('/user/code', {phone})` → `http.js` baseURL `/api`，因此浏览器请求 URL 是 `http://localhost:5174/api/user/code`。
- `frontend/vite.config.js` 只有 5174 端口配置，完全没有 `server.proxy`；故请求未被转发，直接由 Vite 5174 返回 404。后端 IDEA 没有请求日志是预期结果，因为 8090 从未收到该请求。
- 后端唯一映射为 `UserController` 类级 `/api/user` 加 `@PostMapping('/code')`，即 `POST http://localhost:8090/api/user/code`，请求体为 JSON `{ "phone": "13900009991" }`；登录为同类级路径下 `POST /api/user/login`，JSON `{ "phone": "...", "code": "..." }`。`LoginInterceptor` 已把两个 POST 接口纳入匿名白名单。
- 源码未发现第二套认证接口、重复点击事件或表单默认提交：按钮使用唯一 `@click="getCode"` 且 `native-type="button"`。现有 `http.js` 对业务响应自行提示，登录页也提示 catch 错误；本轮将收敛为单层提示，避免同一次失败多次弹窗。
- 验证码登录状态更正为 `awaiting_manual_verification`。自动测试和构建不能覆盖实际 Vite 代理，必须等待用户外部浏览器在修改后的前端开发服务上再次验证。

## 2026-07-10 验证码 404 修复与验证边界

- Vite 最终规则为 `server.proxy['/api'] -> http://localhost:8090`，没有 rewrite。浏览器请求 `/api/user/code` 保留 `/api` 前缀转发，准确匹配 `UserController` 的 `POST /api/user/code`；登录同理为 `/api/user/login`。这修复了 5174 本地 404，且不创建第二套认证路径。
- 用户看到 IDEA 无请求日志的直接原因是旧 Vite 未代理，请求从未到达 8090，不是 `LoginInterceptor` 拦截、Controller 不存在、HTTP 方法不匹配或 JSON 参数不匹配。`LoginInterceptor` 的匿名 POST 白名单已覆盖 `/api/user/code` 与 `/api/user/login`。
- 认证日志现在只记录必要信息：发送/Redis 成功/登录请求使用脱敏手机号，验证码缺失或错误记录失败类别，成功记录用户编号；仅 `dev/local` 追加 `[LOCAL DEV ONLY] phone=..., code=..., ttlMinutes=5`。没有生产明文验证码或完整 Token 日志。
- 对 HTTP 业务错误与传输错误统一设置 `__qingheMessageShown`，登录页兜底提示先检查该标记，避免 HTTP 层和页面层对同次失败重复弹窗。按钮本身仍只有一个 `@click="getCode"`，并指定 `native-type="button"`，未发现重复事件或表单提交。
- 2026-07-10 回归通过：5 项 Maven 测试、0 failure、0 error；Maven 打包和前端生产构建通过。该结果只确认代码/Redis/接口自动测试，不确认真实 Vite 运行实例；验证码功能状态必须保持 `awaiting_manual_verification`，直至用户重启 IDEA/Vite 并完成外部浏览器人工验证。

## 2026-07-10 Redis 登录双拦截器重构审计

- 现有 `LoginInterceptor` 同时读取 Bearer Token、查询 Redis Hash、写入 `UserContext`、刷新 TTL 和拒绝未登录；这不符合刷新与鉴权职责分离。现有 `UserContext` 已是项目唯一的 ThreadLocal 用户容器，可直接作为用户 holder 复用，无需新增平行模块。
- 现有 `UserDTO.toMap()` 已将 Hash 字段转换为字符串，Token Key 已是 `qh:login:token:{token}`、Hash、30 分钟；前端已有统一 Bearer 请求头、Pinia Token 持久化和 401 清理/redirect。
- 待修复的认证差异：`application-dev.yml` 固定 `123456`，`UserServiceImpl` 在 dev 复用该值；`codeLock` 使同一手机号 60 秒内不能刷新验证码；验证码 TTL 为 5 分钟且未集中到 `RedisKeys` 常量。将改为随机六位、2 分钟、重新发送覆盖并刷新 TTL，仅保留开发日志查看。

## 2026-07-10 Redis 登录双拦截器实现与验证

- 已删除固定验证码 Profile 文件。验证码使用随机 6 位数字（明确排除历史固定值 `123456`），每次 `POST /api/user/code` 覆盖 `qh:login:code:{phone}` 并刷新 2 分钟 TTL；生产响应不含验证码，`dev/local` 日志仅输出 `[LOCAL DEV ONLY] phone=138****0001, code=xxxxxx, ttlMinutes=2`。
- `RedisKeys` 集中定义 `LOGIN_CODE_KEY`、`LOGIN_CODE_TTL_MINUTES=2`、`LOGIN_TOKEN_KEY`、`LOGIN_TOKEN_TTL_MINUTES=30`。服务层和刷新拦截器均使用该常量，Token Hash 只写 `UserDTO.toMap()` 的字符串安全字段。
- `RefreshTokenInterceptor`（order 0）负责 Bearer Token、Redis Hash、`UserContext`、30 分钟续期和 `afterCompletion` 清理；`LoginInterceptor`（order 1）只读取 `UserContext`。`UserContext` 是现有 ThreadLocal 用户 holder，未新增平行用户模块。
- 前端已有 Token 持久化、Bearer 请求头、401 清理和 redirect，本轮只新增已登录昵称/退出入口；未创建第二套 Axios、Store 或认证 API。
- 真实集成测试未能完成，根因是 Redis `192.168.100.128:6379` 连接超时。该失败发生在测试所需真实 Redis 操作，导致地址/购物车验证码发送返回 503、认证测试清理异常；不是编译或断言失败。跳过测试的 Maven 打包和前端构建均成功。登录状态继续为 `awaiting_manual_verification`，且真实 Redis 回归也待环境恢复后重跑。

## 2026-07-11 全局异常处理体系审计

- 当前项目仅有 `com.qinghe.life.exception.GlobalExceptionHandler` 一个全局异常处理器；它仅覆盖业务异常、两类绑定校验和兜底异常，校验提示固定且未知异常提示与当前要求不一致，未记录日志，也没有重复键、完整性、请求方法或基础设施异常处理。
- `Result` 固定字段为 `code`、`message`、`data`；`BusinessException` 自带业务 `code`，项目没有额外错误码枚举，故本轮必须直接保留其原始业务码并仅在安全的服务端日志中记录类别。
- 现有 DTO 使用 `@NotBlank`、`@NotNull`、`@Pattern`、`@Min/@Max`、`@Size`，但多数默认消息不适合直接响应；异常处理器应从绑定字段与约束类型生成中文字段级提示，不能返回异常原文。
- 基线 SQL 的唯一约束名为 `uk_qh_user_phone`、`uk_qh_admin_username`、`uk_qh_category_name`、`uk_qh_cart_user_goods`、`uk_qh_order_no`、`uk_qh_user_coupon`、`uk_qh_comment_order`、`uk_qh_blog_like`、`uk_qh_blog_favorite`。映射只能使用约束名白名单，未知约束必须回退为“数据已存在，请勿重复提交”，不得返回 SQL、表名或驱动异常文本。
- 已有 `UserAuthenticationIntegrationTest`、`AddressIntegrationTest`、`CartIntegrationTest` 会通过真实 MySQL/Redis 启动 Spring 上下文；其中认证测试已有未登录 HTTP 401 断言。全局异常新增测试应避免写入数据库或 Redis，并由完整 `mvn test` 同时验证既有回归。

## 2026-07-11 全局异常处理体系最终结果

- `GlobalExceptionHandler` 现有 12 个显式异常处理声明，具体异常先于 `Exception` 兜底；Spring 全量测试上下文成功启动，未出现 `Ambiguous @ExceptionHandler method mapped`。
- 参数错误按字段与约束类型生成固定中文提示，JSON 不可读固定返回“请求体格式错误，请检查 JSON 数据”。前端不接收任何 `getMessage()` 原文、SQL、表名或驱动异常文本；业务异常使用 `BusinessException.userMessage` 保持既有业务码和提示。
- 重复键识别采用预编译的唯一索引名白名单模式，遍历异常因果链进行完整约束名匹配，不用固定位置截取数据库错误文本；所有九个基线唯一约束均有映射，未知约束回退为“数据已存在，请勿重复提交”。
- `GlobalExceptionHandlerTest` 的 8 项测试与既有地址 1 项、购物车 1 项、认证 3 项合计 13 项，`mvn test` 全部通过（0 failure、0 error、0 skipped）。认证未登录 401、地址和购物车回归均保持通过。
- `mvn clean package -DskipTests` 通过并生成可执行 JAR；仅有新增测试引用已弃用 Spring 测试 API 的非阻断编译提示。未启动或停止任何进程，未执行数据库或 Redis 写操作。

## 2026-07-11 前端 UI/UX 改造现状审计

- 当前前端只有 `frontend/src/assets/base.css` 一份全局样式，但内容仅含基础白色卡片和占位网格；可在此文件建立唯一设计变量与公共样式，避免新增平行主题文件。
- `UserLayout` 已复用现有 Pinia 登录态和唯一 Axios；导航没有品牌图形、购物车角标、响应式行为或底部信息。登录页不在本轮范围。
- 首页、商铺列表、商铺详情、购物车、地址和“我的”均调用真实 API。商铺列表的 `getShops` 已传递 `page`、`size`、`categoryId`、`keyword`、`sort`，响应使用 `records` 与 `total`，可实现真实服务端分页。
- 订单、优惠券、探店当前是 `PageScaffold` 占位页面，本轮不实现或伪造这些未完成业务；仅由全局视觉规范保持基础一致性。
- 地址字段已统一为 `receiverName/receiverPhone`，购物车只使用服务端 `CartSummaryVO`、`CartItemVO`；不得改为旧 contact 命名或添加客户端用户编号、金额、订单数据。

## 2026-07-11 前端 UI/UX 改造实施记录

- 全局视觉规范已落在唯一 `frontend/src/assets/base.css`：蓝色主色、语义色、背景/文字/边框、8/12/16/24/32 间距、8/12/16 圆角、轻量卡片阴影和 1200px 内容宽度。
- 新增的复用组件为 `PageHeader`、`AsyncState`、`ShopCard`、`GoodsCard`、`StatusTag`。页面请求仍使用原 API 文件和现有 `http.js`，没有新增 Axios、Router 或 Pinia 实例。
- 用户导航已加入文字标识、图标化当前态、横向滚动小屏导航、真实购物车总数量角标、登录头像昵称和底部信息。购物车角标仅在已登录且 `GET /api/cart` 成功时显示服务端 `totalCount`。
- 首页使用现有 `HomeSummaryVO` 的分类、推荐商铺、热门商品、优惠券、探店和 banners；无数据只显示 Empty，失败可重试，不填充虚假商铺、商品、优惠券或统计数据。
- 商铺列表已将服务端 `records/total` 接至 Element Plus Pagination，布局为 `total, sizes, prev, pager, next, jumper`；page 或 size 改变都会重新调用 `GET /api/shops`，不是前端数组分页。

## 2026-07-11 前端 UI/UX 改造最终验证

- 首次 `D:/develop/NodeJS/npm.cmd run build` 因 `HomeView.vue` scoped CSS 末尾的孤立 `.` 报 PostCSS `Unknown word`；已按错误位置删除该字符，第二次构建成功。
- 最终构建转换 1692 个模块，生成 `frontend/dist/index.html`、CSS 与 JavaScript 产物。没有未定义组件、错误 import 或路由编译错误。
- 静态扫描确认：`frontend/src` 只有 `api/http.js` 中的一次 `axios.create`；`contactName/contactPhone`、`localhost/127.0.0.1` 和订单/支付 HTTP 请求均为 0。
- 当前未进行浏览器或运行服务验证，故不将构建成功视为 UI 交互验收。需要用户在其已启动的 VS Code 前端与 IDEA 后端环境中按 HANDOFF 清单验证。
- 非阻断构建警告仍来自第三方 `@vueuse/core` 两条 `/* #__PURE__ */` 注释位置，以及压缩后 1124.92 kB 主包超过 Vite 500 kB 提示；本轮未为消除警告改动依赖或拆分业务路由。

## 2026-07-11 账号注册前置审计

- 以当前基线 SQL、`docs/database-design.md` 与 `User` 实体为准，`qh_user` 仅含 `phone`、昵称、头像、性别和状态；不存在 `username`、`password_hash` 或 `uk_qh_user_username`，而手机号唯一索引 `uk_qh_user_phone` 已存在。
- 现有认证只有 `/api/user/code`、`/api/user/login`，验证码使用唯一 `RedisKeys.code(phone)`，即 `qh:login:code:{phone}`，TTL 为 2 分钟；Token 使用 `qh:login:token:{token}` Hash。不得创建第二套验证码或 Token 体系。
- 因账号字段缺失，注册实现受用户设定的迁移门禁阻塞。本轮只可准备实体、基线 SQL、一次性迁移 SQL、重复键映射和文档；不得创建注册接口/页面或宣称真实注册测试通过。
- 为兼容旧手机号验证码登录用户，账号字段在迁移期间必须允许 `NULL`：`username/password_hash` 均为空代表尚未绑定账号；服务层在后续注册时只允许同手机号旧用户原地补齐账号字段，不新建用户、不改变 id、地址或购物车关系。新注册用户必须同时写入两个字段与 BCrypt Hash。

## 2026-07-11 账号注册准备结果

- 已新增最小 `spring-security-crypto` 依赖，为后续 `BCryptPasswordEncoder` 提供实现；未引入完整 Spring Security 登录体系，也没有新增账号密码登录逻辑。
- `User` 已映射 `username` 与 `passwordHash`，基线 SQL 已同步新增 `username`、`password_hash`、`uk_qh_user_username`。由于历史手机号用户必须可识别为“未绑定账号”，账号字段在迁移和基线中允许 `NULL`，注册服务的业务校验将强制新注册和绑定操作同时填写两者。
- 已生成一次性 `account_register_increment.sql`，包含两列安全 `ALTER TABLE` 和用户名 `CREATE UNIQUE INDEX`，无 DROP、TRUNCATE、清表或自动执行逻辑。
- 全局重复键映射已登记 `uk_qh_user_username`→“用户名已存在”；手机号既有约束映射仍保持。尚未新增注册 Controller、Service、DTO、页面、路由或真实集成测试，等待人工迁移后再实施。

## 2026-07-11 账号注册迁移准备最终验证

- `mvn -DskipTests compile` 成功，编译 88 个主源码；`spring-security-crypto` 最小依赖解析成功。
- 静态检查确认增量脚本实际 DDL 只有账号两列 `ALTER TABLE` 与 `CREATE UNIQUE INDEX uk_qh_user_username`；未发现注册接口、注册页面或第二套认证/验证码实现。
- 未执行 `account_register_increment.sql`、未连接或修改数据库、未执行 Redis 写操作、未运行 `mvn test` 或真实注册测试。根据迁移停止条件，注册功能状态为 `awaiting_manual_migration`，不是完成状态。

## 2026-07-11 账号注册实施前数据库门禁失败

- 用户报告已手工执行迁移后，实际只读连接 `localhost:3306/qinghe_life` 复核得到：`qh_user` 仍不存在 `username`、`password_hash`；`SHOW INDEX ... uk_qh_user_username` 也无结果。
- `SHOW COLUMNS FROM qh_user` 只显示旧字段 `id`、`phone`、`nickname`、`avatar_url`、`gender`、`status`、时间字段，证明当前连接实例尚未满足注册实现前提。
- 未执行任何写入、SQL、Redis 操作或服务控制；按用户要求立即停止，未创建注册接口、前端、测试或白名单改动。

## 2026-07-11 账号注册实现前数据库门禁通过

- 实际只读连接 `qinghe_life` 确认 `username varchar(32)`、`password_hash varchar(100)` 均允许 NULL，`uk_qh_user_username` 为 `NON_UNIQUE=0` 的唯一索引；`User.username/passwordHash` 映射可继续使用。
- 现有 `UserController` 统一使用 `/api/user` 前缀，故注册将作为该唯一认证 Controller 的 `/api/user/register`，不新建第二个 AuthController；文档会记录该路径与最初设计路径的统一方式。
- `spring-security-crypto` 已存在；`UserDTO`、Redis Token Hash 和前端 Store 不含账号密码字段，满足注册不自动登录、不返回 Token 的边界。

## 2026-07-11 账号注册实现记录

- 注册接口实现为 `POST /api/user/register`，而非新建 `/api/auth` Controller：这是现有用户认证统一前缀，满足单一 Controller 边界。Web 拦截器已将该路径加入未登录白名单，RefreshTokenInterceptor 和其他受保护接口规则未改动。
- 服务流程为：DTO 校验和密码确认 → 同一 `RedisKeys.code(phone)` 验证 → 用户名预检查 → 手机号查询 → BCrypt Hash → 新建或旧用户原地绑定 → 删除同一验证码 Key。失败分支在删除前抛出异常，不创建用户或删除验证码。
- 注册页面只发送 `username/phone/code/password/confirmPassword`，不持久化密码或验证码；成功后清空敏感输入，跳转 `/login?phone=...`，不写 Token、不自动登录。
- 新增集成测试使用 `REGISTER_TEST_` 用户名/昵称前缀、四个固定测试手机号和精确验证码 Key 清理；覆盖公开访问、参数校验、验证码、BCrypt、新用户、旧用户绑定和重复规则，待执行验证。

## 2026-07-11 账号注册最终验证

- 只读数据库门禁通过：`username/password_hash` 均允许 NULL，`uk_qh_user_username` 唯一；`User` 实体映射一致。
- 注册接口实际为 `POST /api/user/register`，这是既有认证 Controller 前缀而非平行 `/api/auth` Controller。它在公开白名单中，现有验证码登录、RefreshTokenInterceptor、LoginInterceptor 和 Bearer Token 逻辑均保持原样。
- `UserRegistrationIntegrationTest` 5 项通过；全量 `mvn test` 18 项通过、0 failure、0 error、0 skipped。测试结束后只读查询 `REGISTER_TEST_` 用户名或昵称残留为 0。
- BCrypt 校验实际通过：新用户与旧用户绑定后的 `password_hash` 以 `$2` 开头且 `BCryptPasswordEncoder.matches` 为真，数据库值不等于明文；成功响应不含 password、passwordHash 或 token，成功后 Redis 验证码 Key 删除。
- 后端打包和前端构建均成功；构建仍保留既有第三方 `@vueuse/core` 注释提示与前端主包超过 500 kB 的非阻断警告。未使用浏览器、未启动或停止任何服务。

## 2026-07-12 AOP 操作日志门禁发现

- 仓库不存在 `UserHolder`；唯一现有用户 ThreadLocal 为 `UserContext`，其 `getUserId()` 直接读取安全 `UserDTO.id`。后续 AOP 必须使用此路径，不得写死用户 ID 或建立第二套 Holder。
- `GlobalExceptionHandler` 已是唯一异常处理器；切面后续必须在记录失败状态和耗时后重新抛出原异常，由它继续生成响应，不能吞掉异常。
- `qh_operate_log` 在基线 SQL、数据库设计、实体与 Mapper 中均不存在，触发用户设定的迁移门禁。已只准备表、实体、Mapper 与文档，尚未创建注解、切面、Controller 注解或测试。
- 新表字段覆盖用户、模块、操作类型、Controller 类/方法、请求路径/HTTP 方法、请求/返回摘要、成功状态、异常摘要、耗时、IP 和操作时间；后续摘要必须截断并排除密码、确认密码、密码哈希、验证码、授权 Token、Redis 密码、二进制文件和完整手机号。

## 2026-07-16 AOP 操作日志实现结论

- `qh_operate_log` 已由用户手工迁移。只读元数据确认全部 15 列、可空规则、主键和两个复合索引与实体匹配，具备实现前提。
- 项目没有 `UserHolder`，唯一用户上下文是 `UserContext`；操作日志切面只读取 `getUserId()`，不会设置、清理或污染该 ThreadLocal。认证拦截器仍负责请求结束清理。
- 切面用 `@Around` 的 `try/catch/finally` 保证成功与异常均记录：业务异常先原样抛出，再在 finally 触发独立 `REQUIRES_NEW` 日志保存；日志保存异常只输出异常类型，不能覆盖业务结果。
- 摘要在序列化前递归处理 Map、集合、数组和 DTO，按字段名掩盖敏感值；最终 JSON 用手机号正则兜底脱敏并限制 2000 字符。Servlet、上传文件、二进制和流对象不被序列化。
- 地址和购物车既有真实集成测试现在会经过切面；清理逻辑先按测试用户 ID 精确删除其日志，避免产生孤立测试操作日志。新增日志测试自身仅删除 `OPERATE_LOG_TEST_` 操作标识记录，不清空整表。
# 2026-07-16 全站功能审计：文档基线

- 审计依据包括当前仓库文档、实际前后端源码、localhost 运行结果、MySQL/Redis 只读检查及仅限 `AUDIT_TEST_` 标记数据的受控写入测试；所有测试写入将在报告前按主键精确清理。
- `docs/database-design.md`、`docs/api-contract.md` 与 `backend/API.md` 目前均把地址建模为 `province/city/district/detail_address`。该模型与校园配送所需的校区、区域、楼栋、房间/配送点等字段存在明显语义差异，待结合实体、DTO、前端和实际表结构判定迁移范围与优先级。
- 文档中接口契约覆盖订单、优惠券、探店、后台；计划状态仍显示 M3 进行中并明确订单、支付、优惠券尚未实现。因此功能状态一律以当前运行实例和源码为准。
- 运行服务均可连接。公开聚合和列表接口运行正常；对不存在的 `/api/*` 路径，认证拦截器先返回 401，导致匿名 API 未能返回资源不存在语义，需进一步核验全局 404 处理优先级。

## 2026-07-16 Codex 卡顿诊断（进行中）

- 初始事实：项目的 5174 前端、8090 后端、3306 MySQL 和 6379 Redis 均在监听，且首页/分类/商铺/商品公开接口在本机返回 200。因此目前没有证据表明“服务未启动”是 Codex 界面未响应的原因。
- 本会话已观察到两类会话开销：超长历史规划/进度/发现文件被一次性读取导致工具输出截断；浏览器自动化在一次不支持的等待状态后没有保留绑定，需重新连接。这两项会增大交互延迟，但尚不能单独证明桌面应用冻结；接下来检查磁盘规模、进程资源与本机页面响应。
