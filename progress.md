# 活动进度

## 2026-07-25 管理员营业报表：启动

- 已按用户书面授权仅执行一次 `git branch --show-current` 与 `git status -sb`：当前为 `feature/business-report`，工作区干净。
- 已建立本轮持久计划：范围限定为管理员概览、趋势、排行、优惠券统计、管理端报表页及专项测试；未修改任何业务代码、SQL、配置或无关模块。
- 下一步：只读核验指定业务表/实体/认证/管理端/调度/文档，确定实时聚合所需的最小接口和索引结论后实施。

## 2026-07-25 管理员营业报表：实施与验证

- 已实现管理员概览、趋势、店铺/商品排行和优惠券使用统计；聚合均在 `BusinessReportMapper` 中完成，服务层统一 `Asia/Shanghai` 左闭右开日期范围、默认 7 天、最大 90 天、空值归零与趋势补零。未新增日报快照、定时任务、缓存或 SQL 脚本。
- 已新增管理端营业报表页面、路由、菜单和 API 模块；使用原生 SVG 趋势线与 Element Plus，未新增图表库。页面不重新计算服务端指标。
- 专项命令首次受沙箱拒绝读取本地 JAR 阻断；受控重试后两次测试分别发现/修复严格 SQL 分组兼容性和测试订单必填地址，第三次 `BusinessReportIntegrationTest` 为 3/0/0/0。测试数据以 `BUSINESS_REPORT_TEST_` 前缀创建，并按用户券、明细、订单、地址、商品、店铺、用户、管理员顺序精确清理。
- 后端 `mvn "-Dmaven.repo.local=C:/Users/28402/.m2/repository" -DskipTests package` 成功；前端 `D:/develop/NodeJS/npm.cmd run build` 成功（1745 modules），仅有第三方 PURE 注释和大 chunk 非阻断警告。

## 2026-07-24 优惠券基础业务与普通订单使用：实施结果（未收口）

- 完成候选人工增量 SQL `backend/src/main/resources/sql/coupon_foundation_increment.sql`；只读实库确认所需字段仍不存在，脚本未执行。
- 完成后端普通券模型、枚举、用户/管理员 API、领取/锁券/核销/取消释放以及订单创建 DTO/服务联动；完成用户优惠券页、结算页券选择/金额展示、后台优惠管理页/路由/API。未接入禁止的秒杀、Lua、Redis Stream、全局领取锁、WebSocket、商品缓存或报表。
- 未完成 `CouponOrderIntegrationTest`：不能把未执行候选迁移和未运行测试环境伪造成覆盖。待人工迁移后须补齐 18 个指定场景并运行用户指定 Maven 命令。
- 验证：指定 Maven 专项命令执行一次即因 `Q:\.m2` Access is denied 停止，未到编译或 Surefire；因此未运行后端 package。前端 `D:/develop/NodeJS/npm.cmd run build` 执行一次，esbuild 因工作区上级目录读取受限且无法加载 `vite.config.js` 失败。
- 未提交、未推送：专项不通过，按规则停止等待人工审核/迁移与环境恢复。

## 2026-07-24 优惠券基础业务与普通订单使用

- 已按用户书面授权仅执行一次 `git status -sb` 和 `git branch --show-current`：当前 `feature/coupon-foundation`，工作区干净。
- 已读取指定优惠券/订单源码、前端骨架、设计/API 文档及候选迁移；未读取或审计宿舍、学籍、资产模块。
- 已对本机 `qinghe_life` 执行只读 `information_schema` 查询：两张券表仍是旧字段，缺少本轮必需字段；未执行 DDL/DML/SQL 导入，下一步只生成候选人工迁移。
- 已将本轮五阶段计划写入 `task_plan.md`，阶段 1 完成。当前进行阶段 2：在不触及无关模块的前提下实现 MySQL 普通领取与订单券生命周期。

## 2026-07-24 订单超时取消与多实例任务锁：实施中

- 已按本轮唯一开始检查确认 `feature/order-timeout-lock` 与干净工作区；未 fetch、pull、reset、rebase、clean 或执行 SQL。
- 静态门禁已核对 `Order` 映射的 `payExpireTime`、`cancelTime`、`cancelReason`、`status`，以及候选迁移中的 `(status, pay_expire_time)`；实际结构仍以用户手工迁移/测试运行结果为准。
- 已抽出 `OrderCancellationService`，用户取消与超时取消共享订单明细库存恢复及直接 `qh_operate_log` 写入；超时扫描服务不持有事务，而每笔 `cancelExpiredOrder` 由独立 Spring Bean 的 `REQUIRED` 事务执行。
- 已增加配置化 Spring Task 和基于既有 `spring.redis` 配置的 Redisson 客户端。调度器只获取 `qh:lock:order:timeout-cancel`、触发扫描、并在当前线程持锁时释放；锁或 Redis 异常结束本轮，不会无锁扫描。
- 指定 Maven 专项测试实际为 20 tests / 0 failures / 0 errors / 0 skipped，其中 `OrderTimeoutCancelIntegrationTest` 为 7/0/0/0，真实 Redis 与 Redisson 锁场景均通过。随后 `mvn -Dmaven.repo.local=Q:/.m2 -DskipTests package` 成功，编译 216 个主源码、26 个测试源码并生成 JAR。

## 2026-07-19 学籍异动、批量毕业与二维码批量管理：实施启动

- 用户已授权从已完成的 `docs/academic-dorm-batch-audit.md` 实施本轮唯一里程碑；已重新读取 AGENTS、项目规格/总计划、审计、交接、现有计划/进度/发现、宿舍资产计划以及数据库、接口和页面设计。
- 已将 `task_plan.md` 顶部切换为实施计划。当前只完成文档与计划恢复，尚未修改业务代码、未执行 SQL、未连接数据库、未运行测试/构建、未控制服务或执行 Git。
- 当前关键不变量：新写当前学籍只能为 `ENROLLED/SUSPENDED/DROPPED/GRADUATED`；历史学生资料与入住分别使用 `current_flag=NULL`、`active_flag=NULL` 保留；退学/毕业和选择退宿的休学必须复用现有退宿事务；二维码令牌和资产套装编号不得清空或改写。
- 阶段 1 已完成：现有 `AdminDormCheckinServiceImpl` 有受事务保护的锁定、关闭和资产释放链路；`DormAssetAdminController`、`adminDorm.js`、`AdminLayout` 和管理员路由是唯一可扩展入口。当前学生扫码仍允许 `TRANSFER_MAJOR/REINSTATED`，本轮将收紧为仅 `ENROLLED`。
- 已完成后端、前端与文档实现：新增学生查询/详情/历史、五类学籍异动、预览令牌批量毕业/退宿/资产释放/二维码停用轮换，抽取共享退宿生命周期，前端新增学生学籍页并在资产页增加批量安全操作。未修改数据库结构、未执行 SQL、未新增日志表、未控制服务或 Git。
- 验证结果：临时 `Q:` 短路径下后端 203 个主源码 `compile` 成功，最新 `clean package -DskipTests` 成功生成 JAR；前端 Vite 生产构建成功（1731 modules）。完整 `clean test` 实际为 77 tests、14 failures、41 errors、0 skipped，均受 Redis 连接失败和管理员登录 503 影响，未达到完成条件。测试失败时报告过一项学生宿舍测试残留断言；依照“不执行 SQL”和不修改 Redis 的边界，尚未进行数据库/Redis 残留清理或只读复核。

## 2026-07-19 学籍异动、毕业处理与管理员批量宿舍操作：业务审计与分阶段设计（完成）

- 已按要求读取规则、交接、计划、进度、发现、宿舍资产设计、数据库与接口契约，并将本轮设为仅审计设计的唯一里程碑。
- 已完成实体、DDL、学生/管理员 Service、Controller、DTO/VO、管理员认证与日志切面的静态审计；确认现有学籍版本、退宿事务、资产套装和二维码字段的可复用基础及 `TRANSFER_MAJOR`/`REINSTATED` 状态收敛风险。
- 已完成统一状态、单个异动、学籍-宿舍联动、管理员边界、批量退宿/释放/二维码、批量毕业恢复与 SQL 评估设计，并已同步审计、数据库、接口、宿舍计划与交接文档。
- 未实现业务、未生成或执行 SQL、未运行测试或构建、未连接数据库、未控制服务、未执行 Git。一次文档负向 `rg` 因预期无匹配返回退出码 1，已记录且未影响审计结论。

## 2026-07-18 宿舍扫码、资产状态与中文化收口（进行中）

- 已按 `planning-with-files` 恢复规则、交接、计划、进度、发现和宿舍文档；将本轮锁定为二维码图片本地识别、资产状态规则、中文化与验证收口这一唯一里程碑。
- 已完成配置与现有实现基线：当前没有 Redis/profile 环境覆盖；解析配置为 `192.168.100.128:6379`、DB 2、`qh:` 前缀。尚无当前 Surefire XML/TXT 产物，旧 503 仅为历史记录，不判定为当前 Redis 故障。
- 已定位待修正点：学生入住未检查核心五件资产缺失/维修/报废；资产详情和管理员表格直接显示英文枚举；扫码页缺少本地图片识别与折叠的高级手动输入。尚未执行 SQL、服务控制或 Git。

## 2026-07-18 管理员校区与楼栋管理（进行中）

- 已按 `planning-with-files` 恢复交接、计划、进度、发现和宿舍设计基线，并将本轮锁定为唯一里程碑。
- 已收到完整范围：校区只读选择、楼栋新增/编辑/启停、寝室按楼栋推导校区、页面下拉联动、自动化回归、构建和文档收口。
- 用户已在 DataGrip 手工增加并复核 `qh_building.remark VARCHAR(255) NULL`；本轮只读复核确认字段、四条原始楼栋、同校区唯一键、寝室双关联和 `active_flag` 当前入住语义均正确。应用未执行 SQL。
- 门禁通过，正在实现管理员楼栋管理和新增寝室按楼栋反推校区；未修改入住事务、二维码、资产编号或现有目录数据。
- 楼栋专项首次 `testCompile` 因测试类遗漏 `UUID` 导入而停止，0 项测试执行；已按最小范围补齐导入，准备重跑。
- 受控专项已执行但未通过：修复环境连通后发现原寝室重复键 HTTP 409 兼容、测试 ID 类型和空有效标记更新三个问题，正在以保留数据库唯一约束和显式条件更新的方式修正。
- 已完成管理员楼栋新增/编辑/启停、编码冻结、校区与楼栋级联和寝室按楼栋推导校区；`area` 与 `remark` 分别映射真实字段，未改入住事务、资产编号或二维码 Token。
- 验证完成：楼栋与宿舍资产专项 5/0/0/0；完整 Maven 71/0/0/0；后端 JAR 打包和真实路径前端构建成功。`ADMIN_DORM_BUILDING_TEST_` 全部精确残留检查为 0，未执行 SQL、服务控制或 Git。

## 2026-07-18 宿舍编译基线与订单创建验证收口（完成）

- 结论：`previousCheckinId` 是换寝内部历史关联，不是管理员或学生响应字段；当前服务源码没有缺失的 VO setter 调用，因此没有添加空 setter 或虚假字段。
- 订单专项 5/0/0/0，已补强并通过所有测试数据的精确清理/零残留断言；完整 Maven 回归 68/0/0/0，后端 JAR 打包和真实前端路径构建均成功。
- 未执行手工 SQL、数据库结构变更、服务控制或 Git；本轮完成后停止，待人工验收。

## 2026-07-17 普通订单创建链路（进行中）

- 已建立本轮单一里程碑计划：先做真实数据库只读门禁，再按门禁结果决定是否进入后端、前端、测试和文档阶段。
- 已恢复项目规则及订单、地址、购物车、接口、页面和校园消费审计基线；未执行 SQL、未修改数据库、未启动或停止服务、未执行 Git。
- 数据库只读门禁失败并停止：`qh_order` 只有 `total_amount`，缺少设计要求的 `goods_amount`。现有订单/明细元数据行数均为 0，不能证明旧数据保全。未修改代码或数据库，未执行 Maven、前端构建或测试。

## 2026-07-17 学生资料与宿舍扫码入住：测试与交付收口（完成）

- 已确认 `StudentProfileVO` 不公开 `currentFlag`；普通学生端响应还排除 `passwordHash`、完整二维码令牌、`activeFlag` 和内部数据库 ID。`currentFlag` 只用于服务端当前学籍版本查询。
- 已扩充 `StudentDormIntegrationTest`：资料首次建档/校验/隔离/唯一性/仅联系电话可改，二维码格式与隐私/占用状态，入住事务/并发/回滚/唯一键转换/日志脱敏，以及“我的宿舍”空状态、隔离和脱敏。
- 验证：Q: 短路径 Maven 全量测试通过 56/0/0/0；后端 `clean package -DskipTests` 成功生成 JAR；前端 `npm.cmd run build` 成功（1718 modules）。仅有非阻断第三方 PURE 注释和大 chunk 警告。
- 测试数据使用并只清理 `STUDENT_DORM_TEST_` 前缀数据和匹配 Redis 登录 Key；测试后的精确残留断言通过。未执行手工 SQL、TRUNCATE、全表 DELETE、FLUSHDB、服务控制或 Git。

