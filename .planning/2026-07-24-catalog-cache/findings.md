# 审计发现

- 基线：`feature/catalog-cache`，`git status -sb` 无改动。
- `ShopServiceImpl` 已有店铺详情 Cache Aside 雏形，使用 `StringRedisTemplate`、`ObjectMapper`、独立空值 Key、30+随机分钟 TTL 和 `setIfAbsent` 字符串锁。
- 现有实现的 Key 不符合本轮 `qh:cache:*` 语义，TTL 与重试为代码常量；锁不能确认当前持有者，finally 无条件删除，Redis 解析失败也未删除坏 Key。
- 商品详情、指定店铺上架商品列表均直接查询 MySQL；商品实时库存不能作为缓存权威数据。
- 现有 `RedissonConfig` 与 Spring Redis 使用同一连接参数；应复用该 `RedissonClient`。
- 店铺写入当前直接删除旧店铺详情/空值 Key；商品写入没有缓存失效，且所有失效都不是事务提交后执行。
- 最小改动：新增配置属性与轻量 `CatalogCache` 组件，统一 `RedisKeys`；改造 `ShopServiceImpl`、`GoodsServiceImpl`、`AdminGoodsServiceImpl` 及直接缓存测试，不改 API 结构或数据库。
