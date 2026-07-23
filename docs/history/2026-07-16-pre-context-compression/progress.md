# M0 进度记录

## 会话：2026-07-10

### M0：需求检查与设计基线

- **状态：** complete
- **开始与完成：** 2026-07-10（本轮）
- **已完成事项：**
  - 读取 `AGENTS.md` 与 `docs/PROJECT_SPEC.md`。
  - 检查项目根目录、目录结构、`backend`、`frontend`、`docs`、`deploy` 的旧文件。
  - 验证 `planning-with-files`、`context-compression`、`context-degradation` 的本地 Skill 文件可识别。
  - 编制 M0 至 M5 的停点式开发计划。
  - 完成数据库、接口、页面设计与命名/状态冲突审查。
  - 检查六个限定输出文件均已创建。
- **未执行事项：** 未进入 M1；未写入业务代码；未安装依赖；未连接 MySQL、Redis；未执行 SQL。
- **新增文件：**
  - `PROJECT_PLAN.md`
  - `progress.md`
  - `findings.md`
  - `docs/database-design.md`
  - `docs/api-contract.md`
  - `docs/page-design.md`
- **修改文件：** 无；`docs/PROJECT_SPEC.md` 仅读取。

### M0：需求闭环修订

- **状态：** complete
- **完成事项：**
  - 确认后端 8090、前端 5174、MySQL 与 Redis 配置基线，并限定密码只通过后端环境变量占位表达式出现。
  - 将地址接口统一为 `/api/addresses`，增加默认地址动作。
  - 将订单评价统一为 `qh_comment` 与 `POST /api/orders/{id}/review`，并登记商铺/探店评论边界。
  - 逐项补齐后台登录、看板和各管理模块接口。
  - 确认首页轮播不增加表，使用启用且推荐的商铺生成 `banners`。
  - 明确本项目为本地项目，不要求 Git 仓库；改以文件清单、`progress.md` 和 `findings.md` 记录变更。
- **修改文件：**
  - `AGENTS.md`
  - `docs/PROJECT_SPEC.md`
  - `PROJECT_PLAN.md`
  - `progress.md`
  - `findings.md`
  - `docs/database-design.md`
  - `docs/api-contract.md`
  - `docs/page-design.md`
- **未执行事项：** 未进入 M1；未创建 Spring Boot 或 Vue 业务代码；未安装依赖；未连接 MySQL、Redis；未执行 SQL。

## 会话：2026-07-10（M1）

### M1：项目骨架、SQL 与公共组件

- **状态：** complete
- **完成事项：**
  - 建立 Java 8 目标、Spring Boot 2.7.18、Maven、MyBatis-Plus、MySQL Driver、Redis、Lombok、Jackson 和 Hibernate Validator 后端工程。
  - 创建公共返回、分页、异常处理、参数校验、MyBatis-Plus 分页、CORS、Redis 序列化、用户上下文和登录拦截器骨架。
  - 创建 15 张表的 SQL、15 个实体和 15 个 Mapper；SQL 不会自动执行。
  - 建立 Vue 3、Vite、Element Plus、Pinia、Vue Router、Axios 前端工程与 5174 端口配置。
  - 建立八类页面、登录页、商铺详情页、用户/管理端布局、Token 工具、Pinia 用户状态与 Axios 拦截器骨架。
  - 创建 `.gitignore`、README、后端 API 初稿、前端环境变量示例和部署说明初稿。
- **未执行事项：** 未启动后端；未连接 MySQL 或 Redis；未执行或导入 SQL；未实现 M2 至 M5 的业务 Controller、Service 或业务接口。
- **新增文件：**
  - 根目录：`.gitignore`、`README.md`。
  - 后端：`backend/pom.xml`、`backend/API.md`、`backend/src/main/resources/application.yml`、`backend/src/main/resources/sql/qinghe_life.sql`，以及 `com.qinghe.life` 下的应用入口、2 个 common、3 个 config、1 个 dto、16 个 entity、2 个 exception、1 个 interceptor、15 个 mapper、1 个 utils 与 4 个包说明文件。
  - 前端：`frontend/package.json`、`frontend/package-lock.json`、`frontend/vite.config.js`、`frontend/.env.example`、`frontend/index.html`，以及 `src` 下的应用入口、Axios、Token、Pinia、2 个布局、1 个通用页面组件、11 个页面和路由文件。
  - 部署：`deploy/README.md`。
- **修改文件：** `PROJECT_PLAN.md`、`progress.md`、`findings.md`。

## 验证记录

| 验证项 | 命令/方法 | 结果 |
|---|---|---|
| 需求与规则读取 | `Get-Content AGENTS.md`、`Get-Content docs/PROJECT_SPEC.md` | 通过。 |
| 目标目录旧文件检查 | `Get-ChildItem backend,frontend,docs,deploy -Force -Recurse` | 通过：仅 `docs/PROJECT_SPEC.md` 已存在且未覆盖。 |
| Skill 可识别性 | 检查 `.agents/skills/<skill>/SKILL.md` | 通过：三项均存在。 |
| 本地项目记录规则 | 阅读 AGENTS.md 与本轮确认方案 | 通过：不要求 Git 仓库，不执行 Git 命令，使用文件清单和规划记录追踪变更。 |
| 允许输出文件创建 | `Test-Path` 检查六个限定路径 | 通过。 |
| 命名冲突审查 | 比对三个 M0 设计文档的命名登记 | 设计层通过；待 M1 落地后复核实际类与路由。 |
| 需求覆盖核验 | 搜索 15 张表、指定 API 前缀和八类页面名称 | 通过：均已登记到对应设计文档。 |
| 范围核验 | 再次枚举 `backend`、`frontend`、`docs`、`deploy` | 通过：`backend`、`frontend`、`deploy` 仍为空；`docs` 仅有需求文件和本轮三个设计文件。 |
| 需求闭环一致性 | 交叉核对数据库、接口与页面设计 | 通过：表/字段、地址接口、订单评价、后台路径、首页 `banners` 数据源和页面映射一致。 |
| 最终设计核验 | 逐项检查三份设计文档的字段、路径、首页响应字段、后台模块与页面映射 | 通过：全部确认项均已存在；未执行 Git、依赖、数据库、Redis 或 SQL 操作。 |
| Maven 构建 | `mvn clean package -DskipTests` | 通过：BUILD SUCCESS，生成 `backend/target/qinghe-life-backend-1.0.0.jar`。 |
| Java 8 目标 | `javap -verbose` 检查应用入口 | 通过：`major version: 52`，即 Java 8 字节码；构建运行时为 Java 21。 |
| 前端依赖 | `D:\develop\NodeJS\npm.cmd install` | 通过：安装 83 个包，生成 `package-lock.json` 与 `node_modules`。 |
| 前端构建 | `D:\develop\NodeJS\npm.cmd run build` | 通过：Vite 构建成功，生成 `frontend/dist/index.html`。 |
| SQL/实体一致性 | 逐表比对 15 个 SQL 表字段与实体字段（含 `id`、`create_time`、`update_time`） | 通过：字段一致。 |
| 路由与请求头 | 静态检查规定路由、`Authorization: Bearer <token>` | 通过：八类页面、登录、后台登录、商铺详情和 Axios 请求/响应拦截器均存在。 |
| 安全与原创标识 | 搜索密码字面量、`DROP DATABASE` 与禁止教学项目标识 | 通过：README/API/前端无密码字面量；SQL 无 `DROP DATABASE`；后端、前端、README、deploy 无禁止标识。 |
| M1 边界 | 统计具体 Controller 与 Service | 通过：均为 0；未提前实现 M2 至 M5 业务。 |

