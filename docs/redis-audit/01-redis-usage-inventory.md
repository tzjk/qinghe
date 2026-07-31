# 01. Redis 使用清单与能力矩阵

## 审计口径

本表仅记录当前 `backend/` 和 `agent-service/` 源码可证明的实现。行号均为当前静态源码位置；没有源码证据的场景标为“未实现”，不以设计文档推断。

| 场景 | 所属模块、代码证据 | 结构、Key、Value | TTL/写入/读取/删除 | PII/并发/事务 | Redis 故障行为与评价 | 风险与建议 |
|---|---|---|---|---|---|---|
| 登录验证码 | `UserServiceImpl#sendCode/login/register`，67、88、123、130、154 行 | String；`qh:login:code:{phone}`；6 位验证码 | 2 分钟；发送时覆写；登录/注册读取；成功后删 | Key 含手机号（PII）；并发低；登录写库不与 Redis 同事务 | 发送写失败返回 503；读取失败可冒泡至全局 503。一次性删除正确 | P1：手机号直接进入 Key，且验证码失败次数/发送频率未限制；改为手机号摘要或受控加密 Key，并增加限流/失败次数，不建议为此加分布式锁 |
| 普通用户会话 | `UserServiceImpl#createLogin/logout/syncCurrentSession`，253-275；`RefreshTokenInterceptor`，27-37 | Hash；`qh:login:token:{uuid32}`；字符串 `id/username/nickname/avatarUrl/phoneMasked/profileCompleted/hasPassword/realName/studentNo/hasStudentProfile` | 初始 30 分钟；每次受保护 API 成功读取后滑动续期；退出删 | 含学号、实名等 PII；高频读取；非数据库事务 | Redis 不可用时认证无法安全降级，统一 503；正确快速失败 | P1：不应把实名和学号放会话；保留最小身份/权限字段。Token 随机不可预测，TTL/退出/ThreadLocal 清理正确 |
| 管理员会话 | `AdminAuthServiceImpl#login/logout`，27-43；`AdminAuthInterceptor`，20-38 | Hash；`qh:admin:token:{uuid32}`；`adminId/username/displayName` | 初始和滑动 TTL 均为 30 分钟；退出删 | 低敏感身份；高频读取；无 DB 事务 | Redis 不可用则拒绝后台请求 | P2：Key 与用户域隔离正确；管理员是滑动会话，需在安全策略中明确多端并存与绝对过期规则 |
| WebSocket 鉴权 | `OrderWebSocketHandshakeInterceptor`，38-60 | 复用两种 Token Hash；不新增 Key | 握手读取并刷新 30 分钟；不会删 Key | 同上；连接高并发 | Redis 故障时拒绝握手 | P2：复用会话正确；未记录 Token。WebSocket 在线连接本身不在 Redis 中，不能称为分布式会话/通知状态 |
| 商铺详情缓存 | `ShopServiceImpl#cachedDetail`，460-469；`CatalogCache` | String(JSON)；`qh:cache:shop:{shopId}`；`ShopVO` 或空标记 | 30-35 分钟随机 TTL；读穿透时加载；后台商铺写事务提交后删 | 不含个人信息；热点候选；读 DB 位于重建锁内 | Redis 读/锁/写异常均回退 MySQL | P2：Cache Aside、空值 2 分钟、抖动 TTL、双检锁已实现；商铺更新失效覆盖详情/商品/商品详情 |
| 商品详情缓存 | `GoodsServiceImpl#detail`，57-85 | String(JSON)；`qh:cache:goods:{goodsId}`；`CachedGoods` 或空标记 | 20-25 分钟随机 TTL；读取时仍读 MySQL 补 stock/sales/status；商品管理写后删 | 公开数据；热点候选；写接口事务后删 | Redis 异常回退 MySQL | P2：一致性较好但缓存并非完整详情，命名/文档应明确“静态商品字段缓存” |
| 店铺商品列表缓存 | `ShopServiceImpl#goods/loadShopGoods`，122-149、472 起 | String(JSON 数组)；`qh:cache:shop-goods:{shopId}`；全量在售 `CachedGoods` | 10-15 分钟随机 TTL；请求分页前全量读；商品/店铺写后删 | 公开数据；可能大 Key/热点；事务后删 | Redis 异常回退 MySQL | P2：按店铺缓存全量列表，商品多时有 Big Key、反序列化和内存风险；限定容量或改页/版本化片段，不建议无证据地上本地二级缓存 |
| 目录缓存重建锁 | `CatalogCache#get`，82-123；`RedisKeys`，23-25 | Redisson RLock；`qh:lock:cache:shop/goods/shop-goods:{id}` | `tryLock(wait=100ms, lease=10s)`；缓存 miss 重建时；finally 条件解锁 | 无 PII；热点时使用；锁内有 DB 查询和 JSON 写入 | 锁/Redis 异常直接回退 DB | P1：显式 lease 不启用 watchdog；慢 DB 超过 10s 会提前放锁并导致重建并发。锁粒度正确，不能以 `synchronized` 替代 |
| 秒杀库存预热与资格 | `CouponSeckillServiceImpl#preheat/claim`，77-123；Lua | String/Set/Hash；`qh:coupon:seckill:stock/users/meta:{couponId}`；库存、用户 ID、活动字段 | 无 TTL；人工预热写；Lua 读取/扣减/加集合；只有再次预热才删旧三 Key | 用户 ID 属个人标识；高并发；与 MySQL 非同事务 | Redis 故障返回 503，不绕过至 DB；方向正确 | P1：无过期/活动结束清理，预热可留下永久 Key；用户 Set 可持续增长；应以活动结束保留期配置 TTL/审计清理任务 |
| 秒杀异步消息与状态 | `CouponSeckillServiceImpl#process`，160-177 | Stream `qh:stream:coupon:claim`；Hash `qh:coupon:seckill:retry/failure` | Stream/Hash 无 TTL、无 MAXLEN；Lua XADD；消费者 ACK/重试状态变更 | 消息含 couponId/userId；高并发；DB 事务完成后 ACK | Redis 不可用拒绝秒杀或消费失败留 Pending | P0/P1：Stream 永久增长；失败 Hash 无 TTL；“failure”只是 Hash，不是死信队列；见 05、06 |
| Pending 恢复锁 | `CouponSeckillStreamTask#recoverPendingMessages`，26-32 | RLock；`qh:lock:coupon:seckill:pending-recovery` | `tryLock(0,10s)`；每 15 秒恢复 Pending；finally 条件解锁 | 高并发消费；锁内含 Redis Pending/claim 和 DB 持久化 | 获取失败跳过本轮，Redis 异常记日志 | P1：10 秒租约可能短于 20 条 DB 处理；无 watchdog；多实例默认 consumer-name 相同风险 |
| 订单超时任务锁 | `OrderPaymentTimeoutTask`，19-59 | RLock；`qh:lock:order:timeout-cancel` | `tryLock(0,55s)`；每分钟扫描；finally 条件解锁 | 订单数据；任务并发；DB 事务在锁内逐单执行 | Redis 不可用则本轮任务失败；下轮仍依赖 Redis | P1：全局锁覆盖不设上限的 while 扫描，超过 55s 提前失效；DB 条件更新仍避免已支付误取消，但可能多实例重复扫描 |
| 探店热榜 | `ExploreHotService`，26-60 | ZSet；`qh:zset:explore:hot`；postId→likeCount | 无 TTL；点赞事务提交后更新；空/异常时 MySQL 回退；`rebuild` 先删再建前 500 | 无 PII；热点 Key；MySQL 为事实 | Redis 异常回退 MySQL；空 Key 同步重建 | P2：可恢复且不阻塞点赞事务；无容量/历史淘汰、重建非原子，热 Key 与 500 条限制需监控 |
| 附近商铺 | `ShopGeoService`，18-22；`ExploreServiceImpl`，119-133 | GEO（ZSet）；`qh:geo:shop`；shopId→经纬度 | 无 TTL；商铺创建/修改/状态变化事务提交后同步；查询半径≤20km，异常回退 MySQL | 不存用户坐标；公开商铺坐标；热点候选 | Redis 异常/WRONGTYPE 回退 MySQL Haversine 全扫描 | P2：状态/经纬度更新与禁用移除正确；按全局 Key 后 DB 分类过滤，规模增长时回退全扫描成本高；无启动重建机制 |
| Agent 公共缓存 | `agent-service/app/cache/public.py` | 进程内 OrderedDict；`tool:canonical-json(args)`；仅 7 个公共工具 | 默认关闭；启用时 30 秒、500 条、LRU；无 Redis | 禁止个人工具缓存；单实例有效 | 进程重启丢失，不影响正确性 | 正确不接 Redis；多实例仅在命中率/后端压力有数据证据时再引入共享缓存接口 |
| Agent 会话、限流、熔断、用量 | `conversation.py`、`core/limits.py`、`providers/middleware.py`、`metrics.py` | 进程内 dict/set/semaphore/Counter，无 Redis Key | 会话 30 分钟、1000 条；限流 60s 窗口；熔断 30s；随进程消失 | 会话摘要可能含用户文本；不存 Token/原始工具结果 | 单实例安全降级为内存行为；多实例不共享 | 未实现 Redis，当前本地/单实例合理；多实例才需要可替换分布式实现，且禁止缓存 Bearer Token、完整地址/订单/宿舍、API Key、系统提示词 |

