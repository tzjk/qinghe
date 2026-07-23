# 接口契约（M0）

## 2026-07-20 学生首次建档多校区与 Redis 测试分层契约

- `PUT /api/student/profile` 在学生无当前 `current_flag=1` 资料时必须提交 `realName`、`studentNo`、`campusId`、`collegeName`、`majorName`、`className` 和合法 `contactPhone`。`campusId` 必须指向真实且 `status=1` 的 `qh_campus`；`campusName` 不属于请求契约，服务端自行读取名称。
- 响应 `StudentProfileVO` 返回 `campusId` 和 `campusName`，并继续排除内部 ID、`currentFlag`、`passwordHash`、`activeFlag` 与二维码令牌。当前资料已存在时，写操作只更新联系电话，归属校区不可由学生端改变。
- `POST /api/student/dorm/qr/resolve`、`POST /api/student/dorm/check-in`、退宿和换寝契约不因本轮改变；入住仍要求学生资料校区与二维码宿舍校区一致。
- 测试契约分层：Mockito Redis 测试只能证明单元分支；管理员登录、Token TTL/删除、验证码和 Redis 会话状态必须使用真实 Redis 的 `@SpringBootTest` 集成测试，在 Windows 本机网络环境运行。沙箱网络隔离结果不得被记录为代码回归失败或完整测试通过。

## 2026-07-19 学籍异动、毕业与批量操作契约

- `GET /api/admin/dorm/students` 使用通用 `page/size`，可选 `studentName/studentNo/collegeName/majorName/className/studentStatus/accommodationStatus`；住宿筛选值为 `IN_DORM`、`NOT_IN_DORM`。当前资料仅由 `current_flag=1` 决定。
- 单个异动均以 `userId` 定位当前学生资料，`reason` 必填且最长 255。转专业仅允许 `ENROLLED` 并要求 `collegeName/majorName/className`；休学仅允许 `ENROLLED`，明确 `checkoutDorm`；退学/毕业仅允许 `ENROLLED`；复学仅允许 `SUSPENDED`。普通学生没有对应写接口。
- 所有批量端点遵循 `POST .../preview {ids}` → `POST ... {ids,reason,previewToken}`。`ids` 为不重复 ID，长度 1 至 100；预览返回选择数、符合数、当前入住数、将释放套装数、不符合数和令牌。提交会重新生成并比对令牌，避免范围、状态或二维码轮换后重复提交。
- 退学、毕业和选择退宿的休学在一个事务中关闭当前入住，写业务原因、时间和管理员，释放对应套装；床位目录状态不变。复学不恢复已关闭入住或旧床位。批量退宿同样只关闭当前入住并保留历史。

## 2026-07-18 管理员校区与楼栋契约

- 管理员接口位于 `/api/admin/dorm`：`GET /campuses`、`GET /buildings`、`GET /buildings/available`、`GET /buildings/{id}`、`POST /buildings`、`PUT /buildings/{id}`、`PUT /buildings/{id}/status`。缺失、普通用户或无效 Token 均按管理员门禁返回 401。
- 楼栋创建 DTO 接收 `campusId/buildingCode/buildingName/buildingType/area/remark/status`；编辑 DTO不接收校区归属；状态修改独立接收 `status`。楼栋响应至少返回 `id/campusId/campusName/buildingCode/buildingName/buildingType/area/remark/status/roomCount/currentCheckinCount`。
- 楼栋编码先去首尾空格并转大写，在同校区内唯一；类型限定为宿舍楼、教学楼、办公楼、实验楼、图书馆或其他。备注可空、最长 255；区域与备注不互作别名。
- 寝室新增请求以 `buildingId/roomNo/floor/capacity/status/remark` 为准；即使兼容请求带有 `campusId` 也不参与关联判定，服务端从楼栋推导校区。

## 2026-07-18 订单与入住契约收口

- `POST /api/orders` 只可信任 `cartItemIds`、`addressId`、`remark`；四项金额、价格、库存和快照均由服务端重新读取计算，响应不含 Token、完整手机号或详细地址。
- `AdminDormCheckinVO` 不定义 `previousCheckinId`。该列只用于服务端换寝历史关联，管理员和学生响应均不得序列化。

## 2026-07-17 学生资料与宿舍扫码入住测试收口

- 学生资料首次建档固定为 `ENROLLED`；当前学号必须唯一。建档后客户端提交的姓名、学号、院系、专业和班级均不覆盖存储值，只有 `contactPhone` 可更新；`qh_user.nickname` 和 `qh_user.phone` 不替代实名或联系电话。
- `currentFlag` 是后端当前版本查询字段，普通响应、二维码解析、入住结果和“我的宿舍”均不得返回它，也不得返回 `passwordHash`、`activeFlag`、完整 `qrToken` 或内部 ID。
- “我的宿舍”仅按当前 Token 的 `UserContext` 查询；无有效入住返回空数据。已入住结果包含校区、楼栋、寝室、床位和资产，学号及联系电话按既定规则脱敏。

## 个人资料与用户头像 OSS 契约（2026-07-17）

