# 普通订单、优惠券、Redis 缓存与限时秒杀架构审计（2026-07-17）

## 2026-07-24 秒杀优惠券实现与验证收口

- 路由分离：普通券仅由 `POST /api/coupons/{couponId}/claim` 在 MySQL 事务中领取；`coupon_status=SECKILL` 只能走 `POST /api/coupons/{couponId}/seckill-claim`。普通路径拒绝秒杀券，未改普通券领取、锁券、核销或退券规则。
- Redis Key：`qh:coupon:seckill:stock:{couponId}`、`qh:coupon:seckill:users:{couponId}`、`qh:coupon:seckill:meta:{couponId}`、`qh:stream:coupon:claim`、`qh:coupon:seckill:retry`、`qh:coupon:seckill:failure`，以及 Pending 恢复锁 `qh:lock:coupon:seckill:pending-recovery`。Stream Key、Consumer Group 和 Consumer 名称均可由 `coupon.seckill` 配置覆盖。
- Lua 以服务端时间原子校验预热元数据、活动状态和时间窗口，检查一人一券集合与 Redis 库存；成功时执行 `DECR`、`SADD`、`XADD`。返回码为：`0` 受理、`1` 已领、`2` Redis 库存不足、`3` 未开始、`4` 已结束、`5` 已停用、`6` 未预热或元数据不完整。
- Consumer Group 通过 Spring Data Redis 2.7.18 底层 `XGROUP CREATE ... 0-0 MKSTREAM` 创建：Stream 不存在时自动创建且不写入伪造业务消息；仅 `BUSYGROUP` 视为幂等成功，连接、权限和其他错误不被吞掉。
- 消费者先在同一 MySQL 事务内检查既有用户券，再以 `available_stock > 0` 条件更新扣减库存并插入用户券；数据库唯一约束 `(user_id,coupon_id)` 保留为重复投递和并发的一人一券最终保护。事务成功后才 ACK Stream 消息并清除该消息重试计数。
- 失败消息保持 Pending；定时恢复使用 `XPENDING`/`XCLAIM`。达到配置的最大重试次数后记录精简失败原因、ACK 该消息，避免无限重复消费；恢复任务的 Redisson 锁只用于跨实例调度互斥，不参与领取请求。
- 实测：`CouponOrderIntegrationTest` 22 项与 `CouponSeckillStreamIntegrationTest` 7 项，合计 29/0/0/0；`mvn "-Dmaven.repo.local=C:/Users/28402/.m2/repository" -DskipTests package` 成功生成后端 JAR。未执行 SQL、Redis 清库或前端构建。

## 2026-07-24 普通优惠券基础业务实施边界

- 本轮普通券只使用 MySQL 事务和条件更新：领取扣减 `available_stock`，订单锁定用户券，支付核销，待支付取消释放。没有 Lua、Redis Stream、秒杀入口、全局领取锁、WebSocket、Redis 商品缓存或营业报表。
- `backend/src/main/resources/sql/coupon_foundation_increment.sql` 现作为结构参考保留：用户已人工完成其中对应的真实字段与索引配置，应用和 Codex 均未执行该 SQL，也不得重复执行。
- 固定金额券使用 `discount_amount`，折扣券使用 `discount_rate`；金额统一 `BigDecimal` 与 `HALF_UP` 两位小数。订单仍只使用 `total_amount`（商品原始总额）和 `pay_amount`（优惠后实付），不新增 `goods_amount`。
- 当前 `(user_id,coupon_id)` 唯一索引保留，故 `per_user_limit=1`。未来若需一人多张同券，必须另行批准唯一约束变更与完整迁移，不能绕过该约束。

### 2026-07-24 基础模块验证收口

- 普通券领取、订单锁定、支付核销与主动/超时取消释放均已在同一 MySQL 事务边界验证；库存领取与用户券状态转换均以条件更新兜底。
- `CouponOrderIntegrationTest` 20 项、`OrderCreateIntegrationTest` 5 项、`OrderLifecycleIntegrationTest` 8 项、`OrderTimeoutCancelIntegrationTest` 7 项，共 40 项通过，0 failures、0 errors；唯一前缀 `COUPON_ORDER_TEST_` 的优惠券、用户券、订单、明细、购物车、地址、商品、店铺、日志和用户残留均为 0。
- 未实现或启动 Lua、Redis Stream、秒杀优惠券、Redis 商品缓存、WebSocket 或营业报表。

## 2026-07-23 订单生命周期状态模型（当前实施边界）

