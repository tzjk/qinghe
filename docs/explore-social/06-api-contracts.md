# API 契约

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/user/sign-in` | 当日幂等签到 |
| GET | `/api/user/sign-in/status` | 今日状态、月累计、连续天数 |
| GET | `/api/user/sign-in/calendar?month=yyyy-MM` | 月日历 |
| POST/DELETE | `/api/follows/{targetUserId}` | 关注/取消关注 |
| GET | `/api/follows/{targetUserId}/status` | 关系及目标计数 |
| GET | `/api/follows/me/following`、`/me/followers` | 当前用户社交列表 |
| GET | `/api/follows/{targetUserId}/common` | 共同关注 |
| GET | `/api/explore/feed/following` | `maxTime`、`offset` 滚动 Feed |

所有写接口及私有列表依赖登录拦截器，身份只从 `UserContext` 获取；没有客户端 currentUserId 参数。

`GET /api/explore/feed/following` 的 `size` 被限制为 1–50。结果包含 `records`、`minTime`、`offset`、`hasMore`；调用方必须把本次返回游标原样带入下一页。被隐藏、删除、禁用作者或已取消关注的旧成员会被过滤并可惰性删除，因此某页可少于请求数量，不会为凑满页面跨越未扫描成员。