- `GET /api/user/me` 与 `PUT /api/user/profile` 均只从 Bearer Token 的 `UserContext` 获取用户，不接收或暴露其他用户 ID。用户资料响应只含 `username`、昵称、`avatarUrl`、脱敏手机号及状态；不得含 `passwordHash`、密码或完整手机号。用户名、手机号在编辑页只读。
- `POST /api/user/avatar` 是唯一头像上传接口，受用户登录保护，使用字段名 `file`。服务端拒绝空文件、超过 2MB 的文件、非 jpg/jpeg/png/webp、Content-Type/扩展名/文件特征不一致的伪造图片；OSS 故障返回“头像上传服务暂不可用”。
- OSS 操作器使用 `avatars/{userId}/{yyyy/MM}/{uuid}.webp`，只允许当前用户头像业务调用。数据库保存失败必须删除刚上传对象；成功后同步当前 Redis Token Hash。旧对象只在 Bucket、域名、`.webp` 后缀和 `avatars/{currentUserId}/` 路径均匹配时删除，删除失败不回滚成功保存。
- 自动化测试 Mock `AliyunOSSOperator`，禁止真实 OSS 上传或删除。运行时必须设置 `OSS_ACCESS_KEY_ID` 与 `OSS_ACCESS_KEY_SECRET`；不得在文档、页面、请求或日志中写入密钥。

## 免注册登录与首次资料完善交付契约（2026-07-17）

- `POST /api/user/login` 为手机号验证码入口。验证码校验成功后，若手机号尚不存在，服务端自动创建基础用户并返回 `newUser=true`、`profileCompleted=false`、`hasPassword=false`；已有用户不重复建档。所有登录成功响应统一返回 `token`、上述三个布尔状态和安全 `user`。
- `POST /api/user/login/password` 接收 `phone`、`password`。密码不存在、手机号不存在及密码不匹配不暴露账户存在性，均返回“手机号或密码错误”。密码登录成功也走既有随机 Token 与 Redis 会话创建路径。
- 受保护的 `POST /api/user/profile/complete` 接收昵称、收货人、收货电话、校区、楼栋和房间号或配送点；头像、楼层、详情、标签、备注和密码可选。接口复用地址的校区/楼栋启用与归属校验，并在同一事务内创建默认地址、更新用户资料和将 `profileCompleted` 置为 `true`。密码不填不影响资料完成；填入时必须 8 至 32 位且同时含字母、数字，并只以 BCrypt 保存。
- `profileCompleted` 和 `hasPassword` 均属于安全 `UserDTO`，持久化到 `qh:login:token:{token}` Redis Hash 并在资料完成时同步当前会话；`newUser` 只表示本次验证码登录是否自动建档。响应、日志、Redis Hash、Pinia 和 localStorage 均不得保存明文密码或密码哈希。
- 前端在 `profileCompleted=false` 时跳转 `/profile/complete`，否则遵循原 `redirect` 或首页路径。Token 继续使用 `Authorization: Bearer <token>`；既有 RefreshTokenInterceptor 续期、LoginInterceptor 鉴权和验证码成功即删除 Key 的约定不变。
- 本批后端回归已在 `Q:\backend` 实际通过 25 项（0 failure、0 error、0 skipped），并已完成 `mvn clean package -DskipTests` 打包。浏览器人工验收不是该命令行结果的替代项。

## 1. 通用约定

- 基础前缀为 `/api`；后台所有路径均以 `/api/admin` 开头。
- 身份请求头：`Authorization: Bearer <token>`。无效或缺失 Token 返回 HTTP 401；前端清理对应登录状态并跳转正确的用户或管理员登录页。
- 统一响应：`{ "code": 200, "message": "success", "data": {} }`。分页 `data` 固定包含 `records`、`total`、`page`、`size`；请求分页参数为 `page`（从 1 开始）、`size`。
- ID 为 `Long`；金额按两位小数的 `BigDecimal` 语义处理。状态修改请求统一传 `{ "status": 0 | 1 }`，商品上架状态改传 `{ "saleStatus": "ON_SALE" | "OFF_SALE" }`。
- 本文档不记录 MySQL 或 Redis 密码；连接配置仅见受控后端 `application.yml` 的环境变量占位表达式。

## 2. 用户、地址与首页

| 方法 | 路径 | 鉴权 | 请求要点 | 响应 data |
|---|---|---|---|---|
| POST | `/api/user/code` | 否 | JSON `{ "phone": "13900009991" }` | 每次随机生成 6 位验证码，覆盖写入 Redis database 2 的 `qh:login:code:{phone}`，TTL 为 2 分钟。仅 `dev/local` profile 在后端本地日志提供测试验证码，响应不含验证码。 |
| POST | `/api/auth/register` | 否 | `username`、`phone`、`code`、`password`、`confirmPassword` | 成功只返回 `code=200`、`data=null`；不自动登录，不返回 Token、密码、密码哈希、验证码或 User。 |
| POST | `/api/user/login` | 否 | `phone`、`code` | `LoginVO`：`token`、`user`。 |

登录读取与发送阶段完全相同的验证码 Key；缺失（含过期）或错误均返回业务 `code=400`，成功后立即删除验证码，再创建 `qh:login:token:{token}` Redis Hash 会话（30 分钟）。Hash 仅保存 `UserDTO` 的字符串安全字段。

认证拦截顺序为：`RefreshTokenInterceptor` order 0（Bearer Token、Redis Hash、`UserContext`、30 分钟续期、afterCompletion 清理）→ `LoginInterceptor` order 1（仅检查 `UserContext`，未登录 HTTP 401）。验证码、登录、分类、首页、公开商铺和商品接口排除登录校验；地址、购物车、资料等接口保持受保护。

