# 宿舍基础档案与资产二维码（2026-07-17）

## 2026-07-19 批量毕业、退宿与二维码管理（实现待完整回归）

- 已在既有 `/api/admin/dorm`、`AdminContext`、`qh_operate_log` 和资产套装栈中实现预览/二次确认/100 条上限的批量毕业、退宿、资产释放、二维码停用和二维码轮换；没有新增表或 SQL。
- 退宿内核统一锁定有效入住和套装，关闭历史并释放套装。资产释放拒绝任何当前入住；二维码停用只改 `qr_status`，轮换只在空闲且无当前入住时写入新令牌/轮换时间，不清空令牌、不改套装编号。
- 管理端学生学籍页和宿舍资产页已经接入批量交互。后端 `compile` 与前端生产构建通过；完整 `clean test` 受 Redis 连接失败阻断，验证仍未完成。

## 2026-07-18 校区与楼栋管理（完成并验证）

- 用户手工迁移后的 `qh_building.remark` 已只读复核，应用未执行 SQL。管理员可查看已有启用校区、新增/编辑/启停楼栋；所有写操作沿用管理员门禁、`AdminContext` 与唯一 `qh_operate_log`。
- 楼栋编码按同校区唯一且规范化为大写。只要存在寝室、床位或资产套装即冻结编码；名称、类型、区域、备注仍可编辑。当前入住存在时不能停用，停用不会删除历史资源，重新启用恢复目录使用。
- 寝室新增只按 `buildingId` 推导真实校区，兼容输入的 `campusId` 被忽略。原床位、资产套装、二维码、学生扫码、管理员入住/退宿/换寝均保留。
- 验证：楼栋专项与既有宿舍资产专项 5/0/0/0；完整 Maven 71/0/0/0；JAR 打包和真实前端路径 Vite 构建均成功。本轮标识数据残留为 0。

## 2026-07-18 校区与楼栋管理续办：结构阻断

- 已只读确认 `qh_building` 使用 `(campus_id, building_code)` 作为楼栋编码唯一范围，资产套装新编号由现有服务按 `building_code + room_no + '-' + bed_no` 生成；楼栋、寝室、床位目录均使用 `status` 启停，当前入住以 `qh_dorm_checkin.active_flag=1` 识别。
- 真实表缺少本轮要求的 `remark` 列，且 `area` 是校园区域，不能用于持久化楼栋备注。请由授权人员在 DataGrip 审核并手工增加可空 `remark VARCHAR(255)` 后重新进行只读门禁；本次未改代码、未生成/执行 SQL、未改变资产编号、二维码或入住历史。

## 2026-07-18 管理员入住编译基线复核

- `previous_checkin_id` 是换寝新入住记录指向原入住记录的内部历史关联，实体和换寝事务继续持久化，历史不删除。
- 当前 `AdminDormCheckinServiceImpl` 没有对 `AdminDormCheckinVO.setPreviousCheckinId(Long)` 的调用，VO 不新增虚假字段；这是方案 B，管理员和学生端均不暴露该内部关联。
- 指定主源码编译成功；管理员入住专项 7 项通过；完整回归 68/0/0/0 通过。

## 2026-07-17 学生资料与扫码入住测试收口（完成）

- 本轮未改数据库结构、未手工执行 SQL、未新增日志表、未控制服务或执行 Git；仅完善学生端公开响应与集成测试证据。
- `currentFlag` 保留在后端当前学籍查询中，不在 `StudentProfileVO` 或任何学生端响应中序列化。二维码和宿舍响应同样不泄露令牌、内部 ID、`activeFlag` 或其他学生资料。
- 全量 Maven 回归 56/0/0/0，包含学生宿舍 9 项测试；后端 JAR 和前端生产构建均成功。测试仅创建并精确清理 `STUDENT_DORM_TEST_` 标识数据与对应 Redis 登录 Key。

## 学生资料与宿舍扫码入住（2026-07-17）

### 实名资料迁移准备（本轮）