## 错误记录

| 时间 | 问题 | 次数 | 处理结果 |
|---|---|---:|---|
| 2026-07-10 | PowerShell 中 `$s:` 插值导致解析错误 | 1 | 改为 `${s}`，后续命令成功。 |
| 2026-07-10 | 最终核验脚本中 `$_` 字符串插值导致解析错误 | 1 | 改用具名循环变量和格式化字符串，重试成功。 |
| 2026-07-10 | 最后状态展示脚本对空值调用 `Trim()` 失败 | 1 | 不重试；此前的文件存在性、表/API/页面覆盖核验均已成功。 |
| 2026-07-10 | 前端首个补丁的 Windows 路径缺少分隔符 | 1 | 补丁未产生部分写入；修正路径后重新创建前端骨架。 |
| 2026-07-10 | PATH 优先命中 `C:\Windows\System32\npm`，命令未安装依赖或产出 dist | 1 | 改用 `D:\develop\NodeJS\npm.cmd`，安装和构建均成功。 |
| 2026-07-10 | 首次 SQL/实体核验的驼峰转换规则错误，产生假阳性 | 1 | 使用 `${1}_${2}` 替换规则重跑，15 张表字段一致。 |

## 复位检查

| 问题 | 当前答案 |
|---|---|
| 我在哪里？ | M1 已完成，等待用户审核。 |
| 下一步去哪里？ | 经用户确认后才可进入 M2。 |
| 目标是什么？ | 建设青禾校园生活服务系统，并在每个里程碑后停点审核。 |
| 已发现什么？ | 见 `findings.md`；设计与 M1 骨架核验已完成，业务实现留待 M2。 |
| 已做什么？ | 见本文件的 M0/M1 完成事项、验证记录和文件清单。 |

## 会话：2026-07-10（M2A）

### M2A：用户登录与认证

- **状态：** complete
- **范围：** 用户验证码、登录、Redis Token 会话、当前用户、资料更新、退出、前端登录、Pinia、Axios 与用户路由守卫。
- **边界：** 不实现 M2B 及后续业务；不访问 hmdp 数据库；仅通过项目数据源验证 qinghe_life。
- **已完成：** 重新读取 M0/M1 基线、`application.yml`、SQL、设计文档、记录文件和当前工程结构。
- **实现：** 完成验证码、用户登录、Token Hash、Bearer 登录拦截器、当前用户、资料更新、退出、前端登录页、Pinia 会话恢复、Axios 401 去重跳转和 redirect 路由守卫。
- **验证：** `mvn test` 通过（1 个集成测试，0 失败）；MySQL `SELECT 1`、Redis PING、验证码、错误验证码、新用户创建、Token Hash 字符串化、401、资料同步、退出失效和 ThreadLocal 清理均通过。`mvn clean package -DskipTests` 与 `D:\develop\NodeJS\npm.cmd run build` 均通过。
- **停止点：** M2A 已完成；未实现首页、商铺、商品或其他后续业务，等待用户审核。

## 会话：2026-07-10（M2B 第一批）

### M2B：分类模块

- **状态：** complete（M2B 总里程碑仍为 in_progress）
- **范围：** 仅实现 `GET /api/categories`，不实现首页、商铺、商品、缓存、前端 API 或前端页面。
- **完成事项：** 新增分类 Controller、Service、ServiceImpl 和 VO；仅查询 `status=1`，按 `sort_order` 升序；将分类查询列入公开 GET 接口白名单。
- **新增文件：** `backend/src/main/java/com/qinghe/life/controller/CategoryController.java`、`backend/src/main/java/com/qinghe/life/service/CategoryService.java`、`backend/src/main/java/com/qinghe/life/service/impl/CategoryServiceImpl.java`、`backend/src/main/java/com/qinghe/life/vo/CategoryVO.java`。
- **修改文件：** `backend/src/main/java/com/qinghe/life/interceptor/LoginInterceptor.java`、`backend/API.md`、`PROJECT_PLAN.md`、`progress.md`、`findings.md`、`task_plan.md`。
- **验证：** 四个新增 Java 文件均已实际检查为存在且非空；在 `backend` 执行 `mvn -DskipTests compile`，BUILD SUCCESS（编译 55 个源文件）。
- **批次衔接：** 分类首批已完成；按恢复指令继续同一 M2B 里的首页模块。M2B 整体完成并通过全部验证后停止，不进入 M3A。

### M2B：首页模块

- **状态：** complete（M2B 总里程碑仍为 in_progress）
- **完成事项：** 新增 `GET /api/home/summary`；固定聚合 `banners`、`categories`、`recommendedShops`、`hotGoods`、`availableCoupons` 和 `featuredBlogs`。轮播和推荐商铺仅取启用且推荐的商铺；热门商品只取上架商品；优惠券仅取在有效期内且有余量的已发布券；探店仅取已发布内容。
- **新增文件：** `HomeController.java`、`HomeService.java`、`HomeServiceImpl.java`、`HomeSummaryVO.java`、`ShopVO.java`、`GoodsVO.java`、`CouponVO.java`、`BlogVO.java`（均位于对应 `com/qinghe/life` 包）。
- **验证：** 新增首页核心文件已检查为存在且非空；首次检查因工作目录与路径前缀不一致失败，未执行 Maven；改为 `backend` 下的 `src/` 相对路径后，`mvn -DskipTests compile` 通过，编译 63 个源文件。

### M2B：商铺模块

- **状态：** complete（M2B 总里程碑仍为 in_progress）
- **完成事项：** 实现商铺分页、分类筛选、关键词查询、默认/评分/最新排序、详情、商铺上架商品和商铺启用评论查询；访问不存在或停用的商铺返回 404 业务结果。
- **新增文件：** `ShopController.java`、`ShopService.java`、`ShopServiceImpl.java`、`ShopQuery.java`、`CommentVO.java`（均位于对应 `com/qinghe/life` 包）。
- **验证：** 新增核心文件均检查为存在且非空；在 `backend` 执行 `mvn -DskipTests compile` 通过，编译 68 个源文件。

## 会话：2026-07-10（M2B 续作恢复）

