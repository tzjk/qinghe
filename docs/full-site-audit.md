# 青禾校园生活服务系统全站功能审计报告

**审计日期：** 2026-07-16  
**范围：** 只测试、分析和报告；未修改业务代码、数据库结构、迁移 SQL 或运行服务。  
**证据优先级：** 当前运行环境 > 当前源码/构建 > 文档契约 > 历史交接。

## 1. 功能总览与审计边界

本次登记了 21 个前端页面/路由和 26 个功能或安全流程。审计开始时 `5174`、`8090`、`3306`、`6379` 均未监听，因此所有浏览器、HTTP、MySQL、Redis、验证码、Token、地址/购物车实际写入及 AOP 入库测试均无法执行。未启动或重启服务，也未创建任何 `AUDIT_TEST_` 数据。

| 结果口径 | 数量 | 说明 |
|---|---:|---|
| 构建通过 | 2 | Maven 编译、Vite 生产构建。不是运行功能通过。 |
| 静态失效/缺失 | 5 | 订单、优惠券、探店、后台、404。 |
| 运行阻塞 | 19 | 依赖未监听的页面、接口、认证、安全和数据流程。 |
| 运行失败 | 0 | 没有把“未启动”错误归为业务失败。 |

已完成的源码级能力包括用户认证、首页、分类、商铺、商品、地址和购物车；订单、优惠券、探店写操作及后台只有文档/实体或页面骨架，不能视作已交付功能。

## 2. 页面—接口—后端—数据表矩阵

| 页面/路由 | 前端状态 | 预期接口 | 实际后端 | 数据表 | 审计结论 |
|---|---|---|---|---|---|
| 首页 `/` | 真实 API 页面 | `/api/home/summary` | `HomeController` | 商铺、分类、商品、券、探店 | 运行阻塞 |
| 登录 `/login` | 真实表单 | `/api/user/code`、`/login` | `UserController` | 用户、Redis | 运行阻塞 |
| 注册 `/register` | 真实表单 | `/api/user/code`、`/register` | `UserController` | 用户、Redis | 运行阻塞 |
| 商铺 `/shops` | 真实筛选/分页 | `/api/categories`、`/api/shops` | Category/Shop Controller | 分类、商铺 | 运行阻塞 |
| 商铺详情 `/shops/:id` | 真实 API/加购 | 商铺、商品、评论、购物车 | Shop/Cart Controller | 商铺、商品、评论、购物车 | 运行阻塞 |
| 商品 | 无独立路由，首页/商铺消费 | `/api/goods`、`/api/goods/{id}` | `GoodsController` | 商品 | 运行阻塞 |
| 购物车 `/cart` | 真实列表和动作 | `/api/cart` 资源与动作 | `CartController` | 购物车、商品、商铺 | 运行阻塞 |
| 地址 `/profile/addresses` | 真实表单/动作 | `/api/addresses` 资源与默认动作 | `AddressController` | 用户地址 | 运行阻塞；模型不适配校园 |
| 我的 `/profile` | 真实入口 | `/api/user/me`、`/profile` | `UserController` | 用户 | 运行阻塞 |
| 订单 `/orders` | `PageScaffold` 骨架 | `/api/orders/**` | **无 Controller** | 订单、明细 | 失效/缺失 |
| 优惠券 `/coupons` | `PageScaffold` 骨架 | `/api/coupons/**` | **无 Controller** | 券、用户券 | 失效/缺失 |
| 探店 `/blogs` | `PageScaffold` 骨架 | `/api/blogs/**` | **无 Controller** | 探店、评论、互动 | 失效/缺失 |
| 后台 `/admin/**` | 看板/资源骨架 | `/api/admin/**` | **无 Controller** | 多业务表 | 失效/缺失 |
| 404 | 无 catch-all 路由 | 应有前端 NotFound/后端 404 | 全局异常未覆盖 NoHandler | 无 | 失效/缺失 |

## 3. 已验证、未验证、缺失和失效

### 已验证通过

- `mvn -DskipTests compile` 成功；未发现 Java 编译错误。
- `npm.cmd run build` 成功，转换 1694 个模块。首次受沙箱文件权限影响未能构建，获批准后成功；该首次失败不是项目构建缺陷。
- 静态确认存在单一 Axios 实例、单一用户认证 Controller、用户/地址/购物车 Controller、全局异常处理器、双认证拦截器与操作日志切面。

