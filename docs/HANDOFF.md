# 青禾校园生活服务系统 V1.0 交接记录

## 2026-07-20 学生首次建档多校区与 Redis 测试分层

- 只读结构门禁已确认 `qh_student_profile.campus_id bigint NOT NULL` 存在；未执行 SQL。首次学生建档不再通过“唯一启用校区”推断，改为校验客户端提交的真实启用 `campusId`；响应安全返回 `campusId/campusName`，已建档学生仍只能更新联系电话。
- `DormScanView.vue` 首次建档新增启用校区下拉，保存不发送 `campusName`；二维码床位校区继续只作入住一致性校验。本轮没有修改扫码入住、退宿或换寝事务。
- 测试分层已写入接口契约：Mockito 可用于纯单元测试的 Redis 依赖替身；管理员登录、Token、验证码、Redis 会话和学生宿舍会话的集成测试必须使用 Windows 本机可访问的真实 Redis。Codex 沙箱无法访问 `192.168.100.128:6379` 是环境隔离，不能修改 Redis 配置或伪造完整回归结果。
- 待本机真实 Redis 验证：在仓库 `backend` 目录运行 `mvn -Dmaven.repo.local=Q:\.m2 -Dtest=AdminAuthenticationIntegrationTest,StudentDormIntegrationTest test`；随后按需要运行完整 `mvn -Dmaven.repo.local=Q:\.m2 clean test`。测试数据清理断言必须保持通过。
- Codex 已尝试指定 `compile` 和 `clean package -DskipTests` 参数，但 `Q:\.m2` 在沙箱不可创建/访问，两个命令均未进入后端编译或打包。前端 `D:/develop/NodeJS/npm.cmd run build` 已成功（1731 modules）；真实 Redis 测试与后端 Maven 验证均保持待本机验证。

## 2026-07-19 学籍异动、批量毕业与二维码批量管理（实现完成，验证阻断）

- 已增加管理员学生管理、资料版本查询、转专业、休学、退学、毕业、复学、批量毕业、批量退宿、批量资产释放和二维码停用/轮换；全部复用 `/api/admin/dorm`、AdminContext、唯一操作日志和既有管理员前端栈。未改数据库结构、未执行 SQL、未新增日志表或服务。
- 退宿核心已抽为共享生命周期：锁定当前入住/套装、关闭入住历史、记录原因/管理员、释放套装。学生扫码入住仅允许 `ENROLLED`，不再接受 `TRANSFER_MAJOR` 或 `REINSTATED`。
- 预览令牌用于提交时重核选择范围和状态；每批最多 100 条，任一项失败回滚全批。二维码令牌不在页面/接口响应中公开，批量轮换不修改套装编号。
- 实际验证：`mvn ... -DskipTests compile` 成功（203 个主源码）；前端构建成功（1731 modules）。完整 `clean test` 实际为 77 tests、14 failures、41 errors，主因 Redis 连接失败及由此返回的管理员登录 503；未达到要求的 0 failures/0 errors，故本轮不得标记“完成”。恢复 Redis 后应先运行完整测试，再打包并复核测试前缀数据和 Redis Key 残留。

## 2026-07-19 学籍异动、毕业处理与管理员批量宿舍操作（审计与设计完成）

- 本轮只完成静态审计和分阶段设计，未实现业务、未生成/执行 SQL、未连接数据库、未运行测试或构建、未控制服务、未执行 Git。完整结论见 `docs/academic-dorm-batch-audit.md`。
- 现有学籍版本、入住关闭、资产套装和二维码字段可承载单个异动及受上限的全事务批处理，无需当前迁移。后续统一当前状态为 `ENROLLED`、`SUSPENDED`、`DROPPED`、`GRADUATED`；`TRANSFER_MAJOR`、`REINSTATED` 改为异动类型，现有学生端入住允许集合需在实施阶段收紧为仅 `ENROLLED`。
- 退学/毕业必须和退宿/资产释放同事务；休学由管理员显式选择保留入住或退宿；复学不恢复旧入住。批量毕业走预览、二次确认、100 人上限和整批回滚，不能无条件全表更新或伪造额外日志表。
- 当前没有管理员角色表，所有学籍与批量宿舍操作继续用 `AdminContext` 保护；继续复用唯一 `qh_operate_log`，其安全摘要不得保留完整身份信息或二维码令牌。

## 2026-07-18 管理员校区与楼栋管理（完成并验证）

- 用户已手工增加并复核 `qh_building.remark VARCHAR(255) NULL`，应用只读确认后继续实现，未再次执行 SQL。`area` 是校园区域、`remark` 是管理备注，二者独立持久化。
- 管理员宿舍既有入口新增校区只读列表、楼栋分页/详情/新增/编辑/启停及启用楼栋选择接口；楼栋写操作由 `AdminContext` 取得身份并复用唯一 `qh_operate_log`。新增寝室只信任 `buildingId` 并由后端写入真实校区。
- 编码在同校区唯一并规范为大写；有任何宿舍下级资源即冻结编码，不修改资产套装编号、二维码 Token 或入住历史。存在当前入住时禁止停用；停用只隐藏可用目录，历史资源保留。
- 验证完成：楼栋专项与宿舍资产专项 5/0/0/0，完整 Maven 71/0/0/0，`mvn -Dmaven.repo.local=Q:\.m2 clean package -DskipTests` 成功，前端真实路径 Vite 构建成功（1725 modules）。`ADMIN_DORM_BUILDING_TEST_` 的校区、楼栋、资产、入住、用户、管理员和日志残留均为 0；未控制服务、未执行 Git 或真实 OSS。

## 2026-07-18 管理员校区与楼栋管理：数据库门禁阻断

- 本轮真实 `qinghe_life` 只读审计确认：`qh_campus` 编码/名称全局唯一且 `status=1` 为启用；`qh_building` 的编码、名称均在同一校区内唯一，校区外键为 `ON UPDATE RESTRICT`。`qh_dorm_room` 同时保存校区和楼栋，寝室号在同楼栋唯一；楼栋、寝室和床位状态为目录启停语义。
- 既有楼栋编码接口只是直接更新。资产套装生成编号依赖 `building_code + room_no + '-' + bed_no`，因此后续实现必须在存在寝室、床位或资产套装时禁止改码，不能重写资产编号或入住历史。
- 停止差异：真实 `qh_building` 没有 `remark` 列，而本轮要求创建、编辑、返回并在有下级资源时继续维护备注；`area` 是校园区域，不能替代。请由授权人员在 DataGrip 审核并手工增加可空 `remark VARCHAR(255)`，再只读确认字段存在后重新开始。此次未生成/执行 SQL、未改代码、未运行测试或构建、未控制服务或执行 Git。

## 2026-07-18 订单创建与宿舍编译收口（完成并验证）

- `previousCheckinId` 是换寝来源历史关联，不属于管理员或学生响应契约；当前源码不存在缺失的 VO setter 调用，也未新增空 setter 或虚假字段。
- 订单专项 5/0/0/0，精确清理并断言 `ORDER_CREATE_TEST_` 的用户、地址、店铺、商品、购物车、订单、明细、操作日志、验证码和登录 Token 无残留。
- 完整 Maven 回归 68/0/0/0；`clean package -DskipTests` 成功生成 JAR；真实前端路径构建成功（1725 modules，PURE 注释和大 chunk 为非阻断警告）。未执行手工 SQL、数据库结构变更、服务控制或 Git。
- 人工验收：普通用户选择本人有效校园地址提交同店购物车，核验服务端金额/快照和精确清车；管理员办理换寝并确认历史保留、页面不显示内部关联字段。

## 2026-07-17 普通订单创建链路：数据库门禁阻断

- 用户声明已手工执行 `order_core_increment.sql`，但本轮真实 `qinghe_life` 只读门禁发现 `qh_order` 使用 `total_amount DECIMAL(10,2)`，并不存在设计要求的 `goods_amount`。此差异未被代码别名或文档猜测掩盖，后端订单、前端结算、测试和构建均未开始。
- 已确认 `order_no` 唯一键、用户/店铺索引、订单明细快照字段和金额 `DECIMAL` 类型大体存在；但 MySQL 在引用 `goods_amount` 时返回 1054。订单和明细当前估算各为 0 行，因此无法以现存数据证明迁移前旧订单未丢失。
- 恢复前需要用户在 DataGrip 中决定并完成其一：使真实表符合 `goods_amount` 契约，或明确批准以 `total_amount` 作为订单商品金额并同步修订设计、API 与数据库文档；之后重新从只读门禁开始。不得在本次阻断状态下实现订单或执行构建。

## 2026-07-17 学生资料与宿舍扫码入住：测试收口完成

- 交付范围仅含学生资料、二维码解析、确认入住和我的宿舍的脱敏与测试收口；未实现换寝、退宿、学籍异动、批量操作或资产报修，未改库或新增日志表。
- `StudentProfileVO` 不再公开 `currentFlag`，且学生响应不含 `passwordHash`、完整 `qrToken`、`activeFlag` 或内部 ID。资料建档后只允许联系电话更新，昵称/登录手机号不作为实名资料替代。
- `StudentDormIntegrationTest` 现有 9 项覆盖资料、二维码、事务、并发、回滚、唯一索引业务错误、日志脱敏和我的宿舍；全量 Maven 已通过 56/0/0/0。测试标识数据和 Redis 登录 Key 经测试内精确清理断言确认无残留。
- 构建已通过：后端 `mvn -Dmaven.repo.local=Q:\.m2 clean package -DskipTests` 生成 `backend/target/qinghe-life-backend-1.0.0.jar`；前端 `D:/develop/NodeJS/npm.cmd run build` 成功（1718 modules）。前端仅有既有 PURE 注释和大 chunk 非阻断警告。
- 后续人工验收：以普通用户登录，首次填写资料后尝试修改实名/学籍字段并确认仅联系电话生效；扫描有效二维码、确认入住并查看宿舍脱敏结果；再以另一用户重复扫描同床位，确认被拒绝且不显示前一学生资料。摄像头不可用时验证手动粘贴二维码降级。

## 2026-07-17 管理端商品筛选编译修复（完成）

- `GET /api/admin/goods` 的筛选参数固定为 `shopId`、`shopCategoryId`、`goodsCategoryId`、`saleStatus`、`keyword`。`shopCategoryId` 对应 `qh_category`，经 `qh_shop.category_id` 过滤；`goodsCategoryId` 对应 `qh_goods_category`，经 `qh_goods.category_id` 过滤。
- `AdminGoodsServiceImpl` 已不存在 `AdminGoodsQuery.getCategoryId()` 调用：原三处店铺类型条件均为 `getShopCategoryId()`，商品分类条件独立为 `getGoodsCategoryId()`。保存时继续校验 `category.shopId == goods.shopId`。
- 管理端商品页将店铺类型与商品分类拆分展示；商品分类仅在选择具体店铺后按该店铺加载，未选择店铺时禁用，避免不同店铺同名分类混用。
- 验证：后端主代码 `compile` 成功（170 个源文件），前端生产构建成功（1718 modules）。`clean test` 和 `clean package -DskipTests` 均在 `ShopCoverServiceTest` 的两处范围外过期构造器调用处于 `testCompile` 阶段失败，未执行测试方法（0 项）。


