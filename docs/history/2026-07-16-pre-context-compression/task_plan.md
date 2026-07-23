# 青禾校园生活服务系统 V1.0：M2B 恢复计划

## 2026-07-16 全站功能审计与运行测试

| 阶段 | 状态 | 内容 |
|---|---|---|
| 65. 文档恢复与静态清单 | in_progress | 已完成规则/基线文档读取和首轮控制器、路由、公开端点盘点；继续以实际路由、页面、API、Controller、Service、SQL 与数据库结构建立审计矩阵。 |
| 66. API、数据库与安全审计 | pending | 仅做本机运行实例的受控冒烟、只读元数据与安全/隔离检查；记录环境阻塞。 |
| 67. 浏览器页面与核心流程 | pending | 不启动进程，逐路由验证可访问性、真实接口、状态与关键用户流程；仅创建并精确清理 `AUDIT_TEST_` 数据。 |
| 68. 审计报告与交接 | pending | 创建 `docs/full-site-audit.md`，更新进度、发现、交接后停止。 |

### 本轮边界

- 只测试、分析和写报告；不得修改业务代码、前端、数据库结构、迁移 SQL 或运行进程。
- 校园地址模型必须单列审计：当前省市区模型若无法精确表达校内收货位置，至少登记为 P1；仅提出独立改造批次，不实施。

### 2026-07-16 临时优先事项：Codex 卡顿诊断

- **状态：** in_progress。
- **目标：** 只读定位当前工作区与会话中可验证的卡顿因素，并采取不改业务代码、不重启服务的低风险缓解措施。
- **验证：** 检查仓库规模、规划记录体积、前后端进程资源和本机页面响应；外置 Edge 不存在可控自动化连接时，如实标记，不能将内置浏览器结果写为 Edge 验收。

## 2026-07-16 AOP 操作日志续作

| 阶段 | 状态 | 内容 |
|---|---|---|
| 61. 恢复与实际结构门禁 | completed | 只读确认 `qinghe_life.qh_operate_log` 的 15 个字段、NULL 规则及主键/两个复合索引均与实体一致；`UserHolder` 不存在，唯一上下文为 `UserContext`。 |
| 62. 注解、切面与日志服务 | completed | 已新增注解、AOP 切面与 `REQUIRES_NEW` 日志服务；切面复用 Spring `ObjectMapper`、`UserContext`，异常原样传播且日志失败隔离。 |
| 63. Controller 标注与安全测试 | completed | 已标注地址四个与购物车四个关键写接口；新增测试覆盖成功/失败、上下文用户、递归脱敏、截断、保存失败与上下文无污染。 |
| 64. 回归、打包与交接 | completed | `mvn test` 22 项通过，`mvn clean package -DskipTests` 成功；残留日志与既有地址/购物车测试数据精确核验为 0，文档已更新。 |

### 本轮边界

- 用户确认已在 DataGrip 手工执行 `operate_log_increment.sql`；本轮只读复核，绝不再次执行或修改数据库结构。
- 项目没有 `UserHolder` 类，后续均复用现有 `UserContext.getUserId()`；不新增并行用户容器，不改登录或 Redis Token。
- 不记录敏感值、文件对象或 Servlet 请求/响应对象；日志保存失败只能写服务端 error，不得改变原业务返回或异常路径。

### 本轮错误记录

| 错误 | 次数 | 处理 |
|---|---:|---|
| `OperateLogIntegrationTest` 同时导入同名注解与实体，导致 testCompile 类型歧义 | 1 | 未执行测试；移除注解导入并改用注解全限定名，保留实体类型导入。 |
| 从 `backend` 工作目录读取根目录 `PROJECT_PLAN.md` 时路径错误 | 1 | 未修改文件；切换至项目根目录后读取并完成计划状态更新。 |

## 目标

依据当前工作区的实际文件恢复状态，按批次完成 M2B（分类、首页、商铺、商品、缓存、前端 API 与页面），在 M2B 全部验证后停止，不进入 M3A。

## 阶段

