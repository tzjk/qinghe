package com.qinghe.life;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qinghe.life.common.PageResult;
import com.qinghe.life.entity.Follow;
import com.qinghe.life.entity.User;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.FollowMapper;
import com.qinghe.life.mapper.UserMapper;
import com.qinghe.life.redis.RedisBusinessMetrics;
import com.qinghe.life.service.FollowingFeedService;
import com.qinghe.life.service.impl.FollowServiceImpl;
import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.utils.UserContext;
import com.qinghe.life.vo.FollowCountsVO;
import com.qinghe.life.vo.PublicUserSummaryVO;
import com.qinghe.life.vo.UserDTO;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class FollowServiceBehaviorTest {
    @AfterEach
    void clearContext() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        UserContext.clear();
        RedisKeys.configureNamespace("qh:");
    }

    @Test
    void followWritesDatabaseThenAddsBothMembershipSetsAfterCommit() {
        Fixture fixture = fixture();
        when(fixture.followMapper.insert(any(Follow.class))).thenReturn(1);
        TransactionSynchronizationManager.initSynchronization();

        FollowCountsVO result = fixture.service.follow(2L);

        verify(fixture.followMapper).insert(any(Follow.class));
        verify(fixture.setOperations, never()).add(anyString(), any(String[].class));
        afterCommit();
        assertTrue(result.getFollowedByMe());
        verify(fixture.setOperations).add(RedisKeys.followings(1L), "2");
        verify(fixture.setOperations).add(RedisKeys.followers(2L), "1");
        verify(fixture.feedService).backfill(1L, 2L);
    }

    @Test
    void duplicateFollowIsIdempotentAndDoesNotRefreshDerivedSets() {
        Fixture fixture = fixture();
        when(fixture.followMapper.insert(any(Follow.class))).thenThrow(new DuplicateKeyException("unique"));
        TransactionSynchronizationManager.initSynchronization();

        fixture.service.follow(2L);
        afterCommit();

        verify(fixture.setOperations, never()).add(anyString(), any(String[].class));
        verify(fixture.feedService, never()).backfill(anyLong(), anyLong());
    }

    @Test
    void unfollowDeletesDatabaseThenRemovesBothMembershipSetsAfterCommit() {
        Fixture fixture = fixture();
        when(fixture.followMapper.delete(any(Wrapper.class))).thenReturn(1);
        TransactionSynchronizationManager.initSynchronization();

        fixture.service.unfollow(2L);

        verify(fixture.setOperations, never()).remove(anyString(), any(Object[].class));
        afterCommit();
        verify(fixture.setOperations).remove(RedisKeys.followings(1L), "2");
        verify(fixture.setOperations).remove(RedisKeys.followers(2L), "1");
        verify(fixture.feedService).cleanupAuthor(1L, 2L);
    }

    @Test
    void repeatedUnfollowDoesNotTouchDerivedSets() {
        Fixture fixture = fixture();
        when(fixture.followMapper.delete(any(Wrapper.class))).thenReturn(0);
        TransactionSynchronizationManager.initSynchronization();

        fixture.service.unfollow(2L);
        afterCommit();

        verify(fixture.setOperations, never()).remove(anyString(), any(Object[].class));
        verify(fixture.feedService, never()).cleanupAuthor(anyLong(), anyLong());
    }

    @Test
    void selfFollowIsRejectedBeforeDatabaseWrite() {
        Fixture fixture = fixture();

        BusinessException exception = assertThrows(BusinessException.class, () -> fixture.service.follow(1L));

        assertEquals(Integer.valueOf(400), exception.getCode());
        verify(fixture.followMapper, never()).insert(any(Follow.class));
    }

    @Test
    void missingTargetIsRejectedBeforeDatabaseWrite() {
        Fixture fixture = fixture();
        when(fixture.userMapper.selectById(2L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> fixture.service.follow(2L));

        assertEquals(Integer.valueOf(404), exception.getCode());
        verify(fixture.followMapper, never()).insert(any(Follow.class));
    }

    @Test
    void redisFailureAfterCommittedFollowKeepsDatabaseResult() {
        Fixture fixture = fixture();
        when(fixture.followMapper.insert(any(Follow.class))).thenReturn(1);
        when(fixture.setOperations.add(anyString(), anyString())).thenThrow(new IllegalStateException("redis down"));
        TransactionSynchronizationManager.initSynchronization();

        FollowCountsVO result = fixture.service.follow(2L);
        afterCommit();

        assertTrue(result.getFollowedByMe());
        verify(fixture.followMapper).insert(any(Follow.class));
        assertEquals(1L, fixture.metrics.value("follow_cache_update_total", "explore_social", "follow", "failure", "IllegalStateException"));
    }

    @Test
    void emptyFollowingCacheGetsLoadedMarkerWithoutSetMembers() throws Exception {
        Fixture fixture = fixture();
        when(fixture.redis.hasKey(RedisKeys.followingsLoaded(1L))).thenReturn(false);
        when(fixture.lock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(fixture.lock.isHeldByCurrentThread()).thenReturn(true);
        when(fixture.followMapper.selectList(any(Wrapper.class))).thenReturn(Collections.<Follow>emptyList());

        Set<Long> result = fixture.service.followingIds(1L);

        assertTrue(result.isEmpty());
        verify(fixture.setOperations, never()).add(anyString(), any(String[].class));
        verify(fixture.valueOperations).set(org.mockito.ArgumentMatchers.eq(RedisKeys.followingsLoaded(1L)), org.mockito.ArgumentMatchers.eq("1"), anyLong(), org.mockito.ArgumentMatchers.eq(TimeUnit.SECONDS));
        verify(fixture.lock).unlock();
    }

    @Test
    void missingFollowingSetRebuildsFromDatabaseAndWritesMembers() throws Exception {
        Fixture fixture = fixture();
        Follow relation = relation(1L, 9L);
        when(fixture.redis.hasKey(RedisKeys.followingsLoaded(1L))).thenReturn(false);
        when(fixture.lock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(fixture.followMapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(relation));

        Set<Long> result = fixture.service.followingIds(1L);

        assertEquals(Collections.singleton(9L), result);
        verify(fixture.setOperations).add(RedisKeys.followings(1L), "9");
        verify(fixture.redis).expire(org.mockito.ArgumentMatchers.eq(RedisKeys.followings(1L)), anyLong(), org.mockito.ArgumentMatchers.eq(TimeUnit.SECONDS));
    }

    @Test
    void cacheLockContentionFallsBackToDatabaseWithoutWrites() throws Exception {
        Fixture fixture = fixture();
        when(fixture.redis.hasKey(RedisKeys.followingsLoaded(1L))).thenReturn(false);
        when(fixture.lock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(false);
        when(fixture.followMapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(relation(1L, 8L)));

        Set<Long> result = fixture.service.followingIds(1L);

        assertEquals(Collections.singleton(8L), result);
        verify(fixture.setOperations, never()).add(anyString(), any(String[].class));
        verify(fixture.valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void followingListReturnsPublicProjectionAndPageMetadata() throws Exception {
        Fixture fixture = fixture();
        Page<Follow> page = new Page<Follow>(2, 3);
        page.setRecords(Collections.singletonList(relation(1L, 2L)));
        page.setTotal(4);
        when(fixture.followMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);
        when(fixture.userMapper.selectBatchIds(Collections.singletonList(2L))).thenReturn(Collections.singletonList(user(2L, "公开昵称")));
        when(fixture.redis.hasKey(RedisKeys.followingsLoaded(1L))).thenReturn(true);
        when(fixture.setOperations.members(RedisKeys.followings(1L))).thenReturn(Collections.singleton("2"));

        PageResult<PublicUserSummaryVO> result = fixture.service.following(1L, 2L, 3L);

        assertEquals(Long.valueOf(4L), result.getTotal());
        assertEquals("公开昵称", result.getRecords().get(0).getNickname());
        assertTrue(result.getRecords().get(0).getFollowedByMe());
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void followerListReturnsUsersWhoFollowTarget() throws Exception {
        Fixture fixture = fixture();
        Page<Follow> page = new Page<Follow>(1, 10);
        page.setRecords(Collections.singletonList(relation(3L, 1L)));
        page.setTotal(1);
        when(fixture.followMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);
        when(fixture.userMapper.selectBatchIds(Collections.singletonList(3L))).thenReturn(Collections.singletonList(user(3L, "粉丝")));
        when(fixture.redis.hasKey(RedisKeys.followingsLoaded(1L))).thenReturn(true);
        when(fixture.setOperations.members(RedisKeys.followings(1L))).thenReturn(Collections.<String>emptySet());

        PageResult<PublicUserSummaryVO> result = fixture.service.followers(1L, 1L, 10L);

        assertEquals(Long.valueOf(1L), result.getTotal());
        assertEquals(Long.valueOf(3L), result.getRecords().get(0).getUserId());
        assertFalse(result.getRecords().get(0).getFollowedByMe());
    }

    @Test
    void relationReturnsFollowingAndFollowerCountsForTarget() {
        Fixture fixture = fixture();
        when(fixture.followMapper.selectCount(any(Wrapper.class))).thenReturn(6L, 8L, 1L);

        FollowCountsVO result = fixture.service.relation(2L);

        assertEquals(Long.valueOf(2L), result.getUserId());
        assertEquals(Long.valueOf(6L), result.getFollowingCount());
        assertEquals(Long.valueOf(8L), result.getFollowerCount());
        assertTrue(result.getFollowedByMe());
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void publicFollowResponseSerializationDoesNotLeakPrivateUserFields() throws Exception {
        Fixture fixture = fixture();
        Page<Follow> page = new Page<Follow>(1, 10);
        page.setRecords(Collections.singletonList(relation(1L, 2L)));
        page.setTotal(1);
        when(fixture.followMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);
        User target = user(2L, "公开昵称");
        target.setPhone("13800000000");
        target.setPasswordHash("secret-hash");
        when(fixture.userMapper.selectBatchIds(Collections.singletonList(2L))).thenReturn(Collections.singletonList(target));
        when(fixture.redis.hasKey(RedisKeys.followingsLoaded(1L))).thenReturn(true);
        when(fixture.setOperations.members(RedisKeys.followings(1L))).thenReturn(Collections.<String>emptySet());

        String json = new ObjectMapper().writeValueAsString(fixture.service.following(1L, 1L, 10L).getRecords().get(0));

        assertFalse(json.contains("13800000000"));
        assertFalse(json.contains("secret-hash"));
        assertTrue(json.contains("公开昵称"));
    }

    private void afterCommit() {
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
        TransactionSynchronizationManager.clearSynchronization();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Fixture fixture() {
        UserDTO current = new UserDTO();
        current.setId(1L);
        UserContext.setUser(current);
        FollowMapper followMapper = mock(FollowMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        SetOperations<String, String> sets = mock(SetOperations.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        RedissonClient redisson = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        FollowingFeedService feedService = mock(FollowingFeedService.class);
        RedisBusinessMetrics metrics = new RedisBusinessMetrics();
        when(userMapper.selectById(2L)).thenReturn(user(2L, "目标"));
        when(userMapper.selectById(1L)).thenReturn(user(1L, "当前用户"));
        when(followMapper.selectCount(any(Wrapper.class))).thenReturn(2L);
        when(followMapper.selectList(any(Wrapper.class))).thenReturn(Collections.<Follow>emptyList());
        when(redis.opsForSet()).thenReturn(sets);
        when(redis.opsForValue()).thenReturn(values);
        when(redisson.getLock(anyString())).thenReturn(lock);
        return new Fixture(new FollowServiceImpl(followMapper, userMapper, redis, redisson, metrics, feedService), followMapper, userMapper, redis, sets, values, lock, feedService, metrics);
    }

    private static Follow relation(Long userId, Long targetId) {
        Follow row = new Follow();
        row.setUserId(userId);
        row.setFollowUserId(targetId);
        return row;
    }

    private static User user(Long id, String nickname) {
        User user = new User();
        user.setId(id);
        user.setNickname(nickname);
        user.setAvatarUrl("/avatar/" + id);
        user.setStatus(1);
        return user;
    }

    private static final class Fixture {
        private final FollowServiceImpl service;
        private final FollowMapper followMapper;
        private final UserMapper userMapper;
        private final StringRedisTemplate redis;
        private final SetOperations<String, String> setOperations;
        private final ValueOperations<String, String> valueOperations;
        private final RLock lock;
        private final FollowingFeedService feedService;
        private final RedisBusinessMetrics metrics;

        private Fixture(FollowServiceImpl service, FollowMapper followMapper, UserMapper userMapper, StringRedisTemplate redis, SetOperations<String, String> setOperations, ValueOperations<String, String> valueOperations, RLock lock, FollowingFeedService feedService, RedisBusinessMetrics metrics) {
            this.service = service;
            this.followMapper = followMapper;
            this.userMapper = userMapper;
            this.redis = redis;
            this.setOperations = setOperations;
            this.valueOperations = valueOperations;
            this.lock = lock;
            this.feedService = feedService;
            this.metrics = metrics;
        }
    }
}
