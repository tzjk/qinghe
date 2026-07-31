package com.qinghe.life;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qinghe.life.entity.ExplorePost;
import com.qinghe.life.entity.Follow;
import com.qinghe.life.entity.User;
import com.qinghe.life.mapper.ExploreLikeMapper;
import com.qinghe.life.mapper.ExplorePostMapper;
import com.qinghe.life.mapper.FollowMapper;
import com.qinghe.life.mapper.ShopMapper;
import com.qinghe.life.mapper.UserMapper;
import com.qinghe.life.redis.RedisBusinessMetrics;
import com.qinghe.life.service.FollowingFeedService;
import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.vo.FollowingFeedVO;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class FollowingFeedBehaviorTest {
    @AfterEach
    void clearTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization();
        RedisKeys.configureNamespace("qh:");
    }

    @Test
    void publishedPostPushesToFollowersOnlyAfterTransactionCommit() {
        Fixture f = fixture();
        followers(f, follow(7L, 2L));
        TransactionSynchronizationManager.initSynchronization();

        f.service.publishedAfterCommit(post(50L, 2L, "PUBLISHED", 1000));

        verify(f.redis, never()).executePipelined(any(RedisCallback.class));
        commit();
        verify(f.redis).executePipelined(any(RedisCallback.class));
    }

    @Test
    void unpublishedPostDoesNotPushToFollowers() {
        Fixture f = fixture();
        f.service.publishedAfterCommit(post(50L, 2L, "DISABLED", 1000));
        verify(f.followMapper, never()).selectPage(any(Page.class), any(Wrapper.class));
        verify(f.redis, never()).executePipelined(any(RedisCallback.class));
    }

    @Test
    void noFollowersEndsSafelyWithoutPipeline() {
        Fixture f = fixture();
        followers(f);
        f.service.publishedAfterCommit(post(50L, 2L, "PUBLISHED", 1000));
        verify(f.redis, never()).executePipelined(any(RedisCallback.class));
    }

    @Test
    void followersArePushedInSeparatePipelineBatches() {
        Fixture f = fixture();
        java.util.ArrayList<Follow> firstRows = new java.util.ArrayList<Follow>();
        for (long id = 7L; id < 207L; id++) firstRows.add(follow(id, 2L));
        Page<Follow> first = new Page<Follow>(1, 200); first.setRecords(firstRows);
        Page<Follow> second = new Page<Follow>(2, 200); second.setRecords(Collections.singletonList(follow(8L, 2L)));
        @SuppressWarnings({ "rawtypes", "unchecked" }) org.mockito.stubbing.OngoingStubbing stub = when(f.followMapper.selectPage(any(Page.class), any(Wrapper.class)));
        stub.thenReturn(first, second);
        f.service.publishedAfterCommit(post(50L, 2L, "PUBLISHED", 1000));
        verify(f.redis, times(2)).executePipelined(any(RedisCallback.class));
    }

    @Test
    void pipelineUsesPostIdAsZaddMember() {
        Fixture f = fixture();
        followers(f, follow(7L, 2L));
        f.service.publishedAfterCommit(post(50L, 2L, "PUBLISHED", 1000));
        verify(f.zcommands).zAdd(org.mockito.ArgumentMatchers.eq(RedisKeys.followingFeed(7L).getBytes(StandardCharsets.UTF_8)), org.mockito.ArgumentMatchers.eq(1000D), org.mockito.ArgumentMatchers.eq("50".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void repeatedPushUsesSameMemberAndIsRedisIdempotent() {
        Fixture f = fixture();
        followers(f, follow(7L, 2L));
        ExplorePost post = post(50L, 2L, "PUBLISHED", 1000);
        f.service.publishedAfterCommit(post);
        f.service.publishedAfterCommit(post);
        verify(f.zcommands, times(2)).zAdd(any(byte[].class), anyDouble(), org.mockito.ArgumentMatchers.eq("50".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void capacityTrimRemovesOldestRanksAndKeepsNewestRecords() {
        Fixture f = fixture();
        followers(f, follow(7L, 2L));
        f.service.publishedAfterCommit(post(50L, 2L, "PUBLISHED", 1000));
        ArgumentCaptor<String> command = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<byte[]> args = ArgumentCaptor.forClass(byte[].class);
        verify(f.connection).execute(command.capture(), args.capture(), args.capture(), args.capture());
        assertEquals("ZREMRANGEBYRANK", command.getValue());
        assertEquals("0", new String(args.getAllValues().get(1), StandardCharsets.UTF_8));
        assertEquals("-101", new String(args.getAllValues().get(2), StandardCharsets.UTF_8));
    }

    @Test
    void firstScrollPageReturnsRecordsAndCursor() {
        Fixture f = fixture();
        tuples(f, tuple("50", 1000D));
        visible(f, post(50L, 2L, "PUBLISHED", 1000));
        FollowingFeedVO result = f.service.followingFeed(1L, null, null, 10);
        assertEquals(1, result.getRecords().size());
        assertEquals(Long.valueOf(1000L), result.getMinTime());
        assertEquals(Long.valueOf(1L), result.getOffset());
    }

    @Test
    void sameTimestampPageUsesOffsetForNextCursor() {
        Fixture f = fixture();
        tuples(f, tuple("51", 1000D), tuple("50", 1000D));
        visible(f, post(51L, 2L, "PUBLISHED", 1000), post(50L, 2L, "PUBLISHED", 1000));
        FollowingFeedVO result = f.service.followingFeed(1L, 1000L, 2L, 10);
        assertEquals(Long.valueOf(4L), result.getOffset());
        assertEquals(Long.valueOf(1000L), result.getMinTime());
    }

    @Test
    void redisTupleOrderIsRestoredInReturnedRecords() {
        Fixture f = fixture();
        tuples(f, tuple("51", 2000D), tuple("50", 1000D));
        visible(f, post(50L, 2L, "PUBLISHED", 1000), post(51L, 2L, "PUBLISHED", 2000));
        FollowingFeedVO result = f.service.followingFeed(1L, null, null, 10);
        assertEquals(Arrays.asList(51L, 50L), Arrays.asList(result.getRecords().get(0).getId(), result.getRecords().get(1).getId()));
    }

    @Test
    void deletedPostIsFilteredAndLazilyRemovedFromFeed() {
        Fixture f = fixture();
        tuples(f, tuple("50", 1000D));
        visible(f, post(50L, 2L, "DELETED", 1000));
        FollowingFeedVO result = f.service.followingFeed(1L, null, null, 10);
        assertTrue(result.getRecords().isEmpty());
        verify(f.zsets).remove(RedisKeys.followingFeed(1L), "50");
    }

    @Test
    void unfollowedAuthorIsFilteredAndLazilyRemoved() {
        Fixture f = fixture();
        tuples(f, tuple("50", 1000D));
        when(f.followMapper.selectList(any(Wrapper.class))).thenReturn(Collections.<Follow>emptyList());
        when(f.postMapper.selectBatchIds(Collections.singletonList(50L))).thenReturn(Collections.singletonList(post(50L, 2L, "PUBLISHED", 1000)));
        FollowingFeedVO result = f.service.followingFeed(1L, null, null, 10);
        assertTrue(result.getRecords().isEmpty());
        verify(f.zsets).remove(RedisKeys.followingFeed(1L), "50");
    }

    @Test
    void invalidMemberIsLazilyRemovedWithoutBreakingFeedShape() {
        Fixture f = fixture();
        tuples(f, tuple("not-a-post", 1000D));
        FollowingFeedVO result = f.service.followingFeed(1L, null, null, 10);
        assertTrue(result.getRecords().isEmpty());
        assertFalse(result.getHasMore());
    }

    @Test
    void redisFailureFallsBackToMysqlPosts() {
        Fixture f = fixture();
        when(f.zsets.reverseRangeByScoreWithScores(anyString(), anyDouble(), anyDouble(), anyLong(), anyLong())).thenThrow(new IllegalStateException("redis down"));
        when(f.postMapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(post(50L, 2L, "PUBLISHED", 1000)));
        visible(f, post(50L, 2L, "PUBLISHED", 1000));
        FollowingFeedVO result = f.service.followingFeed(1L, null, null, 10);
        assertEquals(1, result.getRecords().size());
        assertEquals(Long.valueOf(50L), result.getRecords().get(0).getId());
    }

    @Test
    void backfillPushesOnlyReturnedPublishedPosts() {
        Fixture f = fixture();
        when(f.postMapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(post(50L, 2L, "PUBLISHED", 1000)));
        f.service.backfill(1L, 2L);
        verify(f.zcommands).zAdd(RedisKeys.followingFeed(1L).getBytes(StandardCharsets.UTF_8), 1000D, "50".getBytes(StandardCharsets.UTF_8));
    }

    private static void followers(Fixture f, Follow... rows) {
        Page<Follow> page = new Page<Follow>(1, 200); page.setRecords(Arrays.asList(rows));
        @SuppressWarnings({ "rawtypes", "unchecked" }) org.mockito.stubbing.OngoingStubbing stub = when(f.followMapper.selectPage(any(Page.class), any(Wrapper.class)));
        stub.thenReturn(page);
    }
    private static void tuples(Fixture f, ZSetOperations.TypedTuple<String>... values) { when(f.zsets.reverseRangeByScoreWithScores(anyString(), anyDouble(), anyDouble(), anyLong(), anyLong())).thenReturn(new LinkedHashSet<ZSetOperations.TypedTuple<String>>(Arrays.asList(values))); }
    private static ZSetOperations.TypedTuple<String> tuple(String value, Double score) { ZSetOperations.TypedTuple<String> tuple = mock(ZSetOperations.TypedTuple.class); when(tuple.getValue()).thenReturn(value); when(tuple.getScore()).thenReturn(score); return tuple; }
    private static void visible(Fixture f, ExplorePost... posts) { when(f.followMapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(follow(1L, 2L))); when(f.postMapper.selectBatchIds(any(List.class))).thenReturn(Arrays.asList(posts)); }
    private static ExplorePost post(Long id, Long author, String status, long millis) { ExplorePost post = new ExplorePost(); post.setId(id); post.setUserId(author); post.setPostStatus(status); post.setTitle("标题"); post.setContent("正文"); post.setLikeCount(0); post.setCommentCount(0); post.setCreatedAt(LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), ZoneId.of("Asia/Shanghai"))); return post; }
    private static Follow follow(Long userId, Long targetId) { Follow follow = new Follow(); follow.setUserId(userId); follow.setFollowUserId(targetId); return follow; }
    private void commit() { for (TransactionSynchronization item : TransactionSynchronizationManager.getSynchronizations()) item.afterCommit(); TransactionSynchronizationManager.clearSynchronization(); }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Fixture fixture() {
        ExplorePostMapper posts = mock(ExplorePostMapper.class); ExploreLikeMapper likes = mock(ExploreLikeMapper.class); FollowMapper follows = mock(FollowMapper.class); UserMapper users = mock(UserMapper.class); ShopMapper shops = mock(ShopMapper.class); StringRedisTemplate redis = mock(StringRedisTemplate.class); ZSetOperations<String, String> zsets = mock(ZSetOperations.class); RedisConnection connection = mock(RedisConnection.class); RedisZSetCommands zcommands = mock(RedisZSetCommands.class);
        when(redis.opsForZSet()).thenReturn(zsets); when(redis.hasKey(anyString())).thenReturn(true); when(zsets.range(anyString(), anyLong(), anyLong())).thenReturn(Collections.<String>emptySet()); when(redis.executePipelined(any(RedisCallback.class))).thenAnswer(invocation -> { ((RedisCallback) invocation.getArgument(0)).doInRedis(connection); return Collections.emptyList(); }); when(connection.zSetCommands()).thenReturn(zcommands); when(follows.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(follow(1L, 2L))); when(users.selectBatchIds(any(Set.class))).thenReturn(Collections.singletonList(user(2L))); when(shops.selectBatchIds(any(Set.class))).thenReturn(Collections.emptyList()); when(likes.selectCount(any(Wrapper.class))).thenReturn(0L);
        return new Fixture(new FollowingFeedService(posts, likes, follows, users, shops, redis, mock(RedissonClient.class), new RedisBusinessMetrics(), 100, 10), posts, follows, redis, zsets, connection, zcommands);
    }
    private static User user(Long id) { User user = new User(); user.setId(id); user.setStatus(1); user.setNickname("作者"); return user; }
    private static final class Fixture { private final FollowingFeedService service; private final ExplorePostMapper postMapper; private final FollowMapper followMapper; private final StringRedisTemplate redis; private final ZSetOperations<String, String> zsets; private final RedisConnection connection; private final RedisZSetCommands zcommands; private Fixture(FollowingFeedService service, ExplorePostMapper postMapper, FollowMapper followMapper, StringRedisTemplate redis, ZSetOperations<String, String> zsets, RedisConnection connection, RedisZSetCommands zcommands) { this.service=service; this.postMapper=postMapper; this.followMapper=followMapper; this.redis=redis; this.zsets=zsets; this.connection=connection; this.zcommands=zcommands; } }
}