| 阶段 | 状态 | 内容 |
|---|---|---|
| 1. 恢复与写入探针 | completed | 已核验项目结构、既有记录与 M2B 文件缺失情况；写入探针已创建、读取、校验范围并删除。 |
| 2. 分类模块 | completed | 已实现 `GET /api/categories` 及 Controller、Service、ServiceImpl、CategoryVO，并将接口加入公共访问白名单。 |
| 3. 第一批验证与记录 | completed | 已检查新增文件存在且非空，Maven 编译成功，记录已更新。 |
| 4. 首页模块 | completed | 已实现 `GET /api/home/summary`，固定返回六个数据区域，并完成 Maven 编译。 |
| 5. 商铺、商品与缓存 | completed | 已核验商铺与商品接口、三类 Redis Key、Cache Aside、空值缓存、随机 TTL、有限互斥锁重试及 Redis 异常降级源码均已落盘；最新 Maven 编译成功。 |
| 6. 前端 API 与页面 | completed | 四个 API 文件及首页、商铺列表、商铺详情真实接口接入均已核验存在且可读取。 |
| 7. 总体验证与记录 | completed | M2A/M2B 两个集成测试、Maven 干净打包和前端生产构建均成功；API、计划、进度、发现与 HANDOFF 已更新。 |

## 约束与决策

- 仅操作当前 `qinghe-life-service` 工作区；不执行 Git、SQL 导入或 Redis 清空操作。
- 分类接口只返回 `status=1` 的分类，并按 `sort_order` 升序。
- 本轮只实施 M2B，不进入 M3A。
- 2026-07-10 续作以当前文件系统为唯一事实来源：已完成模块只验证，不无意义覆盖；先完成源码与构建审计，再更新 M2B 状态。

## 错误记录

| 错误 | 处理 |
|---|---|
| 首次读取技能文件路径不存在 | 已改用工作区上级 `.codex` 中列出的实际技能路径读取。 |
| 首页模块首次验证路径错误 | 命令在 `backend` 工作目录下仍使用 `backend/` 前缀，文件检查失败且 Maven 未执行；后续改用 `src/` 相对路径。 |
| 续作文件审计 PowerShell 管道解析失败 | `foreach` 语句直接接管道产生空管道元素错误，未读取或修改源码；改为先收集对象到变量再格式化。 |
| M3A 增量文件核验重复管道解析错误 | 再次误用 `foreach` 后直接接管道，命令在解析阶段失败且未执行 SQL；此后统一先写入 `$rows` 再输出，禁止第三次使用失败写法。 |
| M3A 文档收口大补丁上下文不匹配 | `docs/api-contract.md` 地址行与预期文字不同，补丁整体未应用；改为读取精确行并拆分小补丁。 |

## M3A 第一批：地址管理后端

| 阶段 | 状态 | 内容 |
|---|---|---|
| 8. M3A 恢复与结构审计 | completed | 已确认 M2A/M2B 完成、M3A 未开始，并审计地址、购物车、订单、明细、商品、商铺、用户的 SQL/实体/Mapper。 |
| 9. 安全增量设计 | completed | 已更新数据库设计与基线 SQL，生成未自动执行的 `m3a_increment.sql`，并同步目标实体字段。 |
| 10. 地址管理后端 | completed | 已实现当前用户地址 CRUD、默认地址事务规则、DTO 校验和 VO。 |
| 11. 第一批验证与记录 | completed | 7 个新增文件核验通过，Maven 编译成功；因增量 SQL 未执行，真实地址数据库测试保持阻塞。 |

### M3A 边界

- 本批只实施地址管理后端及 M3A 必需的结构审计/增量 SQL，不实现购物车和订单业务代码。
- 不实现支付、取消、完成、超时关闭、库存恢复、重复提交 Token、优惠券、Lua、探店写操作或后台管理。
- 增量 SQL 仅生成，不自动执行；执行前必须由用户在 DataGrip 审核。

## M3A 续作：地址实测与购物车

| 阶段 | 状态 | 内容 |
|---|---|---|
| 12. 地址字段纠偏 | completed | 已按实际数据库统一为 `receiver_name/receiver_phone` 与 Java `receiverName/receiverPhone`，移除地址领域 contact 混用。 |
| 13. 四表只读结构核验 | completed | 地址表一致；购物车、订单、订单明细仍为旧结构，与实体不一致，触发停止门禁。 |
| 14. 地址真实集成测试 | pending | 覆盖鉴权、映射、归属、默认地址和参数校验；只清理本次测试数据。 |
| 15. 购物车后端与测试 | pending | 仅在地址和四表审计通过后实施购物车，不开始订单。 |
| 16. 地址/购物车前端与总验证 | pending | 完成 API、页面、Maven 测试/打包和前端构建，更新记录后停止。 |