前端浏览器统一请求 `/api/user/code` 与 `/api/user/login`；`http.js` 的 baseURL 为 `/api`，API 文件只写 `/user/code` 与 `/user/login`，不存在重复 `/api`。开发模式 Vite 将 `/api` 原样转发至 `http://localhost:8090`，不删除前缀，故实际后端路径分别为 `POST /api/user/code` 与 `POST /api/user/login`。此前缺少代理是 404 根因；当前状态为 `awaiting_manual_verification`，不得以自动测试替代外部浏览器验收。
| GET | `/api/user/me` | 用户 | 无 | `UserVO`。 |
| PUT | `/api/user/profile` | 用户 | 昵称、头像、性别等允许字段 | `UserVO`。 |
| POST | `/api/user/logout` | 用户 | 无 | 退出结果。 |
| GET | `/api/campuses` | 否 | 无 | 启用校区 `List<CampusVO>`。 |
| GET | `/api/campuses/{campusId}/buildings` | 否 | 无 | 指定校区启用楼栋 `List<BuildingVO>`。 |
| GET | `/api/addresses` | 用户 | 无 | 当前用户 `List<AddressVO>`，默认地址优先。 |
| GET | `/api/addresses/{id}` | 用户 | 无 | 当前用户单个 `AddressVO`。 |
| POST | `/api/addresses` | 用户 | 校园地址字段 | `AddressVO`；首个地址自动默认。 |
| PUT | `/api/addresses/{id}` | 用户 | 校园地址字段 | `AddressVO`；仅本人地址，历史地址保存时转校园地址。 |
| DELETE | `/api/addresses/{id}` | 用户 | 无 | 删除结果；仅本人地址。 |
| PUT | `/api/addresses/{id}/default` | 用户 | 无 | 更新后的 `AddressVO`；事务内保证同一用户仅一个默认地址。 |
| GET | `/api/home/summary` | 否 | 无 | `HomeSummaryVO`。 |

### 校园地址契约（已实施）

- 请求体只接收 `receiverName`、`receiverPhone`、`campusId`、`buildingId`、`floor`、`roomNo`、`deliveryPoint`、`detail`、`label`、`remark`、可选 `isDefault`；`campusId`、`buildingId` 必填，`roomNo` 与 `deliveryPoint` 至少一个非空。客户端的 `userId`、`addressType` 与省市区旧字段不参与保存。
- 服务端验证校区与楼栋启用状态及归属关系，使用 Token 的 `UserContext.userId` 进行所有地址访问/写入隔离；保存 `area`、`buildingType`、`buildingName` 快照并固定 `CAMPUS` 类型。
- `AddressVO` 返回目录名称、快照、位置、标签、备注、格式化完整地址和 `maskedReceiverPhone`。历史记录仍返回旧字段和 `HISTORICAL`；更新后转换为 `CAMPUS`，不删除旧字段历史数据。目录仅提供只读查询，不开放管理接口。

`HomeSummaryVO` 固定包含：`banners`、`categories`、`recommendedShops`、`hotGoods`、`availableCoupons`、`featuredBlogs`。其中 `banners` 从 `qh_shop` 中 `is_featured=1` 且 `status=1` 的商铺按 `sort_order` 取得；不新增轮播表。

## 3. 商铺、商品与购物车

| 方法 | 路径 | 鉴权 | 请求/查询要点 | 响应 data |
|---|---|---|---|---|
| GET | `/api/shops` | 否 | `page`、`size`、`categoryId` | `PageResult<ShopVO>`。 |
| GET | `/api/shops/{id}` | 否 | 无 | `ShopVO`。 |
| GET | `/api/shops/{id}/goods` | 否 | `page`、`size` | `PageResult<GoodsVO>`。 |
| GET | `/api/shops/{id}/comments` | 否 | `page`、`size` | `PageResult<CommentVO>`；按 `shop_id` 与 `status=1` 查询。 |
| GET | `/api/goods` | 否 | `page`、`size`、`shopId` | `PageResult<GoodsVO>`。 |
| GET | `/api/goods/{id}` | 否 | 无 | `GoodsVO`。 |
| GET | `/api/cart` | 用户 | 无 | `CartSummaryVO`。 |
| POST | `/api/cart` | 用户 | `goodsId`、`quantity` | `CartItemVO`。 |
| PUT | `/api/cart/{id}` | 用户 | `quantity` | `CartItemVO`。 |
| PUT | `/api/cart/{id}/selected` | 用户 | `selected` | `CartItemVO`。 |
| DELETE | `/api/cart/{id}` | 用户 | 无 | 删除结果。 |
| DELETE | `/api/cart` | 用户 | 无 | 清空本人购物车结果。 |

M3A 前端已接入上述地址与购物车接口：地址请求仅使用 receiver 命名，购物车请求只发送 `goodsId`、`quantity` 或 `selected`，不发送用户编号、价格、金额或订单数据。

2026-07-10 无浏览器静态复核确认上述请求均复用 `frontend/src/api/http.js` 的 Bearer 请求封装；不存在第二个 Axios 实例、`contactName/contactPhone`、硬编码本地 8090、客户端 `userId` 或订单接口请求。

## 4. 订单、评价与优惠券

