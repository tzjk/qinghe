# 青禾校园生活服务系统 V1.0 开发计划

## 2026-07-17 管理端商品筛选编译修复（完成）

- `GET /api/admin/goods` 查询字段保持 `shopId`、`shopCategoryId`、`goodsCategoryId`、`saleStatus`、`keyword`；不再使用含义不明的查询 `categoryId`。
- 主代码编译与前端构建通过。完整 Maven 测试和 `-DskipTests` 打包在既有 `ShopCoverServiceTest` 的构造器参数不匹配处于 `testCompile` 阶段失败；本轮不扩展修复该范围外测试。


## 目标

在 `qinghe-life-service` 本地项目目录内，按需求分 M0 至 M5 建设前后端分离的校园生活服务演示系统；每个里程碑完成后记录验证结果并停止，等待用户审核后再进入下一里程碑。

## 当前状态

- 当前里程碑：M2A Redis 登录重构为 `awaiting_manual_verification`（代码已编译，真实 Redis 回归因连接超时受阻，仍等待 Redis 可用后的自动测试与用户外部浏览器验证）；M3A 地址与购物车前端保持完成，订单开发继续暂停
- M2 状态：complete（M2A 与 M2B 均已完成）
- M3 状态：in_progress（地址与购物车前后端已完成；订单和优惠券尚未实现）
- 账号注册补充批次：complete（2026-07-17 已在唯一 UserController 内统一为 `POST /api/auth/register`，保留验证码登录与 Token 链路；24 项完整 Maven 回归、后端打包和前端构建均通过）
- 免注册登录与首次资料完善批次：blocked（2026-07-17 只读确认 `qh_user` 缺少 `profile_completed`；已生成未执行的 `backend/src/main/resources/sql/profile_completion_increment.sql`，等待用户在 DataGrip 手工执行和确认）
- 后续批次：未经审核不得启动。
- 本轮范围：保持 M2 不变；完成 M3A 地址与购物车前端及商铺详情加购，不实现订单、支付或优惠券。
- Git 规则：默认不要求 Git 仓库；仅在用户明确书面授权时执行 Git 命令，并使用 `progress.md`、`findings.md` 和文件清单记录变更。

## 里程碑

### M0：需求检查与设计基线

- **目标：** 固化可实施的数据库、接口、页面和命名基线，发现需求冲突。
- **功能范围：** 需求核对；表结构设计；接口契约；路由与页面设计；状态值与命名冲突审查。
- **涉及目录和文件：** `PROJECT_PLAN.md`、`progress.md`、`findings.md`、`docs/database-design.md`、`docs/api-contract.md`、`docs/page-design.md`。
- **实现任务：** 读取规则和需求；检查目录与现存文件；登记 15 张业务表及 Java 映射；定义用户端接口和后台接口命名空间；定义 8 类页面和路由；核对表名、实体、DTO、VO、路径、状态和页面功能。
- **验证命令：** `Test-Path PROJECT_PLAN.md,progress.md,findings.md,docs/database-design.md,docs/api-contract.md,docs/page-design.md`；`Get-ChildItem backend,frontend,docs,deploy -Force -Recurse`；设计文档交叉核对。
- **验收条件：** 六个设计/记录文件存在；六阶段计划完整；设计文档记录所有必需表、指定接口和八类页面；冲突与缺失均有结论。
- **已知风险：** 实体、DTO、VO 和路由尚未创建，命名核查只能覆盖设计基线，M1 后必须复核实际实现；连接参数含本地默认密码，后续仅允许出现在后端 `application.yml` 的环境变量占位表达式中。
- **停止点：** M0 文档完成并通过用户审核前，不创建项目骨架、SQL、实体或前端文件。
- **状态：** complete

#### M0 需求闭环补充（已完成）

- 已固定后端 8090、前端 5174、MySQL 与 Redis 的连接参数及密码占位表达式；密码传播限制已登记。
- 已关闭地址接口、订单评价、后台接口明细和首页轮播四项需求缺口。
- `qh_shop` 已登记 `is_featured`、`cover_image`、`sort_order`；`qh_comment` 已登记统一评论字段及订单/商铺/探店三种评论规则。
- 已移除 Git 仓库核验作为验收条件；本地项目改以文件清单、`progress.md` 和 `findings.md` 记录变更。

### M1：项目骨架、SQL 与公共组件