## M3A 只读数据库结构门禁审计

| 阶段 | 状态 | 内容 |
|---|---|---|
| 17. 文件与映射恢复 | completed | 已读取七表实体、Mapper、设计、基线 SQL、增量 SQL和配置；确认无自定义 Mapper SQL。 |
| 18. 七表只读元数据采集 | completed | 已仅对 `qinghe_life` 执行 SHOW COLUMNS/INDEX/CREATE TABLE 与 COUNT，七表结果完整。 |
| 19. 差异矩阵与增量 SQL 安全审计 | completed | 已完成数据库、Java、文档和两份 SQL 对照；增量 SQL 当前不宜直接执行。 |
| 20. 记录与停止 | completed | 已更新 HANDOFF、progress、findings，M3A 保持 in_progress。 |

### 本轮数据库事实

- 用户提供的 DataGrip 截图显示 `qh_user_address` 实际列为 `receiver_name`、`receiver_phone`，不是 contact 命名。
- 不再对 `qh_user_address` 执行 ALTER；代码、DTO、VO、测试与文档统一使用 receiver 命名。
- 只读查询确认 `qh_cart` 缺少 `shop_id/selected`，`qh_order` 与 `qh_order_item` 仍使用旧快照/状态列；按用户门禁停止，禁止开始购物车。

## M3A 结构门禁修订

| 阶段 | 状态 | 内容 |
|---|---|---|
| 21. 增量 SQL 与映射修订 | completed | 已修订 `m3a_increment.sql` 的列命名、非空约束、遗留列及索引；仅生成脚本，未执行 SQL。 |
| 22. 静态复核与记录 | completed | SQL、实体和文档静态门禁通过，Maven 编译成功；项目记录已同步，M3A 停在人工执行与重新审计门禁。 |

### 本轮约束

- 仅处理 M3A 的数据库结构门禁修订；不得执行 SQL，不进入购物车、订单或优惠券业务实现。
- `qh_user_address` 已与真实数据库一致，增量脚本不得修改该表。

### 本轮错误记录

| 错误 | 次数 | 处理 |
|---|---:|---|
| 静态门禁将注释中的 `qh_user_address` 误判为 DDL 操作 | 1 | 后续检查先移除 SQL 行注释，只审计实际语句；未执行 SQL 或 Maven 编译。 |
| 静态门禁误要求设计文档出现物理索引名 | 1 | 文档登记索引字段组合即可；下一次拆分 SQL、实体与文档的断言，不再混用命名要求。 |

### 本轮验证结果

- 静态门禁通过：已确认购物车新增必填 `shop_id`、选中状态、两个查询索引并移除遗留 `price_snapshot`；订单使用必填 `address_id` 与 receiver 快照；订单明细字段重命名与实体一致。
- `mvn -DskipTests compile` 通过，重新编译 78 个源文件。
- 未执行 `m3a_increment.sql`、未执行数据库写操作、未开始购物车或订单业务实现。

## M3A 人工迁移后复核与地址真实测试

| 阶段 | 状态 | 内容 |
|---|---|---|
| 23. 文件恢复与四表只读复核 | completed | `qinghe_life` 四表实际列、类型、NULL、默认值和索引均通过；四张表均为 0 条记录。 |
| 24. 地址集成测试 | completed | 新增 `AddressIntegrationTest` 并通过；测试地址/用户使用 M3A_ADDR_TEST_ 标识、精确清理。 |
| 25. 回归、打包与交接 | completed | Maven 3 项测试和打包均通过，交接文档已更新；M3A 保持 in_progress 后停止。 |

### 本轮限制

- 用户已确认在 DataGrip 人工执行过一次 `m3a_increment.sql`；本轮绝不再次执行或修改该脚本、数据库结构、购物车或订单业务代码。
- MySQL 仅可连接 `qinghe_life`；结构检查只使用用户列出的元数据/计数/连通性查询。

### 四表只读复核结论

- 已确认当前库为 `qinghe_life`，`SELECT 1` 成功；未访问 `hmdp`。
- `qh_user_address` 的 receiver 列和用户索引正确；`qh_cart` 无 `price_snapshot`，`shop_id` 非空、`selected` 默认 1，并具有用户商品唯一索引和用户商铺索引。
- `qh_order` 无旧地址/状态列，具有 receiver 快照、订单号唯一索引和 DECIMAL 金额列；`qh_order_item` 无旧快照列，具有图片、价格、小计字段和订单索引。

