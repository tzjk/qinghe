# 后端接口说明（M2）

## 2026-07-23 订单状态模型与候选迁移

- 当前真实订单接口仍只有 `POST /api/orders`。它只接收 `cartItemIds`、`addressId`、可选 `remark`，服务端写入 `PENDING_PAY`；本轮未新增支付、取消、管理员订单或订单页面接口。
- `OrderStatus` 统一定义 `PENDING_PAY`（待支付）、`PAID`（已支付）、`ACCEPTED`（已接单）、`DELIVERING`（配送中）、`COMPLETED`（已完成）、`CANCELLED`（已取消）。合法流转为 `PENDING_PAY -> PAID -> ACCEPTED -> DELIVERING -> COMPLETED`，以及 `PENDING_PAY -> CANCELLED`；不得使用 `PENDING_PAYMENT`。
- `pay_time`、`accepted_time`、`delivery_time`、`pay_expire_time` 仅存在于候选人工迁移，尚未核验真实 MySQL，因而不在当前请求或响应中出现。

## 2026-07-20 学生首次建档多校区与 Redis 测试分层

- `PUT /api/student/profile` 首次建档请求增加 `campusId`。服务端仅以 `GET /api/campuses` 返回的真实启用校区（`qh_campus.status=1`）作为可选值；不存在、停用或缺失的编号均拒绝。请求不接收也不信任 `campusName`。
- 首次成功响应和后续 `GET/PUT /api/student/profile` 均返回安全的 `campusId`、`campusName`、实名资料、学号、学院、专业、班级、联系电话和学籍状态；不返回内部资料 ID、`currentFlag`、密码、二维码令牌或入住活动标记。
- 已建档学生的 `PUT` 仍只保存联系电话，客户端提交的 `campusId` 不会变更归属校区。扫码入住继续使用既有“学生资料校区必须与二维码宿舍校区一致”校验，本轮未改入住、退宿或换寝事务。
- Redis 测试分层：纯单元测试可用 Mockito Mock `StringRedisTemplate` 依赖，并必须明确标为单元测试；`AdminAuthenticationIntegrationTest`、用户 Token/验证码状态及学生宿舍会话测试必须连接真实 Redis。Codex 沙箱无法访问局域网 Redis 时仅标记为待本机验证，不调整 Redis 地址、密码、登录逻辑或以 Mock 结果代替真实集成测试。

## 2026-07-19 管理员学籍异动与批量宿舍操作

- 所有接口均在既有管理员会话保护下，统一前缀为 `/api/admin/dorm`，服务层从 `AdminContext` 获取办理管理员，不接收客户端管理员编号。普通用户 Token 不能访问。
- 学生管理：`GET /students` 支持姓名、学号、学院、专业、班级、当前学籍、当前住宿状态的分页筛选；`GET /students/{userId}` 返回当前资料、脱敏联系电话、当前宿舍摘要和资料版本；`GET /students/{userId}/history` 返回历史版本。响应不返回二维码令牌、密码、活动标记或完整联系电话。
- 学籍写操作：`POST /students/{userId}/transfer-major`、`/suspend`、`/drop`、`/graduate`、`/reinstate`，请求均包含 `reason`，转专业另含新的学院/专业/班级，休学可选 `checkoutDorm=true`。新写当前状态只可能为 `ENROLLED`、`SUSPENDED`、`DROPPED`、`GRADUATED`；转专业、复学完成后都为 `ENROLLED`。
- 批量毕业/退宿：先调用 `POST /batch/graduation/preview` 或 `/batch/checkout/preview`（`ids` 最多 100），读取统计和 `previewToken`；再向对应 `/batch/graduation` 或 `/batch/checkout` 提交 `ids`、`reason`、`previewToken`。服务端再次校验范围和状态，任一失败则整批回滚。
- 批量资产/二维码：`/batch/assets/release`、`/batch/qrcode/disable`、`/batch/qrcode/rotate` 各有同名 `/preview` 端点。释放仅允许无当前入住且仍为 `OCCUPIED` 的套装；停用只改 `qr_status`；轮换只允许 `AVAILABLE` 且无当前入住的套装，原 `qr_token` 由新随机令牌替换而不是清空，`asset_set_no` 不变。

## 2026-07-18 管理员校区与楼栋管理