## 2026-07-17 管理端商品筛选编译修复

- 已核对 `AdminGoodsQuery`、`AdminGoodsServiceImpl`、`AdminGoodsController`、`GoodsMapper`（无 XML）、管理端 API/页面、接口契约与交接文档。
- 服务层已使用 `shopCategoryId` 过滤 `qh_shop.category_id`，使用 `goodsCategoryId` 过滤 `qh_goods.category_id`；未发现 `query.getCategoryId()` 遗留调用。
- 管理端请求已使用 `shopId`、`shopCategoryId`、`goodsCategoryId`、`saleStatus`、`keyword`；商品分类仅在选择店铺后加载并启用，避免跨店同名分类混用。
- `mvn -Dmaven.repo.local=Q:\.m2 -DskipTests compile` 成功（170 个主源文件）。`mvn -Dmaven.repo.local=Q:\.m2 clean test` 在 `ShopCoverServiceTest` 两处过期构造器调用的 `testCompile` 阶段失败，执行测试数为 0。`clean package -DskipTests` 同样因 `testCompile` 失败。前端 `npm.cmd run build` 成功（1718 modules）。


## 2026-07-17 店内商品分类与商店主页优化（进行中）

- 已恢复计划并读取项目规则、交接、进度、发现及商品分类、接口、页面和消费审计设计基线；本轮仅在用户已手工迁移的前提下继续实现。
- 只读数据库门禁尚未执行。门禁通过前不修改业务代码或数据库；门禁失败将仅记录差异并停止。
- 门禁已通过：`qh_category` 保持全局唯一店铺类型；`qh_goods_category` 为独立店内分类表，具有 `(shop_id,name)` 唯一索引及店铺外键；`qh_goods.category_id` 可空、已有索引及分类外键。当前分类表为空，4 个已有商品均未分类，符合手工逐步分配的兼容预期。

## 2026-07-17 商品店内分类模型与商店主页重构准备（完成，等待人工执行）

- 已建立本轮仅审计与迁移准备计划；已读取项目规则、交接、计划、历史进度/发现，以及校园消费审计、数据库、API、页面设计文档。
- 文档基线将 `qh_category` 定义为服务/店铺分类，`qh_shop.category_id` 承接店铺类型；商品后台页面说明也明确当前没有独立商品分类输入，需以代码和真实库复核后再决定是否生成增量 SQL。
- 本轮不会执行 SQL、修改业务代码、启动/停止服务、运行测试/构建或 Git；当前尚未生成迁移文件。
- 静态代码已确认：`Category` 仅含名称、图标、排序、启停；`Goods` 没有 `categoryId`，`Shop` 才持有 `categoryId`。管理端商品列表的“分类”选项来自 `/api/categories`，服务端将筛选值先解析成同一店铺类型下的店铺 ID，再以 `qh_goods.shop_id IN (...)` 过滤，并非商品分类关联。
- `AdminGoodsServiceImpl.update` 允许更换 `shopId`，现有模型没有可清空/重选的商品分类字段；购物车只依赖商品 ID、商品所属店铺、上架状态和库存，故后续增加可空分类关联不应改变购物车请求契约。
- 已对真实 `qinghe_life` 执行只读元数据与聚合复核：`qh_category` 为名称全局唯一的两条店铺类型目录（美食、便利服务），每项均已关联店铺及商品；`qh_goods` 有 `shop_id` 索引但没有 `category_id`。这证实其不能表示同店内可重复命名、按店铺隔离的商品分组，必须生成新的店内商品分类迁移准备。
- 已生成 `backend/src/main/resources/sql/goods_category_increment.sql`，仅包含创建 `qh_goods_category`、为 `qh_goods` 新增可空 `category_id`、索引和外键；静态检查未发现 `DROP`、`TRUNCATE` 或 `DELETE`。脚本未执行。
- 已更新数据库、API、页面、消费审计和交接文档，明确后续商品分类 ID 必须与商品店铺一致、改店时清空或重选，且图片策略改为商品 `contain` 与等比方形画布。按用户限制，未修改业务代码、购物车、订单、支付、图片资产或运行服务，也未运行测试、构建或 Git。

## 2026-07-17 学生资料与宿舍扫码入住：实名资料迁移准备（完成，等待人工执行）

- 只读确认应用配置和实际连接库均为 `qinghe_life`。`SHOW CREATE TABLE qh_student_profile` 显示 InnoDB、`utf8mb4_0900_ai_ci`、现有当前学籍唯一索引及三条外键均完整，当前数据行数为 0。
- 已新增 `backend/src/main/resources/sql/student_profile_identity_increment.sql`，仅以一个 `ALTER TABLE` 添加 `real_name VARCHAR(50) NOT NULL`、`contact_phone VARCHAR(20) NOT NULL`，均无虚假默认值；不改 `qh_user`、现有索引/外键、学籍版本模型或任何其他表。脚本注释提供执行前后人工复核 SQL。
- 当前无 `StudentProfile` 实体映射，符合本轮不实现 Java/Vue 业务的范围。未执行 SQL、未运行 Maven/前端构建、未启动或停止服务、未执行 Git。下一阶段必须由用户在 DataGrip 手工执行脚本并完成字段复核后，重新开始只读门禁。

## 2026-07-17 商品后台维护与主图 OSS（完成并验证）

- 最终补充的 `GoodsImageServiceTest` 定向运行 3/0/0/0，且其后 `mvn clean package -DskipTests` 成功。受控网络重跑完整套件因会话额度限制被拒绝；最终报告区分此前 43 项全量成功与新增断言的定向成功，不声称 44 项全量已运行。

- 实时只读 `qh_goods` 门禁通过：店铺关联、名称、简介、`decimal(10,2)` 价格、库存、销售状态和单张 `cover_image` 齐全；分类经店铺关联、无商品排序字段，无需迁移且未执行 SQL。
- 已补齐 `AdminGoodsServiceImpl`、公开商品停用店铺过滤、主图受控清理、管理接口/页面、项目内商品占位图和 Mock OSS 测试；未改购物车模型，未实现订单、优惠券、支付或配送。
- 首次测试在受限沙箱被 Redis 套接字权限阻止，43 项中 3 failures、19 errors；申请受控网络权限后按同一短路径命令重跑，`mvn clean test` 通过 43/0/0/0。`mvn clean package -DskipTests` 成功；前端第一次因父目录 `lstat` EPERM 失败，授予必要读取权限后 `npm.cmd run build` 成功（1711 modules）。

## 2026-07-17 学生资料与宿舍扫码入住（结构门禁阻断）

- 本轮开始时用户声明已手工执行 `student_profile_identity_increment.sql`，但仓库中不存在该脚本，且真实 `qh_student_profile` 未出现要求的 `real_name`、`contact_phone` 列；本轮随后已重新生成待人工执行脚本，历史差异见本文件最新迁移准备记录。
- 当前有效资料与入住的唯一约束均正确：`(user_id,current_flag)`、`(student_no,current_flag)`、`(user_id,active_flag)`、`(dorm_bed_id,active_flag)`；学生、资料、床位和资产套装外键也齐全。但实名/联系电话字段缺失，不能以 `qh_user.nickname` 或 `qh_user.phone` 替代。
- 按字段不一致立即停止：未执行 SQL、未改数据库、未继续检查源码认证/二维码链路，未实现接口、页面、测试或构建。恢复前提是用户确认已在目标 `qinghe_life` 数据库成功执行包含这两列的人工迁移，并提供或恢复对应仓库脚本；之后重新进行完整只读门禁。

## 2026-07-17 登录 Tab、OSS 路径与个人中心文案修正（进行中）

- 已完成样式与路径审计：Tab 问题由登录页局部合并选择器造成，未发现全局覆盖；头像路径调整须兼容历史受控对象删除，不能迁移旧 URL。
- 本轮严格限定为 Tab 状态样式、用户头像 Key/旧对象解析、相关 Mock 断言和个人中心文案；不改登录逻辑、Token、数据库、店铺商品图片或服务状态。


## 2026-07-17 登录、首次资料、个人资料与 OSS 用户头像（完成）

- **UI：** 登录页改为 440px 校园风格双 Tab 卡片；首次资料页改为 760px 分区双列/移动单列布局；“我的”页新增资料编辑、只读账号信息、密码状态、默认校园地址和头像本地预览。
- **头像：** 新增环境变量驱动的 `AliyunOSSProperties`、可复用 `AliyunOSSOperator` 和 `POST /api/user/avatar`，仅由当前用户头像调用。服务端验证文件并更新 `qh_user.avatar_url`，同步当前 Redis Token Hash，失败补偿新对象并限制旧对象删除范围；没有改数据库、SQL、店铺/商品图片或日志表。
- **测试：** `UserAvatarServiceTest` 完全 Mock OSS，覆盖类型/空文件/超大/伪造图片、OSS 失败、数据库更新失败补偿和旧对象删除失败；`UserAvatarIntegrationTest` Mock OSS 覆盖当前用户隔离、Redis 同步、操作日志及安全响应。完整 `mvn clean test` 在 `Q:\backend` 通过 **32 项**（0 failure、0 error、0 skipped）。
- **构建：** `mvn clean package -DskipTests` 成功生成 JAR；真实前端路径执行 `D:/develop/NodeJS/npm.cmd run build` 成功，转换 1696 个模块并生成 `dist`。前端仍有第三方注释和大 chunk 非阻断警告。

## 2026-07-17 登录、首次资料、个人资料与 OSS 用户头像（进行中）

- 已读取项目规则、交接、进度、发现以及现有登录、首次资料、个人资料、用户服务、UserContext、Redis Token、Pinia 与 HTTP 封装；确认范围可严格限定为唯一用户模块和用户头像。
- 当前没有 OSS 组件或依赖；资料更新已复用 `syncCurrentSession` 同步当前 Token Hash。后续将仅新增环境变量驱动的通用 OSS 操作器，由用户头像接口调用，自动化测试完全 Mock。
- 本轮不修改数据库、SQL、商铺/商品图片、订单、支付、服务状态或 Git。


## 2026-07-17 验证码免注册登录、首次资料完善与可选密码登录（交付收尾）

- **本轮范围：** 仅补齐交付文档并重新执行验证；没有修改业务代码、数据库结构、SQL、Redis 登录体系、认证代码、订单或支付，也没有启动、停止服务或执行 Git。
- **最终流程：** 新手机号验证码校验通过即自动建档，登录响应返回 `newUser`、`profileCompleted`、`hasPassword`；资料未完成时前端跳转 `/profile/complete`。该页在一个事务内创建默认校园地址、更新用户资料并将 `profileCompleted` 设为 `true`，可选密码只用 BCrypt 保存。手机号验证码登录和手机号密码登录均创建既有 Redis Token；安全状态同步至 Token Hash，资料完成后同步当前会话。
- **文档：** 已按实际章节补齐 `backend/API.md`、`docs/api-contract.md`、`docs/page-design.md`、本文件、`findings.md`、`docs/HANDOFF.md` 与 `task_plan.md`；未依赖不存在的页面设计标题。
- **最终验证：** 在同一 PowerShell 进程临时映射短路径后，`Q:\backend` 的 `mvn clean test` 实际通过 **25 项**（0 failure、0 error、0 skipped），`mvn clean package -DskipTests` 为 `BUILD SUCCESS` 并生成 `qinghe-life-backend-1.0.0.jar`。真实前端路径的 `D:/develop/NodeJS/npm.cmd run build` 转换 1695 个模块并生成 `frontend/dist`；仅有第三方注释与大 chunk 非阻断警告。
- **验收状态：** 开发、文档与命令行验证可标记为完成；浏览器人工验收尚未执行，不能将其表述为完整用户验收已完成。

## 2026-07-17 免注册登录与首次资料完善（数据库门禁阻塞）

- 已审计既有验证码登录、注册、`User`、User Store、个人中心、校园地址、AOP、Router 和 `http.js`：验证码登录确实会为新手机号创建随机昵称用户并生成既有 Redis Token；当前响应/Token 会话没有 `newUser`、`profileCompleted`、`hasPassword`，也没有密码登录或首次资料完善页。
- 已只读核验 `qh_user` 实际列，确认不存在 `profile_completed` 或等价持久化状态，不能以 localStorage 替代。
- 已新增仅供人工审核执行的 `backend/src/main/resources/sql/profile_completion_increment.sql`，内容仅为幂等新增 `profile_completed TINYINT(1) NOT NULL DEFAULT 0`，没有执行该脚本，也没有改动用户、地址、Redis Token、日志、接口、前端或测试代码。
- **停点：** 等待用户在 DataGrip 审核、手工执行并只读确认该字段后，才能实施免注册登录状态响应、首次资料完善、可选密码登录及对应文档/页面/回归；本轮未运行 Maven 或前端构建，因为业务实现尚未获结构门禁授权。

## 2026-07-17 账号注册功能（本轮完成，等待审核）