## 明确未实现的用户关注场景

- 分类、首页汇总、全局商品列表、优惠券列表、用户优惠券、个人订单、点赞状态、接口防重复提交、通用幂等 Key、订单延迟队列/ZSet 时间轮、Redis Pub/Sub/WebSocket 通知状态：**未实现**。
- `RedisKeys.shopCategory()` 已定义但无调用；不能视为分类缓存已实现。
- 没有 `ReactiveRedisTemplate`、`Cacheable/CachePut/CacheEvict`、`StreamMessageListenerContainer`、`XREADGROUP/XACK/XPENDING/XCLAIM` 原生命令封装、Bloom Filter、Redis 延迟队列或消息 ID 表实现。

## Key 规范结论

- 绝大多数运行时 Key 由 `RedisKeys` 管理，前缀 `qh:`、模块分段和实体 ID 可读；例外是 `OrderPaymentTimeoutTask` 的锁名以及 Stream Key 在 `application.yml`/`@Value` 中重复硬编码。建议将二者收敛到同一 Key 工厂。
- `qinghe.redis-key-prefix` 配置目前没有被源码引用，不能提供环境隔离；开发、测试、生产若连接同一 Redis DB 2 会共享 Key 并可能碰撞。应在连接/部署层隔离 DB 或让 Key 工厂显式包含受控环境命名空间。
- 未发现 `KEYS`、`SCAN`、`FLUSHDB`、`FLUSHALL` 或无边界通配删除；现有删除均为显式 Key/显式集合。批量删除只对已构造的目录 Key 安全，不能推广为模式删除。