- 校区是既有目录，只读接口为 `GET /api/admin/dorm/campuses`，仅返回启用校区；楼栋管理继续位于唯一的 `/api/admin/dorm/**` 命名空间并受管理员会话保护。
- 楼栋接口：`GET /buildings`（校区、状态、关键字分页筛选）、`GET /buildings/available?campusId=`、`GET /buildings/{id}`、`POST /buildings`、`PUT /buildings/{id}`、`PUT /buildings/{id}/status`。响应含校区名称、编码、名称、类型、区域、备注、状态、寝室数、当前入住数和编码是否可编辑。
- 新增与编辑使用独立 DTO；服务端从 `AdminContext` 获取操作者，所有写操作只复用 `qh_operate_log`。`area` 是校园区域，`remark` 是管理备注，二者分别保存。
- 楼栋有寝室、床位或资产套装后，任何修改接口均拒绝改变 `buildingCode`，不重写资产套装编号、不轮换二维码 Token、不改入住历史。停用前若存在 `active_flag=1` 入住则拒绝；停用目录不进入可选楼栋。
- `POST /api/admin/dorm/rooms` 只信任 `buildingId`：后端读取楼栋真实 `campus_id` 写入寝室，忽略兼容请求中的 `campusId`，并校验校区/楼栋启用和同楼栋寝室号唯一。

## 2026-07-18 验证收口：订单创建与管理员入住

- `POST /api/orders` 的专项测试 5/0/0/0 已覆盖鉴权/归属、单店与目录校验、实时金额与快照、条件扣库存并发、事务回滚、精确购物车清理和伪造金额忽略。
- 管理员入住列表/详情与学生宿舍接口均不返回 `previousCheckinId`；它仅为换寝内部历史关联。
- 完整 Maven 回归为 68/0/0/0，后端 JAR 与前端生产构建均成功。

## 2026-07-17 学生资料与宿舍扫码入住测试收口

- `StudentProfileVO` 是普通用户唯一的学生资料响应模型；不包含 `currentFlag`、`passwordHash`、`activeFlag`、二维码令牌或内部数据库 ID。`currentFlag` 只在服务端用于检索当前学籍版本。
- 二维码解析仅返回校区、楼栋、寝室、床位、资产套装和可入住状态；正确格式为 `QH-DORM-V1:{64位十六进制令牌}`。停用二维码或不可用套装被拒绝，已占用套装只返回 `available=false`，不返回任何入住学生信息或完整令牌。
- 确认入住为事务：资料/学籍/床位/套装校验通过后写入快照、套装置为 `OCCUPIED`，但不改变床位目录 `status`；有效入住唯一索引冲突统一转换为 409 业务结果。

## 个人资料与 OSS 用户头像（2026-07-17）

| 方法 | 路径 | 登录 | 请求/响应 | 说明 |
|---|---|---|---|---|
| GET | `/api/user/me` | 是 | 返回安全 `UserDTO` | 返回当前 Token 对应的 `id`、`username`、昵称、头像地址、脱敏手机号、资料完成和密码状态；不返回 `passwordHash`。 |
| PUT | `/api/user/profile` | 是 | JSON `nickname`、`avatarUrl` | 仅使用 `UserContext` 的当前用户更新昵称与既有头像地址，并同步当前 Redis Token Hash；用户名、手机号不接受修改。 |
| POST | `/api/user/avatar` | 是 | `multipart/form-data`，字段 `file`；返回安全 `UserDTO` | 仅上传当前用户头像。允许 jpg/jpeg/png/webp，文件非空、不超过 2MB，并同时校验扩展名、Content-Type 与图片文件特征。 |

- 头像 Object Key 固定为 `avatars/{userId}/{yyyy/MM}/{uuid}.webp`。AccessKey 仅读取环境变量 `OSS_ACCESS_KEY_ID`、`OSS_ACCESS_KEY_SECRET`；服务地址、Bucket、区域使用受控 `aliyun.oss` 配置，不在接口响应或日志中暴露密钥。
- 上传成功后依次更新 `qh_user.avatar_url`、同步当前 `qh:login:token:{token}` Hash、返回安全用户资料；仅当旧地址属于当前 Bucket 且路径归属当前用户的 `avatars/{userId}/` 时，才尽力删除旧对象。默认头像、外部 URL、其他用户对象一律不删除。
- 数据库更新失败时删除本次新对象；旧对象清理失败仅记录异常类型，不影响新头像保存。上传文件内容、完整 Token、密码、验证码与完整手机号不进入操作日志；资料修改与头像上传复用现有唯一 `qh_operate_log` 链路。

## 验证码免注册登录、首次资料完善与可选密码登录（2026-07-17）