## 2026-07-17 学生资料与宿舍扫码入住：实名资料迁移已准备，待人工执行

- 已只读确认应用与实际数据库均为 `qinghe_life`；`qh_student_profile` 现为 0 行，字符集/排序规则为 `utf8mb4` / `utf8mb4_0900_ai_ci`，当前版本唯一索引与三条外键完整。
- 已新增 `backend/src/main/resources/sql/student_profile_identity_increment.sql`。脚本仅新增无默认值的 `real_name VARCHAR(50) NOT NULL`、`contact_phone VARCHAR(20) NOT NULL`，不修改 `qh_user`、索引、外键、版本字段或其他表；脚本内以注释提供 DataGrip 执行前后复核 SQL。
- 请先在 DataGrip 打开该文件，执行注释中的 `SELECT DATABASE()`、`SHOW CREATE TABLE`、行数检查；仅在仍为 `qinghe_life`、表仍为 0 行且两列不存在时手工执行 `ALTER TABLE`。随后执行字段元数据复核。未执行 SQL、未实现学生资料/扫码入住/我的宿舍、未运行构建或服务控制；字段复核通过后才能重新开始下一阶段的只读代码门禁。

## 2026-07-17 学生资料与宿舍扫码入住：续办阻断

- 续办开始时，用户称已手工执行 `student_profile_identity_increment.sql`，但仓库未找到该脚本，且只读真实库确认 `qh_student_profile` 仍缺 `real_name`、`contact_phone`。本轮已重新生成待人工执行脚本；不得借用 `qh_user.nickname` 或登录手机号替代受版本和权限约束的实名资料。
- 当前版本与有效入住的四条复合唯一索引、以及入住关联外键均已存在；阻断仅为两项资料字段。按门禁规则已停止，未执行 SQL、未改表、未实现或验证接口/页面。
- 续办前由用户在目标库手工确认包含两列的增量实际生效，并恢复该迁移脚本至仓库后，重新从只读门禁开始；随后才可审计 UserContext/普通用户 Token、资产二维码链路并实施学生端功能。

## 2026-07-17 登录、首次资料、个人资料与 OSS 用户头像（完成）

- 已在现有用户模块完成：登录页校园风格优化、首次资料分区布局、个人资料编辑、`GET /api/user/me`、`PUT /api/user/profile` 和当前用户 `POST /api/user/avatar`。用户名/手机号只读；昵称和头像保存后同步 Pinia、顶部导航及当前 Redis Token Hash。
- 部署前必须设置 `OSS_ACCESS_KEY_ID` 与 `OSS_ACCESS_KEY_SECRET`。Bucket 配置为 `java-ai1-kevin`、Endpoint 为 `https://oss-cn-beijing.aliyuncs.com`、Region 为 `cn-beijing`；密钥不得写入配置提交、前端、API 文档、日志或测试。
- 人工验收（确保一次上传）：① 登录并进入“我的”→“编辑资料”；② 选择 jpg/jpeg/png/webp，确认仅出现本地 1:1 WebP 预览且尚无 Network 上传请求；③ 修改昵称后只点击一次“确认保存”，确认 Network 只有一个 `POST /api/user/avatar`（未选头像时为零个）；④ 刷新页面，确认头像/昵称和顶部导航同步；⑤ 更换头像一次，确认新地址生效且旧地址仅在本 Bucket 当前用户路径内才清理；⑥ 分别选择空文件、非图片、伪造图片和大于 2MB 文件，确认提示且不上传。
- 验证已完成：`Q:\backend` 的完整 Maven 回归为 32/0/0/0，打包成功；真实前端路径构建成功（1696 modules）。未执行真实 OSS 上传、未启动/停止服务、未执行 SQL 或 Git；店铺和商品图片未改动。

## 2026-07-17 验证码免注册登录、首次资料完善与可选密码登录（可交付）

- 验证码登录保持 `POST /api/user/login`：新手机号验证码正确时自动建档并签发既有 Redis Token，响应提供 `newUser`、`profileCompleted`、`hasPassword` 和安全用户资料。手机号密码登录为 `POST /api/user/login/password`，成功后走同一 Token 会话链路；失败消息不泄露手机号或密码设置状态。
- 首次资料页为受保护路由 `/profile/complete`，接口为 `POST /api/user/profile/complete`。它复用校园地址校验，在事务内创建默认地址、更新资料并设置 `profileCompleted=true`；密码完全可选，若填写只以 BCrypt 保存。`profileCompleted` 和 `hasPassword` 同步进当前 `qh:login:token:{token}` Hash，前端会话恢复从 `/api/user/me` 读取，不依赖本地推断。
- 人工验收前由用户自行启动已部署的前后端（本轮未进行服务控制）：① 新手机号获取验证码并登录，确认直接进入首次资料页；② 缺必填资料、校区/楼栋或房间号和配送点同时为空时确认页面阻止提交；③ 提交完整资料且不填密码，确认进入“我的”且可继续验证码登录；④ 新手机号重复一次，填写符合规则的可选密码后退出，分别用正确/错误密码验证密码登录和统一失败提示；⑤ 刷新页面或重新进入，确认资料完成状态与用户资料保持同步；⑥ 确认浏览器、网络响应、localStorage 与页面日志均无明文密码、密码哈希或完整 Token。
- 最终回归已在 `Q:\backend` 完成：`mvn clean test` 通过 25 项（0 failure、0 error、0 skipped），`mvn clean package -DskipTests` 成功生成 `qinghe-life-backend-1.0.0.jar`。前端已在真实路径 `C:\Users\28402\Desktop\ruanzhu\workplace\qinghe-life-service\frontend` 执行 `D:/develop/NodeJS/npm.cmd run build`，转换 1695 个模块并生成 `dist`；第三方注释与大 chunk 为非阻断警告。
- 可将本模块标记为“开发交付和命令行验证完成”；不得标为“浏览器人工验收完成”。完成上一条人工清单后，再由用户确认最终用户验收状态。本轮未启动/停止服务、未执行 SQL 或 Git。

## 2026-07-17 免注册登录与首次资料完善：等待人工迁移

- 现有验证码登录已经会为新手机号创建随机昵称用户并签发 Redis Token，但登录响应、Token 会话和用户资料没有 `newUser`、`profileCompleted`、`hasPassword` 状态；密码登录和首次资料完善尚未实现。
- 本轮只读核验确认 `qh_user` 缺少可靠的 `profile_completed` 字段。已生成 `backend/src/main/resources/sql/profile_completion_increment.sql`，仅新增 `profile_completed TINYINT(1) NOT NULL DEFAULT 0`，并带 `IF NOT EXISTS`；脚本未自动执行。
- 请在 DataGrip 审核并手工执行后，只读确认 `qh_user.profile_completed` 为非空、默认值 0。确认后再继续：扩展现有 UserController/UserService/AddressService，复用校园地址校验与 `qh_operate_log`，实现双登录、首次资料完善、可选 BCrypt 密码和 24 项以上完整回归；不得改 Redis Token 体系、创建日志表、实施订单或支付。

## 2026-07-17 账号注册路径统一（本轮完成）

- 注册保持在唯一的 `UserController`，公开接口已统一为 `POST /api/auth/register`；不新增 AuthController。验证码、登录、当前用户、资料和退出仍使用既有 `/api/user/**` 路径，Redis 随机验证码、随机 Token 和双拦截器未改。
- 前端 `/register`、登录页“立即注册”、验证码倒计时、校验和防重复提交均已复核；`frontend/src/api/user.js` 通过唯一 `http.js` 调用 `/auth/register`。注册成功清空敏感输入并跳转登录页，不自动登录。
- 兼容策略：手机不存在创建用户；手机已存在且 `username/password_hash` 都为空时原地绑定；已绑定手机号返回“该手机号已注册”；重复用户名返回“用户名已存在”。密码仅 BCrypt 保存；成功删除 `qh:login:code:{phone}`，不响应密码、哈希或 Token。
- 验证：在 `Q:\backend` 执行完整 Maven 回归通过 24 项（0 failure、0 error、0 skipped），注册集成测试 5 项通过；`mvn clean package -DskipTests` 成功生成 JAR，前端生产构建成功。首次受限沙箱测试的 Redis 连接被策略阻断，受控网络重跑通过；没有启动或停止服务，也没有执行 SQL 或 Git。

## 2026-07-16 全站功能审计（本轮）

- 报告：`docs/full-site-audit.md`。本轮只测试、分析和写报告，未修改业务代码、数据库结构、SQL 或运行服务。
- 实时环境结论：5174、8090、3306、6379 均未监听；因此浏览器、HTTP、MySQL、Redis、验证码、Token、注册、地址、购物车、私有隔离、全局异常和 AOP 运行验证全部阻塞。未启动/停止/重启服务，未创建 `AUDIT_TEST_` 数据。
- 静态结论：用户、首页、分类、商铺、商品、地址、购物车有 Controller；订单、优惠券、探店、后台缺少 Controller/API，相关前端页面是 `PageScaffold` 骨架；没有前端 catch-all 404；管理员路由仅检查普通用户 Token。
- 校园地址：SQL、实体、DTO、VO 和前端统一为省/市/区/详细地址，层间静态一致但不适合校园配送；应另起模块迁移到校区、区域、楼栋、房间、配送点、备注模型。
- 构建：Maven 编译成功；前端生产构建成功（1694 模块），但 JS 1129.74KB、CSS 377.30KB，需独立做拆包优化。
- 下一步仅在用户恢复既有服务后，先重跑本报告的受阻运行时验证；不得直接开始订单、支付或迁移。

## 当前阶段

M2A 验证码获取与登录链路为 `awaiting_manual_verification`；M2B 已完成；M3A 的地址与购物车前端、商铺详情加购已完成，并已完成无浏览器命令行验收。M3 仍为 `in_progress`；AOP 操作日志已完成，订单开发继续暂停。

## 已完成内容

