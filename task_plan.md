# 管理员营业报表（2026-07-25）

**本轮唯一里程碑：** 在 `feature/business-report` 实现管理员营业概览、日期范围趋势、店铺/商品排行、优惠券使用统计、管理端图表页与 `BusinessReportIntegrationTest`；采用 MySQL 实时聚合，不执行 SQL 或触及订单状态机、优惠券业务、WebSocket、Redis 商品缓存、宿舍/学籍/资产、Agent 助手或操作日志表结构。

| 阶段 | 状态 | 交付与判定 |
|---|---|---|
| 1. 基线、现有字段审计与报表设计 | completed | 订单/优惠券金额和状态、管理员门禁、管理端入口、任务配置与既有索引均已核对；选择实时聚合。 |
| 2. 后端聚合 Mapper、VO、Service 与管理员接口 | completed | 已实现统一时区、90 天日期范围、空结果零值化、趋势补零及稳定排行。 |
| 3. 管理端营业报表页与路由/API 集成 | completed | 已复用既有 HTTP、管理员路由和 Element Plus；原生 SVG 图表无需新增依赖。 |
| 4. BusinessReportIntegrationTest 与指定验证 | completed | 第三次专项验证通过 3/0/0/0；后端 package、前端 build 均通过。 |
| 5. 指定文档、差异检查、单一 Git 收口 | in_progress | API、设计、交接和计划记录已更新；待执行用户授权的最终 Git 差异检查、单一提交与单次 push。 |

### 本轮固定统计口径

- `total_amount` 为商品原始总金额，`pay_amount` 为优惠后实际支付金额；所有金额均由 SQL `DECIMAL` 映射至 `BigDecimal`，不使用浮点数。
- 营业额及销售排行仅统计 `PAID`、`ACCEPTED`、`DELIVERING`、`COMPLETED`；`PENDING_PAY` 与 `CANCELLED` 不计营业额。优惠金额为 `total_amount - pay_amount`。
- 自然日边界统一使用项目业务时区 `Asia/Shanghai`，查询区间使用开始日含、结束日次日零点不含；默认最近 7 个自然日，最长 90 天。
- 取消率若提供，分母固定为全部订单数；总订单数为 0 时返回 0。待处理订单按状态分项返回，避免混合成无语义指标。
- 不新增日报快照：先以数据库 `SUM/COUNT/GROUP BY` 实时聚合实现，只有审计证据表明当前规模或历史查询无法满足需求才产生候选人工 SQL。

### 已知约束和错误记录

- 当前 `task_plan.md` 的较早轮次均为历史记录；本轮以本节与当前源码/测试/构建输出为准。
- 用户授权仅用于本轮列出的 Git 检查、最终单次提交和单次 push；开始检查已执行一次，后续不重复状态/分支检查。
- 专项测试前两次失败分别为 MySQL `only_full_group_by` 趋势分组表达式/排序不一致和测试订单缺少必填地址；已针对性修正，第三次通过。初次受限环境无法读取本地 Maven JAR 和 Vite 解析目录，受控同命令重试后通过。

# 优惠券基础业务与普通订单使用（2026-07-24）

**本轮唯一里程碑：** 在既有普通订单创建、模拟支付、用户取消和超时取消链路上，实现普通优惠券的模型审计、管理、用户领取、订单锁定/核销/释放、前后端页面与专项集成测试。仅使用 MySQL 事务和条件更新；不执行 SQL，不实现 Lua 抢券、Redis Stream、秒杀、全局领取锁、WebSocket、Redis 商品缓存或营业报表。

| 阶段 | 状态 | 交付与判定 |
|---|---|---|
| 1. 基线、结构审计与业务设计 | completed | 当前一次 Git 基线检查确认分支为 `feature/coupon-foundation`；用户已人工完成真实表结构和索引，本轮只通过集成测试核对映射，不执行 SQL。 |
| 2. 后端优惠券管理、领取与订单联动 | completed | 状态、事务、条件更新、订单金额、支付核销与取消释放经 40 项订单/优惠券专项验证。 |
| 3. 用户端与管理端页面 | completed | 仅修复 `AdminCouponView.vue` 缺失的模板闭合标签；真实路径 Vite build 成功。 |
| 4. CouponOrderIntegrationTest 与指定验证 | completed | `CouponOrderIntegrationTest` 20 项及三个既有订单专项合计 40/0/0/0；默认 Maven 本地仓库的 package 成功。 |
| 5. 指定文档与 Git 收口 | completed | 指定文档、残留核验、差异/敏感产物检查和 staged diff check 均完成；本轮以单一功能提交收口，并按授权仅尝试一次推送。 |

### 本轮不可变约束

- `total_amount` 只表示商品原始总额，`pay_amount` 只表示优惠后实际应付金额；金额使用 `BigDecimal` 和统一舍入规则，绝不新增 `goods_amount`。
- 用户优惠券状态只允许 `AVAILABLE → LOCKED → USED`，失败或取消回到 `AVAILABLE`，取消时已过使用期限转为 `EXPIRED`；状态常量不得散落在 Controller、Service 或前端。
- 下单客户端只能提交 `userCouponId`，优惠金额及订单实付由服务端基于当前用户、店铺、金额和时间计算。
- 普通领取与订单锁定均以 MySQL 条件更新作为并发最终保障，写操作复用 `qh_operate_log`，不新增日志表或 SQL 执行。
- 本轮只触及优惠券和订单相关代码、测试、页面及用户指定文档；不审计或修改宿舍、学籍、资产模块。

### 阶段 1 事实与错误记录

- 一次授权 Git 基线检查显示 `## feature/coupon-foundation...origin/feature/coupon-foundation` 且当前分支为 `feature/coupon-foundation`；不再重复执行状态检查。
- 只读 `information_schema` 已确认实库 `qh_coupon` 仅有 `claimed_count/coupon_status/start_time/end_time`，`qh_user_coupon` 仅有 `coupon_status/claim_time/use_time` 和 `(user_id,coupon_id)` 唯一索引；缺少本轮所需库存可用数、领取/使用双窗口、店铺、限领、锁定时间、过期快照和统一 `status`。
- 既有 `coupon_core_increment.sql` 是历史候选；`coupon_foundation_increment.sql` 作为用户已人工完成结构的参考保留，不执行、不生成第二份、不删除。
- 本轮不得修改真实业务数据；测试数据必须使用唯一前缀、测试内精确清理，且不能使用 TRUNCATE、无条件 DELETE 或 Redis 全库/全前缀清理。

# 订单超时取消与多实例任务锁（2026-07-24）

**本轮唯一里程碑：** 在既有订单生命周期基础上，实现由 Spring Task 触发、Redisson 多实例锁保护的超时未支付订单取消；条件更新、精确库存恢复与统一 `qh_operate_log` 必须在每笔订单的同一 `REQUIRED` 事务中完成。只覆盖订单专项测试、指定文档与 Git 收口；不实现优惠券、Redis Stream、WebSocket、缓存、报表或外卖员系统，也不执行 SQL。

| 阶段 | 状态 | 交付与判定 |
|---|---|---|
| 1. 基线、规划与结构门禁 | completed | 已确认分支 `feature/order-timeout-lock`、工作区干净；实体/迁移已覆盖所需字段，索引由未执行候选迁移准备，未执行 SQL。 |
| 2. 超时取消事务与并发契约 | completed | 条件更新、精确库存恢复、直接统一日志与逐笔代理事务已由订单专项验证；支付与超时取消竞争只允许一个更新成功。 |
| 3. 调度器、Redisson 锁与配置 | completed | 默认每分钟的配置化任务、复用 `spring.redis` 的最小 Redisson 客户端、有限等待/租约与异常结束策略已由真实 Redis 锁测试验证。 |
| 4. 订单专项集成测试 | completed | `OrderCreateIntegrationTest,OrderLifecycleIntegrationTest,OrderTimeoutCancelIntegrationTest` 合计 20/0/0/0；新增超时专项 7/0/0/0。 |
| 5. 指定文档、打包与 Git 收口 | in_progress | 已完成指定文档与 `-DskipTests package` 成功；仅剩用户授权的单次 Git 审计、提交和 push。 |

### 本轮不可变约束

- 待支付状态固定为 `PENDING_PAY`；`total_amount` 是商品原始总额、`pay_amount` 是实际应付金额，不新增 `goods_amount`。
- 超时查询仅为 `status = PENDING_PAY AND pay_expire_time <= now`，以可配置批量（默认 100）逐批获取；每笔订单独立事务。
- 最终正确性依赖 `id + PENDING_PAY + pay_expire_time <= now` 条件更新；锁仅避免多实例重复调度，不能替代数据库并发控制。
- 失败必须整体回滚当前订单的状态、库存与操作日志；保留订单/明细，不恢复购物车，不创建订单日志表。
- 不执行迁移 SQL；若测试表结构缺列或索引，停止受影响测试并报告 `backend/src/main/resources/sql/order_lifecycle_schema_increment.sql`。

### 本轮错误记录

| 问题 | 尝试 | 处理 |
|---|---:|---|
| 初次向 `findings.md` 的无上下文补丁未匹配。 | 1 | 未修改文件；已读取文件头并改用精确锚点写入静态门禁结论。 |
| 静态依赖缓存探测未找到预置 Redisson 目录。 | 1 | 将由 Maven 在用户指定的本地仓库解析；不改 Redis 地址、密码或使用 Mock 锁。 |

# 订单生命周期核心闭环（2026-07-24）

**本轮唯一里程碑：** 在既有普通订单创建事务之上完成支付期限、用户查询/模拟支付/取消及库存恢复、管理员状态流转、超时取消、对应页面与专项测试；不实现优惠券、Redis Stream、WebSocket、缓存、营业报表、真实支付、骑手或配送轨迹。SQL 迁移不由应用执行。

| 阶段 | 状态 | 交付与判定 |
|---|---|---|
| 1. 基线、规划与数据库门禁 | completed | 分支 `feature/order-lifecycle-schema`、工作区干净；Q: 映射与本地 Maven 目录已就绪；只读 `information_schema` 已确认 `pay_time`、`accepted_time`、`delivery_time`、`pay_expire_time`、取消/完成字段及 `(status,pay_expire_time)` 索引均存在。 |
| 2. 后端生命周期接口与事务 | completed | 新建时间字段映射和 15 分钟支付期限；支付、取消、超时和管理员动作均使用条件更新，取消库存恢复与一次直接日志处于同一事务。 |
| 3. 用户与管理员订单页面 | completed | 用户订单列表/详情/模拟支付/取消和管理员订单列表/详情/固定状态动作已复用现有 HTTP、路由守卫与布局。 |
| 4. 订单专项测试与残留检查 | completed | `OrderCreateIntegrationTest,OrderLifecycleIntegrationTest` 为 13/0/0/0；测试前缀订单、明细、购物车、商品、店铺、用户与地址残留均为 0。 |
| 5. 构建、文档与 Git 收口 | in_progress | 后端打包与前端生产构建均通过；本次统一文档更新后仅剩 Git 审计、提交与一次普通 push。 |

### 本轮不可变约束

- 待支付编码仅为 `PENDING_PAY`；金额语义保持 `total_amount`（商品原始总额）与 `pay_amount`（实际应付），不新增 `goods_amount`。
- 用户和管理员身份分别只从 `UserContext` 与 `AdminContext` 获取；Controller 不直接调用 Mapper。
- 取消必须在一个 `REQUIRED` 事务内完成条件状态更新、按订单明细恢复库存和一次直接 `qh_operate_log` 写入；不得触发通用 `REQUIRES_NEW` 成功日志。
- 定时扫描仅触发可直接测试的 Service 批处理；数据库条件更新是支付、取消和超时取消并发下的最终正确性保障。
- 不执行迁移 SQL、全量回归、无关宿舍/学籍/资产测试，或 Git fetch/pull/rebase/reset/clean。

# 订单生命周期状态模型与候选迁移（2026-07-23）

**本轮唯一里程碑：** 只建立订单状态枚举、候选数据库迁移和设计文档，并验证既有普通订单创建。禁止实现支付、取消、库存恢复、管理员订单、Spring Task、优惠券、Redis Stream、WebSocket、营业报表及订单前端页面；禁止真实数据库 SQL。

| 阶段 | 状态 | 交付与判定 |
|---|---|---|
| 1. Git 收口和分支 | completed | `chore/git-workflow` 已推送；`main` 与 `origin/main` 同步；`develop` 已推送；当前分支为 `feature/order-lifecycle-schema`。 |
| 2. 状态模型 | completed | 已新建统一 `OrderStatus`，订单创建和订单创建断言均使用 `PENDING_PAY` 枚举常量；未新增状态变更接口。 |
| 3. 候选迁移和设计文档 | completed | 已生成仅供 DataGrip 人工审核的增量脚本，并记录字段/索引门禁、合法流转与日志事务方案。 |
| 4. 原订单创建验证和功能提交 | partial | `Q:\backend` 与 `Q:\.m2` 均不存在，指定 Maven 命令未运行；待具备该路径和 Redis 的本机验证后才可结束验证。功能提交和推送仍待本轮收口。 |

### 门禁

- `PENDING_PAY` 是唯一待支付编码；不得引入 `PENDING_PAYMENT` 或更改既有数据库状态值。
- `total_amount` 为商品原始总额，`pay_amount` 为最终应付金额；不得新增 `goods_amount`。
- 用户仍须在 DataGrip 手工执行 `SHOW CREATE TABLE qh_order;` 和 `SHOW INDEX FROM qh_order;`。在结果返回前，不向 `Order` 实体加入候选字段，也不运行依赖新列的测试。
- 取消订单的后续实现必须使条件状态更新、库存恢复和成功日志处于同一个 `REQUIRED` 事务，直接复用 `qh_operate_log`，并避免通用 AOP 的 `REQUIRES_NEW` 成功日志重复写入。

# 宿舍寝室号唯一性与资产编号前提修复（2026-07-23）

