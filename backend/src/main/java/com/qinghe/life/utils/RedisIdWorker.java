package com.qinghe.life.utils;

import com.qinghe.life.exception.BusinessException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * Generates a 64-bit id from elapsed seconds and a Redis daily sequence.
 * Redis INCR makes the sequence safe across application instances.
 */
@Component
public class RedisIdWorker {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final LocalDate EPOCH = LocalDate.of(2024, 1, 1);
    private static final long SEQUENCE_MASK = 0xFFFFFFFFL;
    private static final DefaultRedisScript<Long> NEXT_SEQUENCE = new DefaultRedisScript<Long>(
            "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],ARGV[1]); end; return n", Long.class);
    private final StringRedisTemplate redisTemplate;

    public RedisIdWorker(StringRedisTemplate redisTemplate) { this.redisTemplate = redisTemplate; }

    public long nextSeckillOrderId() {
        ZonedDateTime now = ZonedDateTime.now(BUSINESS_ZONE); LocalDate today = now.toLocalDate();
        Long sequence = redisTemplate.execute(NEXT_SEQUENCE, Collections.singletonList(RedisKeys.seckillId(today)), "172800");
        if (sequence == null || sequence.longValue() <= 0L || sequence.longValue() > SEQUENCE_MASK) {
            throw new BusinessException(503, "秒杀订单号暂时不可用");
        }
        long seconds = now.toEpochSecond() - EPOCH.atStartOfDay(BUSINESS_ZONE).toEpochSecond();
        return (seconds << 32) | sequence.longValue();
    }
}