**范围：** 仅复核 `qh_student_profile` 并生成待 DataGrip 人工执行的实名/联系电话增量 SQL；不执行 SQL，不修改其他表，不实现任何学生端或宿舍业务代码，也不运行构建。

| 阶段 | 状态 | 验收要点 |
|---|---|---|
| 1. 实际结构与数据只读复核 | completed | 应用与实际库均为 `qinghe_life`；表为 0 行，列/索引/外键/字符集正确，当前无 StudentProfile 实体。 |
| 2. 安全增量脚本 | completed | 已生成仅含两项 `ADD COLUMN` 的人工迁移，两个字段无默认值且为 `NOT NULL`，附执行前后复核 SQL 注释。 |
| 3. 文档与交接 | completed | 已同步数据库设计、计划、进度、发现和交接；未改变任何业务接口实现状态。 |

### 本轮结论

- `qh_student_profile` 为 InnoDB / `utf8mb4_0900_ai_ci`，当前 0 行；保留其 `user_id`、`student_no`、院系班级、`student_status`、`current_flag`、四项索引和三项外键。
- 新脚本为 `backend/src/main/resources/sql/student_profile_identity_increment.sql`。它不执行、不包含 DML 或其他表变更；由用户在 DataGrip 审核、手工执行并复核后，才可恢复学生资料与入住功能实施。

### 本轮续办计划（用户已在 DataGrip 手工执行实名字段增量）

**目标：** 只在既有用户认证、宿舍资产、二维码、操作日志、Vue Router、Pinia 与 `http.js` 链路内，实现学生实名资料、二维码解析、确认入住和“我的宿舍”；不执行 SQL、不改数据库结构，也不实现换寝、退宿、学籍异动、批量操作、报修或统计。

| 阶段 | 状态 | 验收要点 |
|---|---|---|
| 1. 只读续办门禁 | blocked | 真实库仍缺 `real_name`、`contact_phone`；有效唯一约束正确，但字段门禁不通过，已立即停止。 |
| 2. 后端资料与入住事务 | pending | 在唯一学生入口中实现资料、解析、确认入住、我的宿舍和敏感信息保护，并补齐测试。 |
| 3. 学生端页面 | pending | 实现 `/dorm/scan`、`/dorm/me`，复用唯一 HTTP、用户 Store 与路由。 |
| 4. 文档、回归与构建 | pending | 更新指定文档，运行用户指定 Maven 与前端构建，记录人工验收步骤。 |

### 续办门禁记录

- 用户声明已在 DataGrip 手工执行 `backend/src/main/resources/sql/student_profile_identity_increment.sql`；本轮只允许使用只读查询复核，绝不自动执行或导入 SQL。
- 本次续办开始时，文件清单仅有早期 `dorm_asset_increment.sql`，没有上述实名字段脚本；现已基于本轮只读证据重新生成同名人工迁移文件。
- 真实库只读结果：`qh_student_profile` 仍只有 `student_no`、校区/院系/班级、`student_status` 和版本字段，没有 `real_name`、`contact_phone`；故与本轮学生资料字段要求不一致。`uk_qh_student_profile_user_current(user_id,current_flag)`、`uk_qh_student_profile_no_current(student_no,current_flag)` 以及入住两条有效记录唯一索引均存在且正确。字段门禁失败后未继续审计 `UserContext`、Redis Token 或源码模块，也未写业务代码。

| 阶段 | 状态 | 验收要点 |
|---|---|---|
| 1. 只读结构门禁 | completed | `qh_dorm_checkin` 的学生和床位有效记录唯一约束、字段、状态和外键均已复核通过。 |
| 2. 既有链路审计 | blocked | `qh_student_profile` 缺少实名和联系电话持久化字段；在“不改库”条件下不能忠实实现用户资料规则与版本历史。 |
| 3. 后端与测试 | pending | 等待经用户确认的结构变更后实施。 |
| 4. 用户端页面 | pending | 实现扫码/降级输入、资料表单和我的宿舍页面。 |
| 5. 文档与构建 | pending | 更新指定文档并运行后端/前端构建。 |

- 严格不改数据库、不执行 SQL、不实现换寝、退宿、学籍异动、批量操作、报修、统计、订单或支付。