| 方法 | 路径 | 鉴权 | 请求/查询要点 | 响应 data |
|---|---|---|---|---|
| POST | `/api/orders` | 用户 | 地址 ID、购物车项或选中项、可选 `userCouponId` | `OrderDetailVO`。 |
| GET | `/api/orders` | 用户 | `page`、`size`、可选 `status` | `PageResult<OrderVO>`。 |
| GET | `/api/orders/{id}` | 用户 | 无 | `OrderDetailVO`。 |
| PUT | `/api/orders/{id}/cancel` | 用户 | 可选取消原因 | `OrderVO`。 |
| PUT | `/api/orders/{id}/pay` | 用户 | 无；仅模拟支付 | `OrderVO`。 |
| PUT | `/api/orders/{id}/complete` | 用户 | 无 | `OrderVO`。 |
| POST | `/api/orders/{id}/review` | 用户 | `content`、`score`（1 至 5）、可选 `images` | `CommentVO`；写入 `qh_comment`，同一订单仅一条评价。 |
| GET | `/api/coupons` | 用户 | `page`、`size` | 可领取 `PageResult<CouponVO>`。 |
| POST | `/api/coupons/{id}/claim` | 用户 | 无 | `UserCouponVO`。 |
| GET | `/api/coupons/mine` | 用户 | `page`、`size`、可选 `status` | `PageResult<UserCouponVO>`。 |

订单创建必须校验初始 `PENDING_PAY` 状态、商品上架与库存、地址归属、优惠券归属/有效期/门槛。支付不调用第三方服务。校园地址上线后，订单还必须在创建时写入不可变的完整配送快照，不能在订单详情中实时拼接可被用户修改的地址。

## 5. 探店与互动

| 方法 | 路径 | 鉴权 | 请求/查询要点 | 响应 data |
|---|---|---|---|---|
| GET | `/api/blogs` | 否 | `page`、`size`、可选 `shopId` | `PageResult<BlogVO>`。 |
| GET | `/api/blogs/{id}` | 否 | 无 | `BlogVO`。 |
| POST | `/api/blogs` | 用户 | 标题、内容、封面、可选商铺 ID | `BlogVO`。 |
| PUT | `/api/blogs/{id}/like` | 用户 | 无 | 更新后的互动摘要。 |
| PUT | `/api/blogs/{id}/favorite` | 用户 | 无 | 更新后的互动摘要。 |
| POST | `/api/blogs/{id}/comments` | 用户 | `content`、可选 `parentId`、可选 `images` | `CommentVO`；写入 `qh_comment.blog_id`。 |
| GET | `/api/blogs/mine` | 用户 | `page`、`size` | 本人探店分页。 |
| GET | `/api/blogs/favorites` | 用户 | `page`、`size` | 本人收藏分页。 |

## 6. 后台接口

后台接口使用独立管理员认证，不得以用户端登录态代替管理员权限。以下逐项路径是 M5 的唯一实现基线。

### 管理员登录与数据看板

| 方法 | 路径 | 功能 |
|---|---|---|
| POST | `/api/admin/auth/login` | 管理员登录。 |
| POST | `/api/admin/auth/logout` | 管理员退出。 |
| GET | `/api/admin/auth/me` | 查询当前管理员。 |
| GET | `/api/admin/dashboard/summary` | 查询看板统计。 |

### 分类、商铺与商品管理

| 模块 | 查询 | 新增 | 修改 | 删除 | 状态修改 |
|---|---|---|---|---|---|
| 分类 | `GET /api/admin/categories`、`GET /api/admin/categories/{id}` | `POST /api/admin/categories` | `PUT /api/admin/categories/{id}` | `DELETE /api/admin/categories/{id}` | `PUT /api/admin/categories/{id}/status` |
| 商铺 | `GET /api/admin/shops`、`GET /api/admin/shops/{id}` | `POST /api/admin/shops` | `PUT /api/admin/shops/{id}` | 不提供物理删除 | `PUT /api/admin/shops/{id}/status` |
| 商品 | `GET /api/admin/goods`、`GET /api/admin/goods/{id}` | `POST /api/admin/goods` | `PUT /api/admin/goods/{id}` | `DELETE /api/admin/goods/{id}` | `PUT /api/admin/goods/{id}/sale-status` |

### 订单、优惠券与用户管理

| 模块 | 查询 | 新增 | 修改 | 删除 | 状态修改 |
|---|---|---|---|---|---|
| 订单 | `GET /api/admin/orders`、`GET /api/admin/orders/{id}` | 不提供：订单由用户创建 | `PUT /api/admin/orders/{id}`（仅管理备注等允许字段） | 不提供：保留订单审计记录 | `PUT /api/admin/orders/{id}/status` |
| 优惠券 | `GET /api/admin/coupons`、`GET /api/admin/coupons/{id}` | `POST /api/admin/coupons` | `PUT /api/admin/coupons/{id}` | `DELETE /api/admin/coupons/{id}` | `PUT /api/admin/coupons/{id}/status` |
| 用户 | `GET /api/admin/users`、`GET /api/admin/users/{id}` | 不提供：用户自助注册/登录 | `PUT /api/admin/users/{id}`（仅后台允许字段） | 不提供：保留用户与订单关联 | `PUT /api/admin/users/{id}/status` |

### 探店与评论管理