### 本轮最终验证

- `mvn -Dtest=AddressIntegrationTest test`：通过，1 项测试、0 失败、0 错误。
- `mvn test`：通过，3 项测试、0 失败、0 错误；包括地址测试与既有认证、首页、商铺、商品、缓存回归。
- `mvn clean package -DskipTests`：通过，重新编译 78 个主源码和 2 个测试源码，生成 JAR。
- 只读计数确认 `M3A_ADDR_TEST_` 剩余用户 0、剩余地址 0；本轮未再次执行增量 SQL、未实现购物车或订单。

### 本轮错误记录

| 错误 | 次数 | 处理 |
|---|---:|---|
| 最终禁止标识检查把历史文档中的检查命令误判为源码标识 | 1 | 后续仅检查后端 Java 与前端源代码，不将计划、进度和发现记录纳入源码标识扫描。 |

## M3A 购物车后端批次

| 阶段 | 状态 | 内容 |
|---|---|---|
| 26. 恢复与结构门禁 | completed | `qh_cart`、`qh_goods` 与 `qh_shop` 实际字段、类型、约束和索引均符合本批稳定设计。 |
| 27. 购物车后端实现 | completed | 已实现六个受保护接口、DTO、VO、服务和参数化原子 SQL；单独购物车真实测试通过。 |
| 28. 真实测试、回归与交接 | completed | 购物车集成测试、Maven 全量测试与打包均通过，文档已更新；M3A 保持 in_progress 后停止。 |

### 本轮边界

- 仅实施购物车后端；不修改数据库结构，不执行 `m3a_increment.sql`，不访问 `hmdp`，不实现购物车前端、订单、支付、优惠券或 M3B。
- 所有购物车业务仅使用 `UserContext` 的当前用户编号，测试数据必须采用 `M3A_CART_TEST_` 标识并精确清理。

### 结构门禁结果

- `qh_cart` 使用必填 `user_id/shop_id/goods_id/quantity/selected`，`selected` 默认 1；`uk_qh_cart_user_goods(user_id, goods_id)`、`idx_qh_cart_user_shop(user_id, shop_id)` 均存在，且无 `price_snapshot`。
- `qh_goods` 使用稳定列 `cover_image`、`sale_status`，价格为 `DECIMAL(10,2)`；`qh_shop` 使用 `name` 与 `status`，不采用旧字段假设。

### 本轮最终验证

- `mvn -DskipTests compile`：通过，编译 87 个主源码。
- `mvn -Dtest=CartIntegrationTest test`：通过，1 项测试、0 失败、0 错误。
- `mvn test`：通过，4 项测试、0 失败、0 错误；既有认证、首页/商铺/商品/缓存和地址测试均继续通过。
- `mvn clean package -DskipTests`：通过，编译 87 个主源码、3 个测试源码并生成 JAR。
- 只读残留检查：`M3A_CART_TEST_` 用户、商铺、商品和购物车均为 0；未实现前端、订单、支付、优惠券或 M3B。

### 本轮错误记录

| 错误 | 次数 | 处理 |
|---|---:|---|
| 文档汇总补丁的阶段行上下文不匹配 | 1 | 未应用任何内容；已读取精确行并改为小补丁。 |

## M3A 地址与购物车前端批次

| 阶段 | 状态 | 内容 |
|---|---|---|
| 29. 恢复与前端基线 | completed | 实际 HTTP 封装为 `frontend/src/api/http.js`；购物车、我的页面为骨架，商铺详情已有真实商品列表但未接入加购。 |
| 30. 地址、购物车与详情加购 | completed | 已完成真实 API、地址页、购物车页、我的入口、受保护路由和商铺详情加购；未实现订单。 |
| 31. 构建、回归与交接 | completed | 前端构建、后端回归与打包均通过，静态核验与交接文档已完成；浏览器联调明确交由用户人工验证。 |

### 本轮边界

- 仅实现地址前端、购物车前端和商铺详情加入购物车；不修改数据库结构、增量 SQL 或购物车后端，不实现订单、支付、优惠券或 M3B。
- 首次恢复发现任务列出的 `frontend/src/utils/http.js` 不存在；必须以仓库实际 HTTP 封装位置为准并记录差异。