- **目标：** 建立后端和前端可构建骨架，并提供与设计一致的初始化 SQL、实体和公共能力。
- **功能范围：** Maven/Spring Boot 与 Vue/Vite 初始化；MyBatis-Plus 配置；SQL 文件；全部实体；统一响应、异常处理、CORS、分页、请求鉴权基础；前端 Axios、Pinia、Router、公共布局与状态组件。
- **涉及目录和文件：** `backend/pom.xml`、`backend/src/main/**`、`backend/API.md`、`backend/src/main/resources/sql/qinghe_life.sql`、`frontend/**`、`deploy/**`（仅骨架所需说明）。
- **实现任务：** 按 M0 文档创建目录和依赖清单；在 `application.yml` 使用确认的 MySQL/Redis 环境变量占位表达式；编写不含 `DROP DATABASE` 的 SQL；实现实体字段映射；建立公共 DTO/VO、请求拦截和响应约定；创建前端路由守卫与请求拦截器。
- **验证命令：** `mvn -f backend/pom.xml test`；`npm --prefix frontend run build`；对照 `docs/database-design.md`、SQL 与实体字段；`rg "com\\.sky|com\\.hmdp|黑马|苍穹" backend frontend`。
- **验收条件：** 前后端骨架可构建；SQL、实体和公共约定一致；未执行 SQL、未连接 MySQL 或 Redis；无禁止教学项目标识。
- **已知风险：** 依赖版本兼容性、Java 8 与前端运行环境、SQL 与实体同步维护。
- **停止点：** 仅完成可构建骨架和公共能力；不实现用户业务接口或具体业务页面。
- **状态：** complete

### M2：登录、首页、商铺与商品

- **目标：** 实现用户登录链路及浏览商铺、商品的核心体验。
- **功能范围：** 验证码、登录、Token、个人基础资料；首页摘要；商铺和商品分页、详情、评论读取；Redis 商铺缓存；对应用户端页面。
- **涉及目录和文件：** `backend/src/main/java/com/qinghe/life/**`、`frontend/src/**`、`backend/API.md`、测试文件与 M0 文档的必要更新。
- **实现任务：** 实现 `/api/user/**`、`/api/home/summary`、`/api/shops/**`、`/api/goods/**`；实现登录状态保存、401 清理与跳转；完成首页、商铺页和相关详情页面。
- **验证命令：** `mvn -f backend/pom.xml test`；`npm --prefix frontend run build`；接口测试覆盖登录、分页、详情、401；人工检查 Token 请求头与页面加载/空态/错误态。
- **验收条件：** 固定验证码规则可演示；Token 前后端传递一致；首页、商铺、商品可展示真实演示数据；接口与契约一致。
- **已知风险：** Redis 不可用时的缓存降级、Token 失效、分页参数边界、商铺与商品状态过滤。
- **停止点：** 不实现购物车、订单、优惠券、探店或后台业务。
- **状态：** complete

#### M2A：用户登录与认证（已完成）

- **范围：** 仅实现用户验证码、登录、Token、当前用户、资料更新、退出、Redis 会话、前端登录、Pinia、Axios 和用户路由守卫。
- **边界：** 不实现首页、商铺、商品、购物车、订单、优惠券、探店或后台业务；不访问 hmdp 数据库。
- **验证：** 对项目数据源执行只读 `SELECT 1`，Redis PING 与 database 2 配置检查；随后执行登录链路集成测试、Maven 测试/打包和前端 `npm.cmd run build`。
- **停止点：** M2A 完成后更新记录并停止，等待用户审核后才可进行 M2B。

#### M2B：分类、首页、商铺与商品（已完成）

