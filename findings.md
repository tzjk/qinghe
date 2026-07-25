# 活动发现

## 2026-07-24 优惠券基础业务与普通订单使用：实施/验证阻断

- 已生成未执行候选 `backend/src/main/resources/sql/coupon_foundation_increment.sql` 并将领域模型切换为本轮字段与集中状态枚举；脚本保留旧字段和 `(user_id,coupon_id)` 唯一索引，未删除/重命名列或自动导入 SQL。
- 已实现 `CouponServiceImpl` 的普通领取、用户券查询/延迟过期、订单锁定、支付核销和取消释放；`OrderServiceImpl` 仅从 `OrderCreateDTO.userCouponId` 读取券编号，金额仍在服务端计算。`OrderCancellationService` 在订单条件取消、库存恢复后于同一事务释放该订单锁券。
- 管理员通过 `AdminCouponServiceImpl` 和 `AdminContext` 新建、编辑未开始券、启停及读取领取/使用统计；Controller 写操作使用现有 `@OperateLog`，未新增日志表。
- 本轮指定 Maven 命令只执行一次，失败于 `Could not create local repository at Q:\.m2: Access is denied`，没有编译、Surefire 或测试计数。后端 package 因专项未通过未运行。
- 前端生产构建执行一次后失败于 esbuild 受限读取 `../../../../..`，无法解析工作区 `vite.config.js`；这是环境文件访问证据，不是前端业务断言或构建成功。
- 完整 `CouponOrderIntegrationTest` 尚未添加；在实库候选迁移未手工执行且 Maven 测试运行环境不可用时，不能伪造 18 项测试覆盖或称本轮完成。

## 2026-07-24 优惠券基础业务与普通订单使用：阶段 1 审计

- 基线：一次授权 Git 检查确认当前为 `feature/coupon-foundation`，工作区干净。
- 实库只读核查：`qh_coupon` 当前为 `id/name/coupon_type/discount_amount/threshold_amount/total_stock/claimed_count/coupon_status/start_time/end_time/create_time/update_time`；`qh_user_coupon` 当前为 `id/user_id/coupon_id/order_id/coupon_status/claim_time/use_time/create_time/update_time`，唯一索引为 `(user_id,coupon_id)`。
- 因此实库尚缺本轮状态/时间/库存语义：券缺 `available_stock/receive_start_time/receive_end_time/use_start_time/use_end_time/shop_id/per_user_limit/status`，用户券缺 `status/receive_time/lock_time/expire_time`。按约束只能生成候选人工增量 SQL，不能执行、删除或重命名旧列。
- 旧唯一索引只允许同一用户持有同一优惠券一张；本轮普通券服务将要求 `perUserLimit=1`，并让该唯一约束作为重复提交最终兜底。
- 现有订单创建已经服务端重算总额、优惠额、配送费和实付；支付采用 `id + user_id + PENDING_PAY + pay_expire_time` 条件更新，取消内核先条件取消再恢复库存/写统一日志。优惠券锁定、核销和释放应嵌入这些同一业务事务。

## 2026-07-24 订单超时取消与多实例任务锁：静态门禁

- 当前分支与干净工作区已在本轮唯一的开始检查中确认：`feature/order-timeout-lock`。
- `Order` 已映射 `payExpireTime`、`cancelTime`、`cancelReason` 与 `status`；候选迁移 `backend/src/main/resources/sql/order_lifecycle_schema_increment.sql` 提供 `(status, pay_expire_time)` 索引，但本轮不执行 SQL。
- 现有 `cancelPendingOrder` 已复用订单明细库存恢复和直接 `qh_operate_log` 写入；超时路径仍缺少 `pay_expire_time <= now` 的条件更新、每笔独立事务和可配置批量。
- 现有 `OrderPaymentTimeoutTask` 仅硬编码 cron 并直接触发 Service；项目尚无 Redisson 依赖或客户端配置。
- 模拟支付已使用 `PENDING_PAY` 条件更新，但需要把支付截止时间也加入原子更新，才能与超时取消共享最终并发保障。
- 实现与专项验证完成：`OrderCancellationService` 是用户/超时取消共用的资源释放内核；`OrderTimeoutCancelService` 逐批扫描并通过跨 Bean 调用确保每笔 `REQUIRED` 事务。指定订单测试为 20/0/0/0，真实 Redis/Redisson 锁测试已执行；跳过测试打包成功。

## 2026-07-19 学籍异动、批量毕业与二维码批量管理：实施前恢复

- 本轮实施以 `docs/academic-dorm-batch-audit.md` 为业务设计依据，但现有代码、实体、Mapper、测试和本轮命令输出仍是最终事实来源；此前审计不是实施或验证完成的证据。
- 当前文档明确要求扩展既有管理员与宿舍资源入口：所有读写接口由 `AdminContext` 保护，写操作继续使用唯一 `qh_operate_log`；不新增结构、SQL、日志表、平行 Controller、HTTP、Store 或认证链路。
- 本轮尚待源码核实的风险点包括：学生资料版本复制的全部字段和锁定方式、退宿事务可安全抽取的内核、批量预览的防重复提交契约、资产状态的条件更新、以及前端管理员菜单/路由的现有组织方式。
- 已核实：`AdminDormCheckinServiceImpl.checkout` 采用当前入住和资产套装的 `FOR UPDATE`，再关闭记录并将套装置为 `AVAILABLE`；新学籍处理可在同一 Service 中使用等价锁定/关闭内核。当前 `StudentDormServiceImpl` 的扫码许可集合含过时 `TRANSFER_MAJOR/REINSTATED`，必须收紧为仅 `ENROLLED`。
- 实施入口已确认：`DormAssetAdminController` 是 `/api/admin/dorm/**` 的唯一宿舍管理入口；前端只有 `adminDorm.js`、`AdminLayout`、`AdminDormAssetView`、`AdminDormCheckinView` 和对应管理员路由。
- 已实现并编译：`DormCheckinLifecycle` 将锁定、关闭当前入住、释放套装抽为共享内核；学籍服务在版本切换中以 `FOR UPDATE` 锁定当前资料，历史版本设为 `NULL`，并对退学/毕业/选择退宿的休学调用该内核。批量服务以重新计算的预览令牌抵御重复或过期提交，单批硬限制 100。
- 已消除本轮查询 N+1：当前住宿筛选使用 `EXISTS/NOT EXISTS`，学生当前入住按页面记录批量读取，资产预览一次批量读取有效入住套装。资产轮换只写新随机令牌和轮换时间，不返回令牌。
- 运行验证阻断：在临时 `Q:` 短路径执行的完整测试已进入 Surefire，得到 77 tests / 14 failures / 41 errors / 0 skipped。错误主要是 Redis 不可连接、管理登录 503，未观察到本轮 Java 编译错误；项目限制禁止启动/修改 Redis，因此尚不能宣称测试通过或测试数据无残留。

## 2026-07-19 学籍异动、毕业处理与管理员批量宿舍操作：静态审计

- `qh_student_profile` 的真实模型已经具备版本保留与责任归属字段：`student_status`、`effective_time`、`end_time`、`current_flag`、`change_reason`、`operator_admin_id`；唯一键使每个用户和学号只能有一条 `current_flag=1`，历史版本使用 `NULL`。无需改变 `current_flag` 语义。
- 基线 SQL 与现有学生服务仍将 `TRANSFER_MAJOR`、`REINSTATED` 作为 `student_status`，并允许其扫码入住；本轮设计将二者定义为异动类型而不是持久状态，统一当前状态为 `ENROLLED`、`SUSPENDED`、`DROPPED`、`GRADUATED`。转专业后状态仍为 `ENROLLED`，复学后恢复为 `ENROLLED`；后续实施必须同步收紧学生端允许入住集合。
- `qh_dorm_checkin` 已可复用为历史账本：有效记录 `active_flag=1`，关闭记录为 `NULL`，且有 `checkout_time`、`checkout_reason`、`operator_admin_id`、`student_profile_id`、学籍和位置快照；`AdminDormCheckinServiceImpl.checkout` 已在事务中锁定有效入住、关闭记录并把对应套装置为 `AVAILABLE`。`qh_dorm_bed.status` 始终是目录启停，不能拿来表达占用或毕业。
- 资产与二维码无需增列支持基本安全规则：`qh_asset_set` 已有唯一且保留的 `qr_token`、`qr_status`、`qr_rotated_time`、`AVAILABLE/OCCUPIED/MAINTENANCE/RETIRED`；`qh_asset` 保持五项资产的唯一类型和健康状态。当前只有生成二维码能力，没有二维码停用、启用或轮换 API；轮换字段存在但尚未写入。
- 管理员认证只有 `qh_admin` 的统一账号/状态与 `AdminContext`；没有角色或权限表。`/api/admin/**` 由独立管理员 Token 拦截，宿舍写服务都从 `AdminContext` 获取管理员 ID。操作审计只有 `qh_operate_log`，其切面会掩码姓名、学号、Token、二维码内容、手机号和 `reason`，适合作为安全摘要日志。
- 当前单个退宿/换寝接口已存在，但没有学籍查询/异动、批量退宿、批量毕业、资产强制释放或二维码状态管理接口/DTO/VO。设计必须扩展同一 `/api/admin/dorm/**` 与管理员认证栈，不创建平行栈或新日志表。
- 本轮未连接数据库、未执行任何 SQL、未修改业务代码、未运行测试或构建、未启停服务、未执行 Git。
- 文档静态核对中，针对禁止新增日志表名的负向 `rg` 因无匹配返回退出码 1；这是预期结果，已将四个禁止表名明确写入审计文档，未将其误判为业务或文档失败。

## 2026-07-18 宿舍扫码、资产状态与中文化收口：基线

- 本轮只在既有 `DormAssetAdminController`、`DormAssetServiceImpl`、`StudentDormServiceImpl`、`/dorm/scan`、管理员宿舍页和现有测试栈内修改；不执行 SQL、结构变更、服务控制或 Git。
- 当前只发现 `backend/src/main/resources/application.yml`，未发现其他 `application-*.yml`。配置使用环境变量占位：Redis 默认 `192.168.100.128:6379`、数据库 `2`、应用键前缀 `qh:`。当前进程的 `REDIS_HOST`、`REDIS_PORT`、`SPRING_REDIS_*`、`SPRING_PROFILES_ACTIVE` 均未设置；密码值未读取或记录。
- `backend/target` 保留了 22:28 的 JAR 与编译目录，但当前没有可列出的 Surefire 报告目录；这表示本工作区没有可解析的最新 XML/TXT 产物，不能等同于测试失败，也不能用旧 503 认定 Redis 故障。
- 学生二维码解析已经强制 `QH-DORM-V1:{64位十六进制}`，且现有页面摄像头与手动输入会进入同一 `resolveDormQr` 调用；浏览器原生 `BarcodeDetector` 也可对本地 `ImageBitmap` 解析静态图片，因此可复用而无需引入第三方上传或后端处理。
- 资产初始化使用 `(asset_set_id, asset_type)` 唯一语义并在重复键后复查，已有“只补缺失核心类型”基础；但学生 `context` / `qrView` / `checkIn` 尚未要求五类核心资产均为 `NORMAL`，管理端直接显示多种英文枚举，均需在本轮修正。
- 环境变量初查的 `Get-ChildItem Env:` 被宿主重复键异常中断；改用 .NET 进程环境变量集合后完成读取，未写入任何配置。

## 2026-07-18 管理员校区与楼栋管理：启动基线

- 本轮只能扩展既有 `/api/admin/dorm/**`、`DormAssetAdminController`、管理员宿舍服务、`AdminContext` 和唯一 `qh_operate_log`，不得创建平行模块或日志表。
- 门禁必须确认真实 `qh_campus` / `qh_building` 字段、唯一范围和状态语义，以及楼栋、寝室、床位、资产套装、入住历史之间的实际关联；现有表足够时不得生成或执行 SQL。
- 在门禁结论前，不假定楼栋编码唯一范围、资产套装编号依赖关系或当前接口能力；不改动学生扫码、入住、退宿、换寝、学籍资料或个人中心。

## 2026-07-18 管理员校区与楼栋管理：真实结构门禁阻断

- 真实 `qh_campus` 具有 `id/campus_code/campus_name/status`，编码和名称为全局唯一；`status=1` 为启用。真实 `qh_building` 具有 `id/campus_id/building_code/building_name/building_type/status/area`，编码唯一范围是 `(campus_id,building_code)`，名称唯一范围是 `(campus_id,building_name)`，且对校区外键使用 `ON UPDATE RESTRICT`。
- `qh_dorm_room` 同时持有 `campus_id` 与 `building_id`，且同楼栋寝室号唯一；楼栋/寝室/床位 `status` 全为目录启停型 `TINYINT`。`qh_dorm_checkin.active_flag=1` 是当前入住，实际表还具有入住到床位、资产套装及历史自关联等外键和有效用户/床位唯一键。
- 当前 `PUT /api/admin/dorm/buildings/{buildingId}/code` 已存在但只是直接写码；生成资产套装时会用 `buildingCode + roomNo + '-' + bedNo` 组成编号，故编码保护规则必须建立在该既有逻辑上。当前库 1 个启用校区、4 个启用楼栋，宿舍/床位/资产套装/入住均为 0 行。
- 阻断：`qh_building` 没有 `remark`，而本轮必须创建、编辑并返回楼栋备注，且有下级资源时备注仍须可编辑。`area` 是校园区域，不能作为备注的隐式别名。建议仅由用户在 DataGrip 审核并手工增加可空 `remark VARCHAR(255)`，复核通过后重新从只读门禁开始；本轮未生成或执行 SQL、未修改任何业务文件。

## 2026-07-18 管理员校区与楼栋管理：续办门禁通过

- 用户声明的手工迁移已由本轮只读查询复核：`qh_building.remark` 为可空 `VARCHAR(255)`，原有 4 条楼栋完整保留；应用没有再次执行该 SQL。
- `building_code`、`building_name` 的真实唯一范围都是同校区；`qh_dorm_room` 保持 `campus_id` 与 `building_id` 双列，`qh_dorm_checkin.active_flag=1` 代表当前入住。现有资产套装服务仍按楼栋编码、寝室号和床位号生成新编号。

## 2026-07-18 管理员校区与楼栋管理：交付结论

- 新接口和页面均复用 `DormAssetAdminController`、`DormAssetService`、`AdminContext`、管理员路由/布局和 `adminDorm.js`；未创建第二套控制器、认证、日志或 HTTP 栈。
- 编码修改通过所有楼栋更新入口统一保护：无下级资源时可规范化修改；存在寝室、床位或资产套装后返回“该楼栋已生成宿舍资源，不能修改楼栋编码。”，不改写套装编号或二维码令牌。停用仅在无当前入住时允许。
- 寝室创建兼容接收但忽略客户端 `campusId`，使用楼栋真实校区。专项及完整回归均验证通过，`ADMIN_DORM_BUILDING_TEST_` 的校区、楼栋、资产、入住、用户、管理员和日志残留均为 0。

## 2026-07-18 收口结论

- `qh_dorm_checkin.previous_checkin_id` 由换寝新记录写入原入住 ID；`AdminDormCheckinVO`、管理员前端和学生响应均不使用该内部字段，方案 B 正确。
- `OrderCreateIntegrationTest` 现精确清理/断言用户、地址、店铺、商品、购物车、订单、明细、操作日志、验证码及每个测试登录 Token。专项 5/0/0/0，完整回归 68/0/0/0。
- 后端 `clean package -DskipTests` 与真实路径前端 Vite 构建成功；前端仅有第三方 PURE 注释和大 chunk 非阻断警告。

## 2026-07-17 普通订单创建链路：启动基线

- 用户已声明在 DataGrip 手工执行并复核 `backend/src/main/resources/sql/order_core_increment.sql`；本轮仍须以真实 `qinghe_life` 的只读元数据、约束和历史数据查询作为唯一门禁，绝不自动执行该脚本或其他 SQL。
- 既有审计记录表明购物车可跨店保存且前端按店铺分组，购物车金额从实时商品价格计算；订单创建必须重新读取商品与店铺，采用单店拒绝策略，并把地址/商品信息持久化为订单快照。
- 历史静态审计认为配送费尚无可配置规则。本轮只会在真实结构门禁通过后，按订单设计文档确认；若未定义规则，统一由后端计算为 `0` 并在接口和文档中说明。