### 本轮阻断

- 当前 `qh_student_profile` 仅有 `student_no`、`campus_id`、`college_name`、`major_name`、`class_name`、`student_status` 和版本字段，缺少 `real_name`、`contact_phone`。`qh_user.nickname` 是展示昵称，`qh_user.phone` 是登录手机号，二者均不能安全替代学生实名/联系电话，也不能支持要求的“学生仅修改联系电话、管理员维护实名”的版本模型。
- 结构虽可保证有效入住唯一性，但无法完整、无歧义地实现第一项学生资料需求。按用户禁止改库和不执行 SQL 的限制，本轮未写业务代码、未创建伪字段或覆盖历史记录。

**目标：** 在既有管理员认证、目录维护、AOP 操作日志与 Vue 管理端中，实现宿舍/床位目录和每床位唯一资产套装二维码；不改库、不执行 SQL、不实现入住、学籍、订单或支付。

| 阶段 | 状态 | 验收要点 |
|---|---|---|
| 1. 上下文和只读数据库门禁 | completed | 已逐项复核 `building_code` 以及六张宿舍资产表、索引和外键，与迁移脚本一致。 |
| 2. 既有栈审计与实现设计 | completed | 已确认需在既有 Token/拦截器内补足管理员会话，不可把普通用户 Token 当作管理员身份。 |
| 3. 后端与测试 | completed | 已实现并通过 34 项全量回归。 |
| 4. 管理端页面 | completed | 已增加管理员宿舍基础管理页和独立管理员登录态。 |
| 5. 文档、回归和构建 | completed | 指定文档已更新；Q:\\backend 打包及真实前端路径构建成功。 |

### 本轮约束

- 仅使用用户已手工执行的 `dorm_asset_increment.sql` 形成的真实结构；绝不执行 SQL 或修改数据库结构。
- 所有管理写操作复用唯一 `qh_operate_log`，不新增日志表；二维码仅动态输出 PNG，不上传 OSS。
- 阶段 1 出现表、列、索引或外键不一致即停止，向用户报告，且不进入编码。

### 错误记录

| 次数 | 操作 | 结果与处理 |
|---|---|---|
| 1 | 首次计划文件补丁 | 原计划首行不是预期的单独 `#`，补丁未应用；未改动任何文件。已改用本轮独立计划文件，保留现有历史计划。 |
| 2 | `Q:` 编译预检 | 临时 PSDrive 清理时仍处于 `Q:` 当前目录，出现“Access is denied / 无法删除驱动器”；没有获得可确认的 Maven 编译输出。后续改用真实后端路径完成预检，并在完整验证前修正为 Push/Pop 后再移除映射。 |
| 3 | `Q:\\backend mvn clean test` | 修正映射生命周期后，主代码编译完成，但测试编译在临时盘符 classpath 下无法解析全部 `com.qinghe.life` 主类包，未进入任何测试。改用真实后端路径复核，不将此误报为测试失败。 |

### 2026-07-17 学生资料与宿舍扫码入住：迁移后实施门禁

- 本轮已按用户要求只读复核真实 `qinghe_life`。`real_name`、`contact_phone` 已存在，资料和入住的复合唯一索引、入住关联外键均正确。
- 用户已明确真实字段 `college_name`、`major_name`、`student_status`、`current_flag` 即为本轮契约；Java/接口使用对应 camelCase 名称，不增加别名列，也不改数据库结构。
- 门禁已恢复为继续复核：须确认 `student_status` 现有取值及 `current_flag` 的当前/历史规则，并确认入住有效唯一约束、外键、用户 Token 与既有二维码链路；其余实现尚未开始，未执行 SQL、服务控制、Git 或构建。

### 学生端实现记录（2026-07-17）

- 已实现本人学生资料、二维码解析、确认入住和我的宿舍接口及 `/dorm/scan`、`/dorm/me` 页面，复用既有 Token、Router、Pinia、`http.js`、资产二维码和唯一操作日志链路。
- 床位占用仍以有效入住唯一约束表达，床位目录 `status` 不被占用覆盖；资产套装由 `AVAILABLE` 更新为 `OCCUPIED`。未执行 SQL、服务控制或 Git。