- M2A：验证码、登录、Redis Token、当前用户、资料更新、退出及前端登录状态。
- M2B：分类、首页六区摘要、商铺分页/筛选/详情/商品/评论、商品分页与详情。
- 商铺详情缓存：`qh:shop:detail:{shopId}`、`qh:shop:null:{shopId}`、`qh:lock:shop:{shopId}`；包含 Cache Aside、空值缓存、随机 TTL、有限锁重试及 Redis 异常降级。
- 前端：`category.js`、`home.js`、`shop.js`、`goods.js` 均复用 `http.js`；首页、商铺列表、商铺详情已接入真实接口并处理加载、空数据和错误状态。
- M3A 地址管理：当前用户地址查询、新增、修改、删除和默认地址切换；已实现首地址默认和默认地址删除后的自动补选。
- M3A 结构门禁：用户已人工执行一次 `m3a_increment.sql`；四张目标表实际结构、实体、基线 SQL和设计文档现已一致。
- M3A 地址真实测试：`AddressIntegrationTest` 覆盖认证、默认规则、receiver 映射、用户隔离、越权拒绝、校验、默认补选和上下文清理；测试标识数据已精确清理。
- M3A 购物车后端：六个受保护接口、DTO、VO、服务和参数化原子 SQL 已完成；查询按商铺分组并批量关联当前商品、商铺数据。
- M3A 购物车真实测试：`CartIntegrationTest` 覆盖认证、重复添加、库存/状态、越权、分组、金额统计、选择、清空隔离和精确清理。
- M3A 地址前端：真实地址 API、受保护路由、地址管理入口、列表、表单、默认/删除操作与状态反馈已完成。
- M3A 购物车前端：真实购物车 API、按商铺分组、数量/选择/删除/清空、汇总、结算占位与状态反馈已完成；商铺详情已真实加入购物车。
- M2A Redis 登录重构：匿名 `POST /api/user/code` 接收 JSON `phone`，每次随机生成 6 位验证码并以 String 覆盖写入 Redis database 2 的 `qh:login:code:{phone}`（2 分钟）；登录读取同 Key 并在成功后删除。Token 为随机 UUID 去连字符，以 `qh:login:token:{token}` Redis Hash 保存安全 `UserDTO` 字符串字段，TTL 30 分钟。
- 认证链路已拆分为 `RefreshTokenInterceptor`（order 0：Bearer、Redis、ThreadLocal、续期和清理）与 `LoginInterceptor`（order 1：仅 ThreadLocal 鉴权）；复用现有 `UserContext`，不创建第二套用户容器。
- 404 根因已确认并修复：原 Vite 没有 `/api` 代理，浏览器 `http://localhost:5174/api/user/code` 未到达 8090 而直接 404；现配置原样代理到 `http://localhost:8090/api/user/code`，不 rewrite。登录页错误由 HTTP 层单次展示，按钮仍保持 loading、禁用和倒计时。

## 未完成内容

- M3A 的订单创建与订单查询。
- 支付、订单取消/完成、优惠券领取与使用、探店写操作、后台管理及后续里程碑。

## 本轮测试结果

- `mvn -DskipTests compile`：成功。
- `mvn test`：成功，2 个集成测试，0 失败、0 错误。
- `mvn clean package -DskipTests`：成功，编译 72 个源文件并生成后端 JAR。
- `D:/develop/NodeJS/npm.cmd run build`：成功，生成前端 `dist`。
- M3A 地址批次 `mvn -DskipTests compile`：成功，编译 78 个源文件。
- M3A 结构修订：静态门禁通过；`mvn -DskipTests compile` 成功，重新编译 78 个源文件。
- 四表实际结构复核：通过；仅访问 `qinghe_life`，执行 `SELECT 1`、`SHOW COLUMNS`、`SHOW INDEX` 和 `SELECT COUNT(*)`，未访问 `hmdp`。
- 地址真实数据库测试：通过，1 项测试、0 失败、0 错误；测试结束后 `M3A_ADDR_TEST_` 用户和地址计数均为 0。
- M2A/M2B 回归：`UserAuthenticationIntegrationTest` 2 个测试通过，0 失败、0 错误。
- 地址 receiver 字段纠偏后重新编译：成功，78 个源文件。
- 完整回归：`mvn test` 通过，3 项测试、0 失败、0 错误；`mvn clean package -DskipTests` 成功并生成后端 JAR。
- 购物车批次：`mvn -DskipTests compile` 成功，编译 87 个主源码；`CartIntegrationTest` 1 项测试通过。
- 完整回归：`mvn test` 通过，4 项测试、0 失败、0 错误；`mvn clean package -DskipTests` 成功，生成后端 JAR。
- 购物车测试清理：`M3A_CART_TEST_` 用户、商铺、商品和购物车残留计数均为 0。
- 地址与购物车前端：`D:/develop/NodeJS/npm.cmd run build` 成功；后端 `mvn test` 4 项测试和 `mvn clean package -DskipTests` 已在本轮代码落盘后通过。
- 浏览器自动联调：未完成。浏览器会话异常中断，因此未将其作为业务验收通过；需用户手动启动前后端并在外部浏览器完成地址、加购和购物车操作验证。
- 新对话恢复与前端静态核验：已从仓库持久化文件恢复状态；`frontend/src/api/address.js`、`cart.js`、`AddressView.vue`、`CartView.vue` 各只有一套，均复用实际封装 `frontend/src/api/http.js`。已核验地址路由、购物车导航、“我的”地址入口和商铺详情真实加购；未发现第二个 Axios 实例、硬编码 `http://localhost:8090`、前端 `userId` 提交、`contactName/contactPhone` 混用或订单接口调用。
- 本轮前端构建：`D:/develop/NodeJS/npm.cmd run build` 通过，转换 1681 个模块并生成 `frontend/dist`。
- 本轮后端回归：`mvn test` 通过，4 项测试、0 failure、0 error；`AddressIntegrationTest`、`CartIntegrationTest` 与 `UserAuthenticationIntegrationTest`（2 项）均通过。
- 本轮后端打包：`mvn clean package -DskipTests` 通过，编译 87 个主源码、3 个测试源码，生成 `backend/target/qinghe-life-backend-1.0.0.jar`。
- 本轮进程与 HTTP 只读检查：8090 未监听，5174 正在 `::1:5174` 监听；因 8090 未运行，未执行三条 curl 冒烟请求，记录为人工联调环境的后端未运行，未启动或停止任何进程。
- 验证码修复回归：`mvn test` 通过 5 项、0 failure、0 error；认证测试实际确认验证码 Key 为 STRING、TTL 大于 0、匿名可发送、错误/不存在/过期验证码均拒绝、成功登录后验证码不可复用且 Token 已写入 Redis。`mvn clean package -DskipTests` 与 `D:/develop/NodeJS/npm.cmd run build` 均通过。
- Redis 登录重构验证：`mvn clean package -DskipTests` 成功（88 个主源码、3 个测试源码、JAR 已生成）；前端构建成功。`mvn test` 未通过：当前配置 Redis `192.168.100.128:6379` 连接超时，地址、购物车和认证真实集成测试均被环境阻塞；未修改 Redis 配置、未启动服务、未使用 mock。

## 验证码人工验证

1. 在 VS Code 重启 Vite 开发服务；仅刷新页面不会加载新的代理配置。
2. 在 IDEA 重启 Spring Boot；仅重新编译不会让当前运行实例加载新的认证日志代码。
3. 在登录页输入合法手机号，点击“获取验证码”，确认 Network 请求为 `POST /api/user/code`、仅出现一条提示，按钮 loading 后进入 60 秒倒计时。
4. 在 IDEA 日志查找 `[LOCAL DEV ONLY] phone=138****0001, code=xxxxxx, ttlMinutes=2`；同时应能看到脱敏的验证码请求、Redis 写入、登录成功或失败类别日志。
5. 输入该验证码完成登录；再次使用同一验证码应提示已过期或未发送。
6. 输入错误验证码、非法手机号，确认页面只显示对应错误且按钮恢复可用状态。

## 待用户人工浏览器联调

本轮未使用浏览器自动化；前端构建通过不代表以下业务联调已通过。请在外部浏览器按顺序验证：

1. 登录。
2. 打开地址管理。
3. 新增地址。
4. 编辑地址。
5. 设置默认地址。
6. 删除地址。
7. 进入商铺详情。
8. 选择数量并加入购物车。
9. 打开购物车。
10. 修改数量。
11. 修改选中状态。
12. 删除商品。
13. 清空购物车。
14. 刷新页面并确认数据来自后端。
15. 验证未登录访问时的登录跳转和 `redirect`。

## 下一步任务

等待用户审核后，仅进入 M3A 的订单创建与查询后端批次。不得开始支付、优惠券或 M3B；开始前重新读取本交接文档、计划、进度、发现和当前数据库结构。

## 必须先读取的文件

- `AGENTS.md`
- `docs/PROJECT_SPEC.md`
- `PROJECT_PLAN.md`
- `progress.md`
- `findings.md`
- `task_plan.md`
- `backend/API.md`
- `docs/HANDOFF.md`

## 已知问题

- Vite 构建对第三方 `@vueuse/core` 注释位置发出警告。
- 前端主 JavaScript 压缩包超过 500 kB，属于非阻断分包优化项。
- 本地集成测试依赖项目现有 MySQL `qinghe_life` 与 Redis database 2 可用；本轮测试均已通过。
- 地址表实际使用 `receiver_name/receiver_phone`，Java 已统一为 `receiverName/receiverPhone`。
- 四张复核表已与当前目标实体一致；`m3a_increment.sql` 已由用户人工执行，仍是非幂等脚本，严禁再次执行。
- 本轮仅修改验证码登录链路、开发 profile、认证测试、Vite 代理和 HTTP 错误提示；未实现订单、支付、优惠券或 M3B，未修改地址或购物车业务，也未修改数据库结构。
- 验证码登录当前为 `awaiting_manual_verification`，不应写为 completed，直到用户按本交接文档完成外部浏览器验证。
- 当前真实 Redis 回归也处于阻塞状态；Redis 恢复连通后必须先重跑 `mvn test`，再进行外部浏览器登录验证。
- 8090 本轮未监听，故 `/api/home/summary`、`/api/addresses` 与 `/api/cart` 的现有运行实例 curl 冒烟尚未获得状态码；不得为此重启或替换正在运行的实例。
- 完整逐列矩阵与安全审计见 `findings.md` 的“M3A 七表只读审计”部分。

## 2026-07-11 全局异常处理体系维护完成

- 仅保留 `com.qinghe.life.exception.GlobalExceptionHandler` 一个全局处理器；复用 `Result` 和 `BusinessException`，并为后者增加显式 `userMessage` 字段，避免将通用 `Exception.getMessage()` 直接传给前端。
- 已覆盖业务、DTO/绑定/约束参数、缺少参数、类型不匹配、JSON 不可读、重复键、其他完整性、HTTP 方法、Redis/数据库连接和未知异常。业务与参数类日志不含输入值；系统类日志保留脱敏堆栈，不记录原始 SQL、Token、验证码、密码或完整手机号。
- 重复键只通过基线唯一索引名白名单匹配。`uk_qh_user_phone`、`uk_qh_cart_user_goods`、`uk_qh_order_no` 分别映射为手机号、购物车和订单号提示；未知约束固定为“数据已存在，请勿重复提交”。
- 新增 `GlobalExceptionHandlerTest` 共 8 项，覆盖业务提示、DTO 校验、JSON 格式、已知/未知重复键无泄露、完整性/未知异常安全提示、购物车/订单约束映射和未登录 401。完整 `mvn test` 通过 13 项（0 failure、0 error、0 skipped），现有认证、地址和购物车集成测试均通过，且 Spring 上下文成功启动，未发现异常处理器映射歧义。
- `mvn clean package -DskipTests` 成功，编译 88 个主源码和 4 个测试源码，生成 `backend/target/qinghe-life-backend-1.0.0.jar`。新增测试有一个非阻断的已弃用 API 编译提示。
- 本轮没有启动、停止或重启 IDEA 后端、前端、Redis、MySQL 或浏览器，也没有修改数据库结构或业务功能。若要让已运行的 IDEA 后端加载本轮代码，用户需自行重启该后端；仅查看源码或 JAR 无需重启。