## 2026-07-17 普通订单创建链路：数据库门禁阻断

- 已对 `qinghe_life` 执行只读 `information_schema` 和聚合查询，未执行 SQL 脚本或任何 DDL/DML。`qh_order.order_no` 有唯一键 `uk_qh_order_no`；`user_id`、`shop_id` 有单列和状态时间组合索引；`qh_order_item.order_id` 有索引，订单与明细快照所需的大部分字段存在，金额字段为 `DECIMAL(10,2)`。
- 阻断差异：真实订单商品金额列是 `total_amount DECIMAL(10,2)`，不存在本轮设计要求的 `goods_amount`。不得自行以代码别名掩盖这一结构差异，须由用户决定是修正数据库迁移，还是明确更新设计和全部前后端契约后重新门禁。
- `qh_order`、`qh_order_item` 的元数据估算均为 0 行，且本轮没有迁移前行数或备份基线；因此无法确认“旧订单数据未丢失”。查询引用不存在的 `goods_amount` 后返回 MySQL 1054，之后的聚合/外键检查未执行；未重试以避免违背“发现不一致立即停止”。

## 2026-07-17 学生资料与宿舍扫码入住：测试收口结论

- 当前源码的 `StudentProfileVO` 已不含 `currentFlag`；此前“公开该字段”的启动记录已被当前源码与接口序列化断言否定。该字段仅用于 `current_flag=1` 的服务端查询。
- 补充测试证明：二维码解析可识别正确前缀、64 位十六进制长度与字符集、缺失/停用令牌、不可用套装和已占用状态；返回中不含完整令牌、内部 ID 或其他学生资料。
- 入住的复合唯一索引竞争由实际并发测试保证仅一人成功；模拟 `DuplicateKeyException` 返回 409 且不会泄露约束名。资产更新异常会回滚入住记录并保持套装 `AVAILABLE`，床位目录状态保持不变。
- 2026-07-17 验证完成：全量 Maven 56/0/0/0，后端打包成功，前端生产构建成功。学生宿舍测试类的标识数据与 Redis Key 清理断言通过。

## 2026-07-17 学生资料与宿舍扫码入住：测试收口启动

- 当前仓库记录表明学生端资料、二维码解析、确认入住和我的宿舍代码已实现；本轮仅补齐脱敏、资料/二维码/事务/并发/回滚/我的宿舍测试和文档。
- 历史商品筛选 `getCategoryId()` 编译阻塞已在当前工作区修复，专用测试收口计划现进入源码与测试复核阶段；不重新排查 Q: 映射。
- 当前实现入口已定位为 `StudentDormController`、`StudentDormServiceImpl`、`StudentProfileVO`、三个请求/响应 DTO、学生资料与入住 Mapper/实体、`student-dorm.js`、`DormScanView.vue` 和 `DormMeView.vue`；下一步逐文件审计公开字段与现有测试基线。
- 已确认 `StudentProfileVO` 错误公开并复制了 `currentFlag`；`StudentDormVO` 和 `DormQrResolveVO` 当前不包含 `activeFlag`、二维码 Token 或内部 ID。现有测试目录没有学生宿舍专用测试，需新增独立覆盖。
- 服务端已按 `current_flag=1` 查询当前资料、以 `active_flag=1` 查询有效入住，并依赖入住复合唯一索引处理竞态；确认入住仅更新资产套装为 `OCCUPIED`，不会修改床位目录 `status`。现有测试基线采用带标识的真实数据库/Redis数据并在 `@AfterEach` 精确清理。
- 操作日志切面已对 `realName`、`studentNo`、`qrContent` 和 Token 字段掩码，并对电话脱敏；学生入住测试可直接断言本次标识数据对应日志不含姓名、学号、联系电话或完整二维码 Token。

## 2026-07-17 管理端商品筛选编译修复：结论

- `AdminGoodsServiceImpl` 的三个原 `AdminGoodsQuery.getCategoryId()` 调用均是为店铺类型筛选而服务：计算匹配店铺、空结果短路、`Goods.shopId IN (...)` 条件；当前均正确为 `getShopCategoryId()`。店内商品分类独立使用 `getGoodsCategoryId()` 匹配 `Goods.categoryId`。
- 商品查询 Mapper 为 MyBatis-Plus `GoodsMapper extends BaseMapper<Goods>`，没有 XML；筛选条件由服务层 `LambdaQueryWrapper` 生成。
- 管理端页面不再传查询 `categoryId`。店铺类型从 `qh_category` 读取；店内商品分类通过现有 `/api/admin/goods/categories?shopId=...` 读取，未选择店铺时禁用。保存时的 `categoryId` 仍是商品归属分类，并由服务端验证 `category.shopId == goods.shopId`。
- 指定完整测试未执行到测试方法：17 个测试源文件在 `ShopCoverServiceTest` 两处构造器参数不匹配处失败，故测试数为 0；这不是商品筛选编译错误，按本轮范围未修复。


## 2026-07-17 管理端商品筛选编译修复：初步发现

- `AdminGoodsQuery` 已拆分为 `shopCategoryId` 与 `goodsCategoryId`，当前服务层保留对不存在的 `getCategoryId()` 的调用，构成编译阻塞。
- 初步静态定位表明：`Shop.categoryId` 是店铺类型，`Goods.categoryId` 与 `GoodsCategory.shopId` 是店内商品分类关联；后续逐处确认后才会改动。

## 2026-07-17 店内商品分类与商店主页优化（审计中）

- 本轮设计基线已固定：`qh_category` 仍为店铺类型，`qh_goods_category` 才是店内商品分类；旧商品允许 `category_id = NULL`，商品改店时不能保留异店分类。
- 现有 `AdminGoodsServiceImpl`、`AdminContext`、单一 Axios 实例、购物车接口与 `qh_operate_log` 是唯一可扩展链路；不得新建平行基础设施。

### 实时只读数据库门禁（通过）

- `qh_category` 仍是全局唯一名称的店铺类型目录；`qh_goods_category` 的 `shop_id` 外键指向 `qh_shop`，并有 `uk_qh_goods_category_shop_name(shop_id,name)` 与 `idx_qh_goods_category_shop_status_sort(shop_id,status,sort_order)`。
- `qh_goods.category_id` 已存在、可为 `NULL`、有 `idx_qh_goods_category` 并以 `fk_qh_goods_category` 指向店内分类。当前店内分类为 0 条、未分类商品为 4 条，适合在不回填旧数据的前提下实现后台分配。

### 实现切入点

- 后端将新增 `GoodsCategory` 实体/Mapper/服务/控制器；商品请求与实体增加可空 `categoryId`，管理查询拆分为 `shopCategoryId` 与 `goodsCategoryId`，避免继续复用语义错误的 `categoryId`。
- 用户端商店详情将复用 `getShopGoods`、`addCart`、`getCart` 和现有登录跳转；不新建购物车业务或金额计算。`GoodsCard`、后台缩略图和预览统一改为商品图 `contain`。

### 工具记录

- 首次批量后端补丁在完成前被安全中止；已新增的分类实体/Mapper/DTO/服务/Controller 与 `Goods.categoryId` 均完整落盘，但 `AdminGoodsVO`、公开商品 VO 和服务适配尚未写入。后续将采用小补丁并逐批检查，避免遗漏或覆盖已有改动。

## 2026-07-17 商品店内分类模型与商店主页重构准备（完成，等待人工执行）

- 设计文档中的 `qh_category` 用途是服务分类，`qh_shop.category_id` 用于店铺分类和筛选；这不等同于“某个店内的商品分组”。
- 已完成的商品后台设计只支持按店铺、店铺分类、状态筛选，商品表单不含独立商品分类字段；不能把该筛选文案误判为商品已关联店内分类。
- 迁移决策尚待真实 `qh_category`/`qh_goods` 元数据、实体映射和查询实现复核。若缺口成立，仅允许生成待 DataGrip 人工执行的安全增量脚本，旧商品分类保持 `NULL`。

### 静态代码证据

- `Category` 映射 `qh_category`，字段为 `name`、`iconUrl`、`sortOrder`、`status`；`Shop.categoryId` 是唯一现存分类外键样式字段，`Goods` 只含 `shopId`、名称、简介、价格、库存、销量、上架状态和主图。
- 管理端 `/api/admin/goods?categoryId=...` 的实现先查询 `qh_shop.category_id` 匹配的店铺，再按 `Goods.shopId IN (...)` 筛选。`AdminGoodsVO.categoryId` 也由所属店铺回填，故界面“分类”实际是店铺类型筛选，当前有效但语义不能称为店内商品分类。
- 商品主图上传前端当前会把原图居中裁为正方形并以 `object-fit: cover` 预览/展示，可能裁掉主体；后端只校验输入尺寸且原样上传，未进行服务端压缩或画布适配。购物车逻辑未读取任何商品分类字段，仅依赖商品、店铺、上架状态和库存。

### 实时只读数据库门禁（`qinghe_life`）

- `qh_category` 实际字段为 `id`、全局唯一 `name varchar(64)`、可空 `icon_url`、`sort_order`、`status` 和时间列；只有主键及 `uk_qh_category_name(name)`。实际目录为“美食”“便利服务”，均被现有店铺和商品通过 `qh_shop.category_id -> qh_goods.shop_id` 间接使用，因此它是店铺类型目录，不能复用作店内商品分类。
- `qh_goods` 实际字段为 `id`、非空 `shop_id`、名称、可空简介、价格、库存、销量、上架状态、可空主图和时间列；索引仅为主键和 `idx_qh_goods_shop(shop_id)`。没有 `category_id`、店内分类唯一性或分类排序字段。
- 结论：现有结构不能满足“分类属于具体店铺、同店名称唯一、单商品可空单分类、店铺变化时分类必须清空或重选”。后续实现应使用服务端 ID 关联并在同一写入事务验证 `goodsCategory.shopId == goods.shopId`；不得信任前端分类文本。

### 交付复核

- `backend/src/main/resources/sql/goods_category_increment.sql` 已创建，包含 `qh_goods_category`、`UNIQUE(shop_id, name)`、`INDEX(shop_id, status, sort_order)`、可空 `qh_goods.category_id`、其普通索引和两个关联外键。未写入任何分类或商品数据。
- 静态检查确认脚本不含 `DROP`、`TRUNCATE` 或 `DELETE`；本轮也未执行脚本、测试、构建、服务控制或 Git。后续实施只能在用户手工执行并返回列、索引、约束复核结果后开始。

### 本轮工具记录

- 第一次向既有记录插入阶段摘要因误用 `# 项目进度记录`/`# 项目发现记录` 标题而未应用；已读取实际标题后改为精确补丁，未造成任何部分写入。
- 首次 MySQL 元数据连接未带数据库名，`DATABASE()` 为 `NULL` 并在首个 `information_schema` 查询前报 `ERROR 1046`；没有执行任何 DDL/DML。后续只读复核将显式连接 `qinghe_life`，不重复该调用形式。

## 2026-07-17 学生资料与宿舍扫码入住：实名字段迁移准备

- 连接配置和 `SELECT DATABASE()` 均确认目标为 `qinghe_life`。`qh_student_profile` 当前为 InnoDB、`utf8mb4_0900_ai_ci`、0 行，保留 `(user_id,current_flag)`、`(student_no,current_flag)` 唯一索引、两条普通索引及三条 `RESTRICT` 外键。
- 当前没有 `StudentProfile` Java 实体，也没有现成实名字段迁移。新建的 `student_profile_identity_increment.sql` 只新增 `real_name VARCHAR(50) NOT NULL` 与 `contact_phone VARCHAR(20) NOT NULL`，不设默认值或唯一索引；`contact_phone` 不等同于登录手机号。
- 选择 `NOT NULL` 的唯一依据是执行前真实表为 0 行；脚本明确要求若执行前数据不再为空则停止，改为后续的可空字段/人工补齐/独立收紧方案。未执行 SQL 或任何业务代码。

## 2026-07-17 学生资料与宿舍扫码入住：续办门禁差异

- 续办开始时，真实 MySQL 元数据表明 `qh_student_profile` 没有 `real_name` 或 `contact_phone`，而仓库也没有同名 `student_profile_identity_increment.sql`。本轮已基于该差异重新生成待人工执行脚本；字段在实际库生效前，学生端功能仍不得实施。
- `qh_student_profile` 的当前版本唯一索引和 `qh_dorm_checkin` 的学生/床位有效入住唯一索引均存在；对应学生资料、用户、床位、资产套装等外键齐全。结构仅在实名与联系电话字段处阻断。
- 因用户规定字段、索引或约束不一致须立即停止，本轮未继续读取或改动 UserContext、Redis Token、二维码资产实现、后端/前端代码或测试；没有执行任何 SQL 或 DML。

## 2026-07-17 登录 Tab、OSS 路径与个人中心：修正基线

- 登录 Tab 不可见不是全局 Element Plus 覆盖导致：`LoginView` 把 `.el-tabs__item.is-active` 和 `.el-tabs__active-bar` 合并到同一规则，令活动项文字与背景均为绿色。
- 新头像 Key 应为 `qinghe-life-service/{yyyy}/{MM}/{uuid}.webp`。旧头像仍可能为 `avatars/{userId}/...`；清理解析必须兼容两种受控 Bucket 路径，不能移动旧对象或影响外部 URL。
- 个人页当前通过 `PageHeader` 显示“MY PROFILE / 我的 / 查看并维护当前账号资料”。本轮仅将其替换为“个人中心”及简短用户提示，保留现有移动端、编辑资料、地址和服务布局。


## 2026-07-17 登录、资料与 OSS 用户头像：完成发现

- 头像上传没有创建第二套认证、Controller、Axios、Store 或日志表：当前用户由 `UserContext` 获取，资料与头像均通过现有 `syncCurrentSession` 回写当前 Redis Token Hash，前端用唯一 Pinia Store 立即刷新顶部导航。
- OSS 组件只在调用时创建客户端，因此未设置 AccessKey 不会妨碍应用启动；上传时才从 `OSS_ACCESS_KEY_ID`、`OSS_ACCESS_KEY_SECRET` 读取。自动化测试以 Mock 操作器覆盖上传、删除和补偿，未产生真实 OSS 请求。
- 旧对象删除具备 Bucket/域名/后缀/当前用户路径四重限制；外部、默认和其他用户地址不会被删除。数据库保存失败会尽力删除新对象，旧对象删除失败仅记录异常类型。
- 首次受限 Maven/前端构建分别被 Redis 套接字和父目录元数据权限阻断；在同一命令得到受控权限后，后端 32 项测试、打包和真实前端构建均成功，均非项目代码失败。

## 2026-07-17 登录、资料与 OSS 用户头像：实施前审计

- 登录、首次资料、个人资料和顶部导航均复用唯一 `frontend/src/api/http.js`、Pinia User Store 与 `/api/user/**`。`UserServiceImpl.updateProfile` 与 `syncCurrentSession` 已可将安全 UserDTO 写回当前 Redis Token Hash。
- `UserController` 已有 `GET /api/user/me`、`PUT /api/user/profile` 和唯一的个人资料操作日志链路；本轮应在同一 Controller 增加 `POST /api/user/avatar`，不能建立第二套 Controller、Store、Axios 或认证机制。
- 当前后端未发现阿里云 OSS 依赖、属性类或操作器；`UserContext` 实际路径为 `com.qinghe.life.utils.UserContext`。需要以环境变量配置的可复用 OSS 组件，并在自动化测试中 Mock，绝不真实上传。
- 现有登录与首次资料页面功能正确但布局极简；个人页显示头像和脱敏手机号，尚无资料编辑入口或图片选择、裁剪、压缩和确认上传交互。


## 2026-07-17 免注册登录与首次资料完善：交付收尾发现

