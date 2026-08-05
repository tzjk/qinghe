# 签到 Bitmap 设计

Key 为 `sign:{userId}:{yyyyMM}`，月份与用户隔离，日期 offset 为 `dayOfMonth - 1`。服务固定使用 `Asia/Shanghai`。

签到使用 `SETBIT`，重复写入返回幂等成功；状态用 `GETBIT`，月累计用 `BITCOUNT`，连续天数用 `BITFIELD` 读取截至今天的位段后从低位连续计算。今天未签到时连续天数返回 0。只允许当前月及近 12 个月，禁止未来月。

Redis 不可用时返回 503，不伪造成功。当前没有积分、优惠券或任何奖励；未来涉及权益时需补充 MySQL 奖励流水。
