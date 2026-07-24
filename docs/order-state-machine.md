# 订单状态机与候选迁移（2026-07-23）

## 当前边界

当前订单链路已包含创建、查询、模拟支付、用户取消、管理员固定流转和超时取消。订单创建写入 `PENDING_PAY`；本轮不实现优惠券、Redis Stream、WebSocket、缓存、报表、真实支付或骑手系统。

`qh_order.status` 保持 `VARCHAR(20)`；数据库中的既有 `PENDING_PAY` 是唯一待支付编码，严禁另行引入 `PENDING_PAYMENT`。

## 统一状态定义

| 编码 | 中文名称 | 本轮状态 |
|---|---|---|
| `PENDING_PAY` | 待支付 | 当前订单创建使用 |
| `PAID` | 已支付 | 仅定义，未实现接口 |
| `ACCEPTED` | 已接单 | 仅定义，未实现接口 |
| `DELIVERING` | 配送中 | 仅定义，未实现接口 |
| `COMPLETED` | 已完成 | 仅定义，未实现接口 |
| `CANCELLED` | 已取消 | 仅定义，未实现接口 |

合法流转仅为：

```text
PENDING_PAY -> PAID -> ACCEPTED -> DELIVERING -> COMPLETED
PENDING_PAY -> CANCELLED
```

Java 侧统一由 `OrderStatus` 提供状态编码、中文名称和 `canTransitionTo` 判断。持久化仍写入编码字符串，避免改变既有数据库值。

## 候选数据库增量

候选文件为 `backend/src/main/resources/sql/order_lifecycle_schema_increment.sql`，只提出以下列和索引：

- `pay_time DATETIME NULL`
- `accepted_time DATETIME NULL`
- `delivery_time DATETIME NULL`
- `pay_expire_time DATETIME NULL`
- `(status, pay_expire_time)` 联合索引，用于后续扫描待支付超时订单

脚本不新增 `goods_amount`，不删除或重命名字段，也不重复添加既有候选 `cancel_reason`、`cancel_time`、`completed_time`。

执行前用户必须在 DataGrip 手工运行：

```sql
SHOW CREATE TABLE qh_order;
SHOW INDEX FROM qh_order;
```

本项目通过 DataGrip 人工执行迁移，不伪装为 Flyway 自动迁移。尚未得到真实结果前，候选列不是运行库事实，`Order` 实体不映射它们，相关生命周期集成测试也不运行。

## 金额和日志事务约束

- `total_amount` 是商品原始总额；`pay_amount` 是最终应付金额；不得新增 `goods_amount`。
- 后续取消订单必须将“条件更新状态、恢复商品库存、写取消成功日志”置于同一个订单业务 `REQUIRED` 事务。
- 通用 AOP 操作日志当前使用 `REQUIRES_NEW`。关键订单事务应直接写现有 `qh_operate_log`，并禁止同一关键方法再触发 AOP 成功日志，避免重复记录或主事务回滚后仍留下成功日志。
- 不新增 `order_log` 或任何其他模块日志表。

## 核心闭环实现（2026-07-24）

- 新订单状态为 `PENDING_PAY`，服务端写入 `create_time` 和默认 15 分钟 `pay_expire_time`。支付成功仅能由 `PENDING_PAY` 条件更新为 `PAID` 并写 `pay_time`。
- 用户取消和超时取消均为 `PENDING_PAY -> CANCELLED`。只有状态条件更新受影响行数为 1 的事务才能按 `qh_order_item` 恢复对应商品库存并直接写入一条现有 `qh_operate_log` 成功记录；重复取消或多次扫描不会重复恢复。
- 管理员固定流转为 `PAID -> ACCEPTED -> DELIVERING -> COMPLETED`，每一步以预期状态条件更新并写对应时间与一条操作日志。普通用户令牌不能进入 `/api/admin/**`。
- Spring Task 只获取 `qh:lock:order:timeout-cancel` 后调用可直接测试的批处理服务。Redisson 客户端复用既有 `spring.redis` host、port、password 和 database；锁使用配置化 `tryLock(waitTime, leaseTime, SECONDS)`，未获锁或锁异常直接结束本轮。
- 扫描条件固定为 `status = PENDING_PAY AND pay_expire_time <= 当前时间`，默认每批 100 条。每笔取消通过独立 Spring 事务 Service 执行 `id + PENDING_PAY + pay_expire_time <= 当前时间` 条件更新；更新成功后才恢复订单明细对应库存并写一条统一操作日志。支付更新同时要求仍未过期，因此支付与超时取消并发只会有一个状态变更成功。
- 任一订单的状态、库存或直接日志写入失败均回滚该订单事务；订单/明细保留，购物车不恢复。重复扫描只会得到 0 行条件更新，不能重复恢复库存。