- 已从实际源码复核：`POST /api/user/login/password` 和受保护的 `POST /api/user/profile/complete` 已在唯一 `UserController` 中实现；前端路由为 `/profile/complete`，登录页复用既有 `http.js`、Pinia 和 Router，没有平行认证或请求栈。
- 新手机号验证码登录会自动创建基础用户并返回 `newUser=true`、`profileCompleted=false`、`hasPassword=false`。`profileCompleted`、`hasPassword` 写入安全 `UserDTO` 及 `qh:login:token:{token}` Redis Hash，首次资料完成后同步当前 Token；`newUser` 只对应当次登录响应。
- 首次资料完善复用校园地址校验，在事务内创建默认地址、更新昵称与 `profile_completed=1`；密码为可选，填写时仅 BCrypt 写入 `password_hash`。密码登录成功沿用同一 Token 创建路径；失败场景统一返回“手机号或密码错误”，避免泄露账户或密码设置状态。
- 文档追加采用各文件实际首行锚点，而非假设“注册页面设计”之外的标题。首次整组补丁因 `findings.md` 标题实际为“活动发现”未能应用；随后已按真实内容精确处理，未造成文件改动丢失。
- 最终受控网络回归在 `Q:\backend` 通过 25 项（0 failure、0 error、0 skipped），打包成功；真实前端路径构建成功并转换 1695 个模块。受限沙箱先后出现 Redis 套接字和父目录 `lstat` 权限阻断，均在同一命令获授权后消除，不属于项目故障。

## 2026-07-17 免注册登录与首次资料完善：初步审计

- 当前验证码登录已在唯一 `UserServiceImpl.login` 中为缺失手机号创建用户并生成既有 Redis Token；`LoginVO` 目前只含 `token` 和 `user`，`UserDTO` 与 Redis Hash 未携带 `newUser`、`profileCompleted`、`hasPassword`。
- 当前前端已有单一 Pinia 用户 Store、`http.js`、`LoginView`、`ProfileView` 与校园地址页，且地址写操作已有唯一 `qh_operate_log` 注解链路；尚未发现密码登录或首次资料完善页面/后端状态实现。
- 源码中未发现 `profile_completed` 或等价可靠字段。下一步必须只读核验 `qh_user` 实际列；未确认前不修改认证、地址或前端业务代码。

## 2026-07-17 免注册登录与首次资料完善：结构门禁结果

- 只读 `information_schema.columns` 确认 `qh_user` 现有账号字段为 `username`、`password_hash`，但没有 `profile_completed` 或等价列；因此无法可靠区分首次资料未完善状态。
- 已生成 `backend/src/main/resources/sql/profile_completion_increment.sql`：仅使用 `ALTER TABLE qh_user ADD COLUMN IF NOT EXISTS profile_completed TINYINT(1) NOT NULL DEFAULT 0`，不含 DROP、DELETE、UPDATE、TRUNCATE、数据初始化或 Redis 操作；没有自动执行。
- 根据用户明确停止条件，本轮没有修改 UserController/UserService/AddressService、Redis Token、前端登录/资料页面、接口契约或测试，也没有启动服务、执行 Git、Maven 或前端构建。待用户手工迁移和只读复核后，从第 3 阶段继续。

## 2026-07-17 免注册登录与首次资料完善：迁移复核

- 用户确认后只读核验 `qh_user.profile_completed`：实际为 `tinyint(1)`、NOT NULL、默认值 `0`；数据库门禁已解除，未执行任何 SQL。
- 当前验证码登录已为新手机号创建随机昵称用户并签发既有 Redis Token；实现将扩展同一 LoginVO/UserDTO/Redis Hash，密码登录与首次资料完善均复用 UserController、UserService、AddressService、UserContext 和双拦截器。

## 2026-07-17 免注册登录与首次资料完善：实现设计

- `UserService.completeInitialProfile` 将在一个事务内复用 `AddressService.create` 的校园目录校验和用户隔离，随后更新 `qh_user.profile_completed=1`、可选 BCrypt 密码、当前 Redis Token Hash 与 UserContext；地址校验或数据库更新失败会整体回滚。
- 新密码登录端点仅对不存在用户或密码不匹配返回统一“手机号或密码错误”；已存在但未设置密码的用户返回“该账号未设置密码，请使用验证码登录”。两种成功登录均调用同一既有随机 Token 创建路径。
- `UserDTO` 会持久化 `profileCompleted` 和 `hasPassword` 到 Redis Hash，因此 `/api/user/me`、Pinia 会话恢复、个人中心和导航不依赖 localStorage 推断状态；`newUser` 仅为验证码登录当次响应状态。

## 2026-07-17 账号注册：现状审计

- 现有注册实现位于唯一的 `UserController`、`UserService` 与 `UserServiceImpl`，不存在 `AuthController` 或第二套 Axios/Router/Store；`RegisterView.vue`、`/register` 路由、登录页入口和 `UserRegistrationIntegrationTest` 已存在。
- 服务逻辑已满足用户名/密码规则、两次密码一致、`RedisKeys.code(phone)` 验证、BCrypt 存储、成功删除验证码、不自动登录、新用户创建、历史用户原地绑定、已绑定手机号/重复用户名拒绝；测试覆盖对应分支与密码密文。
- 本轮需求指定 `POST /api/auth/register`，而现有实现、前端、白名单和测试均仍使用 `/api/user/register`；将只在现有 `UserController` 中迁移此单一路由，保留其他 `/api/user/**` 认证端点及 Redis Token 逻辑。

## 2026-07-17 账号注册：实施与验证结论

- 注册路径已在同一 `UserController` 中迁移为 `/api/auth/register`；`WebConfig` 白名单、前端 `api/user.js`、注册集成测试、`backend/API.md`、接口契约和交接均已同步。不存在活动代码的旧 `/api/user/register` 引用。
- `UserRegistrationIntegrationTest` 的 5 个用例合并覆盖正常新用户注册、历史手机号原地绑定、重复用户名、已绑定手机号、错误/缺失验证码、弱密码、两次密码不一致、BCrypt 匹配与密文、响应不泄露敏感字段及成功删除验证码。测试的 `REGISTER_TEST_` 数据和验证码 Key 均在前后清理。
- 在 `Q:\backend` 的受控网络重跑中，完整 Maven 回归为 24/0/0/0；后端跳过测试打包与前端生产构建均成功。首次沙箱测试被 Redis 套接字策略拒绝，属于环境限制，重跑后未复现。

## 2026-07-16 校园地址前后端实施

- **数据库门禁通过：** `QH_MAIN/青禾主校区` 启用，4 条启用楼栋均正确关联；`qh_user_address` 的 12 个校园字段、4 个地址索引和 3 条命名外键均存在，`HISTORICAL` 地址仍有 2 条。
- **唯一实现链路：** 新增目录只读 `CampusController`/`CampusService`，地址仍只使用 `AddressController`、`AddressServiceImpl`、`qh_operate_log` 与原有 `@OperateLog`。未创建目录后台管理、日志表、Axios、Router、Store 或 Redis 登录分支。
- **保存与兼容：** 新校园地址必须选择启用且匹配的校区/楼栋，并填写房间号或配送点；服务端以 `UserContext` 获取用户 ID，地址行保存楼栋区域、类型、名称快照。历史地址可以查看、删除、设默认；更新时转 `CAMPUS`，原省市区/详细地址不删除。
- **验证限制：** 主源码 `mvn compile` 与 `mvn clean package '-Dmaven.test.skip=true'` 成功，前端生产构建成功。`mvn test` 和指定 `mvn clean package -DskipTests` 均在 testCompile 失败：调试输出显示 Maven 传递给 Java 编译器的中文工作区路径乱码，进而无法解析所有主包；需在无中文路径的副本或修复本机 Maven/JDK 路径编码后重跑全量测试。

## 2026-07-16 校园地址功能实施：目录数据门禁阻塞

- **结构一致：** 只读查询 `information_schema` 确认 `qh_campus`、`qh_building`、`qh_user_address` 均为 InnoDB / `utf8mb4_0900_ai_ci`；目录列、12 个校园地址列、四个地址索引及三个命名外键均与 `campus_address_increment.sql` 一致。
- **目录为空：** `qh_campus` 与 `qh_building` 的总记录数和启用记录数均为 0；不存在损坏关联的启用楼栋，但也没有任何可用于地址选择的基础数据。
- **处理结论：** 严格遵循用户停止条件：未实现 `Campus`/`Building` Java 类、只读接口、地址 DTO/VO/Service 改造、前端表单或测试；未执行迁移、初始化脚本、Maven 测试、Maven 打包或前端构建。
- **人工准备：** 已新增 `backend/src/main/resources/sql/campus_catalog_init.sql`。该文件以条件 `INSERT ... SELECT ... WHERE NOT EXISTS` 提供 1 个校区和 4 个演示楼栋，且不覆盖、更新或删除现有目录数据；只可由用户在 DataGrip 审核后手工执行。

## 2026-07-16 校园地址模型与迁移准备

- 已从活动计划、交接和既有 API 说明确认：当前已实现地址链路以 `province`、`city`、`district`、`detailAddress` 为核心，校园配送模型需要独立的兼容迁移设计；本轮不得直接改动前后端实现。
- 必须保持的现有行为包括当前登录用户隔离、首地址默认、默认地址切换、删除默认后的自动补选及手机号脱敏；实际源码和数据库复核仍在进行。
- **实际数据库（只读，MySQL 8.0.34）：** `qh_user_address` 有 11 列：主键、`user_id`、`receiver_name`、`receiver_phone`、`province`、`city`、`district`、`detail_address`、`is_default`、创建/更新时间；仅索引 `PRIMARY(id)` 与 `idx_qh_address_user(user_id)`。现有 2 条记录均含旧地址字段。`qh_campus`、`qh_building` 不存在。
- **实际链路：** `UserAddress`、创建/更新 DTO、`AddressVO`、`AddressServiceImpl`、`AddressController`、`frontend/src/api/address.js` 和 `AddressView.vue` 共同使用旧省市区模型。`AddressServiceImpl` 所有资源操作都以当前 `UserContext.userId` 过滤；首地址自动默认，设默认先清同用户其他默认，删除默认地址后选同用户最新地址。页面仅在展示时对 `receiverPhone` 脱敏。
- **引用边界：** `qh_cart` 当前 0 条且没有地址列；`qh_order` 当前 0 条、已有 `address_id`、收件人和 `delivery_address` 文本快照字段，但没有订单业务 Controller/Service。地址迁移不得改动这两表；后续订单实现须从校园地址生成不可变快照，不能依赖后续可编辑的地址记录。
- **设计结论：** 应新增 `qh_campus`、`qh_building`。`qh_building.campus_id` 使用命名外键，`qh_user_address.campus_id/building_id` 使用命名外键和索引；未来服务层还要校验楼栋与校区的组合归属。地址行保留区域、楼栋类型/名称等快照，避免目录重命名后配送信息不可读。
- **兼容结论：** 不删除或改写旧省、市、区、详细地址值，只将其列改为可空以容纳新校园地址；安全 `UPDATE` 仅为未分类旧记录填充 `detail` 并设置 `address_type='HISTORICAL'`。新校园记录以后使用 `CAMPUS`，保留现有默认地址和用户隔离逻辑。
- **交付与验证：** 已生成但未执行 `backend/src/main/resources/sql/campus_address_increment.sql`。静态解析确认它仅含 `CREATE TABLE`、`ALTER TABLE`、`CREATE INDEX`、`UPDATE`；12 个新增字段与基线 SQL 一致。Maven 编译成功，无业务代码变更。外键 `ADD` 是 MySQL 8.0 语法中不能附加 `IF NOT EXISTS` 的唯一非完全幂等段，已在脚本与交接中要求 DataGrip 先查同名约束。

完整历史发现已原样归档到 `docs/history/2026-07-16-pre-context-compression/findings.md`。

## Codex 卡顿根因判断

## 2026-07-16 Maven 测试编译诊断

- 初始 `mvn clean test -e` 已复现：主源码 104 个文件编译成功，随后 `testCompile` 编译 7 个测试文件时，所有 `com.qinghe.life.*` 主包导入均报“程序包不存在”。这排除了测试源码包名整体偏离的可能，表明 `target/classes` 未被 `javac` 正确读取。
- 环境为 Maven 3.9.11、Oracle JDK 21.0.9（项目仍以 `release 8` 编译）；Maven 报告平台编码为 UTF-8，而 Windows 控制台活动代码页为 936、`Encoding.Default` 为 gb2312。工作区包含中文路径，符合 `javac` 参数文件/类路径解析编码不一致的症状。
- `backend/pom.xml` 只声明 Java 8/release 8，尚未设置 `project.build.sourceEncoding` 或 `maven-compiler-plugin` 的参数文件策略；下一步仅验证关闭参数文件是否能恢复 testCompile，再决定最小构建配置修复。
- `mvn -X test-compile` 已验证 Maven 注入 testCompile 的 classpath 含 `target/classes`，且 7 个测试的包名均为 `com.qinghe.life`、导入均指向实际存在的主包；问题不在依赖声明、测试包结构或导入。
- 用 `-Dmaven.compiler.useArgFile=false` 与 `-Dmaven.compiler.forceJavacCompilerUse=true` 分别复核，均无法在中文路径中恢复主包解析；因此不把未经验证的编码/编译器配置写入 POM。临时 `subst Q: <项目根目录>` 提供 ASCII-only 工作路径，测试编译立即恢复。
- ASCII 路径暴露唯一真实测试错误：`AddressIntegrationTest` 的空位置负例少传 `deliveryPoint`。补齐空字符串后，`mvn clean test` 通过 24 项（0 failure/error/skipped），后续 `mvn clean package -DskipTests` 成功。校园地址可以标记为本轮已验证完成。

1. **主因：本机内存竞争。** Edge 及其 WebView 共约 4.75GiB 工作集/私有内存量级，且设备内存使用率约 78.2%；这会放大 Codex 桌面端的 UI 卡顿与工具响应延迟。
2. **次因：活动上下文过长且任务混杂。** 历史 `task_plan.md`、`progress.md`、`findings.md` 合计约 145.5KiB，叠加超长任务说明、完整技能文本和浏览器/文档输出，会增加模型处理时间，并出现输出截断、工具连接重建等交互症状。
3. **已排除的高概率因素。** 工作区源码规模小，相关项目服务均正常监听且公开接口响应成功；当前 CPU 并未持续满载。浏览器自动化一次失败属于不支持 `networkidle` 等待状态的工具兼容限制，不是项目页面阻塞。
4. **复核结论。** 活动记录压缩持续生效，且已有 `.gitignore` 排除大型依赖与构建目录。当前 Codex 进程约 99MiB 工作集、56 线程，没有发现进程自身持续增长或项目索引错误；外置 Edge（27 进程、约 3.58GiB 私有内存）和 FinalShell Java 是更主要的可控内存消费者。

## 已完成缓解

- 对历史记录实施可逆归档：原始文件保留在 `docs/history/2026-07-16-pre-context-compression/`，根目录只保留活动摘要。
- 后续任务应按模块新开 Codex 任务，并按需读取文件，避免一次性全文读取全部历史文档；全站审计、性能诊断和功能开发不得混在同一长会话中。
- 压缩复核时 5174、8090 连接拒绝；未对运行服务操作。该环境变更阻塞网页连通性复测，但不推翻首次已取得的公开接口 HTTP 200 证据。
- 当前不需要修改项目代码或新增忽略规则。若 Codex 再次无响应，优先释放未使用的 Edge 标签/窗口和 FinalShell 会话，再在新的短任务中继续；不建议在同一任务继续混合全站审计、浏览器自动化、Codex 排障与功能开发。

## 未执行的外部动作

- 未关闭 Edge、FinalShell、Codex 或任何服务。外置 Edge 没有向当前自动化环境提供可控连接，故未声明完成 Edge 自动化验收。

## 2026-07-16 全站功能审计：初始事实