- **认证栈审计：** 注册始终复用唯一 `UserController`、`UserServiceImpl`、`RedisKeys.code(phone)`、双拦截器、`http.js` 与现有 `/register` 页面；未创建 AuthController、第二个 Axios、Router、Store 或账号密码登录链路。
- **接口调整：** 注册公开接口统一为 `POST /api/auth/register`；同一 `UserController` 内的验证码、登录、个人资料和退出仍使用 `/api/user/**`。`LoginInterceptor` 白名单已同步，RefreshTokenInterceptor 和 Redis Token 行为未改。
- **注册规则与兼容：** 用户名/密码/确认密码校验、验证码复用、BCrypt 哈希、成功删除验证码、不自动登录和不返回 Token 均由现有服务逻辑保持；新手机号创建用户，旧手机号且用户名/密码哈希都为空时原地绑定，已绑定手机号或重复用户名拒绝。
- **前端：** 现有 `/register` 页复用 `http.js` 调用 `/auth/register`，保持验证码 60 秒倒计时、表单校验、防重复提交、密码清空和注册成功跳转登录；登录页“立即注册”入口保持可用。
- **验证：** 在同一 PowerShell 会话临时映射 `Q:` 后，受控网络下 `mvn clean test` 为 24 项通过（0 failure、0 error、0 skipped），`UserRegistrationIntegrationTest` 为 5 项通过；`mvn clean package -DskipTests` 成功并生成 `backend/target/qinghe-life-backend-1.0.0.jar`；`D:/develop/NodeJS/npm.cmd run build` 成功生成前端 `dist`。首次受限沙箱测试被 Redis 网络策略阻断，已记录且未视为代码失败。
- **停点：** 本轮不执行 SQL、数据库结构变更、服务控制、Git、账号密码登录、订单或支付，现停止等待用户审核。

## 2026-07-16 校园地址功能实施

- 已按用户确认的前提恢复校园地址实施任务：`campus_address_increment.sql` 已由用户在 DataGrip 手工执行；本任务不会再次执行迁移或改动数据库结构。
- 已读取 AGENTS、交接、项目计划、活动计划/进度/发现、数据库/接口/API/页面设计文档，并定位现有地址实体、DTO、VO、Mapper、Service、Controller、集成测试与前端地址页面/API。
- 已建立本轮五阶段计划，当前执行只读数据库门禁：目录与地址结构不一致或目录基础数据为空时将停止，绝不自动导入或初始化数据。
- **只读门禁结果：** `qh_campus`、`qh_building`、`qh_user_address` 的全部预期列、命名索引以及 `fk_qh_building_campus`、`fk_qh_address_campus`、`fk_qh_address_building` 均与 `campus_address_increment.sql` 和数据库设计一致；地址旧字段已可空，历史兼容字段完整。
- **阻塞原因：** `qh_campus` 总数/启用数均为 0，`qh_building` 总数/启用数均为 0，因此没有可用的校区或楼栋可供新增校园地址选择。按用户规则，未开始后端、前端或测试实现，也未运行 Maven/前端构建。
- 已新增待用户审核并在 DataGrip 手工执行的 `backend/src/main/resources/sql/campus_catalog_init.sql`：事务内仅对缺失的 `QH_MAIN` 校区及四栋演示楼栋执行条件插入；不含 UPDATE、DELETE、TRUNCATE、DROP 或 DDL。脚本未被自动执行。
- **续办只读复核：** 用户确认目录初始化后，实测 `QH_MAIN/青禾主校区` 为启用校区，存在 4 条启用楼栋且全部正确关联；地址的校园字段、索引和三条外键完整，`HISTORICAL` 地址保留 2 条。本轮可进入现有地址链路的后端实现。
- **后端完成：** 新增 `Campus`、`Building` 实体/Mapper/VO/Service/Controller，公开只读 `GET /api/campuses` 与 `GET /api/campuses/{campusId}/buildings`；原地址 Controller 增加本人详情，不新建第二套地址 Controller。
- **地址规则完成：** 地址 DTO 不接收保存 `userId`、`addressType` 或省市区旧字段。创建与更新校验启用校区/楼栋及归属、房间号或配送点至少一个；保存目录关联和区域/楼栋快照，固定 `CAMPUS`。历史地址保留旧字段，编辑后转换为校园地址；默认、隔离、删除补选、全局异常和既有四个 AOP 地址日志注解均保留。
- **前端完成：** 地址页复用 `http.js` 和 `api/address.js`，新增校区/楼栋读取与加载、空态、错误重试；已移除省市区输入，增加校园位置表单、历史更新提示、格式化地址和脱敏号码显示，并保持小屏布局。
- **测试状态：** 已扩展 `AddressIntegrationTest` 覆盖目录查询、校园新增/校验、默认规则、隔离、历史转换、手机号字段和地址操作日志脱敏/复用。执行 `mvn test` 失败于测试编译环境：Maven 将中文工作区路径编码为乱码，导致所有既有及新增测试都找不到 `target/classes` 主包；不是测试断言失败。测试数据没有实际执行，未产生残留。
- **构建状态：** 指定 `mvn clean package -DskipTests` 同样在测试编译阶段失败；改用 `mvn clean package '-Dmaven.test.skip=true'` 成功，已生成 JAR。指定前端构建首次受沙箱 `lstat C:\\Users\\28402` 阻塞，经授权重试成功（1694 modules）；保留第三方 PURE 注释和大包非阻断警告。

## 2026-07-16 校园地址模型与迁移准备

- 已创建本轮独立计划并恢复项目约束；范围严格限于地址模型审计、设计、文档和未执行的迁移 SQL。
- 已确认现有记录将 M3A 地址实现描述为省/市/区/详细地址模型，并记录首地址默认、默认切换、删除默认后自动补选、当前用户隔离和手机号脱敏等既有规则需要保持。
- 当前进入实际数据库结构与代码引用的只读审计；未执行 SQL、未改业务代码、未操作服务、Redis 或 Git。
- 实际 MySQL 只读复核完成：服务器为 MySQL 8.0.34；`qh_user_address` 与基线一致，含旧省/市/区/详细地址四列、`idx_qh_address_user(user_id)` 和 2 条历史记录。`qh_campus`、`qh_building` 当前不存在；`qh_cart`、`qh_order` 均为 0 条，未修改其中任何数据。
- 源码链路复核完成：地址实体、DTO、VO、Mapper、Service、Controller 和前端页面/API 均使用同一组旧地址字段；服务层按当前用户 ID 查询/更新/删除，删除默认地址后补选最新地址，手机号仅在前端展示时脱敏。订单仅有实体与基线快照列，当前未实现订单 Controller/Service。
- 已完成新模型设计：新增 `qh_campus`、`qh_building`，地址表按目录 ID 关联并保存区域、楼栋和配送信息快照；旧地理字段保留但改为可空。两条历史地址将由待执行脚本保留原值、回填 `detail` 并标记为 `HISTORICAL`。
- 已更新 `backend/src/main/resources/sql/qinghe_life.sql`、`docs/database-design.md`、`docs/api-contract.md`、`backend/API.md`、`docs/HANDOFF.md`，并新增未执行的 `backend/src/main/resources/sql/campus_address_increment.sql`。未修改任何业务 Java/Vue 文件，未执行 SQL。
- 静态复核通过：增量脚本 9 条语句的首关键字仅为 `CREATE TABLE`、`ALTER TABLE`、`CREATE INDEX`、`UPDATE`；12 个校园字段在增量脚本与基线 SQL 中一致，四份指定文档均含迁移说明。`mvn -DskipTests compile` 成功；命令末尾有非致命的 `Access is denied.` 输出，但 Maven 以 exit code 0 和 `BUILD SUCCESS` 结束。
- 本轮在脚本生成后停止，等待用户在 DataGrip 审核并手工执行；不得提前实施校园地址前后端、目录后台管理或订单。

完整历史记录已原样归档到 `docs/history/2026-07-16-pre-context-compression/progress.md`。

## 2026-07-16 Codex 卡顿诊断

## 2026-07-16 Maven 测试编译与校园地址验证

- 已完成上下文和测试清单恢复，并在 `backend` 执行 `mvn clean test -e`。
- 结果：主源码 104 个文件编译成功；`testCompile` 7 个测试文件失败，73 个错误，全部集中为无法解析主包。未执行任何测试方法，因而未产生测试数据写入。
- 已确认 Maven 3.9.11 + JDK 21.0.9，Maven 平台 UTF-8 与 Windows CP936/gb2312 默认编码并存；继续只验证 Maven 参数文件与 classpath，不改业务代码。
- **根因与处置：** `mvn -X test-compile` 显示 test classpath 确实包含 `target/classes`，但中文目录段在 Maven 到 `javac` 的路径交接中失真；关闭参数文件和切换 `forceJavacCompilerUse` 均无效。项目 POM 未显式设置 `project.build.sourceEncoding`，但编译器实际已采用 UTF-8，因此未作无效 POM 改动。使用临时 `subst Q:` ASCII 映射后，测试编译恢复并发现唯一真实源码问题。
- **最小修复：** `AddressIntegrationTest` 的“空位置”校验调用遗漏 `deliveryPoint` 参数；已补足一个空字符串，不改变任何业务逻辑、测试场景或断言。
- **完整验证：** 在 `Q:\backend` 执行 `mvn clean test` 成功，24 tests、0 failures、0 errors、0 skipped。校园地址 3 项测试覆盖校区/楼栋查询、目录启用与归属校验、CRUD/默认/删除补选、历史转换、用户隔离、手机号脱敏和地址操作日志；其余 21 项回归覆盖认证、注册、全局异常、购物车及操作日志。
- **打包：** 随后在同一 ASCII 路径执行 `mvn clean package -DskipTests` 成功；测试源码仍参与 testCompile，Surefire 仅按该参数不重复运行，已生成 `backend/target/qinghe-life-backend-1.0.0.jar`。
- **测试数据：** 全量测试正常结束，地址、购物车、认证和日志测试都通过 `@AfterEach` 按各自唯一标识删除关联用户、地址/购物车/商品/日志及对应 Redis code/token 键；未执行清库、清 Redis 或迁移 SQL。

- 已确认本机前端、后端、MySQL、Redis 服务均在监听；`/api/home/summary`、`/api/categories`、`/api/shops?page=1&size=2`、`/api/goods?page=1&size=2` 均返回 HTTP 200。
- 工作区（排除依赖和构建产物）仅 161 个文件；活动计划、进度与发现文件在压缩前分别为 31.2、49.7、64.6 KiB。
- 内存快照：16GB 设备已用约 78.2%；外置 Edge 31 个进程约 3.49GiB 工作集、WebView 约 1.04GiB，FinalShell Java 进程约 606MiB 私有内存；Codex 进程约 133MiB 工作集。CPU 采样为 14.7%，不是持续 CPU 满载。
- 已将三份历史文件原样备份到 `docs/history/2026-07-16-pre-context-compression/`，并将根目录活动文件替换为短摘要，减少长期会话每次读取的项目上下文。
- 压缩后复核：活动文件合计约 4.1KiB（原约 145.5KiB）。`curl` 访问 5174 和 8090 均在约 2.2 秒后连接拒绝；未对服务作任何启动、停止或重启，网页验证因此阻塞。
- 浏览器测试会话在此前不支持的等待状态失败后已丢失绑定，无法执行释放动作；未产生网页提交、数据库写入或用户浏览器状态改动。
- 再次性能采样：可用内存 6.81GiB、使用率 56.9%、CPU 瞬时采样 20.6%；Codex 工作集约 99MiB/私有内存约 119MiB/56 线程，未见异常膨胀。Edge 仍有 27 个进程、约 3.58GiB 私有内存，FinalShell Java 约 613MiB 私有内存。
- 已核验根目录 `.gitignore` 存在，且排除了 `frontend/node_modules/`（11,782 文件、约 99.2MiB）、`backend/target/`、`frontend/dist/`、日志及 IDE 目录；无需新增忽略规则。
- 未修改业务代码、数据库、Redis、SQL、运行服务；未关闭任何用户进程或标签。

## 待用户操作

- 如卡顿仍在，请先关闭不需要的 Edge 标签/窗口和非必要 FinalShell 会话，再新开一个 Codex 任务继续；这些操作超出当前无损诊断的授权范围。

## 2026-07-16 全站功能审计

- 已读取规则、交接、活动计划、进度、发现、API、接口/页面/数据库设计文档。
- 静态检查确认：实际 Controller 仅覆盖用户、首页、分类、商铺、商品、地址和购物车；前端仍有订单、优惠券、探店和后台路由/视图，但后端未发现对应 Controller。
- 运行环境检查：5174、8090、3306、6379 均未监听。未启动、停止或重启任何服务；因此 API、MySQL、Redis、受控 `AUDIT_TEST_` 写入、浏览器流程均记录为阻塞，未伪造通过。
- 地址静态字段一致性初判：SQL、实体、DTO、VO 均使用 `province/city/district/detailAddress`，与校园配送模型不匹配；后续报告将列为需迁移的专项问题。
- 构建核验：`mvn -DskipTests compile` 成功；前端首次构建被受限沙箱的 Node `lstat C:\\Users\\28402` EPERM 阻断，获授权后 `npm.cmd run build` 成功，转换 1694 个模块。产物为 JS 1129.74KB、CSS 377.30KB，均触发/关联大包优化问题；另有两条第三方 `@vueuse/core` PURE 注释警告。
- 深入静态审计：订单、优惠券、探店、后台看板/资源页面均是 `PageScaffold` 骨架，没有对应 Controller/API；路由没有 catch-all 404；`requiresAdmin` 仅复用普通 `getToken()`。认证、脱敏、全局异常和地址/购物车 AOP 注解可在源码中确认，但运行实测保持阻塞。
- 已创建 `docs/full-site-audit.md`。报告登记 21 个页面/路由、26 个功能/安全审计项：构建通过 2，静态失效/缺失 5，运行阻塞 19，运行失败 0；P0=1、P1=5、P2=4、P3=2。未创建任何 `AUDIT_TEST_` 数据，无清理项。
- 本轮修改文件仅为 `task_plan.md`、`progress.md`、`findings.md`、`docs/HANDOFF.md` 和 `docs/full-site-audit.md`；未修改业务代码、SQL、数据库或 Redis，审计结束后停止。