### 前端基线结论

- 复用 `frontend/src/api/http.js` 的现有 Bearer 请求与 401 跳转逻辑；不创建新的 Axios、Token 或登录体系。
- 路由已有受保护的 `/cart` 与 `/profile`，但缺少 `/profile/addresses`；导航已有购物车入口，需保留并接入真实页面。

### 前端实现结果

- 已新增 `address.js`、`cart.js` 和地址页面；`/profile/addresses` 受登录守卫保护，“我的”页面提供入口。地址列表展示脱敏号码，编辑提交保存原始号码。
- 已将购物车页接入真实服务端数据、分组、数量/选择、删除、清空、汇总和结算占位提示；每次操作后均重新读取服务端状态。
- 商铺详情商品列表已增加数量、库存/下架禁用、登录重定向、提交 loading 与真实 `addCart` 调用。
- `npm.cmd run build` 通过；保留第三方注释与大包警告。

### 本轮最终验证与联调结论

- 前端生产构建通过；后端 `mvn test`（4 项、0 失败）和 `mvn clean package -DskipTests` 已在前端代码落盘后通过。
- 九个前端基线文件均实际存在、非空；静态检查确认 receiver 字段、购物车 API、受保护地址路由和详情加购均存在，且未发现 contact 命名或订单 API 请求。
- 浏览器自动联调未完成：浏览器会话异常中断。本轮不再重试浏览器；需用户手动启动前后端并在外部浏览器验证地址、商铺加购与购物车操作。本结论不代替业务代码验收。

### 本轮错误记录

| 错误 | 次数 | 处理 |
|---|---:|---|
| 首次九文件核验命令触发 PowerShell 空管道解析错误 | 1 | 命令未执行；改为先收集结果后格式化，静态核验已通过。 |

## M3A 前端最终命令行验收

| 阶段 | 状态 | 内容 |
|---|---|---|
| 32. 前端、后端与 curl 验证 | completed | 静态核验、前端构建、4 项 Maven 回归和 JAR 打包均通过；8090 未监听，故未执行 curl，5174 正在监听；未操作运行进程。 |
| 33. 人工联调清单与交接 | completed | 已更新交接与设计文档，保留 15 项外部浏览器人工联调清单后停止。 |

## M2A 验证码获取与登录链路修复

| 阶段 | 状态 | 内容 |
|---|---|---|
| 34. 恢复与接口核验 | completed | 已确认真实接口为匿名 `POST /api/user/code` 和 `POST /api/user/login`，两者均接收 JSON `{phone}` / `{phone,code}`；前端和 Controller 路径一致。 |
| 35. 验证码链路修复 | awaiting_manual_verification | 代码级验证码链路已修复，但用户已报告浏览器 404；在外部浏览器真实验证前不得标记为 completed。 |
| 36. Redis/接口实测与构建 | awaiting_manual_verification | 自动测试与构建历史通过，但不替代用户浏览器验收；当前须先修复 Vite 代理并由用户重新验证。 |
| 37. 404 调用链核验 | completed | 已确认前端请求与 Controller 路径一致；根因是 Vite 未配置 `/api` 代理，请求停留在 5174 并 404。 |
| 38. 代理与错误层修复 | completed | 已配置 Vite `/api` 原样代理、认证脱敏/LOCAL DEV ONLY 日志和 HTTP 单次错误提示；未触及其他业务。 |
| 39. 构建与人工验收交接 | awaiting_manual_verification | Maven 测试、打包和前端构建通过；等待用户重启 IDEA/Vite 并在外部浏览器验证。 |
| 40. Redis 登录结构审计 | completed | 已确认现有单拦截器混合刷新与鉴权、dev 固定验证码和发送锁阻止覆盖；复用现有 UserContext 作为 ThreadLocal 用户容器。 |
| 41. 随机验证码与双拦截器重构 | completed | 已完成随机验证码、覆盖 TTL、集中常量、双拦截器、动态认证测试和顶部登录态；未修改数据库或 M3A 业务。 |
| 42. 回归与人工验证交接 | awaiting_manual_verification | 打包/前端构建通过；真实 Redis 测试被连接超时阻塞，等待 Redis 可用后重跑并由用户浏览器验证。 |

### 本轮错误记录