| 模块 | 查询 | 新增 | 修改 | 删除 | 状态修改 |
|---|---|---|---|---|---|
| 探店 | `GET /api/admin/blogs`、`GET /api/admin/blogs/{id}` | `POST /api/admin/blogs` | `PUT /api/admin/blogs/{id}` | `DELETE /api/admin/blogs/{id}` | `PUT /api/admin/blogs/{id}/status` |
| 评论 | `GET /api/admin/comments`、`GET /api/admin/comments/{id}` | `POST /api/admin/comments` | `PUT /api/admin/comments/{id}` | `DELETE /api/admin/comments/{id}` | `PUT /api/admin/comments/{id}/status` |

后台列表查询同样使用 `page`、`size`。状态接口的请求体按第 1 节约定；商品状态接口使用 `saleStatus`，其他资源使用 `status`。

## 7. 冲突审查结果

| 审查对象 | 结果 | 说明 |
|---|---|---|
| 用户与管理员登录 | 无冲突 | `/api/user/login` 与 `/api/admin/auth/login` 分离。 |
| 地址资源 | 无冲突 | 统一为 `/api/addresses`，不再使用 `/api/user/addresses`。 |
| 商铺、订单与探店评论 | 无冲突 | 商铺仅查询 `/api/shops/{id}/comments`；订单评价为 `/api/orders/{id}/review`；探店评论为 `/api/blogs/{id}/comments`。 |
| 后台与用户资源 | 无冲突 | 后台全部使用 `/api/admin` 前缀。 |
| 首页轮播 | 无冲突 | 通过首页聚合响应返回，不新增接口或数据表。 |
| 状态字段 | 无冲突 | 通用 `status` 为 1/0；商品、订单、优惠券和探店使用各自命名字段。 |

## 8. 全局异常契约

- 所有由 `GlobalExceptionHandler` 返回的响应保持 `Result` 字段：`code`、`message`、`data`，且异常时 `data=null`。业务异常保留服务层的业务码和既有友好提示；参数类异常为 `code=400`。
- `MethodArgumentNotValidException`、`BindException`、`ConstraintViolationException`、缺少参数、参数类型不匹配和 JSON 不可读，必须返回字段级或明确提示；不得返回框架异常原文。
- `DuplicateKeyException` 使用唯一索引名白名单映射：`uk_qh_user_phone`→“该手机号已注册”，`uk_qh_user_username`→“用户名已存在”，`uk_qh_cart_user_goods`→“购物车中已存在该商品”，`uk_qh_order_no`→“订单号已存在，请稍后重试”。其余基线唯一约束也有业务提示；未知约束统一为“数据已存在，请勿重复提交”。客户端不得收到 SQL、表名、约束名或驱动错误文本。
- 非重复的数据完整性异常返回 `code=409` 和“数据约束不满足，请检查后重试”；不支持的 HTTP 方法返回 `code=405` 和“请求方法不支持”；Redis 或数据库连接异常返回 `code=503` 和“服务暂时不可用，请稍后重试”；未知异常返回 `code=500` 和“系统繁忙，请稍后重试”。这四类分别使用 HTTP 409、405、503、500 状态。
- 未登录仍由 `LoginInterceptor` 直接返回 HTTP 401；不被全局异常处理器改写。业务与参数问题仅记录不含敏感值的 `warn` 日志，数据完整性、基础设施和未知异常记录脱敏堆栈的 `error` 日志。

## 9. 账号注册契约

- 已实现匿名 `POST /api/auth/register`，复用唯一 `UserController` 的单一认证链路；验证码、登录和个人资料仍保持 `/api/user/**`。`LoginInterceptor` 已将注册路径列入白名单，RefreshTokenInterceptor、Bearer Token 与私有接口保护保持不变。
- 请求体只允许 `username`、`phone`、`code`、`password`、`confirmPassword`，不接受 `userId`、`role`、`status` 等字段。规则：用户名 4 至 20 位且仅字母/数字/下划线；手机号沿用登录校验；验证码 6 位数字；密码 8 至 32 位、同时包含字母和数字；确认密码一致。
- 注册与登录共用 `RedisKeys.code(phone)`，即 `qh:login:code:{phone}`，TTL 为 2 分钟。验证码缺失返回“验证码已过期或尚未获取”，错误返回“验证码错误”；成功后删除，失败不创建用户且不删除验证码。
- 新账号使用 BCrypt 保存至 `password_hash`；不得响应或记录密码、密码哈希、验证码、完整手机号或 Token。注册成功只返回安全结果，不自动登录。
- 手机号已存在且已有账号字段时返回“该手机号已注册”；手机号已存在但两个账号字段均为空时服务会原地绑定，不创建新用户。用户名和手机号预检查不能替代 `uk_qh_user_username`、`uk_qh_user_phone` 数据库唯一约束。
## 宿舍入住与资产二维码管理（设计登记，未实现）

> **结构门禁状态（2026-07-17）：** `real_name`、`contact_phone` 尚未出现在真实 `qh_student_profile`，所以本节仍为设计登记；`/api/student/profile`、`/api/student/dorm/qr/resolve`、`/api/student/dorm/check-in`、`/api/student/dorm/me` 均未实现、不可调用。