- **状态：** in_progress
- **恢复结论：** 项目根目录和八个必需入口均存在；已读取仓库规则、M2 需求、计划、进度、发现记录与后端 API 文档。
- **执行原则：** 以当前文件系统为准核验分类、首页、商铺、商品、缓存、前端和测试；已存在且正确的模块不重复创建；M2B 全部验证后停止，不进入 M3A。
- **恢复错误：** 首次批量文件审计命令在 PowerShell 解析阶段因 `foreach` 后直接接管道失败，未读取或修改源码；已改用结果变量方案。
- **文件审计：** 分类、首页、商铺、商品核心 Java 文件，四个前端 API 文件，以及首页、商铺列表、商铺详情页面均存在、非空、可读取且位于项目根目录内；缓存源码包含规定的三类 `qh:` Key、有限锁重试、空值缓存和随机过期时间。
- **恢复编译：** 在 `backend` 目录执行 `mvn -DskipTests compile` 成功；Maven 报告 72 个源文件的现有 class 均为最新，无需重编译。此前首页批次曾实际重编译 63 个源文件，后续模块批次实际重编译至 72 个源文件。
- **最新测试：** `mvn test` 通过，2 个集成测试、0 失败、0 错误。M2A 登录认证回归继续通过；M2B 覆盖分类、首页六区、商铺、商品、首次详情缓存、二次缓存读取和不存在商铺空值缓存。

### M2B：最终验收

- **状态：** complete
- **后端打包：** `mvn clean package -DskipTests` 成功，干净重编译 72 个源文件并生成 `backend/target/qinghe-life-backend-1.0.0.jar`。
- **前端构建：** `D:/develop/NodeJS/npm.cmd run build` 成功，生成 `frontend/dist/index.html`；保留第三方 `@vueuse/core` 注释和单个压缩包超过 500 kB 的非阻断警告。
- **完成条件：** 分类、首页、商铺、商品、Redis 商铺详情缓存、四个前端 API、三个真实页面、M2A 回归、M2B 集成测试、Maven 打包及前端构建均已满足。
- **修改文件：** `backend/API.md`、`backend/src/main/java/com/qinghe/life/interceptor/LoginInterceptor.java`、`backend/src/main/java/com/qinghe/life/utils/RedisKeys.java`、`backend/src/test/java/com/qinghe/life/UserAuthenticationIntegrationTest.java`、`frontend/src/views/HomeView.vue`、`frontend/src/views/ShopListView.vue`、`frontend/src/views/ShopDetailView.vue`、`PROJECT_PLAN.md`、`progress.md`、`findings.md`、`task_plan.md`。
- **新增文件：** M2B 的分类、首页、商铺、商品 Controller/Service/ServiceImpl、DTO/VO，四个前端 API 文件，以及 `docs/HANDOFF.md`；详细清单见本会话最终回复与交接文档。
- **停止点：** M2B 完成后停止；未实现地址、购物车、订单、优惠券领取、探店写操作、后台管理，未进入 M3A。
- **最终文件核验：** 24 个必需源码、前端、构建产物和交接文件全部存在且非空；分类与首页 Java package 声明正确；后端源码和前端源码未发现 `com.hmdp` 或 hmdp 数据库连接引用。

## 会话：2026-07-10（M3A 第一批）

- **状态：** in_progress
- **范围：** 地址管理后端；同时完成 M3A 表结构审计和安全增量 SQL。购物车与订单业务代码留待后续批次。
- **恢复确认：** M2A/M2B 已完成，M3A 尚未开始；工程配置仍为 Java 8 目标、后端 8090、前端 5174、`qinghe_life`、Redis database 2、`qh:` Key 和 Bearer Token。
- **结构审计：** 地址表使用 `receiver_name/receiver_phone`；购物车缺少 `shop_id/selected`；订单缺少 `address_id/contact_name/contact_phone` 且使用 `address_snapshot/order_status`；订单明细缺少 `goods_image` 且使用 `price_snapshot/subtotal_amount`。索引要求已由现有索引满足。
- **验证限制：** 增量 SQL 不自动执行；在用户通过 DataGrip 审核执行前，不运行依赖新字段的真实地址数据库测试。
- **脚本错误：** 增量文件首次核验误用已知的 PowerShell `foreach | Format-Table` 写法，解析阶段失败，未执行 SQL 或数据库操作；已改为结果数组方案。
- **文档错误：** 首次文档收口补丁因 `docs/api-contract.md` 上下文不匹配而整体未应用；源码和编译结果未受影响，后续拆分更新。

### M3A 第一批：地址管理后端

- **状态：** complete（M3A 总体仍为 in_progress）
- **新增文件：** `backend/src/main/resources/sql/m3a_increment.sql`、`AddressController.java`、`AddressService.java`、`AddressServiceImpl.java`、`AddressCreateDTO.java`、`AddressUpdateDTO.java`、`AddressVO.java`。
- **修改文件：** `docs/database-design.md`、`docs/api-contract.md`、`backend/API.md`、`backend/src/main/resources/sql/qinghe_life.sql`、`UserAddress.java`、`Cart.java`、`Order.java`、`OrderItem.java`、`PROJECT_PLAN.md`、`task_plan.md`、`progress.md`、`findings.md`、`docs/HANDOFF.md`。
- **功能：** 五个地址接口全部要求登录；只操作当前用户地址；实现输入校验、首地址默认、默认切换事务、删除默认地址后补选和越权/不存在异常。
- **文件核验：** 7 个新增文件均存在、非空、可读取且实际路径位于当前项目内。
- **编译：** 在 `backend` 执行 `mvn -DskipTests compile` 成功，实际重编译 78 个源文件。
- **数据库测试：** 未执行。`m3a_increment.sql` 尚未人工应用，按本轮规则不得运行依赖新字段的真实地址数据库测试。
- **停止点：** 地址批次完成后停止，购物车和订单业务代码尚未开始。
- **M2 回归：** `mvn -Dtest=UserAuthenticationIntegrationTest test` 通过，2 个测试、0 失败、0 错误；该测试不访问地址、购物车或订单新字段。
- **最终核验：** 7/7 个地址/增量文件存在且非空；五个地址接口已登记；未创建购物车、订单、优惠券、探店或后台 Controller/Service；增量 SQL 不含 `DROP`、`TRUNCATE`、`FLUSHALL` 或 `FLUSHDB`。

## 会话：2026-07-10（M3A 地址实测与购物车续作）

