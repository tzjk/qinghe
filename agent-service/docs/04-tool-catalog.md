# 青禾校园智能助手：工具目录（仅规划）

## 统一约定

所有工具均为只读、白名单注册、Pydantic 严格校验。返回结构统一封装为 `{data, source, retrieved_at, notices}`；分页工具在 `data` 中包含 `records,total,page,size`。`source` 仅标识 Spring Boot 或受治理知识库。模型连续调用上限为公共工具 3 次、个人工具 2 次、知识工具 1 次；同一工具参数完全相同不得重复调用。

“超时”指 HTTPX 工具预算；仅 GET 发生网络瞬断时最多重试 1 次。失败提示不得暴露后端堆栈、Token 或他人数据。

## 公共实时工具

| 工具 | 场景与输入 | 返回/对应 Spring Boot | 登录 | 超时与失败提示 | 敏感/连续调用/实现条件 |
|---|---|---|---:|---|---|
| `get_today_promotions` | 今天可领取的优惠；`page,size` | 券名、券型、优惠、门槛、库存、领取/使用窗、`shopId`；`GET /api/coupons` | 否 | 5s；“优惠信息暂不可用” | 无；可 3 次；可实现。 |
| `get_active_coupons` | 指定店铺或通用券；`shop_id?`、分页 | 同上；当前接口无 `shopId` 查询参数，Agent 可受限分页筛选 | 否 | 5s；“暂无法查询可领取优惠券” | 无；可 2 次；部分，建议新增后端店铺筛选。 |
| `search_shops` | 按描述找店；`keyword,category_id?,sort?,page,size` | 商铺基础资料；`GET /api/shops` | 否 | 5s；“暂无法搜索商铺” | 电话属于业务联系信息，回答默认不展示；可 3 次；可实现。 |
| `search_goods` | 商品关键词；`keyword,shop_id?,page,size` | 商品名、价格、库存状态；现有 `GET /api/goods` 无 keyword | 否 | 5s；“当前不能按关键词搜索商品” | 无；可 2 次；待补充用户端关键字接口。 |
| `get_shop_detail` | 查询指定商铺；`shop_id` | 商铺详情；`GET /api/shops/{id}` | 否 | 5s；“未找到该商铺或暂不可用” | 电话最小展示；可 2 次；可实现。 |
| `get_discounted_goods` | 商品直接促销；`shop_id?`、分页 | 需要原价、活动价、窗口、规则；当前无接口/字段 | 否 | 不调用；“当前没有可靠的商品直接优惠数据” | 无；不可连续；待补充促销模型和只读接口。 |
| `get_nearby_shops` | 附近店；`longitude,latitude,radius,category_id?,page,size` | 店铺和距离；`GET /api/explore/shops/nearby` | 否 | 8s；“附近商铺暂不可用，可改为搜索商铺” | 精确坐标敏感，不入日志/会话；可 2 次；可实现。 |
| `get_hot_explore_posts` | 热门探店；`shop_id?,page,size` | 已发布探店、点赞/评论摘要；`GET /api/explore/posts?sort=hot` | 否 | 5s；“热门探店内容暂不可用” | 作者信息按现有 VO；可 2 次；可实现。 |

## 登录用户实时工具

| 工具 | 场景与输入 | 返回/对应 Spring Boot | 登录 | 超时与失败提示 | 敏感/连续调用/实现条件 |
|---|---|---|---:|---|---|
| `get_my_profile` | 我是谁、资料是否完整；无身份参数 | 当前用户安全资料；`GET /api/user/me` | 是 | 5s；“登录已失效，请重新登录” | 姓名/手机号；可 1 次；可实现。 |
| `get_my_dorm_info` | 我的宿舍；无身份参数 | 脱敏学生/入住/寝室/床位；`GET /api/student/dorm/me` | 是 | 5s；“暂无法查询当前入住信息” | 高敏感；可 1 次；可实现。 |
| `get_my_coupon_wallet` | 我的券；`status?,page,size` | 用户券状态、金额、店铺 ID、到期时间；`GET /api/coupons/mine` | 是 | 5s；“暂无法查询我的优惠券” | 个人消费信息；可 2 次；可实现。 |
| `get_my_recent_orders` | 最近订单；`status?,page,size` | 当前用户订单摘要；`GET /api/orders` | 是 | 5s；“暂无法查询我的订单” | 高敏感；可 2 次；可实现。 |
| `get_my_order_detail` | 某订单详情；`order_id` | 当前用户订单明细；`GET /api/orders/{orderId}` | 是 | 5s；“未找到该订单或无权访问” | 高敏感地址/电话；可 1 次；可实现。 |
| `get_my_default_address` | 默认配送地址；无身份参数 | 从 `GET /api/addresses` 的当前用户结果筛选 `isDefault=1` | 是 | 5s；“暂无法查询默认地址” | 高敏感地址/电话；可 1 次；部分，建议专用只读接口。 |

## 知识问答工具

| 工具 | 场景与输入 | 返回/对应来源 | 登录 | 超时与失败提示 | 敏感/连续调用/实现条件 |
|---|---|---|---:|---|---|
| `search_campus_guide` | 校园生活问题；`query` | 带标题、版本、生效日和片段的审核知识库 | 视资料 | 5s；“校园指南知识库尚未启用” | 无；1 次；待补充正式资料和索引。 |
| `search_dormitory_rules` | 宿舍规章；`query` | 受审核的正式规章及出处 | 视资料 | 5s；“宿舍规则资料尚未纳入知识库” | 可能含管理联系信息；1 次；待补充正式规章。 |
| `search_system_help` | 系统使用帮助；`query` | 策展 Markdown、文档版本与片段 | 否 | 5s；“系统帮助知识库尚未启用” | 无；1 次；待补充清洗、切片和评测。 |

工具不得接收 `userId`、`studentId`、学号、管理员 Token、原始 SQL、URL 或任意 HTTP 方法。任何未来写工具另建目录、默认禁用，并强制预览与二次确认。