- **范围：** 仅实现公开分类查询 `GET /api/categories`；不进入首页、商铺、商品、缓存或前端 API/页面。
- **实现：** 新增 `CategoryController`、`CategoryService`、`CategoryServiceImpl`、`CategoryVO`；查询仅返回 `status=1` 的分类并按 `sort_order` 升序；将该公开接口加入登录拦截器白名单。
- **验证：** 已检查四个新增文件均实际存在且非空；`mvn -DskipTests compile` 通过。
- **首页批次：** 已新增 `HomeController`、`HomeService`、`HomeServiceImpl`、`HomeSummaryVO` 及复用的商铺、商品、优惠券、探店 VO；`GET /api/home/summary` 固定返回六个数据区域，`mvn -DskipTests compile` 通过。
- **商铺批次：** 已新增商铺分页、详情、商铺商品和商铺评论读取接口，支持分类、关键词、评分/最新排序及分页；仅返回启用商铺、上架商品和启用评论；`mvn -DskipTests compile` 通过。
- **商品与缓存：** 已实现商品分页和详情，只返回 `ON_SALE`；商铺详情缓存实现 Cache Aside、空值缓存、随机 TTL、三次互斥锁重试和 Redis 异常降级查询 MySQL。
- **前端：** 四个 API 文件复用 `http.js`；首页、商铺列表和商铺详情已接入真实接口，包含加载、空数据与错误状态。
- **验证：** `mvn test` 通过（2 个测试，0 失败、0 错误）；`mvn clean package -DskipTests` 成功并编译 72 个源文件；`D:/develop/NodeJS/npm.cmd run build` 成功。
- **停止点：** M2B 已完成，立即停止并等待用户审核，不进入 M3A。

### M3：购物车、订单与优惠券

- **目标：** 打通从购物车到模拟支付、完成订单和优惠券使用的交易演示链路。
- **功能范围：** 购物车 CRUD 与金额计算；订单创建、查询、取消、模拟支付、完成、评价；优惠券领取、库存、重复领取、使用门槛和 Redis 库存同步；对应页面。
- **涉及目录和文件：** `backend/src/main/java/com/qinghe/life/**`、`frontend/src/**`、`backend/API.md`、测试文件与 M0 文档的必要更新。
- **实现任务：** 实现 `/api/cart`、`/api/orders/**`、`/api/coupons/**`；实现订单状态流转校验；将优惠金额计算限定在模拟订单内；实现购物车、订单、优惠券页面及确认弹窗。
- **验证命令：** `mvn -f backend/pom.xml test`；`npm --prefix frontend run build`；接口测试覆盖数量、库存、重复领取、状态转移和金额计算；人工检查状态标签、空态和错误提示。
- **验收条件：** 交易链路可演示且不接入真实支付；非法状态转移和重复领取被拒绝；订单与优惠券状态值符合 M0 定义。
- **已知风险：** 金额精度、库存同步一致性、优惠券使用并发；本项目不引入消息队列和分布式事务。
- **停止点：** 不实现探店互动、个人中心扩展或后台管理。
- **状态：** in_progress

#### M3 维护批次：AOP 操作日志（已完成）

- **范围：** 为重要新增、修改、删除接口增加显式操作日志注解；使用现有认证上下文记录当前用户、请求/返回摘要、结果、异常、耗时、IP 与操作时间。
- **数据库门禁：** 已只读复核用户手工迁移后的 `qh_operate_log`，字段、主键和两个复合索引均与实体一致；本项目未执行 SQL 或修改表结构。
- **边界：** 不全局拦截写请求；不记录密码、验证码、授权 Token、Redis 密码、文件二进制内容或完整手机号；日志入库失败不得影响业务；不实现订单或修改认证、Redis Token 和前端。

#### M3A 第一批：地址管理后端（已完成）

- **范围：** 地址查询、新增、修改、删除和设置默认地址；不实现购物车、订单、支付、取消、完成或优惠券。
- **结构审计：** 发现地址、购物车、订单和订单明细目标字段缺口；已更新设计与基线 SQL，并生成 `backend/src/main/resources/sql/m3a_increment.sql`，未自动执行。
- **实现：** 新增 `AddressController`、`AddressService`、`AddressServiceImpl`、`AddressCreateDTO`、`AddressUpdateDTO`、`AddressVO`；全部用户编号来自 `UserContext`，并实现首地址默认、默认切换事务、默认地址删除后补选和地址归属校验。
- **验证：** 7 个新增文件均实际存在、非空、可读取且位于项目根目录；`mvn -DskipTests compile` 成功，编译 78 个源文件。
- **阻塞：** 地址表已确认使用 `receiver_name/receiver_phone`，Java 已纠偏并编译；但只读查询确认 `qh_cart`、`qh_order`、`qh_order_item` 仍是旧结构，因此未运行地址真实测试，也未开始购物车。
- **停止点：** 本批完成后停止等待审核；M3A 总体保持 `in_progress`。

#### M3A 续作门禁（阻塞）