- 本轮检测时前端 5174、后端 8090、MySQL 3306 和 Redis 6379 均未监听，直接阻塞所有需要真实运行实例的检查；未进行服务控制或数据库写入。
- 当前后端 Controller 实现为 `UserController`、`HomeController`、`CategoryController`、`ShopController`、`GoodsController`、`AddressController`、`CartController`。订单、优惠券、探店、管理员管理只有前端路由/占位页面或文档契约，没有同名后端 Controller。
- 地址 SQL、实体、创建/更新 DTO 和 VO 共同采用省、市、区、详细地址字段；字段命名大体一致，但业务模型不符合校区/楼栋/房间/配送点为中心的校园配送需求，且改造必然需要数据库迁移。
- 前端生产构建成功，但主 JS 为 1129.74KB、CSS 为 377.30KB；缺少路由级动态导入或 Rollup 手动分包。全局样式仅有一个 `base.css`（3.5KB），项目共有 13 个 Vue 样式块；未发现足以静态证实的重复 CSS 选择器，重复 CSS 结论为“未确认”。
- `OrdersView`、`CouponsView`、`BlogsView`、`AdminDashboardView`、`AdminResourceView` 都是明确的页面骨架。其后端契约及实体虽存在，实际 Controller/Service API 未实现，不能认定为可用功能。
- 认证源码采用随机六码、Redis Key、30 分钟 Token 续期、BCrypt、手机号脱敏、注册字段忽略和 AOP 递归脱敏；全局异常层覆盖 400/401/405/409/500/503。上述仅为静态确认，受运行环境影响未执行真实验证。
- 正式结论见 `docs/full-site-audit.md`：当前最先需要解决的是运行环境恢复并重跑受阻验证，其次是校园地址模型迁移设计，随后才是订单创建/查询。订单、优惠券、探店、后台与 404 均不能宣称已完成。

## 2026-07-16 英文路径迁移验证发现

- 用户指定的英文目标路径 `C:\ruanzhu\workplace\qinghe-life-service` 当前不存在，故不具备执行 `cd backend` 后 Maven 与前端构建命令的前提。
- 扫描规则：检索项目文本文件中带中文字符的 Windows 绝对路径，并精确检索原工作区根；排除了 `backend/target`、`frontend/node_modules`、`frontend/dist`、`.git`、`.agents` 与 `.codex`。
- 命中仅两处：`docs/HANDOFF.md` 的历史 Maven 指令（会影响后续构建，已改为待目标目录存在后使用的英文路径）和 `docs/history/2026-07-16-pre-context-compression/findings.md` 的归档事实（不影响构建或运行，保留）。没有命中业务源码、`pom.xml`、Vite 配置、启动脚本、应用配置或测试配置。
- 迁移正式验收条件：目标目录出现后，在其 `backend` 目录执行 `mvn clean test` 与 `mvn clean package -DskipTests`，再在其 `frontend` 目录执行 `D:/develop/NodeJS/npm.cmd run build`；目前三项均为未执行，而非通过或失败。

## 2026-07-16 实际英文工作目录测试诊断

- 当前工作目录已确认是 `C:\Users\28402\Desktop\ruanzhu\workplace\qinghe-life-service`，未访问 `C:\ruanzhu`。
- `backend` 中的首次 `mvn clean test` 已执行：104 个主源码文件成功编译，随后 7 个测试源文件在 `testCompile` 报所有 `com.qinghe.life.*` 主包不存在，故 0 个测试方法实际运行。
- 主编译产物实际存在：`target/classes/com/qinghe/life` 下可见应用入口、实体、Mapper、Service、Controller 等 `.class` 文件。当前证据指向 Maven 测试编译 classpath 解析问题，不是业务源码缺失；正在读取详细编译参数，未修改代码或测试。

### 结论

- Maven `-X` 输出明确列出 `target/test-classes`、现存的 `target/classes` 和全部依赖 JAR；但 7 个测试源文件仍同时无法解析所有主包。`forceJavacCompilerUse` 与 fork 外部 `javac` 也失败，故当前无法从证据确定安全的项目内修复；不应借此删除、跳过或禁用测试，也不应猜测性调整 POM。
- 用户指定的 `mvn clean package -DskipTests` 同样经过 testCompile 后失败，未生成 `qinghe-life-backend-1.0.0.jar`。`-DskipTests` 的语义仅是不执行 Surefire 测试，并非跳过测试源码编译。
- 前端构建在放行父目录元数据读取后成功：1694 个模块、CSS 377.82 kB、JS 1,133.47 kB；两条 `@vueuse/core` PURE 注释与大 chunk 警告均为非阻断。
- 校园地址的正式验收条件尚未满足：本轮测试方法执行数为 0，尽管历史 ASCII 映射环境曾有 24 项通过记录。建议先解决 Maven/JDK 的 testCompile classpath 故障；随后唯一推荐进入的功能模块为订单创建与查询。

## 2026-07-17 Q: 短路径对照结论

- 对照条件相同：Maven 3.9.11、Oracle JDK 21.0.9、POM 的 maven-compiler-plugin 3.10.1/release 8、104 个主源码及 7 个测试源文件均未改动。唯一有效变量是 Maven 实际工作路径：`Q:\backend` 对比当前较长的 Windows 绝对路径。
- Q: 下 testCompile 直接成功，证明 `target/classes`、`com.qinghe.life.*` 主包、测试源文件、POM 依赖、compiler fork/executable/compilerArgs 配置均不是根因；因此不需要有效 POM、effective-pom、JAVA_HOME、MAVEN_OPTS、CLASSPATH 或 classpath 调试日志的进一步修复调查。
- 在允许访问现有 Redis 的 Q: 重跑中，完整 `mvn clean test` 为 24/0/0/0，随后 `mvn clean package -DskipTests` 成功并生成后端 JAR。此前受限沙箱出现的 Redis `getsockopt` 权限错误是执行环境网络限制，不是项目测试失败。
- 真实根因：Windows 长路径场景下 Maven/Javac testCompile 对 classpath/参数的解析异常，导致即使 debug 输出列出 `target/classes`，测试编译仍无法加载主包。短路径映射是经验证的本机构建规避方式，不涉及删除或禁用测试。
- 校园地址模块现具备后端正式验收证据：三项地址集成测试已包含在 24 项完整回归中，且后端 JAR 已成功生成；浏览器端人工交互验收仍是独立步骤。
## 2026-07-17 登录 Tab、头像 OSS 路径与个人中心文案修正

- 根因：`LoginView.vue` 将 `.el-tabs__item.is-active` 与 `.el-tabs__active-bar` 写在同一规则中，同时把前景色和背景色设为 `#2f855a`，造成激活文字与绿色背景同色。全局 `base.css` 仅覆盖主按钮和分页，没有 Element Plus Tab 的全局覆盖。
- 修正：未激活 Tab 保持深色字与透明背景；hover、focus、active 状态分别明确；激活 Tab（含 hover/focus）统一为绿色背景和白色文字，并保留可见焦点轮廓。
- 新头像 Key 为 `qinghe-life-service/{yyyy}/{MM}/{uuid}.webp`（标准 UUID，WebP）。数据库仍接收 OSS 返回的完整 URL。旧 `avatars/{userId}/{yyyy}/{MM}/{uuid32}.webp` URL 不迁移、不批量修改且仍可展示；受控旧 Key 识别仍可在下一次更换头像时清理它。
- `ProfileView.vue` 的页头改为“个人中心 / 管理个人资料与校园服务”，移除英文 eyebrow 和后台式冗余说明；编辑资料、用户名、手机号、校园地址、常用服务及既有移动端样式均未改动。
- Maven 验证的 PowerShell `New-PSDrive` 不是 DOS 短路径：编译器诊断仍显示 C: 长路径，导致历史 `testCompile` classpath 解析问题再次出现。该失败未反映本轮源代码；后续验证必须使用一次性 `subst Q:` 的真实驱动器映射。
- 验证完成：真实 `subst Q:` 映射下，受控网络重跑 `mvn clean test` 通过 32 项（0 failure / 0 error / 0 skipped）；`mvn clean package -DskipTests` 成功并生成 `qinghe-life-backend-1.0.0.jar`。前端 `npm.cmd run build` 成功，转换 1696 个模块。Vite 的 `@vueuse/core` PURE 注释与大 chunk 警告均为非阻断警告。
## 2026-07-17 宿舍入住与资产二维码管理：设计与迁移准备

- 现有目录复用结论：`qh_campus` 的 `campus_code`/`campus_name` 唯一，`qh_building` 通过 `campus_id` 外键关联校区且同校区楼栋名唯一；设计仅以两者的既有主键作外键，不创建重复校区或楼栋表。
- 身份与审计边界：用户端必须由现有 `UserContext.getUserId()` 确定本人；管理员沿用独立 `qh_admin` 认证体系。所有宿舍与资产管理写操作在下一阶段使用唯一 `@OperateLog` / `qh_operate_log`，不新增模块日志表。
- 当前约束：本轮不执行任何 SQL 或数据库写入，不实现 Java/Vue/API；仅生成待人工审核的增量 DDL 及设计契约。
- 数据模型结论：`qh_student_profile` 与 `qh_dorm_checkin` 采用可空历史标记模式，只有当前/有效行写 `1`，历史行写 `NULL`；MySQL 复合唯一索引因此可阻止同一学生或床位存在两条有效入住，同时允许保留多条历史。
- 二维码结论：`qh_asset_set.qr_token` 为 64 位十六进制随机令牌且唯一，二维码只携带版本标记与令牌；学生登录后由服务端从 `UserContext` 绑定，不能靠扫码直接绑定。ZXing 运行时生成图片，默认不使用 OSS。
- 管理结论：换寝关闭旧入住记录并创建关联的新记录；退宿、学籍异动和批量清除均关闭有效入住、释放资产套装、记录原因，禁止硬删除。所有管理写操作下一阶段复用 `@OperateLog`/`qh_operate_log`。
- 静态迁移复核：`dorm_asset_increment.sql` 仅有 1 条 `ALTER TABLE` 和 6 条 `CREATE TABLE`；不存在数据写入、数据清理或查询语句。所有外键为 `RESTRICT`，不触发关联数据变更。
## 宿舍基础档案与资产二维码（2026-07-17）

- 用户声明 `backend/src/main/resources/sql/dorm_asset_increment.sql` 已由 DataGrip 手工执行；本轮的硬门禁是仅使用数据库元数据复核 `qh_building.building_code`、`qh_dorm_room`、`qh_dorm_bed`、`qh_student_profile`、`qh_asset_set`、`qh_asset`、`qh_dorm_checkin` 的列、索引和外键。不一致即停止且不执行 SQL。
- 已读取项目规则、产品约束、交接、计划、进度、发现和数据库设计。设计文档登记：资产套装号为 `building_code + room_no + '-' + bed_no`；`qr_token` 为至少 32 字节 SecureRandom 值的 64 位十六进制；二维码载荷仅为 `QH-DORM-V1:{qrToken}`，不得含个人信息；二维码按请求动态输出而不持久化或上传 OSS。
- 现有 `task_plan.md` 包含较早任务的未完成条目；本轮使用独立 `dorm_asset_plan.md` 持久化计划，避免将旧任务误判为当前工作。
- 数据库只读门禁通过：`qh_building.building_code` 为可空 `varchar(32)`，并存在唯一索引 `(campus_id, building_code)`；`qh_dorm_room`、`qh_dorm_bed`、`qh_student_profile`、`qh_asset_set`、`qh_asset`、`qh_dorm_checkin` 的列、默认值、唯一/普通索引和外键均与 `dorm_asset_increment.sql` 一致。全程仅查 `information_schema`，未执行 DDL/DML。
- `qh_asset_set` 已有床位、套装号、二维码令牌三个唯一约束，`qh_asset` 已有 `(asset_set_id, asset_type)` 唯一约束，可作为并发/重复生成时的最终兜底。
- 既有后端仅有普通用户的 `qh:login:token:{token}` 与 `UserContext`，前端 `requiresAdmin` 目前只判断“是否有任意 Token”，没有服务端管理员鉴权；`qh_admin` 实体与 Mapper 已存在但未被使用。因此本轮将补充最小管理员会话链路（`qh:admin:token:{token}`、管理员登录和 `/api/admin/**` 拦截器），避免将普通用户身份误作管理员。这是“管理接口必须校验管理员身份”的必要实现，不新增表或日志表。
- 保留 `RefreshTokenInterceptor`、`LoginInterceptor` 和 `@OperateLog`：管理员拦截器确认独立管理员会话后向现有 `UserContext` 提供仅含管理员 ID/显示名的审计上下文，随后仍由既有登录拦截器和 AOP 记录到唯一 `qh_operate_log`。不创建并行 HTTP/AOP 链路。
- 后端拟复用 MyBatis-Plus、`PageQuery`/`PageResult`；新增 ZXing 仅用于内存 `BitMatrix` 到 HTTP PNG/ZIP 响应。前端复用现有 `http.js`、`/admin` 布局和路由守卫。
## 2026-07-17 校园消费服务专项审计：初始证据

- 本轮严格只读：已读取规则、项目规格、计划、交接、进度、发现、接口、数据库设计和页面设计；不会执行 SQL、连接数据库实例、修改业务代码或启动服务。
- 既有交接记录表明：商铺、商品、购物车已有现有栈实现与构建/集成测试记录；订单、优惠券和后台资源管理仍不得因页面或文档出现而视为实现完成。校园配送模型需要以当前 SQL、实体和接口的静态核对为准。
- 本轮数据库结论只能依据仓库内 `qinghe_life.sql`、增量 SQL、实体映射和设计文档；真实 `qh_*` 表的列、索引和数据状态属于“数据库或接口阻塞”，不作已验证声明。
- 首次相关文件枚举的正则过滤命令返回退出码 1 且无输出；后续改为先列出实际目录，再按模块用独立命令核对，未重复同一命令。

## 2026-07-17 校园消费服务专项审计：代码与图片链路证据

- 现有后端只有 `ShopController`/`GoodsController`/`CartController`、对应 Service 和前端 `shop.js`/`goods.js`/`cart.js`；不存在 Order、Coupon、Delivery 或 `/api/admin/**` Controller/Service。`OrdersView.vue`、`CouponsView.vue` 和 `AdminResourceView.vue` 都是 `PageScaffold` 占位页。
- `CartServiceImpl` 和 `CartMapper` 为加购/改数量使用当前 `qh_goods` + `qh_shop` 条件校验上架、库存、商铺启用、用户归属及库存原子限制；但购物车查询仅读当前商品/商铺并跳过缺失行，下单前仍需重新校验价格、库存和单店归属。
- `qh_shop.cover_image` 与 `qh_goods.cover_image` 为可空 `VARCHAR(255)`，对「一张封面/一张主图」已足够，无需仅为此迁移。现有 `AliyunOSSOperator` 使用一个 `aliyun.oss.bucket-name` 配置，但只有用户头像上传和仅限头像 key 的旧图清理；店铺/商品上传、管理员权限、压缩与 Mock OSS 测试尚未实现。
- 前次文件批读中误引用了不存在的 `backend/src/main/java/com/qinghe/life/oss/OSSProperties.java`；实际配置类是 `config/AliyunOSSProperties.java`。后续以实际路径核对，不重复无效读取。
## 2026-07-17 校园消费服务专项审计：最终结论

- 商铺/商品用户端已有 Entity、Mapper、Service、Controller、前端 API 与页面，但没有后台维护和专项测试；订单/优惠券只有 Entity、Mapper、SQL、文档契约与占位页面；配送仅有地址配送点字段，缺服务范围、费用和状态模型。
- 购物车是现有范围内唯一完整消费链路：`CartIntegrationTest` 覆盖未登录、库存、上下架、停用商铺、越权、分组、金额、勾选和清空。该测试本轮未执行，因用户禁止执行 SQL；报告将历史验证证据与本轮未验证事实分开记录。
- `qh_shop.cover_image`、`qh_goods.cover_image` 均为单一可空 `VARCHAR(255)` 图片 URL 字段，适合当前“一店一封面、一商品一主图”，不需要迁移。现有 `AliyunOSSOperator`/单 Bucket 可复用，但仅实现头像路径校验与删除；店铺、商品必须新增独立、严格前缀的安全旧图解析，不能调用 `ownAvatarKey`。
- 推荐先分四个不迁移的独立批次完成商铺资料/封面、商品资料/主图、商铺后台、商品后台；订单创建随后需要经审核的配送快照迁移，配送费用与状态需要独立迁移。所有写操作继续复用唯一 `qh_operate_log`，不新增模块日志表。

