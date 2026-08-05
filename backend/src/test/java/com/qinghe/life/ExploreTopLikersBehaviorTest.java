package com.qinghe.life;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qinghe.life.entity.ExploreLike;
import com.qinghe.life.entity.ExplorePost;
import com.qinghe.life.entity.Shop;
import com.qinghe.life.entity.User;
import com.qinghe.life.mapper.ExploreCommentMapper;
import com.qinghe.life.mapper.ExploreLikeMapper;
import com.qinghe.life.mapper.ExplorePostMapper;
import com.qinghe.life.mapper.ShopMapper;
import com.qinghe.life.mapper.UserMapper;
import com.qinghe.life.oss.AliyunOSSOperator;
import com.qinghe.life.redis.RedisBusinessMetrics;
import com.qinghe.life.service.ExploreHotService;
import com.qinghe.life.service.FollowService;
import com.qinghe.life.service.FollowingFeedService;
import com.qinghe.life.service.impl.ExploreServiceImpl;
import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.utils.UserContext;
import com.qinghe.life.vo.ExplorePostVO;
import com.qinghe.life.vo.PublicUserSummaryVO;
import com.qinghe.life.vo.UserDTO;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

class ExploreTopLikersBehaviorTest {
    @AfterEach
    void clearContext() {
        UserContext.clear();
        RedisKeys.configureNamespace("qh:");
    }

    @Test
    void likeAfterCommitUsesZaddNxWithStableLikeMember() {
        Fixture fixture = fixture();
        ExploreLike like = like(12L, 8L, LocalDateTime.of(2026, 7, 1, 9, 0));

        fixture.feed.likedAfterCommit(55L, like);

        verify(fixture.zsets).addIfAbsent(RedisKeys.exploreLikers(55L), "00000000000000000012:8", timestamp(like.getCreatedAt()));
    }

    @Test
    void unlikeAfterCommitUsesZremForSameStableMember() {
        Fixture fixture = fixture();
        ExploreLike like = like(12L, 8L, LocalDateTime.of(2026, 7, 1, 9, 0));

        fixture.feed.unlikedAfterCommit(55L, like);

        verify(fixture.zsets).remove(RedisKeys.exploreLikers(55L), "00000000000000000012:8");
    }

    @Test
    void cachedTopLikersReturnsEarliestFiveInZsetOrder() {
        Fixture fixture = fixture();
        cached(fixture, 55L, "00000000000000000001:11", "00000000000000000002:12", "00000000000000000003:13", "00000000000000000004:14", "00000000000000000005:15");
        when(fixture.userMapper.selectBatchIds(Arrays.asList(11L, 12L, 13L, 14L, 15L))).thenReturn(Arrays.asList(user(11L), user(12L), user(13L), user(14L), user(15L)));

        List<PublicUserSummaryVO> result = fixture.feed.topLikers(55L);

        assertEquals(Arrays.asList(11L, 12L, 13L, 14L, 15L), ids(result));
        verify(fixture.likeMapper, never()).selectList(any(Wrapper.class));
    }

