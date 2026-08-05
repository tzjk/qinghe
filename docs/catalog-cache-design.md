# 店铺与商品目录 Redis 缓存设计

## 范围

缓存公开店铺详情、公开商品详情和指定店铺上架商品列表。MySQL 始终是最终事实来源；购物车、订单、用户数据和库存扣减不作为 Redis 缓存事实。

## Key 与 TTL

| 类型 | Key | TTL 配置 |
|---|---|---|
| 店铺详情 | `qh:cache:shop:{shopId}` | `catalog.cache.shop-ttl-minutes` + 抖动 |
| 商品详情 | `qh:cache:goods:{goodsId}` | `catalog.cache.goods-ttl-minutes` + 抖动 |
| 店铺上架商品列表 | `qh:cache:shop-goods:{shopId}` | `catalog.cache.list-ttl-minutes` + 抖动 |
| 热点锁 | `qh:lock:cache:shop:{shopId}`、`qh:lock:cache:goods:{goodsId}`、`qh:lock:cache:shop-goods:{shopId}` | `catalog.cache.lock-lease-seconds` |

空值标记使用原缓存 Key 的短 TTL，不把 Java `null` 序列化为业务 JSON。正常缓存 TTL 的随机范围为基础分钟数至基础分钟数加 `catalog.cache.ttl-jitter-minutes`。

## 读取与故障处理

读取先检查 Redis：正常 JSON 反序列化返回，空值标记返回不存在或空列表。缺失时以 Redisson 业务 ID 锁有限等待、有限重试并二次读缓存；锁异常或 Redis 异常直接回源 MySQL。JSON 反序列化失败会删除坏 Key 后回源。数据库查询失败不写缓存。

商品缓存仅保存目录静态字段；库存与销量由当前 MySQL 记录覆盖，避免 Redis 成为实时库存来源。

## 写后失效

管理员店铺和商品服务在事务提交后才删除 Key。店铺修改删除本店详情、商品列表及本店商品详情；商品创建、修改、状态、库存和主图更新删除商品详情及所属店铺列表，换店同时删除旧店与新店列表。Redis 删除失败只记录日志，成功的数据库事务不回滚。