| 方法 | 路径 | 登录 | 请求 | 说明 |
|---|---|---|---|---|
| POST | `/api/user/login` | 否 | `phone`、`code` | 手机号验证码登录。新手机号校验成功后自动创建基础用户（随机昵称、`profileCompleted=false`、无密码），再签发既有 Redis Token；响应含 `token`、`newUser`、`profileCompleted`、`hasPassword` 和安全 `user`。 |
| POST | `/api/user/login/password` | 否 | `phone`、`password` | 手机号密码登录。用户不存在、未设置密码或密码不匹配均返回“手机号或密码错误”；成功后签发与验证码登录完全一致的 Redis Token，并返回相同状态字段。 |
| POST | `/api/user/profile/complete` | 是 | `nickname`、`receiverName`、`receiverPhone`、`campusId`、`buildingId`、`roomNo` 或 `deliveryPoint`；可选 `avatarUrl`、`floor`、`detail`、`label`、`remark`、`password` | 首次资料完善：在一个事务内新增默认校园地址、更新昵称和 `profileCompleted=true`；填写密码时才以 BCrypt 写入 `password_hash`。返回同步后的安全 `UserDTO`。 |

- `profileCompleted` 是服务端持久化状态：`false` 的登录用户须进入首次资料完善页；`hasPassword` 仅表示是否已设置密码。二者同时写入 `UserDTO` 和 `qh:login:token:{token}` Redis Hash，`/api/user/me`、Token 刷新和前端会话恢复均从同一状态取得，不以 localStorage 推断。
- 资料完善成功后同步当前 Token Hash；Token 仍由既有双拦截器读取、续期和清理。验证码登录成功后仍删除验证码 Key。密码或密码哈希不写入响应、Redis 会话、日志或前端本地存储。
- 密码为可选项；省略时仍能完成首次资料，后续继续使用验证码登录。填写时须为 8 至 32 位且同时包含字母和数字，后端使用 `BCryptPasswordEncoder` 存储。
- 最终回归在 `Q:\backend` 完成：`mvn clean test` 为 25 项通过（0 failure、0 error、0 skipped），`mvn clean package -DskipTests` 成功生成 JAR。前端真实路径构建和浏览器人工验收见页面设计与交接记录。

统一响应为 `{ "code": 200, "message": "success", "data": {} }`。受保护接口必须携带 `Authorization: Bearer <token>`。

| 方法 | 路径 | 登录 | 请求 | 说明 |
|---|---|---|---|---|
| POST | `/api/user/code` | 否 | JSON `{ "phone": "13900009991" }` | 每次生成随机 6 位验证码，以 String 覆盖写入 Redis database 2 的 `qh:login:code:{phone}`，TTL 2 分钟；仅 `dev/local` profile 输出脱敏手机号的 `[LOCAL DEV ONLY]` 日志。 |
| POST | `/api/user/login` | 否 | `{ "phone": "13900009991", "code": "123456" }` | 返回 `token` 和含编号、昵称、头像、脱敏手机号的 `user`。 |
| GET | `/api/user/me` | 是 | 无 | 从 Token 会话读取当前用户。 |
| PUT | `/api/user/profile` | 是 | `{ "nickname": "青禾用户", "avatarUrl": "" }` | 仅更新昵称、头像，并同步当前 Redis 会话。 |
| POST | `/api/user/logout` | 是 | 无 | 删除当前 `qh:login:token:{token}`。 |
| GET | `/api/categories` | 否 | 无 | 返回启用状态的分类，按 `sortOrder` 升序；每项含 `id`、`name`、`iconUrl`、`sortOrder`。 |
| GET | `/api/home/summary` | 否 | 无 | 固定返回 `banners`、`categories`、`recommendedShops`、`hotGoods`、`availableCoupons`、`featuredBlogs` 六个数据区域。 |
| GET | `/api/shops` | 否 | `page`、`size`、可选 `categoryId`、`keyword`、`sort` | 返回启用商铺的分页数据；`sort` 支持默认排序、`score` 和 `latest`。 |
| GET | `/api/shops/{id}` | 否 | 无 | 返回启用商铺详情。 |
| GET | `/api/shops/{id}/goods` | 否 | `page`、`size` | 返回该商铺已上架商品的分页数据。 |
| GET | `/api/shops/{id}/comments` | 否 | `page`、`size` | 返回该商铺已启用评论的分页数据。 |
| GET | `/api/goods` | 否 | `page`、`size`、可选 `shopId` | 返回上架商品分页数据，按销量和编号倒序。 |
| GET | `/api/goods/{id}` | 否 | 无 | 返回上架商品详情；不存在或下架返回业务 404。 |
| GET | `/api/campuses` | 否 | 无 | 返回全部启用校区，按 `sortOrder`、ID 升序。 |
| GET | `/api/campuses/{campusId}/buildings` | 否 | 无 | 返回该校区的启用楼栋，含区域、类型与名称。 |
| GET | `/api/addresses` | 是 | 无 | 返回当前用户地址，默认地址优先；含校园名称、楼栋快照、格式化地址和脱敏号码字段。 |
| GET | `/api/addresses/{id}` | 是 | 无 | 返回当前用户的单个地址；不可读取他人地址。 |
| POST | `/api/addresses` | 是 | `receiverName`、`receiverPhone`、`campusId`、`buildingId`、`floor`、`roomNo`/`deliveryPoint`、可选 `detail`/`label`/`remark`/`isDefault` | 创建 `CAMPUS` 地址；首个地址自动设为默认。 |
| PUT | `/api/addresses/{id}` | 是 | 同新增校园字段 | 仅更新当前用户自己的地址；编辑 `HISTORICAL` 地址会转换为 `CAMPUS`。 |
| DELETE | `/api/addresses/{id}` | 是 | 无 | 仅删除当前用户地址；删除默认地址后自动补选。 |
| PUT | `/api/addresses/{id}/default` | 是 | 无 | 在事务中取消其他默认地址并设置当前地址。 |
| GET | `/api/cart` | 是 | 无 | 返回按商铺分组的 `CartSummaryVO`，空购物车返回空集合和数值 0。 |
| POST | `/api/cart` | 是 | `goodsId`、`quantity`（1 至 99） | 添加商品；同一用户同一商品原子累加，校验当前商品、商铺状态与库存。 |
| PUT | `/api/cart/{id}` | 是 | `quantity`（1 至 99） | 修改当前用户购物车数量，校验当前库存。 |
| PUT | `/api/cart/{id}/selected` | 是 | `selected` | 修改当前用户购物车选中状态。 |
| DELETE | `/api/cart/{id}` | 是 | 无 | 仅删除当前用户的指定购物车项。 |
| DELETE | `/api/cart` | 是 | 无 | 仅清空当前用户购物车。 |

