package com.qinghe.life.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.entity.Coupon;
import com.qinghe.life.enums.CouponClaimStatus;
import com.qinghe.life.enums.CouponStatus;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.CouponMapper;
import com.qinghe.life.service.CouponSeckillService;
import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.vo.CouponClaimVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class CouponSeckillServiceImpl implements CouponSeckillService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CouponSeckillServiceImpl.class);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Pattern BUSY_GROUP = Pattern.compile("(^|\\s)BUSYGROUP(?:\\s|$)");
    private final CouponMapper couponMapper;
    private final StringRedisTemplate redisTemplate;
    private final CouponSeckillPersistenceService persistenceService;
    private final DefaultRedisScript<Long> claimScript = new DefaultRedisScript<Long>();

    @Value("${coupon.seckill.consumer-group:coupon-seckill-group}")
    private String consumerGroup;
    @Value("${coupon.seckill.stream-key:qh:stream:coupon:claim}")
    private String streamKey;
    @Value("${coupon.seckill.consumer-name:coupon-seckill-local}")
    private String consumerName;
    @Value("${coupon.seckill.consume-batch-size:20}")
    private long consumeBatchSize;
    @Value("${coupon.seckill.pending-min-idle-seconds:30}")
    private long pendingMinIdleSeconds;
    @Value("${coupon.seckill.pending-max-retries:3}")
    private int pendingMaxRetries;

    public CouponSeckillServiceImpl(CouponMapper couponMapper, StringRedisTemplate redisTemplate,
                                    CouponSeckillPersistenceService persistenceService) {
        this.couponMapper = couponMapper;
        this.redisTemplate = redisTemplate;
        this.persistenceService = persistenceService;
    }

    @PostConstruct
    void configureScript() {
        claimScript.setLocation(new ClassPathResource("lua/coupon-seckill-claim.lua"));
        claimScript.setResultType(Long.class);
    }

    @Override
    public void preheat(Long couponId) {
        Coupon coupon = requireSeckillCoupon(couponId);
        LocalDateTime now = LocalDateTime.now();
        if (!CouponStatus.ENABLED.name().equals(coupon.getStatus())) throw new BusinessException(409, "Seckill activity is disabled");
        if (coupon.getAvailableStock() == null || coupon.getAvailableStock() <= 0) throw new BusinessException(409, "Seckill stock is unavailable");
        if (coupon.getReceiveStartTime() == null || coupon.getReceiveEndTime() == null || !coupon.getReceiveStartTime().isBefore(coupon.getReceiveEndTime())) {
            throw new BusinessException(409, "Seckill activity window is invalid");
        }
        String metaKey = RedisKeys.couponSeckillMeta(couponId);
        Map<Object, Object> existing = redisTemplate.opsForHash().entries(metaKey);
        if (!existing.isEmpty() && "ENABLED".equals(existing.get("status"))
                && now.toInstant(BUSINESS_ZONE.getRules().getOffset(now)).toEpochMilli() <= longValue(existing.get("endAt"))) {
            throw new BusinessException(409, "Seckill activity is already running and cannot be overwritten");
        }
        redisTemplate.delete(Arrays.asList(RedisKeys.couponSeckillStock(couponId), RedisKeys.couponSeckillUsers(couponId), metaKey));
        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("couponId", String.valueOf(couponId));
        metadata.put("status", CouponStatus.ENABLED.name());
        metadata.put("startAt", String.valueOf(epochMillis(coupon.getReceiveStartTime())));
        metadata.put("endAt", String.valueOf(epochMillis(coupon.getReceiveEndTime())));
        redisTemplate.opsForValue().set(RedisKeys.couponSeckillStock(couponId), String.valueOf(coupon.getAvailableStock()));
        redisTemplate.opsForHash().putAll(metaKey, metadata);
    }

    @Override
    public CouponClaimVO claim(Long couponId, Long userId) {
        if (couponId == null || userId == null) throw new BusinessException(400, "Invalid seckill claim request");
        Long outcome;
        try {
            outcome = redisTemplate.execute(claimScript, Arrays.asList(RedisKeys.couponSeckillStock(couponId),
                    RedisKeys.couponSeckillUsers(couponId), RedisKeys.couponSeckillMeta(couponId), streamKey),
                    String.valueOf(userId), String.valueOf(couponId), String.valueOf(epochMillis(LocalDateTime.now())));
        } catch (DataAccessException exception) {
            throw new BusinessException(503, "Seckill service is temporarily unavailable");
        }
        if (outcome == null) throw new BusinessException(503, "Seckill service is temporarily unavailable");
        switch (outcome.intValue()) {
            case 0: return CouponClaimVO.seckillAccepted();
            case 1: return CouponClaimVO.unavailable(CouponClaimStatus.ALREADY_CLAIMED, "Coupon has already been claimed");
            case 2: return CouponClaimVO.unavailable(CouponClaimStatus.OUT_OF_STOCK, "Coupon stock is unavailable");
            case 3: return CouponClaimVO.unavailable(CouponClaimStatus.NOT_STARTED, "Seckill activity has not started");
            case 4: return CouponClaimVO.unavailable(CouponClaimStatus.ENDED, "Seckill activity has ended");
            case 5: return CouponClaimVO.unavailable(CouponClaimStatus.ACTIVITY_DISABLED, "Seckill activity is disabled");
            default: return CouponClaimVO.unavailable(CouponClaimStatus.ACTIVITY_NOT_READY, "Seckill activity is not ready");
        }
    }

    @Override
    public void markDisabled(Long couponId) {
        if (couponId != null && Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeys.couponSeckillMeta(couponId)))) {
            redisTemplate.opsForHash().put(RedisKeys.couponSeckillMeta(couponId), "status", CouponStatus.DISABLED.name());
        }
    }

    @Override
    public void consumeNewMessages() {
        ensureGroup();
        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(Consumer.from(consumerGroup, consumerName),
                StreamReadOptions.empty().count(consumeBatchSize), StreamOffset.create(streamKey, ReadOffset.lastConsumed()));
        if (records != null) for (MapRecord<String, Object, Object> record : records) process(record);
    }

    @Override
    public void recoverPendingMessages() {
        ensureGroup();
        PendingMessages pending = redisTemplate.opsForStream().pending(streamKey, consumerGroup, Range.unbounded(), consumeBatchSize);
        for (PendingMessage message : pending) {
            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().claim(streamKey, consumerGroup, consumerName,
                    Duration.ofSeconds(Math.max(1, pendingMinIdleSeconds)), message.getId());
            if (records != null) for (MapRecord<String, Object, Object> record : records) process(record);
        }
    }

    private void ensureGroup() {
        try {
            redisTemplate.execute((RedisCallback<String>) connection -> connection.xGroupCreate(
                    streamKey.getBytes(StandardCharsets.UTF_8), consumerGroup, ReadOffset.from("0-0"), true));
        } catch (DataAccessException exception) {
            if (!isBusyGroup(exception)) throw exception;
        }
    }

    private void process(MapRecord<String, Object, Object> record) {
        String messageId = record.getId().getValue();
        try {
            Long couponId = requiredLong(record, "couponId");
            Long userId = requiredLong(record, "userId");
            persistenceService.persist(couponId, userId);
            redisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, record.getId());
            redisTemplate.opsForHash().delete(RedisKeys.couponSeckillRetry(), messageId);
        } catch (Exception exception) {
            long attempts = redisTemplate.opsForHash().increment(RedisKeys.couponSeckillRetry(), messageId, 1L);
            if (attempts >= Math.max(1, pendingMaxRetries)) {
                redisTemplate.opsForHash().put(RedisKeys.couponSeckillFailure(), messageId, conciseReason(exception));
                redisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, record.getId());
                LOGGER.error("Coupon seckill stream message permanently failed: {}", messageId, exception);
            } else {
                LOGGER.warn("Coupon seckill stream message remains pending: {}, attempt {}", messageId, attempts, exception);
            }
        }
    }

    private Coupon requireSeckillCoupon(Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) throw new BusinessException(404, "Coupon does not exist");
        if (!SECKILL_COUPON_STATUS.equals(coupon.getCouponStatus())) throw new BusinessException(409, "Coupon is not a seckill activity");
        return coupon;
    }
    private long epochMillis(LocalDateTime time) { return time.atZone(BUSINESS_ZONE).toInstant().toEpochMilli(); }
    private long longValue(Object value) { try { return Long.parseLong(String.valueOf(value)); } catch (Exception ignored) { return Long.MIN_VALUE; } }
    private Long requiredLong(MapRecord<String, Object, Object> record, String field) { Object value = record.getValue().get(field); try { return Long.valueOf(String.valueOf(value)); } catch (Exception exception) { throw new BusinessException(400, "Invalid stream " + field); } }
    private String conciseReason(Exception exception) { String message = exception.getMessage(); return exception.getClass().getSimpleName() + ":" + (message == null ? "unknown" : message.substring(0, Math.min(180, message.length()))); }
    private boolean isBusyGroup(DataAccessException exception) { for (Throwable cause = exception; cause != null; cause = cause.getCause()) { String message = cause.getMessage(); if (message != null && BUSY_GROUP.matcher(message).find()) return true; } return false; }
}