## 2026-07-11 前端 UI/UX 专业化改造完成

- 范围仅为普通用户业务页 UI/UX：`UserLayout`、首页、商铺列表/详情、购物车、地址和“我的”。登录页、注册、订单、支付、后端、数据库、Redis Token、路由路径与现有 API 均未改动。
- 统一视觉落在唯一 `frontend/src/assets/base.css`；新增并复用 `PageHeader`、`AsyncState`、`ShopCard`、`GoodsCard`、`StatusTag`。没有第二套 Axios、Router、Pinia 或主题文件。
- 商铺列表已使用真实服务端分页；导航购物车角标仅在登录后从真实 `GET /api/cart` 的 `totalCount` 获得。首页、详情、购物车和地址持续使用既有真实接口与字段。
- `D:/develop/NodeJS/npm.cmd run build` 最终成功，转换 1692 个模块并生成 `frontend/dist`。首次构建因 `HomeView.vue` scoped CSS 的孤立 `.` 失败，已定位并删除后重建成功。
- 静态核验：`frontend/src` 仅 1 个 Axios 实例；`contactName/contactPhone`、硬编码 localhost、订单/支付 HTTP 请求均为 0。构建仍有 `@vueuse/core` 两条第三方注释位置提示及 1124.92 kB 主包超过 500 kB 的非阻断警告。

## 待用户外部浏览器人工验收

1. 首页：Hero、分类、推荐商铺、热门商品、优惠券、探店的加载、空态、失败重试与真实图片。
2. 商铺：筛选、关键词、排序、page/size 分页、卡片跳转详情。
3. 商铺详情：营业状态、下架/无库存提示、数量选择、登录跳转、加购 loading 与成功提示。
4. 购物车：分组、选择、数量、删除/清空确认、汇总金额、订单建设中提示和小屏布局。
5. 地址与我的：脱敏号码、Dialog 表单校验、默认地址、编辑/删除、入口跳转。
6. 顶部导航：当前态、登录头像昵称、真实购物车角标、窄屏横向导航与页脚。

## 下一步任务

等待用户审核后，下一批仅进行登录页重构与注册登录设计；开始前重新读取本交接、计划、进度、发现、页面设计和当前前端源码。不得与本轮混合执行订单、支付或后端改造。

## 2026-07-11 账号注册迁移准备（等待人工执行）

- 用户表基线缺少账号字段，本轮已准备 `backend/src/main/resources/sql/account_register_increment.sql`，但没有自动执行。脚本仅新增 `username`、`password_hash` 和 `uk_qh_user_username`，不含 DROP、TRUNCATE、清表、数据删除或 Redis 操作。
- 请在 DataGrip 中先确认当前库为 `qinghe_life`、`qh_user` 尚无这两个列和该索引；审核后仅执行一次该脚本。执行完成后请复核：`username VARCHAR(32)`、`password_hash VARCHAR(100)` 和唯一索引 `uk_qh_user_username` 均存在，且既有手机号用户记录、地址、购物车数据未改变。
- 账号字段允许 NULL 是旧手机号验证码登录用户的兼容标记。后续注册服务会复用 `qh:login:code:{phone}`（TTL 2 分钟），验证后对旧用户原地绑定账号，或创建新用户；新账号仅保存 BCrypt 密文，成功后删除验证码，不自动登录。
- 本轮尚未创建 `POST /api/auth/register`、注册 DTO/Service、公开白名单、注册页面、登录页入口或注册集成测试。迁移完成并由用户确认后，必须先进行字段/索引只读核验，再实施这些内容；不得跳到账号密码登录、订单或支付。
- `mvn -DskipTests compile` 已通过（88 个主源码）。因迁移未执行，未运行 Maven 全量测试、真实注册集成测试或前端构建，也不将注册写为已完成。

## 后续任务顺序

1. 用户完成并确认账号字段迁移。
2. 实施并验证账号注册功能。
3. 注册验收后，下一批才是账号密码登录；不得提前实现。

## 2026-07-11 实施前结构复核阻塞

- 虽用户报告已执行 `account_register_increment.sql`，但本轮只读连接 `qinghe_life` 实测 `qh_user` 仍没有 `username`、`password_hash` 和 `uk_qh_user_username`。请在 DataGrip 核对目标实例、数据库和脚本执行结果。
- 在实际数据库结构与实体一致前，注册实现被阻塞；本轮未创建注册接口、页面、测试或登录白名单，也未执行任何 SQL。

## 2026-07-11 账号注册实现完成

- 数据库只读复核通过：`qh_user.username varchar(32)`、`password_hash varchar(100)` 均允许 NULL，`uk_qh_user_username` 为唯一索引；本轮未再次执行迁移 SQL。
- 已在现有 `UserController` 实现匿名 `POST /api/auth/register`，复用 `qh:login:code:{phone}`（TTL 2 分钟）、现有 Redis Token 登录体系和全局异常处理；验证码、登录和个人资料路径保持 `/api/user/**`。新手机号创建用户；旧手机号账号字段均为空时原地绑定；已绑定手机号或重复用户名返回安全提示。
- 密码使用 `BCryptPasswordEncoder` 保存至 `password_hash`；响应、日志、Redis、Pinia 与 localStorage 均不保存密码或密码哈希。注册成功删除验证码，不自动登录、不返回 Token。
- 已新增 `/register`、注册 API、注册表单和登录页注册链接；注册成功清空敏感输入并跳回登录页。未实现账号密码登录、找回密码、订单或支付。
- 验证通过：`mvn -Dtest=UserRegistrationIntegrationTest test` 为 5 项通过；全量 `mvn test` 为 18 项通过（0 failure、0 error）；Maven 打包及前端生产构建均成功。测试数据 `REGISTER_TEST_` 残留计数为 0。

## 待用户外部浏览器验证

1. 打开 `/register`，检查用户名、手机号、验证码、密码和确认密码校验。
2. 获取验证码并确认 60 秒倒计时、loading 和错误提示正常。
3. 使用新手机号注册，确认跳转登录页、手机号回填、未自动登录。
4. 使用历史手机号验证码用户注册，确认原用户资料、地址和购物车未变化且账号绑定成功。
5. 验证重复用户名、已绑定手机号、错误/过期验证码不会创建用户且不会泄露密码或 SQL。

## 下一步任务

等待用户审核后，下一批才可实现账号密码登录；不得提前进入找回密码、订单、支付或其他业务。

## 2026-07-16 AOP 操作日志完成记录

- 数据库门禁：只读复核 `qinghe_life.qh_operate_log` 的 15 个字段、主键及两个复合索引，均与 `OperateLog` 实体一致；未执行 SQL 或修改表结构。
- 实现：新增 `@OperateLog`、`OperateLogAspect`、`OperateLogService` 与独立新事务实现；复用 Spring `ObjectMapper` 和现有 `UserContext.getUserId()`。项目不存在 `UserHolder`，未创建第二套用户上下文。
- 标注范围：地址的新增、修改、删除、设默认；购物车的新增、数量修改、删除、清空。未标注查询、验证码、登录、Token 刷新或购物车选中状态修改。
- 安全：递归脱敏且总长度限制 2000 字符；隐藏密码、验证码、授权/Token、Redis/数据库密码和完整手机号，省略文件二进制、上传文件、Servlet 请求/响应与流对象。异常仅记录类型，原异常不吞掉。
- 测试：全量 `mvn test` 通过 22 项（0 failure、0 error、0 skipped）；测试日志操作标识、地址摘要标识、地址测试用户和购物车测试用户残留计数均为 0。`mvn clean package -DskipTests` 成功并生成 JAR。
- 本轮未修改登录、Redis Token、前端或订单，未启动、停止或重启服务，未执行 Git。若已有 IDEA 后端运行，用户需自行重启才能加载本轮代码。

## 2026-07-16 校园地址模型迁移准备（等待人工审核与执行）

- 本轮只完成审计、设计、基线 SQL、文档和未执行脚本；没有修改地址、购物车、订单、登录、Redis 或前端业务实现，也没有执行任何迁移 SQL。
- 实际 MySQL 8.0.34 只读复核确认：`qh_user_address` 当前为旧省/市/区/详细地址模型，含 2 条历史记录与 `idx_qh_address_user(user_id)`；`qh_campus`、`qh_building` 不存在；`qh_cart`、`qh_order` 均为 0 条。
- 新基线增加 `qh_campus`、`qh_building`；地址表保留旧列且将其改为可空，增加 `campus_id`、`area`、`building_id`、`building_type`、`building_name`、`floor`、`room_no`、`delivery_point`、`detail`、`label`、`remark`、`address_type`。目录 ID 用于关联，地址行同时保留配送可读快照。
- 旧地址迁移规则：旧四段文本复制到新 `detail`，旧字段不覆盖、不删除；没有校区和楼栋 ID 的记录标记为 `HISTORICAL`。后续新校园地址标记 `CAMPUS`。既有用户隔离、首地址默认、默认切换、删除默认后的自动补选与手机号脱敏必须保持。
- 待审核脚本：[campus_address_increment.sql](../backend/src/main/resources/sql/campus_address_increment.sql)。它只含 `CREATE TABLE`、`ALTER TABLE`、`CREATE INDEX` 和一条安全 `UPDATE`；无 DROP、TRUNCATE、清表、INSERT、DELETE 或 Redis 操作。表/列/索引使用 `IF NOT EXISTS`；MySQL 外键语法没有对应的 `IF NOT EXISTS`，因此脚本末尾两个外键 `ALTER` 仅在对应约束不存在时执行。

### 用户在 DataGrip 的执行前检查

1. 确认连接的是 `qinghe_life`，并执行只读 `SELECT VERSION()`；本轮验证版本为 8.0.34。
2. 通过 `SHOW CREATE TABLE qh_user_address` 确认旧列、`idx_qh_address_user` 和当前 2 条历史地址仍存在；确认 `qh_campus`、`qh_building` 不存在。
3. 审核脚本，特别是旧字段改可空、`detail` 回填规则和最后两个命名外键；确认没有同名 `fk_qh_address_campus`、`fk_qh_address_building` 后再执行。
4. 人工执行一次脚本后，复核新表、12 个新地址列、`idx_qh_address_user_default`/校区/楼栋索引、三个命名外键及两条历史地址的 `address_type='HISTORICAL'` 与 `detail` 回填结果。
5. 确认 `qh_user`、`qh_cart`、`qh_order` 的行数和业务数据未改变。不要重跑此前的 `m3a_increment.sql`。