- **地址映射：** Java、DTO、VO、Service 与接口文档统一为 `receiverName/receiverPhone`，通过下划线转驼峰映射实际列。
- **只读核验：** 当前数据库明确为 `qinghe_life`；地址表结构与索引正确；购物车联合唯一索引和订单号唯一索引存在。
- **结构差异：** 购物车缺少 `shop_id/selected`；订单仍为 `address_snapshot/order_status`；订单明细仍为 `price_snapshot/subtotal_amount` 且缺少 `goods_image`。
- **验证：** 地址纠偏后 `mvn -DskipTests compile` 成功，编译 78 个源文件。
- **停止点：** 按门禁立即停止，不运行地址真实测试，不开始购物车、订单或 M3B。

#### M3A 结构门禁修订（已完成，待人工执行）

- **增量脚本：** `m3a_increment.sql` 已将 `qh_cart.shop_id`、`qh_order.address_id` 设为 `NOT NULL`；移除未映射且无默认值的 `qh_cart.price_snapshot`；新增 `(user_id, selected)` 与 `(user_id, shop_id)` 索引。
- **订单映射：** `Order`、基线 SQL、设计文档与增量 SQL 已统一使用 `receiver_name/receiver_phone` 和 Java `receiverName/receiverPhone`；不再使用 contact 命名。
- **前置条件：** 脚本仅适用于三张待迁移表仍为 0 条记录且保留 M2B 旧字段的本地库；脚本非幂等，仅可在 DataGrip 人工审核后执行一次。
- **验证：** 静态门禁通过；`mvn -DskipTests compile` 成功，编译 78 个源文件。
- **停止点：** 未执行 SQL，未连接数据库，未运行地址真实测试，未开始购物车、订单或 M3B；需人工执行后重新执行只读结构审计。

#### M3A 人工迁移后复核与地址真实测试（已完成）

- **人工执行确认：** 用户已在 DataGrip 人工审核并执行一次 `m3a_increment.sql`；本轮未再次执行或修改数据库结构。
- **实际结构：** 只读查询确认 `qh_user_address`、`qh_cart`、`qh_order`、`qh_order_item` 与实体、基线 SQL、设计文档及增量脚本预期一致；三张迁移表无旧字段，购物车与订单约束/索引均已落库。
- **地址测试：** 新增 `AddressIntegrationTest`，真实覆盖未登录、创建和默认地址规则、receiver 列映射、隔离与越权、参数校验、默认删除补选及 ThreadLocal 清理；测试后剩余标识用户和地址均为 0。
- **验证：** `mvn test` 通过，3 项测试、0 失败、0 错误；`mvn clean package -DskipTests` 成功并生成 JAR。
- **停止点：** M3 保持 `in_progress`；本轮未实现购物车、订单、优惠券或 M3B。下一步仅可在用户审核后进入购物车后端批次。

#### M3A 购物车后端批次（已完成）

- **接口：** 已实现 `GET/POST /api/cart`、`PUT /api/cart/{id}`、`PUT /api/cart/{id}/selected`、`DELETE /api/cart/{id}` 和 `DELETE /api/cart`；全部使用用户登录态，不接收前端用户编号。
- **规则：** 添加和修改校验商品在售、商铺启用、数量 1 至 99 与当前库存；所有按编号操作均同时匹配 `id` 与 `user_id`，清空只匹配当前用户。
- **并发与查询：** 基于现有 `(user_id, goods_id)` 唯一索引，采用参数化条件原子累加及重复键冲突重试，防止重复行和超库存；查询批量读取商品、商铺后以 Map 分组，未修改 M2B 商铺缓存。
- **验证：** `CartIntegrationTest` 通过；`mvn test` 通过 4 项测试、0 失败、0 错误；`mvn clean package -DskipTests` 成功，编译 87 个主源码和 3 个测试源码并生成 JAR；测试标识用户、商铺、商品与购物车残留均为 0。
- **停止点：** M3 保持 `in_progress`；本轮未实现购物车前端、订单、支付、优惠券或 M3B。下一步仅可在用户审核后进入地址与购物车前端接入批次，订单仍未开始。

#### M3A 地址与购物车前端批次（已完成）