## 2026-07-16 英文路径迁移验证

- 门禁：`C:\ruanzhu\workplace\qinghe-life-service` 不存在；当前可访问副本仍在 `C:\Users\28402\Desktop\ruanzhu\workplace\qinghe-life-service`。未创建、移动或复制项目。
- 路径扫描：排除依赖和构建产物后仅匹配两处旧中文绝对路径。`docs/HANDOFF.md` 的旧 Maven 指令已改为目标英文路径的待执行命令；`docs/history/2026-07-16-pre-context-compression/findings.md` 为归档历史，未修改。源码和构建/运行配置未命中。
- 由于目标目录缺失，未执行用户要求的 Maven 测试、Maven 打包或前端构建；不得将旧副本既有的 24 项测试通过、JAR 或 `dist` 视为本次迁移验证结果。
- 本轮仅改动记录文件和一条会误导构建的交接命令；未改业务代码、数据库结构、SQL、服务、IDEA、Vite、MySQL、Redis、浏览器或 Git。

## 2026-07-16 实际英文工作目录迁移验证

- 已确认实际工作目录为 `C:\Users\28402\Desktop\ruanzhu\workplace\qinghe-life-service`；本轮未访问或使用其他迁移路径，未修改业务逻辑、数据库结构、SQL 或服务状态。
- `backend` 中的 `mvn clean test` 失败：104 个主源码文件编译成功，7 个测试源文件在 testCompile 阶段均无法解析 `com.qinghe.life.*` 主包，0 个测试方法运行、0 份 Surefire 报告。调试确认 `target/classes` 存在且 Maven classpath 已列出该目录；强制另一编译器模式和 fork 外部 `javac` 均复现，未作猜测性代码/POM 修复。
- `mvn clean package -DskipTests` 同样失败且未生成 JAR：该参数不跳过 testCompile，故仍被相同 classpath 问题阻塞。
- `D:/develop/NodeJS/npm.cmd run build` 成功：Vite 转换 1694 个模块，生成 `frontend/dist`；保留两条第三方 `@vueuse/core` 注释警告和主 JS 1,133.47 kB 的非阻断分包提示。
- 校园地址模块不标记为本轮正式完成：此前 24 项回归是历史基线，本轮 0 项测试实际运行。待 Maven testCompile 工具链恢复后，须重跑完整测试和打包后再验收。

## 2026-07-17 Q: 短路径 Maven testCompile 验证

- 初始没有 Q: 映射；本轮仅在各 Maven PowerShell 进程内临时映射 `Q:` 到当前项目根目录，并在进程结束前解除。未修改业务代码、POM、测试、数据库、SQL、Redis 配置或服务状态。
- 环境：Apache Maven 3.9.11、Oracle JDK 21.0.9、Java release 8。相同 POM、相同 104 个主源码和相同 7 个测试源文件，在 `Q:\backend` 的 testCompile 成功。
- `mvn clean test`：24 tests、0 failures、0 errors、0 skipped，`BUILD SUCCESS`。首次受限沙箱运行已成功穿过 testCompile 但被 Redis 套接字权限阻断；获授权后使用同一 Q: 命令重跑，真实 Redis 集成测试全部通过。
- `mvn clean package -DskipTests`：成功，testCompile 仍正常编译 7 个测试文件，Surefire 按参数跳过执行，并生成 `backend/target/qinghe-life-backend-1.0.0.jar`。
- 根因已确认：当前 Windows 长绝对路径下，Maven/Javac 在 testCompile 的 classpath/参数解析不能正确加载已存在的 `target/classes`；短路径 Q: 在相同工具链与源码下无此问题。无需、也未实施项目内构建配置或业务代码修复。
- 校园地址模块的后端回归与打包验收现可标记为完成；前端浏览器人工交互验收仍按既有交接单独进行。
## 2026-07-17 登录 Tab、头像 OSS 路径与个人中心文案修正

- 已完成：修正 LoginView Tab 的激活态可读性；头像上传 Object Key 改为 `qinghe-life-service/{yyyy}/{MM}/{uuid}.webp`；旧头像 Key 的受控清理兼容保留；ProfileView 页头改为“个人中心 / 管理个人资料与校园服务”。
- 已更新回归断言：服务单元测试校验新 URL/补偿删除 Key，集成测试校验当前用户保存的新路径；旧 `avatars/{userId}/...` 测试输入仍验证旧对象清理链路。
- 未做：未修改数据库、登录/Redis Token 逻辑、日志表或商品/店铺图片；未启动或停止服务，未执行 Git。下一步仅执行用户指定 Maven 与前端构建命令并记录结果。
- 验证尝试 1：用 PowerShell `New-PSDrive Q:` 运行 Maven 后，Maven/Javac 仍解析为 C: 长路径，`testCompile` 报全量 `com.qinghe.life.*` 主包不存在，0 项测试执行。该映射不满足短路径前提；未改代码，将改用一次性 DOS `subst Q:` 映射后重跑。
- 验证完成：一次性 DOS `subst Q:` 映射下，`mvn clean test` 在受控网络权限中通过 32/0/0/0；`mvn clean package -DskipTests` 成功并生成后端 JAR；前端 `D:\develop\NodeJS\npm.cmd run build` 成功，1696 个模块完成生产构建。前端构建仅输出既有依赖 PURE 注释和大 chunk 非阻断警告。
- 本里程碑修改文件：`frontend/src/views/LoginView.vue`、`frontend/src/views/ProfileView.vue`、`backend/src/main/java/com/qinghe/life/service/impl/UserServiceImpl.java`、`backend/src/main/java/com/qinghe/life/oss/AliyunOSSOperator.java`、`backend/src/test/java/com/qinghe/life/UserAvatarServiceTest.java`、`backend/src/test/java/com/qinghe/life/UserAvatarIntegrationTest.java`、`task_plan.md`、`findings.md`、`progress.md`。
## 2026-07-17 宿舍入住与资产二维码管理：设计与迁移准备

- 已开始：已读取项目规则、交接、进度、发现和数据库设计，并静态核对校园目录、用户/管理员身份边界、`UserContext` 与唯一 `qh_operate_log` 链路。
- 范围锁定：仅进行数据模型、未执行迁移脚本及文档准备；不执行 SQL、不实现前后端、不启动或停止服务、不执行 Git。
- 已完成数据模型：复用校区、楼栋、用户、管理员和唯一操作日志；新增宿舍寝室/床位、学生资料版本、资产套装/明细、入住历史六表，并仅为现有楼栋增加可空 `building_code` 支持套装编号。
- 已生成未执行 DDL：`backend/src/main/resources/sql/dorm_asset_increment.sql`。脚本不含数据写入或清理语句；文档中的接口仅为下一阶段设计登记，未实现。
- 静态复核通过：脚本共 7 条 DDL（1 条 `ALTER TABLE`、6 条 `CREATE TABLE`），没有其他语句；已同步 `docs/database-design.md`、`docs/api-contract.md`、`docs/HANDOFF.md`、`findings.md` 和 `task_plan.md`。未执行 SQL、未运行构建或测试，符合本轮纯设计边界。
## 2026-07-17 宿舍基础档案与资产二维码

- 已开始本轮单一里程碑，读取 AGENTS.md、PROJECT_SPEC、PROJECT_PLAN、HANDOFF、progress、findings、数据库设计和现有 task_plan；未执行 Git、SQL 或服务控制。
- 已建立 `dorm_asset_plan.md`，当前处于“只读数据库门禁”阶段；下一步仅检查用户声明已执行迁移形成的真实列、索引和外键。
- 只读数据库复核完成且通过：已核验用户指定的 `building_code` 与六张宿舍资产表的列、索引和外键；与 `dorm_asset_increment.sql` 一致。未执行 DDL、DML 或数据迁移，现进入既有实现栈审计。
- 栈审计完成：已发现并界定管理员鉴权缺口。将以既有 Redis Token、拦截器、`UserContext` 与 `@OperateLog` 最小扩展管理员会话；不会把普通用户 Token 视为管理员，也不会新增任何表或独立日志体系。
## 2026-07-17 校园消费服务专项审计（完成，未开始修复）

- 范围：商铺、商品、购物车、订单、优惠券、校园配送与店铺/商品图片；只读检查文档、SQL、前后端代码和测试源码。
- 新增报告：`docs/campus-consumption-audit.md`。报告将商铺/商品列为“已实现但未验证”，购物车列为“已完成并验证（引用已有测试和交接记录，本轮未重跑）”，订单/优惠券列为“部分实现”，校园配送列为“数据库或接口阻塞”，图片列为“部分实现”。
- 核对结果：订单、优惠券、配送和 `/api/admin/**` 没有 Controller/Service；订单/优惠券/后台页面为占位；购物车真实接入现有 API 并具备库存、状态、商铺、归属和当前价格读取校验。
- 图片结论：`qh_shop.cover_image` 与 `qh_goods.cover_image` 均已足够保存一张主图 URL，不需要为单图迁移；后续应复用唯一 `AliyunOSSOperator`、同一 Bucket 和 `qh_operate_log`，采用 `shops/`、`goods/` 前缀的受限旧图清理。
- 未运行 Maven、前端构建、HTTP 或集成测试：用户要求不执行 SQL，集成测试会写入数据库/Redis；因此本轮无新的运行验证结果，也未连接真实数据库实例。
- 修改文件：`task_plan.md`、`findings.md`、`progress.md`、`docs/HANDOFF.md`、`docs/campus-consumption-audit.md`。未修改 Java、Vue、SQL、配置或测试代码。
- 后端编译预检首次尝试未形成可确认结果：临时 `Q:` 映射在当前目录仍位于该盘符时被删除，出现访问拒绝错误。未把这次当作构建成功；已记录并切换到不重复失败方式。
- 指定 `Q:\\backend` 的完整 Maven 测试已尝试但未执行测试：临时 PSDrive 映射下 testCompile 无法解析主类包（主代码已编译），这是路径/classpath 失败。下一步改用真实后端路径核验；该次不计为功能测试失败或成功。
## 2026-07-17 管理端认证与权限门禁：阶段 1 审计进行中

- 已按项目规则及 `planning-with-files` 读取 AGENTS、项目规范、计划、交接、进度和发现记录；复用既有四阶段管理员认证计划，不进入店铺、商品、订单或图片上传范围。
- 静态审计确认已有未完成的管理员认证骨架：`AdminAuthServiceImpl` 已使用 BCrypt、随机 Token、`qh:admin:token:{token}` 及 30 分钟 TTL；但 `AdminAuthInterceptor` 将管理员写入 `UserContext`，并将普通用户 Token 处理为 403，均不符合本轮隔离和 401 要求；服务还缺少当前管理员与退出接口。
- 前端当前将 `/admin/login` 复用普通 `LoginView`，管理守卫只检查普通用户 Token；后续改为独立管理员会话存储与守卫，并继续复用唯一 `http.js`。
- 尚未修改业务代码、执行 SQL、控制服务或运行 Git；下一步为只读核验实际 `qh_admin` 列和索引。

- 只读数据库门禁已通过：`qh_admin` 实际具备非空 `username`、`password_hash varchar(255)`、`display_name`、`status tinyint NOT NULL DEFAULT 1`、时间列和唯一 `uk_qh_admin_username(username)`；不缺 BCrypt 密码字段或状态字段，无需生成 SQL。聚合结果为 1 个管理员、0 个有效 BCrypt 哈希、1 个启用状态、0 个空状态。存量管理员数据不在本轮被修改。
- 阶段 1 已完成；阶段 2（后端管理员会话与门禁）开始，保持本轮范围仅限认证和访问控制。

- 阶段 2 设计已确定：管理员由独立 `AdminContext` 管理，`/api/admin/**` 从普通用户 `LoginInterceptor` 排除，由管理员拦截器独占并统一返回 401；唯一操作日志切面将在无普通用户时读取 `AdminContext` 作为操作人。前端将以独立管理员会话取代普通用户 Token 复用，但继续使用唯一 Axios 实例。
- 前端首个组合补丁未因编码差异而匹配，未写入任何文件；已切换为逐文件安全替换策略。
- 首次 `Q:` Maven 验证未实际进入 `Q:\backend`：受限 PowerShell 中新建的临时盘符不可见，`Push-Location` 失败后 Maven 在仓库根目录报无 POM。该命令未编译或执行任何测试，不计为测试结果；后续改用同一 `cmd.exe` 会话管理临时映射。
- 第二次 `cmd.exe` 映射尝试因调用层双引号转义被作为路径字符传入而在 `subst` 前置阶段失败；未执行 Maven。下一次改为不含空格的未加引号绝对路径参数。
- 实际 `backend` 目录的 `mvn clean test` 已执行但失败于 `testCompile`：134 个主源文件编译成功，随后 12 个测试源文件在 Windows 长路径下无法解析已编译 `com.qinghe.life.*` 主包，0 个测试方法运行。此为既有 classpath 环境问题，不是本轮测试断言失败；需继续记录指定打包和主代码回退验证结果。
- 本里程碑完成：受控环境 `mvn clean test` 为 34/0/0/0；按 `Q:\\backend` 执行 `mvn clean package -DskipTests` 成功并生成 JAR；按指定 `D:/develop/NodeJS/npm.cmd run build` 成功（1699 modules）。前端仅有既有依赖 PURE 注释与大 chunk 警告，无构建失败。
## 2026-07-17 管理端认证与权限门禁：本轮结束

