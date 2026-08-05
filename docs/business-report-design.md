# 管理员营业报表设计（2026-07-25）

## 范围与口径

- 仅管理员可读取 `/api/admin/reports/**`；管理员身份只由既有管理员 Bearer Token 写入的 `AdminContext` 提供。
- 使用 `Asia/Shanghai` 的自然日，数据库查询区间固定为 `[开始日 00:00, 结束日次日 00:00)`；默认最近 7 天，最多 90 天。
- `total_amount` 是商品原始总额，`pay_amount` 是优惠后的实付金额。营业额和销售统计只计 `PAID`、`ACCEPTED`、`DELIVERING`、`COMPLETED`；优惠额为 `total_amount - pay_amount`。`PENDING_PAY`、`CANCELLED` 不计营业额。
- 待处理数据按 `PENDING_PAY`、`PAID`、`ACCEPTED`、`DELIVERING` 分项展示；本轮没有含义不清的待处理总数，也不增加取消率字段。

## 实现选择

当前规模和既有表模型适合实时聚合：Mapper 直接使用 `COUNT`、`SUM`、`GROUP BY` 和订单/明细/店铺/优惠券关联查询；服务层只做参数校验、空值归零和日期补零，不把订单加载到 Java 内存统计。因此不新增 `qh_business_daily_report`、定时日报任务、缓存或迁移脚本。

趋势按订单 `create_time` 的自然日统计，反映当日创建订单在当前状态下的经营结果；报表数据库查询均以该时间范围为筛选条件。店铺销售额为有效订单 `pay_amount` 合计；商品销售额为有效订单明细 `subtotal` 合计，不对订单级优惠任意分摊。

## 索引结论

现有 `qh_order_item.order_id`、`qh_goods.shop_id`、`qh_shop` 店铺索引可支持关联；设计文档未登记适合日期范围与状态聚合的订单复合索引，也未登记用户券按订单核销统计的索引。数据量增大或 `EXPLAIN` 显示全表扫描时，由人工审核后执行以下候选 SQL（本轮未生成脚本、未执行 SQL）：

```sql
CREATE INDEX idx_qh_order_report_time_status_shop ON qh_order (create_time, status, shop_id);
CREATE INDEX idx_qh_user_coupon_order_status ON qh_user_coupon (order_id, status);
```

是否建立索引必须先在目标库核对同名/等价索引与 `EXPLAIN`，避免重复索引。实时今日数据不缓存；历史趋势暂无性能证据，不引入 TTL 缓存。