### 运行未验证（环境阻塞）

- 验证码发送/登录、随机验证码、注册、Token 恢复和续期、退出。
- 首页、分类、商铺筛选/分页、商品、评论与缓存。
- 加购物车、数量/选中/删除/清空、地址新增/编辑/默认/删除、私有数据隔离。
- 401、405、409、500、503、未存在资源 404、重复请求和 AOP 入库/脱敏。
- Redis Key 类型、TTL、Token 删除、真实数据库字段/索引和 `qh_operate_log` 实际写入。
- 浏览器控制台、网络错误、加载/Skeleton/Empty/重试交互和按钮禁用。

### 已确定缺失或失效

- 订单、优惠券、探店和后台是可见路由，但页面为 `PageScaffold` 占位，且缺少对应 Controller/API。
- Vue Router 没有 `/:pathMatch(.*)*`，不存在产品级 404 页面。
- `requiresAdmin` 守卫只检查普通 `getToken()`；没有独立管理员登录态或后端管理员认证 API。

## 4. 校园地址专项

### 当前结论

当前地址**不符合校园配送主场景**。`qh_user_address` 与 `UserAddress`、`AddressCreateDTO`、`AddressUpdateDTO`、`AddressVO`、`AddressView.vue` 都围绕 `province/city/district/detailAddress` 设计。命名在各层大体一致，归属与默认规则在源码中正确，但语义仍是通用城市收货地址。

| 审计项 | 结论 |
|---|---|
| 省/市/区合理性 | 对单校区/校园配送为冗余主字段，不应成为必填核心。 |
| 前端、DTO、实体、SQL 一致性 | 静态一致：均使用 receiver + 省市区 + detailAddress；实际表未能运行复核。 |
| 当前用户归属 | 静态通过：按 `id + user_id` 查询，越权抛 404。 |
| 默认地址 | 静态通过：首地址默认、设默认前清除本人旧默认、删除默认后补选；实际并发/事务未验证。 |
| 是否需要迁移 | **需要**，表与 DTO/VO/API/前端表单都必须同步替换。 |
| 推荐方案 | 校区/区域/楼栋类型使用固定下拉；楼栋、配送点使用字典表或后台配置；房间、详细位置、备注保留文本。 |

### 推荐目标字段

`campus_id`、`campus_area`、`building_type`、`building_name`、`floor`、`room_no`、`delivery_point_id`、`detail_location`、`receiver_name`、`receiver_phone`、`is_default`、`delivery_remark`。

建议先建立可配置字典表：校区、校园区域、楼栋、配送点。前台使用受控下拉，后台管理其启停和排序；不要把楼栋和自提点永久硬编码在前端。迁移需要保留旧地址并制定省市区到默认校区/备注的回填策略。

## 5. UI、接口、安全与性能

### UI/接口

- 已实现的商铺列表有筛选、服务端分页、Loading、`AsyncState`、Skeleton、Empty 与重试；地址/购物车也使用相同状态组件。
- 订单、优惠券、探店、后台没有真实动作、分页或错误重试，属于骨架而非功能页。
- 未知路由没有 404 页面；需要增加 catch-all 和显式返回入口。
- 浏览器服务未启动，无法检查控制台错误、重复请求、图片失败和无效按钮的真实表现。

### 安全

- 源码积极项：验证码随机生成、Redis TTL、登录成功删码；Token 使用随机值并在刷新拦截器续期；密码为 BCrypt；注册 DTO 忽略 `userId/role/status/passwordHash`；用户 DTO 和操作日志对手机号/密码/验证码/Token 脱敏。
- `GlobalExceptionHandler` 静态覆盖 400、401、405、409、500、503，且不直接把原始异常信息返回给前端。
- AOP 只标注地址四个写操作与购物车新增、数量、删除、清空；**购物车选中状态修改未被审计日志覆盖**。
- 所有上述安全项因环境不可达而未做运行回归，不能当作实测通过。

### 性能与工程质量

- 前端产物：主 JS `1129.74KB`（gzip `374.13KB`）、CSS `377.30KB`（gzip `51.78KB`）；Vite 已给出大包警告。应对路由页面动态导入，并将 Element Plus/图标与业务页拆分为手工 chunk。
- `@vueuse/core` 有两条 Rollup PURE 注释警告，非阻断但需随依赖升级复核。
- 全局基础 CSS 为单一 `base.css`（3.5KB）；项目有 13 个 Vue 样式块。未静态确认同名选择器冲突，故“重复 CSS”结论为未确认，不能声称无重复。
- 未发现订单/券/探店 API 调用藏在已实现页面；这些路由直接使用占位组件，避免了假接口成功，但构成可见的假功能页。