### 2026-07-18 管理员宿舍资源管理修复（验证阻断）

- 已在既有管理员宿舍栈实现宿舍楼过滤、寝室/床位编辑启停条件删除、资产套装五件资产详情与资产状态/备注编辑；`UATA` 在无床位/资产时可通过编辑改为 `101`，不执行 SQL 修正。
- 只读门禁确认 `宿舍楼` 是真实类型值；资产类型固定为 `BED/BED_BOARD/DESK/WARDROBE/STOOL`，资产状态只允许 `NORMAL/REPAIR/SCRAPPED`。未修改学生扫码、入住、退宿、换寝、个人中心或数据库结构。
- 本轮新增专项测试能完成编译，但 Redis `192.168.100.128:6379` 不可连接，管理员登录返回 503，实际测试为 3 failures、0 errors，不能作为业务完成验证。后端打包与前端构建通过，测试标识数据残留为 0；待 Redis 恢复后先重跑专项，再重跑全量测试。

### 2026-07-18 管理员入住查询、退宿与换寝测试收口（已完成并验证）

- 范围仅含管理员查询、单个退宿、单个换寝、并发/回滚、学生端联动、测试残留和交付验证；未实现学籍异动、批量退宿、统计或资产报修，未改库、未执行 SQL。

### 2026-07-19 学籍异动、毕业与批量宿舍操作审计（仅设计）

- 后续统一使用当前状态 `ENROLLED`、`SUSPENDED`、`DROPPED`、`GRADUATED`；转专业和复学分别是异动类型，完成后当前状态为 `ENROLLED`。`current_flag=1` 和 `qh_dorm_checkin.active_flag=1` 的当前/历史语义保持不变。
- 休学可选择仅变更学籍或同事务退宿；退学、毕业必须同事务关闭当前入住、记录原因/时间/管理员并释放原套装，床位目录状态不变。复学不恢复旧床位或旧入住。
- 批量退宿、资产释放与二维码停用/启用/轮换全部复用管理员宿舍栈和唯一操作日志：不物理删除入住、不清空 Token、不强制释放仍有当前入住的套装。批量毕业使用预览、二次确认、100 人上限和全事务回滚；本轮未实现、未生成或执行 SQL。
- 验证：管理员专项 `AdminDormCheckinIntegrationTest` 为 7/0/0/0；完整 Maven Surefire XML 汇总 68/0/0/0；后端 `clean package -DskipTests` 成功生成 JAR；前端生产构建成功（1725 modules）；测试内 `@AfterAll` 残留断言为零。

### 2026-07-20 管理员宿舍楼按校区管理（实现完成，完整回归阻断）

- 实现限定在 `/api/admin/dorm` 与 `/admin/dorm`：按校区查询、固定真实类型的新增、受资源约束的编辑、启停和条件删除；未改动寝室、床位、资产、二维码、入住、退宿、换寝和学籍异动规则。
- 楼栋页面统计使用 `BuildingMapper.selectDormBuildingStats` 聚合查询；基础楼栋插入/更新/删除使用 MyBatis-Plus。校区/类型/关联检查在 Service 内，Controller 不直接调用 Mapper。
- 专项 `AdminDormBuildingIntegrationTest` 加入过滤、创建、编码冻结、启停、条件删除和零残留覆盖；与 `AdminDormCheckinIntegrationTest` 联合实测 11/0/0/0。前端 Vite build 成功（1731 modules），后端默认缓存 `clean package -DskipTests` 成功（204 主源码、23 测试源码）。
- 不得标记完成：指定 `Q:\.m2` 不可创建；替代环境完整 `mvn clean test` 为 76 项、2 failures、0 errors，后续已修复入住筛选夹具，剩余 `DormAssetIntegrationTest` 重复寝室应为 409 但实际为 200。该问题涉及既有寝室唯一性规则，本轮不放宽断言或擅自改变寝室业务；需获得范围授权后才能将全量回归收口为 0 failures/0 errors。