- **状态：** in_progress
- **截图事实：** DataGrip 显示 `qinghe_life.qh_user_address` 仍使用 `receiver_name/receiver_phone`，主键、自增、`user_id` 索引、默认值和时间字段均符合用户描述。
- **当前缺陷：** Java 地址实体、DTO、VO、Service 和接口文档使用 `contactName/contactPhone`，与实际数据库不一致；本轮优先统一改为 `receiverName/receiverPhone`，不修改已确认正确的数据库列。
- **门禁：** 必须先完成四表只读结构核验和地址真实测试；任一失败都不得开始购物车。
- **只读核验：** MySQL CLI 仅连接 `qinghe_life`，执行 `SHOW COLUMNS/SHOW INDEX`。地址表主键、自增、`user_id` 索引、默认值和时间字段均正确。
- **阻塞差异：** `qh_cart` 实际缺少 `shop_id/selected` 且仍有 `price_snapshot`；`qh_order` 实际仍为 `address_snapshot/order_status` 且缺少 `address_id/contact_name/contact_phone`；`qh_order_item` 实际仍为 `price_snapshot/subtotal_amount` 且缺少 `goods_image`。
- **索引结果：** `qh_cart(user_id, goods_id)` 联合唯一索引和 `qh_order.order_no` 唯一索引均存在。
- **门禁结果：** 其他 M3A 表未同步，立即停止；未运行地址真实测试，未创建购物车代码，未开始订单或 M3B。
- **地址纠偏编译：** `mvn -DskipTests compile` 成功，实际重编译 78 个源文件。
- **最终边界核验：** 地址领域已无 `contactName/contactPhone` 混用；未创建任何购物车实现文件。

## 会话：2026-07-10（M3A 只读数据库结构门禁）

- **状态：** in_progress
- **范围：** 仅审计 `qinghe_life` 七张表，不修改数据库、源码或前端，不执行增量 SQL。
- **文件恢复：** 七个实体均通过 `@TableName` 指向 `qh_` 表；主键继承 `BaseEntity.id` 的 `@TableId`；未使用 `@TableField`；全局下划线转驼峰已启用；七个 Mapper 均仅继承 `BaseMapper`，无注解或 XML 自定义 SQL。
- **文档基线：** `qinghe_life.sql` 与当前目标实体一致；`m3a_increment.sql` 只包含购物车、订单和订单明细三个 ALTER，不再修改地址表。
- **只读命令：** 仅执行 `SELECT DATABASE()`、`SELECT 1`、七表 `SHOW COLUMNS/INDEX/CREATE TABLE` 和 `SELECT COUNT(*)`；确认当前库为 `qinghe_life`，没有写操作。
- **一致表：** `qh_user_address`、`qh_goods`、`qh_shop`、`qh_user` 与对应实体、Mapper、文档和基线 SQL 一致。商品沿用项目既定 `cover_image/sale_status`，用户沿用 `avatar_url`。
- **不一致表：** `qh_cart`、`qh_order`、`qh_order_item` 仍是 M2B 旧结构；三表当前记录数均为 0。
- **增量 SQL 结论：** 当前不宜直接执行。购物车遗留非空 `price_snapshot`、新增 `shop_id` 可空且缺少用户/商铺索引；订单新增 `address_id` 可空且使用 contact 快照命名，与本轮 receiver 目标冲突；脚本不是幂等的，重复执行会报字段/索引或旧列不存在错误。
- **门禁：** 地址真实测试受三表门禁规则阻塞；购物车和订单开发均阻塞。未修改数据库、源码或前端，未执行增量 SQL，未进入 M3B。

## 会话：2026-07-10（M3A 数据库结构门禁修订）

- **状态：** complete（M3A 总体仍为 in_progress，等待人工执行与重新审计）
- **范围：** 仅修订三张待迁移表的增量 SQL、`Order` 目标字段、基线 SQL 和数据库设计文档；不执行 SQL，不开始购物车、订单或优惠券业务。
- **修改文件：** `backend/src/main/resources/sql/m3a_increment.sql`、`backend/src/main/resources/sql/qinghe_life.sql`、`backend/src/main/java/com/qinghe/life/entity/Order.java`、`docs/database-design.md`、`docs/HANDOFF.md`、`PROJECT_PLAN.md`、`task_plan.md`、`progress.md`、`findings.md`。
- **修订内容：** 购物车 `shop_id` 改为必填，移除未映射且无默认值的 `price_snapshot`，增加用户选中/商铺查询索引；订单 `address_id` 改为必填，收货快照统一为 receiver 命名；订单明细维持图片新增与价格/小计列重命名。
- **验证：** 静态门禁通过；`mvn -DskipTests compile` 成功，重新编译 78 个源文件。
- **检查脚本错误：** 第一次将注释中的 `qh_user_address` 误判为 DDL，第二次误将物理索引名作为设计文档必须项；均发生在 Maven 前，未执行 SQL 或数据库操作。拆分断言后第三次静态检查通过。
- **停止点：** 未执行 `m3a_increment.sql`、未连接或写入数据库、未运行地址真实测试，未开始购物车、订单或 M3B。需由用户在 DataGrip 人工审核并仅执行一次后，重新进行只读结构审计。

## 会话：2026-07-10（M3A 人工迁移后复核与地址真实测试）

- **状态：** complete（M3 总体仍为 in_progress）
- **人工执行确认：** 用户确认已在 DataGrip 人工审核并执行一次 `backend/src/main/resources/sql/m3a_increment.sql`；本轮未再次执行脚本、未修改数据库结构。
- **只读结构复核：** 仅连接 `qinghe_life`，执行 `SELECT 1`、四表 `SHOW COLUMNS`、`SHOW INDEX` 与 `SELECT COUNT(*)`。`qh_user_address`、`qh_cart`、`qh_order`、`qh_order_item` 与 Java 实体、`qinghe_life.sql`、`database-design.md` 和增量脚本预期一致；四表计数均为 0；未访问 `hmdp`。
- **新增文件：** `backend/src/test/java/com/qinghe/life/AddressIntegrationTest.java`。
- **修改文件：** `backend/API.md`、`PROJECT_PLAN.md`、`progress.md`、`findings.md`、`docs/HANDOFF.md`、`task_plan.md`。
- **地址测试：** 单独运行 1 项测试通过；覆盖未登录 401、地址创建、首地址默认、默认切换、receiver 数据库映射、用户隔离、越权更新/删除拒绝、参数校验、删除默认地址补选与 ThreadLocal 清理。
- **数据清理：** 测试地址和用户使用 `M3A_ADDR_TEST_` 标识；只删除本次两名测试用户、其地址、验证码与 Token Key。结束后只读计数的测试用户和地址均为 0，未删除展示数据。
- **回归与打包：** `mvn test` 通过，3 项测试、0 失败、0 错误；`mvn clean package -DskipTests` 通过，重新编译 78 个主源码与 2 个测试源码并生成 JAR。
- **停止点：** 未实现购物车、订单、优惠券或 M3B；M3 保持 in_progress。下一步仅可在用户审核后进入购物车后端批次。

## 会话：2026-07-10（M3A 购物车后端批次）