## 2026-07-17 管理端认证与权限门禁：结构审计

- 仓库基线与 `Admin` 实体均声明复用 `qh_admin(username, password_hash, display_name, status)`，并有唯一用户名索引；结构设计满足 BCrypt 密文和启用状态的最低条件。必须再以真实数据库只读元数据确认后才可实现。
- 现有普通用户会话为 `qh:login:token:{token}`，`RefreshTokenInterceptor` 将其写入 `UserContext`；`RedisKeys` 已预留 `qh:admin:token:{token}` 和 30 分钟 TTL。管理员必须保持独立 `AdminContext` 与拦截器，不能复用普通用户 ThreadLocal。
- `WebConfig` 已引用 `AdminAuthInterceptor` 并配置 `/api/admin/**`，但普通 `LoginInterceptor` 也会拦截该路径；管理员拦截器需要在普通用户门禁之前完成判定且普通门禁必须排除 `/api/admin/**`。前端 `/admin/login` 当前错误复用用户 `LoginView`，路由守卫仅检查普通 Token，不能作为管理员门禁。
- 基线 SQL 的演示管理员密码哈希不是有效 BCrypt 哈希；本轮不得把明文或可预测密码写进 SQL/文档。真实库若没有可用 BCrypt 管理员账户，测试应创建并精确清理带标识的临时管理员，而不是更改既有管理员。
- 首次只读数据库门禁命令因把 `application.yml` 的纯文本 `username: root` 当作仅占位符格式解析而失败，未连接或执行 SQL；已改为按实际 YAML 字段分别解析 URL、用户名与密码占位符后再尝试，不重复该解析方式。
- 真实 `qh_admin` 只读门禁通过：存在 `id`、`username varchar(64)`、`password_hash varchar(255)`、`display_name varchar(64)`、`status tinyint NOT NULL DEFAULT 1`、时间字段及 `uk_qh_admin_username`，当前有 1 条记录。无须生成迁移 SQL。命令仅查询 `information_schema`、索引和记录数；客户端给出命令行密码安全警告，未输出密码或哈希。
- 工作区已有未完成的管理员认证骨架：登录接口/服务会写入 `qh:admin:token:{token}` 并 BCrypt 校验，但缺少 `AdminContext`、当前/退出接口、管理员测试、独立前端会话/API/登录页；`AdminAuthInterceptor` 错误将管理员身份写入 `UserContext`，且对普通用户 Token 返回 403 而非本轮要求的 401。必须在既有骨架内修正，不新建平行认证栈。
- 首次 `Q:` 编译预检的临时盘符清理流程不正确：PowerShell 在仍位于 `Q:` 时尝试删除盘符，且 Maven 输出不足以确认是否完成。因此该次不能算编译成功；后续将用 `Push-Location`/`Pop-Location` 修正临时映射生命周期。
## 管理端认证与权限门禁：静态审计（2026-07-17）

- 复用基线：`qh_admin` 实体/Mapper、`AdminAuthServiceImpl`、`AdminAuthInterceptor`、`RedisKeys.adminToken` 和唯一 `qh_operate_log` 已存在；不需要新建管理员或日志表。
- 现有管理员登录服务已以 `BCryptPasswordEncoder.matches` 校验 `password_hash`，以去连字符 UUID 创建 Redis Hash，并设置 `qh:admin:token:{token}` 的 30 分钟 TTL；需补齐独立 `AdminContext`、当前管理员查询、退出与测试。
- 安全缺口：现有管理员拦截器把管理员会话映射到 `UserContext`，且对普通用户 Token 返回 403。目标实现必须使用 `AdminContext`，并使所有无效、过期或普通用户 Token 访问 `/api/admin/**` 时返回 401。
- 路由缺口：`/admin/login` 现在复用普通用户登录页，`requiresAdmin` 守卫仅调用普通 `getToken()`；目标实现要将管理员 Token/资料与普通用户 Pinia 会话隔离，仍使用唯一 `frontend/src/api/http.js`。
- 待完成门禁：只读检查实际 `qh_admin` 的 `password_hash`、`status` 和用户名唯一索引；若不匹配，仅生成增量 SQL 并停止。

### 只读数据库门禁结果

- 实际 `qh_admin` 列为 `id`、`username varchar(64)`、`password_hash varchar(255)`、`display_name varchar(64)`、`status tinyint NOT NULL DEFAULT 1`、创建/更新时间；索引为主键和唯一 `uk_qh_admin_username(username)`，结构门禁通过。
- 仅聚合检查（未输出哈希）显示 1 条管理员记录中有效 BCrypt 格式数量为 0。该数据质量问题不表示字段缺失，且本轮禁止管理员注册和未经授权的存量凭据变更；集成测试须自行插入并精确清理带标识的 BCrypt 管理员。

### 实现决策

- 新建仅承载管理员身份与 Token 的 `AdminContext`，管理员拦截器只写入该上下文并在 `afterCompletion` 清理；`RefreshTokenInterceptor` 继续只管理 `UserContext`。
- `LoginInterceptor` 将排除 `/api/admin/**`，由管理员拦截器独占该路径；其 Redis Hash 不存在、过期、畸形或携带普通用户 Token 时均统一返回 HTTP 401。
- 管理操作仍由现有 `@OperateLog` 切面写入唯一 `qh_operate_log`。为保存管理员操作人，切面在没有普通用户上下文时回退读取 `AdminContext` 的管理员 ID；不会把管理员伪装成普通用户上下文。
- 前端将废弃共用的 `qh:token`/`qh:admin-mode` 标识组合，改用独立管理员 Token、管理员资料和 Pinia store；唯一 Axios 实例按 `/admin/` 请求选择管理员 Token，并在 401 时只清理相应会话、跳转相应登录页。

### 审计命令记录

- 一次源码读取误将 `UserDTO` 当作 `dto` 包文件，PowerShell 报路径不存在；已按实际 `vo/UserDTO.java` 读取，未修改文件或重复相同错误命令。
- 前端批量补丁首次因历史文件中的中文文本编码与补丁上下文不一致而未匹配 `AdminLayout.vue`；补丁工具保持原子性，未产生部分写入。后续改用按文件替换和只含稳定 ASCII 上下文的小补丁，不重复同一匹配方式。
- 首次 Maven 验证尝试的 PowerShell `subst` 映射不可被同一受限会话的 `Push-Location` 识别，导致 Maven 在项目根目录报无 POM；临时映射清理亦被拒绝。未运行测试或改动项目文件。后续仅使用同一 `cmd.exe` 子会话创建、使用并清理映射，且若盘符冲突即停止该命令。
- `cmd.exe` 首次替代命令把转义双引号传给了 `subst`，其报路径不存在并在 Maven 前退出；后续使用无空格绝对路径，避免重复该转义写法。
- 直接从实际 `backend` 路径执行 `mvn clean test`：主代码 134 个源文件编译成功；测试编译阶段所有测试文件无法解析既有 `com.qinghe.life.*` 主包，0 个测试执行。该现象与已知 Windows 长路径 `testCompile` classpath 故障一致，不能作为管理员认证测试通过的证据。
- 最终验证：完整 Maven 回归 34 项全部通过，涵盖新增宿舍资产测试以及原认证、地址、购物车和 AOP 测试；指定 Q: 后端打包和真实前端路径生产构建均成功。未启动服务、未执行 SQL、未上传 OSS。
## 管理端认证与权限门禁：最终结果（2026-07-17）

- 管理员认证与普通用户认证已隔离：管理员 Redis Key 为 `qh:admin:token:{token}`，管理员 `AdminContext` 不写入 `UserContext`；普通用户 Redis Token 访问任何 `/api/admin/**` 时由管理员拦截器统一拒绝为 HTTP 401。
- 新增测试覆盖正确/错误 BCrypt 密码、随机 Token、管理员当前信息、普通用户 Token 拒绝、过期、退出和两类 ThreadLocal 清理；既有宿舍管理员回归断言同步为 401。完整运行受 Windows 长路径 testCompile 影响而未执行，不能声明这些测试已通过。
- 构建结果：指定 `mvn clean test` 和 `mvn clean package -DskipTests` 均因 testCompile classpath 故障失败；`-Dmaven.test.skip=true` 主代码打包成功；前端生产构建成功。前端两条第三方 PURE 注释警告和大 chunk 警告均为非阻断警告。
## 2026-07-17 学生资料与宿舍扫码入住

- 只读元数据门禁通过：`qh_student_profile` 有 `(user_id,current_flag)`、`(student_no,current_flag)` 唯一约束；`qh_dorm_checkin` 有 `(user_id,active_flag)` 和 `(dorm_bed_id,active_flag)` 唯一约束，且连接学生资料、用户、床位、资产套装的外键均存在。因此当前结构足以保证一名学生与一个床位各仅一条有效入住记录。
- 本轮仅执行了 `information_schema` 查询，未执行 DDL、DML 或 SQL 导入。
- 阻断差异：`qh_student_profile` 实际字段中没有 `real_name`/`realName` 或 `contact_phone`/`contactPhone`，而用户资料要求这两项均持久化、受权限限制且学籍版本可追溯。将实名塞入 `qh_user.nickname` 或将联系人电话复用为登录 `qh_user.phone` 会改变既有业务语义，不能满足“管理员维护身份字段、学生仅修改联系电话”的规则。因此在不修改数据库结构的前提下，不能继续实现该模块。
## 店铺后台维护与封面 OSS：静态结构审计（2026-07-17）

- `qh_shop` 基线字段支持本轮最小管理能力：`name`、`category_id`、`address`、可空 `phone`、`score`、`status`、`is_featured`、可空单图 `cover_image`、`sort_order` 和时间列；索引为主键、`idx_qh_shop_category(category_id)`、`idx_qh_shop_featured(is_featured,status,sort_order)`。
- 不存在店铺简介、营业时间或独立营业状态字段。因此后台表单只维护真实字段，停用/启用仅使用现有 `status`；不创建图片表或业务字段迁移。
- 现有公开详情缓存 Key 为 `qh:shop:detail:{shopId}`、`qh:shop:null:{shopId}` 和 `qh:lock:shop:{shopId}`；后续写操作仅删除本店详情/空值 Key，不会使用 Redis 全库清理。
- 现有 OSS 组件为单例 `AliyunOSSProperties` + `AliyunOSSOperator`，用户头像测试已通过 Mock 该操作器；店铺封面应复用它并增加受限 shops Key 清理。

### 只读数据库门禁结果

- 实际 `qh_shop` 列、空值约束、默认值和三个索引与设计完全一致；`cover_image` 为可空 `varchar(255)`，能够保存单张店铺封面 URL。
- 当前 2 条店铺记录均有封面且为启用状态，没有 `category_id`、`name` 或 `address` 缺失记录。结构足以实现本轮店铺后台维护与单图替换，不生成或执行迁移 SQL。

### 实施前复核

- 已直接读取实际 `qh_category`：`id`、唯一 `name`、可空 `icon_url`、`sort_order`、`status` 和时间列；管理员店铺表单可仅选择既有分类，服务端必须继续校验分类存在且启用。
- 后端和前端文件清单确认已有 `ShopController`/`ShopServiceImpl`、`AdminContext`/管理员拦截器、唯一 `http.js`/管理员 store/`AdminLayout` 以及用户端列表、详情和 `ShopCard`。封面接口和管理页尚未存在，应在这些既有链路中扩展。

### 后端实现与验证记录

- 已在现有 `ShopService`/`ShopServiceImpl` 中增加管理员分页、详情、新增、编辑、状态调整和封面上传；没有提供物理删除接口。全部写接口通过现有 `/api/admin/**` 管理员门禁并标注唯一 `@OperateLog`。
- 封面仅接受 jpg/jpeg/png/webp、空文件拒绝、原文件上限 3MB、扩展名/Content-Type/签名交叉校验。PNG/JPEG 使用 `ImageIO` 解码并限制 1200×800；WebP 校验 RIFF/VP8 结构和画布尺寸。上传 Key 固定为 `qinghe-life-service/shops/{yyyy}/{MM}/{uuid}.webp`，数据库更新失败补偿删除新对象，成功后只会尝试删除本 Bucket 且严格匹配 shops 前缀的旧 Key。
- `mvn -DskipTests compile` 成功（139 个主源码）。在实际中文路径运行测试触发既知 `testCompile` classpath 故障：15 个测试源码均找不到已编译主包，0 个测试执行；不重复同一路径命令，后续须用短路径会话验证。

### 最终验证结论（已完成并验证）

- 使用同一 `cmd.exe` 会话临时映射 `Q:` 后，`mvn clean test` 成功编译 139 个主源码和 15 个测试源码，执行 40 项测试，0 failure、0 error、0 skipped；随后 `mvn clean package -DskipTests` 成功生成 `qinghe-life-backend-1.0.0.jar`。映射在每个命令结束时已移除。
- 新增 `AdminShopIntegrationTest` 和 `ShopCoverServiceTest` 均在该回归内实际运行：覆盖管理员/普通用户门禁、资料/状态/缓存、公开读取、非图片/空/超大文件、Mock OSS 失败、数据库失败补偿、受控旧图删除和日志敏感信息保护。读库确认测试标识店铺、分类、管理员和日志均为 0。
- 前端真实路径构建成功（1707 modules）；第三方 `@vueuse/core` PURE 注释和超过 500kB chunk 是非阻断警告。浏览器交互和真实 OSS 上传依用户限制未自动进行，仍需人工验收。

## 商品后台维护与主图 OSS：只读数据库门禁（2026-07-17）

- 实际 `qh_goods` 结构满足本轮和订单前置最低要求：非空 `shop_id`、`name`、`price decimal(10,2)`、`stock int default 0`、`sale_status varchar(16) default ON_SALE`，以及可空单图 `cover_image varchar(255)`；有主键和 `idx_qh_goods_shop(shop_id)`，无需迁移。
- 商品没有直接分类列，但 `qh_goods.shop_id` 关联的 `qh_shop` 有非空 `category_id`，后台可在查询时以店铺分类过滤且不会复制/伪造分类。商品表也没有 `sort_order`，本轮只能使用真实 `id`、销量等排序，不增加虚构字段或迁移。
- 实际数据聚合显示当前 2 条商品均具备名称、店铺、价格、库存、销售状态和主图。`qh_shop`、`qh_category` 的真实列继续支持有效店铺和启用分类校验。

### 本次续作复核（2026-07-17）

- 已再次仅以 `information_schema.columns`、`SHOW INDEX` 及只读联表查询复核实时 `qh_goods`。字段为：`id`、非空 `shop_id`、非空 `name`、可空 `description`、非空 `price decimal(10,2)`、非空 `stock int default 0`、`sales_count`、非空 `sale_status varchar(16) default ON_SALE`、可空单图 `cover_image varchar(255)` 和时间列。
- 索引仅有 `PRIMARY(id)` 与 `idx_qh_goods_shop(shop_id)`；当前两条商品均关联启用店铺、带主图且有有效价格、库存和销售状态。此查询未执行 DDL/DML 或 SQL 导入。
- 本轮不需要数据库迁移。分类筛选必须通过 `qh_goods.shop_id -> qh_shop.category_id` 实现；`qh_goods` 没有独立分类或排序列，管理端不能承诺编辑商品排序字段。
- 续作静态核对发现，`AdminGoodsController`、请求 DTO、VO 与接口已存在，但没有 `AdminGoodsServiceImpl`，因此当前后端尚不可用；公开 `GoodsServiceImpl` 仅筛选 `ON_SALE`，未排除停用店铺，需在既有查询服务内修正。购物车现有加购校验已同时检查商品上架、库存与店铺启用状态，保持其接口不变。
- 一次读取店铺封面实现时，预设的 `AdminShopServiceImpl.java` 路径不存在；未修改文件，后续改按实际服务清单定位，避免重复使用该错误路径。
- 最终实现复用单例 `AliyunOSSOperator` 及既有 `ownGoodsImageKey`，没有第二套 OSS 客户端。当前商品没有 Redis 缓存，主图和资料写入后无需扩大缓存清理范围；公开查询直接读取最新数据库数据。
- `AdminGoodsIntegrationTest` 与 `GoodsImageServiceTest` 使用 Mock OSS 覆盖门禁、资料、状态、库存、用户端可见性、图片拒绝、上传失败、数据库失败补偿和受控旧图删除。受控网络下完整 Maven 回归实际执行 43 项，0 failures、0 errors、0 skipped；无真实 OSS 调用。
- 后续补充的非 `goods/` 前缀旧图拒删断言已通过 `GoodsImageServiceTest` 3/0/0/0 定向运行，并在其后重新打包成功。受控网络全量重跑被会话额度限制拒绝，不能据此声称最终测试源集合已完成 44 项全量运行。

