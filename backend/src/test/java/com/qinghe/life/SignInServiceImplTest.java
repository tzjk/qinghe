package com.qinghe.life;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.redis.RedisBusinessMetrics;
import com.qinghe.life.service.impl.SignInServiceImpl;
import com.qinghe.life.utils.UserContext;
import com.qinghe.life.vo.UserDTO;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class SignInServiceImplTest {
    @AfterEach void clear() { UserContext.clear(); }
    @Test void rejectsUnauthenticatedSignIn() { assertThrows(BusinessException.class, () -> new SignInServiceImpl(mock(StringRedisTemplate.class), new RedisBusinessMetrics()).signIn()); }
    @Test void rejectsFutureAndMalformedMonthBeforeRedisRead() {
        SignInServiceImpl service = signedService(true);
        assertThrows(BusinessException.class, () -> service.calendar("2999-01"));
        assertThrows(BusinessException.class, () -> service.calendar("2026/01"));
    }
    @Test void firstSignInUsesTodayOffsetAndIsNotDuplicate() {
        SignInServiceImpl service = signedService(true);
        assertTrue(service.signIn().getSignedToday());
    }
    @Test void redisFailureNeverReportsSuccess() {
        UserDTO user = new UserDTO(); user.setId(7L); UserContext.setUser(user);
        StringRedisTemplate redis = mock(StringRedisTemplate.class); ValueOperations<String, String> value = mock(ValueOperations.class); when(redis.opsForValue()).thenReturn(value); when(value.setBit(anyString(), anyLong(), org.mockito.ArgumentMatchers.eq(true))).thenThrow(new RuntimeException("down"));
        assertThrows(BusinessException.class, () -> new SignInServiceImpl(redis, new RedisBusinessMetrics()).signIn());
    }
    @SuppressWarnings({ "unchecked", "rawtypes" }) private SignInServiceImpl signedService(boolean signed) {
        UserDTO user = new UserDTO(); user.setId(7L); UserContext.setUser(user);
        StringRedisTemplate redis = mock(StringRedisTemplate.class); ValueOperations<String, String> value = mock(ValueOperations.class); RedisConnection connection = mock(RedisConnection.class); RedisStringCommands strings = mock(RedisStringCommands.class);
        when(redis.opsForValue()).thenReturn(value); when(value.setBit(anyString(), anyLong(), org.mockito.ArgumentMatchers.eq(true))).thenReturn(false); when(value.getBit(anyString(), anyLong())).thenReturn(signed); when(connection.stringCommands()).thenReturn(strings); when(strings.bitCount(any(byte[].class))).thenReturn(1L); when(strings.bitField(any(byte[].class), any(org.springframework.data.redis.connection.BitFieldSubCommands.class))).thenReturn(Collections.singletonList(1L));
        when(redis.execute(any(RedisCallback.class))).thenAnswer(invocation -> ((RedisCallback) invocation.getArgument(0)).doInRedis(connection));
        return new SignInServiceImpl(redis, new RedisBusinessMetrics());
    }
}