- 当前仅完成状态枚举和候选迁移设计。`PENDING_PAY` 是现有待支付编码；`PAID`、`ACCEPTED`、`DELIVERING`、`COMPLETED`、`CANCELLED` 只作为状态机定义，当前没有支付、取消、管理员履约或超时扫描实现。
- 本轮不实现优惠券、Redis Stream、WebSocket、订单缓存、营业报表或订单前端页面。
- 后续取消订单必须以同一订单业务 `REQUIRED` 事务完成条件状态更新、库存恢复和成功日志；直接写唯一 `qh_operate_log`，避免通用 `REQUIRES_NEW` AOP 成功日志重复或脱离业务事务。
- 候选 `pay_expire_time` 与 `(status, pay_expire_time)` 只用于未来超时未支付扫描设计，尚待真实表/索引人工核验，当前不运行依赖它们的测试。

## 2026-07-18 普通订单创建验证结果

- 普通订单创建已完成并验证：专项测试 5/0/0/0，完整 Maven 回归 68/0/0/0，后端 JAR 与前端生产构建均成功。
- 专项覆盖未登录/归属/跨店、目录和地址、实时价格/四项金额/快照、订单明细、条件库存并发、任一库存不足与写入异常回滚、精确清车和伪造金额忽略。
- `ORDER_CREATE_TEST_` 清理并断言用户、地址、店铺、商品、购物车、订单、明细、操作日志、验证码和全部测试登录 Token 均为零；未使用 TRUNCATE、无条件 DELETE 或 Redis 清库。

## 1. 审计范围与证据边界

本轮只读取仓库中的 SQL、实体、Mapper、Service、Controller、配置和设计文档；未连接 MySQL 或 Redis、未执行 SQL、未启动服务、未运行测试或构建。因此“现状”是静态仓库结论，不代表运行中数据库已复核。

必须分开的四类职责如下：

| 类别 | 入口与处理方式 | 最终事实 |
|---|---|---|
| A. 普通商品订单 | 购物车选中项，同步数据库事务 | `qh_order`、`qh_order_item`、`qh_goods.stock` |
| B. 普通优惠券 | 领取与核销均在数据库事务中 | `qh_coupon`、`qh_user_coupon` |
| C. 限时秒杀优惠券 | Redis Lua 受理、Redis Stream 异步落库 | 秒杀活动表、秒杀受理订单表和数据库库存 |
| D. 商铺和商品查询缓存 | Cache Aside 查询加速 | MySQL；Redis 只是不可靠时可降级的副本 |

本轮已确认 `UserContext` 与 `AdminContext` 独立；关键写操作只能继续复用唯一 `qh_operate_log`，不新建模块日志表。

## 2. 普通订单现状与目标边界

### 2.1 静态现状

`qh_cart` 及其 `CartController`/`CartServiceImpl` 已可按 `shop_id` 分组保存多店商品；`qh_order`、`qh_order_item` 有实体与 Mapper，但没有订单 Service 或 Controller。订单表已有订单号唯一、用户/店铺/地址 ID、收件人和电话、可读配送地址、总额/优惠额/应付额、状态和备注；明细已有商品名称、图片、单价、数量和小计快照。

缺口为结构化校园地址快照、独立配送费、取消原因、取消时间、完成时间、已核销用户券关联和订单查询索引。现表也不能作为秒杀受理表使用。

### 2.2 第一版普通订单规则

1. 购物车可保留多店商品，但提交请求只传购物车项 ID 与地址 ID；服务端先锁定并重新读取这些项、商品、商铺和地址。
2. 所有选中项必须属于当前用户且 `shop_id` 相同；跨店直接拒绝，不拆单、不静默筛选。
3. 商品名称、图片、单价、店铺与库存均由服务端重新读取；前端价格、总额、库存结论和优惠金额均不可信。
4. 在同一数据库事务中，对每个商品执行 `UPDATE qh_goods SET stock=stock-? WHERE id=? AND stock>=? AND sale_status='ON_SALE'`。任一影响行数为 0 即抛错并回滚订单、明细、库存、券状态和购物车删除。
5. 成功后只按本次提交的购物车项 ID、当前用户和同店条件删除；不得清空其他店或未提交项。
6. 地址必须归当前用户所有；订单写入不可变的校区、楼栋、房间、配送点、详细位置、收件人和电话快照，同时保留 `delivery_address` 的可读文本。
7. 普通订单不使用 Redis 锁作为库存正确性的唯一条件；数据库条件更新和事务才是库存边界。

### 2.3 状态与退券规则

