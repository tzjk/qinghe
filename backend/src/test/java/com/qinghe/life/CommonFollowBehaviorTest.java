package com.qinghe.life;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.qinghe.life.entity.Follow;
import com.qinghe.life.entity.User;
import com.qinghe.life.mapper.FollowMapper;
import com.qinghe.life.mapper.UserMapper;
import com.qinghe.life.redis.RedisBusinessMetrics;
import com.qinghe.life.service.FollowingFeedService;
import com.qinghe.life.service.impl.FollowServiceImpl;
import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.utils.UserContext;
import com.qinghe.life.vo.PublicUserSummaryVO;
import com.qinghe.life.vo.UserDTO;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class CommonFollowBehaviorTest {
    @AfterEach
    void clearContext() {
        UserContext.clear();
        RedisKeys.configureNamespace("qh:");
    }

    @Test
    void commonFollowUsesRedisIntersectionAndReturnsSortedPublicUsers() {
        Fixture fixture = fixture();
        when(fixture.redis.hasKey(anyString())).thenReturn(true);
        when(fixture.sets.intersect(RedisKeys.followings(1L), RedisKeys.followings(2L))).thenReturn(new java.util.LinkedHashSet<String>(Arrays.asList("9", "3")));
        when(fixture.sets.members(RedisKeys.followings(1L))).thenReturn(Collections.<String>emptySet());
        when(fixture.users.selectBatchIds(Arrays.asList(3L, 9L))).thenReturn(Arrays.asList(user(3L, "三号"), user(9L, "九号")));

        List<PublicUserSummaryVO> result = fixture.service.common(2L);

        assertEquals(Arrays.asList(3L, 9L), Arrays.asList(result.get(0).getUserId(), result.get(1).getUserId()));
        verify(fixture.sets).intersect(RedisKeys.followings(1L), RedisKeys.followings(2L));
    }

    @Test
    void redisIntersectionFailureFallsBackToDatabaseIntersection() {
        Fixture fixture = fixture();
        when(fixture.redis.hasKey(anyString())).thenReturn(true);
        when(fixture.sets.intersect(anyString(), anyString())).thenThrow(new IllegalStateException("redis down"));
        when(fixture.sets.members(RedisKeys.followings(1L))).thenReturn(Collections.<String>emptySet());
        when(fixture.followMapper.selectList(any(Wrapper.class))).thenReturn(Arrays.asList(relation(1L, 3L), relation(1L, 4L)), Arrays.asList(relation(2L, 4L), relation(2L, 5L)));
        when(fixture.users.selectBatchIds(Collections.singletonList(4L))).thenReturn(Collections.singletonList(user(4L, "共同")));

        List<PublicUserSummaryVO> result = fixture.service.common(2L);

        assertEquals(1, result.size());
        assertEquals(Long.valueOf(4L), result.get(0).getUserId());
        assertEquals(1L, fixture.metrics.value("common_follow_query_total", "explore_social", "common_follow", "fallback", "IllegalStateException"));
    }

    @Test
    void commonFollowOmitsDisabledUserFromPublicResult() {
        Fixture fixture = fixture();
        when(fixture.redis.hasKey(anyString())).thenReturn(true);
        when(fixture.sets.intersect(anyString(), anyString())).thenReturn(Collections.singleton("3"));
        when(fixture.sets.members(RedisKeys.followings(1L))).thenReturn(Collections.<String>emptySet());
        User disabled = user(3L, "停用");
        disabled.setStatus(0);
        when(fixture.users.selectBatchIds(Collections.singletonList(3L))).thenReturn(Collections.singletonList(disabled));

        List<PublicUserSummaryVO> result = fixture.service.common(2L);

        assertTrueEmpty(result);
    }

    @Test
    void commonFollowUsesCurrentUserContextRatherThanClientSuppliedIdentity() {
        Fixture fixture = fixture();
        when(fixture.redis.hasKey(anyString())).thenReturn(true);
        when(fixture.sets.intersect(RedisKeys.followings(1L), RedisKeys.followings(2L))).thenReturn(Collections.<String>emptySet());

        List<PublicUserSummaryVO> result = fixture.service.common(2L);

        assertTrueEmpty(result);
        verify(fixture.sets).intersect(RedisKeys.followings(1L), RedisKeys.followings(2L));
    }

    private static void assertTrueEmpty(List<PublicUserSummaryVO> result) {
        assertEquals(0, result.size());
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
        when(userMapper.selectById(2L)).thenReturn(user(2L, "目标"));
        when(followMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(redis.opsForSet()).thenReturn(sets);
        when(redis.opsForValue()).thenReturn(values);
        RedisBusinessMetrics metrics = new RedisBusinessMetrics();
        FollowServiceImpl service = new FollowServiceImpl(followMapper, userMapper, redis, mock(RedissonClient.class), metrics, mock(FollowingFeedService.class));
        return new Fixture(service, followMapper, userMapper, redis, sets, metrics);
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
        user.setStatus(1);
        user.setNickname(nickname);
        user.setAvatarUrl("/avatar/" + id);
        return user;
    }

    private static final class Fixture {
        private final FollowServiceImpl service;
        private final FollowMapper followMapper;
        private final UserMapper users;
        private final StringRedisTemplate redis;
        private final SetOperations<String, String> sets;
        private final RedisBusinessMetrics metrics;

        private Fixture(FollowServiceImpl service, FollowMapper followMapper, UserMapper users, StringRedisTemplate redis, SetOperations<String, String> sets, RedisBusinessMetrics metrics) {
            this.service = service;
            this.followMapper = followMapper;
            this.users = users;
            this.redis = redis;
            this.sets = sets;
            this.metrics = metrics;
        }
    }
}