- **地址前端：** 新增地址 API 与受保护页面 `/profile/addresses`；“我的”页提供入口，支持真实列表、新增、编辑、删除、默认地址、校验与状态反馈。展示号码脱敏，提交保持 receiver 原始字段。
- **购物车前端：** 新增购物车 API 与真实页面；支持按商铺分组、选中、数量、删除、清空、金额汇总、空/加载/错误状态。每次操作都重新读取服务端状态；订单未实现时不调用订单接口。
- **商铺详情：** 商品列表接入真实 `addCart`，提供库存范围数量、下架/缺货禁用、提交 loading 和登录 redirect 返回。
- **验证：** 前端生产构建通过；后端 `mvn test` 4 项测试和 Maven 打包均在本轮代码落盘后通过。浏览器自动联调因浏览器会话异常未完成，需用户手动启动前后端并在外部浏览器验证；这不替代业务代码验收。
- **最终命令行验收：** 已重新静态核验唯一的地址/购物车 API 和页面、`http.js` 复用、地址路由、购物车导航、地址入口、详情加购与无订单调用；`D:/develop/NodeJS/npm.cmd run build` 通过（1681 个模块、生成 `dist`）；`mvn test` 通过（4 项、0 失败、0 错误）；`mvn clean package -DskipTests` 通过（87 个主源码、3 个测试源码、JAR 已生成）。8090 未监听，未执行 curl；5174 正在监听。未启动或停止进程，未使用浏览器。
- **停止点：** M3 继续保持 `in_progress`；下一批为订单创建与查询后端。未实现订单、支付、优惠券或 M3B。

#### M2A：验证码获取与登录链路修复（awaiting_manual_verification）

- **范围：** 仅修复登录页获取验证码交互、验证码 Redis 存取、验证码登录校验、开发环境测试方式和认证回归测试；不修改地址、购物车、首页、商铺、商品、订单或数据库结构。
- **根因：** 前端发送按钮缺少请求中状态，失败分支为空、倒计时不在组件卸载时清理，并在用户界面硬编码开发验证码；后端则在所有环境固定 `123456`，且登录额外要求该常量，无法安全支持非开发环境生成的验证码。
- **修复：** 登录页复用既有 `http.js` 和 `POST /api/user/code` JSON 请求，增加 loading、禁用、60 秒倒计时、错误提示和卸载清理；默认 `dev` profile 的 `application-dev.yml` 提供本地验证码，服务仅在 `dev/local` 日志输出，其他环境生成随机六码且只比较 Redis 实际值。Redis 写入异常返回明确业务错误，成功登录删除验证码。
- **404 根因与修复：** 真实浏览器请求是 `/api/user/code`，后端映射同为 `POST /api/user/code`；原 `vite.config.js` 无 `server.proxy`，请求停在 5174 返回 404，故 IDEA 8090 无日志。现已配置 `/api -> http://localhost:8090` 且不 rewrite，并统一 HTTP 层的一次错误提示。
- **验证：** `mvn test` 通过 5 项（0 失败、0 错误）；认证测试实际验证 Key `qh:login:code:{phone}`、STRING 类型、正 TTL、匿名发送、错误/不存在/过期拒绝、成功一次性使用和 Token 会话。`mvn clean package -DskipTests` 与前端生产构建均通过。
- **停止点：** 这不是浏览器验收。用户须重启 Vite 和 IDEA Spring Boot 后在外部浏览器人工验证；在其完成前验证码登录不得标记 completed。M3 保持 `in_progress`，订单开发继续暂停。

#### M2A：Redis 登录双拦截器重构（awaiting_manual_verification）

- **实现：** 验证码改为随机六码、覆盖写入和 2 分钟 TTL；Token 为 32 位 UUID 随机值，`UserDTO` 字符串 Hash 写入 Redis 30 分钟。新增刷新拦截器（order 0）和纯鉴权拦截器（order 1），复用 `UserContext` ThreadLocal。
- **验证状态：** `mvn clean package -DskipTests` 与前端构建通过；`mvn test` 被配置 Redis `192.168.100.128:6379` 连接超时阻塞，未以 mock 或跳过测试伪造通过。仍须等待 Redis 可用后重跑，并由用户完成外部浏览器验证。

### M4：探店互动与个人中心