| 错误 | 次数 | 处理 |
|---|---:|---|
| `mvn test` 连接 Redis `192.168.100.128:6379` 超时，地址、购物车与认证真实集成测试全部受阻 | 1 | 不修改 Redis 配置、不启动服务、不改用 mock；记录完整失败，继续仅执行跳过测试的打包与前端构建检查编译。 |

### 本轮边界

- 不使用浏览器、不启动或停止进程、不执行 Git 或数据库结构变更、不执行 Redis FLUSHALL/FLUSHDB。
- 复用 `frontend/src/api/http.js`，不新建认证接口或 Axios 实例；验证码发送和登录必须继续使用同一个 `RedisKeys.code(phone)` Key。

### 本轮错误记录

| 错误 | 次数 | 处理 |
|---|---:|---|
| 认证集成测试重排时遗漏静态导入 `assertThrows`，`mvn test` 在 testCompile 阶段失败 | 1 | 后端主源码和数据库均未执行；补回单个 JUnit 静态导入后重新运行测试。 |
| 只读端口检查在无监听端口时返回 PowerShell 非零退出码 | 1 | 8090、5174 均确认无监听；未执行 curl、未操作进程，不视为构建或测试失败。 |

### 本轮限制

- 用户确认 PID 36220 的 JAR 正在 8090 运行、Vite 正在 5174 运行；本轮不启动、停止或重启任何进程，也不使用浏览器工具。

### 2026-07-10 新对话恢复记录

- 已按本地持久化文件恢复 M3A 状态：地址与购物车前端仅作静态核验和命令行验收，M3A 仍为 `in_progress`。
- 本轮执行顺序固定为：前端实现核验、前端生产构建、后端回归测试、后端打包、8090/5174 只读端口检查、仅在 8090 监听时执行三条只读 HTTP 冒烟，随后更新交接文件。
- 禁止启动或停止进程、使用浏览器、执行迁移 SQL、修改数据库结构，或开始订单/M3B。

### 本轮错误记录

| 错误 | 次数 | 处理 |
|---|---:|---|
| 前端静态扫描命令中的 PowerShell 正则引号导致解析失败 | 1 | 命令未执行、未改动源码；后续改为逐项使用单引号正则，避免在哈希表中混合引号。 |
| 交接文档批量补丁的进度文件上下文不匹配 | 1 | 补丁未应用；先读取当前末尾的精确行，再按实际内容完成进度与发现更新。 |

## 2026-07-11 全局异常处理体系维护批次

| 阶段 | 状态 | 内容 |
|---|---|---|
| 43. 现状与约束审计 | completed | 已确认唯一全局异常处理入口、`Result`/`BusinessException` 兼容边界、DTO 校验类型、基线 SQL 的九个唯一约束和既有集成测试范围。 |
| 44. 异常处理与测试实现 | completed | 已完成业务、参数、重复键、完整性、HTTP 方法、基础设施和未知异常处理；新增 8 项隔离测试，未改变正常业务逻辑。 |
| 45. 回归、文档与交接 | completed | 定向测试 8 项、全量 Maven 测试 13 项和跳过测试打包均通过；API、契约、发现、交接与进度已更新。 |

### 本轮边界

- 仅完善后端全局异常处理体系及相关测试、文档；复用现有 `Result`、`BusinessException` 和包结构。
- 不修改登录、Redis、地址、购物车、订单等正常业务逻辑，不改数据库结构，不运行 SQL，不启动或停止任何进程，不执行 Git 命令。
- 必须保持未登录访问 HTTP 401 行为；所有异常响应保持现有 `Result` 字段兼容，并避免向前端暴露 SQL、表名或异常原文。

### 最终验证

- `mvn -Dtest=GlobalExceptionHandlerTest test`：通过，8 项测试，0 failure、0 error。
- `mvn test`：通过，13 项测试，0 failure、0 error、0 skipped；认证、地址、购物车回归和 Spring 异常处理器映射均通过。
- `mvn clean package -DskipTests`：通过，编译 88 个主源码和 4 个测试源码，生成 `backend/target/qinghe-life-backend-1.0.0.jar`。
- 未启动、停止或重启任何进程，未运行 SQL、未修改数据库结构；若已有 IDEA 后端实例运行，需由用户自行重启以加载本轮代码。

## 2026-07-11 前端 UI/UX 专业化改造批次