订单状态首版为 `PENDING_PAY`、`PAID`、`CANCELLED`、`COMPLETED`。模拟支付不接真实支付。已在 `PENDING_PAY` 被取消的订单可在同一事务中将该订单核销的普通用户券退回 `AVAILABLE`，清除 `order_id/use_time` 并记录 `returned_time`；`PAID` 或 `COMPLETED` 后取消不退券。订单创建失败必须回滚券核销。

## 3. 普通优惠券现状与目标边界

`qh_coupon` 当前只有名称、类型、固定优惠金额、门槛、总库存/已领数、状态和单一有效期；`qh_user_coupon` 有用户券状态、订单、领取/使用时间及 `(user_id,coupon_id)` 唯一约束。没有领取/使用双窗口、适用范围、每人限领、折扣率或过期时间快照；也没有领取、核销接口或服务。

`coupon_core_increment.sql` 仅补齐普通券的范围、折扣率、领取/使用窗口、限领与用户券过期/来源/退券字段。既有唯一约束不能在本轮允许一人持有同一券多张，因此普通券第一版规定 `per_user_limit=1`；后台发布时拒绝大于 1 的配置。若未来必须支持多张，需另行获批包含唯一约束重构的迁移，不能在本轮绕开。

普通领取在事务内以条件更新 `claimed_count < total_stock`，再插入用户券；重复键或影响行数 0 均拒绝。普通下单核销同样以 `coupon_status='AVAILABLE'`、归属用户、未过期、范围/店铺匹配、金额达门槛为条件更新，且将 `qh_order.user_coupon_id` 的唯一索引作为额外保护。优惠金额始终由服务端按券定义和订单实时商品金额计算。

## 4. 限时秒杀优惠券架构

### 4.1 结构选择

现有 `qh_coupon` 未提供秒杀时间、秒杀库存、资格、异步订单和一人一活动约束，不能安全承担秒杀。设计采用专用 `qh_seckill_coupon_activity` 与 `qh_seckill_coupon_order`，而不是向普通订单 Controller 添加分支。

- 活动表保存专用优惠券、数据库库存、开始/结束时间、状态、每人限购和秒杀价/规则。
- 受理订单表保存活动、用户、Redis Stream 消息 ID、处理状态和最终用户券；`(activity_id,user_id)`、`stream_message_id` 和受理订单号均唯一。
- 第一版限购固定为 1；活动使用专用 `coupon_id`，不得复用已在普通领取流通的券，避免 `qh_user_coupon` 既有唯一约束冲突。

### 4.2 发布、Lua 与异步落库

发布/预热事务成功后，将活动元数据写入 `qh:seckill:activity:{activityId}`，将库存写入 `qh:seckill:stock:{activityId}`，并清理旧用户标记。Lua 脚本在单个 Redis 分片内原子执行：

1. 校验活动状态和当前时间位于开始、结束之间；
2. 读取库存并拒绝非正值；
3. 检查 `qh:seckill:users:{activityId}` 是否已有当前用户；
4. 扣减库存并写入用户标记；
5. `XADD qh:stream:seckill-orders * activityId ... userId ... couponId ...`；
6. 返回“已受理”，不在 HTTP 线程创建数据库订单或用户券。

消费者在数据库事务中先根据 Stream 消息 ID 和 `(activity_id,user_id)` 幂等查询；不存在时，执行 `UPDATE qh_seckill_coupon_activity SET stock=stock-1 WHERE id=? AND stock>0 AND activity_status='PUBLISHED'`，创建受理订单，插入来源为 `SECKILL` 的用户券并回填受理订单。任何失败都回滚数据库事务；数据库是最终库存和发券事实，Redis 只用于削峰与预校验。

## 5. Redis Stream、重试和最终一致性

仓库没有 RabbitMQ、Kafka、Redis Stream 或 Redisson 依赖/实现，当前只具备 Spring Data Redis。因此后续优先用 Redis Stream，避免本阶段引入额外基础设施；本轮不添加依赖或消费者。

- Stream：`qh:stream:seckill-orders`；Consumer Group：`qh:seckill-order-group`；Consumer 名称使用实例 ID 加线程 ID。
- 消费者成功完成数据库事务后才 ACK。每条消息的 Redis Stream ID 保存到 `qh_seckill_coupon_order.stream_message_id`，重复投递先读该唯一记录并直接 ACK。
- 定时扫描 Pending List；优先使用兼容版本的 `XPENDING` + `XCLAIM`，若运行 Redis 版本支持可采用 `XAUTOCLAIM`。超过 5 次仍失败，写入 `qh:stream:seckill-orders:dlq` 并将受理订单标记 `FAILED`，记录脱敏失败原因；不无限重试。
- 发布补偿和人工恢复以数据库活动库存、受理订单和用户券为准。Redis 丢失或消费者故障后不得直接信任残留库存；需依据数据库重建活动缓存，并对 Pending/DLQ 逐条恢复。

