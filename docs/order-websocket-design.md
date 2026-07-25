# 订单 WebSocket 通知设计（2026-07-24）

## 定位

WebSocket 只增强订单状态变化的实时体验；`qh_order` 与既有 HTTP 订单接口始终是状态事实来源。发送失败、断线或漏消息不回滚业务事务，页面首次打开或重连后必须通过 HTTP 刷新。

## 鉴权与通道

- 用户：`/ws/orders/user`，握手子协议为 `qh-user.{token}`。
- 管理员：`/ws/orders/admin`，握手子协议为 `qh-admin.{token}`。
- 握手拦截器查询 `qh:login:token:{token}` 或 `qh:admin:token:{token}` Redis Hash，拒绝匿名、无效、过期或角色不匹配连接；Token 不进入 URL，也不写日志。
- 连接身份只由握手会话属性确定。不存在客户端提供 `userId` 的订阅协议，因此用户无法订阅其他用户，普通用户不能订阅管理员通道。

## 消息与时机

```json
{
  "messageType": "ORDER_PAID",
  "orderId": 101,
  "orderNo": "QH...",
  "orderStatus": "PAID",
  "statusText": "已支付",
  "occurredAt": "2026-07-24T21:00:00",
  "summary": "订单已支付"
}
```

订单服务和取消服务只发布统一订单事件；监听器在 `AFTER_COMMIT` 阶段将事件转换为用户和管理员消息。创建对应 `ORDER_CREATED` 与 `ADMIN_NEW_ORDER`；取消对应用户取消/超时取消类型及 `ADMIN_ORDER_CANCELLED`；支付和管理员流转对应用户状态类型及 `ADMIN_ORDER_STATUS_CHANGED`。

## 连接与前端

- 注册表以并发 Map 保存每个用户的全部 Session，管理员也使用独立并发容器；关闭、传输异常、关闭 Session 和发送异常均会清理。
- Vue 订单页使用共享连接工具，在挂载时连接、销毁时关闭；登出 Store 也主动关闭连接。
- 断线最多重试 5 次，延迟按 1s、2s、4s、8s 上限递增。重复帧按类型、订单、状态和发生时间去重；状态顺序检查阻止旧消息覆盖新状态。管理员列表收到事件时通过一次 HTTP 刷新对齐。

## 部署边界

当前实现仅覆盖单实例。多实例需要 Redis Pub/Sub 或消息代理传播提交后的事件，并由每个实例向其本地连接发送；离线消息、持久化通知和补偿不在本轮范围内。