- **状态：** complete（M3 总体仍为 in_progress）
- **结构门禁：** 仅连接 `qinghe_life` 并执行连接检查、`SHOW COLUMNS`、`SHOW INDEX`。`qh_cart` 的稳定字段、非空约束、默认值、用户商品唯一与用户商铺索引均正确；`qh_goods.cover_image/sale_status` 和 `qh_shop.name/status` 与实体一致。未访问 `hmdp`，未修改数据库结构。
- **新增文件：** `CartController.java`、`CartService.java`、`CartServiceImpl.java`、`CartAddDTO.java`、`CartUpdateDTO.java`、`CartSelectedDTO.java`、`CartItemVO.java`、`CartShopGroupVO.java`、`CartSummaryVO.java`、`CartIntegrationTest.java`。
- **修改文件：** `CartMapper.java`、`backend/API.md`、`docs/api-contract.md`、`PROJECT_PLAN.md`、`progress.md`、`findings.md`、`docs/HANDOFF.md`、`task_plan.md`。
- **接口：** 实现 `GET/POST /api/cart`、`PUT /api/cart/{id}`、`PUT /api/cart/{id}/selected`、`DELETE /api/cart/{id}`、`DELETE /api/cart`；全部仅操作 `UserContext` 当前用户。
- **并发与查询：** 重复添加使用参数化条件原子累加和唯一键冲突重试；查询批量加载相关商品、商铺后用 Map 分组，金额使用当前价格与 `BigDecimal` 计算，未修改 M2B 商铺缓存。
- **测试：** `CartIntegrationTest` 覆盖未登录、重复添加、数量/库存、商品和商铺状态、越权、按商铺分组、当前数据映射、金额/选择统计、清空隔离、空购物车、ThreadLocal 清理与精确清理。
- **验证：** `mvn -DskipTests compile` 成功，87 个主源码；`mvn -Dtest=CartIntegrationTest test` 成功，1 项测试；`mvn test` 成功，4 项测试、0 失败、0 错误；`mvn clean package -DskipTests` 成功，编译 87 个主源码、3 个测试源码并生成 JAR。
- **测试数据清理：** `M3A_CART_TEST_` 用户、商铺、商品、购物车残留均为 0；未执行整表清理、危险 SQL、Redis 清库或 Git 命令。
- **停止点：** 未实现购物车前端、订单、支付、优惠券或 M3B。M3 保持 in_progress，下一批仅可在用户审核后进入地址与购物车前端接入。

## 会话：2026-07-10（M3A 地址与购物车前端批次）

- **状态：** complete（M3 总体仍为 in_progress）
- **新增文件：** `frontend/src/api/address.js`、`frontend/src/api/cart.js`、`frontend/src/views/AddressView.vue`。
- **修改文件：** `frontend/src/views/CartView.vue`、`frontend/src/views/ShopDetailView.vue`、`frontend/src/views/ProfileView.vue`、`frontend/src/router/index.js`、`docs/page-design.md`、`docs/api-contract.md`、`PROJECT_PLAN.md`、`progress.md`、`findings.md`、`docs/HANDOFF.md`、`task_plan.md`。
- **功能：** 地址页复用 `api/http.js` 并受登录保护；购物车页调用真实六个后端接口并在操作后重新加载；商铺详情支持真实加购、库存边界和登录 redirect。未发送 `userId`、价格、金额或订单数据，未创建第二套 Axios/Token/登录逻辑。
- **静态核验：** 九个前端基线文件存在且非空；receiver 字段、地址路由、购物车 API、详情加购和导航入口均存在；未发现 contact 命名或订单 API 请求。
- **构建与回归：** 前端 `npm.cmd run build` 成功，保留第三方注释与大包警告；后端 `mvn test` 已通过 4 项测试，`mvn clean package -DskipTests` 已通过并生成 JAR。
- **浏览器联调：** 未完成。自动浏览器会话异常中断，本轮不再使用浏览器或启动新前后端实例；需用户手动在外部浏览器验证全链路。该结论不替代业务代码验收。
- **停止点：** 未实现订单、支付、优惠券或 M3B。M3 保持 in_progress，下一批仅可在用户审核后进入订单创建与查询后端。

## 会话：2026-07-10（M3A 前端最终命令行验收）

- **状态：** complete（M3A 总体仍为 `in_progress`，等待用户人工浏览器联调与审核）。
- **恢复与静态核验：** 依据 `AGENTS.md`、`docs/HANDOFF.md`、`PROJECT_PLAN.md`、`progress.md`、`findings.md`、规格/接口/页面设计、`backend/API.md`、`task_plan.md` 和实际前端目录恢复状态。地址/购物车 API 与页面各只有一套，均复用 `frontend/src/api/http.js`；地址路由、购物车导航、“我的”入口和商铺详情加购存在。未发现第二个 Axios 实例、硬编码 8090、`userId`、`contactName/contactPhone`、订单 API 调用或虚假支付/结算成功逻辑。
- **代码改动：** 无。本轮仅将 `docs/api-contract.md` 中地址更新接口的旧“联系人、电话”表述纠正为实际 `receiverName/receiverPhone`，并同步交接、计划、进度、发现和页面设计文档。
- **前端构建：** `D:/develop/NodeJS/npm.cmd run build` 成功，转换 1681 个模块并生成 `frontend/dist`。第三方 `@vueuse/core` 的两条 `/* #__PURE__ */` 注释位置提示和压缩后主包 1105.63 kB（超过 500 kB）均为非阻断警告。
- **后端回归：** `mvn test` 成功，`AddressIntegrationTest` 1 项、`CartIntegrationTest` 1 项、`UserAuthenticationIntegrationTest` 2 项，共 4 项，0 failure、0 error、0 skipped。
- **Maven 打包：** `mvn clean package -DskipTests` 成功，编译 87 个主源码和 3 个测试源码，并生成 `backend/target/qinghe-life-backend-1.0.0.jar`。
- **进程与 curl：** `Get-NetTCPConnection` 显示 8090 无监听、5174 在 `::1:5174` 监听。因 8090 未监听，未执行 `/api/home/summary`、`/api/addresses`、`/api/cart` 三条 curl；未启动、停止或重启任何进程，也未使用浏览器。
- **停止点：** 外部浏览器仍须完成 HANDOFF 中的 15 项人工联调；下一开发批次为订单创建与查询后端，但本轮未开始。

## 会话：2026-07-10（M2A 验证码获取与登录链路修复）

