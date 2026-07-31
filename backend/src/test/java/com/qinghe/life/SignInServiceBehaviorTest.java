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

import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.redis.RedisBusinessMetrics;
import com.qinghe.life.service.impl.SignInServiceImpl;
import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.utils.UserContext;
import com.qinghe.life.vo.SignInCalendarVO;
import com.qinghe.life.vo.SignInStatusVO;
import com.qinghe.life.vo.UserDTO;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class SignInServiceBehaviorTest {
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    @AfterEach
    void clearContext() {
        UserContext.clear();
        RedisKeys.configureNamespace("qh:");
    }

    @Test
    void firstSignInWritesTodayBitWithZeroBasedOffset() {
        RedisFixture fixture = fixture(Collections.singletonList(1L));
        when(fixture.value.setBit(anyString(), anyLong(), org.mockito.ArgumentMatchers.eq(true))).thenReturn(false);
        when(fixture.value.getBit(anyString(), anyLong())).thenReturn(true);

        SignInStatusVO result = fixture.service().signIn();

        ZonedDateTime now = ZonedDateTime.now(SHANGHAI);
        assertTrue(result.getSignedToday());
        verify(fixture.value).setBit(RedisKeys.signIn(7L, now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))), now.getDayOfMonth() - 1L, true);
    }

    @Test
    void duplicateSignInReturnsStatusAndRecordsDuplicateOutcome() {
        RedisFixture fixture = fixture(Collections.singletonList(1L));
        when(fixture.value.setBit(anyString(), anyLong(), org.mockito.ArgumentMatchers.eq(true))).thenReturn(true);
        when(fixture.value.getBit(anyString(), anyLong())).thenReturn(true);

        SignInStatusVO result = fixture.service().signIn();

        assertTrue(result.getSignedToday());
        assertEquals(1L, fixture.metrics.value("sign_in_duplicate_total", "explore_social", "sign_in", "duplicate", "none"));
    }

    @Test
    void statusReadsGetBitAndMonthBitCountForCurrentUser() {
        RedisFixture fixture = fixture(Arrays.asList(7L));
        when(fixture.value.getBit(anyString(), anyLong())).thenReturn(true);

        SignInStatusVO result = fixture.service().status();

        ZonedDateTime now = ZonedDateTime.now(SHANGHAI);
        assertTrue(result.getSignedToday());
        assertEquals(7, result.getMonthDays());
        verify(fixture.value).getBit(RedisKeys.signIn(7L, now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))), now.getDayOfMonth() - 1L);
    }

    @Test
    void streakCountsConsecutiveLowOrderBits() {
        RedisFixture fixture = fixture(Collections.singletonList(7L));
        when(fixture.value.getBit(anyString(), anyLong())).thenReturn(true);

        assertEquals(Integer.valueOf(3), fixture.service().streak());
    }

    @Test
    void streakStopsAtFirstMissingDay() {
        RedisFixture fixture = fixture(Collections.singletonList(5L));
        when(fixture.value.getBit(anyString(), anyLong())).thenReturn(true);

        assertEquals(Integer.valueOf(1), fixture.service().streak());
    }

    @Test
    void streakIsZeroWhenTodayIsNotSigned() {
        RedisFixture fixture = fixture(Collections.singletonList(0L));
        when(fixture.value.getBit(anyString(), anyLong())).thenReturn(false);

        SignInStatusVO result = fixture.service().status();

        assertFalse(result.getSignedToday());
        assertEquals(Integer.valueOf(0), result.getStreakDays());
    }

    @Test
    void calendarUsesRequestedHistoricalMonthKeyAndEachDayOffset() {
        RedisFixture fixture = fixture(Collections.singletonList(2L));
        YearMonth target = YearMonth.now(SHANGHAI).minusMonths(1);
        String month = target.toString();

        SignInCalendarVO result = fixture.service().calendar(month);

        assertEquals(month, result.getMonth());
        assertEquals(target.lengthOfMonth(), result.getDays().size());
        verify(fixture.value).getBit(RedisKeys.signIn(7L, month), 0L);
        verify(fixture.value).getBit(RedisKeys.signIn(7L, month), target.lengthOfMonth() - 1L);
    }

    @Test
    void currentAndPreviousMonthUseDifferentRedisKeys() {
        RedisFixture fixture = fixture(Collections.singletonList(0L));
        YearMonth current = YearMonth.now(SHANGHAI);
        YearMonth previous = current.minusMonths(1);

        fixture.service().calendar(current.toString());
        fixture.service().calendar(previous.toString());

        verify(fixture.value).getBit(RedisKeys.signIn(7L, current.toString()), 0L);
        verify(fixture.value).getBit(RedisKeys.signIn(7L, previous.toString()), 0L);
    }

    @Test
    void futureMonthIsRejectedBeforeAnyRedisCall() {
        RedisFixture fixture = fixture(Collections.singletonList(0L));
        String future = YearMonth.now(SHANGHAI).plusMonths(1).toString();

        BusinessException exception = assertThrows(BusinessException.class, () -> fixture.service().calendar(future));

        assertEquals(Integer.valueOf(400), exception.getCode());
        verify(fixture.redis, never()).opsForValue();
    }

    @Test
    void malformedMonthIsRejectedBeforeAnyRedisCall() {
        RedisFixture fixture = fixture(Collections.singletonList(0L));

        BusinessException exception = assertThrows(BusinessException.class, () -> fixture.service().calendar("2026/02"));

        assertEquals(Integer.valueOf(400), exception.getCode());
        verify(fixture.redis, never()).opsForValue();
    }

    @Test
    void redisFailureDuringStatusMapsToServiceUnavailable() {
        RedisFixture fixture = fixture(Collections.singletonList(0L));
        when(fixture.value.getBit(anyString(), anyLong())).thenThrow(new IllegalStateException("redis offline"));

        BusinessException exception = assertThrows(BusinessException.class, () -> fixture.service().status());

        assertEquals(Integer.valueOf(503), exception.getCode());
        assertEquals(1L, fixture.metrics.value("sign_in_failure_total", "explore_social", "status", "failure", "IllegalStateException"));
    }

    @Test
    void calendarRedisFailureMapsToServiceUnavailable() {
        RedisFixture fixture = fixture(Collections.singletonList(0L));
        when(fixture.value.getBit(anyString(), anyLong())).thenThrow(new IllegalStateException("redis offline"));

        BusinessException exception = assertThrows(BusinessException.class, () -> fixture.service().calendar(YearMonth.now(SHANGHAI).toString()));

        assertEquals(Integer.valueOf(503), exception.getCode());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private RedisFixture fixture(final List<Long> bitFieldValues) {
        UserDTO user = new UserDTO();
        user.setId(7L);
        UserContext.setUser(user);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> value = mock(ValueOperations.class);
        RedisConnection connection = mock(RedisConnection.class);
        RedisStringCommands strings = mock(RedisStringCommands.class);
        RedisBusinessMetrics metrics = new RedisBusinessMetrics();
        when(redis.opsForValue()).thenReturn(value);
        when(connection.stringCommands()).thenReturn(strings);
        when(strings.bitCount(any(byte[].class))).thenReturn(7L);
        when(strings.bitField(any(byte[].class), any(org.springframework.data.redis.connection.BitFieldSubCommands.class))).thenReturn(bitFieldValues);
        when(redis.execute(any(RedisCallback.class))).thenAnswer(invocation -> ((RedisCallback) invocation.getArgument(0)).doInRedis(connection));
        return new RedisFixture(redis, value, metrics);
    }

    private static final class RedisFixture {
        private final StringRedisTemplate redis;
        private final ValueOperations<String, String> value;
        private final RedisBusinessMetrics metrics;

        private RedisFixture(StringRedisTemplate redis, ValueOperations<String, String> value, RedisBusinessMetrics metrics) {
            this.redis = redis;
            this.value = value;
            this.metrics = metrics;
        }

        private SignInServiceImpl service() {
            return new SignInServiceImpl(redis, metrics);
        }
    }
}