## 学生资料与宿舍扫码入住：实时只读门禁差异（2026-07-17）

- `qinghe_life.qh_student_profile` 已包含用户手工迁移后的 `real_name`、`contact_phone`，以及 `student_no`、`college_name`、`major_name`、`class_name`、`student_status`、`current_flag`；不包含 `college`、`major`、`academic_status`、`active_flag`。本轮要求的状态/版本字段名称与真实结构不一致。
- `uk_qh_student_profile_user_current(user_id,current_flag)` 和 `uk_qh_student_profile_no_current(student_no,current_flag)` 正确保证当前有效资料唯一。`uk_qh_dorm_checkin_user_active(user_id,active_flag)` 与 `uk_qh_dorm_checkin_bed_active(dorm_bed_id,active_flag)` 正确保证有效入住唯一。
- `qh_dorm_checkin` 外键已关联 `qh_user(id)`、`qh_dorm_bed(id)`、`qh_asset_set(id)`，另有学生资料、管理员和历史关联外键。仅使用 `information_schema` 只读查询；不执行 SQL 导入、DDL 或 DML，不进入源码实现、测试或构建。

## 学生资料与宿舍扫码入住：字段契约已确认（2026-07-17）

- 用户已纠正上一轮阻断依据：`college_name`、`major_name`、`student_status`、`current_flag` 是真实且获准的字段契约，不能因为不存在 `college`、`major`、`academic_status`、`active_flag` 而停止或改表。实体映射和请求/响应分别采用 camelCase `collegeName`、`majorName`、`studentStatus`、`currentFlag`。
- 只读数据库结果确认：`qh_student_profile` 当前为 0 行，故状态与版本真实取值不存在样本；字段定义和既有迁移设计共同确定 `student_status` 是 `varchar(16)`，`current_flag` 是默认 `1` 的可空 `tinyint`。迁移契约规定 `1` 表示当前版本、`NULL` 表示历史版本；`ENROLLED` 是首次学生资料必须使用的现有在读状态。
- `qh_dorm_checkin` 以默认 `1` 的可空 `active_flag` 表示有效入住、以 `NULL` 保留历史；用户和床位的复合唯一索引已在实时库中存在。学生、床位、资产套装关联外键也已实时复核通过，因此门禁不再受字段名称阻断；下一步仅审计现有源码、Token 和二维码链路后实现。
- 源码审计已完成：用户 Token 只通过 `RefreshTokenInterceptor` 从 `Authorization: Bearer <token>` 的 `qh:login:token:{token}` Hash 恢复为 `UserContext`，且请求结束会清理 ThreadLocal。二维码生成器已有安全随机 32 字节、64 位十六进制 Token 及精确 `QH-DORM-V1:` 载荷；管理员和普通用户请求的 Axios Token 分流也已存在。学生端实现必须直接复用，而非创建平行会话或 HTTP 客户端。

## 学生资料与宿舍扫码入住：实施数据语义（2026-07-17）

- 真实学生资料表还有非空 `campus_id`。当前仅一条启用校区，首次资料会由服务端在“恰好一条”条件下确定校区；二维码床位校区会在入住时与资料校区核对，避免客户端传入或伪造校区。
- 床位 `status` 仅表示目录是否可用，当前设计明确不以入住状态覆盖它；有效入住复合唯一索引才是占用规则。实现保持床位启用状态，使用 `qh_asset_set.status=OCCUPIED` 同步套装占用，并通过有效入住唯一约束处理并发。

## 学生资料与宿舍扫码入住：验证缺口（2026-07-17）

- Maven 主代码编译通过，但完整测试在 testCompile 解析主包的中文路径环境错误中断，实际执行 0 项。必须在可用测试环境补跑本模块的资料、二维码、并发、回滚、脱敏及既有模块回归，不能将当前构建回退视为测试通过。
# 店内商品分类只读门禁（2026-07-17）

- `qh_category` 没有 `shop_id`，其 `name` 全局唯一；`qh_goods_category.shop_id` 外键指向 `qh_shop`，`(shop_id,name)` 唯一，`(shop_id,status,sort_order)` 为导航索引；`qh_goods.category_id` 可空并外键指向 `qh_goods_category`。模型与本轮契约一致，可进入代码实现。

# 2026-07-17 店内商品分类与商店主页优化：恢复记录

- 当前活动计划已将本轮拆为四个阶段：只读门禁、后端分类与关联、前端管理与商店详情、回归构建和文档交接。恢复时阶段 2 为进行中，先前一次大批量补丁曾被中断，必须先以当前文件为准核对实际落盘内容。
- 用户明确确认真实库已有 `qh_goods_category`、`qh_goods.category_id` 以及对应唯一索引、分类索引和外键；本轮仍必须完成只读一致性门禁，任何差异都只报告并停止，不执行迁移或结构修正。

# 普通订单、优惠券、Redis 缓存与限时秒杀：静态审计发现（2026-07-17）

- 证据范围为仓库内基线 SQL、迁移脚本、实体和现有代码；本轮按限制未连接 MySQL/Redis、未执行任何 SQL，不能将结论表述为运行中实例已验证。
- `qh_cart` 已有 `user_id`、`shop_id`、`goods_id`、`quantity`、`selected` 和 `(user_id, goods_id)` 唯一约束；购物车可存多店商品，并由 `CartServiceImpl` 按店铺分组。`CartController`/`CartService`/`CartServiceImpl` 已存在，但订单和优惠券没有对应 Controller 或 Service 实现。
- `qh_order` 已有订单号唯一、用户/店铺/地址关联、收件人和电话快照、拼接配送地址、商品总额、优惠额、应付额、状态和备注；仍缺独立配送费、校区/楼栋/详细地址结构化快照、取消原因以及取消/完成时间。`qh_order_item` 已有商品名称/图片/单价/数量/小计快照。
- `qh_coupon` 仅有名称、类型、固定优惠金额、门槛、总库存/已领数、单组起止时间和状态；缺失适用范围、每人限领、独立领取/使用窗口、折扣规则与秒杀配置。`qh_user_coupon` 有 `(user_id,coupon_id)` 唯一、订单引用、用户券状态、领取/使用时间；该唯一规则不能同时支持普通券的“每人可领多张”和秒杀活动的一人一单。
- 当前 Redis 只使用 Spring Data Redis 与 `StringRedisTemplate`；Key 已统一以 `qh:` 开头，已定义登录、管理员会话和商铺详情/空值/互斥 Key。仓库未见 Redisson、Redis Stream、RabbitMQ、Kafka 或其依赖/消费者实现。
- 商铺详情已有 Cache Aside、空值缓存、随机 TTL、有限互斥重建和 Redis 异常回源。现有 `setIfAbsent` 锁使用固定值并直接删除锁 Key，缺少令牌所有者校验；该实现只能作为后续热点缓存整改对象，不能扩展为普通订单或库存正确性方案。

## 2026-07-24 店铺与商品 Redis 热点缓存：最终发现

- 旧店铺缓存仅覆盖详情，且采用非所有者安全的字符串锁、散落 TTL 和独立空值 Key；已收敛为复用现有 `StringRedisTemplate`、`ObjectMapper`、`RedissonClient` 的轻量 `CatalogCache`。
- 缓存范围是店铺详情、商品详情和店铺上架商品目录；商品库存/销量逐次回源 MySQL，不以 Redis 实时扣减库存。
- 事务提交后失效包含商品换店的新旧列表及店铺状态对本店商品详情的影响。测试使用 `CATALOG_CACHE_TEST_` 数据前缀并只删除相应 `qh:cache:*`/`qh:lock:cache:*` Key。

- 迁移决策：普通订单补齐快照、配送费、状态时间和用户券唯一关联；普通券补齐范围、领取/使用窗口与用户券审计字段。因既有 `(user_id,coupon_id)` 唯一约束无法仅靠 ADD 操作放宽，本轮明确第一版每人限领 1 张。
- 秒杀决策：使用独立 `qh_seckill_coupon_activity` 和 `qh_seckill_coupon_order`，以 `(activity_id,user_id)` 和 `stream_message_id` 唯一约束防重复；活动只能绑定专用券。Redis Lua 负责受理，数据库条件库存更新、唯一约束和事务负责最终正确性。
- 过程错误：一次文件清单命令误用了 Bash 花括号，在 PowerShell 参数解析阶段失败，未读写项目文件；已改为显式目录参数，未重复该写法。
# 管理员入住记录、退宿与换寝（2026-07-17）

- 本轮以用户指定的单一里程碑推进：管理员查询入住记录、单个退宿、单个换寝、资产套装状态同步和历史保留；不包含学生自助操作、批量操作、学籍异动、统计或资产报修。
- 静态基线已定位：`DormCheckin` 已含 `previousCheckinId`、`checkinStatus`、`activeFlag`、`checkoutTime`、`checkoutReason`、位置与学生快照、`operatorAdminId`；`dorm_asset_increment.sql` 定义用户/床位有效入住唯一索引、入住历史关联外键和管理员外键。真实数据库仍须按本轮门禁重新只读确认后才可编码。
- 已确认语义基线：床位 `status` 是目录启停，入住占用由有效 `qh_dorm_checkin` 和资产套装 `AVAILABLE`/`OCCUPIED` 表达；不得以床位状态表示空闲或占用。
- 实时库只读结果（本轮）：`qh_dorm_checkin.active_flag` 为可空 `tinyint`、默认 `1`；具备 `checkin_time`、`checkout_time`、`checkout_reason`、`checkin_status`、`previous_checkin_id`、`operator_admin_id` 与完整学生/位置/资产套装快照。用户与床位的 `(id, active_flag)` 复合唯一索引，以及用户、学生资料、床位、资产套装、前序入住和管理员六项外键均存在。
- 实时库当前尚无 `qh_dorm_checkin`、`qh_dorm_bed` 或 `qh_asset_set` 状态分组数据；因此下一次门禁将继续只读核对床位/资产套装的字段默认值及目录结构，不以空表推断状态规则。
- 现有实现边界：管理员宿舍基础控制器已统一在 `/api/admin/dorm`，学生扫码入住在 `StudentDormServiceImpl` 创建 `ACTIVE`/`active_flag=1` 历史行并把资产套装置 `OCCUPIED`；管理员入住记录、退宿、换寝接口和页面尚不存在，可在该唯一宿舍栈中扩展。
- **数据库只读门禁通过：** 十一张指定表均存在；`qh_asset_set.status` 为非空 `varchar(16)`、默认 `AVAILABLE`，`qh_dorm_bed.status` 为非空 `tinyint`、默认 `1`。结合现有扫码服务对 `AVAILABLE`/`OCCUPIED` 的同一状态规则与设计文档的目录语义，表结构可安全表达退宿和换寝，无需增量 SQL。`previous_checkin_id` 外键足以从新入住记录关联原入住记录。
- 管理端后端将扩展既有 `DormAssetAdminController` 的 `/api/admin/dorm` 前缀，不新建平行控制器；分页类型实际为 `com.qinghe.life.common.PageResult`。管理员身份只可用 `AdminContext.getAdminId()` 取得。
- 实现进展：已新增管理员入住查询/详情、退宿、换寝、可用目标床位接口和后台页面，并复用 `AdminContext`、`@OperateLog`、`active_flag` 历史规则与资产套装状态。后端仅主代码编译和前端生产构建已通过；专项测试、完整回归、打包和全部文档收口仍未完成，不能作为交付完成结论。
# 2026-07-18 普通订单创建链路：金额契约恢复

- 用户已批准真实数据库列 `qh_order.total_amount` 作为订单商品小计：`total_amount = sum(qh_order_item.subtotal)`，`pay_amount = total_amount - discount_amount + delivery_fee`。`discount_amount` 和 `delivery_fee` 本轮均由后端写入非负 `BigDecimal`，初始均为 `0.00`。
- 订单实现门禁不再引用不存在的 `goods_amount`，且不得因接口别名或设计名称与真实列名不同停止。仅缺少不可替代业务字段、主键或必要关联才阻断；历史数据必须由真实 `SELECT COUNT(*)` 确认，不使用 `information_schema.table_rows` 估算。
- 本轮订单创建请求只允许购物车项 ID、地址 ID 和备注；用户、价格、金额、库存、状态和快照均须在服务端事务内重新取得或计算。库存必须经 MySQL 条件更新扣减，任一商品失败时整体回滚。
- 实时只读元数据（2026-07-18）：`qh_order.total_amount`、`discount_amount`、`delivery_fee`、`pay_amount` 均为 `DECIMAL(10,2)`；`order_no` 为唯一索引，`user_id`、`shop_id` 及用户/店铺状态时间索引存在。`qh_order_item.order_id` 有关联索引，`goods_price`、`subtotal` 为 `DECIMAL(10,2)`。实时 `COUNT(*)` 为订单 0、明细 0。
- 门禁未查询 `goods_amount`。字面字段核对显示 `qh_user_address` 没有 `address_area`、`qh_goods` 没有 `status`；需以现有实体/Mapper 和完整表列确认其真实等价字段，再决定服务端映射，不能创建或迁移重复列。
- 等价字段确认完成：地址快照的 `address_area` 应来自 `UserAddress.area`（真实列 `qh_user_address.area`）；商品可售判断应使用 `Goods.saleStatus`（真实列 `qh_goods.sale_status`）且值需与既有购物车逻辑的 `ON_SALE` 一致。两者不是结构缺失，不阻断本轮。
- 静态实体盘点：`Order` 已将 `totalAmount` 自动映射至 `total_amount`，但尚未声明 `deliveryFee` 和完整结构化地址快照字段；`OrderItem` 的商品快照字段已齐全。`CartMapper` 已用 `sale_status='ON_SALE'` 与店铺 `status=1` 做加购实时校验，可作为订单校验语义基线。
- 现有栈审计：`CartServiceImpl` 已从 `UserContext` 取用户、按用户隔离购物车，并使用 `ON_SALE`、店铺 `status=1` 和当前库存校验。`AddressServiceImpl` 已按用户隔离地址并将校区目录名称保存在 `Campus` 中，订单服务应读取地址的区域/楼栋快照和当前 `Campus.campusName`，而不能复用可变地址的拼接结果。
- 现有 `OperateLogAspect` 已掩码 Token 和手机号，但地址各字段不是敏感字段；订单创建响应必须设计为不回传完整手机号和完整地址，日志请求仅有 ID/备注即可满足脱敏边界。
- 实现决策：订单创建响应 `OrderCreateVO` 只含订单 ID/号、店铺、四类金额和不含收件人电话、详细位置的地址摘要；请求 DTO 不含用户、商品、金额、库存或状态字段。订单主表保存完整不可变地址快照，明细保存实时数据库商品快照。

# 2026-07-18 普通订单自动化收口：新增发现与阻断

