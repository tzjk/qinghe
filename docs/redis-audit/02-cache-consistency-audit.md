# 02. 缓存一致性审计

## 当前模式

目录缓存是 Redis **Cache Aside**：读 miss 后从 MySQL 加载，再写 Redis；写接口先完成 MySQL 事务，`CatalogCache#evictAfterCommit` 在 `afterCommit` 删除相关 Key。没有 Read Through、Write Through、Write Behind、本地二级缓存或 Spring Cache 注解实现。

## 缓存对象与失效矩阵

| 对象 | 读路径 | 写路径 | 当前失效 | 结论 |
|---|---|---|---|---|
| 商铺详情 | `ShopServiceImpl#detail/cachedDetail` | 新建、更新、状态、封面上传 | `ShopServiceImpl#invalidateShopCache` 在提交后删详情、店铺商品列表及该店商品详情 | 正确采用“DB 成功提交后删缓存”；不会在事务回滚时提前删 |
| 商品详情 | `GoodsServiceImpl#detail` | 管理端创建、更新、状态、库存、图片等（均经 `invalidateGoodsCache`） | 提交后删商品详情及旧/新店铺商品列表 | 正确覆盖商品跨店移动时需失效的店铺列表（源码传入相关 shopId） |
| 店铺商品列表 | `ShopServiceImpl#goods` | 同上 | 店铺更新删本店列表；商品写删相关店铺列表 | 基本完整；列表内缓存全量静态字段但每次仍查当前商品 stock/sales/status |
| 负缓存 | `CatalogCache` | 缺失/下架对象 | 空对象写 `__QH_CATALOG_CACHE_NULL__`，TTL 2 分钟 | 能降低重复非法 ID 回源；对空列表也使用同一标记 |
| 热榜/GEO | 非普通缓存 | 点赞、店铺变更 | 都在事务提交后更新/删除；失败只日志 | 是可重建派生索引，不应与业务 DB 事务强绑定 |

## 已确认优点

- `CatalogCache#evictAfterCommit` 先检查 Spring 事务同步状态；事务存在时只在 `afterCommit` 删除，避免“数据库回滚但缓存已被提前删”的经典不一致。
- 目录缓存写失败、读失败、Redisson 故障均返回 MySQL，不把 Redis 当业务事实源。
- 缓存被污染/JSON 反序列化失败时只删除已知单 Key，随后回源；没有危险的模式清理。
- 管理端商铺更新同时使 GEO 事务后同步，商铺禁用会从 GEO 移除；商品/店铺的详情和列表 Key 均被失效。

## 风险与建议

### P1：目录重建锁显式 10 秒租约

`CatalogCache#get` 使用 `tryLock(100ms, 10s)`，数据库加载和 JSON 写入在临界区内。若慢查询、GC 或 Redis 网络抖动超过租约，锁会先过期，第二个实例可重建同一 Key；原线程的 `isHeldByCurrentThread` 会避免误解锁，但无法避免击穿时的重复回源。

建议：先采集加载耗时；若 P99 远小于 10 秒则保持显式 lease 并告警超时，若无法保证则改为无 lease 的 watchdog 语义或将锁内只保留必要加载。不要直接增加“延迟双删”。

### P2：全量店铺商品列表是潜在大 Key

`loadShopGoods` 将一个店所有在售商品序列化进一个 String，随后所有分页/关键字请求都反序列化全量数组。商品数/描述/图片增长时，会放大 Redis 单 Key、网络、GC 和每次请求 CPU。

建议：以真实店铺商品上限和 `MEMORY USAGE`/响应体数据决定；达到阈值时分段或仅缓存 ID/短字段。当前不建议盲目拆分所有页面或引入二级缓存。

### P2：缓存删除失败无补偿/观测

失效失败被捕获并仅记 warn，旧值将存活至最多 35/25/15 分钟。对于公开目录数据可接受最终一致；但缺少删除失败计数、Key 类型和重试队列，不应宣称强一致。

建议：先增加业务指标和结构化日志；仅在写后展示一致性有明确 SLA 时再考虑短延迟重试或版本 Key。无需 CDC、Binlog 或消息化失效。

### P3：通用 `RedisTemplate<String,Object>` 未被业务路径使用

`RedisConfig` 以 `GenericJackson2JsonRedisSerializer` 配置通用模板，但实际 Redis 业务均使用 `StringRedisTemplate` 和显式 Jackson JSON。当前没有 Long Hash 写入导致 `ClassCastException` 的证据；该通用 Bean 的类型元数据兼容性/安全面却不带来收益。

建议：后续若无真实调用可移除或限制其用途；不要把会话 Hash 改为对象序列化。

## 不建议的方案

- 默认延迟双删、CDC、逻辑过期/后台刷新、多级缓存：现有数据量和一致性 SLA 未证明需要。
- 为所有非法 ID 上 Bloom Filter：2 分钟空值缓存与参数校验已覆盖通常场景，需先有恶意探测/规模证据。