- 已复用 `qh_admin`，未创建管理员表、日志表、JWT 或管理员注册入口；只读门禁确认字段和唯一索引齐全。现存 1 条管理员记录不是有效 BCrypt 格式，未擅自修改；自动化测试已改为临时插入、精确清理 BCrypt 管理员。
- 后端已完成独立 `AdminContext`、`qh:admin:token:{token}` Redis Hash（30 分钟）、BCrypt 登录、`GET /api/admin/auth/me`、`POST /api/admin/auth/logout` 和 `/api/admin/**` 管理员拦截。普通用户 Token、缺失/随机/过期 Token 均为 HTTP 401；普通用户 `UserContext` 与管理员上下文在请求完成后分别清理。`LoginInterceptor` 已排除 `/api/admin/**`，`@OperateLog` 继续使用唯一 `qh_operate_log` 并在无普通用户时记录管理员操作人。
- 前端已新增独立 `/admin/login`、`admin-session`、Pinia 管理员 store、基础后台布局和路由守卫；唯一 `http.js` 依据 `/admin/` 请求使用管理员 Token，401 只清理对应会话。
- 验证：`mvn clean test` 与 `mvn clean package -DskipTests` 在实际长路径下均于 testCompile 阶段失败（主代码 134 文件成功编译，0 测试方法运行）；`mvn clean package '-Dmaven.test.skip=true'` 成功生成 JAR；`D:/develop/NodeJS/npm.cmd run build` 在授权重跑后成功（1704 modules）。未启动/停止服务、未执行 SQL、未执行 Git。
- 本轮新增文件：`backend/src/main/java/com/qinghe/life/utils/AdminContext.java`、`backend/src/main/java/com/qinghe/life/vo/AdminInfoVO.java`、`backend/src/test/java/com/qinghe/life/AdminAuthenticationIntegrationTest.java`、`frontend/src/utils/admin-session.js`、`frontend/src/api/admin-auth.js`、`frontend/src/stores/admin.js`、`frontend/src/views/AdminLoginView.vue`。修改文件：管理员认证服务/控制器/拦截器/Web 配置/AOP、管理员与普通用户前端会话、路由、后台布局、既有管理员回归测试，以及 API、接口契约、页面设计、计划、发现和交接记录。
- 最终静态核对已确认新增认证文件与全部必需文档存在；`/api/admin/**` 的独立 Token、独立上下文、独立前端守卫和 401 约束均有源码与文档落点。
## 2026-07-17 学生资料与宿舍扫码入住

- 已读取用户指定的交接、计划和设计资料，并完成只读数据库门禁。结构支持有效入住唯一性；现进入既有认证、资产和前端用户链路审计。
- 本轮停止于结构/模型审计：有效入住唯一约束已满足，但 `qh_student_profile` 缺少实名与联系电话字段，无法按要求实现资料持久化、权限和版本历史。未写业务代码、未执行 SQL、未启动服务或执行 Git；等待用户决定是否允许生成并手工执行增量迁移后再继续。
## 2026-07-17 店铺后台维护与封面 OSS：结构审计进行中

- 已读取项目规则、交接、进度、发现、消费审计、数据库设计、接口契约和页面设计，并建立本轮四阶段计划；本轮范围严格限于店铺资料、单封面、后台页面和用户端同步。
- 静态基线显示 `qh_shop` 有 `category_id`、`name`、`address`、`phone`、`score`、`status`、`is_featured`、`cover_image`、`sort_order` 及分类/推荐索引。它没有简介、营业时间或独立营业状态字段，后续不会伪造这些数据。
- 已确认已有 Shop 实体/Mapper/Service/Controller、公开详情 Cache Aside Key、Aliyun OSS 组件、AdminContext、管理员拦截器、唯一操作日志和用户端商铺列表/详情页；尚未修改业务代码、执行 SQL 或调用 OSS。
- 只读数据库门禁通过：`qh_shop` 实际字段、索引、可空 `cover_image varchar(255)` 与基线一致；现有 2 条店铺均有封面且启用，必填字段异常数为 0。无需生成 SQL，阶段 2 已开始。

## 2026-07-17 店铺后台维护与封面 OSS：实现启动

- 已复核真实库：`qh_shop` 有 12 个业务/时间字段，封面使用可空 `cover_image varchar(255)`；索引只有主键、`idx_qh_shop_category(category_id)` 与 `idx_qh_shop_featured(is_featured,status,sort_order)`。`qh_category` 具备名称、状态和排序字段；当前 2 条店铺均有封面。结构门禁通过，不生成、不执行 SQL。
- 已开始在既有 Shop、管理员认证、Cache Aside、Aliyun OSS 与前端管理员布局中定位复用点；不新增独立 HTTP、认证、缓存或 OSS 客户端。
- 后端已实现管理员店铺 Controller、请求/响应模型、现有 ShopService 扩展与受限 OSS 旧图识别；`mvn -DskipTests compile` 成功，编译 139 个主源码。新增店铺封面 Mock OSS 集成/单元测试待在短路径环境运行。
- 首次在实际中文路径执行 `mvn -Dtest=ShopCoverServiceTest test` 失败于 `testCompile`：15 个测试源码均无法解析已编译的 `com.qinghe.life.*` 主包，0 个测试方法运行。这与既知 Windows 长路径 classpath 问题一致，不计作测试通过或断言失败；后续验证改用单一 `cmd.exe` 会话的短路径映射，不重复该命令。

## 2026-07-17 店铺后台维护与封面 OSS：本轮结束（已完成并验证）

- 已实现管理员店铺分页/详情/新增/编辑/启停/封面接口，字段仅覆盖真实 `qh_shop` 列；无物理删除、无 SQL、无图片表、无日志表。所有写操作复用 `AdminContext` 与唯一 `qh_operate_log`。
- 已实现受限 OSS 流程：输入校验、shops Key、数据库失败补偿、新图保存后精确缓存失效、成功后受限旧图清理；测试 Mock `AliyunOSSOperator`，没有真实上传。
- 已实现管理页、左侧菜单与首页入口；复用现有管理员 Store、Router、`http.js`，选择封面后仅本地裁剪/压缩/预览，保存时上传一次；上传失败保留表单资料。用户端既有公开列表/详情读取更新后的 `coverImage`。
- 验证完成：真实字段/索引只读门禁通过；`Q:\backend` 的 `mvn clean test` 为 40/0/0/0；`mvn clean package -DskipTests` 成功生成 JAR；真实前端路径构建成功（1707 modules）。`ADMIN_SHOP_TEST_` 店铺、分类、管理员和操作日志残留均为 0。未启动/停止服务、未执行 SQL/Git 或真实 OSS。

## 2026-07-17 商品后台维护与主图 OSS：结构审计进行中

- 已读取规则、交接、计划、进度、发现和指定设计文档；本轮范围仅限商品后台资料、单张主图、后台页面和用户端同步。
- 真实 `qh_goods` 门禁通过：存在 `shop_id`、`name`、可空 `description`、`price decimal(10,2)`、`stock int`、`sales_count`、`sale_status varchar(16)`、可空 `cover_image varchar(255)` 与时间列；索引为主键和 `idx_qh_goods_shop(shop_id)`。当前 2 条商品无名称/店铺/价格/库存/状态/主图缺失。
- `qh_goods` 不含独立商品分类和排序字段；分类可通过关联店铺的 `category_id` 筛选，后台不伪造可编辑排序。订单前置所需价格、库存、状态、店铺关联和主图字段均存在，因此不生成、不执行 SQL，阶段 2 开始。
# 商品后台维护与主图 OSS：续作（2026-07-17）

- 已按本轮要求恢复 `AGENTS.md`、交接、进度、发现、校园消费审计、数据库/API/页面设计与现有计划上下文。
- 已重新完成 `qh_goods` 实时只读数据库门禁：价格、库存、状态、店铺关联和单张主图字段齐全；分类经店铺关联，商品表无排序字段；无需生成或执行迁移 SQL。
- 已定位商品后台已有的接口骨架；发现其实现类缺失，并记录公开商品未排除停用店铺的同步缺口。一次店铺封面服务路径假设失败，未重试相同路径。

## 2026-07-17 学生资料与宿舍扫码入住：只读门禁阻断

- 只读连接已确认数据库为 `qinghe_life`。用户手工迁移新增的 `real_name varchar(50) NOT NULL` 与 `contact_phone varchar(20) NOT NULL` 已在 `qh_student_profile` 生效。
- 但表实际使用 `college_name`、`major_name`、`student_status` 与 `current_flag`，不含本轮硬性门禁所列的 `college`、`major`、`academic_status` 与 `active_flag`；其中状态和版本标记名不一致，故立即停止开发。
- 已核验 `qh_student_profile` 当前有效资料的两条复合唯一索引，以及 `qh_dorm_checkin` 的用户/床位有效入住复合唯一索引和到用户、床位、资产套装的外键均正确。未执行 DDL/DML 或 SQL 导入，未修改业务代码、未运行测试/打包/前端构建。
- 记录一次无效计划补丁尝试：预设首行上下文不存在，补丁未应用、未改动文件；已改用末尾精确上下文记录本次门禁。

## 2026-07-17 学生资料与宿舍扫码入住：真实字段契约恢复与门禁复核

- 用户已明确 `college_name`、`major_name`、`student_status`、`current_flag` 是唯一数据库契约；Java 与 API 将使用 `collegeName`、`majorName`、`studentStatus`、`currentFlag`，不新增任何概念别名列或修改数据库。
- 已只读复核真实 `qinghe_life`：学生资料字段类型为 `real_name varchar(50)`、`student_no varchar(32)`、`college_name/major_name/class_name varchar(100)`、`contact_phone varchar(20)`、`student_status varchar(16)`、`current_flag tinyint default 1`；资料表当前无数据，因此尚无可观察的状态或历史标记样本。
- 已复核当前资料复合唯一索引，以及有效入住的 `(user_id, active_flag)`、`(dorm_bed_id, active_flag)` 复合唯一索引和到用户、床位、资产套装的外键。迁移 DDL 的既有规则为当前标记 `1`、历史标记 `NULL`，学生在读状态为 `ENROLLED`；未执行 DDL/DML、SQL 导入、服务控制、Git、测试或构建。

## 2026-07-17 学生资料与宿舍扫码入住：实现设计收敛

- `qh_student_profile` 的非空 `campus_id` 不在学生填写字段中。当前目录只有一条启用校区，服务端将在首次建档时仅在启用校区恰好一条时安全推导该 ID；空目录或多个启用校区会返回明确错误，不接收客户端 `campusId`。
- `qh_dorm_bed.status` 的真实语义是目录启停，既有数据库设计明确禁止用入住覆盖它；有效 `qh_dorm_checkin` 是床位占用事实。确认入住将创建有效入住并将资产套装从 `AVAILABLE` 同步为 `OCCUPIED`，不把床位目录停用。

## 2026-07-17 学生资料与宿舍扫码入住：实现与验证状态

- 已新增学生资料、二维码解析、确认入住和我的宿舍的后端分层与 `/dorm/scan`、`/dorm/me` 页面；接口和 AOP 日志不记录完整姓名、学号、联系电话或二维码内容。
- `mvn clean test` 编译 159 个主源码后在既有 Windows 中文路径 testCompile 问题处失败，17 个测试源码均未编译，实际执行测试数为 0。`mvn clean package -Dmaven.test.skip=true` 已生成 JAR；真实前端路径生产构建成功（1716 modules）。
# 只读门禁结果（2026-07-17）

- 已通过：`qh_category` 为全局店铺类型目录；`qh_goods_category` 有同店名称唯一与 `(shop_id,status,sort_order)` 导航索引；`qh_goods.category_id` 可为 NULL，且商品分类和分类店铺外键均存在。未执行 DDL、DML 或 SQL 导入。

# 2026-07-17 店内商品分类与商店主页优化：继续实施

- 已恢复本轮活动计划；用户确认 `goods_category_increment.sql` 已由 DataGrip 手工执行并复核。本轮只进行只读门禁、代码、测试、构建和文档同步，不执行 SQL、不改数据库结构、不控制服务或执行 Git。
- 下一步：核对真实分类模型与现有后端、前端、购物车和图片处理链路，再以小批次补齐本轮唯一里程碑。
# 店内商品分类与商店主页优化：完成（2026-07-17）

