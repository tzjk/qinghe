# Explore Social Phase 2 离线测试完成记录

## 结论

离线业务行为矩阵已完成：59 个 Mockito 隔离的业务行为场景均通过，超过 45 项门槛。静态 DynamicTest 65 项另行统计，不计入业务行为数；真实 Redis/MySQL 入口默认跳过，未连接任何真实基础设施。

## 最终验证

| 验证 | 实际结果 |
|---|---|
| 后端主代码 compile | `mvn "-Dmaven.repo.local=C:/Users/28402/.m2/repository" -DskipTests compile`：BUILD SUCCESS |
| 所有离线社交测试 | 126 passed、1 skipped、0 failed、0 errors（127 run） |
| Mockito 业务行为 | 59 passed |
| 非 Mockito 业务行为 | 0 passed |
| DynamicTest 静态审计 | 65 passed |
| 普通静态契约补充 | 2 passed，不计入业务行为 |
| 受控真实入口 | 1 skipped，未设置 `QINGHE_REAL_SOCIAL_TESTS=true` |
| 前端生产构建 | `D:/develop/NodeJS/npm.cmd run build`：1778 modules transformed，成功；仅既有第三方 PURE 注释和 bundle-size 警告 |

## 覆盖判定

- 签到：SETBIT、GETBIT、BITCOUNT、BITFIELD 连续值、漏签/未签到、月份隔离、错误月份和 Redis 503 均已通过。
- 关注与共同关注：数据库事实写入、afterCommit Set 更新、缓存重建/锁回退、SINTER/MySQL 回退、公开响应脱敏均已通过。
- 点赞前五：提交后 ZADD/ZREM、最早五人、created_at/id 稳定顺序、缓存重建、Redis 回退、likedByMe 和公开字段均已通过。
- Feed：afterCommit 推送、200+1 Pipeline、postId member、容量裁剪 `ZREMRANGEBYRANK 0,-101`、同时间戳 offset、顺序恢复、惰性过滤、MySQL 回退和回填均已通过。

## 生产代码问题

本轮没有修改生产代码。测试执行过程中修正了测试夹具的 Mock 返回值、锁持有状态和 Spring Data Redis `execute` 参数捕获；没有发现需要生产修复的业务缺陷。

## 安全与边界

- 未连接 Redis、MySQL、网络或真实接口；未启动 Spring Boot、Redis、MySQL 或 Vue。
- 未执行 SQL。`explore_social_increment.sql` 经静态审查：仅 `CREATE TABLE IF NOT EXISTS qh_follow`，无 DROP/TRUNCATE/数据修改，MySQL 8 兼容，唯一索引 `uk_qh_follow_user_target` 与两条时间索引存在；已有 `qh_follow` 时必须停止并人工比对，脚本最多人工执行一次。
- 未修改 `agent-service`、订单、支付、秒杀、管理端，亦未新增私信、双向好友、积分、推荐、RabbitMQ 或 Kafka。

## 下一步

技术条件已满足“可进入隔离环境真实联调”，但仍须由人工提供独立 Redis namespace、测试 MySQL 数据与执行授权，并按 09 号清单手工核验/执行候选 SQL。当前 Git 提交需先经人工审阅并获得明确 Git 授权；本轮未执行任何 Git 写操作。