### 获准后的下一阶段边界

- 先进行只读结构复核，再仅实施校园地址后端实体/DTO/VO、校区/楼栋只读查询、地址服务校验与地址前端表单/展示；不做目录后台管理。
- 订单仍不在本轮或下一地址实现批次内。以后订单创建必须校验地址归属当前用户，并写完整、不可变的校园配送快照，不能回读后续被编辑的地址。

## 2026-07-16 校园地址功能实施：目录数据待人工初始化

- 本轮只读复核通过：`qh_campus`、`qh_building`、`qh_user_address` 的预期字段、命名索引和 `fk_qh_building_campus`、`fk_qh_address_campus`、`fk_qh_address_building` 三条外键均存在并与迁移设计一致。未再次执行 `campus_address_increment.sql`。
- 阻塞条件：两个目录表均为 0 条记录（启用数也为 0），无法让用户选择有效校区和楼栋。因此按本任务明确规则，未开始校园地址后端/前端/API/测试，未执行 Maven 或前端构建，也没有改动 Redis、订单、支付、购物车或数据库结构。
- 已生成待审核脚本：[campus_catalog_init.sql](../backend/src/main/resources/sql/campus_catalog_init.sql)。它仅在记录不存在时插入 `QH_MAIN` 与四栋演示楼栋，使用事务包裹，不含 DDL、UPDATE、DELETE、TRUNCATE 或 DROP；本项目没有自动执行它。

### 用户下一步（DataGrip 手工操作）

1. 审核 `campus_catalog_init.sql` 中的校区、楼栋名称、区域、类型和排序是否符合演示需求；如需真实校区名称，请在执行前由用户自行修订脚本文案。
2. 确认连接目标仍是 `qinghe_life` 后，在 DataGrip 人工执行该脚本一次；执行末尾的 SELECT 应显示 1 个启用校区和 4 个启用楼栋。
3. 回复确认执行完成及复核结果。下一轮先做只读目录核验，再继续本任务中的校园地址后端、前端、测试和文档；仍不涉及订单、支付或目录后台管理。

## 2026-07-16 校园地址前后端实施完成

- 只读数据库复核通过：`QH_MAIN/青禾主校区` 和 4 条启用楼栋存在且关联正确；地址校园字段、索引、外键完整，2 条历史地址仍为 `HISTORICAL`。本轮未执行任何 SQL 或修改表结构。
- 后端：新增只读 `GET /api/campuses`、`GET /api/campuses/{campusId}/buildings`，并在原 `AddressController` 增加 `GET /api/addresses/{id}`。创建/更新只接受校园地址字段，校验启用状态、楼栋归属和房间号/配送点；从 `UserContext` 取用户 ID，保存区域/楼栋快照和 `CAMPUS` 类型。
- 历史兼容：历史地址可看、删、设默认；编辑后转换成校园地址，旧省市区/详细地址仍保留。地址 VO 返回格式化地址、目录/楼栋字段和脱敏号码字段；页面仅用脱敏号码展示。
- 前端：现有地址 API 与 `http.js` 增加目录读取，地址页改为校区/楼栋和校园位置表单，校区变化清空楼栋，并提供 loading、空态、错误重试和“历史地址，请更新”提示。没有新增 Axios、Router、Pinia、地址 Controller、日志表或目录后台。
- 操作日志：地址新增、修改、删除、设默认仍使用原有四个 `@OperateLog` 注解，并落在唯一 `qh_operate_log`；新增测试断言地址新增日志成功且不包含完整手机号。
- 验证：前端 `npm.cmd run build` 成功（1694 模块）；`mvn clean package '-Dmaven.test.skip=true'` 成功并生成 JAR。`mvn test` 与用户指定的 `mvn clean package -DskipTests` 被本机 Maven 测试编译的中文工作区路径乱码阻塞，所有测试无法解析主包，未能运行到断言阶段。

### 人工验收步骤

1. 用户自行重启 IDEA 中已经运行的后端，令其加载本轮 JAR/源码；本轮没有启动、停止或重启任何服务。
2. 登录后打开 `/profile/addresses`，确认校区下拉显示“青禾主校区”，切换校区后楼栋重新加载，楼栋显示区域、类型和名称。
3. 新增地址：验证缺少校区、楼栋，或房间号和配送点均为空时失败；成功保存后检查格式化地址、标签、备注和默认地址。
4. 编辑一个历史地址：确认卡片与 Dialog 提示“历史地址，请更新”，保存校园信息后检查变为校园地址且历史省市区数据仍在数据库中保留。
5. 使用两个用户分别管理地址，确认不能读取/修改/删除对方地址；删除默认地址后确认剩余最新地址自动补为默认。
6. 在 `qh_operate_log` 查验地址新增、修改、删除、设默认日志，确认请求摘要不含完整手机号、Token 或密码。
7. 修复或绕过本机 Maven/JDK 对中文工作区路径的测试编译编码问题后，重新执行 `mvn test` 和 `mvn clean package -DskipTests`，再确认全部既有认证、异常、购物车与新增地址测试通过。

## 2026-07-16 Maven 测试编译修复与校园地址验证完成

- 根因已确认：Maven 3.9.11 与 Oracle JDK 21.0.9 在当前 Windows CP936/gb2312 默认编码环境中，无法将中文工作区路径稳定传递给 testCompile 的 `javac` classpath。调试输出包含 `target/classes`，但主包在中文路径下仍全部无法解析；测试源码的包名/导入和 Maven 依赖均正确。
- POM 的实际源文件编码已为 UTF-8（来自父配置），单独设置 `project.build.sourceEncoding`、关闭参数文件或强制另一 `javac` 调用方式均不能解决该主机路径问题，因此未添加无效构建配置。
- 最小代码修复：`AddressIntegrationTest` 的空校园位置负例补齐缺失的 `deliveryPoint` 空字符串参数；没有删除、跳过或禁用任何测试，也没有变更业务代码、数据库结构、Redis 登录或 SQL。
- 已在临时 ASCII 路径 `Q:\backend` 执行：`mvn clean test`，结果 24 tests / 0 failures / 0 errors / 0 skipped；随后执行 `mvn clean package -DskipTests`，结果 `BUILD SUCCESS`，生成 `backend/target/qinghe-life-backend-1.0.0.jar`。
- 校园地址模块可标记为完成：其 3 项集成测试已实际覆盖校区和楼栋查询、楼栋归属/启用校验、地址新增修改删除和默认补选、历史地址转换、用户隔离、手机号脱敏及 AOP 操作日志；认证、异常、购物车和原操作日志测试也已回归通过。测试使用各自唯一标识，并在 `@AfterEach` 精确清理关联数据库数据和对应 Redis 键。

### 路径迁移后的 Maven 命令（待目标目录实际存在后）

```powershell
Set-Location C:\ruanzhu\workplace\qinghe-life-service\backend
mvn clean test
mvn clean package -DskipTests
```

### 用户人工验收

1. 在前端登录两个不同用户，进入“我的地址”，确认校区和楼栋仅显示启用目录项。
2. 新增地址时分别验证：空位置、停用楼栋、跨校区楼栋会被拒绝；填写房间号或配送点之一后可以保存。
3. 新增两条地址，切换默认并删除默认地址，确认剩余最新地址自动成为默认。
4. 编辑一条历史地址，保存为校园地址后确认其旧省/市/区/详细地址仍保留；用另一个用户访问该地址应返回不存在。
5. 在操作日志中检查地址新增、修改、删除、设默认记录，确认摘要没有完整手机号、Token 或密码。

## 2026-07-16 英文路径迁移验证

- 用户指定的目标目录 `C:\ruanzhu\workplace\qinghe-life-service` 当前经 `Test-Path` 确认不存在；本次可访问的工作副本仍为 `C:\Users\28402\Desktop\ruanzhu\workplace\qinghe-life-service`。未创建、复制或移动目录。
- 全项目（排除依赖与构建产物）仅发现两处旧中文绝对路径：本文件原有的 Maven 指令，以及 `docs/history/2026-07-16-pre-context-compression/findings.md` 的历史事实。前者已改为目标英文路径的待执行命令；归档历史未改动。未发现源码、Maven 配置、Vite 配置、启动脚本或运行配置的旧中文绝对路径。
- 因目标英文目录不存在，未在旧路径运行或伪造以下迁移验证：`mvn clean test`、`mvn clean package -DskipTests`、`D:/develop/NodeJS/npm.cmd run build`。本轮没有测试、打包或前端构建结果，也没有业务、数据库结构、SQL 或运行服务变更。
- 校园地址模块的既有 24 项后端回归通过记录不等于本次迁移验收；在目标英文目录存在后，必须先执行本节列出的三条命令，均成功后才可正式验收。

## 2026-07-16 实际英文工作目录构建验证

- 已在 `C:\Users\28402\Desktop\ruanzhu\workplace\qinghe-life-service` 完成验证；未修改业务逻辑、数据库结构、SQL、测试策略或运行服务。
- 后端 `mvn clean test`：失败于 testCompile。104 个主源码文件已编译，7 个测试源文件无法解析 `com.qinghe.life.*` 主包；0 个测试方法执行、0 份 Surefire 报告。详细输出确认现存 `target/classes` 已列在 classpath，替代编译器模式和 fork 外部 `javac` 均复现。未删除、跳过或禁用任何测试，也未作未经验证的 POM 改动。
- 后端 `mvn clean package -DskipTests`：失败于相同 testCompile 问题，未生成 JAR；该参数不跳过测试源码编译。
- 前端 `D:/develop/NodeJS/npm.cmd run build`：成功，转换 1694 个模块并生成 `frontend/dist`。保留第三方 `@vueuse/core` 注释提示和超过 500 kB 的非阻断 chunk 警告。
- 校园地址模块本轮不能正式验收，因为本轮实际执行测试为 0 项。应先恢复 Maven/JDK testCompile 对主编译产物的 classpath 解析，再重跑完整测试和打包；之后下一批功能仅建议订单创建与查询，暂不进入支付、优惠券或其他模块。

## 2026-07-17 Q: 短路径 testCompile 根因验证完成