- 本节仅登记下一阶段接口边界，当前没有 Controller、Service、Mapper、页面或可调用接口；不得据此宣称功能已上线。
- 学生扫码后的拟定流程为：客户端解析 `QH-DORM-V1:{qr_token}`，若未登录先完成既有用户登录；登录后调用 `POST /api/dorm/checkins/confirm`，请求体只含 `qrToken`。服务端从 `UserContext` 取得当前用户，不接受客户端 `userId`，并校验令牌、学生当前学籍、床位与资产套装可用性后才创建入住记录。
- 拟定管理员资源统一位于 `/api/admin/dorm/**`：寝室/床位目录、资产套装和资产明细、入住记录查询、换寝、退宿、批量退宿与学籍异动。批量清除不得设计为删除接口，而是 `POST /api/admin/dorm/checkins/batch-checkout`，请求必须包含记录范围与 `reason`。
- 所有拟定管理员写接口在实现时均使用现有管理员认证和 `@OperateLog`，并在操作摘要中保留原因；不得新增独立模块日志、公开二维码图片 URL 或 OSS 上传流程。
## 宿舍资产管理契约

资产套装编号为 `buildingCode + roomNo + '-' + bedNo`。二维码二进制响应只编码 `QH-DORM-V1:{qrToken}`，其中 `qrToken` 为唯一 64 位十六进制随机值；响应不暴露令牌，也不包含任何学生个人信息。重复请求同一床位的资产套装接口返回既有套装及五项资产（床、床板、书桌、衣柜、凳子）。

### 管理员宿舍资源维护（2026-07-18）

| 接口 | 行为边界 |
|---|---|
| `GET /api/admin/dorm/buildings`、`/buildings/available` | 仅输出真实 `building_type=宿舍楼`；停用宿舍楼不进入可新增寝室选择。 |
| `PUT /rooms/{id}`、`PUT /rooms/{id}/status`、`DELETE /rooms/{id}` | 可维护 `roomNo/floor/capacity/status`；当前入住禁停用/删除，有床位、资产或历史时禁物理删除，有资产套装时禁改寝室号。 |
| `PUT /beds/{id}`、`PUT /beds/{id}/status`、`DELETE /beds/{id}` | 可维护 `bedNo/status/remark`；当前入住禁停用/删除，有套装或历史时禁物理删除，有套装时禁改床位号。 |
| `POST/GET /beds/{id}/asset-set`、`PUT /assets/{id}` | 套装按床位幂等生成固定五项资产；详情返回套装/二维码/当前入住状态和资产真实名称、类型、状态、备注。资产状态仅限 `NORMAL/REPAIR/SCRAPPED`。 |
## 管理员认证契约（2026-07-17）

### POST /api/admin/auth/login

请求：`{"username":"管理员账号","password":"管理员密码"}`。成功：`{"code":200,"message":"success","data":{"token":"随机令牌","admin":{"id":1,"username":"...","displayName":"..."}}}`。密码错误、停用或不存在均返回统一业务 `code=401`，不返回密码、哈希或 Token。

### GET /api/admin/auth/me 与 POST /api/admin/auth/logout

两接口均使用 `Authorization: Bearer <adminToken>`。`me` 返回安全管理员资料；`logout` 删除当前 Redis 会话并返回成功。任何普通用户、过期、随机或缺失 Token 访问 `/api/admin/**` 返回 HTTP 401。管理员 Token 使用 `qh:admin:token:{token}`，普通用户 Token 使用既有 `qh:login:token:{token}`，两者不互认。

## 管理端店铺维护与封面契约（2026-07-17，已完成并验证）

## 管理端商品维护与主图契约（2026-07-17，已完成并验证）

- `/api/admin/goods` 提供分页列表、详情、新增、编辑、状态、库存与主图接口；所有写请求均从管理员会话取得操作人，客户端不得传入 `adminId`。
- `GET /api/admin/goods` 查询参数为 `shopId`、`shopCategoryId`、`goodsCategoryId`、`saleStatus`、`keyword`、`page`、`size`：`shopCategoryId` 通过 `qh_shop.category_id`（店铺类型）过滤，`goodsCategoryId` 通过 `qh_goods.category_id`（店内商品分类）过滤，两个条件可以并用且不得合并为 `categoryId`。
- `AdminGoodsSaveRequest` 包含可空 `categoryId`；服务端校验其所属店铺等于商品 `shopId`，否则拒绝保存。`price >= 0`、`stock >= 0`，店铺必须存在且启用。
- 公开 `/api/goods` 与 `/api/goods/{id}` 仅返回已上架且所属店铺启用的商品；店铺详情继续返回其已上架商品，库存为 0 的项目保留为售罄展示但购物车现有校验拒绝加购。
- 主图替换使用同一 Bucket 的 `goods/` 前缀；自动化测试 Mock `AliyunOSSOperator`，不得连接真实 OSS。当前商品查询没有 Redis 缓存，数据库为实时来源，因此不为本轮新增缓存体系。

