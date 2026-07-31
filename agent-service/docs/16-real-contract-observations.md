# Phase 4 真实契约观察

本记录仅保留路径、参数、结构、状态和耗时；不记录 Token、个人数据或完整响应体。

## 源码复核的契约

| 接口 | 鉴权 | 参数/默认值 | data 结构 |
|---|---|---|---|
| `GET /api/coupons` | 公共 | `page=1,size=10,status?`，size ≤ 100 | `PageResult<CouponVO>` |
| `GET /api/shops` | 公共 | `page=1,size=10,categoryId?,keyword?,sort?`，size ≤ 100 | `PageResult<ShopVO>` |
| `GET /api/shops/{id}` | 公共 | 正整数路径 `id` | `ShopVO` |
| `GET /api/shops/{id}/goods` | 公共 | `page=1,size=10,keyword?,categoryId?` | `PageResult<GoodsVO>` |
| `GET /api/explore/posts` | 公共 | `page=1,size=10,sort=latest,shopId?`；Agent 热门使用 `sort=hot` | `PageResult<ExplorePostVO>` |
| `GET /api/explore/shops/nearby` | 公共 | longitude、latitude、radius（0.1--20）、page=1、size=10、categoryId? | `PageResult<NearbyShopVO>` |
| `/api/user/me`、`/api/coupons/mine`、`/api/orders`、`/api/orders/{orderId}`、`/api/student/profile`、`/api/student/dorm/me`、`/api/addresses` | Bearer | 没有 userId 参数；订单/优惠券分页 size ≤ 100 | 由 Spring 当前用户上下文决定 |

所有已审计接口均由 `Result<T>` 包装，结构固定为 `code`、`message`、`data`；成功业务码为 200。分页固定为 `records`、`total`、`page`、`size`。Java `LocalDateTime` 以 Spring JSON 序列化格式返回；金额字段为 BigDecimal JSON 数值。

## 阶段 A 实测（2026-07-30）

| 接口 | HTTP/业务码 | data 形状 | 耗时 |
|---|---:|---|---:|
| `/api/coupons?page=1&size=10` | 200 / 200 | `records,total,page,size` | 374 ms |
| `/api/shops?page=1&size=10` | 200 / 200 | `records,total,page,size` | 53 ms |
| `/api/shops/{existing-id}` | 200 / 200 | `id,categoryId,name,address,phone,score,coverImage,sortOrder` | 44 ms |
| `/api/shops/{existing-id}/goods?page=1&size=10` | 200 / 200 | `records,total,page,size` | 101 ms |
| `/api/explore/posts?page=1&size=10&sort=hot` | 200 / 200 | `records,total,page,size` | 43 ms |
| `/api/explore/shops/nearby?longitude=120&latitude=30&radius=5&page=1&size=10` | 200 / 200 | `records,total,page,size` | 56 ms |

## Agent 侧兼容

- 确定性 Provider 可与 Spring 工具组合，不再要求模型 Key；Mock 默认值不变。
- 真实宿舍字段 `campusName/buildingName/roomNo/bedNo` 被最小化为 Agent 回答使用的校区、楼栋、寝室、床位；同时兼容离线 Mock 的旧别名。
- 公开优惠使用 `availableStock`（并仅兼容离线样例的 `stock`）；只依据状态、库存和领取窗口过滤，不声明商品折扣。
- 商铺、探店、订单、学生档案、地址和个人资料只保留回答所需字段；订单摘要不包含地址、收件人、电话、备注或商品明细。
- 所有 Spring 工具继续返回统一 `ToolResult`：success、tool_name、data、data_source=spring、error_code、user_message、backend_code、duration_ms、retryable；公开 API 仅输出安全工具追踪，不输出 ToolResult 数据。

## Phase 4.5 FastAPI 实测（2026-07-30）

- `/health` 返回 `ok` 且 `mock_mode=false`；`/ready` 返回 Provider `deterministic` 和 15 个注册工具；开发环境的 `/api/v1/tools` 未暴露 Key、Token 或内部后端 URL。
- 以下每项均通过真实 8100 Socket 的 `POST /api/v1/chat`，而非仅证明 8090 直连：`get_today_promotions`、`get_active_coupons`、`search_shops`、`get_shop_detail`、`get_shop_goods`、`get_hot_explore_posts` 和 `get_nearby_shops`。商铺详情和商品查询使用本轮公共商铺列表中的真实 ID，但未记录该 ID 或业务内容。
- `get_shop_goods` 只允许正整数商铺 ID、分页、可选公开分类和关键词；输出仅保留公开商品摘要字段。它不将普通商品价格解释为折扣，也不会注册商品级促销工具。
- 无 Token 的宿舍请求、固定无效 Token、不存在商铺、经纬度越界和半径越界均返回安全失败工具追踪；公开响应、SSE 和会话中均未出现 Token、内部 URL、Python/Java 堆栈。个人成功路径未执行。