- 本轮仅解决构建链路。每条 Maven 命令都在同一 PowerShell 进程内将 Q: 临时映射到当前项目根目录、进入 `Q:\backend` 后执行，并在进程结束前解除映射；未修改业务代码、POM、测试、数据库、SQL、Redis 配置或服务。
- 环境为 Maven 3.9.11 与 Oracle JDK 21.0.9。`mvn clean test` 的 testCompile 成功编译 7 个测试源文件，完整回归结果为 **24 tests / 0 failures / 0 errors / 0 skipped**；首次受限沙箱的 Redis 网络权限错误在授权连接现有 Redis 后消失，非项目故障。
- `mvn clean package -DskipTests` 成功：testCompile 正常通过，Surefire 按参数跳过执行，生成 `backend/target/qinghe-life-backend-1.0.0.jar`。
- 根因确定为 Windows 长绝对路径下 Maven/Javac 的 testCompile classpath/参数解析异常：同一源码、POM、Maven 与 JDK 在 Q: 短路径可正常加载 `target/classes`，而长路径会使 7 个测试文件无法解析 `com.qinghe.life.*`。不需要项目内 POM 或代码修复；本机 Maven 验证应使用 Q: 短路径映射。
- 校园地址模块的后端正式验收现已恢复：其 3 项地址集成测试包含在本次 24 项全量回归中，且 JAR 已生成。浏览器端人工交互验收仍遵循既有交接清单。下一功能模块仍仅推荐订单创建与查询。
## 2026-07-17 宿舍入住与资产二维码管理：设计与迁移准备

- 已生成待人工审核脚本：`backend/src/main/resources/sql/dorm_asset_increment.sql`。脚本仅包含 `ALTER TABLE`、`CREATE TABLE`、`CREATE INDEX` 及表内受限外键，未执行、未自动导入、未写入任何目录或业务数据。
- 设计复用现有 `qh_campus`、`qh_building`、`qh_user`、`qh_admin` 与 `qh_operate_log`。新增结构为 `qh_dorm_room`、`qh_dorm_bed`、`qh_student_profile`、`qh_asset_set`、`qh_asset`、`qh_dorm_checkin`；`qh_building.building_code` 为新增可空编码，需由管理员后续配置，脚本不回填。
- 二维码使用随机 `qr_token` 和 ZXing 动态生成；二维码载荷不得包含任何学生个人信息，默认不上传 OSS。学生必须先经过既有登录并明确确认，服务端再从 `UserContext` 绑定入住。
- 下一阶段仅在用户审核并手工执行迁移、完成列/索引/外键只读复核后实施后端实体/服务/接口、管理员身份接入和前端页面。批量清除必须实现为批量退宿、释放资产和记录原因，不得硬删除。
## 2026-07-17 校园消费服务专项审计（本轮完成，未开始修复）

- 审计报告：`docs/campus-consumption-audit.md`。本轮只读检查商铺、商品、购物车、订单、优惠券、校园配送和店铺/商品图片；未修改业务代码、SQL、配置或测试，未执行 SQL、测试、构建、HTTP 调用或服务控制。
- 当前可用链路：商铺/商品用户端查询和购物车 API/页面已经存在；购物车服务端重新校验商品上架、库存、商铺启用和用户归属，`CartIntegrationTest` 已覆盖关键规则并有既有验证记录。订单、优惠券和后台管理不可因实体、契约或页面骨架而视为上线。
- 图像方向：`qh_shop.cover_image` 和 `qh_goods.cover_image` 足够保存一张 OSS URL，单图无需数据库迁移。后续只复用现有 `AliyunOSSOperator`、同一 Bucket、管理员授权和唯一 `qh_operate_log`；推荐 key 为 `qinghe-life-service/shops/{yyyy}/{MM}/{uuid}.webp` 与 `qinghe-life-service/goods/{yyyy}/{MM}/{uuid}.webp`。替换旧图须先落库成功再仅删除同 Bucket/同资源前缀的受控 key；Mock OSS 测试参考头像测试。
- 后续只能逐批实施且每批只处理一个模块：先商铺资料/封面，再商品资料/主图，再商铺后台维护、商品后台维护；随后订单创建（需受审计的结构化配送快照迁移）、订单查询/取消、优惠券使用和校园配送状态（需独立迁移）。不新建模块日志表。
- 数据库限制：本轮未运行任何 SQL，未确认真实 `qh_*` 表列、索引或数据量；报告的数据库结论仅来自仓库 SQL、实体和文档。下一次如用户声明迁移已执行，应先只读核对实际结构再开始对应单模块实现。
## 2026-07-17 宿舍基础档案与资产二维码

- 数据库已只读核验通过；未执行 SQL、未改表、未启动服务、未上传 OSS。
- 管理端接口强制独立管理员 Redis 会话，普通用户 Token 返回 403；宿舍写操作仍写入唯一 `qh_operate_log`。
- 验证记录以 `progress.md` 和 `findings.md` 为准；在本轮结束前需检查其中的 Maven、打包和前端构建最终结果。
## 2026-07-17 管理端认证与权限门禁（代码完成，完整测试受环境阻断）

- 本轮仅完成管理员登录和 `/api/admin/**` 访问控制。复用 `qh_admin`、既有 Redis、唯一 `qh_operate_log` 和唯一 Axios 实例；未实现店铺、商品、订单、支付或图片上传，也未提供管理员前台注册。
- 接口：`POST /api/admin/auth/login`、`GET /api/admin/auth/me`、`POST /api/admin/auth/logout`。管理员登录使用 BCrypt，成功会话为随机 Token 的 Redis Hash `qh:admin:token:{token}`（30 分钟）；`AdminContext` 与 `UserContext` 独立，管理员会话和普通用户会话不互认。过期、退出、随机或普通用户 Token 访问管理接口均返回 HTTP 401。
- 前端：`/admin/login` 已使用独立管理员 Pinia/localStorage 会话；`/admin` 基础布局提供当前管理员、返回用户端和退出。管理路由只检查管理员 Token；管理员接口 401 只清理管理员会话。
- 数据门禁：实际 `qh_admin` 的 BCrypt 字段、状态字段和用户名唯一索引齐全，无 SQL 迁移。当前唯一存量管理员哈希不是有效 BCrypt 格式，未修改其数据；部署前需由授权人员手工配置一个有效 BCrypt 管理员凭据，测试通过临时标识账号隔离。
- 验证：在实际长路径下，`mvn clean test` 与 `mvn clean package -DskipTests` 均于 testCompile classpath 阶段失败，0 测试方法执行；`mvn clean package '-Dmaven.test.skip=true'` 成功生成 JAR；前端 `D:/develop/NodeJS/npm.cmd run build` 成功（1704 modules）。未启动/停止服务、未执行 SQL 或 Git。若需要完整 Maven 回归，需在可用的真实短路径映射/英文目录环境中重新执行；本轮不将它标记为已通过。
## 2026-07-17 学生资料与宿舍扫码入住：结构阻断

- 只读复核确认 `qh_dorm_checkin` 的学生/床位有效记录唯一约束及外键均齐全，入住并发兜底可实现。
- 但 `qh_student_profile` 不含学生实名和联系电话字段；`qh_user.nickname` 与登录手机号不能安全替代这两个有权限和版本要求的资料字段。在禁止修改数据库结构和执行 SQL 的本轮限制下，未实现任何学生资料、扫码或入住接口/页面。
- 如允许后续推进，需要先由用户审核并在 DataGrip 手工执行一份仅新增 `real_name`、`contact_phone` 的增量迁移；执行后再只读核验，随后再实现本模块。

## 2026-07-17 店铺后台维护与封面 OSS（已完成并验证）

## 2026-07-17 商品后台维护与主图 OSS（已完成并验证）

> 最终测试补充：受控网络下的全量回归曾通过 43/0/0/0；补充“非 goods 前缀旧图不删除”断言后，网络重跑因会话额度限制被拒绝，改以不连接 Redis 的 `GoodsImageServiceTest` 定向执行 3/0/0/0。不得将其表述为最终源代码的 44 项全量回归。

- 已完成商品后台资料维护、单主图 OSS 替换、`/admin/goods` 页面和用户端同步，未执行 SQL、未真实调用 OSS、未启动或停止服务、未执行 Git。
- 后端新增 `AdminGoodsServiceImpl`，复用既有 `AdminGoodsController`、`AdminContext`、`AliyunOSSOperator`、`ownGoodsImageKey` 和唯一 `qh_operate_log`。公开商品查询增加停用店铺过滤；购物车接口未重构，库存为 0 仍由既有加购校验拒绝。
- 主图上传遵循上传新对象 → 更新 `qh_goods.cover_image` → 成功后受控旧图尽力删除；数据库更新失败时尽力删除新对象。仅严格匹配当前 Bucket 的 `qinghe-life-service/goods/{yyyy}/{MM}/{uuid}.webp` Key 可删除。
- 已新增 `AdminGoodsIntegrationTest` 与 `GoodsImageServiceTest`，OSS 均为 Mock。短路径 `Q:\backend` 下 `mvn clean test` 为 43 tests / 0 failures / 0 errors / 0 skipped；`mvn clean package -DskipTests` 成功生成 JAR；真实前端路径 `D:/develop/NodeJS/npm.cmd run build` 成功构建 1711 modules。前端仅有第三方 PURE 注释和大 chunk 非阻断警告。
- 人工验收：管理员登录后进入 `/admin/goods`，新增商品并选择主图，确认本地预览不会上传；保存后核验列表、店铺详情和公开商品查询；依次验证下架、库存 0、店铺停用、主图替换和前端售罄/加购禁用。真实 OSS 替换需在已授权环境中检查对象 Key 与旧图清理结果。

- 数据门禁：真实 `qh_shop` 的 12 个字段、3 个索引和可空单图 `cover_image varchar(255)` 已通过只读复核；`qh_category` 可用于启用分类校验。无简介、营业时间或独立营业状态字段，因此本轮没有生成或执行迁移 SQL。
- 后端：新增 `/api/admin/shops` 的分页、详情、新增、编辑、状态和封面接口；全部复用 `AdminContext`、管理员拦截器与唯一 `qh_operate_log`，不接受客户端 `adminId`，不提供物理删除。资料/状态/封面写入只失效 `qh:shop:detail:{id}` 与 `qh:shop:null:{id}`；公开读取继续只展示启用店铺。
- OSS：唯一操作器增加严格 shops 旧对象识别；新图 Key 为 `qinghe-life-service/shops/{yyyy}/{MM}/{uuid}.webp`。上传新图后更新 `cover_image`，数据库失败补偿删除新对象，成功后才尽力删除同 Bucket、同 shops 前缀的旧对象；外部 URL、默认图、头像和其他资源不删除。自动化测试全程 Mock OSS，未真实调用。
- 前端：新增 `/admin/shops` 管理页、菜单和首页入口；复用现有管理员 Store、路由和唯一 Axios。表单支持筛选、分页、新增/编辑、启停、封面本地裁剪预览、一次保存上传、失败保留资料及移动端单列；用户端原有卡片与详情页已通过公开 `coverImage` 同步展示新封面。
- 验证：短路径 `Q:\backend` 下 `mvn clean test` 通过 40/0/0/0，`mvn clean package -DskipTests` 成功生成 JAR，真实前端路径 `D:/develop/NodeJS/npm.cmd run build` 成功（1707 modules）。测试标识店铺、分类、管理员与日志残留均为 0。未启动/停止服务、未执行 SQL、未执行 Git、未真实上传 OSS。
- 人工验收：使用已配置的有效管理员账号登录，进入“店铺管理”；新增/编辑并选择一张图片，确认选择阶段无上传请求、保存阶段仅一个封面请求；刷新用户端商铺列表/详情确认新封面；停用后确认用户端不可正常访问；在已授权的测试 Bucket 中确认替换仅尝试删除 shops 前缀旧对象。