商铺详情采用 Cache Aside：正常缓存使用 `qh:shop:detail:{shopId}` 并增加随机过期时间；不存在商铺使用 `qh:shop:null:{shopId}` 短期空值缓存；重建互斥锁使用 `qh:lock:shop:{shopId}`，限定三次重试。Redis 异常时降级查询 MySQL，不执行 Redis 清库操作。

错误示例：验证码不存在、过期或错误返回业务 `code=400` 和清晰 `message`；缺少、错误或过期 Bearer Token 返回 HTTP 401。接口不返回数据库密码、Redis 密码或内部连接信息。

验证码登录只比较同手机号 Redis Key 中尚未过期的实际验证码；登录成功后立即删除验证码 Key，随后以 `qh:login:token:{token}` 的 Redis Hash 写入安全 `UserDTO` 字段，TTL 30 分钟。生产环境不在响应或日志中输出验证码；开发日志格式为 `[LOCAL DEV ONLY] phone=138****0001, code=xxxxxx, ttlMinutes=2`。

请求链使用两层拦截：`RefreshTokenInterceptor`（order 0）从 `Authorization: Bearer <token>` 读取 Redis Hash、写入 `UserContext`、刷新 Token TTL 并在请求结束清理；`LoginInterceptor`（order 1）只检查 `UserContext`，无用户返回 HTTP 401，不再重复查询 Redis。

验证码接口的浏览器调用路径固定为 `/api/user/code`，Vite 开发代理目标为 `http://localhost:8090` 且不配置 rewrite，因此 8090 实际收到 `POST /api/user/code`。此前 Vite 无代理配置，导致请求留在 5174 并返回 404；当前代码与构建已通过，状态为 `awaiting_manual_verification`，须由用户重启开发服务后在外部浏览器确认。

M3A 地址接口已通过真实集成测试：覆盖未登录访问、首地址默认、默认切换、收件人字段映射、当前用户隔离、越权拒绝、参数校验、删除默认地址补选和请求后上下文清理。测试数据使用 `M3A_ADDR_TEST_` 标识并在结束后精确清理。

## 校园地址（已实施）

- 新增地址只能提交校园字段；DTO 忽略客户端 `userId`、`addressType` 和旧省/市/区字段，服务端仅从 `UserContext` 取得用户 ID，并固定保存 `addressType=CAMPUS`。
- `campusId`、`buildingId`、收件人和手机号必填；校区/楼栋必须启用，楼栋必须属于校区；房间号和配送点至少填写一项。保存时使用目录数据写入 `area`、`buildingType`、`buildingName` 快照。
- 响应含 `campusName`、`area`、`buildingType`、`buildingName`、`floor`、`roomNo`、`deliveryPoint`、`detail`、`label`、`remark`、`addressType`、`formattedAddress` 和 `maskedReceiverPhone`。`receiverPhone` 保留给受保护的编辑表单，页面展示使用脱敏字段。
- `HISTORICAL` 地址仍可查看、删除、设默认；编辑时转换为 `CAMPUS`，但旧 `province/city/district/detailAddress` 历史字段不删除、不覆盖。订单仍未实现。

