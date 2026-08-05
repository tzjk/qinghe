# Stream DLQ、重试与保留

失败按集中错误分类处理。字段非法和已确认业务冲突直接死信；数据访问与未知持久化异常采用 2 秒起步、30 秒封顶的指数退避。retry 是按 messageId 的 TTL String，记录次数、首失败时间和下一次可处理时间；没有 `Thread.sleep` 阻塞消费者。

达到上限时，`coupon-seckill-dlq.lua` 以 `originalMessageId` 作为去重索引写入 `qh:stream:coupon:claim:dlq`，字段包括 retryCount、failureCode、failureSummary、firstFailedAt、deadLetteredAt、consumer、orderId、couponId、userId。DLQ 成功后才 ACK 主消息；DLQ 失败保留 Pending。R1 不自动重放 DLQ。

主/死信 Stream 分别配置建议最大长度、告警阈值和保留期，但 R1 不执行 XTRIM、MAXLEN、DEL 或自动清空。人工治理必须先检查 Pending/消费者状态，再按已确认 ACK 水位实施。