- **状态：** complete（M3 总体仍为 `in_progress`）。
- **根因：** `LoginView.vue` 的获取验证码请求没有 loading/请求中禁用，失败 `catch` 为空，倒计时定时器不在组件卸载时清理，且页面直接展示固定开发验证码；`UserServiceImpl` 又在所有 profile 固定该验证码，并在登录时额外比较常量，无法安全支持非开发环境验证码。
- **修改文件：** `frontend/src/views/LoginView.vue`、`backend/src/main/java/com/qinghe/life/service/impl/UserServiceImpl.java`、`backend/src/main/resources/application.yml`、`backend/src/main/resources/application-dev.yml`、`backend/src/test/java/com/qinghe/life/UserAuthenticationIntegrationTest.java`、`backend/API.md`、`docs/api-contract.md`、`PROJECT_PLAN.md`、`progress.md`、`findings.md`、`docs/HANDOFF.md`、`task_plan.md`。
- **实现：** 前端继续调用 `POST /api/user/code` 的 JSON `{phone}`，复用 `api/http.js`；增加发送 loading、重复禁用、60 秒倒计时、真实错误消息、成功消息和卸载清理。默认 `dev` profile 将 `123456` 仅配置于 `application-dev.yml`，服务仅在 `dev/local` 后端日志输出；其他环境生成随机六码，登录只比较同一 Redis Key 的实际值。
- **Redis 与登录测试：** `UserAuthenticationIntegrationTest` 实际确认 `qh:login:code:13900009981` 存在、TYPE 为 STRING、TTL 大于 0 且不超过 5 分钟；匿名发送可访问，错误/不存在/实际过期验证码均拒绝，成功登录删除验证码且 Token Hash 写入 `qh:login:token:{token}`。测试数据仅使用 `M2A_CODE_TEST_` 和精确 Redis Key 清理。
- **验证：** `mvn test` 成功，共 5 项，0 failure、0 error、0 skipped；`mvn clean package -DskipTests` 成功，编译 87 个主源码、3 个测试源码并生成 JAR；前端 `D:/develop/NodeJS/npm.cmd run build` 成功，转换 1681 个模块并生成 `dist`。
- **运行实例：** 8090 和 5174 均未监听，未执行 curl，未启动、停止或重启服务，未使用浏览器。前端构建仍保留第三方注释与超过 500 kB 主包的非阻断警告。
- **停止点：** 用户须按 HANDOFF 的验证码人工验证步骤在现有开发环境完成 UI 验证；订单和 M3B 均未开始。

## 会话：2026-07-10（验证码 404 代理修复）

- **状态：** `awaiting_manual_verification`。用户已在外部浏览器确认原登录页点击“获取验证码”出现多次 404，故此前自动测试结论不能作为浏览器验收。
- **真实根因：** `LoginView.getCode` 调用 `sendCode`，`user.js` 调用 `/user/code`，`http.js` baseURL 为 `/api`，浏览器实际请求 `http://localhost:5174/api/user/code`。后端唯一 Controller 是 `POST /api/user/code`（JSON `{phone}`）且匿名白名单正确；但原 `vite.config.js` 没有 `server.proxy`，请求留在 5174 返回 404，8090 未收到请求，因此 IDEA 没有对应日志。
- **修改：** 新增 Vite `/api` 原样代理至 `http://localhost:8090`，不 rewrite；浏览器 `/api/user/code` 现在应转发为后端 `POST http://localhost:8090/api/user/code`。HTTP 封装为已展示错误添加标记，登录页只在未展示时兜底，避免同次失败多弹窗。认证服务增加脱敏发送/登录日志、Redis 成功日志、失败类别日志和仅 `dev/local` 的 `[LOCAL DEV ONLY]` 明文验证码日志。
- **自动验证：** `mvn test` 成功，5 项、0 failure、0 error、0 skipped；地址、购物车、M2A/M2B 回归继续通过。`mvn clean package -DskipTests` 成功，87 个主源码、3 个测试源码并生成 JAR；前端构建成功，1681 个模块并生成 `dist`。
- **人工门禁：** 用户须在 VS Code 重启 Vite（代理配置仅在启动时读取）并在 IDEA 重启 Spring Boot（载入新日志代码）后，外部浏览器验证请求、单次提示、倒计时、日志验证码、登录及验证码一次性使用。仅刷新前端不足以加载 Vite 新代理。未启动或停止任何进程，未使用浏览器。

## 会话：2026-07-10（M2A Redis 登录双拦截器重构）

- **状态：** `awaiting_manual_verification`。订单及 M3A 后续开发保持暂停。
- **实现：** 删除固定开发验证码配置，验证码改为随机 6 位、同手机号覆盖写入 `qh:login:code:{phone}`、TTL 2 分钟；Token 使用去连字符 UUID，安全 `UserDTO` 字符串 Hash 写入 `qh:login:token:{token}`、TTL 30 分钟。验证码成功登录后删除，验证码错误/缺失不创建用户或 Token。
- **拦截器：** 新增 `RefreshTokenInterceptor` order 0，负责 Bearer 解析、Redis Hash 查询、写入/清理 `UserContext` 与 Token 续期；`LoginInterceptor` order 1 只检查 `UserContext` 并返回 401，不重复 Redis 查询。公开接口已在 WebMvc 配置排除，地址、购物车、资料保持登录保护。
- **前端：** 继续使用唯一 Axios、认证 API 和 Pinia Store；请求头统一为 `Authorization: Bearer <token>`，401 清理与 redirect 不变。用户端顶部在已登录时显示昵称和退出入口，刷新后由现有 Store 恢复登录态。
- **测试变更：** 认证测试覆盖随机验证码、重新获取覆盖、2 分钟 TTL、固定 `123456` 无效、Redis Hash/Token 格式、受保护访问、错误 Token、Token 续期和 ThreadLocal 清理；地址、购物车测试改为读取本次 Redis 随机验证码，不再依赖固定值。
- **验证：** `mvn clean package -DskipTests` 成功，编译 88 个主源码、3 个测试源码并生成 JAR；前端生产构建成功，转换 1681 个模块并生成 `dist`。`mvn test` 失败：Redis `192.168.100.128:6379` 连接超时，测试共 5 项中地址/购物车为 503，认证 3 项在 Redis 清理阶段异常；未用 mock、未改配置、未启动服务或清库。
- **停止点：** Redis 恢复连通后必须重跑 `mvn test`；之后用户须重启 IDEA 后端和 Vite、在外部浏览器完成验证码登录与顶部登录态验证。完成前不得将登录标记为 completed。

## 会话：2026-07-11（全局异常处理体系维护）

- **状态：** complete。本轮仅完善全局异常处理与测试、文档记录；M3 总体状态不变，未进入订单、支付、优惠券或 M3B。
- **修改文件：** `backend/src/main/java/com/qinghe/life/exception/BusinessException.java`、`backend/src/main/java/com/qinghe/life/exception/GlobalExceptionHandler.java`、`backend/src/test/java/com/qinghe/life/GlobalExceptionHandlerTest.java`、`backend/API.md`、`docs/api-contract.md`、`docs/HANDOFF.md`、`task_plan.md`、`findings.md`、`progress.md`。
- **实现：** 保持 `Result(code,message,data)` 兼容；业务异常保留原业务码与安全提示；处理六类参数异常、重复键、完整性、方法不支持、Redis/数据库连接及未知异常。唯一索引名白名单映射覆盖手机号、购物车、订单号及其余基线唯一约束；未知重复键不泄露数据库文本。
- **测试：** 定向 `mvn -Dtest=GlobalExceptionHandlerTest test` 通过 8 项。全量 `mvn test` 通过 13 项，0 failure、0 error、0 skipped；地址、购物车、认证测试与未登录 HTTP 401 均继续通过，Spring 上下文未报告异常处理器歧义。
- **打包：** `mvn clean package -DskipTests` 通过，编译 88 个主源码与 4 个测试源码，生成 `backend/target/qinghe-life-backend-1.0.0.jar`。测试源码有一个已弃用 API 的非阻断编译提示。
- **边界与停止点：** 未修改数据库、SQL、登录、Redis、地址、购物车、订单等正常业务逻辑；未启动、停止或重启任何服务。若本地 IDEA 后端正在运行，需由用户自行重启后才会加载异常处理改动；本轮到此停止。