- 完成店内分类管理、商品同店分类校验、筛选语义修正、商店详情紧凑布局、商品主图完整展示和固定购物车入口；未实现订单、优惠券、支付或配送。
- 验证：只读数据库门禁通过；`mvn clean test` 为 47/0/0/0；`mvn clean package -DskipTests` 成功；真实前端路径 `D:/develop/NodeJS/npm.cmd run build` 成功。未启动或停止服务，未执行 SQL、Git 或真实 OSS 上传。

# 订单、普通优惠券、Redis 缓存与限时秒杀：架构审计与迁移准备（2026-07-17）

- 已完成：静态审计 `qh_cart`、`qh_order`、`qh_order_item`、`qh_goods`、`qh_shop`、`qh_user_address`、`qh_coupon`、`qh_user_coupon`，以及现有实体、Mapper、购物车链路、Redis 配置/Key、`UserContext`、`AdminContext` 和唯一 `qh_operate_log`。
- 已创建：`docs/order-coupon-redis-design.md`；`backend/src/main/resources/sql/order_core_increment.sql`、`coupon_core_increment.sql`、`seckill_coupon_increment.sql`。脚本均未执行，只含允许的建表/加列/加索引/外键语句。
- 已更新：`docs/database-design.md`、`docs/api-contract.md`、`docs/campus-consumption-audit.md`、`docs/HANDOFF.md`、`findings.md`、`task_plan.md`。
- 验证：按本轮限制未执行 SQL、未连接数据库/Redis、未启动或停止服务，未运行测试、构建或 Git。仅以静态文件核对 SQL 关键字、命名和职责边界；运行时验证待后续阶段。
# 2026-07-17 管理员入住记录、退宿与换寝：阶段 1 进行中

- 已读取规则、交接、规划、进度、发现和宿舍设计文档，建立本轮唯一里程碑与停止边界；未执行 SQL/DDL/DML、未修改业务代码、未启动或停止服务、未执行 Git。
- 已完成静态定位：入住实体和迁移设计已有历史关闭、退宿原因、换寝前序关联、管理员办理人、位置/学籍快照、有效入住唯一约束与资产套装状态基线。下一步仅对真实 `qinghe_life` 执行只读门禁并审计现有宿舍/管理员代码。
- 首次真实 MySQL 只读联合元数据查询失败；输出因凭据保护被过度脱敏，尚未得到错误类别。未执行 DDL、DML 或 SQL 导入，已按三次错误协议改为最小只读连通性诊断。
- 最小只读连通性诊断最初显示 MySQL `ERROR 1045`；安全诊断确认配置密码是 Spring `${MYSQL_PASSWORD:...}` 占位符，命令行曾把它原样传入。将按 Spring 等价规则解析该占位符后只重试一次真实门禁查询；期间未执行任何写操作。
- 已按 Spring 等价规则成功执行真实数据库只读查询：入住表的关闭字段、换寝前序关联、操作管理员、快照、可空有效标记、两项有效入住唯一索引和六项外键均存在。当前入住、床位与资产套装表没有状态分组数据，仍需对资产套装/床位字段元数据完成最后门禁；未执行 DDL/DML。
- 已审计现有代码：`DormAssetAdminController` 是唯一管理员宿舍基础入口，`StudentDormServiceImpl` 已采用有效入住唯一约束与资产套装 `AVAILABLE`/`OCCUPIED` 同步。管理员入住管理尚未实现，后续只能复用这套控制器前缀、管理员门禁、HTTP/Store/Router 与 `qh_operate_log`。
- 阶段 1 已完成：实时库只读确认十一张指定表存在；`qh_asset_set.status` 默认 `AVAILABLE`、`qh_dorm_bed.status` 默认 `1`，加上入住表的历史关闭、前序关联、有效唯一索引与六项外键，可安全实现单个退宿和换寝历史，不需生成 SQL。阶段 2 已开始。
- 后端实现已新增管理员入住查询、详情、退宿、换寝和可用目标床位服务/接口，均位于既有管理员宿舍控制器前缀。首次构建受 Maven 本地仓库权限阻断；受控重跑又因 PowerShell 未引用 `Q:\.m2` 参数而在 Maven 参数解析阶段失败，尚未进入 Java 编译，下一次将采用等价的单引号参数。
- 后端主代码已在工作区本地 Maven 缓存下编译通过（177 个源文件）。前端生产构建已通过（1720 modules）；第三方 PURE 注释和大 chunk 仅为非阻断警告。尚未补齐或运行用户要求的管理员入住专项自动化测试、完整 Maven 回归、打包命令，以及指定 API/设计/交接文档更新；本轮不得标记完成。
# 2026-07-18 普通订单创建链路：阶段 1 开始

- 已读取用户批准的金额语义与完整事务、并发、页面和测试边界：`qh_order.total_amount` 是商品小计，`pay_amount` 是最终应付金额；本轮不得新增或查询 `goods_amount`。
- 已恢复既有计划、进度、发现和交接上下文。此前订单实现因把设计列名当作真实列名而阻断；本轮将改以真实字段、索引、必要关联与 `COUNT(*)` 做只读门禁，单项差异不会中止其他元数据检查。
- 当前仅在阶段 1 做只读审计，尚未修改业务代码、SQL、数据库或运行服务，尚未执行测试或构建。
- 真实 MySQL 只读门禁已连通 `qinghe_life`：`qh_order` 具备主键、唯一 `order_no`、用户/店铺查询索引、完整订单地址快照列及 `total_amount`、`discount_amount`、`delivery_fee`、`pay_amount` 四个 `DECIMAL(10,2)` 金额列；`qh_order_item` 具备主键、`order_id` 索引、全部商品快照列及 `goods_price`/`subtotal` 的 `DECIMAL(10,2)`。
- 按要求以真实 `COUNT(*)`（未使用 `information_schema.table_rows`）得到 `qh_order=0`、`qh_order_item=0`。门禁还确认 `qh_cart`、`qh_goods`、`qh_shop`、`qh_user_address` 的主键和订单所需主要字段存在；发现待静态/等价字段确认项：地址表未见 `address_area`，商品表未见字面 `status`，后续将核对真实等价列与现有实体映射，不会因命名不同停止。
- 真实等价字段已确认：`qh_user_address.area` 是地址区域来源，`qh_goods.sale_status` 是商品可售状态，现有 `UserAddress.area`、`Goods.saleStatus` 实体映射与真实库一致。订单实体现仅有旧 `deliveryAddress` 与三项金额字段，尚缺结构化快照和 `deliveryFee`，可在本轮真实订单列上补齐；未发现 `double` 金额字段。
- 一次只读字段盘点命令在查询成功并返回完整字段后，因 MySQL 的命令行密码安全警告被 PowerShell 以原生 stderr 错误形式退出；后续只读查询将通过不把该警告当作业务失败的执行方式处理，不重复相同命令结构。
- 已确认现有购物车、地址、认证与操作日志可复用：购物车服务按当前用户隔离并使用 `sale_status='ON_SALE'`、店铺 `status=1`；地址服务保留校园区域/楼栋结构化数据；日志切面默认会掩码 Token 与手机号。订单响应将避免回传完整电话/地址，不能将可变地址展示字段作为订单历史快照。
- 已实现后端普通订单创建：新增受限请求 DTO、订单服务与控制器，订单实体补齐真实快照/配送费映射，库存使用 `qh_goods` 条件更新，所有主表、明细、扣库存与精确购物车删除位于同一事务。前端已新增订单 API、`/checkout` 与 `/orders/{orderId}/success`，并把购物车同店选中项导航到结算。
- 尚未执行编译、自动化测试或前端构建；下一步先运行用户指定 Maven 全量测试以发现编译和回归问题，再补齐必要的订单专项测试与文档。
- 后端 `mvn -Dmaven.repo.local=./.m2-order-test -DskipTests compile` 成功。用户指定的 `Q:\.m2` 在本会话不可写；改用项目内缓存后全量 `clean test` 因首次依赖解析超过 124 秒超时，未实际取得 tests/failures/errors/skipped，不能视为测试通过。
- 前端 `D:/develop/NodeJS/npm.cmd run build` 成功（1725 modules）；保留既有 `@vueuse/core` PURE 注释与大 chunk 非阻断警告。数据库、API 与页面设计文档已新增 `total_amount` 到 `totalAmount` 的一致契约说明。
# 2026-07-18 管理员入住专项测试与交付收口

- 已恢复既有计划、交接、进度与发现，并按 `planning-with-files` 建立本轮单一里程碑：M1 代码与测试基线审查。
- 已确认任务限制：仅当前仓库；不执行 SQL、服务控制或 Git；不改数据库结构；不扩展学籍异动、批量操作、统计或资产报修。
- 待执行：逐文件审查管理员入住实现和现有测试，随后仅补齐测试、运行完整验证、检查测试数据残留并更新交付文档。
# 2026-07-18 管理员入住模块自动化测试与交付收口

- 已恢复 `task_plan.md`、`progress.md`、`findings.md` 和交接文档；本轮仅处理管理员入住查询、退宿与换寝测试及交付验证。
- 已从既有交接记录确认：业务逻辑位于 `DormAssetAdminController` / `AdminDormCheckinServiceImpl`，管理员身份取自 `AdminContext`；床位 `status` 不得承载入住状态，入住历史不可删除，资产沿用 `AVAILABLE/OCCUPIED`。
- 尚未执行 SQL、迁移、服务启停、Git 或构建命令。

- 代码审查后仅修复三项明确边界：查询、详情、可用目标床位服务均显式从 `AdminContext` 校验管理员；管理端 VO 不再返回 `previousCheckinId`；操作日志掩码办理原因，避免自由文本带入完整个人信息。
- `AdminDormCheckinIntegrationTest` 使用唯一短前缀隔离每次运行，并只删除本次记录的 Redis Key；新增退宿后学生 `/dorm/me` 正常空状态、实名资料保留及原二维码床位可重新入住断言。
- 专项 `mvn -Dmaven.repo.local=Q:\.m2 -Dtest=AdminDormCheckinIntegrationTest test` 已通过：7 tests、0 failures、0 errors、0 skipped。执行时以临时 `subst Q:` 映射当前仓库并在命令结束移除；没有执行 SQL、服务启停或 Git。

- 完整 `clean test` 的 Surefire XML 已汇总 20 个套件、68 tests、0 failures、0 errors、0 skipped；外层命令采集在 64 秒时结束，但测试子进程已完成且全部最终 XML 均为通过。
- `mvn -Dmaven.repo.local=Q:\.m2 clean package -DskipTests` BUILD SUCCESS，编译 182 个主源码与 21 个测试源码并生成 `qinghe-life-backend-1.0.0.jar`；前端 `D:/develop/NodeJS/npm.cmd run build` BUILD SUCCESS，1725 modules。第三方 PURE 注释和大 chunk 仅为非阻断警告。
- 指定文档均已同步，管理员入住自动化测试与交付收口完成；按用户指示在此停止，不进入其他功能。

# 2026-07-18 普通订单创建链路：自动化测试与交付收口（阻断）

- 环境：Maven 3.9.11、JDK 21.0.9；本会话初始不存在 `Q:`，以单命令临时 `subst Q:` 映射运行 `Q:\backend`。PowerShell 虚拟驱动不能被 Maven/JVM 识别；`dependency:go-offline` 在真实 `Q:\.m2` 依赖准备阶段超时 64 秒，缓存保留。该超时不是测试通过或失败结论。
- 订单范围修复：`OrderServiceImpl` 增加购物车 `shopId` 与商品当前店铺的一致性校验，并确认地址楼栋存在、启用且属于地址校区；`OrderCreateDTO` 明确忽略前端伪造未知金额/库存字段；成功页改为只消费本次服务端响应的路由 state，不再从 URL 查询参数读取金额。
- 已新增 `OrderCreateIntegrationTest`，使用 `ORDER_CREATE_TEST_`、精确用户/商品/地址/订单/明细/购物车/操作日志与 Redis Key 清理，覆盖鉴权、输入、归属、商品/店铺、BigDecimal 金额、快照、并发最后库存、事务写入失败和精确购物车清理。
- 专项测试四次实际运行均完成主源码和测试源码编译并进入 5 项测试；夹具先后暴露购物车唯一键、地址外键和 `building_code` 长度问题，均仅在测试夹具中修复。第五次专项重跑在测试启动前被范围外编译错误阻断：`AdminDormCheckinServiceImpl` 调用不存在的 `AdminDormCheckinVO.setPreviousCheckinId(Long)`。
- 未执行完整 `clean test`、后端 `clean package -DskipTests` 或本轮最终前端构建；未取得本轮 tests/failures/errors/skipped、零残留或构建成功证据，普通订单创建不得标记完成。
# 2026-07-18 宿舍编译基线与订单创建验证收口

- 用户授权的唯一里程碑：修复 `AdminDormCheckinServiceImpl` 对缺失 `AdminDormCheckinVO.setPreviousCheckinId(Long)` 的编译阻塞，之后完成既有普通订单创建测试、回归、构建和指定文档收口；不新增业务、不改库、不手工执行 SQL、不控制服务、不使用 Git。
- 已恢复 `task_plan.md`、`progress.md`、`findings.md`、`docs/HANDOFF.md`、`dorm_asset_plan.md` 与订单设计上下文；当前处于“宿舍字段契约与主源码编译修复”阶段。