## 6. 问题分级清单

| 级别 | 问题 | 复现/证据 | 原因 | 修复建议 | 涉及文件 | 迁移 |
|---|---|---|---|---|---|---|
| P0 | 整套运行环境不可用 | 5174/8090/3306/6379 均无监听 | 本轮运行实例未启动或不可访问 | 由用户恢复既有服务后重跑本报告的运行阻塞项 | 运行环境 | 否 |
| P1 | 订单页无业务实现 | 打开 `/orders`；源码仅 `PageScaffold`，无 Controller | M3 未完成 | 单独实施订单创建/查询模块，先做接口和集成测试，不进入支付 | OrdersView、后端订单域 | 否（现表待复核） |
| P1 | 优惠券页无业务实现 | `/coupons` 为骨架且无 Controller | M3 未完成 | 在订单稳定后单独实施券查询/领取 | CouponsView、优惠券域 | 否（现表待复核） |
| P1 | 探店页无业务实现 | `/blogs` 为骨架且无 Controller | M4 未完成 | 在订单/券后单独实施探店读写与互动 | BlogsView、探店域 | 否（现表待复核） |
| P1 | 后台假路由且无管理员隔离 | `/admin/**` 共用骨架，守卫仅 `getToken()` | 无 Admin auth/API，普通 Token 可通过前端守卫 | 单独做管理员认证、独立 Store/Token 和后端授权 | router、Admin*View、admin domain | 否 |
| P1 | 地址模型不是校园配送模型 | SQL/DTO/实体/表单均要求省市区 | 继承通用地址模型 | 按本报告校园字段方案重构并迁移 | 地址 SQL、Java、Vue、文档 | **是** |
| P2 | 无前端 404 | Router 无 catch-all | 路由未覆盖未知地址 | 加 NotFoundView 和 `/:pathMatch(.*)*` | router、NotFoundView | 否 |
| P2 | 未知 API 的 404 行为未实测 | 服务不可达；历史记录显示鉴权可先返回 401 | 拦截器范围过宽的风险 | 服务恢复后专测匿名/受保护未知路径并调整匹配顺序 | WebConfig、异常层 | 否 |
| P2 | 前端首包过大 | Vite: JS 1129.74KB、CSS 377.30KB | 所有页面/依赖聚合进入入口 chunk | 路由懒加载、manualChunks、按需图标 | router、vite config | 否 |
| P2 | 购物车选中未写 AOP 日志 | `CartController` 的 selected 动作无 `@OperateLog` | 标注覆盖不完整 | 是否纳入审计策略需明确；若属于关键写操作则补注解和测试 | CartController、AOP 测试 | 否 |
| P3 | 页面 E2E/控制台回归缺失 | 浏览器自动化无可用运行环境 | 只有构建/集成测试记录 | 服务恢复后为核心流增加浏览器回归清单/自动化 | frontend tests | 否 |
| P3 | 样式重复未建立自动检查 | 13 个 Vue 样式块，单一 base.css | 无 CSS lint/重复选择器检测 | 引入 lint 或在 UI 批次做组件样式审查 | frontend styles | 否 |

**问题数量：P0=1，P1=5，P2=4，P3=2。**

## 7. 推荐后续开发顺序（每次仅一个模块）

1. **运行环境恢复与最小回归。** 用户启动既有 5174、8090、MySQL、Redis 后，先执行验证码、登录、Token、地址、购物车、404、AOP 的受控实测与精确清理；不改代码。
2. **校园地址改造。** 先完成数据模型/迁移设计和回滚方案，用户人工执行迁移后再单独实现前后端与真实隔离/默认规则测试。
3. **订单创建与查询。** 只实现订单创建、查询和详情，不进入支付、取消、完成或优惠券；完成后再评估优惠券。

后续的优惠券、探店、后台、404/性能优化均应独立成批次，不能与上述模块混做。

## 8. 审计数据清理

本轮未连接 MySQL 或 Redis，未创建任何 `AUDIT_TEST_` 用户、地址、购物车、日志、Token 或验证码 Key；因此没有待清理测试数据。禁止操作均未执行。