M3A 购物车接口已通过真实集成测试：返回项读取当前商品名称、封面、价格、库存和销售状态；金额使用 `BigDecimal` 计算，选中金额只统计已选项。测试数据使用 `M3A_CART_TEST_` 标识并精确清理。

## 全局异常响应

异常响应仍使用 `{ "code": 业务码, "message": "友好提示", "data": null }`，不改变前端既有字段。业务异常沿用服务层定义的业务码和提示；DTO、查询参数和 JSON 读取异常使用 `code=400` 并返回字段级或明确提示。`LoginInterceptor` 不经全局异常处理器，未登录请求仍返回 HTTP 401 和原有响应结构。

| 类型 | code / HTTP 状态 | 前端提示 |
|---|---|---|
| `BusinessException` | 原业务码 / 保持既有响应行为 | 服务层定义的业务提示 |
| DTO 或绑定校验 | 400 / 保持既有响应行为 | 如“手机号格式不正确”“数量数值范围不正确” |
| 缺少参数、类型错误、JSON 无法读取 | 400 / 保持既有响应行为 | 明确参数名或“请求体格式错误，请检查 JSON 数据” |
| `DuplicateKeyException` | 409 / HTTP 409 | 按唯一约束名映射；未知时“数据已存在，请勿重复提交” |
| `DataIntegrityViolationException` | 409 / HTTP 409 | “数据约束不满足，请检查后重试” |
| 不支持的请求方法 | 405 / HTTP 405 | “请求方法不支持” |
| Redis 或数据库连接异常 | 503 / HTTP 503 | “服务暂时不可用，请稍后重试” |
| 未知异常 | 500 / HTTP 500 | “系统繁忙，请稍后重试” |

重复键只以基线 SQL 的唯一约束名白名单识别：`uk_qh_user_phone`（该手机号已注册）、`uk_qh_user_username`（用户名已存在）、`uk_qh_cart_user_goods`（购物车中已存在该商品）、`uk_qh_order_no`（订单号已存在，请稍后重试），并覆盖管理员账号、分类、用户优惠券、订单评价、点赞与收藏的对应唯一约束。不会向前端返回 SQL、表名、驱动异常文本、Token、验证码、密码或完整手机号。

## 账号注册

| 方法 | 路径 | 登录 | 请求 | 响应 |
|---|---|---|---|---|
| POST | `/api/auth/register` | 否 | `username`、`phone`、`code`、`password`、`confirmPassword` | 成功返回 `code=200`、`data=null`；不返回 Token、密码、密码哈希、验证码或完整 User。 |

注册复用现有唯一 `UserController`，在同一 Controller 中映射 `/api/auth/register`，不新建第二个 AuthController；验证码、登录和个人资料接口仍保持 `/api/user/**`。用户名为 4 至 20 位字母、数字或下划线；手机号沿用现有规则；验证码为 6 位数字；密码为 8 至 32 位且同时含字母和数字；确认密码必须一致。请求 DTO 不接收 `userId`、`role`、`status`、`passwordHash`。

注册与现有登录共用 `qh:login:code:{phone}`（TTL 2 分钟）验证码 Key：不存在返回“验证码已过期或尚未获取”，错误返回“验证码错误”；注册成功后删除，所有失败分支保留 Key 且不创建用户。新手机号创建用户；旧手机号用户的 `username/password_hash` 同时为空时原地绑定账号；已绑定手机号返回“该手机号已注册”。密码使用 `spring-security-crypto` 的 BCrypt 保存到 `password_hash`，不引入账号密码登录或完整 Spring Security 登录体系。

## 操作日志

`qh_operate_log` 已由用户手工迁移并通过只读结构复核。系统仅对下列现有关键写接口启用 `@OperateLog`；查询、验证码、登录、Token 刷新及购物车选中状态修改均不记录。

| 方法 | 路径 | 模块 | 操作 |
|---|---|---|---|
| POST | `/api/addresses` | 地址管理 | 新增地址 |
| PUT | `/api/addresses/{id}` | 地址管理 | 修改地址 |
| DELETE | `/api/addresses/{id}` | 地址管理 | 删除地址 |
| PUT | `/api/addresses/{id}/default` | 地址管理 | 设为默认地址 |
| POST | `/api/cart` | 购物车 | 新增购物车项 |
| PUT | `/api/cart/{id}` | 购物车 | 修改购买数量 |
| DELETE | `/api/cart/{id}` | 购物车 | 删除购物车项 |
| DELETE | `/api/cart` | 购物车 | 清空购物车 |