## 会话：2026-07-11（前端 UI/UX 专业化改造）

- **状态：** in_progress。仅优化普通用户业务页面视觉与交互反馈；登录、注册、订单、支付、后端、数据库和 Redis Token 均不在本轮范围。
- **已完成：** 在现有 `base.css` 建立统一设计变量和响应式基础；新增标题、异步状态、商铺/商品卡、状态标签复用组件；完成用户布局、首页、商铺列表/详情、购物车、地址和“我的”的 UI 改造。
- **保留：** 单一 `http.js`、路由、Pinia 登录态、Bearer Token、地址 CRUD、真实购物车接口与商铺详情加购逻辑均未改写。
- **待验证：** 执行前端生产构建，检查图标、模板、CSS、重复组件、旧地址字段与不应出现的订单/支付请求；构建通过后更新文档并停止。

### 前端 UI/UX 最终验证与交接

- **状态：** complete。M3 总体状态不变；本轮仅完成既有普通业务页面的 UI/UX 改造，未进入登录、注册、订单、支付、后端或数据库任务。
- **新增文件：** `frontend/src/components/PageHeader.vue`、`AsyncState.vue`、`ShopCard.vue`、`GoodsCard.vue`、`StatusTag.vue`。
- **修改文件：** `frontend/src/assets/base.css`、`frontend/src/layouts/UserLayout.vue`、`HomeView.vue`、`ShopListView.vue`、`ShopDetailView.vue`、`CartView.vue`、`AddressView.vue`、`ProfileView.vue`、`docs/page-design.md`、`docs/HANDOFF.md`、`task_plan.md`、`findings.md`、`progress.md`。
- **分页：** 仅 `ShopListView` 接入真实 `GET /api/shops` 服务端分页，使用 `total, sizes, prev, pager, next, jumper`；page/size 更新均重新请求接口。
- **构建：** 首次构建因 `HomeView.vue` 的孤立 CSS `.` 失败，删除后 `D:/develop/NodeJS/npm.cmd run build` 成功，转换 1692 个模块并生成 `frontend/dist`。
- **静态检查：** 单一 Axios 实例；旧地址字段、本地地址硬编码、订单/支付 HTTP 请求均未发现。保留第三方 `@vueuse/core` 注释和 1124.92 kB 主包的非阻断警告。
- **停止点：** 未启动、停止或重启任何服务，未使用浏览器。构建不替代人工 UI 验收；下一批经用户审核后才可进入登录页重构与注册登录设计。

## 会话：2026-07-11（账号注册数据库准备）

- **状态：** awaiting_manual_migration。基线 `qh_user`、数据库设计和 `User` 实体均缺少 `username/password_hash` 与用户名唯一索引，触发用户设定的数据库迁移停止门禁。
- **已完成：** 为 `User` 增加账号字段映射；为全新安装基线 SQL 增加账号字段和 `uk_qh_user_username`；新增一次性 `account_register_increment.sql`；全局异常映射增加“用户名已存在”；增加最小 BCrypt 依赖。
- **未实施：** 注册 Controller/Service/DTO、公开白名单、注册页面/路由/登录页入口、注册集成测试和账号密码登录均未创建。验证码、Redis Token、地址、购物车、订单及其他业务代码未改动。
- **迁移策略：** 账号字段允许 `NULL`，用于标识旧手机号验证码用户未绑定账号；将来注册只能原地绑定同手机号旧用户，或创建新用户，且必须保存 BCrypt Hash。脚本必须由用户在 DataGrip 审核、手动执行并复核后才能开始真实注册实现与测试。
- **待验证：** 仅运行 `mvn -DskipTests compile` 检查实体/依赖/SQL准备关联的源码编译；按迁移停止门禁，不执行 `mvn test`、真实注册测试或前端构建。

### 账号注册迁移准备最终结果

- **编译：** `mvn -DskipTests compile` 成功，编译 88 个主源码；最小 `spring-security-crypto` 依赖已可解析。
- **静态核验：** `account_register_increment.sql` 只有 `ALTER TABLE qh_user` 新增账号字段和 `CREATE UNIQUE INDEX uk_qh_user_username`；无自动执行、DROP、TRUNCATE、清表或 Redis 操作。实体、基线 SQL、迁移 SQL 和重复键映射均包含 `username/password_hash/uk_qh_user_username`。
- **测试边界：** 数据库迁移未执行，故未运行 `mvn test`、注册集成测试或前端构建；不伪造注册成功。现有验证码登录、Redis Token、地址、购物车与其他业务未改动。
- **停止点：** `awaiting_manual_migration`。用户须在 DataGrip 审核并仅执行一次增量脚本、复核字段与索引后，再授权下一轮实现注册接口、注册页面和真实测试；账号密码登录仍是注册验收后的后续批次。

## 会话：2026-07-11（账号注册实施前结构复核）

- **状态：** blocked。用户报告已执行增量 SQL，但只读 MySQL 核验连接到 `qinghe_life` 后，`qh_user` 仍无 `username/password_hash` 和 `uk_qh_user_username`。
- **只读证据：** `SELECT DATABASE()` 返回 `qinghe_life`；`SHOW COLUMNS FROM qh_user` 仅有旧用户字段；`SHOW INDEX FROM qh_user WHERE Key_name='uk_qh_user_username'` 无记录。
- **停止点：** 未创建或修改注册接口、DTO、Service、前端页面、路由、公开白名单、测试或现有认证逻辑；未执行 SQL、Redis 写入、服务控制或 Git。请用户确认 DataGrip 连接实例和脚本执行结果后再继续。

## 会话：2026-07-11（账号注册实现）

- **状态：** in_progress。数据库结构只读门禁已通过；注册后端和集成测试已实现，前端注册页面、文档和全量验证待完成。
- **后端实现：** 复用 `UserController` 的 `/api/user` 前缀新增匿名 `POST /api/user/register`；`UserService` 复用 `RedisKeys.code(phone)`、BCrypt 和事务，新手机号创建用户，历史手机号用户原地绑定，已绑定手机号/重复用户名拒绝。
- **安全：** 请求 DTO 仅含用户名、手机号、验证码和两次密码，忽略敏感附加字段；不创建 Token、不保存明文密码或密码 Redis 数据、不记录密码或验证码。
- **测试：** 新增 `UserRegistrationIntegrationTest`，用 `REGISTER_TEST_` 用户名/昵称前缀和精确 Redis Key 清理；尚未运行 Maven 测试，不能把实现写为已验证通过。

### 注册前端实现