## 2026-07-18 管理员宿舍资源管理修复

- 已启动本轮唯一里程碑并恢复 `AGENTS.md`、交接、计划、进度、发现、宿舍资产设计、数据库/API/页面设计上下文。
- 当前处于“真实结构门禁”阶段：先只读确认宿舍楼类型、宿舍/床位/资产套装/资产/入住表的字段、状态、索引、外键和数据基线；尚未修改代码、SQL、数据库、服务或测试。

- 只读门禁通过：`qh_building.building_type` 实际值包含 `宿舍楼`、`教学楼`、`图书馆`，其中两栋宿舍楼均启用；寝室/床位/资产套装/资产/入住表的字段、状态、唯一索引和外键与本轮契约一致。
- 数据基线：寝室 1、床位 2、资产套装 1、资产 5、入住历史 0、当前入住 0；误录寝室 `UATA` 的 ID 为 251，当前无入住历史。未执行 DDL/DML、迁移或数据修正。

- 已完成现有控制器、服务、实体、DTO/VO、前端 API/页面和既有宿舍测试审查。确认只需扩展既有 `DormAssetAdminController`、`DormAssetServiceImpl`、`adminDorm.js` 与 `AdminDormAssetView.vue`，不会创建平行栈。

- 已完成后端资源维护和前端三级页面的首轮实现；临时 `Q:` 映射下 `mvn -Dmaven.repo.local=Q:\.m2 -DskipTests compile` 成功，编译 191 个主源码文件。尚未运行本轮专项测试、完整回归、打包或前端构建。

- `AdminDormResourceIntegrationTest` 已编译并启动，但 Redis `192.168.100.128:6379` 不可连接，管理员登录三次均返回 503；专项结果为 3 failures、0 errors，未重试或控制 Redis。
- `mvn -Dmaven.repo.local=Q:\.m2 clean package -DskipTests` 成功（191 主源码、23 测试源码、JAR）；前端 `D:/develop/NodeJS/npm.cmd run build` 成功（1725 modules，只有第三方 PURE 注释与大 chunk 警告）。
- 只读精确残留检查：`ADMIN_DORM_RESOURCE_TEST_` 的 buildings/rooms/beds/sets/assets/checkins/admins 均为 0。完整测试 0 failures/0 errors 未达成，本里程碑保持 blocked，等待 Redis 恢复后复验。
# 2026-07-18 宿舍模块 MyBatis-Plus 有限整改

- 已开始本轮唯一里程碑；已恢复 `AGENTS.md`、项目规范、既有规划与宿舍历史上下文。
- 边界已锁定：不执行手工 SQL、不改库、不改并发/事务/接口；先审计后最小整改。
- 当前阶段：1（静态审计）。
## 2026-07-18 宿舍模块 MyBatis-Plus 有限整改：阶段 1-2 完成

- 已完成 Entity、Mapper、Service、XML、Controller 与测试清点。
- 结论：宿舍 Mapper 已全部是空 `BaseMapper<T>`；不存在 Mapper XML 和可删的重复单表自定义 SQL，因此未修改业务代码或映射文件。
- 已记录 4 处服务层 N+1 风险；本轮不为“使用 MyBatis-Plus”将复杂关联拆为循环查询。
- 下一步：静态复审、审阅宿舍测试清理断言后运行专项测试。
## 2026-07-18 宿舍模块 MyBatis-Plus 有限整改：静态复审完成，验证阻断

- 静态复审通过：Controller 未直接使用 Mapper；Entity、DTO、VO 的分离未改变。
- 不作代码改动：没有可迁移的自定义单表 CRUD，也没有 Mapper XML 可删除。
- 数据残留静态证据：5 个宿舍测试类共 26 个测试；其中管理员入住和学生宿舍测试含最终零残留断言。
- 验证未运行：`Q:\backend` 不存在，未擅自创建盘符映射或改用其他路径。故专项测试、完整回归、后端打包和运行后残留检查均为 blocked，不能标记完成。

## 2026-07-18 宿舍二维码完整交互链路修复：阶段 1 启动

- 已按要求读取项目规则、交接、计划、进度、发现、宿舍资产计划和二维码相关接口/页面设计文档；已建立本轮唯一里程碑，尚未修改业务代码、数据库或服务状态。
- 既有交接表明学生 `POST /api/student/dorm/qr/resolve` 及入住事务、资产二维码动态 PNG/ZIP、学生宿舍集成测试均存在；本轮仅修复客户端识别/反馈、管理员查看与必要的规则/测试收口。
- 下一步：按文件和符号审计 `DormScanView.vue`、`AdminDormAssetView.vue`、`adminDorm.js`、学生/管理员 Controller 与 Service、二维码依赖及现有测试，确定最小修复方案。

## 2026-07-18 宿舍二维码完整交互链路修复：阶段 1 审计进展

- 已确认图片/摄像头失败共同根因是前端只依赖原生 `BarcodeDetector`、未安装任何二维码解码依赖；摄像头 API 可用时仍会因该 API 缺失停止在降级文案，且流未立即释放。
- 已确认后端 QR 生成与学生解析不需改 Token 格式或入住事务：生成使用 ZXing，解析响应没有 Token，现有 `DormAssetHealth` 已覆盖固定五件资产与非 NORMAL 状态。
- 已确认管理员“查看二维码”未实现：按钮只进入资产详情；待在既有管理员 PNG API 基础上实现弹窗预览、下载与 URL 回收。

## 2026-07-20 学生首次建档多校区与 Redis 测试分层：结构门禁通过

- 已建立本轮唯一里程碑并完成只读数据库门禁：本机 `qinghe_life.qh_student_profile` 的 `campus_id bigint NOT NULL` 存在且已索引；未执行 SQL。
- 已定位首次建档单校区限制位于 `StudentDormServiceImpl.saveProfile()` 的 `singleEnabledCampus()` 推断。实体已支持 `campusId`，因此可在不变更数据库结构且不触碰入住、退宿、换寝事务的前提下实施最小修复。
- 下一步：复核校区目录服务、安全响应、学生页面与现有 Redis 测试，再补齐请求、校验、响应、页面、测试和指定文档。
- 读取错误：首次使用错误的 `studentDorm.js` 路径，真实文件为 `student-dorm.js`；未产生文件改动。

## 2026-07-20 学生首次建档多校区与 Redis 测试分层：交付收口

- 已完成多校区首次建档修复：`campusId` 只在首次建档使用，服务端读取真实启用校区；安全资料响应增加 `campusId/campusName`，学生后续保存仍只更新联系电话。未修改数据库、Redis 配置、扫码入住、退宿或换寝事务。
- 已更新 `StudentDormIntegrationTest`，覆盖多启用校区、停用校区拒绝和伪造 `campusName` 不影响服务端结果；该测试依赖真实 Redis，会留给 Windows 本机执行。
- Redis 分层文档已同步：单元测试可 Mock Redis 依赖，但管理员登录、Token/验证码/会话状态和学生宿舍会话验证必须使用真实 Redis。Codex 沙箱无法访问局域网 Redis 仅为环境限制。
- 后端验证阻断：`mvn -Dmaven.repo.local=Q:\.m2 -DskipTests compile` 和 `mvn -Dmaven.repo.local=Q:\.m2 clean package -DskipTests` 均因 `Q:\.m2` 不可创建/访问而未进入编译或打包。前端 `D:/develop/NodeJS/npm.cmd run build` 成功（1731 modules）；仅有既有 PURE 注释和大 chunk 警告。

## 2026-07-20 管理员宿舍楼按校区管理：只读门禁进展

- 已建立本轮唯一里程碑，完成真实库只读基线：主校区有 2 栋真实宿舍楼，分校区当前没有真实宿舍楼；No Data 必须按当前校区分别判断。
- 已确认后端现有分页接口已按 `campusId/status/keyword` 和真实 `building_type=宿舍楼` 过滤，创建/编辑/启停已有基础；待复核前端请求参数与加载逻辑，并补齐固定类型、条件删除、统计字段和专项测试。
- 当前未执行 SQL、未改代码、未控制服务或 Redis、未执行 Git。

## 2026-07-20 管理员宿舍楼按校区管理：阶段 1 完成

- No Data 排查完成：主校区有 2 条真实宿舍楼记录，分校区为 0；前端默认首个启用校区并正确传递数值 `campusId`，后端参数名和类型过滤也正确。
- 已识别本轮最小修改点：将本模块创建类型固定为真实 `宿舍楼`、禁止通过编辑改变类型/校区，补充聚合统计的床位数与关联历史检查、条件删除端点、前端顶部操作和专项测试。

## 2026-07-20 管理员宿舍楼按校区管理：阶段 2 启动

- 已复核当前源码：端点骨架、管理员门禁和操作日志注解已在既有宿舍栈内；后续以 DTO 输入边界、聚合统计、条件删除原因和专项测试为实际完成标准，不将旧端点误判为已交付。

## 2026-07-20 管理员宿舍楼按校区管理：实现与验证收口（阻断）

- 实现：创建请求删除可变 `buildingType`；按校区列表将 `campusId` 设为必填并校验校区启用；服务端继续固定真实“宿舍楼”类型。楼栋删除 API 接入前端，页面完成新增、编辑、启停、条件删除、统计列、二次确认、未选校区禁用和新建后自动选中。
- 复用：简单 Building CRUD 走 MyBatis-Plus；`BuildingMapper.selectDormBuildingStats` 聚合寝室/床位/资产套装/历史与当前入住统计；控制器不直连 Mapper，写操作继续经 `AdminContext` 和唯一 `qh_operate_log`。
- 验证：`AdminDormBuildingIntegrationTest,AdminDormCheckinIntegrationTest` 为 11 tests、0 failures、0 errors；前端构建成功（1731 modules）；默认 Maven 缓存 `clean package -DskipTests` 成功（204 主源码、23 测试源码、JAR）。指定 `Q:\.m2` 不可创建。
- 完整 `mvn clean test` 实测 76 项、2 failures、0 errors；不满足 0 failures/0 errors，不能标记完成。剩余阻断为既有 `DormAssetIntegrationTest` 对重复寝室 409 的断言，而当前服务/数据库未拒绝重复；不放宽断言，且不在本轮修改寝室业务规则。

- 最终状态复核：当前源码再次执行默认 Maven 缓存 `mvn clean package -DskipTests` 成功，编译 204 个主源码和 23 个测试源码，并生成 `backend/target/qinghe-life-backend-1.0.0.jar`；完整回归阻断结论不变。
# 2026-07-20 个人中心姓名显示语义修复

- 已完成阶段 1 的首轮静态审计：读取用户指定的交接、计划、接口和页面文档，并核对 `User`、`StudentProfile`、`UserDTO`、用户/学生 Controller 与 Service、Store、个人中心和导航实现。
- 已确认本轮不修改数据库、不执行 SQL、不触碰学生建档、入住、退宿、换寝、学籍异动或认证流程。
- 定位过程中两条推断文件路径不存在：`StudentProfileServiceImpl.java`、`StudentProfileController.java`；实际学生资料实现为 `StudentDormServiceImpl.java`、`StudentDormController.java`。另一个推断的前端 `api/student.js` 不存在，实际 API 文件为 `api/student-dorm.js`。这些是路径核对结果，未修改业务代码。
- 已完成用户服务、`UserDTO`、个人中心、用户导航、公共显示名工具和针对性 Mockito 测试修改；已更新 `backend/API.md`、`docs/api-contract.md`、`docs/page-design.md`、`docs/HANDOFF.md` 和 `task_plan.md`。
- 后端指定验证前置检查：`Q:\backend=False`、`Q:\.m2=False`，故未执行 `mvn -Dmaven.repo.local=Q:\.m2 clean test` 或打包命令，也未创建/改动 Q 盘、Maven 或 Redis 配置。待在具备该路径的本机执行。
- 实际工作区的受控专项验证 `mvn -Dtest=UserAvatarServiceTest test` 通过：6 tests、0 failures、0 errors。覆盖当前 `current_flag=1` 学生资料的实名/学号映射、管理员修改实名后下一次读取生效、昵称不被覆盖，以及安全映射不含 `currentFlag/passwordHash/token`。
- 实际工作区完整 `mvn clean test` 已结束：77 tests、1 failure、0 errors、0 skipped。失败为范围外 `DormAssetIntegrationTest.administratorMaintainsUniqueRoomBedAndCreatesIdempotentAssetSetWithPrivateQr`，断言期望 HTTP 409、实际 HTTP 200；本轮没有修改宿舍资产实现或断言。主源码 204 个、测试源码 23 个均编译。
- 用户指定真实路径的前端生产构建已在受控权限下通过：Vite 5.4.21，1732 modules；仅有既有 PURE 注释与大 chunk 警告。`mvn clean package -DskipTests` 因平台拒绝受控执行申请（额度限制）未运行，未使用替代或绕过方式。
# 2026-07-23 GitHub initialization and first push

- User explicitly authorized Git use and provided `https://github.com/tzjk/qinghe.git` as the target remote. The local repository was initialized on `main`, and `origin/main` was fetched before any push.
- The remote contains only its existing `Initial commit` and a one-line README. Local README content will be retained when reconciling histories.
- Added `.m2/` and `backend/.m2-*/` to `.gitignore`. Local Maven caches were removed from the Git index only and remain on disk.
# 2026-07-23 GitHub initialization and first push completed