切面从 `UserContext.getUserId()` 获取用户 ID，记录模块、操作、Controller 类/方法、路径、HTTP 方法、IP、请求/返回摘要、成功状态、异常类型、耗时与操作时间。成功和异常场景均写日志；异常仍按原路径抛给 `GlobalExceptionHandler`。日志写入使用独立 `REQUIRES_NEW` 事务，保存失败只记录服务端 error，不影响原业务。

请求和返回 JSON 递归脱敏并限制为 2000 字符：不记录 `password`、`confirmPassword`、`passwordHash`、`code`、`Authorization`、Token、Redis/数据库密码、文件二进制、上传文件、Servlet 请求/响应或流对象；手机号只保留前三后四位。
## 宿舍基础档案与资产二维码管理

> **学生端入住接口状态（2026-07-17）：** 真实 `qh_student_profile` 缺少 `real_name`、`contact_phone`，本轮门禁已阻断；`GET /api/student/profile`、`PUT /api/student/profile`、`POST /api/student/dorm/qr/resolve`、`POST /api/student/dorm/check-in`、`GET /api/student/dorm/me` 未实现且不可调用。

管理员先通过 `POST /api/admin/auth/login` 获得独立管理员 Token。`/api/admin/dorm/**` 只接受该会话：可维护 `PUT /buildings/{id}/code`、新增/分页查询寝室、查看/新增床位、`POST /beds/{id}/asset-set` 幂等生成套装、查看明细，并下载单个 PNG 或 `GET /asset-sets/qrcode.zip?ids=...` 批量 ZIP。所有宿舍写操作均标注 `@OperateLog`。

## 2026-07-18 管理员宿舍资源维护

- `GET /api/admin/dorm/buildings` 与 `/buildings/available` 仅返回真实 `building_type=宿舍楼` 的楼栋；非宿舍楼不出现在寝室新增选择器。
- 新增 `PUT/DELETE /rooms/{roomId}`、`PUT /rooms/{roomId}/status`、`PUT/DELETE /beds/{bedId}`、`PUT /beds/{bedId}/status` 与 `PUT /assets/{assetId}`。所有接口均只接受管理员 Token，写操作复用 `@OperateLog`/`qh_operate_log`。
- 寝室、床位删除只在无下级资源及无入住历史时执行；当前入住禁止停用或删除。已有资产套装时，分别禁止修改寝室号、床位号。资产编辑仅允许真实 `NORMAL`、`REPAIR`、`SCRAPPED` 状态和备注；资产套装编号、二维码令牌和入住历史均不可修改。
## 管理端认证与访问控制（2026-07-17）

管理员不提供注册接口，且只复用 `qh_admin`。账号密码均由 `POST /api/admin/auth/login` 的 JSON `username`、`password` 提交；服务端仅用 BCrypt 校验 `password_hash`，登录成功返回随机 32 位 Token 及不含敏感字段的管理员资料。管理员会话为 Redis Hash：`qh:admin:token:{token}`，TTL 为 30 分钟，不使用 JWT。