**本轮唯一里程碑：** 修复重复寝室号未返回 HTTP 409 的服务层缺口，保障“楼栋编码+寝室号+床位号”资产套装编号的唯一性前提；仅修改寝室新建/编辑的查重与相关记录，运行专项测试后创建 Git 本地历史快照。不执行 SQL、不修改表结构、不启动或停止服务、不变更学生入住、退宿、资产二维码生成或学籍事务。

| 阶段 | 状态 | 交付与判定 |
|---|---|---|
| 1. 根因与范围确认 | completed | 已确认 `createRoom/updateRoom` 仅依赖数据库重复键异常；当前集成测试中该约束未生效，重复寝室号返回 200。 |
| 2. 服务层查重修复 | completed | 已在同楼栋内以 MyBatis-Plus 主动检查寝室号，创建与改名冲突均返回 HTTP 409，并保留数据库重复键兜底。 |
| 3. 专项验证、记录与 Git 快照 | partial | 专项主/测试源码编译完成；测试运行受沙箱 Redis 不可达阻断，待本机重跑。代码与记录已创建本地 Git 快照 `dd7149e`。 |

### 本轮验证

- `mvn -Dtest=DormAssetIntegrationTest test`：主源码 204 个、测试源码 23 个均编译；2 项测试在 `setUp` 阶段因 Redis `192.168.100.128:6379` 连接超时而报错，未执行业务断言。未修改 Redis、数据库或测试断言。
- Git：已创建本地提交 `dd7149e fix: enforce dorm room number uniqueness`，仅包含服务修复和本轮计划/发现/进度记录；用户已有 `.gitignore` 修改未纳入提交。

### 本轮边界与结论

- 不依赖或修改数据库唯一索引；服务层以 `(building_id, room_no)` 在写入前查重，仍保留现有 `DuplicateKeyException` 兜底。
- `DormAssetIntegrationTest` 已覆盖 `JA101-01` 编号、五件资产和二维码；本轮只让其重复寝室断言可靠通过。

# 个人中心姓名显示语义与接口映射修复（2026-07-20）

**本轮唯一里程碑：** 修复个人中心与用户导航对账号昵称、登录手机号和学生实名资料的混用。只调整当前用户资料查询、响应映射、个人中心及导航显示、针对性测试与指定文档；不修改数据库结构、不执行 SQL、不修改学生建档、入住、退宿、换寝、学籍异动或认证流程，不启动或停止服务、不执行 Git。

| 阶段 | 状态 | 交付与判定 |
|---|---|---|
| 1. 交接恢复、当前契约与根因审计 | completed | 已读取指定交接/计划/API/页面文档，并核对 qh_user、qh_student_profile、Entity、用户/学生接口、Store、个人中心和导航实现。 |
| 2. 后端当前实名资料映射与安全测试 | completed | `/api/user/me` 已在用户服务组合当前登录用户与 `current_flag=1` 的学生资料，安全返回 `realName`、`studentNo`、`hasStudentProfile`，不回写昵称；已新增 Mockito 针对性覆盖，待 Maven 执行。 |
| 3. 前端统一名称回退与容错展示 | completed | 个人中心、导航和真实姓名卡已使用同一 `realName → nickname → 脱敏手机号 → 未设置` 规则；未建档、加载失败、实名为空均有独立中文状态。 |
| 4. 回归验证、文档与交接收口 | partial | 文档已更新；针对性后端测试 6/0/0、前端构建成功。完整 Maven 回归为 77/1/0（范围外宿舍断言失败）；用户指定 Q 盘路径缺失，跳过测试打包因受控执行额度拒绝而未运行。 |

### 本轮硬性边界

- `qh_user.phone` 是登录手机号，`qh_user.nickname` 是账号昵称，`qh_student_profile.real_name` 是学生真实姓名，`student_no` 是学号；不新增或以空 `username` 充当真实姓名。
- 当前学生资料只以 `current_flag=1` 查询；不返回 `currentFlag`、`activeFlag`、`passwordHash`、二维码令牌、Token 或内部资料 ID。
- 不把 `realName` 写回 `nickname`，不修改 `qh_user.phone`，不允许学生经个人中心修改受保护的学籍字段。
- 不通过 Controller 直接调用 Mapper；优先由既有 Service 使用 MyBatis-Plus 完成当前用户与当前学生资料组合。

### 阶段 1 已知事实与错误记录

- 初步检索显示 `/api/user/me` 直接返回 Redis 会话中的 `UserDTO`；该对象目前仅含 `id/username/nickname/avatarUrl/phoneMasked/profileCompleted/hasPassword`，未查询或映射当前学生实名资料。`ProfileView.vue` 与 `UserLayout.vue` 仅以 `nickname` 显示名称，且个人中心仍标注“用户名”。
- 读取时按推断路径访问 `StudentProfileServiceImpl.java` 和 `StudentProfileController.java` 失败，原因是当前项目的学生资料实现文件名不同；下一步必须先用文件清单定位真实类名，不重复同一路径尝试。
- 用户指定的验证路径 `Q:\backend` 与 Maven 仓库 `Q:\.m2` 在本会话均不存在。不能在该前提下执行指定的两条 Maven 命令；不会创建、映射或修改该驱动器路径，后续仅运行真实工作区内允许的前端构建并如实记录后端验证阻塞。
- 实际工作区的替代验证：受控 `mvn -Dtest=UserAvatarServiceTest test` 为 6/0/0；受控 `mvn clean test` 主/测试源码均编译，结果为 77 tests、1 failure、0 errors，失败为范围外 `DormAssetIntegrationTest.administratorMaintainsUniqueRoomBedAndCreatesIdempotentAssetSetWithPrivateQr` 期望 HTTP 409 实得 200。本轮未改宿舍资产逻辑或测试断言。前端真实路径 `D:/develop/NodeJS/npm.cmd run build` 成功（1732 modules）。
- `mvn clean package -DskipTests` 的受控执行申请被平台额度限制拒绝；没有绕过、重试或以替代命令伪造打包结果。

# 学籍异动、批量毕业与二维码批量管理：实施与验证（2026-07-19）

**本轮唯一里程碑：** 在既有管理员认证、学生资料版本、入住退宿、资产套装和唯一操作日志链路中，实现管理员学生管理、学籍异动、单个/批量联动退宿、批量毕业及二维码批量操作；不修改数据库结构、不执行 SQL、不启动或停止服务、不执行 Git。只有完整 Maven 测试为 0 failures/0 errors，且后端打包、前端构建成功后才可标记完成。

| 阶段 | 状态 | 交付与判定 |
|---|---|---|
| 1. 交接恢复、现有契约与实现门禁 | completed | 已确认实体、Mapper、现有退宿事务、AdminContext、管理员路由/API 及前端现有栈；可在同一宿舍入口实施。 |
| 2. 管理端学生查询与学籍异动后端 | completed | 已实现管理员查询、历史版本、转专业、休学、退学、毕业和复学；扫码许可已收紧为仅 ENROLLED。 |
| 3. 受限批量宿舍、资产与二维码后端 | completed | 已实现预览令牌、二次确认提交、100 上限、整批事务的毕业、退宿、资产释放、二维码停用和轮换。 |
| 4. 管理端页面与 API 集成 | completed | 已复用 AdminLayout、管理员 Store、单一 HTTP 实例及宿舍资源页，完成学生和资产批量管理交互。 |
| 5. 自动化测试、全量验证、残留检查与文档 | partial | 文档、后端编译/打包和前端构建已完成；完整 Maven 回归因 Redis 连接失败为 77/14/41/0，未完成专项覆盖与零残留复核。 |

### 本轮硬性边界

- 当前 `student_status` 只能新写 `ENROLLED`、`SUSPENDED`、`DROPPED`、`GRADUATED`。`TRANSFER_MAJOR`、`REINSTATED` 仅为异动类型，不能作为当前状态或新增入住许可。
- `current_flag=1` 仅代表当前学生资料，关闭历史版本设为 `NULL`；`active_flag=1` 仅代表当前入住，关闭后设为 `NULL`。不得物理删除学生资料或入住历史。
- 退学、毕业及选择退宿的休学必须复用并抽取既有退宿事务内核；不得直接改 `active_flag` 或 `asset_set.status` 绕过锁定、关闭和资产释放规则。床位 `status` 始终只表示目录启停。
- 批量毕业、退宿、资产释放和二维码操作必须先预览、二次确认并限制每批至多 100 条；禁止无条件全表更新、无条件 DELETE、TRUNCATE、FLUSHDB、清空 `qr_token` 或修改 `asset_set_no`。
- 全部管理员读写接口和服务入口都从 `AdminContext` 取得身份；普通用户不得修改实名、学号、院系、专业、班级或学籍状态。写操作复用唯一 `qh_operate_log`，日志不得含完整姓名、学号、电话或二维码令牌。

### 已知验证前提

- 用户指定 Maven 命令依赖会话可见的 `Q:` 路径；执行前先核验该路径及工作区实际状态，不创建或修改数据库、服务和配置。
- 历史验证记录不是本轮结果。完整回归、打包、前端构建和测试数据残留均须以本轮实际输出为准。

### 阶段 1 实现门禁结论

- `AdminDormCheckinServiceImpl.checkout` 已以 `FOR UPDATE` 锁定当前入住和资产套装，并在事务中关闭入住、写入时间/原因/管理员、释放套装；新增学籍服务必须复用等价内核，不能绕过它直接更新状态。
- 当前学生扫码 `ALLOWED` 集合仍包含 `TRANSFER_MAJOR`、`REINSTATED`，需在本轮收紧为仅 `ENROLLED`。学生资料初次建档已有 `ENROLLED` 基线。
- 宿舍管理员唯一 Controller 是 `DormAssetAdminController`，前端复用 `adminDorm.js`、`AdminLayout` 与独立管理员路由；新页面/接口应在此既有路径扩展。

### 阶段 5 错误与停止状态

- 首次工作区直接 Maven 编译访问工作区 `.m2` 中 jar 时被 `Access is denied` 阻断；改用用户指定的临时 `Q:` 短路径后，203 个主源码成功编译，未重复原命令。
- 完整 `clean test` 已实际执行，测试源码编译通过，但 Redis 不可连接，导致管理员登录 HTTP 503 与各集成测试清理中的 `RedisConnectionFailure`；统计为 77 tests、14 failures、41 errors、0 skipped。按项目约束未启动、停止或修改 Redis，也未把环境故障归因为通过。
- 最新 `clean package -DskipTests` 和前端生产构建均成功；因完整测试并非 0 failures/0 errors，保留本里程碑 `partial`，等待 Redis 可用后从完整回归、测试数据/Key 精确残留检查继续。

## 已完成的前置审计（仅设计，保留）

# 学生学籍异动、毕业处理与管理员批量宿舍操作：业务审计与分阶段设计（2026-07-19）

**本轮唯一里程碑：** 基于现有学生资料、入住、资产、管理员和操作日志模型，完成只读静态审计、规则设计、安全增量 SQL 评估和分阶段实施计划；不实现业务代码，不执行 SQL，不启动或停止服务，不执行 Git。完成审计文档后立即停止。

| 阶段 | 状态 | 交付与判定 |
|---|---|---|
| 1. 交接、计划与现有契约恢复 | completed | 已读取规则、交接、计划、进度、发现、宿舍资产设计、数据库与接口契约；已确认本轮仅审计设计。 |
| 2. 学籍、入住、资产、二维码与管理员静态审计 | completed | 已核对真实字段、实体、Service、Controller、DTO/VO、管理员门禁、日志切面和既有退宿事务；已明确可复用能力与状态契约漂移。 |
| 3. 业务规则、安全 SQL 评估与批处理恢复设计 | completed | 已形成学籍异动、单个/批量退宿、资产释放、二维码和毕业批处理边界；结论为无需生成或执行增量 SQL。 |
| 4. 文档收口与停止 | completed | 已更新 findings.md、progress.md、dorm_asset_plan.md、docs/database-design.md、docs/api-contract.md、docs/HANDOFF.md，并创建完整审计文档；未执行运行验证。 |

### 本轮硬性边界

- 不实现任何 Controller、Service、Mapper、DTO、VO、前端页面、测试或数据库迁移脚本；不执行 SQL、服务控制或 Git。
- 不物理删除入住历史，不清空 `qr_token`，不强制释放仍存在当前入住的资产套装，不改变 `qh_dorm_bed.status` 的目录启停语义。
- 仅复用既有 `qh_operate_log`；不设计或新增 `academic_log`、`graduation_log`、`dorm_clear_log`、`qr_clear_log`。
- `current_flag=1` 仅表示当前学生资料，历史资料保持 `NULL`；不借机重定义字段语义。

### 本轮记录的验证问题

| 问题 | 处理 |
|---|---|
| 文档核对命令中的“禁止新增日志表名”负向 `rg` 未命中而返回退出码 1。 | 这是预期的无匹配结果，不是文档缺失；已在审计文档明确列出四个禁止新增表名。未重试相同命令。 |

## 已暂停的前序里程碑（未在本轮继续）

# 宿舍扫码、资产状态与中文化收口（2026-07-18）

**本轮唯一里程碑：** 仅在既有宿舍模块中核实当前 Redis 测试事实，增加浏览器本地二维码图片识别，固化套装占用与单件资产健康规则，统一前端中文状态显示，并完成专项、完整回归、构建、残留检查和指定文档收口。完成后立即停止。

| 阶段 | 状态 | 交付与判定 |
|---|---|---|
| 1. 上下文、Redis 与当前实现基线核实 | in_progress | 解析最新 Surefire XML/TXT、实际 Spring Redis 配置与环境覆盖；审计二维码、资产、前端字典及测试基线，不能将旧 HTTP 503 当作 Redis 故障。 |
| 2. 后端资产规则与专项测试 | pending | 保持数据库枚举不变；实现可入住条件、派生健康状态、幂等初始化/补齐和明确中文拒绝原因，且不触碰退宿、换寝事务。 |
| 3. 前端图片扫码与中文展示 | pending | 本地识别 PNG/JPG/JPEG/WEBP，三种输入复用同一解析链路；统一所有宿舍业务枚举的中文映射。 |
| 4. 专项回归、完整验证、残留检查与文档 | pending | 专项、`clean test`、`clean package -DskipTests`、真实路径 Vite 构建和文档全部实际完成，且完整回归为 0 failures/0 errors 后方可完成。 |