- `main` was initialized, connected to `https://github.com/tzjk/qinghe.git`, and safely merged with the existing README-only remote history.
- The initial project commit is `b8e5f77`; merge commit `68efe71` is verified on both local `main` and `origin/main`.
- Git ignored `.m2/` and `backend/.m2-*/`; no Maven cache, build output, `node_modules`, or frontend `dist` files were committed.
# 2026-07-23 Git ignore hardening

- Added ignore rules for local environment files and common private-key/certificate formats while retaining `.env.example` templates. Existing build, dependency-cache, and IDE exclusions remain unchanged.
- Scope is limited to repository-ignore configuration and project records; no business code, service, database, build, commit, or push operation was performed.
# 2026-07-23 宿舍管理需求只读复核

- 已按三项原始需求静态核对资产二维码、学生扫码入住、管理员学籍与批量资产操作的后端、前端和测试记录；未修改代码、数据库、配置、服务或 Git。
- 结论：三项主链路均已实现；资产编号唯一性相关的重复寝室 409 集成测试仍是唯一明确未收口风险。详情已记入 `findings.md`，本次仅审计，不执行修复。

## 2026-07-23 重复寝室号 409 最小修复

- 已仅修改 `DormAssetServiceImpl`：创建寝室和改名均在写入前按同一楼栋、同一寝室号查重，冲突返回 HTTP 409；未改表结构、SQL、学生入住、资产生成、二维码或学籍事务。
- `mvn -Dtest=DormAssetIntegrationTest test` 已编译 204 个主源码和 23 个测试源码；运行因沙箱无法连接 `192.168.100.128:6379`，2 项测试均在 Redis 清理阶段报错，未进入重复寝室断言。未改 Redis 配置。
- 已创建本地 Git 快照 `dd7149e fix: enforce dorm room number uniqueness`，仅提交服务修复和本轮规划记录；用户已有 `.gitignore` 修改保持未暂存、未提交。

## 2026-07-23 订单生命周期状态模型与候选迁移

- 已创建 `OrderStatus` 并以其 `PENDING_PAY` 编码替换普通订单创建和订单创建专项测试中的待支付魔法字符串；状态机只定义，不新增支付、取消、管理员、定时或前端功能。
- 已创建候选人工迁移 `backend/src/main/resources/sql/order_lifecycle_schema_increment.sql`，并更新订单状态机、API、接口契约、数据库和订单设计文档。候选脚本未执行，`Order` 实体未添加新列。
- 已确认 `Q:\backend` 与 `Q:\.m2` 均不存在，因此未运行用户指定 Maven compile 与 `OrderCreateIntegrationTest`。未修改 Redis 或 Maven 配置；验证待可访问 Redis 且具备指定路径的本机环境。

## 2026-07-24 订单生命周期核心闭环

- 真实库只读门禁通过；Q: 映射仅用于 Maven 短路径，未执行任何迁移 SQL。
- 后端新增支付期限、用户订单读取、模拟支付、用户取消与精确库存回补、管理员固定流转、超时取消批处理/定时触发；订单创建原有金额、快照、条件扣库存和精确清车逻辑保持不变。
- `mvn -Dmaven.repo.local=Q:\.m2 -Dtest=OrderCreateIntegrationTest,OrderLifecycleIntegrationTest test`：13 tests、0 failures、0 errors、0 skipped（创建 5、生命周期 8）。
- `mvn -Dmaven.repo.local=Q:\.m2 -DskipTests package`：成功，生成 `backend/target/qinghe-life-backend-1.0.0.jar`。
- `D:/develop/NodeJS/npm.cmd run build`：成功，1736 modules；仅既有第三方 PURE 注释与大 chunk 警告。
- 测试前缀用户、店铺、商品、订单、明细、购物车、地址残留计数均为 0。

## 2026-07-24 普通优惠券基础模块：验证与收口

- 真实 `qh_coupon/qh_user_coupon` 的集成测试映射验证通过；未运行迁移 SQL。`coupon_foundation_increment.sql` 已标记为用户人工完成结构后的保留参考。
- 优惠券业务复核：普通领取使用 MySQL 事务和 `available_stock > 0` 条件扣减；用户券状态固定为 `AVAILABLE -> LOCKED -> USED`，主动/超时取消按过期状态回到 `AVAILABLE` 或转为 `EXPIRED`。订单仅接收 `userCouponId`，服务端计算 `discountAmount/payAmount`，且释放与库存恢复同一事务。
- `mvn "-Dtest=OrderCreateIntegrationTest,OrderLifecycleIntegrationTest,OrderTimeoutCancelIntegrationTest,CouponOrderIntegrationTest" test` 实际为 40 tests、0 failures、0 errors、0 skipped；其中 `CouponOrderIntegrationTest` 为 20 项。随后 `mvn -DskipTests package` 成功，生成 `Q:\backend\target\qinghe-life-backend-1.0.0.jar`。
- 首次前端 build 定位到 `AdminCouponView.vue` 的 `el-table-column` 缺失结束标签；只修复该模板标签后，真实 frontend 路径 build 成功（1741 modules）。`COUPON_ORDER_TEST_` 精确只读残留检查的 10 个表/范围均为 0。
# 2026-07-24 Coupon duplicate-claim display and feedback

- Restored project instructions, planning records, and coupon implementation context.
- Audit complete: repeated claims are service-idempotent but have no explicit API state; available-list and client state do not display a claim.
- Started from a clean worktree on `feature/coupon-foundation`; no branch was created, no SQL was executed, and no service was controlled.

## 2026-07-24 Coupon duplicate-claim completion evidence

- Backend: `CouponClaimVO.claimStatus` now distinguishes first success, already claimed, out of stock, not started, ended, and disabled. Existing claims are checked before availability validation and return their original `userCouponId` without decrementing stock or inserting a row.
- List: one current-user `qh_user_coupon` read maps `claimed/userCouponId/userCouponStatus`; all four user-coupon states remain claimed across a refreshed list.
- Frontend: claimed/in-flight cards are disabled; first success updates the card and displayed remaining stock immediately; a stale `ALREADY_CLAIMED` response shows the exact message and reloads the list.
- Verification: requested `CouponOrderIntegrationTest` passed 22/0/0/0; requested Maven `-DskipTests package` and frontend production build both passed. No non-coupon tests or SQL were run.
- Git: committed `1066b83 fix(coupon): prevent duplicate claims with clear user feedback`. The normal non-force push to `origin/feature/coupon-foundation` was attempted once and failed because GitHub port 443 could not connect through `127.0.0.1`; no retry, force-push, merge, or remote rewrite was performed.
# 2026-07-24 Coupon seckill stream milestone - started

- Confirmed the requested branch `feature/coupon-seckill-stream` and a clean working tree.
- Restored the persistent planning workflow and began the limited coupon/Redis design audit; no application code, MySQL, Redis configuration, or SQL data has been changed.
- Read-only schema verification confirmed the required `(user_id, coupon_id)` unique constraint is present. The implementation will use existing `coupon_status=SECKILL` as the explicit activity marker, avoiding an automatic schema migration.
- Added the unverified seckill implementation and real-Redis-focused test source. The required targeted Maven test command stopped before compilation because Maven cannot create `Q:\.m2` (`Access is denied`). No package, Git commit, or push was performed.

## 2026-07-24 Seckill consumer-group compatibility follow-up

- Restored the active coupon-seckill milestone and inspected the current uncommitted implementation.
- Confirmed the compile failure is isolated to `CouponSeckillServiceImpl.ensureGroup()`: Spring Data Redis 2.7.18 has no `StreamOperations.create(...)`; the low-level stream API exposes `xGroupCreate(..., mkStream)` for the required `MKSTREAM` behavior.
- Next: apply the narrow group-creation compatibility fix, then rerun the exact user-specified focused Maven test.

## 2026-07-24 Seckill consumer-group compatibility verification result

- Implemented the compatible `xGroupCreate(streamKey bytes, consumerGroup, ReadOffset.from("0-0"), true)` call. `true` maps to Redis `MKSTREAM`; no synthetic business message, stream deletion, consumer-group deletion, Redis address/password change, or flush operation was used. `BUSYGROUP` is the only ignored group-creation error.
- `mvn -DskipTests compile`: success, 236 main sources compiled. The original missing `StreamOperations.create` error is resolved.
- `mvn "-Dtest=CouponOrderIntegrationTest,CouponSeckillStreamIntegrationTest" test`: attempted twice. Both runs stopped in `testCompile` before test execution because all integration tests could not resolve `com.qinghe.life.*` main packages. Read-only diagnostics confirmed `target/classes` includes `Coupon.class` and `javap` can load it; a further Maven diagnostic encountered an access denial under `D:\maven\apache-maven-3.9.11\Repository`.
- Per the requested order, package, final Git diff checks, commit, and the one normal push were not performed after the focused test failed.

## 2026-07-24 秒杀优惠券 Redis Stream：测试、打包与待 Git 收口

- 使用用户指定的 Maven 本地仓库执行完整专项测试：`CouponOrderIntegrationTest` 22 项、`CouponSeckillStreamIntegrationTest` 7 项，合计 29 tests、0 failures、0 errors、0 skipped。
- 后端 `mvn "-Dmaven.repo.local=C:/Users/28402/.m2/repository" -DskipTests package` 成功，生成 `backend/target/qinghe-life-backend-1.0.0.jar`。本轮无前端改动，未执行 npm build。
- 已更新 API、订单/优惠券/Redis 设计和交接文档，记录普通/秒杀领取分流、Lua 结果码、Redis Key、Stream Group、ACK、Pending 恢复、MySQL 条件库存更新和唯一约束。下一步仅为 Git 差异检查、提交和一次普通 push。

## 2026-07-24 店铺与商品 Redis 热点缓存

- 统一目录缓存组件、Key、TTL、空值、互斥重建、坏值删除、MySQL 降级和事务提交后失效已完成；库存和销量保持实时 MySQL 回源。
- 首次专项测试为 12/1/0：不存在商品未写空值缓存。修复加载器返回 null 后，同一聚焦命令通过 12/0/0/0。
- `mvn "-Dmaven.repo.local=C:/Users/28402/.m2/repository" -DskipTests package` 成功，生成后端 JAR；未运行前端或范围外测试。
# 2026-07-24 — Order WebSocket notification milestone

- Initial Git check (performed once as requested): current branch is `feature/order-websocket-notify`; worktree was clean.
- Started a single WebSocket notification milestone plan. No implementation or Git write has occurred yet.
- Inspected only the permitted order, authentication/context, Redis, Axios token, and user/admin order page code. Confirmed the post-commit event is the minimal backend integration point.
- Added backend WebSocket implementation, frontend lifecycle support, and the focused test class. First requested Maven test attempt downloaded the new Spring WebSocket dependency and compiled main sources, then stopped at two incorrect test mock imports; corrected the imports before retrying.
- Second focused Maven attempt compiled main code and exposed only test-helper type mismatches. The test now uses servlet handshake mocks and the production WebSocket message VO.
- Third focused Maven attempt passed: 31 tests, 0 failures, 0 errors, 0 skipped; `OrderWebSocketIntegrationTest` contributed 16 passing scenarios.
- Requested backend `-DskipTests package` passed. The first frontend build stopped at sandboxed esbuild file reads; the controlled real-path retry passed (1742 modules) with only non-blocking third-party PURE-comment and bundle-size warnings.
- Updated only the requested API/order-design/handoff documentation plus planning records. Pending final Git diff audit, one commit, and one normal push.

## 2026-07-26 V1.0 release closure

- Restored AGENTS.md, project specification, plan, progress, findings, and release handoff context. The user-authorized initial Git check was executed once: `chore/release-v1.0` with a clean worktree.
- Created the release-closure plan in `task_plan.md`. Full backend regression is now in progress; no service, SQL, or Redis data operation has been performed.

- First specified Maven attempt stopped during compilation because the sandbox could not read the existing `jackson-datatype-jsr310` JAR. The one controlled identical real-path retry compiled and ran 147 tests, reporting `1 failure / 0 errors / 0 skipped`.
- The only failure was `DormAssetIntegrationTest`: its repeated-room assertion expected HTTP 409 while the established `BusinessException` transport contract returns HTTP 200 with body `code=409`. The test now asserts that contract and additionally asserts that exactly one `(building_id, room_no)` record exists; no production business logic, database data, Redis data, or schema was changed.
- Two diagnostic-command issues were recorded: a source/target `rg` command referenced `docs` from the backend directory, and a first PowerShell regex command had quote parsing errors. Neither changed project files or runtime state.

- Full regression after the duplicate-room/bed fixes passed: `147 tests / 0 failures / 0 errors / 0 skipped`. Backend `-DskipTests package` and frontend production build also passed; Vite reported only third-party PURE-comment and large-chunk warnings.
- Security closure removed non-empty MySQL and Redis password defaults from `application.yml`. The required post-change full regression then failed with `125 errors`: Redis replied `NOAUTH Authentication required` because this process has no `REDIS_PASSWORD`. Per release constraints, no system environment variable was changed and no password was restored to source. Release verification, package-after-security-change, Git diff/commit, and push are blocked pending secure credential injection.