- `qh_cart` 的 `(user_id, goods_id)` 唯一键意味着订单测试不能为同一用户/商品构造两条购物车记录；专项夹具已改为使用不同测试商品，并精确删除本次用户和商品关联的购物车记录。
- 订单创建原先未验证 `Cart.shopId` 与 `Goods.shopId` 是否仍一致，商品改店后可能把旧购物车项错误纳入新店铺订单；现已在服务端事务的实时商品查询后返回 409 并要求重新加购。
- 校区存在不足以证明校园地址仍有效。现已同时验证地址楼栋存在、启用且属于地址校区；专项测试以真实存在但 `status=0` 的临时楼栋覆盖该分支，避免绕过外键伪造无效 ID。
- `OrderCreateDTO` 忽略未知 JSON 字段，客户端伪造 `totalAmount`、`payAmount`、`deliveryFee`、库存等字段不会影响服务端重新查询和 `BigDecimal` 计算。成功页面不再读取可被编辑的 URL 金额参数，只读取创建响应的路由 state。
- Maven 临时 `subst Q:` 可使 JVM 使用 `Q:\backend` 与 `Q:\.m2`；PowerShell `New-PSDrive` 不可替代。`dependency:go-offline` 发生 64 秒超时但缓存保留，之后专项 Maven 命令可完成 182 个主源码和 21 个测试源码编译。
- 当前硬阻断不在订单范围：`AdminDormCheckinServiceImpl` 引用不存在的 `AdminDormCheckinVO.setPreviousCheckinId(Long)`，使最后一次订单专项在 Surefire 前失败。尚无全绿订单专项、完整回归、零残留、后端打包或本轮前端构建结果。
# 2026-07-18 宿舍编译基线与订单创建验证收口

- 待源码核验的明确阻塞：`AdminDormCheckinServiceImpl` 调用 `AdminDormCheckinVO.setPreviousCheckinId(Long)`，而当前 VO 缺少该属性；不能用空 setter 掩盖。
- 前序只读记录表明 `qh_dorm_checkin.previous_checkin_id` 是换寝创建新有效入住时指向原入住记录的历史关联，且学生端 VO 不应暴露内部关联字段。本轮仍须以当前实体、Mapper/服务映射、管理员接口契约和前端实际使用为准。

## 2026-07-18 管理员入住专项审查与测试证据

- 管理员 HTTP 路由已由 `AdminAuthInterceptor` 保护；为防止服务被非 HTTP 调用绕过，`AdminDormCheckinServiceImpl` 的分页、详情和可用床位查询也显式校验 `AdminContext.getAdminId()`。退宿与换寝均已有 `@Transactional(rollbackFor=Exception.class)`，只锁定/关闭 `active_flag=1` 的记录；床位 `status` 只用于目录启停，资产状态只使用 `AVAILABLE/OCCUPIED`。
- 管理端响应现仅输出脱敏学号和电话；移除了 `previousCheckinId`，不会返回 `activeFlag`、二维码 Token、密码散列等内部字段。操作日志切面继续掩码 Token、姓名、学号、电话，并对办理原因整体掩码。
- `AdminDormCheckinIntegrationTest` 已覆盖管理员/普通用户门禁、分页与所有筛选、当前/历史区分、详情关联与脱敏、退宿历史/资产/床位目录、退宿回滚、日志安全、换寝历史/资产/学生端联动、拒绝场景、同目标床位并发、插入/资产写入回滚与 `@AfterAll` 残留断言。测试数据使用本次运行唯一 `ADMIN_CHECKIN_TEST_***` 前缀，并只删除所记录的精确 Redis Key。
- 专项验证实测通过：`mvn -Dmaven.repo.local=Q:\.m2 -Dtest=AdminDormCheckinIntegrationTest test` 为 7 tests、0 failures、0 errors、0 skipped。完整 `clean test` 的 20 份 Surefire XML 汇总为 68 tests、0 failures、0 errors、0 skipped；后端打包和前端构建均完成，本轮已可标记“已完成并验证”。
- 当前源码、测试和 SQL 设计一致：`DormCheckin` 持久化该字段，`transfer(...)` 写入 `old.getId()`；`AdminDormCheckinIntegrationTest` 明确要求管理员列表和详情也不序列化 `previousCheckinId`。因此字段不是正式管理员响应契约，采用方案 B 的语义：保留持久化历史关联，移除/不恢复任何 VO 映射调用，学生端同样不暴露。

## 2026-07-18 管理员宿舍资源管理修复

- 本轮仅修复管理员 `/admin/dorm` 的宿舍资源目录和资产套装管理。必须使用真实库的 `building_type`、状态枚举、唯一索引与外键作为契约；文档和旧实现不是替代证据。
- 安全边界已固定：`qh_dorm_checkin.active_flag=1` 表示当前入住；床位 `status` 仅表示目录启停；已有资产套装时不得直接改寝室号或床位号；所有删除均须以无下级资源且无历史为前提。

- 实时只读门禁：宿舍楼类型的真实取值为 `宿舍楼`；`qh_dorm_bed.dorm_room_id`、`qh_asset_set.dorm_bed_id`、`qh_asset_set.asset_set_no` 均存在且分别有唯一约束。资产真实字段为 `asset_name`、`asset_type`、`asset_status`、`remark`，状态仅有 `NORMAL`、`REPAIR`、`SCRAPPED`。
- 状态与历史契约：寝室/床位目录 `status` 是 `tinyint` 1/0；资产套装状态为 `AVAILABLE`、`OCCUPIED`、`MAINTENANCE`、`RETIRED`，二维码状态为 1/0；当前入住严格以 `qh_dorm_checkin.active_flag=1` 判定。资产 `(asset_set_id, asset_type)` 与套装 `(dorm_bed_id)` 唯一，适合五件资产的幂等初始化。
- 基线：`qh_dorm_room=1`、`qh_dorm_bed=2`、`qh_asset_set=1`、`qh_asset=5`、`qh_dorm_checkin=0`、当前入住 0；`UATA` 的寝室行存在（ID 251），无入住历史。门禁查询仅使用 `information_schema` 和只读 `SELECT`，未执行 DDL/DML。

- 源码基线：唯一入口是 `DormAssetAdminController` 与 `DormAssetServiceImpl`；现有 `listBuildings`、`listAvailableBuildings` 未过滤楼栋类型，寝室/床位只有新增，资产套装已具备固定五件资产与 PNG/ZIP 二维码能力但页面仅显示资产名称。
- 现有服务已在每个宿舍管理方法读取 `AdminContext`，二维码响应只输出动态 PNG；资产套装 `qrToken` 未在 VO 中暴露。修复应扩展该服务而非新增控制器、会话、AOP 或日志表。

- 首轮实现将宿舍资源查询和可选楼栋统一限制为真实 `building_type=宿舍楼`；新增寝室/床位编辑、状态、条件删除和资产状态/备注编辑接口。服务端在入参前再次验证父级为宿舍楼，避免直接调用接口绕过页面过滤。
- `Q:` 临时短路径主代码编译成功（191 files）。当前还没有本轮专项测试或全量回归证据，不能标记完成。

- 新增 `AdminDormResourceIntegrationTest` 覆盖目录过滤、空 `UATA` 安全改号、寝室/床位编辑启停删除、资产套装幂等五件资产、资产编辑、二维码及当前入住拦截；测试代码完成 testCompile。
- 运行时阻断不是断言或编译问题：Redis `192.168.100.128:6379` 拒绝连接，管理员登录被全局基础设施处理器转换为 HTTP 503，专项 3 failures/0 errors。按项目限制未启动、停止或修改 Redis，不能宣称测试通过。
- 打包与前端构建均成功；`ADMIN_DORM_RESOURCE_TEST_` 精确残留查询为楼栋、寝室、床位、套装、资产、入住、管理员全部 0。
# 2026-07-18 宿舍模块 MyBatis-Plus 有限整改：初始约束

- 本轮目标是消除重复单表 CRUD 的自定义 Mapper/XML，而非消灭 Mapper。
- 已知宿舍服务中已有大量 `selectById`、`insert`、`updateById`、`deleteById`、`selectList`、`selectCount` 与 Lambda Wrapper 调用；是否存在可删自定义 SQL 必须以后续方法级审计和调用点为准。
- 残留检查不得通过手工 SQL 进行；仅能引用现有专项测试的精确清理/断言与 Maven 测试输出。
## 2026-07-18 宿舍模块 MyBatis-Plus 有限整改：Mapper/XML 审计

- 宿舍域 8 个 Mapper 均为无方法的 `BaseMapper<T>`：`CampusMapper`、`BuildingMapper`、`DormRoomMapper`、`DormBedMapper`、`AssetSetMapper`、`AssetMapper`、`DormCheckinMapper`、`StudentProfileMapper`。没有重复的自定义单表 CRUD 方法。
- `backend/src/main/resources` 没有 MyBatis Mapper XML；`sql/dorm_asset_increment.sql` 是未执行的增量脚本，不是映射文件，不应删除或改写。
- `DormAssetAdminController`、`StudentDormController`、`CampusController` 未直接引用任何 Mapper；DTO、VO、Entity 的包与映射维持分离。
- N+1 风险（本轮只记录，不做扩大整改）：`AdminDormCheckinServiceImpl.page()->view()` 对每条入住记录查询学生资料；`availableBeds()` 对每个资产套装逐条查询床位、寝室、楼栋、校区和入住数；`DormAssetServiceImpl.listBuildings()/listAvailableBuildings()` 对每栋楼重复查询校区及资源统计；`listRooms()/listBeds()` 的每条 VO 补查统计/资产套装/入住数。若后续性能整改，应新增聚合 Mapper 查询，不可继续以循环查询替代联表。
## 2026-07-18 宿舍模块 MyBatis-Plus 有限整改：静态复审和测试前置条件

- Controller 边界已复核：`DormAssetAdminController` 仅注入 `DormAssetService` 与 `AdminDormCheckinService`；`StudentDormController` 仅注入 `StudentDormService`；`CampusController` 仅注入 `CampusService`。未发现 Controller 直接调用 Mapper。
- Entity 均以 `@TableName` 映射；DTO/VO/Entity 未混用，本轮未作任何业务或接口代码修改。
- 静态专项基线：26 个宿舍相关 `@Test`（3+8+3+2+10）。`AdminDormCheckinIntegrationTest` 与 `StudentDormIntegrationTest` 在 `@AfterAll` 断言标记测试数据和 Redis Token 为零；其他宿舍测试包含清理逻辑，但运行后结论不能由静态审阅替代。
- 验证阻断：`Q:\backend` 不存在。为遵守只操作当前仓库和不擅自修改外部环境的规则，未创建 `subst`/PSDrive，未在不同路径替代运行 Maven。

## 2026-07-18 宿舍二维码完整交互链路修复：初始事实

- 历史交接记录显示学生二维码解析和入住服务端链路曾随 `StudentDormIntegrationTest` 通过，学生响应已刻意避免公开完整 `qrToken`；本轮应优先复用而非重建解析或入住事务。
- 管理员宿舍资产已有动态 PNG 与批量 ZIP 下载能力；本轮要补齐可靠查看体验、Object URL 生命周期和权限/下载回归。
- 本轮禁止数据库结构变化、SQL、服务控制、Git 及二维码图片上传/持久化。任何完整测试、构建或人工浏览器结果均须以当前实际输出为准，不能沿用历史通过记录。

## 2026-07-18 宿舍二维码完整交互链路修复：审计结论

- `DormScanView.vue` 当前只调用原生 `BarcodeDetector`，而 `frontend/package.json` 与已安装依赖均没有静态图片/视频二维码解码库。因此不支持该实验性 API 的浏览器必然显示“浏览器不支持自动识别”；图片、摄像头都没有可用兜底。
- 现有图片选择已经是隐藏 input，但只在原生 API 成功时有单一简短反馈；摄像头在原生 API 不可用时会保留已获取的媒体流，定时器错误也被吞掉，且未显示可审计的扫描阶段。
- 后端二维码 PNG 由 ZXing 动态生成，编码内容为既有精确格式；学生解析服务已校验 Token 格式、`qr_status`、目录、套装状态、五件固定资产健康状态和当前入住，且解析响应不返回 Token。管理员 PNG/ZIP 接口已由 `/api/admin/dorm/**` 门禁保护。
- 管理员页的“查看二维码”实际调用 `showSet`，仅刷新资产详情，没有读取 PNG；下载功能存在但未对 Object URL 的查看生命周期建模。

## 2026-07-20 学生首次建档多校区与 Redis 测试分层：结构门禁

- 已通过对本机 `qinghe_life` 的只读 `information_schema.COLUMNS` 查询确认：`qh_student_profile.campus_id` 存在，类型为 `bigint`、`NOT NULL`、普通索引（`MUL`）。本轮未执行任何 DDL/DML 或 SQL 导入。
- 现状缺陷已定位：`StudentDormServiceImpl.saveProfile()` 在首次建档调用 `singleEnabledCampus()` 推断校区，因而在启用校区数量不为 1 时会阻断建档；实体已有 `campusId`，但请求 DTO、学生安全 VO 和 `DormScanView.vue` 均未承载校区选择。
- 现有扫码入住保持学生资料校区与二维码宿舍校区一致校验；该事务不在本轮修改范围。地址校区和扫码结果均不可代替首次建档的明确 `campusId`。
- 首次读取前端 API 时误用了 `studentDorm.js` 文件名；真实文件为 `frontend/src/api/student-dorm.js`。该读取失败未修改任何文件，后续改用真实路径。

## 2026-07-20 学生首次建档多校区与 Redis 测试分层：实现与验证结论

- `StudentProfileSaveRequest` 已承载 `campusId`；服务端仅在首次建档读取该编号并通过 `CampusMapper.selectById` 校验 `status=1`。已建档分支仍只更新联系电话，任何客户端 `campusId` 或未知 `campusName` 都不能改写学生所属校区。
- `StudentProfileVO` 安全返回 `campusId/campusName`，不增加内部资料 ID、版本标记、Token 或二维码字段。`DormScanView.vue` 复用现有 `GET /api/campuses`，首次显示启用校区下拉；建档后仅展示服务端校区名称。
- `StudentDormIntegrationTest` 增加了“两个启用校区仍能按提交值建档、伪造 campusName 不生效、停用校区被拒绝”的覆盖，并扩展了带前缀校区测试数据的清理。该类及 `AdminAuthenticationIntegrationTest` 均为真实 Redis 集成测试，未在沙箱使用 Mock 冒充通过。
- 指定后端 Maven 命令均未能开始项目编译或打包：修正 PowerShell 参数引用后确认 `Q:\.m2` 不可创建/访问；`compile` 与 `clean package -DskipTests` 均在本地仓库初始化处停止。未创建映射盘、未修改 Maven/Redis 配置。前端 `D:/develop/NodeJS/npm.cmd run build` 在授权读取项目配置后成功（1731 modules），仅有既有 PURE 注释和大 chunk 警告。

## 2026-07-20 管理员宿舍楼按校区管理：只读门禁初步结果

- 真实库中有两个启用校区：`QH_MAIN` 有 2 栋真实 `building_type=宿舍楼`（均启用），`QH_DIST` 当前为 0 栋。故选择分校区时的 No Data 是真实空结果，不能按缺陷修复；主校区若显示 No Data 则必须排查前端参数或加载链路。
- 真实 `qh_building.building_type` 还存在 `DORM`、`教学楼`、`图书馆`；宿舍模块的正确筛选常量是中文 `宿舍楼`，不能把 `DORM` 或通用目录楼栋混入本页。
- 现有后端 `GET /api/admin/dorm/buildings` 已使用 `campusId/status/keyword` 并过滤真实宿舍楼，创建、编辑、启停接口已存在；缺少条件删除端点，且创建/编辑 DTO 仍允许客户端指定其他楼栋类型，未满足本页固定宿舍楼类型的边界。
- 当前主校区两栋宿舍楼中：松园 1 号已有寝室、床位、资产套装和 1 条当前入住；桂园 2 号无下级资源与入住历史，可作为条件删除允许的基线。未执行任何 SQL。
- `AdminDormAssetView.vue` 启动后先获取启用校区、默认选择首个校区，并以数字型 `campusId` 调用 `/api/admin/dorm/buildings`；后端同名参数亦正确参与筛选。因此主校区 No Data 才是缺陷信号，分校区 No Data 是当前数据基线。
- 现有分页和可用楼栋列表在逐条 `buildingView()` 中分别查询校区及下级资源，存在 N+1。为满足本轮统计/关联检查边界，应将楼栋页统计收敛到 `BuildingMapper` 的聚合查询；基础 insert/update/delete 保持 MyBatis-Plus。