| 阶段 | 状态 | 内容 |
|---|---|---|
| 46. 前端现状与接口审计 | completed | 已确认单一全局 CSS、Axios、Pinia 与路由；商铺接口支持服务端 `page`、`size`、`total`，首页、详情、地址和购物车均有真实数据源。 |
| 47. 统一视觉与公共组件 | completed | 已在唯一 `base.css` 建立变量和通用布局，并新增标题、异步状态、商铺卡、商品卡、状态标签组件。 |
| 48. 业务页面与导航优化 | completed | 已优化首页、商铺、详情、购物车、地址、我的和用户布局，保留真实接口、路由、Token 与加购逻辑。 |
| 49. 构建、文档与交接 | completed | 最终前端构建和静态约束检查通过；页面设计、发现、进度和交接已更新。 |

### 本轮边界

- 仅优化普通用户业务页面的 UI/UX；不重构登录页、不实现注册、订单、支付或新后端接口。
- 不修改后端、数据库、Redis Token、路由路径、Axios/Pinia 实例或既有地址/购物车正常业务逻辑。
- 不启动、停止或重启任何服务，不使用浏览器，不执行 Git；构建仅使用用户指定的前端命令。

### 本轮错误记录

| 错误 | 次数 | 处理 |
|---|---:|---|
| `npm run build` 在 `HomeView.vue` scoped CSS 发现孤立 `.`，PostCSS 报 `Unknown word` | 1 | 已定位为本轮样式末尾的单字符拼写错误，删除后改用重新构建验证。 |

### 最终验证

- `D:/develop/NodeJS/npm.cmd run build`：通过，转换 1692 个模块并生成 `frontend/dist`。
- 静态检查：仅 1 个 Axios 实例；无 `contactName/contactPhone`、无硬编码 localhost、无订单/支付 API 请求；商铺列表使用真实服务端分页。
- 非阻断警告：第三方 `@vueuse/core` 的两条注释位置提示，以及压缩后主包 1124.92 kB 超过 500 kB。
- 未启动、停止或重启任何服务，未使用浏览器；需用户按 HANDOFF 的人工清单验收。下一批须经审核后才可进行登录页重构与注册登录设计。

## 2026-07-11 账号注册数据库准备批次

| 阶段 | 状态 | 内容 |
|---|---|---|
| 50. 认证与用户表审计 | completed | 基线 SQL、设计文档与 `User` 实体均确认缺少 `username`、`password_hash` 和用户名唯一索引；现有验证码/Token 登录链路保持单一实现。 |
| 51. 实体、SQL 与异常映射准备 | completed | 已完成账号字段实体映射、基线 SQL、一次性迁移脚本、用户名重复键提示和最小 BCrypt 依赖；未创建注册接口或页面。 |
| 52. 编译、文档与迁移交接 | awaiting_manual_migration | 编译通过，API/契约/数据库/页面设计/发现/进度/交接已更新；等待用户人工执行并复核增量 SQL 后才能实施注册功能。 |

## 2026-07-11 账号注册实施前数据库门禁复核

| 阶段 | 状态 | 内容 |
|---|---|---|
| 53. 恢复与只读结构核验 | blocked | 用户报告已执行迁移，但实际 `qinghe_life.qh_user` 不含 `username`、`password_hash` 或 `uk_qh_user_username`，与实施前提不符。 |

### 停止结论

- 只读 `information_schema`、`SHOW COLUMNS` 与 `SHOW INDEX` 均连接至 `qinghe_life`，显示 `qh_user` 仍仅有 `phone/nickname/avatar_url/gender/status` 等旧列。
- 严格按本轮门禁停止：未创建注册接口、DTO、Service、前端页面、路由、测试或公开白名单；未重新执行、修改或删除任何 SQL。
- 需用户在 DataGrip 确认是否对正确的 `qinghe_life` 实例执行脚本，并在字段与索引实际出现后重新发起实施任务。

## 2026-07-11 账号注册实现批次

| 阶段 | 状态 | 内容 |
|---|---|---|
| 54. 恢复与数据库门禁 | completed | 只读确认 `qinghe_life.qh_user` 已有可空 `username/password_hash` 与唯一索引 `uk_qh_user_username`，实体字段一致。 |
| 55. 注册后端与测试 | completed | 已在现有用户认证模块实现注册、旧用户原地绑定、BCrypt 与精确清理的集成测试；待全量运行确认。 |
| 56. 注册前端与文档 | completed | 已新增注册 API、匿名路由、注册页面和登录入口；后续补齐最终文档结果。 |
| 57. 全量验证与停止 | completed | 注册测试、全量 Maven 测试、后端打包与前端构建均通过；接口、页面、发现、进度和交接已更新。 |

