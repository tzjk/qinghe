package com.qinghe.life.service.impl;

import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.redis.RedisBusinessMetrics;
import com.qinghe.life.service.SignInService;
import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.utils.UserContext;
import com.qinghe.life.vo.SignInCalendarVO;
import com.qinghe.life.vo.SignInStatusVO;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SignInServiceImpl implements SignInService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private final StringRedisTemplate redisTemplate;
    private final RedisBusinessMetrics metrics;

    public SignInServiceImpl(StringRedisTemplate redisTemplate, RedisBusinessMetrics metrics) { this.redisTemplate = redisTemplate; this.metrics = metrics; }

    @Override public SignInStatusVO signIn() {
        Long userId = requireUser(); ZonedDateTime now = ZonedDateTime.now(BUSINESS_ZONE); String key = RedisKeys.signIn(userId, month(now));
        try {
            Boolean already = redisTemplate.opsForValue().setBit(key, now.getDayOfMonth() - 1L, true);
            metrics.count(already ? "sign_in_duplicate_total" : "sign_in_total", "explore_social", "sign_in", already ? "duplicate" : "success", "none");
            return summary(userId, now);
        } catch (Exception exception) {
            metrics.count("sign_in_failure_total", "explore_social", "sign_in", "failure", errorType(exception));
            throw new BusinessException(503, "签到服务暂不可用");
        }
    }

    @Override public SignInStatusVO status() { return summary(requireUser(), ZonedDateTime.now(BUSINESS_ZONE)); }

    @Override public SignInCalendarVO calendar(String month) {
        Long userId = requireUser(); YearMonth target = parseMonth(month); String key = RedisKeys.signIn(userId, target.format(MONTH_FORMAT));
        try {
            List<Boolean> days = new ArrayList<Boolean>();
            for (int day = 1; day <= target.lengthOfMonth(); day++) days.add(Boolean.TRUE.equals(redisTemplate.opsForValue().getBit(key, day - 1L)));
            SignInCalendarVO view = new SignInCalendarVO(); view.setMonth(target.format(MONTH_FORMAT)); view.setMonthDays(monthDays(key)); view.setDays(days); return view;
        } catch (Exception exception) {
            metrics.count("sign_in_failure_total", "explore_social", "calendar", "failure", errorType(exception));
            throw new BusinessException(503, "签到服务暂不可用");
        }
    }

    @Override public Integer streak() { return summary(requireUser(), ZonedDateTime.now(BUSINESS_ZONE)).getStreakDays(); }

    private SignInStatusVO summary(Long userId, ZonedDateTime now) {
        String key = RedisKeys.signIn(userId, month(now));
        try {
            SignInStatusVO view = new SignInStatusVO(); view.setSignedToday(Boolean.TRUE.equals(redisTemplate.opsForValue().getBit(key, now.getDayOfMonth() - 1L))); view.setMonthDays(monthDays(key)); view.setStreakDays(streak(key, now.getDayOfMonth())); return view;
        } catch (Exception exception) {
            metrics.count("sign_in_failure_total", "explore_social", "status", "failure", errorType(exception));
            throw new BusinessException(503, "签到服务暂不可用");
        }
    }

    private Integer monthDays(final String key) {
        Long count = redisTemplate.execute(new RedisCallback<Long>() { @Override public Long doInRedis(org.springframework.data.redis.connection.RedisConnection connection) { return connection.stringCommands().bitCount(key.getBytes(StandardCharsets.UTF_8)); } });
        return count == null ? 0 : count.intValue();
    }

    private Integer streak(final String key, final int today) {
        List<Long> values = redisTemplate.execute(new RedisCallback<List<Long>>() { @Override public List<Long> doInRedis(org.springframework.data.redis.connection.RedisConnection connection) { return connection.stringCommands().bitField(key.getBytes(StandardCharsets.UTF_8), BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(today)).valueAt(0)); } });
        long bits = values == null || values.isEmpty() || values.get(0) == null ? 0L : values.get(0).longValue();
        if ((bits & 1L) == 0L) return 0;
        int days = 0; while ((bits & 1L) == 1L) { days++; bits >>>= 1; } return days;
    }

    private YearMonth parseMonth(String value) {
        if (value == null || value.trim().isEmpty()) return YearMonth.now(BUSINESS_ZONE);
        try {
            YearMonth month = YearMonth.parse(value, MONTH_FORMAT); YearMonth now = YearMonth.now(BUSINESS_ZONE);
            if (month.isAfter(now) || month.isBefore(now.minusMonths(11))) throw new BusinessException(400, "月份只能查询当前月及近12个月");
            return month;
        } catch (DateTimeParseException exception) { throw new BusinessException(400, "月份格式必须为yyyy-MM"); }
    }
    private String month(ZonedDateTime now) { return now.format(MONTH_FORMAT); }
    private Long requireUser() { Long userId = UserContext.getUserId(); if (userId == null) throw new BusinessException(401, "请先登录"); return userId; }
    private String errorType(Exception exception) { return exception.getClass().getSimpleName(); }
}
