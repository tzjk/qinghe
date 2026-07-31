package com.qinghe.life;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.vo.PublicUserSummaryVO;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Offline safety matrix: every assertion reads only checked-in source/SQL or pure value objects.
 * It deliberately has no Spring context, network client, Redis client, or JDBC connection.
 */
class ExploreSocialPhase2StaticSafetyTest {
    @TestFactory Stream<DynamicTest> phase2SafetyMatrix() throws IOException {
        String sql = text("src/main/resources/sql/explore_social_increment.sql");
        String signIn = text("src/main/java/com/qinghe/life/service/impl/SignInServiceImpl.java");
        String follow = text("src/main/java/com/qinghe/life/service/impl/FollowServiceImpl.java");
        String feed = text("src/main/java/com/qinghe/life/service/FollowingFeedService.java");
        String explore = text("src/main/java/com/qinghe/life/service/impl/ExploreServiceImpl.java");
        String blogs = text("../frontend/src/views/BlogsView.vue");
        String profile = text("../frontend/src/views/ProfileView.vue");
        List<DynamicTest> tests = new ArrayList<DynamicTest>();
        addContains(tests, sql, "sql only creates qh_follow", "CREATE TABLE IF NOT EXISTS qh_follow");
        addFalse(tests, sql, "sql has no DROP", "DROP ");
        addFalse(tests, sql, "sql has no TRUNCATE", "TRUNCATE");
        addFalse(tests, sql, "sql has no destructive update", "UPDATE qh_");
        addContains(tests, sql, "sql uses bigint user_id", "user_id BIGINT NOT NULL");
        addContains(tests, sql, "sql uses bigint follow target", "follow_user_id BIGINT NOT NULL");
        addContains(tests, sql, "sql has unique relation", "UNIQUE KEY uk_qh_follow_user_target (user_id, follow_user_id)");
        addContains(tests, sql, "sql indexes user timeline", "KEY idx_qh_follow_user_time (user_id, created_at)");
        addContains(tests, sql, "sql indexes follower timeline", "KEY idx_qh_follow_target_time (follow_user_id, created_at)");
        addContains(tests, sql, "sql is InnoDB utf8mb4", "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        addContains(tests, sql, "sql has created timestamp", "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
        addContains(tests, sql, "sql has updated timestamp", "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
        addContains(tests, signIn, "sign in uses Shanghai zone", "ZoneId.of(\"Asia/Shanghai\")");
        addContains(tests, signIn, "sign in uses day offset", "now.getDayOfMonth() - 1L");
        addContains(tests, signIn, "sign in uses SETBIT", ".setBit(key");
        addContains(tests, signIn, "sign in reads GETBIT", ".getBit(key");
        addContains(tests, signIn, "sign in counts BITCOUNT", ".bitCount(");
        addContains(tests, signIn, "sign in reads BITFIELD current days", "BitFieldType.unsigned(today)");
        addContains(tests, signIn, "sign in returns zero for unsigned today", "if ((bits & 1L) == 0L) return 0");
        addContains(tests, signIn, "sign in rejects future months", "month.isAfter(now)");
        addContains(tests, signIn, "sign in limits to twelve months", "now.minusMonths(11)");
        addContains(tests, signIn, "sign in maps Redis failure to 503", "new BusinessException(503, \"签到服务暂不可用\")");
        addContains(tests, signIn, "sign in identity comes from UserContext", "UserContext.getUserId()");
        addContains(tests, follow, "follow has transaction", "@Transactional(rollbackFor = Exception.class) public FollowCountsVO follow");
        addContains(tests, follow, "unfollow has transaction", "@Transactional(rollbackFor = Exception.class) public FollowCountsVO unfollow");
        addContains(tests, follow, "follow handles database duplicate", "catch (DuplicateKeyException ignored)");
        addContains(tests, follow, "follow blocks self relation", "userId.equals(targetUserId)");
        addContains(tests, follow, "follow validates public target", "目标用户不存在或不可公开");
        addContains(tests, follow, "follow cache mutation is after commit", "registerSynchronization");
        addContains(tests, follow, "follow cache rebuild has user lock", "RedisKeys.followCacheLock(userId)");
        addContains(tests, follow, "follow cache has ttl jitter", "ThreadLocalRandom.current().nextLong(0, 121)");
        addContains(tests, follow, "follow cache has loaded marker", "followingsLoaded");
        addContains(tests, follow, "common uses SINTER", ".intersect(RedisKeys.followings(userId)");
        addContains(tests, follow, "common has database fallback", "databaseIntersection(userId, targetUserId)");
        addContains(tests, follow, "common result has limit", "COMMON_LIMIT = 100");
        addContains(tests, follow, "follow list pages in database", "followMapper.selectPage(new Page<Follow>");
        addContains(tests, follow, "cache failure has metric", "follow_cache_update_total");
        addContains(tests, feed, "feed uses reverse score range", "reverseRangeByScoreWithScores");
        addContains(tests, feed, "feed bounds page size", "Math.min(50, requestedSize)");
        addContains(tests, feed, "feed preserves Redis order", "for (Long id : ids)");
        addContains(tests, feed, "feed filters published posts", "\"PUBLISHED\".equals(post.getPostStatus())");
        addContains(tests, feed, "feed rechecks current following", "followingFromDatabase(viewerId)");
        addContains(tests, feed, "feed has MySQL fallback", "fallbackFeed(userId, ceiling, skip, size)");
        addContains(tests, feed, "feed fallback caps follow set", "following.size() > 500");
        addContains(tests, feed, "feed pipeline has fixed batch", "PUSH_PIPELINE_BATCH_SIZE = 200");
        addContains(tests, feed, "feed pipeline writes post ID only", "String.valueOf(post.getId()).getBytes");
        addContains(tests, feed, "feed capacity trimming removes oldest ranks", "ZREMRANGEBYRANK");
        addContains(tests, feed, "feed backfill only published", ".eq(ExplorePost::getPostStatus, \"PUBLISHED\")");
        addContains(tests, feed, "liker cache is isolated version", "exploreLikers(postId)");
        addContains(tests, feed, "liker rebuild has stable id order", ".orderByAsc(ExploreLike::getId)");
        addContains(tests, feed, "liker response restricts range to five", ".range(key, 0, 4)");
        addContains(tests, explore, "post publish schedules feed after commit", "followingFeedService.publishedAfterCommit(post)");
        addContains(tests, explore, "like schedules stable liker member", "followingFeedService.likedAfterCommit(id, like)");
        addContains(tests, explore, "unlike removes stable liker member", "followingFeedService.unlikedAfterCommit(id, like)");
        addContains(tests, blogs, "front end has three feed tabs", "label=\"following\"");
        addContains(tests, blogs, "front end prevents duplicate follow submit", "followSubmitting[post.userId]");
        addContains(tests, blogs, "front end deduplicates following records", "function mergePosts");
        addContains(tests, blogs, "front end ignores stale requests", "feedRequestVersion");
        addContains(tests, blogs, "front end shows at most five avatars", "post.topLikers.slice(0, 5)");
        addFalse(tests, blogs, "front end does not render post HTML", "v-html");
        addContains(tests, profile, "front end calendar uses Shanghai month", "timeZone: 'Asia/Shanghai'");
        addContains(tests, profile, "front end disables signed-in button", ":disabled=\"signStatus.signedToday\"");
        addFalse(tests, profile, "front end does not render profile HTML", "v-html");
        tests.add(DynamicTest.dynamicTest("public social VO excludes sensitive fields", () -> assertNoPrivateFields()));
        tests.add(DynamicTest.dynamicTest("runtime social keys stay namespaced and contain no phone", () -> assertKeyBoundary()));
        return tests.stream();
    }

    private void addContains(List<DynamicTest> tests, String value, String name, String expected) { tests.add(DynamicTest.dynamicTest(name, () -> assertTrue(value.contains(expected)))); }
    private void addFalse(List<DynamicTest> tests, String value, String name, String forbidden) { tests.add(DynamicTest.dynamicTest(name, () -> assertFalse(value.toUpperCase(Locale.ROOT).contains(forbidden.toUpperCase(Locale.ROOT))))); }
    private String text(String relative) throws IOException { Path path = Paths.get(relative); return new String(Files.readAllBytes(path), StandardCharsets.UTF_8); }
    private void assertNoPrivateFields() { List<String> forbidden = Arrays.asList("phone", "realname", "studentno", "dorm", "address", "token", "password"); for (Field field : PublicUserSummaryVO.class.getDeclaredFields()) assertFalse(forbidden.contains(field.getName().toLowerCase(Locale.ROOT))); }
    private void assertKeyBoundary() { RedisKeys.configureNamespace("qh:test:social:run:"); for (String key : Arrays.asList(RedisKeys.signIn(1L, "2026-07"), RedisKeys.followings(1L), RedisKeys.followers(1L), RedisKeys.exploreLikers(2L), RedisKeys.followingFeed(1L))) { assertTrue(key.startsWith("qh:test:social:run:")); assertFalse(key.contains("phone")); } }
}