- 所有 `/api/admin/shops/**` 接口仅由 `AdminContext` 确定操作人；客户端不得传入 `adminId`。查询支持名称、分类、`status` 和分页筛选；新增/编辑只维护 `qh_shop` 已有的 `name`、`categoryId`、`address`、`phone`、`score`、`status`、`isFeatured`、`sortOrder` 和单张 `coverImage`。
- `categoryId` 必须指向启用分类，名称、地址、电话、评分 0 至 5、状态 0/1、推荐 0/1、排序非负均由服务端校验。不存在简介、营业时间或独立营业状态字段，故接口不伪造这些字段。
- `POST /api/admin/shops/{id}/cover` 仅收字段 `file`。后端校验 jpg/jpeg/png/webp、空文件、3MB 上限、扩展名、Content-Type、真实图像结构及 1200×800 最大尺寸；自动化测试 Mock 唯一 `AliyunOSSOperator`，不得真实上传。
- 写操作标注现有 `@OperateLog` 并落入唯一 `qh_operate_log`。每次新增、资料修改、状态调整或封面替换均只删除 `qh:shop:detail:{id}` 与 `qh:shop:null:{id}`；不存在列表缓存时不扩大清理范围，也不清空 Redis 数据库。

## 商品店内分类接口契约（2026-07-17）

| 方法 | 路径 | 权限 | 约定 |
|---|---|---|---|
| GET | `/api/admin/shops/{shopId}/goods-categories` | 管理员 | 返回该店铺的店内分类；支持状态筛选，按 `sortOrder`、`id` 排序。 |
| POST | `/api/admin/shops/{shopId}/goods-categories` | 管理员 | 创建 `{name, sortOrder, status}`；服务端以路径店铺 ID 写入。 |
| PUT | `/api/admin/shops/{shopId}/goods-categories/{categoryId}` | 管理员 | 修改名称、排序和状态；分类必须属于路径店铺。 |
| GET | `/api/shops/{shopId}/goods-categories` | 公开 | 仅返回店铺启用且分类启用的导航项。 |

- `AdminGoodsSaveRequest.categoryId` 为可空店内商品分类 ID；服务端读取该分类并验证其 `shopId` 与请求商品的 `shopId` 相同。更换商品店铺时，若未提供新店铺有效分类 ID，则将 `categoryId` 置空；不得保留旧店铺分类。
- `GET /api/admin/goods` 使用明确的 `shopCategoryId`（店铺类型）与 `goodsCategoryId`（店内商品分类）查询参数。管理端仅在已选择具体店铺时加载并启用商品分类筛选，避免跨店同名分类混用。
- 商品分类只能用服务端已存在的 ID 创建、修改或关联；禁止接受分类名称并自动创建、匹配或伪造分类。购物车 `/api/cart` 的请求字段和现有商品 ID 依赖保持不变。

## 学生资料与宿舍扫码入住（已实现）

- 所有 `/api/student/**` 接口使用既有普通用户 Token 与 `UserContext`。资料实体映射 `college_name -> collegeName`、`major_name -> majorName`、`student_status -> studentStatus`、`current_flag -> currentFlag`，不创建字段别名。
- 二维码只接受精确版本前缀及 64 位十六进制令牌；解析响应不返回令牌或其他学生资料。确认入住在一个事务中校验当前学籍、目录、二维码、资产套装和两项有效入住唯一约束，写入快照并将套装置为 `OCCUPIED`。

## 2026-07-19 学籍异动、毕业与批量宿舍接口边界（仅设计）

- 后续管理员学生资料查询、学籍异动、批量毕业、批量退宿、资产释放和二维码操作均受现有 `/api/admin/**` 与 `AdminContext` 保护；普通用户 Token 一律拒绝。当前没有角色表，不伪造 RBAC。
- 学籍异动请求必须包含异动类型、原因和生效时间；转专业只接受 `collegeName`、`majorName`、`className`，休学必须显式给出 `KEEP_DORM` 或 `CHECKOUT_DORM`。学生端不得提交这些字段。
- 批量毕业采用“筛选预览 → 脱敏受影响名单与当前入住数量 → 二次确认 → 受上限单事务执行”的接口形态；确认时重检范围和版本，任一冲突整批回滚并要求重新预览。不得暴露完整学号、手机号、内部 ID 或二维码令牌。
- 批量退宿只关闭当前入住并释放其绑定套装；批量资产释放只处理无当前入住的套装；二维码停用/启用只改 `qrStatus`，轮换只允许空闲无当前入住套装并更新令牌/轮换时间，不修改 `assetSetNo`。任何“清除二维码入住信息”均不得成为物理删除接口。
- 所有写接口复用 `@OperateLog` 与唯一 `qh_operate_log`；日志记录操作类型、范围摘要和结果计数，且不记录完整姓名、学号、手机号、二维码令牌或原文。
# 店内商品分类与商店主页接口增补（2026-07-17）

| 接口 | 权限 | 契约 |
|---|---|---|
| `GET /api/admin/goods/categories` | 管理员 | 必传 `shopId`，可传 `status`；返回该店铺分类、排序、状态、商品数量。 |
| `POST /api/admin/goods/categories` | 管理员 | `{shopId,name,sortOrder}`；店铺必须存在且可管理，同店名称唯一。 |
| `PUT /api/admin/goods/categories/{id}` | 管理员 | `{name,sortOrder}`；不改变分类所属店铺。 |
| `PUT /api/admin/goods/categories/{id}/status` | 管理员 | `{status:"0"|"1"}`；停用不删除历史商品。 |
| `GET /api/shops/{id}/goods-categories` | 公开 | 仅启用的店内商品分类，按 `sortOrder,id` 排序。 |