- **新增：** `UserRegisterRequest`、`UserRegistrationIntegrationTest`、`frontend/src/views/RegisterView.vue`；现有 `UserController`、`UserService`、`UserServiceImpl`、`WebConfig`、异常映射、前端用户 API、Router 和 LoginView 已最小扩展。
- **前端：** `/register` 未登录可访问，已登录访问会回首页；页面复用 `sendCode`、`http.js` 和现有 UI 变量，完成校验、倒计时、密码显示/隐藏、提交防重和成功跳转。没有新 Axios、Router 或 Store。
- **后端：** 注册不生成 Token；BCrypt Hash 只保存到 `password_hash`；历史手机号用户仅补齐账号字段。待运行新增测试后再进入全量回归。

### 账号注册最终验证与交接

- **状态：** complete。数据库字段与唯一索引只读核验通过；注册接口、页面、路由和登录入口均已实现，本轮未进入账号密码登录、找回密码、订单或支付。
- **新增文件：** `backend/src/main/java/com/qinghe/life/dto/UserRegisterRequest.java`、`backend/src/test/java/com/qinghe/life/UserRegistrationIntegrationTest.java`、`frontend/src/views/RegisterView.vue`。
- **修改文件：** `UserController.java`、`UserService.java`、`UserServiceImpl.java`、`WebConfig.java`、`GlobalExceptionHandler.java`、`GlobalExceptionHandlerTest.java`、`frontend/src/api/user.js`、`frontend/src/router/index.js`、`frontend/src/views/LoginView.vue`、`backend/API.md`、`docs/api-contract.md`、`docs/page-design.md`、`docs/HANDOFF.md`、`task_plan.md`、`findings.md`、`progress.md`。
- **测试：** 定向注册测试 5 项通过；`mvn test` 全量 18 项通过，0 failure、0 error、0 skipped；`REGISTER_TEST_` 数据库残留计数为 0。
- **构建：** `mvn clean package -DskipTests` 成功，编译 89 个主源码和 5 个测试源码并生成 JAR；前端 `npm.cmd run build` 成功，转换 1694 个模块并生成 `frontend/dist`。
- **停止点：** 未启动、停止或重启服务，未使用浏览器，未执行 SQL 或 Git。构建与测试不替代外部浏览器人工注册验收；下一批经审核后才可实施账号密码登录。

## 2026-07-12 AOP 操作日志数据库迁移准备

- **门禁：** 已对基线 SQL、数据库设计、实体、Mapper 和现有测试完成静态核验；未找到 `qh_operate_log`，因此按用户指定分支停止在人工迁移前。
- **新增：** `backend/src/main/resources/sql/operate_log_increment.sql`（仅一条 `CREATE TABLE`）、`OperateLog` 实体、`OperateLogMapper`；基线 `qinghe_life.sql` 已同步新表。
- **文档：** 已更新 `backend/API.md`、`docs/database-design.md`、`docs/HANDOFF.md`、`task_plan.md`、`findings.md` 和 `PROJECT_PLAN.md`，说明后续切面字段、安全脱敏与 `UserContext` 复用约束。
- **验证边界：** 未执行 SQL，未运行 `mvn test` 或 `mvn clean package -DskipTests`，因为用户要求在生成 SQL、实体、Mapper 和文档后停止等待 DataGrip 手工迁移；不将任何操作日志持久化测试标记为通过。
- **错误记录：** 首个跨多个文档的合并补丁因 `database-design.md` 的上下文未匹配而未应用；随即改为按实际文件锚点拆分补丁，未造成部分写入或覆盖。
- **静态复核：** 增量脚本存在且只有 1 条 `CREATE TABLE`，未出现 `ALTER`、`DROP`、`INSERT`、`UPDATE`、`DELETE` 或 `TRUNCATE`；表字段、两个索引和实体字段均无缺失。`OperateLogAspect` 与注解文件均不存在，符合迁移停止门禁。

## 2026-07-16 AOP 操作日志实现、测试与交接

- **数据库复核：** 只读连接 `qinghe_life` 确认 `qh_operate_log` 的 15 个字段、NULL 规则、主键和 `(user_id, operate_time)`、`(module, operate_time)` 索引均与实体一致；未执行 SQL 或修改结构。
- **实现：** 新增 `@OperateLog`、`OperateLogAspect`、`OperateLogService`、`OperateLogServiceImpl` 和 AOP starter；日志服务用 `REQUIRES_NEW` 保存，切面从 `UserContext.getUserId()` 取当前用户，保存失败仅记录无敏感 error，原业务返回和异常均保持。
- **接口范围：** 标注地址新增/修改/删除/设默认，以及购物车新增/数量修改/删除/清空；查询、验证码、登录、Token 刷新、购物车选中状态修改未标注。
- **安全与测试：** 摘要递归脱敏并截断至 2000 字符，隐藏密码、验证码、授权/Token、Redis/数据库密码、完整手机号并省略二进制/文件/Servlet/流对象。新增 4 项操作日志测试；全量 Maven 测试 22 项通过、0 failure、0 error、0 skipped。
- **打包与清理：** `mvn clean package -DskipTests` 成功，生成 JAR；`OPERATE_LOG_TEST_` 日志、地址摘要标识、地址测试用户、购物车测试用户残留均为 0。首次测试因同名注解/实体导入冲突在 testCompile 阶段失败一次，修正后完整通过。
# 2026-07-16 全站功能审计

- 已按用户要求读取 `AGENTS.md`、`docs/HANDOFF.md`、`PROJECT_PLAN.md`、`task_plan.md`、`progress.md`、`findings.md`、`backend/API.md`、`docs/api-contract.md`、`docs/page-design.md`、`docs/database-design.md`。
- 已确认本轮审计计划为 `task_plan.md` 阶段 65 至 68，当前仅测试、分析、写报告和更新交接记录；不得改业务代码、执行迁移或操作运行服务。
- 文档基线初判：用户与地址、首页、商铺/商品、购物车已有实现记录；订单、优惠券、探店及后台须以源码和运行端点重新核验，不能依据接口契约宣称可用。
- 运行环境检查：5174、8090、3306、6379 均处于监听状态；`GET /api/home/summary`、`/api/categories`、`/api/shops?page=1&size=2`、`/api/goods?page=1&size=2` 均为 HTTP 200。未知路径 `/api/not-a-real-route` 被鉴权层返回 HTTP 401，尚未落入 404 处理，待列入接口审计。
- 浏览器首次页面等待使用不受当前浏览器运行时支持的 `networkidle`，未改变页面或系统状态；后续改为 `load`/DOM 快照验证，不重复该失败方式。
- 用户已将本轮优先事项切换为 Codex 卡顿/未响应诊断；全站审计停在阶段 65，未进入写入型业务测试。已启用官方 Codex 文档路径：本地手册抓取因 `developers.openai.com` 返回 HTTP 403 受阻，随后使用官方站点检索作为回退；不把该文档访问失败误报为项目故障。