### 本轮硬性边界

- 不修改数据库结构、不执行 SQL、不进行 MyBatis-Plus 大范围重构；不改二维码格式、不上传二维码图片、不在界面或日志泄露完整 Token。
- 不修改确认入住、退宿或换寝事务；不开发报修工单、不新增日志表、不启动或停止服务、不执行 Git。
- Redis 仅使用安全 `PING` 或测试前缀短期 Key 验证；必须精确清理，不写入密码或伪造 Mock Redis 测试通过。

### 当前证据与待核实项

- 旧 `AdminDormResourceIntegrationTest` 记录过 3 failures/0 errors，原因描述为 `192.168.100.128:6379` 拒绝连接并被转换为 HTTP 503；这是历史证据，不是本轮结论。
- 较新的交接记录表明后续完整 Maven 回归曾到 71/0/0/0，但需由当前工作区的最新 Surefire 产物、测试过程和配置重新核实，不能只引用历史日志。
- 当前 `backend/target/surefire-reports` 在初始检查中未列出报告，需区分“无现存产物”与“测试失败”，必要时通过受控测试生成新的 XML 证据。

## 暂存的前序计划

# 普通订单创建链路：自动化测试与交付收口（2026-07-18）

**本轮唯一里程碑：** 先在既有宿舍管理员链路中消除 `AdminDormCheckinVO` 与 `previous_checkin_id` 契约不一致造成的主源码编译阻塞；随后只完成已实现的普通订单创建自动化测试、精确残留检查、完整回归、两端构建与文档收口。不得新增业务或修改数据库结构。

| 阶段 | 状态 | 交付与判定 |
|---|---|---|
| 1. 宿舍字段契约与主源码编译修复 | completed | 真实语义为换寝内部历史关联；当前服务已无缺失 VO setter 调用，方案 B 保持不向响应暴露。指定 compile 成功。 |
| 2. 宿舍最小回归 | completed | 管理员入住专项 7 项通过；首次单独学生合跑的多校区前置诊断未在干净完整回归复现，最终 68/0/0/0 通过。 |
| 3. 普通订单创建专项测试与残留检查 | completed | `OrderCreateIntegrationTest` 5/0/0/0；已补强购物车、操作日志和全部测试登录 Token 的精确清理/零残留断言。 |
| 4. 完整后端回归与产物构建 | completed | `clean test` 为 68/0/0/0；随后 `clean package -DskipTests` 成功生成 JAR。 |
| 5. 前端生产构建 | completed | 用户指定真实路径的 Vite 构建成功，1725 modules。 |
| 6. 文档与人工验收交接 | completed | 11 份指定文档和计划记录均已更新；订单创建满足“已完成并验证”的全部条件。 |

### 本轮硬性边界

- `total_amount` 始终表示商品小计；`pay_amount` 是最终应付金额；所有金额均使用 `BigDecimal`，不新增 `goods_amount` 或其他重复金额列，也不执行迁移。
- 仅支持单店普通订单，不实现订单取消/回补、支付、普通优惠券核销、秒杀、Redis Stream、分布式锁或配送状态。
- 用户仅由 `UserContext` 取得；请求仅允许 `cartItemIds`、`addressId`、`remark`，客户端价格、金额、库存、状态和快照均不可信。
- 库存正确性由 MySQL 事务中的条件更新保证，不用 Redis 库存或普通订单分布式锁；SQL、服务或测试不得清空 MySQL/Redis。

### 本轮错误记录

| 尝试 | 结果 | 后续处理 |
|---|---|---|
| 前序订单门禁 | 因把设计字段 `goods_amount` 误作真实必需列而停止；真实库使用 `total_amount`。 | 用户已明确批准 `total_amount` 的金额语义；本轮门禁仅核对真实字段和不可替代关联。 |
| 只读字段盘点 | MySQL 查询已返回完整结果，但命令行密码安全警告被 PowerShell 作为原生 stderr 使脚本退出。 | 不将该非业务警告当作门禁失败；后续静态和只读查询采用不提升该警告的执行方式。 |
| Maven 依赖准备 | PowerShell 的 `New-PSDrive` 不是 JVM 可见的 DOS 驱动器；改用临时 `subst Q:` 后 `dependency:go-offline` 超过 64 秒。 | 缓存已保留且后续专项命令完成 182 个主源码、21 个测试源码编译；该超时不等于测试结果。 |
| 订单专项测试夹具 | 前四次专项运行分别暴露购物车唯一约束、失效楼栋外键和楼栋编码长度问题。 | 仅修正 `ORDER_CREATE_TEST_` 夹具；第五次重跑转为范围外宿舍主源码编译错误。 |
| 范围外主源码编译 | `AdminDormCheckinServiceImpl` 调用缺失的 `AdminDormCheckinVO.setPreviousCheckinId(Long)`。 | 本轮用户已将宿舍编译基线纳入范围；先核实其持久化语义、管理员契约和学生端隔离，再作最小真实修复。 |
| 首次受限宿舍测试 | 沙箱拒绝 Redis 连接；受控重跑时学生单测因多启用校区规则出现 1 项前置失败。 | 不改学生业务或降低断言；干净完整回归随后 68/0/0/0，故不作为当前失败。 |
| 首次前端构建 | 沙箱拒绝 Node 解析 `C:\Users\28402` 元数据。 | 以相同真实路径受控重跑，Vite 构建成功。 |

## 暂存的前序计划

# 管理员入住记录、退宿与换寝（2026-07-17）

**本轮唯一里程碑：** 在真实 `qinghe_life` 数据库通过只读门禁的前提下，复用既有管理员认证、宿舍基础管理、学生扫码入住和唯一操作日志链路，实现管理员入住记录查询、单个退宿、单个换寝、资产套装状态同步、完整历史保留及自动化回归；完成后停止等待审核。

| 阶段 | 状态 | 交付与判定 |
|---|---|---|
| 1. 上下文、现有栈与真实数据库只读门禁 | completed | 实时库已确认入住历史/关闭/换寝关联字段、可空有效标记、用户与床位有效唯一索引、六项入住外键、资产套装 `AVAILABLE` 默认状态、床位目录 `status=1` 默认状态及所需十一张表均存在。 |
| 2. 后端入住查询、退宿、换寝与测试 | in_progress | 在既有 `/api/admin/**`、`AdminContext`、MyBatis-Plus、事务和 `qh_operate_log` 栈中实现分页详情、退宿和换寝，并覆盖授权、并发、回滚、历史和脱敏。 |
| 3. 管理员入住管理页面 | pending | 复用 `AdminLayout`、唯一 HTTP 实例、管理员 Store 和宿舍目录 API，提供查询、详情、退宿、换寝与防重复提交。 |
| 4. 文档、回归构建、精确清理与交接 | pending | 更新用户指定文档；执行指定 Maven 测试/打包和前端构建；精确清理本轮标识测试数据及 Redis Key，记录实际结果后停止。 |

### 本轮硬性边界

- 不自动执行 SQL、DDL、DML 导入或数据库配置修改；不启动、停止或重启服务；不执行 Git。
- 仅支持管理员查询、单个退宿和单个换寝；不实现学生自助操作、批量退宿、学籍异动、统计、资产报修或二维码 OSS 上传。
- 不覆盖或删除入住历史；`active_flag=1` 是唯一有效入住，关闭记录使用既有可空历史规则；`qh_dorm_bed.status` 保持目录启停语义。
- 管理员 ID 仅由 `AdminContext` 获取；所有写操作只复用唯一 `qh_operate_log`，不新增任何日志表。

### 本轮错误记录

| 尝试 | 结果 | 后续处理 |
|---|---|---|
| 首次 MySQL 只读联合元数据查询 | MySQL 返回失败；为避免在输出中泄露连接凭据，诊断文本被整体脱敏，尚不能判断是连接策略还是凭据解析问题。未执行任何写操作。 | 改用单一只读连通性查询并按实际用户名/密码值精确脱敏，仅保留错误类别；不重复执行原联合查询。 |
| 最小 MySQL 只读连通性查询 | 初始返回 `ERROR 1045`；安全形态检查确认配置密码为 Spring `${MYSQL_PASSWORD:...}` 占位符，命令行将其原样传入，未按应用规则解析。未执行任何业务 SQL。 | 以 Spring 等价规则优先读取当前进程 `MYSQL_PASSWORD`，缺失时使用配置默认值；只重试一次只读门禁查询。 |
| 配置值安全形态诊断 | 首次诊断脚本因 PowerShell 中 `$key:` 的变量边界语法报错，未读取或输出凭据。 | 改为 `${key}` 变量边界与无正则的行解析；已确认用户名为直接值、密码为占位符，未输出敏感值。 |
| 后端现有栈批量读取 | 预设 `vo/PageResult.java` 路径不存在；实际类型位于 `common` 包，其他宿舍/管理员代码已正常读取。 | 后续使用已确认的 `com.qinghe.life.common.PageResult`，不创建重复分页类型。 |
| 首次后端编译 | Maven 在受限环境中无法写入其配置的本地依赖缓存，未进入 Java 编译。 | 申请同一只读依赖缓存的受控权限重跑。 |
| 受控编译重跑 | PowerShell 将未加引号的 `Q:\.m2` 参数解析为 Maven 插件前缀，失败于 Maven 参数解析，仍未进入 Java 编译。 | 对用户指定参数整体单引号转义后重跑，不改变命令语义。 |
| 已引用的受控编译重跑 | 当前会话不存在 `Q:` 驱动器，Maven 无法创建 `Q:\.m2`，仍未进入 Java 编译。 | 在同一 PowerShell 进程建立仅会话有效、根目录指向当前工作区的 `Q:` PSDrive，再按用户指定目录和参数运行；结束后移除该映射。 |

## 暂存的前序计划

# 普通订单创建链路（2026-07-17）

**目标：** 在用户已手工执行并复核 `order_core_increment.sql` 的前提下，先通过真实 `qinghe_life` 的只读结构与历史数据门禁；通过后，仅实现购物车结算、单店普通订单创建、地址/商品快照、数据库条件扣库存、精确清理购物车和下单成功页。

| 阶段 | 状态 | 交付与判定 |
|---|---|---|
| 1. 上下文、真实数据库门禁与现有栈审计 | blocked | 真实 `qh_order` 不存在设计要求的 `goods_amount`，而是 `total_amount`；按用户规则立即停止，不修改数据库。 |
| 2. 后端订单事务与自动化测试 | pending | 等待用户以 DataGrip 修正或确认金额字段契约后，才可实现。 |
| 3. 前端结算与下单成功页 | pending | 依赖阶段 2，不得提前实施。 |
| 4. 文档、回归构建与交接 | partial | 已记录门禁阻断；未执行代码、测试或构建。 |

### 本轮不可突破边界

- 不执行或修改 SQL/数据库；不执行优惠券或秒杀脚本；不启动、停止服务或执行 Git。
- 只做普通单店订单；不做取消、回补、支付、配送流转、优惠券核销、秒杀、Redis Stream、分布式锁或 Redisson。
- 普通商品库存仅由 MySQL 事务内条件更新保证；前端价格、金额、状态与库存均不可信。
- 若阶段 1 门禁不通过，只记录结构差异与停止原因；不进入后续阶段。

### 阶段 1 阻断证据（只读，2026-07-17）

- 目标库为 `qinghe_life`；查询的 `qh_order`、`qh_order_item`、`qh_cart`、`qh_goods`、`qh_shop`、`qh_user_address` 均为 InnoDB。
- `qh_order` 已有 `order_no` 唯一键、`user_id`/`shop_id` 索引、地址与校区/楼栋/配送点快照字段、`discount_amount`、`delivery_fee`、`pay_amount`、`status`、`remark` 与时间字段；`qh_order_item` 已有订单/商品快照与 `order_id` 索引。
- 但金额主字段实际为 `total_amount DECIMAL(10,2) NOT NULL`，没有 `goods_amount`。这与本轮设计的订单商品金额字段名不一致，不能在未确认语义映射与文档/实体契约前继续。
- 元数据估算显示 `qh_order=0`、`qh_order_item=0`；由于没有既存订单行或迁移前基线，本轮无法证明“旧订单数据没有因迁移丢失”。只读聚合和外键查询在引用不存在的 `goods_amount` 时被 MySQL 拒绝，未执行任何 DDL/DML。

## 任务上下文（前序记录，保留）

# 管理端商品筛选编译修复（2026-07-17）

**目标：** 仅修复 `AdminGoodsServiceImpl` 对已拆分查询字段的三处过期调用，并核对店铺类型与店内商品分类筛选语义；不执行 SQL、不改数据库、不扩展功能。

| 阶段 | 状态 | 交付与判定 |
|---|---|---|
| 1. 上下文、契约与筛选语义核对 | completed | 已确认商品分页采用 MyBatis-Plus `BaseMapper` 与 `LambdaQueryWrapper`，没有商品查询 XML；店铺类型与店内分类条件分别对应两张表。 |
| 2. 最小编译修复 | completed | 服务层已处于正确的双字段状态；管理端移除旧查询参数并补齐现有店内分类查询 API 的导出与筛选参数同步。 |
| 3. 指定回归构建与记录 | completed | 主代码 `compile` 与前端生产构建通过；完整测试和 `-DskipTests package` 均因范围外 `ShopCoverServiceTest` 的测试编译错误失败，已如实记录。 |

**边界：** 不修改数据库结构，不执行 SQL，不新增字段或日志表，不启动或停止服务，不执行 Git；不修改购物车、订单、优惠券、配送、OSS 或宿舍业务。