`/api/admin/goods` 的 `shopCategoryId` 是 `qh_category` 店铺类型，`goodsCategoryId` 是 `qh_goods_category` 店内商品分类；`categoryId` 仅用于商品新增、编辑的可选关联。普通用户 Token 访问所有 `/api/admin/**` 仍返回 401。

## 订单、普通优惠券与秒杀券：后续接口边界（仅设计，2026-07-17）

以下接口尚未实现，本轮不创建 Controller。普通下单与秒杀受理必须使用不同 Controller 方法和 DTO，不能按一个参数分支混用。

| 接口 | 类型 | 契约要点 |
|---|---|---|
| `POST /api/orders` | 普通订单 | 仅接收购物车项 ID、地址 ID、可选用户券 ID 与备注；服务端重读商品、价格、库存和券规则，拒绝跨店。 |
| `GET /api/orders`、`GET /api/orders/{id}` | 普通订单 | 仅返回当前用户订单；读取不可变商品和地址快照。 |
| `POST /api/orders/{id}/cancel` | 普通订单 | 仅允许合法状态迁移；待支付取消可按既定规则退普通券并回补库存。 |
| `POST /api/coupons/{id}/claim`、`GET /api/coupons/mine` | 普通优惠券 | 数据库事务处理库存、范围、时间和一人一券；不信任客户端优惠金额。 |
| `POST /api/seckill/coupons/{activityId}/orders` | 秒杀受理 | 只调用 Lua 并返回已受理结果；不接收购物车、价格或库存结论，不同步创建数据库订单。 |

普通订单创建、取消、普通券领取/核销和后续秒杀活动发布均继续使用现有认证上下文及唯一 `qh_operate_log`；Redis Stream 消费者不通过 HTTP 操作日志伪造请求，失败仅记录脱敏业务状态和死信消息。
# 普通订单创建（2026-07-18）

`POST /api/orders` 仅接收 `cartItemIds`、`addressId` 和可选 `remark`。服务端从登录上下文取得用户，重读购物车、商品、店铺和地址；不接受客户端提交的商品、价格、金额、库存或状态。响应使用 `totalAmount` 对应数据库 `total_amount`，并返回 `discountAmount`、`deliveryFee`、`payAmount`，同一响应不返回重复的 `goodsAmount`。

## 管理员入住操作契约（2026-07-18，已验证）

| 接口 | 认证与输入 | 行为与响应边界 |
|---|---|---|
| `GET /api/admin/dorm/checkins` | 仅管理员 Token；分页和筛选参数 | 返回脱敏学号；不返回 `passwordHash`、`qrToken`、`activeFlag`、资产内部 ID。 |
| `GET /api/admin/dorm/checkins/{id}` | 仅管理员 Token | 返回学生、床位和资产套装关联信息；联系电话脱敏。 |
| `POST /api/admin/dorm/checkins/{id}/checkout` | 仅 `reason`，1-255 字符 | 仅关闭当前入住，保留历史，资产恢复 `AVAILABLE`；更新失败整体回滚。 |
| `POST /api/admin/dorm/checkins/{id}/transfer` | 仅 `targetBedId` 与 `reason` | 仅当前入住可换寝；目标必须启用、空闲且有 `AVAILABLE` 资产；冲突返回统一业务错误。 |

补充响应边界：列表和详情不返回 `previousCheckinId`；`reason` 不写入可读操作日志摘要。专项测试与完整回归分别验证为 7/0/0/0 和 68/0/0/0。

## 管理员宿舍楼按校区管理（2026-07-20）

| 接口 | 请求 | 约束与响应 |
|---|---|---|
| `GET /api/admin/dorm/buildings` | `campusId` 必填；可选 `status`、`keyword`、分页 | 仅返回真实“宿舍楼”，含校区名和寝室/床位/当前入住统计；未登录或普通用户为 401。 |
| `POST /api/admin/dorm/buildings` | `campusId, buildingCode, buildingName, area, remark, status` | 服务端固定类型“宿舍楼”；不接收 ID、时间、管理员号或客户端类型。 |
| `GET/PUT /api/admin/dorm/buildings/{id}` | PUT: `buildingCode, buildingName, area, remark, status` | 不可改变校区/类型；有下级资源或入住历史时禁止改编码。 |
| `PUT /api/admin/dorm/buildings/{id}/status` | `status` | 当前入住存在时拒绝停用；停用后不进入新增寝室的可用下拉。 |
| `DELETE /api/admin/dorm/buildings/{id}` | 无 | 仅无关联资源和入住历史时物理删除；不级联删除。 |
# 2026-07-20 个人中心实名展示契约

- `GET /api/user/me` 返回安全 `UserDTO`，其中 `realName`、`studentNo`、`hasStudentProfile` 来自当前登录用户唯一的 `current_flag=1` 学生资料；接口每次读取时组合当前资料，不能仅信任旧 Redis 会话，确保管理员改名后用户刷新可见最新结果。
- `phoneMasked` 表示登录手机号的脱敏展示；`nickname` 表示账号昵称；`realName` 表示实名学籍资料；`studentNo` 表示学号。可空 `username` 不是实名资料，不能参与页面显示名回退。
- `UserDTO` 不公开学生资料内部 ID、`currentFlag`、`activeFlag`、`passwordHash`、二维码令牌或 Token。`PUT /api/user/profile` 仍只接受昵称和头像，普通学生不得通过个人中心修改实名或其他受保护学籍字段。
