# 订单状态机与候选迁移（2026-07-23）

## 当前边界

当前真实订单接口只有 `POST /api/orders`，它创建普通订单并写入 `PENDING_PAY`。本轮不实现支付、用户取消、库存恢复、管理员接单、配送、完成、定时扫描、优惠券、Redis Stream、WebSocket、报表或订单页面。

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