## 2026-07-17 学生资料与宿舍扫码入住：迁移后只读门禁阻断

- 真实库为 `qinghe_life`，且用户手工执行的实名资料增量已生效：`qh_student_profile.real_name` 与 `contact_phone` 均存在。
- 结构仍不符合本轮硬性字段契约：实际为 `college_name`、`major_name`、`student_status`、`current_flag`，缺少 `college`、`major`、`academic_status`、`active_flag`。不得在业务代码中把这些不同字段名默认为等价。
- 当前有效学生资料的两条复合唯一索引、有效入住的用户/床位两条复合唯一索引、以及入住到用户、床位、资产套装的外键均已只读核验正确；阻断仅为学生资料字段命名契约。
- 已按门禁停止：未执行 SQL 或数据库写入，未实现接口、事务、页面或测试，未运行 Maven/前端构建，未启动/停止服务或执行 Git。待用户明确确认采用真实字段名或另行批准结构调整后，再从只读门禁重新开始。

## 2026-07-17 商品店内分类模型与商店主页重构准备（完成，等待人工执行）

- 实时只读结论：`qh_category` 是全局唯一名称的店铺类型目录，现有商品后台“分类”筛选通过 `qh_shop.category_id` 间接过滤商品；筛选本身有效，但不是店内商品分类。`qh_goods` 只有 `shop_id` 索引，缺少商品分类关联。
- 待执行脚本：[goods_category_increment.sql](../backend/src/main/resources/sql/goods_category_increment.sql)。该脚本仅创建 `qh_goods_category`，并向 `qh_goods` 添加可空 `category_id`、索引和外键；不回填历史商品，不修改 `qh_category`，不新增图片表或日志表，且不含 `DROP`、`TRUNCATE`、`DELETE`。
- 执行前：在 DataGrip 选择 `qinghe_life`，确认目标脚本与当前库一致，检查 `qh_goods_category` 与 `qh_goods.category_id` 均尚不存在；确认后一次性执行该文件。不得由应用启动、部署脚本或自动化流程执行。
- 执行后只读复核：

```sql
SHOW COLUMNS FROM qh_goods_category;
SHOW INDEX FROM qh_goods_category;
SHOW COLUMNS FROM qh_goods LIKE 'category_id';
SHOW INDEX FROM qh_goods WHERE Key_name = 'idx_qh_goods_category';
SELECT constraint_name, table_name, referenced_table_name
FROM information_schema.key_column_usage
WHERE table_schema = 'qinghe_life'
  AND table_name IN ('qh_goods_category', 'qh_goods')
  AND referenced_table_name IS NOT NULL
ORDER BY table_name, constraint_name;
```

- 后续唯一允许的实现范围：复用现有商品管理、管理员会话、单一 HTTP/OSS 栈和购物车接口，新增店内分类管理/选择/筛选与商店详情紧凑商品布局。服务端必须验证分类与商品同店；商品改店时清空或重选分类。商品主图改为 1:1 `contain`、浅色内边距和等比方形画布，店铺封面继续为 3:2 `cover`。不进入订单、优惠券、支付或配送。
- 本轮未执行 SQL、未修改业务代码或购物车、未生成图片、未启动/停止服务、未运行测试/构建或 Git。下一阶段必须在人工迁移及复核后重新开始，不得把本轮设计文档当作实现完成。

## 2026-07-17 学生资料与宿舍扫码入住：代码已实现，测试待补跑

- 已实现学生本人资料、二维码解析、确认入住和我的宿舍接口及 `/dorm/scan`、`/dorm/me`。使用真实 `college_name/major_name/student_status/current_flag` 字段映射；有效入住占用床位，资产套装更新为 `OCCUPIED`，不修改床位目录状态。
- 未执行 SQL、服务控制或 Git。后端主代码打包和前端构建通过；`mvn clean test` 在 17 个测试源码的 testCompile 中文路径错误中止，0 项测试执行。后续补跑学生资料、扫码、并发、回滚、脱敏与全量回归后，再做浏览器摄像头验收。
# 店内商品分类与商店主页优化：交接（2026-07-17）

- 已交付：管理员 `/api/admin/goods/categories` 分类管理，管理员商品 `shopCategoryId/shopId/goodsCategoryId/saleStatus` 语义，商品分类归属校验与停用历史规则，公开店内分类导航，紧凑商店详情页和固定购物车入口。
- 人工验收应使用已部署前后端：创建两个店铺并在两店创建同名分类；确认同店重名拒绝、停用后不再出现在用户端导航、历史商品可保留但新商品不可选停用分类；切换商品店铺确认分类清空；确认商品包装完整展示、店铺封面仍裁切填满、移动端分类横滑和购物车底栏正常。
- 构建：完整 Maven 47/0/0/0；JAR 打包成功；前端构建成功（第三方 PURE 注释和大 chunk 为非阻断警告）。未进行浏览器联调或真实 OSS 上传，未启动服务。

# 订单、优惠券、Redis 缓存与限时秒杀：审计与迁移准备（2026-07-17）

- 本轮只做仓库静态审计和设计，未连接 MySQL/Redis、未执行 SQL、未启动服务、未运行测试/构建或 Git。不要把表结构结论视为运行中实例已复核。
- 普通订单与普通券都尚未实现业务层；普通订单将从购物车单店同步创建，普通券在数据库事务内领取和核销。秒杀券必须由独立受理接口、Lua、Redis Stream 消费者和独立活动/受理订单表处理，绝不与购物车下单共用 Controller 方法。
- 已生成、未执行：`backend/src/main/resources/sql/order_core_increment.sql`、`coupon_core_increment.sql`、`seckill_coupon_increment.sql`。先在 DataGrip 检查目标列/索引不存在、评估存量订单/券的人工补齐，再逐个手工执行并只读复核；脚本不可重复执行。
- 详细架构、Key、Lua、Stream Consumer Group、Pending/DLQ、锁边界、缓存治理、测试矩阵和八阶段计划见 `docs/order-coupon-redis-design.md`。下一阶段必须从“阶段一：普通订单创建”的只读数据库门禁开始，不得提前实现秒杀、MQ 或 Redisson。
# 2026-07-18 普通订单创建链路：实现待自动化测试收口

- 已以真实数据库字段复核：`qh_order.total_amount`、`discount_amount`、`delivery_fee`、`pay_amount` 均为 `DECIMAL(10,2)`，`order_no` 唯一键、用户/店铺索引与明细 `order_id` 索引存在；真实 `COUNT(*)` 为订单 0、明细 0。`qh_user_address.area` 映射订单 `address_area`，`qh_goods.sale_status` 是可售状态。
- 已实现 `POST /api/orders`、单店事务、地址/商品快照、MySQL 条件库存扣减、精确购物车删除、结算页及成功页；未实现取消/回补、支付、配送、优惠券、秒杀、Redis Stream 或 Redisson。
- 后端主代码 compile 与前端生产构建通过。全量 Maven 测试在项目内新缓存首次依赖解析阶段超时，尚无实际 tests/failures/errors/skipped 计数；必须补跑后才能完成验收。

## 2026-07-18 普通订单创建自动化收口：范围外编译阻断

- 本轮仅在普通订单创建范围内补齐了 `OrderCreateIntegrationTest` 和三处明确边界修复：购物车店铺漂移拒绝、地址楼栋有效性复核、成功页不再从 URL 读取服务端金额。未改数据库结构、SQL、服务、Git、支付、配送、订单查询/取消/回补或优惠券。
- Maven 3.9.11 / JDK 21.0.9；`Q:` 在会话开始时不存在，必须在每条 Maven 命令中临时 `subst Q:` 到当前仓库根目录后从 `Q:\backend` 执行。`dependency:go-offline` 曾在 64 秒依赖准备阶段超时，缓存保留；这不是测试结论。
- 订单专项已多次实际编译并运行 5 项测试，测试夹具问题已按真实约束修正。最后一次重跑未进入 Surefire：`AdminDormCheckinServiceImpl` 编译时调用缺失的 `AdminDormCheckinVO.setPreviousCheckinId(Long)`。此错误属于宿舍模块，订单范围不得修复。
- 恢复顺序：先由宿舍模块修复或回退该范围外主源码编译错误；随后从 `mvn -Dmaven.repo.local=Q:\.m2 -Dtest=OrderCreateIntegrationTest test` 重跑订单专项，再执行用户指定 `mvn -Dmaven.repo.local=Q:\.m2 clean test`、成功后的 `clean package -DskipTests` 和真实前端路径构建。专项及完整回归均为 0 failures/0 errors、测试残留为 0、两端构建成功前，不得标记订单创建“已完成并验证”。

## 2026-07-18 管理员入住查询、退宿与换寝：测试收口完成

- 已修复管理端响应内部关联泄露和服务边界身份校验：查询、详情和可用床位服务均读取 `AdminContext`；`previous_checkin_id` 只用于持久化历史关联，不返回给页面；办理原因不写入可读日志摘要。
- `AdminDormCheckinIntegrationTest` 实测 7/0/0/0，覆盖门禁、筛选、脱敏、退宿、换寝、并发、回滚、学生端联动和精确残留检查。完整 Maven Surefire XML 汇总 68/0/0/0；`mvn -Dmaven.repo.local=Q:\.m2 clean package -DskipTests` 成功生成 JAR；前端构建成功（1725 modules）。
- 测试与构建均以临时 `subst Q:` 指向当前仓库后运行，并在命令结束清理映射；未执行 SQL、服务控制、Git 或真实 OSS。前端仍有第三方 PURE 注释和大 chunk 非阻断警告。
- 人工验收：管理员登录后在 `/admin/dorm/checkins` 分别核验筛选与详情脱敏；执行退宿后核验资产可用、床位目录不变、学生端空状态；执行换寝后核验新寝室/资产、原入住历史和原床位可重新分配。

## 2026-07-18 管理员宿舍资源管理修复（进行中）

- 当前唯一里程碑：修复 `/admin/dorm` 的宿舍楼过滤、寝室/床位安全维护、资产套装五件资产展示和二维码查看；不触碰学生扫码、入住、退宿、换寝或个人中心。
- 先决条件：必须完成只读数据库门禁，确认 `qh_building.building_type` 的宿舍真实值、六张宿舍表的字段/状态/索引/外键与当前历史基线。字段不可替代或语义不一致时停止，不生成/执行 SQL。
- 完成条件：新增专项测试及学生端入住链路回归均为 0 failures/0 errors，测试前缀数据和精确关联数据残留为 0，Maven 测试/打包和前端构建成功；随后更新 API、设计、页面、计划、进度、发现和本交接。