### 本轮边界

- 仅实现账号注册；不实现账号密码登录、找回密码、订单、支付、优惠券或其他业务。
- 不改数据库结构、不再次执行迁移 SQL、不启动或停止任何服务、不使用浏览器、不执行 Git。
- 注册必须复用现有 `UserController`、`RedisKeys.code(phone)`、`qh:login:token:` 会话体系、`http.js`、Router 与 Pinia Store。

### 最终验证

- 数据库只读门禁：`username varchar(32)`、`password_hash varchar(100)` 均允许 NULL，`uk_qh_user_username` 为唯一索引；实体字段一致。
- `mvn -Dtest=UserRegistrationIntegrationTest test`：通过，5 项测试。
- `mvn test`：通过，18 项测试，0 failure、0 error、0 skipped；认证、异常、地址和购物车回归继续通过。
- `mvn clean package -DskipTests`：通过，编译 89 个主源码和 5 个测试源码，生成 `backend/target/qinghe-life-backend-1.0.0.jar`。
- `D:/develop/NodeJS/npm.cmd run build`：通过，转换 1694 个模块并生成 `frontend/dist`；保留第三方注释与主包大小非阻断警告。
- `REGISTER_TEST_` 用户名/昵称残留数据库计数为 0。未执行 SQL、Git、浏览器或服务控制；下一批需用户审核后才可进行账号密码登录。

### 本轮停止门禁

- 当前基线缺少注册所需账号字段，必须生成 `account_register_increment.sql`，但严禁自动执行。
- 在用户通过 DataGrip 人工执行并完成字段/索引核验前，不创建注册 Controller/Service/DTO/前端页面，不运行或宣称真实注册集成测试。
- 不改动现有 `/api/user/code`、`/api/user/login`、Redis Token、地址、购物车、订单或其他业务代码。

### 最终验证

- `mvn -DskipTests compile`：通过，编译 88 个主源码；`spring-security-crypto` 最小依赖解析成功。
- 静态检查：迁移脚本只含两列 `ALTER TABLE` 与一个 `CREATE UNIQUE INDEX`；实体、基线 SQL、迁移 SQL和全局重复键映射已同步账号字段/索引；不存在注册接口、页面或第二套认证实现。
- 未执行 SQL、未连接或修改数据库、未运行 `mvn test`、注册集成测试或前端构建。注册功能状态为 `awaiting_manual_migration`，不得声称已完成。

## 2026-07-12 AOP 操作日志批次

| 阶段 | 状态 | 内容 |
|---|---|---|
| 58. 现状恢复与数据库表门禁 | completed | 已确认现有项目只有 `UserContext`，且基线 SQL、设计、实体和 Mapper 均无 `qh_operate_log`。 |
| 59. 日志持久化准备或实现 | awaiting_manual_migration | 已生成基线 SQL、仅含 `CREATE TABLE` 的 `operate_log_increment.sql`、实体和 Mapper；按门禁不创建注解或切面。 |
| 60. 回归、文档与交接 | awaiting_manual_migration | 静态复核通过：增量仅 1 条 `CREATE TABLE`，字段/索引/实体无缺失，未出现注解或切面；API、数据库设计、交接、进度与发现已登记迁移前边界。 |

### 本轮边界

- 仅实现 AOP 操作日志；复用现有 `UserContext`、`UserDTO`、全局异常处理、Mapper 约定和 Spring 注入的 `ObjectMapper`。
- 禁止修改登录、Redis Token、前端、订单或服务进程；不执行 SQL、Git 或数据库写入脚本。
- 记录成功和失败状态、耗时与必要请求上下文；日志持久化失败仅写服务端 error 日志，不影响原业务或异常传播。

### 本轮错误记录

| 错误 | 次数 | 处理 |
|---|---:|---|
| 跨多个文档的首次合并补丁在 `database-design.md` 上下文不匹配 | 1 | 未应用任何更改；改为读取实际末尾和表格锚点后分拆补丁，后续更改均已写入。 |
