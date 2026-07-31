# 探店社交离线业务行为测试矩阵

## 口径

- 仅“实际调用 Controller、Service 或核心业务方法，并以 Mockito 隔离 Mapper、Redis、Redisson 或事务依赖，且断言结果、异常或交互”的测试计入本矩阵的 45 项目标。
- `@TestFactory` / `DynamicTest` 的源码、SQL 和字段审计保留为静态补充，单独列示，绝不计入业务行为场景数。
- 真实 Redis/MySQL 测试仅保留显式开关入口；本轮不运行。

## 开始时的既有测试清单

| 分类 | 测试类 | 方法或工厂 | Mockito / 外部资源 | 计入 45 项 | 状态 |
|---|---|---|---|---:|---|
| A. Mockito 业务行为 | `SignInServiceImplTest` | `rejectsUnauthenticatedSignIn`、`rejectsFutureAndMalformedMonthBeforeRedisRead`、`firstSignInUsesTodayOffsetAndIsNotDuplicate`、`redisFailureNeverReportsSuccess` | Mock `StringRedisTemplate`、`ValueOperations`、Redis connection | 4 | 已有，覆盖不足 |
| B. 普通业务行为 | 无独立业务测试类 | 无 | 无 | 0 | 待补 |
| C. 静态审计 | `ExploreSocialContractTest` | 2 个 `@Test` | 反射与纯 Key 值 | 0 | 保留，不计入 |
| C. 静态审计 | `ExploreSocialPhase2StaticSafetyTest` | `phase2SafetyMatrix` | 源码/SQL 文本与反射 | 0 | 保留，不计入 |
| D. 真实 Redis/MySQL 集成 | `ExploreIntegrationTest`、`ExploreInteractionIntegrationTest`、`ExploreNearbyShopIntegrationTest`、`ExploreImageUploadIntegrationTest` | 各现有集成场景 | Spring、真实基础设施 | 0 | 本轮不运行 |
| E. 跳过入口 | `RealSocialIntegrationReservedTest` | `requiresDedicatedPerRunRedisNamespace` | 显式环境开关 | 0 | 默认 skipped |

## 本轮分批检查点

| 批次 | 目标业务场景 | 测试类 | 结果 | 尚缺 |
|---|---:|---|---|---|
| 第一批：签到 | 12（另有既有 4 项） | `SignInServiceBehaviorTest`、`SignInServiceImplTest` | `mvn "-Dtest=SignInServiceImplTest,SignInServiceBehaviorTest" test`：16/0/0/0 | 已完成：首次 SETBIT、重复幂等、GETBIT、BITCOUNT、连续/漏签/未签到、跨月 Key、未来/非法月份、状态/日历 Redis 503；闰年二月需在其处于近 12 个月窗口时作为真实时钟回归补充 |
| 第二批：关注与共同关注 | 18 | `FollowServiceBehaviorTest`（14）、`CommonFollowBehaviorTest`（4） | `mvn "-Dtest=FollowServiceBehaviorTest,CommonFollowBehaviorTest" test`：18/0/0/0 | 已完成：关注/取消关注提交后 SADD/SREM、重复幂等、自关注/缺失目标拒绝、Redis 失败不回滚、空 loaded、缓存重建、锁失败回退、列表/计数、SINTER/MySQL 回退、公开字段序列化 |
| 第三批：点赞前五 | 10 | `ExploreTopLikersBehaviorTest` | `mvn "-Dtest=ExploreTopLikersBehaviorTest" test`：10/0/0/0 | 已完成：ZADD/ZREM、最早五位、缓存重建、同时间 ID 稳定、Redis DB 回退、空 loaded、点赞后名单、likedByMe 与公开字段 |
| 第四批：Feed | 15 | `FollowingFeedBehaviorTest` | `mvn "-Dtest=FollowingFeedBehaviorTest" test`：15/0/0/0；最终全量社交回归将含修订的 200+1 分批断言 | 已完成：提交后投递、无/非公开帖子、200 批 Pipeline、member、容量裁剪、滚动游标、同分 offset、顺序、惰性过滤、回退和回填 |

## 当前结论

开始时的 71 passed 中，约 65 项是静态 `DynamicTest` 审计；它们不是 Mockito 业务行为测试。四批已完成 59 个实际 Mock 隔离的业务行为场景（签到 16、关注/共同关注 18、点赞 10、Feed 15），已经达到 45 项门槛；是否可进入隔离真实联调仍取决于最终 compile、全体离线测试、前端 build 和约束核验。

## 已执行的真实业务场景明细