### 当前验证状态

- 代码与文档已更新，后端 `clean package -DskipTests` 和前端 Vite build 成功；`ADMIN_DORM_RESOURCE_TEST_` 精确残留为 0。
- 不得标记完成：新专项测试在管理员登录时因 Redis `192.168.100.128:6379` 不可连接而返回 503，结果为 3 failures/0 errors；完整回归与学生扫码/入住/退宿/换寝回归尚未运行。
- 恢复后只需在临时 `Q:` 映射下先执行 `mvn -Dmaven.repo.local=Q:\.m2 -Dtest=AdminDormResourceIntegrationTest test`，再执行用户指定的完整 `clean test`；两者 0 failures/0 errors 后再复核残留、打包与前端构建。

## 2026-07-20 管理员宿舍楼按校区管理：实现已落地，验证未收口

- No Data 结论：启用主校区有 2 栋真实“宿舍楼”，分校区为 0；分校区空列表是数据基线，主校区空列表才应排查客户端加载。前端和接口均使用数值 `campusId`，后端固定筛选 `building_type=宿舍楼`。
- 新增不接收客户端 `buildingType`、ID、时间或管理员号；服务端固定宿舍楼类型并校验启用校区、同校区编码/名称唯一。编辑不允许迁移校区或改变类型，已有下级资源/入住历史时编码冻结；当前入住时不能停用；删除无级联且需无全部关联数据。
- 前端已提供校区、状态、关键字筛选，新增/编辑/启停/删除确认、寝室跳转、无校区禁用和新建后自动选中；不要求手输 campusId 或 buildingId。
- 验证：楼栋+入住专项 11/0/0/0；前端构建成功（1731 modules）；后端默认缓存打包成功。完整回归未通过：首次为 76 项、2 failures、1 error，修复测试夹具后为 76 项、2 failures、0 errors；其中入住筛选夹具已由专项验证通过，剩余重复寝室 409 断言需在不违反“不得改寝室业务规则”的前提下另获授权处理。`Q:\.m2` 不可创建，故用户指定的 Maven 缓存命令未能执行。
# 2026-07-20 个人中心姓名显示修复（代码完成，验证部分阻塞）

- 根因：旧 `/api/user/me` 只返回 Redis 会话内的账号 `UserDTO`，未查询当前 `qh_student_profile`；个人中心和导航又只读取 `nickname`，导致实名已建档仍显示账号昵称，且误将可空 `username` 呈现为用户信息。
- 实施：用户服务将只对当前 `UserContext` 身份读取 `qh_user` 与 `current_flag=1` 学生资料，安全映射 `realName/studentNo/hasStudentProfile`，并刷新当前会话；不写回昵称、不改手机号、不改变学生资料写入流程。前端使用共享回退函数，并发加载账号和学生资料，已区分未建档、资料加载失败和实名为空。
- 验证：`UserAvatarServiceTest` 6/0/0；完整 Maven 回归 77/1/0，唯一失败为范围外 `DormAssetIntegrationTest` 的 409/200 断言差异，未修改其逻辑或断言。前端真实路径 Vite 构建成功（1732 modules）。`Q:\backend`、`Q:\.m2` 不存在，且实际工作区的 `mvn clean package -DskipTests` 受控执行申请受平台额度限制拒绝，故后端打包未验证；需在本机具备 Q 路径时运行用户指定两条 Maven 命令。

# 2026-07-23 订单生命周期状态模型与候选迁移（代码/文档完成，验证待本机）

- `OrderStatus` 已统一六个状态及五条合法流转；当前订单创建仍只写既有 `PENDING_PAY`，不引入 `PENDING_PAYMENT`，也未新增支付、取消、管理员或页面接口。
- `order_lifecycle_schema_increment.sql` 是 DataGrip 人工审核候选，拟新增 `pay_time`、`accepted_time`、`delivery_time`、`pay_expire_time` 与 `(status, pay_expire_time)`；不重复 `cancel_reason`、`cancel_time`、`completed_time`，不新增 `goods_amount`。真实库仍需用户手工执行 `SHOW CREATE TABLE qh_order;` 与 `SHOW INDEX FROM qh_order;`。
- 后续取消订单需以 `REQUIRED` 订单事务同时完成条件状态更新、库存恢复和直接写入 `qh_operate_log`；关键方法不能再触发通用 `REQUIRES_NEW` AOP 成功日志。
- 本会话确认 `Q:\backend`、`Q:\.m2` 均不存在，故指定 Maven compile 和 `OrderCreateIntegrationTest` 均未执行；未创建映射路径、未改 Maven/Redis 配置。待本机环境可用后，须重跑两条指定命令并要求订单专项为 5 tests / 0 failures / 0 errors。

# 2026-07-24 订单生命周期核心闭环

- 已只读确认真实 `qh_order` 含 `pay_time`、`accepted_time`、`delivery_time`、`pay_expire_time`、取消/完成字段及 `(status,pay_expire_time)` 索引；候选 SQL 未由应用执行。
- 已完成用户列表/详情、模拟支付、待支付取消及精确库存恢复；管理员列表/详情、接单、开始配送、完成订单；以及每分钟超时取消任务。支付、取消和超时取消的状态竞争均以数据库条件更新收敛。
- 已完成用户与管理员订单页面，复用单一 HTTP 实例和既有身份守卫。订单专项为 13/0/0/0（原创建 5、生命周期 8），后端 package 与前端 build 均成功；测试前缀数据残留为 0。
- 未实现优惠券、Redis Stream、WebSocket、缓存、营业报表、真实支付、骑手或配送轨迹。多实例超时任务锁保留为后续增强，不能替代现有数据库条件更新。

## 2026-07-24 订单超时取消与多实例任务锁（完成并验证）

- 新增 `OrderTimeoutCancelService` 扫描 `PENDING_PAY AND pay_expire_time <= now`，默认每批 100 条；每笔调用独立代理事务。`OrderCancellationService` 是用户取消与超时取消共用的资源释放内核：条件更新成功后精确按 `qh_order_item` 恢复库存、直接写一次 `qh_operate_log`，保留订单/明细且不恢复购物车。
- 支付条件更新也要求 `pay_expire_time >= now`；超时取消条件同时限制 `id`、`PENDING_PAY` 和截止时间。数据库条件更新是支付/取消并发与重复扫描的最终幂等保障。
- `OrderPaymentTimeoutTask` 仅以 Redisson 的 `qh:lock:order:timeout-cancel` 获取有限等待和租约锁并触发扫描。Redisson 复用 `spring.redis`，未获锁、Redis 异常或锁异常均结束本轮，且仅当前线程持锁才解锁。
- 本轮指定订单测试为 20/0/0/0（新增 `OrderTimeoutCancelIntegrationTest` 7 项，含真实 Redis 锁未获得与释放后重试）；`mvn -Dmaven.repo.local=Q:/.m2 -DskipTests package` 成功生成后端 JAR。未执行 SQL、未改 Redis 地址/密码，未进入优惠券、WebSocket、缓存或报表任务。
# 2026-07-24 普通优惠券基础业务：实现完成，验证受环境和候选迁移阻断

## 2026-07-24 秒杀优惠券 Redis Stream：验证与交接完成

- 路由边界：`POST /api/coupons/{couponId}/claim` 保持普通券的 MySQL 同步领取，并拒绝 `coupon_status=SECKILL`；`POST /api/coupons/{couponId}/seckill-claim` 为秒杀券唯一入口。
- Lua 使用 `qh:coupon:seckill:stock:{couponId}`、`qh:coupon:seckill:users:{couponId}`、`qh:coupon:seckill:meta:{couponId}` 和 `qh:stream:coupon:claim` 原子校验活动、时间、重复领取与库存后受理。返回码依次为：0 受理、1 已领、2 库存不足、3 未开始、4 已结束、5 已停用、6 未预热。
- Consumer Group 使用配置化 Stream/Group，底层 `XGROUP CREATE ... 0-0 MKSTREAM` 安全创建空 Stream；仅忽略 `BUSYGROUP`。消费者在 MySQL 事务成功完成用户券幂等检查、`available_stock > 0` 条件扣减和用户券写入后 ACK。`qh_user_coupon(user_id,coupon_id)` 唯一约束是最终一人一券保护。
- Pending 恢复以 `XPENDING`/`XCLAIM` 执行；失败消息保留 Pending，达到配置重试上限后记录精简失败原因并 ACK。Redisson 锁只保护跨实例的恢复调度。
- 已验证：`CouponOrderIntegrationTest` 22 项、`CouponSeckillStreamIntegrationTest` 7 项，合计 29/0/0/0；后端 `mvn "-Dmaven.repo.local=C:/Users/28402/.m2/repository" -DskipTests package` 成功，JAR 已生成。未运行 SQL、前端构建、商品缓存、WebSocket 或营业报表任务。

- 已新增普通券领域模型、管理员/用户端 API、领取条件更新、订单锁定/支付核销/取消释放、用户与管理员页面，以及候选人工迁移 `backend/src/main/resources/sql/coupon_foundation_increment.sql`。不含任何秒杀、Lua、Redis Stream、WebSocket、商品缓存或报表代码。
- 真实 `qh_coupon/qh_user_coupon` 仍是旧结构，缺少本轮字段；候选 SQL 未执行。手工审核并执行后，先重跑本轮指定四类 Maven 专项测试，再进行跳过测试打包和前端构建。
- 本轮指定 Maven 命令已仅执行一次，因 `Q:\.m2` Access is denied 在 Maven 启动阶段失败，未到编译/Surefire；后端打包按“专项通过后”规则未运行。前端 `npm run build` 已执行一次，受 esbuild 读取工作区上级目录限制而无法加载 `vite.config.js`，未生成构建结论。
- 后续收口结果见下节；本段“迁移/环境阻断”是此前快照，不能作为当前状态。

## 2026-07-24 普通优惠券基础模块：验证与收口完成

- 用户已人工完成优惠券真实表结构与索引；`coupon_foundation_increment.sql` 保留为参考，未由 Codex 或应用自动执行。
- `mvn "-Dtest=OrderCreateIntegrationTest,OrderLifecycleIntegrationTest,OrderTimeoutCancelIntegrationTest,CouponOrderIntegrationTest" test` 在 `Q:\backend` 使用默认 Maven 本地仓库通过：40 tests、0 failures、0 errors、0 skipped（优惠券专项 20 项）。
- `mvn -DskipTests package` 成功并生成 `Q:\backend\target\qinghe-life-backend-1.0.0.jar`；真实 `frontend` 路径 Vite 构建在修复 `AdminCouponView.vue` 缺失的表格列闭合标签后成功（1741 modules）。
- `COUPON_ORDER_TEST_` 的优惠券、用户券、订单、明细、购物车、地址、商品、店铺、操作日志和用户均经只读计数确认残留为 0。未开始 Lua、Redis Stream、秒杀券、Redis 商品缓存、WebSocket 或营业报表。
