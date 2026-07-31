# Confirmed Spring Read-only Tool Contracts

All entries below are static GET whitelist entries. No admin endpoint is registered.

| Tool | Endpoint | Auth | Typed inputs |
|---|---|---:|---|
| get_today_promotions | `/api/coupons` | no | none |
| get_active_coupons | `/api/coupons` | no | page, size, status |
| search_shops | `/api/shops` | no | categoryId, keyword, sort, page, size |
| get_shop_detail | `/api/shops/{id}` | no | positive id |
| get_nearby_shops | `/api/explore/shops/nearby` | no | longitude, latitude, radius, categoryId, page, size |
| get_hot_explore_posts | `/api/explore/posts` | no | page, size, `sort=hot` |
| get_my_profile | `/api/user/me` | yes | none |
| get_my_dorm_info | `/api/student/dorm/me` | yes | none |
| get_my_coupon_wallet | `/api/coupons/mine` | yes | page, size, status |
| get_my_recent_orders | `/api/orders` | yes | page, size, status |
| get_my_order_detail | `/api/orders/{orderId}` | yes | positive orderId |
| get_my_default_address | `/api/addresses` | yes | none |

Inputs forbid extra values and do not expose `userId`, `studentId`, telephone, name, arbitrary URL, SQL, headers, or path fragments. Longitude is -180 to 180, latitude is -90 to 90, radius is 0.1 to 20 km, and backend-compatible page sizes are limited to 50 or 100 as applicable.

`ToolResult` normalizes success, data source, backend code/request ID, safe error code/message, duration, and retryability. Backend results use the Qinghe `Result(code, message, data)` wrapper. Empty address selection returns `found=false`, not a fabricated address.

## Unavailable

`search_goods` and `get_discounted_goods` are intentionally unavailable. Current audited goods data lacks original/promotional price, promotion window, item-to-coupon relation, and reliable discount aggregation. Coupons can be reported as active; ordinary goods must not be described as discounted.