| 编号 | 领域 | 场景 | 测试类 / 方法 | Mock 依赖 | 核心断言 | 结果 |
|---:|---|---|---|---|---|---|
| B01 | 签到 | 未登录拒绝 | `SignInServiceImplTest#rejectsUnauthenticatedSignIn` | Redis Template | 401 业务异常 | passed |
| B02 | 签到 | 未来/格式错误月份拒绝 | `SignInServiceImplTest#rejectsFutureAndMalformedMonthBeforeRedisRead` | Redis Template | 400 业务异常 | passed |
| B03 | 签到 | 首次签到 | `SignInServiceImplTest#firstSignInUsesTodayOffsetAndIsNotDuplicate` | Redis Template、ValueOps、连接 | 返回签到状态 | passed |
| B04 | 签到 | 写 Redis 异常 | `SignInServiceImplTest#redisFailureNeverReportsSuccess` | Redis Template、ValueOps | 503 业务异常 | passed |
| B05 | 签到 | 首次 SETBIT/day-1 | `SignInServiceBehaviorTest#firstSignInWritesTodayBitWithZeroBasedOffset` | Redis Template、ValueOps、连接 | Key 与零基偏移 | passed |
| B06 | 签到 | 重复签到幂等 | `SignInServiceBehaviorTest#duplicateSignInReturnsStatusAndRecordsDuplicateOutcome` | 同上 | duplicate 指标/状态 | passed |
| B07 | 签到 | 今日状态与 BITCOUNT | `SignInServiceBehaviorTest#statusReadsGetBitAndMonthBitCountForCurrentUser` | 同上 | GETBIT、月累计 | passed |
| B08 | 签到 | 连续三天 | `SignInServiceBehaviorTest#streakCountsConsecutiveLowOrderBits` | Redis connection | BITFIELD=111 返回 3 | passed |
| B09 | 签到 | 中间漏签 | `SignInServiceBehaviorTest#streakStopsAtFirstMissingDay` | Redis connection | BITFIELD=101 返回 1 | passed |
| B10 | 签到 | 今天未签到 | `SignInServiceBehaviorTest#streakIsZeroWhenTodayIsNotSigned` | ValueOps、连接 | 连续值 0 | passed |
| B11 | 签到 | 查询历史月 | `SignInServiceBehaviorTest#calendarUsesRequestedHistoricalMonthKeyAndEachDayOffset` | ValueOps、连接 | 指定 Key、首末日偏移 | passed |
| B12 | 签到 | 跨月隔离 | `SignInServiceBehaviorTest#currentAndPreviousMonthUseDifferentRedisKeys` | ValueOps、连接 | 两个不同 Key | passed |
| B13 | 签到 | 未来月份 | `SignInServiceBehaviorTest#futureMonthIsRejectedBeforeAnyRedisCall` | Redis Template | 400 且不访问 Redis | passed |
| B14 | 签到 | 非法月份格式 | `SignInServiceBehaviorTest#malformedMonthIsRejectedBeforeAnyRedisCall` | Redis Template | 400 且不访问 Redis | passed |
| B15 | 签到 | status Redis 故障 | `SignInServiceBehaviorTest#redisFailureDuringStatusMapsToServiceUnavailable` | ValueOps | 503/失败指标 | passed |
| B16 | 签到 | calendar Redis 故障 | `SignInServiceBehaviorTest#calendarRedisFailureMapsToServiceUnavailable` | ValueOps | 503 | passed |
| B17 | 关注 | 关注提交后写 Set | `FollowServiceBehaviorTest#followWritesDatabaseThenAddsBothMembershipSetsAfterCommit` | Follow/User Mapper、SetOps、事务 | 插入后 SADD/backfill | passed |
| B18 | 关注 | 重复关注 | `FollowServiceBehaviorTest#duplicateFollowIsIdempotentAndDoesNotRefreshDerivedSets` | Mapper、SetOps、事务 | 无缓存写 | passed |
| B19 | 关注 | 取消关注提交后清理 | `FollowServiceBehaviorTest#unfollowDeletesDatabaseThenRemovesBothMembershipSetsAfterCommit` | Mapper、SetOps、事务 | delete 后 SREM/cleanup | passed |
| B20 | 关注 | 重复取消 | `FollowServiceBehaviorTest#repeatedUnfollowDoesNotTouchDerivedSets` | Mapper、SetOps、事务 | 无缓存写 | passed |
| B21 | 关注 | 禁止关注自己 | `FollowServiceBehaviorTest#selfFollowIsRejectedBeforeDatabaseWrite` | Mapper | 400 且无 insert | passed |
| B22 | 关注 | 目标不存在 | `FollowServiceBehaviorTest#missingTargetIsRejectedBeforeDatabaseWrite` | User/Follow Mapper | 404 且无 insert | passed |
| B23 | 关注 | Redis 失败不回滚事实 | `FollowServiceBehaviorTest#redisFailureAfterCommittedFollowKeepsDatabaseResult` | Mapper、SetOps、事务 | 结果保留/失败指标 | passed |
| B24 | 关注 | 空关注集合 loaded | `FollowServiceBehaviorTest#emptyFollowingCacheGetsLoadedMarkerWithoutSetMembers` | Mapper、Redis、RLock | loaded 写入、无 SADD | passed |
| B25 | 关注 | 缓存缺失重建 | `FollowServiceBehaviorTest#missingFollowingSetRebuildsFromDatabaseAndWritesMembers` | Mapper、Redis、RLock | DB→SADD/TTL | passed |
| B26 | 关注 | 锁竞争回退数据库 | `FollowServiceBehaviorTest#cacheLockContentionFallsBackToDatabaseWithoutWrites` | Mapper、Redis、RLock | 返回 DB、无缓存写 | passed |
| B27 | 关注 | 我的关注列表 | `FollowServiceBehaviorTest#followingListReturnsPublicProjectionAndPageMetadata` | Mapper、SetOps | 分页/公开投影 | passed |
| B28 | 关注 | 我的粉丝列表 | `FollowServiceBehaviorTest#followerListReturnsUsersWhoFollowTarget` | Mapper、SetOps | 粉丝身份/分页 | passed |
| B29 | 关注 | 关注/粉丝计数 | `FollowServiceBehaviorTest#relationReturnsFollowingAndFollowerCountsForTarget` | Follow/User Mapper | 三个计数结果 | passed |
| B30 | 安全 | 关注响应 PII | `FollowServiceBehaviorTest#publicFollowResponseSerializationDoesNotLeakPrivateUserFields` | Mapper、SetOps | JSON 无 phone/passwordHash | passed |
| B31 | 共同关注 | SINTER | `CommonFollowBehaviorTest#commonFollowUsesRedisIntersectionAndReturnsSortedPublicUsers` | Mapper、SetOps | 交集排序/公开用户 | passed |
| B32 | 共同关注 | Redis 异常 DB 回退 | `CommonFollowBehaviorTest#redisIntersectionFailureFallsBackToDatabaseIntersection` | Mapper、SetOps | 交集回退/指标 | passed |
| B33 | 共同关注 | 停用用户过滤 | `CommonFollowBehaviorTest#commonFollowOmitsDisabledUserFromPublicResult` | Mapper、SetOps | 空公开结果 | passed |
| B34 | 安全 | 身份来自 UserContext | `CommonFollowBehaviorTest#commonFollowUsesCurrentUserContextRatherThanClientSuppliedIdentity` | SetOps | SINTER 使用当前用户 1 | passed |
| B35 | 点赞前五 | 点赞 ZADD NX | `ExploreTopLikersBehaviorTest#likeAfterCommitUsesZaddNxWithStableLikeMember` | ZSetOps | Key/member/score | passed |
| B36 | 点赞前五 | 取消 ZREM | `ExploreTopLikersBehaviorTest#unlikeAfterCommitUsesZremForSameStableMember` | ZSetOps | 删除稳定 member | passed |
| B37 | 点赞前五 | 最早五人顺序 | `ExploreTopLikersBehaviorTest#cachedTopLikersReturnsEarliestFiveInZsetOrder` | Redis、ZSetOps、User Mapper | 前五顺序 | passed |
| B38 | 点赞前五 | 缺失缓存重建/同时间 ID 稳定 | `ExploreTopLikersBehaviorTest#missingLikerZsetRebuildsFromDatabaseCreatedAtThenIdOrder` | Like Mapper、Redis、RLock、ZSetOps | createdAt/id member 顺序 | passed |
| B39 | 点赞前五 | Redis 异常 DB 读取 | `ExploreTopLikersBehaviorTest#redisFailureReadsFirstFiveDirectlyFromDatabase` | Like/User Mapper、Redis | DB 前五/降级指标 | passed |
| B40 | 点赞前五 | 取消后名单变化 | `ExploreTopLikersBehaviorTest#unlikeChangesNextTopLikerReadWithoutChangingDatabaseLikeCountContract` | ZSetOps、User Mapper | ZREM 后名单 | passed |
| B41 | 点赞前五 | 空 loaded 防击穿 | `ExploreTopLikersBehaviorTest#emptyLoadedLikerCacheDoesNotRebuildDatabase` | Redis、ZSetOps、Like Mapper | 空结果/无 DB 查询 | passed |
| B42 | 安全 | 点赞公开字段 | `ExploreTopLikersBehaviorTest#topLikerResponseSerializesOnlyPublicFields` | Redis、ZSetOps、User Mapper | JSON 无 PII | passed |
| B43 | 点赞 | 登录 likedByMe/完整计数 | `ExploreTopLikersBehaviorTest#loggedDetailKeepsFullLikeCountAndMarksLikedByCurrentUser` | Post/Like/User/Shop Mapper、Feed/Follow Service | liked=true、likeCount=9、top5 | passed |
| B44 | 点赞 | 匿名 likedByMe | `ExploreTopLikersBehaviorTest#anonymousDetailDoesNotQueryCurrentUserLikeAndReturnsFalse` | 同上 | liked=false、无 like 查询 | passed |
| B45 | Feed 推送 | 提交前不投递 | `FollowingFeedBehaviorTest#publishedPostPushesToFollowersOnlyAfterTransactionCommit` | Post/Follow/Like/User/Shop Mapper、Redis、事务 | afterCommit 后 Pipeline | passed |
| B46 | Feed 推送 | 非 PUBLISHED 不投递 | `FollowingFeedBehaviorTest#unpublishedPostDoesNotPushToFollowers` | Follow Mapper、Redis | 无分页/无 Pipeline | passed |
| B47 | Feed 推送 | 无粉丝 | `FollowingFeedBehaviorTest#noFollowersEndsSafelyWithoutPipeline` | Follow Mapper、Redis | 安全空结束 | passed |
| B48 | Feed 推送 | 200+1 分批 Pipeline | `FollowingFeedBehaviorTest#followersArePushedInSeparatePipelineBatches` | Follow Mapper、Redis pipeline | 两次 Pipeline | passed |
| B49 | Feed 推送 | ZADD member | `FollowingFeedBehaviorTest#pipelineUsesPostIdAsZaddMember` | Redis connection/ZSet | member=postId | passed |
| B50 | Feed 推送 | 重复投递 | `FollowingFeedBehaviorTest#repeatedPushUsesSameMemberAndIsRedisIdempotent` | Redis connection/ZSet | 两次同 member | passed |
| B51 | Feed 推送 | 容量裁剪方向 | `FollowingFeedBehaviorTest#capacityTrimRemovesOldestRanksAndKeepsNewestRecords` | Redis connection | ZREMRANGEBYRANK 0,-101 | passed |
| B52 | Feed 滚动 | 首页游标 | `FollowingFeedBehaviorTest#firstScrollPageReturnsRecordsAndCursor` | Mapper、Redis ZSet | records/minTime/offset | passed |
| B53 | Feed 滚动 | 同时间 offset | `FollowingFeedBehaviorTest#sameTimestampPageUsesOffsetForNextCursor` | Mapper、Redis ZSet | offset=4 | passed |
| B54 | Feed 滚动 | Redis 顺序恢复 | `FollowingFeedBehaviorTest#redisTupleOrderIsRestoredInReturnedRecords` | Mapper、Redis ZSet | 51,50 顺序 | passed |
| B55 | Feed 过滤 | 删除帖子惰性清理 | `FollowingFeedBehaviorTest#deletedPostIsFilteredAndLazilyRemovedFromFeed` | Mapper、Redis ZSet | 空结果/ZREM | passed |
| B56 | Feed 过滤 | 取消关注作者过滤 | `FollowingFeedBehaviorTest#unfollowedAuthorIsFilteredAndLazilyRemoved` | Mapper、Redis ZSet | 空结果/ZREM | passed |
| B57 | Feed 过滤 | 非法 member | `FollowingFeedBehaviorTest#invalidMemberIsLazilyRemovedWithoutBreakingFeedShape` | Redis ZSet | 稳定空结构 | passed |
| B58 | Feed 回退 | Redis 失败 MySQL 回退 | `FollowingFeedBehaviorTest#redisFailureFallsBackToMysqlPosts` | Post/Follow/Like/User/Shop Mapper、Redis | 返回 MySQL 记录 | passed |
| B59 | Feed 回填 | 新关注回填 | `FollowingFeedBehaviorTest#backfillPushesOnlyReturnedPublishedPosts` | Post Mapper、Redis connection | 仅返回 PUBLISHED 帖子写入 | passed |

## 生产代码问题记录

本轮截至第四批没有生产代码修改；失败均为测试夹具的 Mock 配置或 Redis API 参数捕获不完整，修正后相应目标测试均通过。因此没有“测试暴露后修复的生产代码问题”。

## 最终执行统计

- Mockito 业务行为测试：59 passed（B01-B59）。
- 非 Mockito 业务行为测试：0 passed。
- DynamicTest 静态审计：65 passed（`ExploreSocialPhase2StaticSafetyTest`）。
- 普通静态契约补充：2 passed（`ExploreSocialContractTest`，不计入行为测试）。
- 受控真实 Redis/MySQL 集成入口：1 skipped（`RealSocialIntegrationReservedTest`，未设置启用环境变量）。
- 社交离线命令总计：126 passed，1 skipped，0 failed，0 errors（127 run）。