**当前发现：** `AdminGoodsQuery` 已含 `shopId`、`shopCategoryId`、`goodsCategoryId`、`saleStatus` 和 `keyword`；`AdminGoodsServiceImpl` 同时读取 `Shop.categoryId`（店铺类型）与 `Goods.categoryId`（店内商品分类），必须按查询条件来源分别映射。

# 续办复核（2026-07-17）

- 本轮阶段 1 已由只读 `information_schema` 查询再次确认：全局店铺类型 `qh_category`、店内商品分类 `qh_goods_category`、可空 `qh_goods.category_id`、唯一索引、导航索引和两项外键均符合用户指定契约。阶段 2 继续进行。

# 青禾校园生活服务系统：全站功能审计计划（2026-07-16）
## 店内商品分类与商店主页优化（2026-07-17）

**目标：**在用户已手工执行并复核商品分类增量迁移的前提下，复用现有管理员认证、商品、购物车、OSS、HTTP、Router 与操作日志链路，实现店内商品分类管理、商品同店分类关联、后台筛选语义修正、商店详情紧凑布局、商品图不裁切和固定购物车入口。

| 阶段 | 状态 | 交付与判定 |
|---|---|---|
| 1. 上下文与只读数据门禁 | completed | 已读取规则、交接、记录与设计文档；真实 `qinghe_life` 的表、列、索引和外键均符合迁移契约。 |
| 2. 后端分类与商品关联 | in_progress | 在现有管理员门禁、服务和日志链路中实现分类 CRUD、同店校验与查询语义；补充自动化测试。 |
| 3. 前端管理与商店详情 | pending | 在现有管理布局、HTTP、Router 和购物车链路中完成分类管理、筛选、图片处理和商店详情重构。 |
| 4. 回归、构建与文档交接 | pending | 执行指定 Maven 测试/打包、前端构建，更新所有指定文档和记录后停止。 |

### 本轮硬性边界

- 不执行 SQL、不修改数据库结构、不启动或停止服务、不执行 Git；不实现订单、优惠券、支付或配送。
- 不重构购物车核心业务，不新增图片表、日志表、缓存体系或第二套认证/HTTP/OSS 栈；自动化测试只 Mock OSS。
- 管理写操作只从 `AdminContext` 获取操作者；分类与商品关联只能由服务端 ID 校验，不信任客户端分类文本。

### 本轮错误记录

| 尝试 | 结果 | 调整 |
|---|---|---|
| 首次大批量后端补丁 | 编辑器长时间无完成信号，手动中止后发现新增分类文件、`Goods.categoryId` 与部分 DTO 已写入，但后续 VO/服务改动未完成。 | 已立即核对实际落盘状态；后续改为小批量原子补丁，并只在每批后复核。 |

## 商品店内分类模型与商店主页重构准备（2026-07-17）

**目标：**仅审计真实数据库结构、现有代码与文档，判断店内商品分类是否需要新增模型；如需要，仅生成待人工执行的安全增量 SQL 和设计/交接文档。不修改业务代码、不执行 SQL、不启动服务、不运行构建或测试。

| 阶段 | 状态 | 交付与判定 |
|---|---|---|
| 1. 上下文、文档与现状恢复 | completed | 已读取项目规则、交接/计划/进度/发现及指定设计审计文档，建立本轮只读边界。 |
| 2. 数据库和代码语义审计 | completed | 已通过静态实现与 `qinghe_life` 只读元数据复核分类、商品、筛选、展示、图片和购物车依赖；缺少店内商品分类模型已证实。 |
| 3. 迁移与页面/图片设计 | completed | 已生成待人工执行的 `goods_category_increment.sql`，并补齐数据库、API、页面和审计设计。 |
| 4. 静态复核与交接 | completed | 已复核 SQL 不含禁用关键字、文档覆盖齐全，并更新进度、发现和交接；按本轮停止条件结束。 |

### 本轮硬性边界

- 不执行 SQL，不修改数据库，不启动、停止或重启服务，不运行测试、构建或 Git 命令。
- 不修改任何业务代码、购物车业务、订单、优惠券、支付或配送；不新增商品图片表或日志表，不生成图片。
- 若 `qh_category` 实为店铺类型，绝不复用为店内商品分类；旧商品的 `category_id` 保持 `NULL`，不伪造默认或未知分类。

### 本轮错误记录

| 尝试 | 结果 | 调整 |
|---|---|---|
| 首次 MySQL 只读元数据查询 | MySQL CLI 会话未选择数据库，`DATABASE()` 返回 `NULL`，后续元数据查询报 `ERROR 1046 No database selected`；未执行任何 DDL/DML。 | 下一次在同一只读查询中显式指定已由应用配置声明的 `qinghe_life`，不重复无库名调用。 |


## 登录 Tab、用户头像 OSS 路径与个人中心文案修正（2026-07-17）

**目标：** 不开发新功能；仅修复登录 Tab 可读性、调整用户头像 Object Key、保留旧头像清理兼容，并将个人页面改为自然文案。

| 阶段 | 状态 | 内容 |
|---|---|---|
| 1. 样式、路径与文案审计 | completed | 已读取 LoginView、ProfileView、全局样式、OSS 操作器、头像服务与测试；确认没有全局 Tab 覆盖。 |
| 2. 小范围修正与测试更新 | completed | 已分离 Tab 活动文字/背景样式；已切换新 Object Key，保留历史路径删除兼容；已更新头像回归断言与个人中心文案。 |
| 3. 回归、构建与记录 | completed | `Q:\backend` 下 Maven 32/0/0/0 通过并成功打包；真实前端路径生产构建成功，结果已记录。 |

### 本轮边界

- 不修改登录业务、Redis Token、数据库结构、资料接口或操作日志体系；不实现商品、店铺、订单或支付图片。
- 不移动、不批量改写既有 OSS 对象或数据库 URL；旧 URL 继续显示，替换时仅按现有单对象清理逻辑尝试删除。


## 登录、首次资料、个人资料与 OSS 头像上传（2026-07-17）

**目标：** 在现有用户认证、资料更新、Redis Token、Pinia 和唯一操作日志链路中，完成登录与资料页面优化、本人头像 OSS 上传及测试；仅处理用户头像，不修改数据库结构或其他业务图片。

| 阶段 | 状态 | 内容 |
|---|---|---|
| 1. 上下文与现有链路审计 | completed | 已读取规则、交接、进度、发现、登录/资料页面、UserController、UserService、User Store、UserContext、Redis Token 与 http.js；确认可在唯一用户模块内扩展。 |
| 2. OSS 安全组件与头像接口 | completed | 新增环境变量配置和可复用操作器；实现本人文件校验、上传、数据库/Redis 同步、新对象补偿与受限旧对象清理。 |
| 3. 用户页面与前端上传交互 | completed | 已优化登录和首次资料页；个人页完成资料编辑、本地 1:1 预览裁剪压缩和一次确认上传。 |
| 4. Mock 测试、构建、文档与交接 | completed | Mock OSS 测试与现有回归共 32/0/0/0；后端打包、真实前端构建均成功；指定文档已更新。 |

### 本轮边界

- 只处理用户头像：不得实现或修改商铺封面、商品主图、图片表、媒体表、批量上传或业务数据图片。
- 不修改数据库结构、不执行 SQL、不真实上传 OSS、不新增日志表、不做全局去重；不启动或停止服务，不执行 Git。
- AccessKey 仅可由 `OSS_ACCESS_KEY_ID`、`OSS_ACCESS_KEY_SECRET` 环境变量读取，日志不得记录文件内容、完整 Token、密码、验证码或完整手机号。

### 本轮错误记录

| 尝试 | 结果 | 后续处理 |
|---|---|---|
| 首次用户模块组合补丁 | `UserDTO.java` 实际将 `@Data` 与类声明写在同一行，预期上下文不匹配；补丁未应用，没有部分业务改动。 | 已读取精确内容，改为按文件拆分、以实际单行结构替换。 |
| 首次完整 Maven 回归 | 主/测试源码均已编译；受限沙箱阻断既有 Redis，且头像纯 Mock 测试的条件分支触发 Mockito 未使用预置校验。 | Mock 单元测试改为宽松预置模式；随后以相同 Q: 命令申请 Redis 网络权限重跑。 |
| 真实前端路径首次构建 | 沙箱拒绝读取 `C:\Users\28402` 元数据，Vite 未进入编译。 | 对同一构建命令授予父目录读取权限后重跑，1696 modules 构建成功。 |


## 验证码免注册登录、首次资料完善与可选密码登录交付收尾（2026-07-17）

**目标：** 不修改业务代码、数据库、SQL、Redis 登录体系或运行服务；基于实际已实现链路补齐接口、页面、进度、发现、交接和本计划文档，并在指定真实路径完成回归构建验证。

| 阶段 | 状态 | 交付与判定 |
|---|---|---|
| 1. 文档现状核对与收尾计划 | completed | 已读取实际接口、页面、计划、进度、发现与交接文档；确认本轮仅文档收尾，采用追加独立章节而非依赖不匹配标题。 |
| 2. 接口、页面与交接文档补齐 | completed | 已按每份文件的实际章节追加接口、页面、状态同步、密码规则、构建要求与人工验收记录。 |
| 3. 指定路径回归与最终记录 | completed | `Q:\backend` Maven 回归 25/0/0/0、打包成功；真实前端路径构建成功（1695 modules）；结果已写入文档，浏览器人工验收明确保留。 |

### 本轮边界

- 仅修改 `backend/API.md`、`docs/api-contract.md`、`docs/page-design.md`、`progress.md`、`findings.md`、`docs/HANDOFF.md` 和 `task_plan.md`。
- 不修改业务代码、数据库结构、SQL、Redis 登录体系、认证代码、订单或支付；不启动、停止或重启服务；不执行 Git。

### 本轮错误记录

| 尝试 | 结果 | 后续处理 |
|---|---|---|
| 首次多文件补丁 | `findings.md` 实际首行标题与假设不一致，整组补丁未应用。 | 改为先读取各文件实际首行和章节，再分别精确追加。 |
| PowerShell 首行查看 | `$file:` 被 PowerShell 解析为无效变量引用。 | 改用 `${file}` 或不带冒号的输出格式。 |
| 首次 `Q:\backend` Maven 回归 | 此独立 PowerShell 进程没有 `Q:` 临时映射，未进入后端目录且 Maven 在项目根报无 POM；0 项测试执行。 | 在同一进程内临时映射当前项目根目录到 `Q:`，进入 `Q:\backend` 后重跑，并在 finally 中解除映射。 |
| Q: 下受限沙箱 Maven 回归 | 已编译 106 个主源码和 8 个测试源码，并实际运行 25 项；沙箱拒绝连接既有 Redis，结果为 3 failures、10 errors。 | 按原命令申请受控网络权限重跑；该错误不视为业务代码失败。 |
| 真实前端路径首次构建 | Node 在 Vite 启动前被沙箱拒绝 `lstat C:\Users\28402`，未产生构建结果。 | 对相同 `npm.cmd run build` 命令申请读取父目录元数据权限后重跑。 |

## 免注册登录、首次资料完善与可选密码登录（2026-07-17）

**目标：** 仅扩展既有 UserController、UserService、AddressService、Redis Token、Pinia、Router 与 `http.js`，实现验证码免注册登录、可靠的首次资料状态、资料完善和可选密码登录；不启动服务、不执行 Git、不实现订单或支付。

| 阶段 | 状态 | 内容 |
|---|---|---|
| 1. 现有认证、资料、地址与前端状态审计 | completed | 已确认验证码登录会自动创建用户并签发既有 Redis Token；但 LoginVO/UserDTO/Redis Hash/Store 未含首次资料或密码状态，地址服务已有可复用校园校验与用户隔离。 |
| 2. `profile_completed` 只读数据库门禁 | completed | 用户已手工迁移；本轮只读确认实际列为 `tinyint(1)`、NOT NULL、默认 0，满足可靠后端状态要求。 |
| 3. 后端登录、资料完善与测试 | in_progress | 在既有服务中实现登录状态、密码登录、资料完善事务、地址复用与集成回归测试。 |
| 4. 前端登录、资料完善与资料同步 | pending | 在既有 Router/Pinia/http.js 中实现双登录 Tab、首次跳转、资料完善和导航同步。 |
| 5. 回归、构建、文档与交接 | pending | 在 Q: 短路径运行指定 Maven 测试/打包和前端构建，并同步接口、页面、进度、发现、交接和计划。 |

### 本轮硬性边界

- 不修改 Redis Token 体系、不新增认证模块或日志表，不实现订单或支付，不启动或停止服务，不执行 Git。
- 可靠状态必须持久化到后端数据；若 `profile_completed` 缺失，只生成待人工执行 SQL 并立即停止，不修改业务代码。
- 资料完善必须复用既有校园地址校验和 `qh_operate_log` 的唯一日志链路；密码只使用 BCrypt，登录失败提示不得泄露手机号是否存在。

### 本轮门禁证据