- **目标：** 完成内容发布、互动和用户个人资料/地址管理。
- **功能范围：** 探店列表和详情、发布、点赞、收藏、评论、我的发布和收藏；个人资料、地址管理、退出登录；对应页面。
- **涉及目录和文件：** `backend/src/main/java/com/qinghe/life/**`、`frontend/src/**`、`backend/API.md`、测试文件与 M0 文档的必要更新。
- **实现任务：** 实现 `/api/blogs/**`、用户资料与地址接口；利用唯一约束保障单用户单内容点赞/收藏；实现探店、我的和地址维护页面。
- **验证命令：** `mvn -f backend/pom.xml test`；`npm --prefix frontend run build`；接口测试覆盖发布权限、重复点赞/收藏、评论可见性、地址归属与退出；人工检查页面加载、空态、错误态和确认弹窗。
- **验收条件：** 用户仅能管理自己的资料、地址和内容；互动数据不重复；探店与个人中心满足页面设计。
- **已知风险：** 内容审核状态、关联内容删除策略、点赞计数一致性。
- **停止点：** 不开始后台管理、总体验收或部署交付文件。
- **状态：** pending

### M5：后台管理、联调与交付

- **目标：** 完成后台业务、全链路联调、测试、部署说明、接口文档和软著文案。
- **功能范围：** 管理员登录、看板、分类/商铺/商品/订单/优惠券/用户/内容/评论管理；接口文档、测试报告、启动脚本、README、软著文案。
- **涉及目录和文件：** `backend/**`、`frontend/**`、`docs/test-report.md`、`backend/API.md`、`README.md`、`deploy/README.md`、`deploy/start-backend.bat`、`deploy/start-frontend.bat`、`frontend/src/assets/copyright-text.md`。
- **实现任务：** 实现 `/api/admin/**` 及后台路由布局；完成权限隔离与联调；编写测试报告和部署说明；生成符合禁词限制的软著文案；逐项验证项目原创标识和文档完整性。
- **验证命令：** `mvn -f backend/pom.xml test`；`npm --prefix frontend run build`；`rg "com\\.sky|com\\.hmdp|黑马|苍穹" backend frontend docs README.md`；接口回归测试；启动脚本静态检查。
- **验收条件：** 管理端与用户端路由、接口和权限互不冲突；前后端构建通过；交付文件齐全；测试结果真实记录；没有真实支付、短信、云存储、消息队列或危险数据库操作。
- **已知风险：** 全链路联调遗漏、文案禁词、软著截图准备、部署环境差异。
- **停止点：** 提交 M5 验证结果和文件清单，等待用户最终审核。
- **状态：** pending

## 已记录错误

| 问题 | 次数 | 处理结果 |
|---|---:|---|
| 首次 PowerShell 检查中 `$s:` 插值语法无效 | 1 | 改用 `${s}` 后重试成功。 |
| 最终核验脚本中 `$_` 字符串插值解析失败 | 1 | 改用具名循环变量和格式化字符串后重试成功。 |
| 最后状态展示脚本对空值调用 `Trim()` 失败 | 1 | 不重试；文件存在性和设计覆盖核验已成功完成，错误不影响 M0 结论。 |

## 2026-07-12 至 2026-07-16 M3 维护批次：AOP 操作日志

- **状态：** complete。
- **门禁结论：** 用户完成 DataGrip 手工迁移后，实际 `qh_operate_log` 的字段、NULL 规则、主键与复合索引均通过只读核验；未执行 SQL。
- **实现与验证：** 已新增注解、`@Around` 切面、`REQUIRES_NEW` 日志服务和地址/购物车八个关键写接口标注。全量 `mvn test` 22 项通过，`mvn clean package -DskipTests` 成功；测试日志及地址/购物车测试标识残留均为 0。

## 2026-07-17 M5 子里程碑：店铺后台维护与封面 OSS

- **状态：** complete（已完成并验证）。
- **范围：** 仅完成店铺真实资料、单张封面、管理员店铺页面和用户端封面同步；不实施商品、订单、优惠券、支付或配送。
- **结构与实现：** 实际 `qh_shop.cover_image` 支持单图，未生成/执行迁移。管理员接口、页面、缓存精确失效、Mock OSS 补偿与受限旧图删除均复用现有店铺、管理员、AOP、Redis 和 HTTP 栈。
- **验证：** `Q:\backend` 完整 Maven 回归 40/0/0/0；`mvn clean package -DskipTests` 成功；真实前端路径生产构建成功（1707 modules）。真实 OSS 和浏览器验收保留人工步骤，未虚报为已执行。