## 6. 分布式锁边界

仓库未见 Redisson。现有商铺缓存的 `setIfAbsent` 锁固定值直接 `delete`，没有令牌校验和续期，不能复制到下单或秒杀。

后续若已获批引入 Redisson，仅可用于：秒杀消费者按 `userId + activityId` 的短互斥、热点缓存重建的双重检查、明确跨实例的短事务。仍必须同时保留 Lua 原子资格判断、数据库唯一索引、`stock>0` 条件更新和消费者幂等。禁止普通订单全局锁、商品级大锁和以锁代替库存条件更新；解锁必须校验所有者，不能删除其他线程的锁。

## 7. Redis 查询缓存治理

保留既有 `qh:shop:detail:{shopId}`、`qh:shop:null:{shopId}`、`qh:lock:shop:{shopId}`，不另建 `qh:cache:shop:*` 平行体系。建议后续在 `RedisKeys` 的同一命名入口补齐：

| 类别 | Key | 规则 |
|---|---|---|
| 商品详情 | `qh:goods:detail:{goodsId}`、`qh:goods:null:{goodsId}`、`qh:lock:goods:{goodsId}` | Cache Aside、空值短 TTL、热点双重检查 |
| 店铺商品列表 | `qh:shop:goods:{shopId}` | 参数化列表另含页码/分类/排序版本；写后只删该店相关 Key |
| 店内商品分类 | `qh:goods-category:shop:{shopId}` | 店内分类写后精确删除 |
| 普通券详情 | `qh:coupon:detail:{couponId}`、`qh:coupon:null:{couponId}`、`qh:lock:coupon:{couponId}` | 只缓存公开可领取资料，不缓存用户券余额 |
| 秒杀活动 | `qh:seckill:activity:{activityId}`、`qh:seckill:stock:{activityId}`、`qh:seckill:users:{activityId}` | 活动预热专用，不与普通券详情混用 |
| 异步队列 | `qh:stream:seckill-orders`、`qh:stream:seckill-orders:dlq` | Stream 与死信 Stream |

所有查询入口先校验非法 ID。空值缓存 TTL 明显短于实体缓存；实体 TTL 加随机值，热点可错峰预热。热点 Key 只能互斥双检或逻辑过期异步重建，避免并发回源。Redis 不可用允许有限并发的数据库降级，需设置舱壁/限流，不能造成数据库雪崩。写路径先完成数据库事务，再在 after-commit 精确删除相关 Key；禁止 `FLUSHDB`、`FLUSHALL` 与无关批量删除。

## 8. 后续测试矩阵

| 范围 | 必测项 |
|---|---|
| 普通订单 | 单店下单、跨店拒绝、实时价格、库存不足、并发扣库存、事务回滚、地址/商品快照、购物车精确清理 |
| 普通优惠券 | 重复领取、超过限领、未开始/已过期、未达门槛、范围不匹配、重复核销、下单失败回滚、待支付取消退券 |
| 限时秒杀 | 未开始/已结束、Redis 库存不足、一人一单、Lua 原子性、高并发不超卖、重复消费、失败重试、Pending 恢复、数据库唯一兜底、Redis/数据库最终一致性 |
| 缓存 | 空值缓存、热点重建互斥、随机 TTL、并发回源次数、写后精确失效、Redis 降级限流 |

## 9. 推荐实施阶段

1. 阶段一：普通订单创建。
2. 阶段二：普通订单查询、取消和库存回补。
3. 阶段三：普通优惠券领取与核销。
4. 阶段四：Redis 查询缓存治理。
5. 阶段五：限时秒杀券 Lua 与 Redis 库存。
6. 阶段六：Redis Stream/MQ 异步创建秒杀订单。
7. 阶段七：分布式锁、幂等、重试和异常恢复。
8. 阶段八：压力测试与数据一致性验证。

每阶段均需先由用户在 DataGrip 人工执行并复核对应 SQL，再进入该阶段的代码门禁；不得提前开始下一阶段。
# 普通订单金额实现状态（2026-07-18）

第一阶段采用真实 `qh_order.total_amount` 作为商品金额合计，`pay_amount` 作为最终应付金额；不新增 `goods_amount`。本次仅实现普通订单同步事务、MySQL 条件扣库存与购物车精确清理；不进入取消/回补、优惠券、秒杀、Redis Stream、Redisson、支付或配送状态。