- 只读 `information_schema.columns` 显示 `qh_user` 只有 `id`、`phone`、`username`、`password_hash`、`nickname`、`avatar_url`、`gender`、`status`、创建/更新时间；没有可作为首次资料完成状态的字段。
- 未执行任何 SQL；`backend/src/main/resources/sql/profile_completion_increment.sql` 仅包含 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS profile_completed`，等待用户在 DataGrip 审核、手工执行并确认复核后才能继续第 3 阶段。

## 账号注册功能实施（2026-07-17）

**目标：** 仅在现有用户认证链路中实现并验证匿名注册：`POST /api/auth/register` 与对应 `/register` 页面；复用现有验证码 Key、用户表、拦截器、Axios 实例及异常体系，不执行 SQL、数据库结构变更、服务控制或 Git。

| 阶段 | 状态 | 内容 |
|---|---|---|
| 1. 认证栈与现状审计 | completed | 已确认注册业务、页面和集成测试均已在现有认证栈中实现；唯一差异为注册路径仍为 `/api/user/register`，本轮需迁至 `/api/auth/register`。 |
| 2. 后端注册实现与测试 | completed | 已在同一 UserController 中迁移注册映射和公开白名单至 `/api/auth/register`，保留 UserService/RedisKeys/Token 逻辑；24 项完整回归通过，注册测试 5 项通过。 |
| 3. 前端注册页接入 | completed | 既有 `/register` 页面、登录页入口、验证码倒计时、校验、防重复提交和跳转登录流程已复核；注册 API 改为复用 `http.js` 请求 `/auth/register`。 |
| 4. 回归构建与文档交接 | completed | Q: 下 `mvn clean test` 24/0/0/0、`mvn clean package -DskipTests` 成功并生成 JAR、前端 `npm.cmd run build` 成功；已更新接口契约、进度、发现和交接。 |

### 本轮硬性边界

- 仅处理账号注册；不实施账号密码登录、订单、支付或其他里程碑。
- 不修改数据库结构、不执行任何 SQL、不修改 Redis Token 逻辑、不启动或停止服务、不执行 Git。
- 注册必须复用既有认证模块与 `qh:login:code:{phone}`；成功后不自动登录且不返回 Token 或任何密码字段。

### 本轮验证记录

- 首次受限沙箱运行完整 Maven 测试时，Redis 到既有 `192.168.100.128:6379` 的连接被策略拒绝，导致 24 项中 3 个失败、9 个错误；未修改代码或服务，改用同一 Q: 命令在受控网络权限下重跑。
- 受控网络重跑 `mvn clean test` 通过 24 项（0 failure、0 error、0 skipped），其中 `UserRegistrationIntegrationTest` 5 项通过；后续 `mvn clean package -DskipTests` 和前端生产构建均通过。

仅测试、分析和生成报告；不得改业务代码、数据库结构、迁移 SQL 或运行服务。前次 Codex 性能诊断记录已在 `docs/history/2026-07-16-pre-context-compression/` 保留。

| 阶段 | 状态 | 内容 |
|---|---|---|
| 1. 基线与静态清单 | completed | 已建立实际 Controller、路由、页面和地址字段初步矩阵；订单/优惠券/探店/后台没有对应 Controller。 |
| 2. 运行环境与公开接口 | blocked | 5174、8090、3306、6379 均未监听；不启动或重启服务，只记录阻塞。 |
| 3. 受控认证与私有流程 | blocked | MySQL/Redis/后端未监听，未创建 `AUDIT_TEST_` 数据，无法实测认证、地址、购物车、隔离、Token 或 AOP。 |
| 4. 浏览器页面与 UX | blocked | 前端 5174 未监听；浏览器技能缓存路径也不可用，未伪造浏览器验收。 |
| 5. 安全、性能与地址专项 | completed | 完成源码、构建产物、路由、异常/AOP 与地址字段静态审计。 |
| 6. 报告与交接 | completed | 已创建报告并同步进度、发现和交接；本轮立即停止。 |

## 审计边界

- 不创建订单、不发起支付，不改原有数据。
- 受控测试仅使用可识别的 `AUDIT_TEST_` 标识；在报告前用返回主键和测试用户主键精确删除测试数据及对应操作日志/Redis 键。
- 不执行 `FLUSHALL`、`FLUSHDB`、`TRUNCATE`、数据库迁移或服务控制。

## 校园地址模型与迁移准备（2026-07-16）

**目标：** 在不修改地址、登录、Redis、购物车或订单业务实现的前提下，基于实际 `qh_user_address` 与代码链路，完成校园地址模型设计、兼容迁移设计、基线 SQL 文档更新和一份待人工执行的增量脚本。

| 阶段 | 状态 | 内容 |
|---|---|---|
| 1. 上下文恢复与范围确认 | completed | 已读取项目规则、交接、计划、进度、发现及地址相关设计/API 文档；未执行 Git 或服务控制。 |
| 2. 实际结构与引用审计 | completed | 已完成源码与 MySQL 8.0.34 只读复核；现表有 2 条历史地址、无校园/楼栋表，购物车与订单均为 0 条。 |
| 3. 校园地址模型与历史兼容设计 | completed | 已采用校区/楼栋 ID + 地址行配送快照；旧记录安全标记为 `HISTORICAL`，订单快照边界已登记。 |
| 4. 文档与迁移脚本 | completed | 已更新基线 SQL、四份指定文档，并生成未执行的 `campus_address_increment.sql`。 |
| 5. 静态复核与人工交接 | completed | SQL 语句首关键字、12 个字段映射、文档覆盖与 Maven 编译均通过；已列出 DataGrip 人工检查项。 |

### 本轮硬性边界

- 不执行任何 SQL；不连接写入数据库；不改业务代码、前端页面、登录、Redis、购物车或订单。
- 不删除字段、表或数据；省、市、区字段保留，新字段初期允许 `NULL`。
- 迁移脚本仅可包含 `CREATE TABLE`、`ALTER TABLE`、`CREATE INDEX` 和安全数据迁移语句；禁止 `DROP`、`TRUNCATE`、清表。
- 本轮结束条件是文档和 SQL 通过静态复核；脚本必须等待用户在 DataGrip 审核并人工执行。

### 错误记录

| 次数 | 操作 | 结果与处理 |
|---|---|---|
| 1 | 跨多个文件的首次文档/基线 SQL 补丁 | 因 `docs/database-design.md` 的匹配上下文不精确而未应用，未产生任何文件改动；后续改为逐段定位、小补丁。 |
| 2 | 增量 SQL 禁止词初检 | 检测器把外键的 `ON DELETE RESTRICT` 误判为 `DELETE` 语句；改按分号切分后的语句首关键字复核，9 条语句仅含 `CREATE TABLE`、`ALTER TABLE`、`CREATE INDEX`、`UPDATE`，通过。 |

## 校园地址功能实施（2026-07-16）

**目标：** 在用户已通过 DataGrip 手工执行 `campus_address_increment.sql` 的前提下，复核真实结构与基础目录数据，并仅实施校园地址目录只读查询、地址后端、现有地址页和测试/文档。

| 阶段 | 状态 | 内容 |
|---|---|---|
| 1. 上下文、源码与计划恢复 | completed | 已重新读取项目约束、交接、活动记录、设计/API 页面文档、地址链路、UserContext 与操作日志实现。 |
| 2. 只读数据库门禁 | completed | 已确认 1 个启用校区、4 个启用楼栋、正确关联、完整地址结构/索引/外键以及 2 条历史地址。 |
| 3. 后端与测试实现 | completed | 已新增目录只读链路、校园地址校验/快照/详情和扩展集成测试；全量测试受 Windows 中文路径编码问题阻塞。 |
| 4. 前端地址页改造 | completed | 已在现有地址页/API 内完成校园表单、目录状态反馈和历史地址转换提示。 |
| 5. 构建、文档与交接 | completed | 已更新指定文档；前端构建和跳过测试源码的后端打包成功，指定 Maven 测试/打包命令均因测试编译路径编码失败。 |

### 本轮边界

- 不执行迁移 SQL、DDL、目录初始化 SQL 或任何数据库写操作；不启动、停止或重启服务；不执行 Git。
- 仅处理校园地址；不实现订单、支付、校区/楼栋后台管理，不改 Redis 登录、购物车或既有前端基础设施。
- 如果目录表结构、地址字段/索引/外键与迁移设计不一致，或目录基础数据为空，立即记录结果并停止等待用户处理。

### 本轮门禁结果

- `qh_campus`、`qh_building`、`qh_user_address` 的列定义、命名索引及 `fk_qh_building_campus`、`fk_qh_address_campus`、`fk_qh_address_building` 均与迁移设计一致。
- `qh_campus` 与 `qh_building` 都是 0 条记录，无法为校园地址提供合法可选项；未实施 Java/Vue/API/测试，未运行 Maven 或前端构建。
- 已新增仅供用户审核、DataGrip 手工执行的 `backend/src/main/resources/sql/campus_catalog_init.sql`；它只插入不存在的演示目录数据，不更新或删除任何既有数据。

### 本轮续办门禁（用户确认目录初始化后）

- 只读复核结果：启用校区为 `QH_MAIN/青禾主校区`；启用楼栋为 4 条，均关联该校区；`HISTORICAL` 地址为 2 条；不存在关联到无效校区的启用楼栋。
- 本轮不执行 SQL 或变更数据库结构，只在现有地址模块内实现校园地址。

### 错误记录

| 次数 | 操作 | 结果与处理 |
|---|---|---|
| 1 | PowerShell 中使用 Bash 风格花括号路径 | 命令在解析阶段失败，未读写项目文件；已改用 PowerShell 数组和单一路径检索。 |
| 2 | 相关 Java 文件筛选无匹配导致 `rg` 返回 1 | 目标源码已在前段逐个读取，后续不依赖该筛选命令。 |
| 3 | 首次替换地址测试的补丁缺少 `*** Begin Patch` | 补丁未应用，随后以正确补丁格式成功写入。 |
| 4 | `mvn test` 与 `mvn clean package -DskipTests` | 测试编译将工作区中文路径转为乱码，`target/classes` 无法加入测试 classpath，所有测试均无法解析主包；主源码编译与跳过测试源码打包成功。 |
| 5 | PowerShell 未转义 `-Dmaven.test.skip=true` | `$maven` 被展开导致 Maven 将 `.test.skip=true` 视为生命周期阶段；用单引号传参后打包成功。 |
| 6 | 静态复核再次使用 Bash 风格花括号路径 | PowerShell 解析失败，未执行读取或写入；已改用多个明确目录参数完成复核。 |

## Maven 测试编译与校园地址验证（2026-07-16）

**目标：** 仅解决 Maven 测试编译链路，运行完整回归并据实确认校园地址模块；不改变正常业务逻辑、数据库结构、Redis 登录或服务状态。

| 阶段 | 状态 | 内容 |
|---|---|---|
| 1. 上下文与测试清单恢复 | completed | 已读取规则、交接、进度、发现、构建文件及全部测试文件清单；确认地址测试含带标识的精确数据库与 Redis 清理。 |
| 2. 编译失败证据采集 | completed | 已确认 Maven 3.9.11/JDK 21.0.9、UTF-8 Maven 平台与 CP936/gb2312 Windows 默认编码并存；调试 classpath 含 `target/classes`，但中文路径在 Maven→`javac` 交接中失真。 |
| 3. 最小修复与复核 | completed | 不改 POM 或业务逻辑；ASCII 路径暴露唯一真实测试缺陷：地址测试少传 `deliveryPoint`，已补足空字符串参数。 |
| 4. 完整回归与打包 | completed | 在临时 `Q:` ASCII 映射下，`mvn clean test` 为 24/0/0/0；随后 `mvn clean package -DskipTests` 成功生成 Spring Boot JAR。 |
| 5. 文档交接与停点 | completed | 已更新 `progress.md`、`findings.md`、`docs/HANDOFF.md`；本轮结束，等待用户审核。 |

### 本轮边界

- 不执行迁移 SQL、DDL 或目录初始化 SQL；不启动、停止或重启服务；不执行 Git。
- 不以 `maven.test.skip=true`、删除测试、排除测试或禁用测试解决问题。
- 若必须从纯英文路径执行，只提供用户命令或 `subst` 映射建议；不移动或复制项目。

### 错误记录

| 次数 | 操作 | 结果与处理 |
|---|---|---|
| 1 | `mvn test-compile -Dmaven.compiler.useArgFile=false` | PowerShell 将未加引号的 `$maven` 当作变量展开，Maven 收到 `.compiler.useArgFile=false` 并报未知生命周期；未进入编译。后续统一用单引号传递 `-D` 参数。 |
| 2 | ASCII 路径下的完整 `mvn clean test` | 已穿透中文路径 classpath 问题并编译到真实测试错误：`AddressIntegrationTest` 第 176 行调用少传 `deliveryPoint`。补足空字符串参数，未删改测试断言或业务代码。 |
| 3 | ASCII 路径下复跑完整测试与指定打包 | `mvn clean test` 通过 24 项（0 failure/error/skipped）；`mvn clean package -DskipTests` 成功。 |

## 英文路径迁移验证（2026-07-16）

**目标：** 仅验证用户指定的英文路径 `C:\ruanzhu\workplace\qinghe-life-service` 是否可作为项目工作副本，并在该副本内检查旧中文绝对路径、执行完整后端测试/打包和前端构建；不开发功能、不改数据库或服务状态。

| 阶段 | 状态 | 内容 |
|---|---|---|
| 1. 路径与上下文门禁 | completed | 指定英文路径经 `Test-Path` 确认为不存在；当前可访问副本仍位于 `C:\Users\28402\Desktop\ruanzhu\workplace\qinghe-life-service`。 |
| 2. 旧路径引用扫描 | completed | 仅命中交接中的旧 Maven 指令和一条归档历史；仅修复前者。 |
| 3. 指定构建验证 | blocked | 英文目标目录不存在，三条构建命令无法执行；未改在旧路径执行。 |
| 4. 记录与交接 | completed | 已更新 `progress.md`、`findings.md`、`docs/HANDOFF.md`，本轮立即停止。 |

### 本轮边界

- 不创建、复制或移动项目目录；不启动、停止或重启 IDEA、Vite、MySQL、Redis 或浏览器。
- 不执行 Git、SQL、数据库结构修改或业务逻辑修改。
- 英文目标目录缺失时，构建命令不得改在旧路径执行并冒充迁移验证结果。

### 本轮错误记录

| 次数 | 操作 | 结果与处理 |
|---|---|---|
| 1 | 对固定运行配置文件清单做旧路径检索 | `application-dev.yml` 与两个 `deploy` 启动脚本在当前副本不存在，`rg` 返回缺失文件提示；未重试该固定清单。此前全项目文本扫描已确认构建和运行配置没有旧中文绝对路径。 |

## 实际英文工作目录迁移验证（2026-07-16）

**目标：** 仅在已确认存在的 `C:\Users\28402\Desktop\ruanzhu\workplace\qinghe-life-service` 运行完整后端测试、后端打包和前端生产构建；不开发功能、不修改数据库或服务状态。

| 阶段 | 状态 | 内容 |
|---|---|---|
| 1. 工作目录与记录恢复 | completed | 当前 PowerShell 工作目录与用户确认的项目根目录完全一致；已读取 AGENTS、交接、进度、发现和活动计划。 |
| 2. 后端完整测试 | failed | 三种 Maven/JDK 编译器执行方式均在 testCompile 解析主类 classpath 失败，0 个测试方法运行；未改业务或测试。 |
| 3. 后端打包与前端构建 | completed | Maven 打包仍因 testCompile 失败；前端生产构建成功，生成 `dist`。 |
| 4. 记录与停点 | completed | 已同步 `progress.md`、`findings.md`、`docs/HANDOFF.md`，本轮立即停止。 |

### 本轮边界

- 不访问或检查 `C:\ruanzhu`；不启动、停止或重启任何服务。
- 不修改业务逻辑、数据库结构、SQL、Redis 配置或测试策略；若测试失败，仅依据真实输出实施最小必要修复。

### 本轮错误记录

| 次数 | 操作 | 结果与处理 |
|---|---|---|
| 1 | 当前实际英文目录中的 `mvn clean test` | 主源码 104 个文件编译成功；7 个测试源文件在 testCompile 无法解析 `com.qinghe.life.*` 主包，0 个测试方法运行。下一步采集 Maven 详细 classpath，不重试相同命令。 |
| 2 | `mvn -X test-compile` 与强制 `forceJavacCompilerUse` 完整测试 | 调试确认 classpath 明确包含现存 `target/classes`；替代编译器模式仍报同一主包缺失。下一步仅切换为 fork 外部 `javac` 验证工具链，不改业务或测试。 |
| 3 | 强制 fork 外部 `javac` 的完整测试 | 外部 `javac` 同样无法解析已存在且位于 classpath 的主包。停止测试编译器参数尝试，不实施未经验证的 POM/业务改动。 |
| 4 | `mvn clean package -DskipTests` | `-DskipTests` 不跳过 testCompile，因同一主类 classpath 失败而未生成 JAR。 |
| 5 | 受限沙箱中的前端构建 | Node 首次 `lstat C:\Users\28402` 返回 EPERM；经用户授权读取父目录元数据后，使用相同命令成功完成生产构建。 |

## Q: 短路径 testCompile 对照（2026-07-17）

**目标：** 仅在 Q: 短路径复现并比较 Maven testCompile，不开发新功能，不修改业务逻辑、数据库、SQL 或服务状态。

| 阶段 | 状态 | 内容 |
|---|---|---|
| 1. Q: 映射与上下文 | completed | Q: 初始未映射；已确认临时映射有效但只在创建它的 PowerShell 进程内可见，后续 Maven 将在同一进程内创建映射并运行。已读取构建配置、交接和 7 个测试文件清单。 |
| 2. Q: Maven 验证 | completed | Maven 3.9.11/JDK 21.0.9；`mvn clean test` 为 24/0/0/0，`mvn clean package -DskipTests` 成功并生成 JAR。 |
| 3. 差异诊断或最小修复 | completed | Q: 的 7 个测试源文件成功完成 testCompile，无需 POM、业务或测试修改；根因已收敛为 Windows 长路径 testCompile classpath/参数解析问题。 |
| 4. 记录与停点 | completed | 已同步 `progress.md`、`findings.md`、`docs/HANDOFF.md`，本轮立即停止。 |

### 本轮边界

- 仅创建并使用临时 Q: 短路径映射；不修改业务、数据库、SQL、服务或测试策略。
- 若 Q: 通过，根因记录为当前 Windows 长路径下 Maven testCompile classpath/参数解析问题，不做代码改动。

### 本轮错误记录

| 次数 | 操作 | 结果与处理 |
|---|---|---|
| 1 | 以 Q: 作为受限执行器的直接工作目录 | 执行器创建进程返回错误 267；改为在已允许的工作目录启动 PowerShell，并在同一进程内创建 Q: 后 `Set-Location Q:\backend`，不重复直接工作目录方式。 |
| 2 | 跨工具调用复用临时 Q: 映射 | 后续独立 PowerShell 进程看不到 Q:；确认映射为进程隔离状态，Maven 验证改为单进程创建映射、切换和执行。 |
| 3 | 受限沙箱中的 Q: 完整测试 | testCompile 成功并运行 24 项，但沙箱阻止 Redis 套接字连接；按授权在相同 Q: 命令下重跑，24 项全部通过。 |
# 宿舍入住与资产二维码管理：设计与迁移准备（2026-07-17）

**目标：** 仅完成复用现有校区/楼栋的宿舍、床位、学籍、资产套装、资产及入住历史模型设计，生成未执行的增量 SQL，并同步指定设计文档；不实现前后端业务、不执行 SQL。

| 阶段 | 状态 | 内容 |
|---|---|---|
| 1. 现有结构与边界核对 | completed | 已复核既有校园目录、用户、管理员、UserContext 与唯一操作日志链路；不创建重复目录表。 |
| 2. 数据模型与迁移脚本 | completed | 已定义六张新增表、历史保留、唯一约束、资产编号与二维码安全边界，并生成仅含允许 DDL 的 SQL。 |
| 3. 文档与静态复核 | completed | 已更新数据库设计、API 契约、进度、发现与交接；静态确认脚本为 1 条 ALTER TABLE 和 6 条 CREATE TABLE 后停止。 |

### 本轮硬性边界

- 仅生成 `backend/src/main/resources/sql/dorm_asset_increment.sql`，不得执行 SQL、连接写入数据库或修改现有数据。
- 不实现 Controller、Service、Mapper、实体、前端页面、管理员业务或二维码图片上传；不启动/停止服务，不执行 Git。
- 复用 `qh_campus`、`qh_building`、`qh_user` 与唯一 `qh_operate_log`；禁止新增校区、楼栋或模块日志表。

## 校园消费服务专项审计（2026-07-17）

**目标：** 仅审计商铺、商品、购物车、订单、优惠券、校园配送和店铺/商品图片。生成审计报告并同步交接文档；不修改业务代码、不执行 SQL、不实现功能、不启动或停止服务。

| 阶段 | 状态 | 内容 |
|---|---|---|
| 1. 上下文与审计计划 | completed | 已读取指定规则、交接、设计、接口、页面、进度与发现文档，建立审计证据矩阵。 |
| 2. 实际代码、SQL 与测试核对 | completed | 已静态核对各模块后端分层、前端 API/页面/按钮连接、SQL 模型和测试源码；未连接或执行数据库 SQL。 |
| 3. 报告、交接与停点 | completed | 已创建 `docs/campus-consumption-audit.md`，更新 `progress.md`、`findings.md`、`docs/HANDOFF.md`；已声明静态审计证据与数据库/运行验证缺口并停止。 |

### 本轮硬性边界

- 不修改 Java、Vue、SQL、配置或测试代码；不执行任何 SQL，不写入数据库或 Redis，不启动、停止或重启任何服务，不执行 Git。
- 数据库表仅以仓库内 SQL/设计文档和代码映射为证据；无法宣称现网实例已验证。
- 所有操作日志建议仅能复用 `qh_operate_log`，不建议新增模块日志表。

## 管理端认证与权限门禁（2026-07-17）

**目标：** 仅在既有 `qh_admin`、Redis、拦截器、AOP 操作日志和 Vue Router 基础上，实现管理员账号密码登录、独立 Token 会话和 `/api/admin/**` 门禁；不实现后台业务资源、商铺/商品/订单/图片功能，不启动或停止服务，不执行 Git。

| 阶段 | 状态 | 内容 |
|---|---|---|
| 1. 上下文与结构门禁 | completed | 已审计 `qh_admin` 实际列与索引、管理员实体/Mapper、Redis Token、拦截器、`UserContext`、路由、AOP；字段和唯一索引满足门禁，无需增量 SQL。当前既有管理员记录不含有效 BCrypt 哈希，不改动存量数据；测试将使用精确清理的临时 BCrypt 管理员。 |
| 2. 后端管理员会话与门禁 | completed | 已实现独立 `AdminContext`、Redis Key、登录/当前/退出接口、管理员拦截器与测试，不创建管理员表或日志表。 |
| 3. 前端管理员登录与守卫 | completed | 已实现 `/admin/login`、基础后台布局、独立管理员会话存储和路由守卫；不接入业务管理页面。 |
| 4. 回归、构建、文档与停点 | completed | 已运行指定后端测试/打包和前端构建，并更新 API/契约/页面/进度/发现/交接；后端完整测试受 Windows 长路径 testCompile 环境问题阻断，主代码打包与前端构建通过。 |

### 本轮硬性边界

- 只复用 `qh_admin`、唯一 `qh_operate_log`、既有 Redis 与 HTTP 基础设施；不创建第二张管理员表、模块日志表、JWT、管理员注册入口或并行 Axios 实例。
- 管理员会话必须使用独立 `qh:admin:token:{token}` Redis Hash、独立 ThreadLocal 上下文和独立拦截器；普通用户 Token 绝不能通过 `/api/admin/**`。
- 若数据库结构门禁不满足，只生成未执行的增量 SQL 并立即停止；不执行 SQL、不自动导入迁移。

### 本轮错误记录

| 次数 | 操作 | 结果与调整 |
|---:|---|---|
| 1 | `qh_admin` 只读数据库门禁参数解析 | 首次脚本假设 `username` 也是环境变量占位符，实际 YAML 为纯文本 `root`，因此在连接前终止，未执行 SQL；后续改为分别处理 URL、纯文本用户名和密码占位符。 |
# 店铺后台维护与封面 OSS（2026-07-17）

| 阶段 | 状态 | 内容 |
| --- | --- | --- |
| 1. 结构与既有链路门禁 | completed | 已只读核验 `qh_shop`、`qh_category`、单封面字段、索引、公开查询缓存、OSS 组件与管理员认证链路；实际结构满足本轮范围，无需增量 SQL。 |
| 2. 后端后台维护与封面流程 | completed | 已扩展既有店铺模块，实现管理员分页、资料/状态维护、单封面上传替换、精确缓存失效和 Mock OSS 测试。 |
| 3. 前端店铺维护与用户端同步 | completed | 已在既有 AdminLayout、管理员 Store、Router 和唯一 http.js 中实现店铺页面、封面本地预览上传和用户端封面同步展示。 |
| 4. 回归、构建、文档与停点 | completed | `Q:\backend` Maven 40/0/0/0、后端打包和真实前端构建均通过；指定文档已同步，保留真实 OSS/浏览器人工验收后停止。 |

## 边界

- 只维护 `qh_shop` 已有字段：名称、分类、地址、电话、评分、启用状态、推荐状态、排序和单张 `cover_image`；不伪造简介、营业时间或独立营业状态。
- 不新增图片表、日志表、商品/订单/优惠券/支付/配送能力；不执行 SQL、真实 OSS 上传、服务控制或 Git。
- 写操作使用 `AdminContext`、唯一 `qh_operate_log`、既有 Cache Aside 和精确 Redis Key 删除；自动化测试 Mock `AliyunOSSOperator`。

# 商品后台维护与主图 OSS（2026-07-17）

| 阶段 | 状态 | 内容 |
| --- | --- | --- |
| 1. 结构与既有链路门禁 | completed | 已只读核验 `qh_goods`、`qh_shop`、`qh_category` 的实际列、索引和图片字段；价格、库存、销售状态、店铺关联和单主图字段齐全，无需迁移。 |
| 2. 后端商品维护与主图流程 | completed | 已在既有栈中补齐商品后台查询/写入、上下架、库存、主图替换、公开端停用店铺过滤及 Mock OSS 测试；无第二套 OSS、缓存或日志体系。 |
| 3. 前端商品维护与用户端同步 | completed | 已接入 `/admin/goods`、菜单、唯一 http.js、1:1 本地主图预览和项目内占位图；库存 0 显示售罄并禁用加购。 |
| 4. 回归、构建、文档与停点 | completed | 受控网络下全量 Maven 回归曾为 43/0/0/0；新增非 goods 前缀拒删断言后定向测试为 3/0/0/0，受额度限制未能再次全量运行；后端打包与真实前端构建均成功，指定文档已同步，等待人工验收。 |

## 本轮边界

- `qh_goods` 没有独立 `category_id` 或 `sort_order`：分类筛选通过关联 `qh_shop.category_id` 实现；页面只按现有 `id`/销量等真实字段排序，不伪造可编辑商品排序字段。
- 仅处理商品资料、单张 `cover_image`、上/下架、库存与用户端同步；不改购物车业务模型，不实现订单、优惠券、支付或配送。
- 不执行 SQL、真实 OSS、服务控制或 Git；写操作复用 `AdminContext`、唯一 `qh_operate_log` 和既有 `AliyunOSSOperator`。

# 学生资料与宿舍扫码入住：实现里程碑（2026-07-17）

| 阶段 | 状态 | 内容 |
| --- | --- | --- |
| 1. 实时数据库只读门禁 | completed | 已实时复核字段、索引、外键、目录与空表状态；`ENROLLED` 为首次在读状态，`current_flag=1` 为当前、`NULL` 为历史。 |
| 2. 后端资料、解析与入住事务 | completed | 已实现本人资料、二维码解析、确认入住、有效记录并发约束、资产套装同步及脱敏宿舍查询。 |
| 3. 学生端扫码与我的宿舍 | completed | 已实现 `/dorm/scan`、`/dorm/me`、摄像头降级手动输入、首次资料表单和重复提交防护。 |
| 4. 回归、构建与契约文档 | blocked | 文档与前端构建完成；完整 Maven 测试在 testCompile 被 Windows 中文路径类路径错误阻断，未运行本模块测试。 |

## 本轮门禁证据

- 只读目标为 `qinghe_life`。`qh_student_profile` 已含 `real_name`、`contact_phone`、`student_no`、`college_name`、`major_name`、`class_name`、`student_status`、`current_flag`。用户已明确这些真实字段为本轮唯一数据库契约；不得因不存在 `college`、`major`、`academic_status`、`active_flag` 而停工或改库。
- 当前资料唯一索引正确：`uk_qh_student_profile_user_current(user_id,current_flag)`、`uk_qh_student_profile_no_current(student_no,current_flag)`；入住唯一索引正确：`uk_qh_dorm_checkin_user_active(user_id,active_flag)`、`uk_qh_dorm_checkin_bed_active(dorm_bed_id,active_flag)`。
- `qh_dorm_checkin` 到 `qh_user`、`qh_dorm_bed`、`qh_asset_set` 的外键均存在；未执行 DDL/DML 或 SQL 导入。后端主代码编译与跳过测试源码打包、前端构建已通过；完整 Maven 测试尚受环境阻断。
# 店内商品分类与商店主页优化：最终状态（2026-07-17）

| 阶段 | 状态 | 结果 |
|---|---|---|
| 1. 只读数据门禁 | completed | `qh_category`、`qh_goods_category`、可空 `qh_goods.category_id`、索引与外键均已只读复核。 |
| 2. 后端分类与关联 | completed | 分类管理、同店校验、停用历史规则、公开启用分类和测试已完成。 |
| 3. 前端管理与商店详情 | completed | 管理页、筛选语义、完整商品主图、紧凑详情页和固定购物车入口已完成。 |
| 4. 回归、构建与文档 | completed | Maven 47/0/0/0、JAR、前端构建及指定文档同步完成。 |

# 普通订单、优惠券、Redis 缓存与限时秒杀：架构审计与迁移准备（2026-07-17）

**目标：** 只审计现有普通订单、普通优惠券、查询缓存及异步基础设施；形成与限时秒杀优惠券隔离的架构设计、必要的未执行增量 SQL 和八阶段实施计划。

| 阶段 | 状态 | 内容 |
|---|---|---|
| 1. 上下文、表结构与现有链路审计 | completed | 已核对指定表、实体、Mapper、购物车链路、Redis 配置/Key、上下文与唯一操作日志；订单和优惠券均未实现业务层，未见 MQ 或 Redisson。 |
| 2. 四类业务边界与一致性设计 | completed | 已将普通订单、普通券、秒杀券和查询缓存拆分，定义数据库条件更新、Lua、Redis Stream、Pending/DLQ、幂等和锁边界。 |
| 3. 安全增量 SQL 与测试矩阵 | completed | 已生成三个未执行脚本和完整后续测试矩阵；普通券第一版维持既有一人一券约束。 |
| 4. 文档、计划与交接 | completed | 已创建设计文档并同步数据库/API/消费审计、进度、发现、交接和本计划；按要求立即停止。 |

## 本轮硬性边界

- 不实现或修改任何 Java/Vue 业务代码；不执行 SQL、不连接数据库或 Redis、不启动/停止服务、不运行测试、构建或 Git。
- 不添加 RabbitMQ、Kafka、Redis Stream、Redisson 或任何依赖；不改商店主页、购物车、支付、认证或现有 Redis Token 链路。
- 普通商品订单、普通优惠券、限时秒杀优惠券和商铺/商品查询缓存必须分开审计与设计；操作审计只复用唯一 `qh_operate_log`，不新增业务日志表。
- 迁移 SQL 仅可包含 `CREATE TABLE`、`ALTER TABLE`、`ADD COLUMN`、`ADD INDEX`、`ADD CONSTRAINT`；绝不包含 `DROP`、`TRUNCATE`、`DELETE` 或伪造历史数据，且不得自动执行。

### 本轮错误记录

| 次数 | 操作 | 结果与处理 |
|---|---|---|
| 1 | PowerShell 文件清单使用 Bash 花括号路径展开 | 在 PowerShell 参数解析阶段失败，未读写项目文件；改为显式目录参数后完成同一只读清单。 |
# 管理员入住专项测试与交付收口（2026-07-18）

## 目标

在不改数据库结构、不执行 SQL、不启停服务、不执行 Git 的约束下，完成管理员入住查询、退宿和换寝的自动化测试、完整回归、打包构建、测试数据精确清理检查与文档收口。仅在当前仓库内执行；后端验证以仓库内 `backend` 为实际目标，不能访问或改动仓库外的 `Q:\backend`。

## 当前里程碑：M1 代码与测试基线审查

| 阶段 | 状态 | 交付与验收条件 |
|---|---|---|
| M1 代码与测试基线审查 | 进行中 | 审计管理员身份来源、事务、活跃入住、资产状态、异常与脱敏；确定现有测试可复用点。 |
| M2 管理员入住专项测试 | 待开始 | 补齐查询、退宿、换寝、并发、回滚、日志与学生端联动测试，测试数据可精确清理。 |
| M3 完整回归与构建 | 待开始 | 运行仓库内 Maven `clean test`、`clean package -DskipTests` 与前端生产构建；记录真实 totals/failures/errors/skipped。 |
| M4 残留检查与文档收口 | 待开始 | 测试残留为 0，更新指定九份文档并给出人工验收步骤。 |

## 本轮边界与决定

- 不开发学籍异动、批量退宿、统计、资产报修或其他新功能。
- 不修改数据库结构，不执行 SQL，不启动或停止服务，不执行 Git。
- 所有管理员业务身份只能来自 `AdminContext`；测试不得依赖真实 OSS。

## 错误记录

| 错误 | 尝试 | 处理 |
|---|---:|---|
| 初次汇总读取输出过长且控制台中文编码显示异常 | 1 | 改为按目标文件和代码符号分段读取；文件内容与后续命令输出为准。 |
# 2026-07-18 M5 子里程碑：管理员入住查询、退宿与换寝自动化测试及交付收口

- **状态：** completed
- **范围：** 仅补齐管理员入住专项测试、执行完整 Maven 回归与打包、前端生产构建、检查测试数据残留，并收口既有接口/数据/页面/计划文档；不修改数据库结构、不执行迁移 SQL、不启动或停止服务、不执行 Git。
- **验收门槛：** 管理员入住专项测试通过；最新完整回归 0 failures/0 errors；带前缀测试数据残留为 0；Maven 打包和前端构建成功，才可标记“已完成并验证”。
- **阶段：**
  1. `completed` 恢复交接、审查现有实现与测试基线；已收紧服务层 AdminContext、响应字段和日志敏感文本边界。
  2. `completed` 在既有测试栈内补齐并通过查询、退宿、换寝、并发/回滚、学生联动与残留检查（专项 Maven：7/0/0/0）。
  3. `completed` 完整 Maven Surefire XML 汇总 68 tests、0 failures、0 errors、0 skipped；后端打包和前端构建均成功。
  4. `completed` 已更新指定 API、设计、计划、进度、发现和交接文档，并记录人工验收步骤。

## 2026-07-18 管理员宿舍资源管理修复

**目标：** 在既有 `/api/admin/dorm`、`DormAssetAdminController`、`AdminContext`、`qh_operate_log` 与管理员页面中，完成仅宿舍楼过滤、寝室/床位安全维护、资产套装五件资产展示与二维码查看；不修改学生扫码、入住、退宿、换寝或个人中心，不改库、不执行 SQL、不控制服务或 Git。

| 阶段 | 状态 | 验收要点 |
|---|---|---|
| 1. 上下文与真实结构门禁 | completed | 实时库确认宿舍楼真实值为 `宿舍楼`；六表字段、状态、唯一约束、外键均满足，`UATA` 存在但无入住历史。 |
| 2. 后端资源维护与测试 | blocked | 实现与专项测试源码已完成；专项测试在 Redis 不可连接时于管理员登录返回 503，未取得 0 failures/0 errors。 |
| 3. 管理端三级页面 | completed | `/admin/dorm` 已重构为宿舍楼→寝室→床位与资产套装，包含确认、状态和具体后端拒绝原因。 |
| 4. 回归、构建与文档 | blocked | 后端打包、前端构建、文档和精确残留检查完成；完整自动化测试仍被 Redis 外部状态阻断。 |

### 本轮硬性边界

- 不修改数据库结构、不执行 SQL、不自动修正 `UATA` 数据；错误寝室号只能通过新建的管理员编辑接口和页面处理。
- 不修改学生扫码、入住、退宿、换寝、个人中心、学籍异动、统计、报修或任何专用日志表；写操作仅复用 `AdminContext` 与 `qh_operate_log`。
- 完整测试为 0 failures、0 errors、精确测试残留为 0，且后端/前端构建都成功前，不得标记完成。

- **最终证据：** `AdminDormCheckinIntegrationTest` 7/0/0/0；`mvn -Dmaven.repo.local=Q:\.m2 clean package -DskipTests` BUILD SUCCESS（182 主源码、21 测试源码、JAR 已生成）；`D:/develop/NodeJS/npm.cmd run build` BUILD SUCCESS（1725 modules）。`@AfterAll` 精确残留断言通过；未执行 SQL、服务控制或 Git。
# 宿舍模块 MyBatis-Plus 有限整改（2026-07-18）

**本轮唯一里程碑：** 在不执行任何手工 SQL、不改变数据库结构、事务边界、并发入住规则、业务行为或 HTTP 接口契约的前提下，完成宿舍模块 Entity、Mapper、Service、XML 与测试的审计；仅将确认重复的单表 CRUD 改为 MyBatis-Plus 内置能力，并仅删除已无调用的重复 Mapper/XML。

| 阶段 | 状态 | 验收条件 |
|---|---|---|
| 1. 上下文恢复与静态审计 | in_progress | 清点宿舍 Entity、Mapper、Service、XML、Controller 与测试，建立每个 Mapper 方法的调用和 SQL 分类表。 |
| 2. 最小 MyBatis-Plus 整改 | pending | 仅替换重复单表 CRUD；保留联表、分页聚合、统计、锁、原子条件更新和删除关联检查。 |
| 3. 静态复审与 N+1 审计 | pending | 确认 Controller 未直接调用 Mapper、DTO/VO/Entity 分离未变、删除内容无调用点，并记录 N+1 风险。 |
| 4. 宿舍专项测试与残留证据 | pending | 按现有测试命名运行宿舍专项测试；不运行手工 SQL，残留结论仅基于测试清理断言/测试输出。 |
| 5. 完整回归与后端打包 | pending | 运行用户指定 Maven 命令；仅在完整测试为 0 failures、0 errors 时标记本里程碑完成。 |

### 本轮硬性边界
- Mapper 继续继承 `BaseMapper`；Controller 不直接调用 Mapper；DTO、VO、Entity 保持分离。
- 不为 MyBatis-Plus 将联表查询拆为循环查询，不改事务注解或入住、退宿、换寝并发规则。
- 保留楼栋/寝室/床位/资产联表与分页查询、资源统计、`SELECT ... FOR UPDATE`、条件原子更新、唯一约束兜底、删除前关联检查、复杂批写及避免 N+1 的聚合 SQL。
- 不执行手工 SQL、不修改数据库结构、不自动导入 SQL、不启停服务、不执行 Git 命令。
## 2026-07-18 宿舍模块 MyBatis-Plus 有限整改：阶段状态更新

| 阶段 | 状态 | 证据 |
|---|---|---|
| 1. 上下文恢复与静态审计 | completed | 宿舍域 8 个 Mapper 均仅继承 `BaseMapper<T>`，无 Mapper XML、无注解自定义 SQL；Controller 未检出 Mapper 直接引用。 |
| 2. 最小 MyBatis-Plus 整改 | completed | 无重复单表自定义 Mapper 方法或 XML 可替换/删除；现有简单 CRUD 已使用 MyBatis-Plus 内置方法与 Lambda Wrapper，故不做无效改动。 |
| 3. 静态复审与 N+1 审计 | in_progress | 已识别候选 N+1 调用点，待完成实体、契约和测试清理证据复审。 |
| 4. 宿舍专项测试与残留证据 | pending | 待运行。 |
| 5. 完整回归与后端打包 | pending | 待运行。 |

### 阶段 1-2 审计结论
- Mapper：`CampusMapper`、`BuildingMapper`、`DormRoomMapper`、`DormBedMapper`、`AssetSetMapper`、`AssetMapper`、`DormCheckinMapper`、`StudentProfileMapper` 均为空接口并继承 `BaseMapper`。
- XML：`backend/src/main/resources` 不存在 Mapper XML；唯一宿舍 SQL 文件是未执行的增量脚本，不属于 MyBatis 映射。
- 简单操作已使用 `selectById`、`insert`、`updateById`、`deleteById`、`selectList`、`selectCount`、`selectPage`、`LambdaQueryWrapper` 与 `LambdaUpdateWrapper`；没有自定义单表 CRUD 可迁移。
- 保留的复杂语义全部以现有 MyBatis-Plus Wrapper 表达：`last("FOR UPDATE")`、`active_flag` 条件更新、唯一约束异常兜底、关联资源删除门禁和资产初始化的幂等补写。为避免范围扩大，本轮不将其重写成 XML。
## 2026-07-18 宿舍模块 MyBatis-Plus 有限整改：静态复审与验证阻断

| 阶段 | 状态 | 结果 |
|---|---|---|
| 3. 静态复审与 N+1 审计 | completed | Entity 均以 `@TableName` 映射；Controller 仅依赖 Service；DTO/VO/Entity 分离未受影响。发现的 N+1 风险已记录，未用循环替代联表。 |
| 4. 宿舍专项测试与残留证据 | blocked | 用户指定的 `Q:\backend` 在当前会话不存在；未创建/映射 Q 盘，因其属于仓库外环境变更。 |
| 5. 完整回归与后端打包 | blocked | 与阶段 4 相同：无法在用户指定路径执行 Maven 命令。 |

### 验证阻断记录
- 已只读验证 `Test-Path Q:\backend` 返回 `False`。
- 未运行 `mvn`，未执行任何手工 SQL，未改动数据库或 Redis；因此没有可报告的宿舍专项、完整回归、打包或运行后数据残留结果。
- 宿舍测试静态基线为 26 个 `@Test`：管理员楼栋 3、管理员入住 8、管理员资源 3、资产 2、学生宿舍 10。管理员入住与学生宿舍测试具有 `@AfterAll` 的精确零残留断言；其余三类测试具有 `@AfterEach` 清理，但只有管理员楼栋和管理员资源显式断言主要表为零。运行结果仍必须以实际 Maven 输出为准。

# 2026-07-18 宿舍二维码完整交互链路修复

**本轮唯一里程碑：** 在既有学生宿舍解析与管理员资产栈内修复二维码生成、查看、摄像头扫描、本地图片识别和统一解析反馈；不改库、不执行 SQL，不修改入住、退宿、换寝事务，也不开发学籍异动。

| 阶段 | 状态 | 交付与验收条件 |
|---|---|---|
| 1. 上下文与依赖审计 | 进行中 | 复核学生/管理员页面、API、PNG/ZIP 输出、测试和二维码解码依赖，确定图片与摄像头失效根因。 |
| 2. 统一扫码与图片识别 | 待开始 | 三种输入进入同一安全解析；支持视频帧、PNG/JPG/JPEG/WEBP 本地解码及完整中文状态反馈，不泄露完整 Token。 |
| 3. 管理员二维码查看 | 待开始 | 受保护 PNG 动态查看、下载与批量 ZIP；关闭后释放 Object URL，不显示完整 Token。 |
| 4. 后端规则与自动化测试 | 待开始 | 解析严格校验 QR、套装、目录、五件资产和空床位条件；补齐权限、失败与回归证据。 |
| 5. 文档、构建与交接 | 待开始 | 更新指定文档；专项/完整测试均为 0 failures/0 errors，后端与真实前端路径构建成功才可标记完成。 |

## 本轮硬性边界

- 不修改数据库结构、不执行 SQL、不启停服务、不执行 Git；不修改入住、退宿和换寝事务，不开发学籍异动或新增日志表。
- 不改变 `QH-DORM-V1:{64位十六进制Token}` 格式；普通学生响应、页面、Console 和日志不得输出完整 `qrToken`。
- 图片仅在浏览器本地解码，不上传后端、OSS 或持久化；管理员图片仅来自既有受保护 PNG/ZIP 接口。

### 错误记录

| 错误 | 尝试 | 处理 |
|---|---:|---|
| 首次汇总读取超过输出上限 | 1 | 已切换为按目标文件、符号与测试分段审计；后续以实际源码和命令输出为准。 |

# 2026-07-20 学生首次建档多校区修复与 Redis 测试分层

**本轮唯一里程碑：** 在不执行 SQL、不改 Redis 地址/密码或登录逻辑、不改扫码入住、退宿和换寝事务、不执行 Git 的前提下，审计并修复学生首次建档的校区归属语义，明确 Redis 测试分层，并完成指定构建与文档收口。

| 阶段 | 状态 | 交付与验收条件 |
|---|---|---|
| 1. 结构门禁与现状审计 | completed | 已只读确认 `qh_student_profile.campus_id bigint NOT NULL` 且已索引；已审计学生链路与 Redis 集成测试。 |
| 2. 受限实现或安全停止 | completed | 首次建档改为显式校验真实启用 `campusId`，安全返回校区信息；扫码入住、退宿和换寝事务未修改。 |
| 3. 测试分层与文档 | completed | 已区分 Mockito 单元测试与必须使用真实 Redis 的集成测试，并写入接口、设计和交接文档。 |
| 4. 编译、构建与交接 | completed（验证部分阻断） | 前端生产构建成功；指定后端 Maven 命令因 `Q:\.m2` 不可访问未进入编译/打包，真实 Redis 测试待 Windows 本机执行。 |

## 本轮硬性边界

- 不执行 SQL，不以地址校区或扫码床位校区替代学生所属校区；不修改宿舍入住、退宿、换寝事务。
- 不修改 Redis 地址、端口、密码、登录链路或业务配置，不使用 Mock Redis 伪装真实集成测试成功，也不暴露 Redis 到公网。
- 不执行 Git；每个阶段结束同步 `progress.md`、`findings.md` 和本计划，完成本里程碑后停止等待审核。

## 错误记录

| 错误 | 尝试 | 处理 |
|---|---:|---|
| 初次向既有计划插入新里程碑时锚点不匹配 | 1 | 未写入任何文件；已改为使用当前文件尾部的精确上下文追加本轮计划。 |
| 读取错误的 `studentDorm.js` 路径 | 1 | 未修改任何文件；已改用真实的 `frontend/src/api/student-dorm.js`。 |
| 测试资源目录不存在 | 1 | `backend/src/test/resources` 不存在；配置只使用现有 `application.yml`，未创建替代 Redis 配置。 |
| 初次测试清理补丁缺少一个右括号 | 1 | 静态复核时发现，未运行测试；已立即修正为原有精确清理语义后再继续。 |
| 指定 Maven 本地仓库不可访问 | 4 | 初次受沙箱默认缓存写入限制；修正 PowerShell 参数引用后确认 `Q:\.m2` 无法创建/访问。指定 `compile` 与 `clean package -DskipTests` 均在该前置条件处停止；未创建盘符映射或替代缓存。 |
| 前端构建在沙箱读取配置目录失败 | 1 | 按需获准后以相同命令重试，Vite 生产构建成功；仅保留既有 PURE 注释与大 chunk 警告。 |

# 2026-07-20 管理员宿舍楼按校区管理完善

**本轮唯一里程碑：** 在既有 `/api/admin/dorm`、`AdminContext`、`qh_operate_log` 与管理员宿舍页面内，完成按校区的宿舍楼查询、新增、编辑、启停、条件删除及交付收口；不改数据库、不执行 SQL，不触及寝室、床位、资产、二维码、入住、退宿、换寝或学籍异动事务。

| 阶段 | 状态 | 交付与验收条件 |
|---|---|---|
| 1. 只读门禁与 No Data 审计 | completed | 已确认主校区有 2 栋真实宿舍楼、分校区为 0；前端与接口均传 `campusId`，No Data 的真实空态与功能缺口已区分。 |
| 2. 最小后端完善与测试 | completed（完整回归阻断） | DTO 输入边界、聚合统计、条件删除和楼栋专项均已完成；完整测试仍有范围外重复寝室断言失败。 |
| 3. 管理页面交互 | completed | 校区选择后自动加载，完成新增/编辑/启停/删除确认及无校区禁用状态，不暴露手工 ID 输入。 |
| 4. 文档、回归与交接 | blocked | 九份文档已更新，专项/构建通过；完整 Maven 回归非 0 failures，不能完成交付。 |

## 本轮硬性边界

- 仅管理真实 `building_type=宿舍楼` 的楼栋；教学楼、图书馆继续存在于通用目录，不出现在本模块。
- 不执行 SQL、DDL/DML 或自动导入；不启动、停止服务或执行 Git；不新增日志表。
- 不修改寝室、床位、资产套装、二维码、学生扫码、入住、退宿、换寝和学籍异动的规则或事务。

## 错误记录

| 错误 | 尝试 | 处理 |
|---|---:|---|
| 初始上下文汇总输出过长 | 1 | 已改为按指定文档段落、代码符号和真实数据库证据分段审计。 |

### 阶段 1 结论（2026-07-20）

- 实时只读证据与当前源码一致：`campusId` 为数值型查询参数，后端使用相同参数并固定 `building_type=宿舍楼`；分校区空列表是实际数据状态，主校区空列表才应视为缺陷。
- 当前源码已有楼栋创建、编辑、状态变更和删除端点的初步实现；阶段 2 将逐项核验 DTO 输入边界、关联统计/删除门禁、前端契约和集成测试，不能直接以端点存在判定完成。

### 阶段 2-4 结果（2026-07-20）

- 已固定创建类型、强制按启用校区查询、保留 MyBatis-Plus 基础 CRUD 与 `BuildingMapper` 聚合统计，页面和接口文档均已收口。楼栋+入住专项为 11/0/0/0；前端构建成功；后端默认缓存打包成功。
- 指定 `Q:\.m2` 不可创建。替代环境的完整 `mvn clean test` 运行 76 项，初次 2 failures/1 error，修正测试夹具后为 2 failures/0 errors；入住筛选已在专项验证通过，剩余重复寝室 409 断言要求改变既有寝室唯一性实现或数据库约束，超出本轮边界。不得标记完成。

- 当前源码的最终 `mvn clean package -DskipTests` 已再次成功（204 主源码、23 测试源码、JAR）；该结果不改变完整回归未达 0 failures/0 errors 的阻断状态。
# 2026-07-23 GitHub initialization and first push completed

- Result: `main` is initialized locally, tracks `origin/main` at `https://github.com/tzjk/qinghe.git`, and both sides resolve to `68efe71a531757dc11c6bafa2f4c18d8dcda60c5`.
- Scope: project files plus project-level `.agents` skills were committed. Maven caches, build output, frontend dependencies, and distribution output remain excluded by `.gitignore`.

# 2026-07-24 Coupon duplicate-claim display and feedback

| Phase | Status | Evidence |
|---|---|---|
| 1. Audit and contract | completed | Existing retry behavior, identity boundary, list gap, and UI gap verified. |
| 2. Backend state and focused tests | completed | Explicit claim result, bulk claimed-state mapping, retry/status/concurrency tests all passed. |
| 3. Frontend state and feedback | completed | Claimed and in-flight cards disable; success updates the card; stale duplicate result warns and refreshes. |
| 4. Verification, records, and Git | in_progress | Requested Maven test, package, and frontend build passed; final record and authorized commit/push remain. |

# 2026-07-24 Coupon seckill claim with Redis Stream

**Single milestone:** add the Redis-only, Lua-gated asynchronous claim path for explicitly marked seckill coupons while preserving the existing MySQL transaction path for ordinary coupons. No SQL will be executed automatically.

| Phase | Status | Acceptance evidence |
|---|---|---|
| 1. Baseline, schema, and contract gate | completed | Clean `feature/coupon-seckill-stream` confirmed; scope limited to coupon, Redis, specified tests/configuration, and coupon Redis design. |
| 2. Seckill design and backend implementation | partial | Core implementation was written, but it remains uncompiled and unverified until the required Maven local repository is writable. |
| 3. Focused real Redis integration coverage | blocked | The exact required Maven command stopped before compilation because `Q:\.m2` cannot be created or written in this session. |
| 4. Documentation, package, and Git handoff | blocked | Package, diff checks, commit, and push are intentionally deferred because the required targeted test has no result. |

## Non-negotiable constraints

- Only coupons explicitly classified as time-limited high-concurrency activities use Lua -> Redis Stream -> asynchronous MySQL persistence; ordinary claim remains the existing MySQL flow.
- A single request never uses a global Redisson lock. Redisson may protect only the scheduled pending-recovery task.
- MySQL remains authoritative: unique `(user_id, coupon_id)` protection, conditional stock decrement, and transaction-based idempotency are all required.
- No database SQL is executed automatically. If the unique index is missing, produce only a candidate incremental SQL file for human review.
- Do not modify Redis endpoints/passwords, run unrelated tests, or implement cache, WebSocket, reports, order-rule redesign, or delivery modules.

## Blocking evidence

- `mvn "-Dmaven.repo.local=Q:/.m2" "-Dtest=CouponOrderIntegrationTest,CouponSeckillStreamIntegrationTest" test` was executed once as required and failed before compilation with `Could not create local repository at Q:\.m2` / `Access is denied`.
- No alternative Maven repository, Redis configuration, database migration, commit, or push was attempted.

## 2026-07-24 Seckill Redis Stream consumer-group compatibility follow-up

| Phase | Status | Acceptance evidence |
|---|---|---|
| 1. Diagnose Spring Data Redis API mismatch | completed | `StreamOperations.create(...)` is absent in Spring Data Redis 2.7.18; its API exposes `createGroup(...)`, while the underlying `RedisStreamCommands.xGroupCreate(..., boolean mkStream)` supports the required empty-stream behavior. |
| 2. Apply minimal compatible group creation | completed | `CouponSeckillServiceImpl` compiles with `xGroupCreate(stream, group, ReadOffset.from("0-0"), true)`, configured `stream-key`/group values, and narrow `BUSYGROUP` recognition. |
| 3. Focused real Redis validation | blocked | The exact command reached test compilation but every integration test failed to resolve `com.qinghe.life.*` main packages, despite `mvn -DskipTests compile` and `javap` confirming the main artifacts exist. This is outside the allowed seckill-code repair boundary. |
| 4. Package and authorized Git handoff | blocked | Per requested sequence, package, diff checks, commit, and the one normal push remain deferred until the focused test succeeds. |

# 2026-07-26 V1.0 release closure

**Scope:** project-level verification, release documentation, deployment instructions, presentation material, and only demonstrated defect fixes on `chore/release-v1.0`. No new business domain, automatic SQL execution, service control, or broad refactor.

| Phase | Status | Exit condition |
|---|---|---|
| 1. Release baseline and persistent plan | completed | Branch and clean worktree were checked once; required project records were restored. |
| 2. Backend full regression and package | in_progress | Full Maven test has 0 failures and 0 errors; package succeeds after it. |
| 3. Frontend production build | pending | Requested Vite build completes; warnings are classified. |
| 4. Read-only DB, Redis, permission, and secret audits | pending | Source and live-schema evidence is recorded; any gap yields only candidate manual SQL or a narrowly scoped fix. |
| 5. Release documentation and manual acceptance checklist | pending | README, API, handoff, database/deployment, acceptance, and presentation material are current and credential-safe. |
| 6. Final diff, up to two commits, and one push | pending | Diff checks are clean, generated artifacts are excluded, commits are created, and one normal push is attempted. |

## Release constraints

- The initial `git branch --show-current` and `git status -sb` checks have already been performed exactly once; do not repeat them until the explicitly requested final Git closeout.
- Tests may clean only their own uniquely prefixed fixtures; Redis/MySQL global cleanup, `TRUNCATE`, unconditional `DELETE`, and automatic migrations remain prohibited.
- A failed release verification may be repaired only when the root cause is demonstrated and the fix stays within release scope. Every rerun and failure must be recorded in `progress.md` and `findings.md`.