## 2026-07-20 管理员宿舍楼按校区管理：阶段 2 源码复核

- 当前 `DormAssetServiceImpl` 已固定列表查询的真实类型，并通过 `AdminContext` 和 `@OperateLog` 保护写端点；仍须确认创建/编辑请求不接受可变 `buildingType`、状态更新和条件删除的关联理由完整、以及统计不再逐栋 N+1 查询。

## 2026-07-20 管理员宿舍楼按校区管理：实现与回归结论

- `AdminDormBuildingCreateRequest` 仅保留当前校区、编码、名称、区域、备注和状态；类型由服务端固定，编辑 DTO 不含校区/类型。`AdminDormBuildingQuery.campusId` 为必填并在服务层校验启用状态。
- 楼栋响应和页面已包含床位数；前端从当前校区生成创建请求，未显示 campusId/buildingId 输入。停用、删除二次确认及后端中文阻断原因均已接入。
- `BuildingMapper.selectDormBuildingStats` 是保留的自定义聚合 Mapper；`insert`、`updateById`、`deleteById`、`selectPage`、唯一性检查均使用 MyBatis-Plus。无新 XML、控制器无 Mapper 依赖。
- 实测楼栋/入住专项 11/0/0/0，且测试类的前缀清理断言通过。完整回归在替代 Maven 缓存下为 76 项、2 failures、0 errors；剩余重复寝室 409 失败不能通过弱化测试解决，因会掩盖既有唯一性规则未被实时环境保证的问题。

- 当前源码最终 `clean package -DskipTests` 再次通过（204 主源码、23 测试源码），JAR 已重新生成；这只证明编译/打包，不替代完整回归的失败结论。
# 2026-07-20 个人中心姓名显示语义修复

- 根因已定位：`GET /api/user/me` 的 `UserServiceImpl.currentUser()` 直接返回认证拦截器从 Redis 会话还原的 `UserDTO`。该 DTO 只有账号信息（含 `username`、`nickname`、脱敏手机号）且不读取 `qh_student_profile`；因此管理员更新当前学生资料中的 `real_name` 后，刷新个人中心也不会得到最新实名。
- 前端进一步固化了该偏差：`ProfileView.vue` 顶部和 `UserLayout.vue` 导航只读取 `profile.nickname`，个人中心卡片和编辑弹窗仍将可空的 `username` 称为“用户名”。
- 现有学生资料接口位于 `StudentDormController` 与 `StudentDormServiceImpl`，不是推断的 `StudentProfile*` 类。它已按 `user_id + current_flag=1` 查询本人资料，并以 `StudentProfileVO` 安全返回实名、学号与学籍资料；本轮不改该建档/入住链路。
- 账户与学生字段语义：`qh_user.phone` 为登录手机号；`qh_user.nickname` 为账号昵称；`qh_user.username` 为可空账号用户名，不能作实名回退；`qh_student_profile.real_name` 为当前实名资料；`student_no` 为学号。
- 后端修复采用 `UserServiceImpl.userView`：按当前 `UserContext` 用户 ID 查询 `qh_user` 后，用 MyBatis-Plus 查询同一用户 `current_flag=1` 的 `StudentProfile`，仅映射 `realName/studentNo/hasStudentProfile`。`currentUser()` 不再只返回 Redis 旧快照，因此资料管理员改名后的刷新可见；同步会话只写安全字段。
- 前端共享 `formatDisplayName`，个人中心并行读取账号和学生资料；导航从刷新后的安全 `UserDTO` 使用同一函数。实名资料不存在、请求失败、实名为空和账号资料刷新失败有不同且不阻断页面的中文反馈。
- 本会话 `Test-Path Q:\backend` 与 `Test-Path Q:\.m2` 均为 `False`，指定 Maven 验证命令尚不可运行；未创建驱动器映射、未改 Maven/Redis 配置。
- 受控专项 Maven 测试结果为 6/0/0。实际工作区完整回归为 77/1/0：唯一失败为范围外 `DormAssetIntegrationTest` 的宿舍资产状态断言（期望 409、实际 200），与本轮姓名查询、DTO 映射、个人中心和导航修改无直接调用关系；按约束未以降低断言或改动范围外逻辑换取通过。
- 用户指定的跳过测试打包尚未运行：`Q:` 路径不存在，实际工作区的受控打包申请又被平台额度限制拒绝。前端构建已成功，后端主/测试源码均在完整回归中通过编译。
# 2026-07-23 GitHub publishing findings

- Authenticated GitHub connector login is `tzjk`; the GitHub CLI is not installed, so the user-provided HTTPS remote is used directly.
- `origin/main` already exists at `5690a1d` with a README-only initial commit. A normal merge with unrelated histories is required; force-push is out of scope.
- Initial staging exposed local dependency caches (`.m2/` and `backend/.m2-order-test/`). Both are now ignored, and the latter was removed from the index without deleting local files.
# 2026-07-23 GitHub publishing result

- Local and remote `main` both resolve to `68efe71a531757dc11c6bafa2f4c18d8dcda60c5` after the first push.
- The remote's pre-existing `5690a1d Initial commit` remains in history. Its one-line README was reconciled in favor of the fuller local README without force-pushing.
- The initial push command exceeded the shell wait limit, but the follow-up tracking push reported `Everything up-to-date`; hash verification confirms the push completed successfully.
# 2026-07-23 Git ignore findings

- The repository previously ignored build output, dependency caches, and IDE metadata but not local `.env` files or certificate/private-key formats.
- `application.yml` and `.env.example` remain intentionally trackable templates; real credentials must continue to use local environment values rather than repository files.
# 2026-07-23 宿舍管理需求只读复核

- 需求 1 已具备核心实现：管理员按床位生成幂等资产套装，编号为 `楼栋编码 + 寝室号 + '-' + 床位号`（测试示例 `JA101-01`），同步生成随机 64 位令牌二维码；套装自动补齐床、床板、书桌、衣柜、凳子五件固定资产，并支持 PNG/ZIP 下载、停用和轮换二维码。
- 需求 2 已具备完整服务端与页面链路：学生先录入实名、学号、学院、专业、班级、联系电话和校区，再以摄像头、二维码图片或手动内容解析床位二维码并确认入住。服务端校验当前实名资料、`ENROLLED` 学籍、校区一致性、资产健康、二维码状态和当前入住唯一性。
- 需求 3 已具备管理员受保护的学籍管理（转专业、休学、退学、复学、毕业）及预览后二次确认的批量毕业、批量退宿、资产释放、二维码停用/轮换。系统不物理“清空入住信息”，而以批量退宿关闭当前入住、保留历史并释放套装，符合可追溯性。
- 主要未收口风险：最新记录的完整 Maven 回归为 77 tests、1 failure、0 errors；`DormAssetIntegrationTest` 期待重复寝室号返回 409，实际为 200。该差异会削弱资产编号 `楼栋+寝室号+床位号` 的唯一性前提，故需求 1 不能判为完全验收通过。未在本次只读复核中运行服务、SQL 或测试。

## 2026-07-23 重复寝室号 409 修复

- 根因是 `DormAssetServiceImpl.createRoom/updateRoom` 仅依赖数据库重复键异常；当前环境未触发该约束，因而重复寝室号可返回 200。
- 已在两条写入路径增加 `(building_id, room_no)` 的 MyBatis-Plus 主动查重，排除编辑目标自身；冲突使用 `BusinessException(409, ...)`，确保 HTTP 409 与既有专项测试一致。数据库重复键捕获同样改为 409 兜底。
- 专项测试编译成功，但运行在测试清理阶段因 Redis `192.168.100.128:6379` 连接超时而 2 errors，未触发业务断言；需在可访问 Redis 的本机执行同一专项复核。
- 本地历史快照为 `dd7149e fix: enforce dorm room number uniqueness`；快照未包含用户已有 `.gitignore` 修改。

## 2026-07-23 订单生命周期状态模型与候选迁移

- 已新增 `OrderStatus`：`PENDING_PAY`、`PAID`、`ACCEPTED`、`DELIVERING`、`COMPLETED`、`CANCELLED`，提供编码、中文名称、编码解析和合法流转判断。订单创建和专项断言均改用 `OrderStatus.PENDING_PAY.getCode()`；未新增任何状态变更接口。
- 新候选脚本 `order_lifecycle_schema_increment.sql` 仅提出 `pay_time`、`accepted_time`、`delivery_time`、`pay_expire_time` 和 `(status, pay_expire_time)`；静态核对确认既有 `order_core_increment.sql` 已定义 `cancel_reason`、`cancel_time`、`completed_time`，新脚本不重复添加。脚本要求先用 DataGrip 运行 `SHOW CREATE TABLE qh_order;` 与 `SHOW INDEX FROM qh_order;`，本轮没有执行 SQL。
- 当前会话 `Q:\backend`、`Q:\.m2` 均不存在，用户指定的 compile 与 `OrderCreateIntegrationTest` 命令无法开始。未创建路径映射、未调整 Maven/Redis 配置，也未用其他命令冒充指定验证结果。
- 后续取消订单的设计固定为“条件状态更新、库存恢复、成功日志”同处 `REQUIRED` 订单事务，直接写现有 `qh_operate_log`，并避免 `REQUIRES_NEW` 通用 AOP 成功日志重复写入。

## 2026-07-24 订单生命周期实现与验证

- 只读 `information_schema` 已确认候选生命周期列和超时索引都实际存在，因此 `Order` 安全映射时间/取消字段；候选迁移脚本未执行。
- 取消实现先执行 `id + status`（用户请求另加 `user_id`）条件更新，成功后才恢复每个 `qh_order_item` 对应库存并在同一业务事务直接写一条 `qh_operate_log`；通用 `@OperateLog` 未标注这些关键端点，避免其 `REQUIRES_NEW` 成功日志重复记录。
- 支付、取消和超时扫描共享同一待支付状态竞争边界；管理员状态机只暴露三个固定操作。订单/明细列表以订单分页、批量店铺/用户读取和自定义批量明细 Mapper 组装，未产生逐订单查询。
- 首次专项运行暴露测试订单号超列长度及先前失败导致的孤立测试订单；均只修正测试夹具与精确清理锚点。最终专项 13/0/0/0，前缀数据残留为 0；后端打包与前端构建均通过。
# 2026-07-24 Coupon duplicate-claim audit

- `CouponServiceImpl.claim` is transactional and obtains identity only from `UserContext`; existing `(user_id,coupon_id)` is returned before decrement/insert, but `UserCouponVO` does not distinguish the first and duplicate claim.
- `CouponVO` and `pageAvailable` have no per-user claim fields. One bulk `qh_user_coupon` query for coupon IDs in the current page can map state without N+1.
- `uk_qh_user_coupon(user_id,coupon_id)` is the persistence backstop for one-person-one-coupon. Retain the conditional decrement and transaction behavior under concurrency.
- `CouponsView.vue` only marks an in-flight request; it does not disable claimed coupons, update the card immediately, or give a specific retry message.

## 2026-07-24 Coupon duplicate-claim final findings

- The duplicate response is a normal 200 business result (`ALREADY_CLAIMED`), never a 500. The front end branches on `claimStatus`, not the Chinese message.
- The refreshed-list test proves `AVAILABLE`, `LOCKED`, `USED`, and `EXPIRED` user coupons all return `claimed=true` and cannot be claimed again.
- The concurrent-claim test proves one user-coupon row and one stock decrement remain after two simultaneous calls; the persistence unique key and conditional decrement stay in force.
- Git push evidence: `git push origin feature/coupon-foundation` failed before remote contact because the configured local proxy at `127.0.0.1` refused the GitHub HTTPS connection. The local commit remains available for a later normal push.
# 2026-07-24 Coupon seckill stream milestone

- Start gate: `feature/coupon-seckill-stream` is current and clean (`git status -sb` and `git branch --show-current` executed once as requested).
- Scope is limited to coupon entities/mappers/services/controllers, `qh_coupon` and `qh_user_coupon`, Redis/Redisson configuration, Maven/application configuration, the coupon Redis design, and the two coupon-focused integration tests.
- Existing source inventory includes `backend/src/main/resources/sql/seckill_coupon_increment.sql`; it is a candidate only until schema verification confirms whether a manual index review is necessary. It will not be imported or executed.
- Live read-only MySQL verification confirmed `qh_user_coupon.uk_qh_user_coupon (user_id, coupon_id)` exists. No unique-index candidate SQL is required. `qh_coupon` has `available_stock`, receiving windows, and `status`, but no dedicated activity column; the existing `coupon_status` field is the smallest non-migration activity marker for this milestone (`SECKILL` only).
- The legacy `seckill_coupon_increment.sql` proposes separate activity/order tables, which conflicts with this milestone's required final persistence in `qh_coupon` and `qh_user_coupon`; it remains untouched and will not be used.
- Redis and Redisson dependencies/configuration already exist. Scheduling is enabled and the existing Redisson lock pattern is reserved for scheduled work, not a claim request.
- Verification blocker: the user-mandated Maven command could not create `Q:\.m2` and exited before compilation with `Access is denied`. Per scope, no alternate repository path, configuration mutation, fake Redis test, SQL execution, commit, or push was attempted.

## 2026-07-24 Seckill consumer-group API compatibility

- The source used `StreamOperations.create(streamKey, ReadOffset.from("0-0"), groupName)`, which does not exist in the Spring Data Redis 2.7.18 API selected by Spring Boot 2.7.18.
- `StreamOperations.createGroup(streamKey, ReadOffset.from("0-0"), groupName)` is available, but it cannot request Redis `MKSTREAM`; `RedisStreamCommands.xGroupCreate(key, group, ReadOffset.from("0-0"), true)` is the smallest compatible command path for creating an empty stream and its group without adding a synthetic claim message.
- The follow-up must treat only a Redis `BUSYGROUP` response as idempotent success. Connection, authorization, and all other command failures remain observable exceptions.
- Main-source verification succeeded: `mvn -DskipTests compile` compiled all 236 main sources after the change. The Maven warning reports unchecked operations in `CouponSeckillServiceImpl` without a source location; no raw type is introduced by the small `RedisCallback<String>` call, so no broad warning-only refactor was made.
- The requested focused test command was retried after successful main compilation, but `testCompile` failed before either target test ran: all integration tests report their `com.qinghe.life.*` imports as missing while `target/classes` contains and `javap` resolves `com.qinghe.life.entity.Coupon`. A read-only Maven diagnostic also hit `AccessDeniedException` creating a tracking directory under `D:\maven\apache-maven-3.9.11\Repository`. These failures are not caused by, or safely repairable within, the seckill coupon scope.
# 2026-07-24 — Order WebSocket notification milestone

- Scope is authenticated user/admin order notifications only. Existing order state transitions and database schema must remain unchanged.
- The notification transport is advisory: HTTP order queries remain the source of truth after refresh or reconnect.
- Existing transaction boundaries are `OrderServiceImpl#create`, `simulatePay`, the three administrator transitions, and `OrderCancellationService` for user/timeout cancellation. An application event plus `@TransactionalEventListener(AFTER_COMMIT)` can cover every successful path without changing state rules.
- User authentication is a Redis hash at `qh:login:token:{token}` and administrator authentication is a Redis hash at `qh:admin:token:{token}`. Native browser WebSocket cannot set `Authorization`; the chosen fixed-path handshake carries the existing token in `Sec-WebSocket-Protocol`, never in a URL.
- Axios keeps user and administrator tokens separately in localStorage. The order pages currently reload via HTTP after local operations, so WebSocket handling can do idempotent in-memory updates and still rely on HTTP on first load/reconnect.
- Focused Maven verification passed 31/0/0/0: new `OrderWebSocketIntegrationTest` is 16/0/0/0 and existing lifecycle/timeout tests complete the requested run. Backend package and frontend production build also passed.
- The first frontend build was blocked by sandboxed esbuild file reads; the single controlled real-path retry built 1742 modules successfully. Vite reported only existing third-party PURE-comment and bundle-size warnings.