| 方法 | 路径 | 管理员认证 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/admin/auth/login` | 否 | 管理员账号密码登录，失败返回业务 `code=401`，不泄露账号状态。 |
| GET | `/api/admin/auth/me` | 是 | 返回当前管理员 `id`、`username`、`displayName`。 |
| POST | `/api/admin/auth/logout` | 是 | 删除当前 `qh:admin:token:{token}` 会话。 |

`/api/admin/**`（除登录外）仅接受管理员 Token。普通用户 `qh:login:token:{token}`、过期 Token、随机 Token 或缺失 Token 均返回 HTTP 401 和 `{code:401,...}`；管理员身份只进入 `AdminContext`，绝不写入 `UserContext`。管理员写操作仍使用唯一 `qh_operate_log`：切面优先记录普通用户，否则记录 `AdminContext` 中的管理员 ID。

## 管理端店铺维护与封面 OSS（2026-07-17，已完成并验证）

## 管理端商品维护与主图 OSS（2026-07-17，已完成并验证）

| 方法 | 路径 | 管理员认证 | 请求/响应 | 说明 |
| --- | --- | --- | --- | --- |
| GET | `/api/admin/goods` | 是 | `page`、`size`、可选 `keyword`、`shopId`、`categoryId`、`saleStatus` | 管理分页；分类通过所属店铺的 `category_id` 筛选。 |
| GET | `/api/admin/goods/{id}` | 是 | 无 | 返回商品、所属店铺及其分类资料。 |
| POST | `/api/admin/goods` | 是 | `shopId`、`name`、可选 `description`、`price`、`stock`、`saleStatus` | 新增商品；不接收 `adminId`、主图 URL 或排序字段。 |
| PUT | `/api/admin/goods/{id}` | 是 | 同新增 | 更新资料；不提供物理删除。 |
| PUT | `/api/admin/goods/{id}/status` | 是 | `{"saleStatus":"ON_SALE"|"OFF_SALE"}` | 上架或下架，历史商品以 `OFF_SALE` 保留。 |
| PUT | `/api/admin/goods/{id}/stock` | 是 | `{"stock":0}` | 将库存设置为不小于 0 的整数。 |
| POST | `/api/admin/goods/{id}/image` | 是 | `multipart/form-data`，字段 `file` | 上传或替换单张商品主图，返回更新后的 `AdminGoodsVO`。 |

- 所有接口均由既有 `/api/admin/**` 门禁和 `AdminContext` 保护；客户端不得传入 `adminId`。所有写操作复用唯一 `@OperateLog` / `qh_operate_log`，不新增商品日志表。
- 服务端校验启用店铺、商品名称、`BigDecimal` 价格（不小于 0）、库存（不小于 0）和 `ON_SALE`/`OFF_SALE` 状态。`qh_goods` 没有独立分类或排序字段，接口不会伪造它们。
- 主图只接收 jpg/jpeg/png/webp、非空且不超过 3MB 的文件，并交叉校验扩展名、Content-Type、真实图片结构和最大 800×800 尺寸。前端仅本地裁剪为 1:1、压缩为 WebP（目标不超过 300KB）和预览；点击保存时上传一次。
- Object Key 为 `qinghe-life-service/goods/{yyyy}/{MM}/{uuid}.webp`。数据库更新失败会尽力删除本次新对象；成功后仅尝试删除当前 Bucket 内、严格匹配 `qinghe-life-service/goods/` 的旧对象。

| 方法 | 路径 | 管理员认证 | 请求/响应 | 说明 |
| --- | --- | --- | --- | --- |
| GET | `/api/admin/shops` | 是 | `page`、`size`、可选 `keyword`、`categoryId`、`status` | 管理分页，返回 `AdminShopVO`，含真实 `status`、`isFeatured`、`coverImage`。 |
| GET | `/api/admin/shops/{id}` | 是 | 无 | 返回任意状态店铺的后台资料。 |
| POST | `/api/admin/shops` | 是 | 名称、启用分类、地址、可选电话、评分、状态、推荐、排序 | 新增店铺；不接收管理员编号或封面 URL。 |
| PUT | `/api/admin/shops/{id}` | 是 | 同新增 | 更新既有资料；不提供物理删除接口。 |
| PUT | `/api/admin/shops/{id}/status` | 是 | `{"status":0|1}` | 统一使用现有 `qh_shop.status` 启用/停用；停用店铺不会由公开接口作为可用店铺返回。 |
| POST | `/api/admin/shops/{id}/cover` | 是 | `multipart/form-data`，字段 `file` | 上传或替换单张封面，返回更新后的 `AdminShopVO`。 |

- 封面仅接受 jpg/jpeg/png/webp，拒绝空文件、超过 3MB、扩展名/Content-Type/签名不一致或无效尺寸的内容；前端在本地裁剪为约 3:2、1200×800 并优先压缩 WebP，预览不发起上传。
- Object Key 固定为 `qinghe-life-service/shops/{yyyy}/{MM}/{uuid}.webp`；密钥只读取 `OSS_ACCESS_KEY_ID` 与 `OSS_ACCESS_KEY_SECRET` 环境变量，接口、日志和文档不记录密钥、完整管理员 Token 或文件内容。
- 成功顺序为上传新对象、更新 `qh_shop.cover_image`、删除精确详情/空值缓存、再尽力删除旧对象。数据库更新失败会补偿删除新对象；旧对象仅在本 Bucket 且严格匹配 `qinghe-life-service/shops/` 受控 Key 时才尝试删除，外部 URL、占位图、用户头像和其他资源不会删除。

## 学生资料与宿舍扫码入住

- `GET/PUT /api/student/profile`：仅从 `UserContext` 取得本人；首次提交使用 `realName`、`studentNo`、`collegeName`、`majorName`、`className`、`contactPhone`，后续仅保存联系电话。实体映射真实列 `college_name`、`major_name`、`student_status`、`current_flag`。
- `POST /api/student/dorm/qr/resolve` 与 `POST /api/student/dorm/check-in` 仅接收 `qrContent`，格式为 `QH-DORM-V1:{64位十六进制}`；不接收用户、床位或套装 ID。`GET /api/student/dorm/me` 无有效入住时正常返回空数据，且仅返回脱敏学号和联系电话。
# 店内商品分类与商品筛选（2026-07-17）

- `GET /api/admin/goods/categories?shopId={id}&status={0|1}`：管理员按店铺查看店内商品分类，返回排序、状态和商品数量。
- `POST /api/admin/goods/categories`：管理员新增 `{shopId,name,sortOrder}`；同店名称唯一。
- `PUT /api/admin/goods/categories/{id}`：管理员修改 `{name,sortOrder}`；`PUT /api/admin/goods/categories/{id}/status`：管理员启停 `{status:"0"|"1"}`。不提供删除接口。
- 管理商品查询参数为 `keyword`、`shopCategoryId`（全局店铺类型）、`shopId`、`goodsCategoryId`（店内商品分类）、`saleStatus`；不得再将店铺类型称作商品分类。
- 商品保存可选 `categoryId`。服务端校验分类归属店铺；停用分类仅允许历史已关联商品原样保留，不允许新分配。`GET /api/shops/{id}/goods-categories` 仅返回启用分类，`GET /api/shops/{id}/goods` 仅返回上架商品。
# 普通订单创建

`POST /api/orders`：请求仅含 `cartItemIds`、`addressId`、可选 `remark`。`totalAmount` 映射数据库 `total_amount`（商品金额合计），`payAmount` 映射 `pay_amount`（最终应付金额）；服务端重读商品、地址与库存，不接受客户端金额。

## 管理员入住查询、退宿与换寝（2026-07-18，已验证）

- 管理接口均在 `/api/admin/dorm` 下，由既有管理员拦截器和 `AdminContext` 认证；请求体不接收 `adminId`、`activeFlag` 或 `assetSetId`。
- `GET /checkins` 支持分页及姓名、学号、校区、楼栋、寝室、学生状态和 `ACTIVE`/`CHECKED_OUT` 筛选；列表学号脱敏，详情联系电话脱敏，响应不含密码哈希、二维码令牌、活动标记或资产内部 ID。
- `POST /checkins/{id}/checkout` 与 `POST /checkins/{id}/transfer` 仅操作 `active_flag=1`；退宿保留历史并释放原资产，换寝关闭原记录、释放原资产、创建关联的新记录并占用新资产。写入失败会回滚，唯一冲突返回统一业务错误。
- 验证收口：管理员服务查询和写入均在服务边界读取 `AdminContext`；响应也不返回 `previousCheckinId`。`AdminDormCheckinIntegrationTest` 通过 7/0/0/0，完整 Maven 回归 XML 汇总为 68/0/0/0。

## 管理员按校区管理宿舍楼（2026-07-20）

- `GET /api/admin/dorm/campuses` 返回启用校区；`GET /buildings` 要求数值 `campusId`，支持 `status`、`keyword` 与分页，并仅返回真实 `building_type=宿舍楼` 的楼栋。
- `POST /buildings` 仅接收 `campusId`、`buildingCode`、`buildingName`、`area`、`remark`、`status`。服务端固定类型为“宿舍楼”，不接收 ID、时间、管理员号或客户端类型；编码去空格转大写，编码和名称同校区唯一。
- `GET/PUT /buildings/{id}`、`PUT /buildings/{id}/status`、`DELETE /buildings/{id}` 均受管理员门禁保护。编码在有下级资源或入住历史时冻结；当前入住时拒绝停用；删除只在无寝室、床位、资产套装、入住历史和当前入住时执行，且绝不级联。
- 响应至少包含校区、编码、名称、类型、区域、备注、状态、寝室数、床位数和当前入住人数；全部写操作复用唯一 `qh_operate_log`。
# 2026-07-20 个人中心姓名显示契约

- `GET /api/user/me` 仅从当前 Bearer Token 对应的 `UserContext` 取得用户身份，再由用户服务查询该用户的 `qh_user` 与 `qh_student_profile.current_flag=1` 当前资料。响应新增安全展示字段 `realName`、`studentNo`、`hasStudentProfile`；不返回 `currentFlag`、`activeFlag`、`passwordHash`、二维码令牌、Token 或学生资料内部 ID。
- 字段语义固定为：`phone` 是登录手机号（响应中仅返回 `phoneMasked`）；`nickname` 是账号昵称；`realName` 是学生实名资料；`studentNo` 是学号。`username` 如存在仅是账号字段，不能作为真实姓名或显示名回退。
- `PUT /api/user/profile` 继续只允许更新账号昵称与头像，不接受或更新 `realName`、`studentNo` 等受保护学籍字段；实名资料不回写 `qh_user.nickname`。学生资料变更后刷新页面会重新查询当前资料并显示最新实名。