    @Test
    void missingLikerZsetRebuildsFromDatabaseCreatedAtThenIdOrder() throws Exception {
        Fixture fixture = fixture();
        when(fixture.redis.hasKey(RedisKeys.exploreLikers(55L))).thenReturn(false);
        when(fixture.redis.hasKey(RedisKeys.exploreLikersLoaded(55L))).thenReturn(false);
        when(fixture.lock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(fixture.lock.isHeldByCurrentThread()).thenReturn(true);
        ExploreLike first = like(10L, 7L, LocalDateTime.of(2026, 7, 1, 9, 0));
        ExploreLike second = like(11L, 8L, LocalDateTime.of(2026, 7, 1, 9, 0));
        when(fixture.likeMapper.selectList(any(Wrapper.class))).thenReturn(Arrays.asList(first, second));
        when(fixture.zsets.range(RedisKeys.exploreLikers(55L), 0, 4)).thenReturn(new LinkedHashSet<String>(Arrays.asList("00000000000000000010:7", "00000000000000000011:8")));
        when(fixture.userMapper.selectBatchIds(Arrays.asList(7L, 8L))).thenReturn(Arrays.asList(user(7L), user(8L)));

        List<PublicUserSummaryVO> result = fixture.feed.topLikers(55L);

        assertEquals(Arrays.asList(7L, 8L), ids(result));
        verify(fixture.zsets).addIfAbsent(RedisKeys.exploreLikers(55L), "00000000000000000010:7", timestamp(first.getCreatedAt()));
        verify(fixture.zsets).addIfAbsent(RedisKeys.exploreLikers(55L), "00000000000000000011:8", timestamp(second.getCreatedAt()));
        verify(fixture.values).set(org.mockito.ArgumentMatchers.eq(RedisKeys.exploreLikersLoaded(55L)), org.mockito.ArgumentMatchers.eq("1"), anyLong(), org.mockito.ArgumentMatchers.eq(TimeUnit.SECONDS));
        verify(fixture.lock).unlock();
    }

    @Test
    void redisFailureReadsFirstFiveDirectlyFromDatabase() {
        Fixture fixture = fixture();
        when(fixture.redis.hasKey(anyString())).thenThrow(new IllegalStateException("redis down"));
        List<ExploreLike> likes = Arrays.asList(like(1L, 21L, LocalDateTime.now()), like(2L, 22L, LocalDateTime.now()), like(3L, 23L, LocalDateTime.now()));
        when(fixture.likeMapper.selectList(any(Wrapper.class))).thenReturn(likes);
        when(fixture.userMapper.selectBatchIds(Arrays.asList(21L, 22L, 23L))).thenReturn(Arrays.asList(user(21L), user(22L), user(23L)));

        List<PublicUserSummaryVO> result = fixture.feed.topLikers(55L);

        assertEquals(Arrays.asList(21L, 22L, 23L), ids(result));
        assertEquals(1L, fixture.metrics.value("liker_zset_rebuild_total", "explore_social", "fallback", "failure", "IllegalStateException"));
    }

    @Test
    void unlikeChangesNextTopLikerReadWithoutChangingDatabaseLikeCountContract() {
        Fixture fixture = fixture();
        ExploreLike removed = like(1L, 21L, LocalDateTime.now());
        fixture.feed.unlikedAfterCommit(55L, removed);
        cached(fixture, 55L, "00000000000000000002:22", "00000000000000000003:23");
        when(fixture.userMapper.selectBatchIds(Arrays.asList(22L, 23L))).thenReturn(Arrays.asList(user(22L), user(23L)));

        List<PublicUserSummaryVO> result = fixture.feed.topLikers(55L);

        verify(fixture.zsets).remove(RedisKeys.exploreLikers(55L), "00000000000000000001:21");
        assertEquals(Arrays.asList(22L, 23L), ids(result));
    }

    @Test
    void emptyLoadedLikerCacheDoesNotRebuildDatabase() {
        Fixture fixture = fixture();
        when(fixture.redis.hasKey(RedisKeys.exploreLikers(55L))).thenReturn(true);
        when(fixture.redis.hasKey(RedisKeys.exploreLikersLoaded(55L))).thenReturn(true);
        when(fixture.zsets.range(RedisKeys.exploreLikers(55L), 0, 4)).thenReturn(Collections.<String>emptySet());

        List<PublicUserSummaryVO> result = fixture.feed.topLikers(55L);

        assertTrue(result.isEmpty());
        verify(fixture.likeMapper, never()).selectList(any(Wrapper.class));
    }

    @Test
    void topLikerResponseSerializesOnlyPublicFields() throws Exception {
        Fixture fixture = fixture();
        cached(fixture, 55L, "00000000000000000001:21");
        User user = user(21L);
        user.setPhone("13900000000");
        user.setPasswordHash("hidden");
        when(fixture.userMapper.selectBatchIds(Collections.singletonList(21L))).thenReturn(Collections.singletonList(user));

        String json = new ObjectMapper().writeValueAsString(fixture.feed.topLikers(55L).get(0));

        assertFalse(json.contains("13900000000"));
        assertFalse(json.contains("hidden"));
        assertTrue(json.contains("userId"));
    }

    @Test
    void loggedDetailKeepsFullLikeCountAndMarksLikedByCurrentUser() {
        ExploreFixture fixture = exploreFixture(true);

        ExplorePostVO result = fixture.service.detail(55L);

        assertEquals(Integer.valueOf(9), result.getLikeCount());
        assertTrue(result.getLiked());
        assertEquals(5, result.getTopLikers().size());
    }

    @Test
    void anonymousDetailDoesNotQueryCurrentUserLikeAndReturnsFalse() {
        ExploreFixture fixture = exploreFixture(false);

        ExplorePostVO result = fixture.service.detail(55L);

        assertFalse(result.getLiked());
        verify(fixture.likeMapper, never()).selectCount(any(Wrapper.class));
    }

    private static void cached(Fixture fixture, Long postId, String... members) {
        when(fixture.redis.hasKey(RedisKeys.exploreLikers(postId))).thenReturn(true);
        when(fixture.redis.hasKey(RedisKeys.exploreLikersLoaded(postId))).thenReturn(true);
        when(fixture.zsets.range(RedisKeys.exploreLikers(postId), 0, 4)).thenReturn(new LinkedHashSet<String>(Arrays.asList(members)));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Fixture fixture() {
        ExplorePostMapper postMapper = mock(ExplorePostMapper.class);
        ExploreLikeMapper likeMapper = mock(ExploreLikeMapper.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ZSetOperations<String, String> zsets = mock(ZSetOperations.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        RedissonClient redisson = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        UserMapper userMapper = mock(UserMapper.class);
        when(redis.opsForZSet()).thenReturn(zsets);
        when(redis.opsForValue()).thenReturn(values);
        when(redisson.getLock(anyString())).thenReturn(lock);
        RedisBusinessMetrics metrics = new RedisBusinessMetrics();
        FollowingFeedService feed = new FollowingFeedService(postMapper, likeMapper, mock(com.qinghe.life.mapper.FollowMapper.class), userMapper, mock(ShopMapper.class), redis, redisson, metrics, 100, 10);
        return new Fixture(feed, likeMapper, userMapper, redis, zsets, values, lock, metrics);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private ExploreFixture exploreFixture(boolean loggedIn) {
        if (loggedIn) {
            UserDTO current = new UserDTO();
            current.setId(1L);
            UserContext.setUser(current);
        }
        ExplorePostMapper posts = mock(ExplorePostMapper.class);
        ExploreLikeMapper likes = mock(ExploreLikeMapper.class);
        UserMapper users = mock(UserMapper.class);
        ShopMapper shops = mock(ShopMapper.class);
        FollowingFeedService feed = mock(FollowingFeedService.class);
        FollowService follows = mock(FollowService.class);
        ExplorePost post = new ExplorePost();
        post.setId(55L); post.setUserId(2L); post.setShopId(3L); post.setPostStatus("PUBLISHED"); post.setLikeCount(9); post.setCommentCount(0); post.setTitle("内容"); post.setContent("正文"); post.setCreatedAt(LocalDateTime.now());
        when(posts.selectById(55L)).thenReturn(post);
        when(users.selectBatchIds(Collections.singleton(2L))).thenReturn(Collections.singletonList(user(2L)));
        Shop shop = new Shop(); shop.setId(3L); shop.setName("店铺");
        when(shops.selectBatchIds(Collections.singleton(3L))).thenReturn(Collections.singletonList(shop));
        when(feed.topLikers(55L)).thenReturn(Arrays.asList(summary(11L), summary(12L), summary(13L), summary(14L), summary(15L)));
        if (loggedIn) { when(follows.followingIds(1L)).thenReturn(Collections.<Long>emptySet()); when(likes.selectCount(any(Wrapper.class))).thenReturn(1L); }
        ExploreServiceImpl service = new ExploreServiceImpl(posts, likes, mock(ExploreCommentMapper.class), shops, users, mock(ExploreHotService.class), mock(StringRedisTemplate.class), mock(AliyunOSSOperator.class), feed, follows);
        return new ExploreFixture(service, likes);
    }

    private static ExploreLike like(Long id, Long userId, LocalDateTime time) {
        ExploreLike like = new ExploreLike(); like.setId(id); like.setUserId(userId); like.setCreatedAt(time); return like;
    }
    private static User user(Long id) { User user = new User(); user.setId(id); user.setStatus(1); user.setNickname("用户" + id); user.setAvatarUrl("/avatar/" + id); return user; }
    private static PublicUserSummaryVO summary(Long id) { PublicUserSummaryVO view = new PublicUserSummaryVO(); view.setUserId(id); return view; }
    private static List<Long> ids(List<PublicUserSummaryVO> users) { java.util.ArrayList<Long> ids = new java.util.ArrayList<Long>(); for (PublicUserSummaryVO user : users) ids.add(user.getUserId()); return ids; }
    private static double timestamp(LocalDateTime value) { return (double) value.atZone(java.time.ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli(); }

    private static final class Fixture {
        private final FollowingFeedService feed; private final ExploreLikeMapper likeMapper; private final UserMapper userMapper; private final StringRedisTemplate redis; private final ZSetOperations<String, String> zsets; private final ValueOperations<String, String> values; private final RLock lock; private final RedisBusinessMetrics metrics;
        private Fixture(FollowingFeedService feed, ExploreLikeMapper likeMapper, UserMapper userMapper, StringRedisTemplate redis, ZSetOperations<String, String> zsets, ValueOperations<String, String> values, RLock lock, RedisBusinessMetrics metrics) { this.feed = feed; this.likeMapper = likeMapper; this.userMapper = userMapper; this.redis = redis; this.zsets = zsets; this.values = values; this.lock = lock; this.metrics = metrics; }
    }
    private static final class ExploreFixture { private final ExploreServiceImpl service; private final ExploreLikeMapper likeMapper; private ExploreFixture(ExploreServiceImpl service, ExploreLikeMapper likeMapper) { this.service = service; this.likeMapper = likeMapper; } }
}
