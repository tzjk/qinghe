package com.qinghe.life.service.impl;

import com.qinghe.life.entity.Coupon;
import com.qinghe.life.enums.CouponClaimStatus;
import com.qinghe.life.enums.CouponStatus;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.CouponMapper;
import com.qinghe.life.redis.RedisBusinessMetrics;
import com.qinghe.life.redis.SeckillStreamErrorClassifier;
import com.qinghe.life.service.CouponSeckillService;
import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.vo.CouponClaimVO;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
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
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class CouponSeckillServiceImpl implements CouponSeckillService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CouponSeckillServiceImpl.class);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Pattern BUSY_GROUP = Pattern.compile("(^|\\s)BUSYGROUP(?:\\s|$)");
    private final CouponMapper couponMapper; private final StringRedisTemplate redisTemplate;
    private final CouponSeckillPersistenceService persistenceService; private final RedisBusinessMetrics metrics;
    private final SeckillStreamErrorClassifier errorClassifier;
    private final DefaultRedisScript<Long> claimScript = new DefaultRedisScript<Long>();
    private final DefaultRedisScript<String> dlqScript = new DefaultRedisScript<String>();
    @Value("${coupon.seckill.consumer-group:coupon-seckill-group}") private String consumerGroup;
    @Value("${coupon.seckill.consumer-name:coupon-seckill-local}") private String consumerName;
    @Value("${coupon.seckill.consume-batch-size:20}") private long consumeBatchSize;
    @Value("${coupon.seckill.pending-min-idle-seconds:30}") private long pendingMinIdleSeconds;
    @Value("${coupon.seckill.pending-batch-size:20}") private long pendingBatchSize;
    @Value("${coupon.seckill.max-retries:3}") private int maxRetries;
    @Value("${coupon.seckill.retry-base-delay:2s}") private Duration retryBaseDelay;
    @Value("${coupon.seckill.retry-max-delay:30s}") private Duration retryMaxDelay;
    @Value("${coupon.seckill.retry-ttl-seconds:86400}") private long retryTtlSeconds;
    @Value("${coupon.seckill.reservation-ttl-seconds:604800}") private long reservationTtlSeconds;
    @Value("${coupon.seckill.dlq-index-ttl-seconds:2592000}") private long dlqIndexTtlSeconds;
    @Value("${coupon.seckill.stream-alert-length:80000}") private long streamAlertLength;
    @Value("${coupon.seckill.dlq-alert-length:40000}") private long dlqAlertLength;

    public CouponSeckillServiceImpl(CouponMapper couponMapper, StringRedisTemplate redisTemplate,
            CouponSeckillPersistenceService persistenceService, RedisBusinessMetrics metrics,
            SeckillStreamErrorClassifier errorClassifier) {
        this.couponMapper = couponMapper; this.redisTemplate = redisTemplate; this.persistenceService = persistenceService;
        this.metrics = metrics; this.errorClassifier = errorClassifier;
    }
    @PostConstruct void configureScripts() {
        claimScript.setLocation(new ClassPathResource("lua/coupon-seckill-claim.lua")); claimScript.setResultType(Long.class);
        dlqScript.setLocation(new ClassPathResource("lua/coupon-seckill-dlq.lua")); dlqScript.setResultType(String.class);
    }
    @Override public void preheat(Long couponId) {
        Coupon coupon = requireSeckillCoupon(couponId); LocalDateTime now = LocalDateTime.now();
        if (!CouponStatus.ENABLED.name().equals(coupon.getStatus())) throw new BusinessException(409, "Seckill activity is disabled");
        if (coupon.getAvailableStock() == null || coupon.getAvailableStock() <= 0) throw new BusinessException(409, "Seckill stock is unavailable");
        if (coupon.getReceiveStartTime() == null || coupon.getReceiveEndTime() == null || !coupon.getReceiveStartTime().isBefore(coupon.getReceiveEndTime())) throw new BusinessException(409, "Seckill activity window is invalid");
        String metaKey = RedisKeys.couponSeckillMeta(couponId); Map<Object, Object> existing = redisTemplate.opsForHash().entries(metaKey);
        if (!existing.isEmpty() && "ENABLED".equals(existing.get("status")) && epochMillis(now) <= longValue(existing.get("endAt"))) throw new BusinessException(409, "Seckill activity is already running and cannot be overwritten");
        redisTemplate.delete(Arrays.asList(RedisKeys.couponSeckillStock(couponId), RedisKeys.couponSeckillUsers(couponId), metaKey));
        Map<String, String> metadata = new HashMap<String, String>(); metadata.put("couponId", String.valueOf(couponId)); metadata.put("status", CouponStatus.ENABLED.name()); metadata.put("startAt", String.valueOf(epochMillis(coupon.getReceiveStartTime()))); metadata.put("endAt", String.valueOf(epochMillis(coupon.getReceiveEndTime())));
        redisTemplate.opsForValue().set(RedisKeys.couponSeckillStock(couponId), String.valueOf(coupon.getAvailableStock())); redisTemplate.opsForHash().putAll(metaKey, metadata);
    }
    @Override public CouponClaimVO claim(Long couponId, Long userId) {
        if (couponId == null || userId == null) throw new BusinessException(400, "Invalid seckill claim request");
        String orderId = UUID.randomUUID().toString().replace("-", ""); Long outcome;
        try { outcome = redisTemplate.execute(claimScript, Arrays.asList(RedisKeys.couponSeckillStock(couponId), RedisKeys.couponSeckillUsers(couponId), RedisKeys.couponSeckillMeta(couponId), RedisKeys.couponSeckillStream(), RedisKeys.couponSeckillReservation(orderId)), String.valueOf(userId), String.valueOf(couponId), String.valueOf(epochMillis(LocalDateTime.now())), orderId, String.valueOf(Math.max(60L, reservationTtlSeconds))); }
        catch (DataAccessException exception) { metrics.count("seckill_reservation_failure_total", "seckill", "claim", "failure", "REDIS_UNAVAILABLE"); throw new BusinessException(503, "Seckill service is temporarily unavailable"); }
        if (outcome == null) { metrics.count("seckill_reservation_failure_total", "seckill", "claim", "failure", "SECKILL_RESERVATION_FAILED"); throw new BusinessException(503, "Seckill service is temporarily unavailable"); }
        switch (outcome.intValue()) {
            case 0: metrics.count("stream_produced_total", "seckill", "claim", "success", "none"); return CouponClaimVO.seckillAccepted();
            case 1: return CouponClaimVO.unavailable(CouponClaimStatus.ALREADY_CLAIMED, "Coupon has already been claimed");
            case 2: return CouponClaimVO.unavailable(CouponClaimStatus.OUT_OF_STOCK, "Coupon stock is unavailable");
            case 3: return CouponClaimVO.unavailable(CouponClaimStatus.NOT_STARTED, "Seckill activity has not started");
            case 4: return CouponClaimVO.unavailable(CouponClaimStatus.ENDED, "Seckill activity has ended");
            case 5: return CouponClaimVO.unavailable(CouponClaimStatus.ACTIVITY_DISABLED, "Seckill activity is disabled");
            case 8: metrics.count("seckill_reservation_failure_total", "seckill", "claim", "failure", "REDIS_STRUCTURE_INVALID"); LOGGER.error("Seckill Redis structure invalid for couponId={}", couponId); throw new BusinessException(503, "Seckill service data is unavailable");
            case 9: metrics.count("seckill_reservation_failure_total", "seckill", "claim", "failure", "STREAM_ENQUEUE_FAILED"); LOGGER.error("Seckill stream enqueue rolled back for couponId={}", couponId); throw new BusinessException(503, "Seckill service is temporarily unavailable");
            default: return CouponClaimVO.unavailable(CouponClaimStatus.ACTIVITY_NOT_READY, "Seckill activity is not ready");
        }
    }
    @Override public void markDisabled(Long couponId) { if (couponId != null && Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeys.couponSeckillMeta(couponId)))) redisTemplate.opsForHash().put(RedisKeys.couponSeckillMeta(couponId), "status", CouponStatus.DISABLED.name()); }
    @Override public void consumeNewMessages() { ensureGroup(); observeCapacity(RedisKeys.couponSeckillStream(), streamAlertLength, "main"); observeCapacity(RedisKeys.couponSeckillDlqStream(), dlqAlertLength, "dlq"); List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(Consumer.from(consumerGroup, consumerName), StreamReadOptions.empty().count(Math.max(1, consumeBatchSize)), StreamOffset.create(RedisKeys.couponSeckillStream(), ReadOffset.lastConsumed())); if (records != null) for (MapRecord<String, Object, Object> record : records) process(record, false); }
    @Override public void recoverPendingMessages() {
        ensureGroup(); PendingMessages pending = redisTemplate.opsForStream().pending(RedisKeys.couponSeckillStream(), consumerGroup, Range.unbounded(), Math.max(1, pendingBatchSize));
        for (PendingMessage message : pending) {
            if (message.getElapsedTimeSinceLastDelivery().compareTo(Duration.ofSeconds(Math.max(1L, pendingMinIdleSeconds))) < 0 || !retryDue(message.getId().getValue())) continue;
            try { List<MapRecord<String, Object, Object>> claimed = redisTemplate.opsForStream().claim(RedisKeys.couponSeckillStream(), consumerGroup, consumerName, Duration.ofSeconds(Math.max(1L, pendingMinIdleSeconds)), message.getId()); if (claimed != null) for (MapRecord<String, Object, Object> record : claimed) { metrics.count("stream_pending_recovered_total", "seckill", "claim", "success", "none"); process(record, true); } }
            catch (Exception exception) { LOGGER.warn("Pending Stream message claim failed, messageId={}, errorType={}", message.getId().getValue(), exception.getClass().getSimpleName()); }
        }
    }
    private void process(MapRecord<String, Object, Object> record, boolean recovered) {
        String messageId = record.getId().getValue(); if (!retryDue(messageId)) return;
        try { Long couponId = requiredLong(record, "couponId"); Long userId = requiredLong(record, "userId"); String orderId = requiredText(record, "orderId"); persistenceService.persist(couponId, userId); markReservation(orderId, "PERSISTED"); acknowledge(record); redisTemplate.delete(RedisKeys.couponSeckillRetry(messageId)); metrics.count("stream_consumed_total", "seckill", recovered ? "recover" : "consume", "success", "none"); }
        catch (Exception exception) { handleFailure(record, exception); }
    }
    private void handleFailure(MapRecord<String, Object, Object> record, Exception exception) {
        String messageId = record.getId().getValue(); SeckillStreamErrorClassifier.Classification classification = errorClassifier.classify(exception); RetryState prior = retryState(messageId); int attempts = prior.attempts + 1; long now = System.currentTimeMillis(); long firstFailedAt = prior.firstFailedAt == 0 ? now : prior.firstFailedAt;
        if (!classification.isRetryable() || attempts >= Math.max(1, maxRetries)) { if (moveToDlq(record, attempts, classification.getCode(), firstFailedAt, now)) { try { acknowledge(record); redisTemplate.delete(RedisKeys.couponSeckillRetry(messageId)); markReservation(record, "DEAD_LETTERED"); metrics.count("stream_dlq_total", "seckill", "dlq", "success", classification.getCode()); } catch (Exception ackFailure) { metrics.count("stream_dlq_write_failure_total", "seckill", "ack", "failure", "STREAM_ACK_FAILED"); LOGGER.error("DLQ created but main stream ACK failed, messageId={}, errorType={}", messageId, ackFailure.getClass().getSimpleName()); } } return; }
        long nextRetryAt = now + retryDelayMillis(attempts); redisTemplate.opsForValue().set(RedisKeys.couponSeckillRetry(messageId), attempts + "|" + firstFailedAt + "|" + nextRetryAt, Math.max(60L, retryTtlSeconds), java.util.concurrent.TimeUnit.SECONDS); metrics.count("stream_retry_total", "seckill", "retry", "pending", classification.getCode()); LOGGER.warn("Seckill stream message retained for delayed retry, messageId={}, attempt={}, errorType={}", messageId, attempts, classification.getCode());
    }
    private boolean moveToDlq(MapRecord<String, Object, Object> record, int attempts, String failureCode, long firstFailedAt, long now) {
        String messageId = record.getId().getValue(); try { String result = redisTemplate.execute(dlqScript, Arrays.asList(RedisKeys.couponSeckillDlqStream(), RedisKeys.couponSeckillDlqIndex(messageId)), messageId, String.valueOf(attempts), failureCode, failureCode, String.valueOf(firstFailedAt), String.valueOf(now), consumerName, optionalText(record, "orderId"), optionalText(record, "couponId"), optionalText(record, "userId"), String.valueOf(Math.max(60L, dlqIndexTtlSeconds))); if (result == null || result.trim().isEmpty()) throw new IllegalStateException("DLQ write returned no message id"); return true; } catch (Exception dlqFailure) { metrics.count("stream_dlq_write_failure_total", "seckill", "dlq", "failure", "STREAM_DLQ_FAILED"); LOGGER.error("DLQ write failed; original message remains pending, messageId={}, errorType={}", messageId, dlqFailure.getClass().getSimpleName()); return false; }
    }
    private void acknowledge(MapRecord<String, Object, Object> record) { Long acknowledged = redisTemplate.opsForStream().acknowledge(RedisKeys.couponSeckillStream(), consumerGroup, record.getId()); if (acknowledged == null || acknowledged.longValue() != 1L) throw new IllegalStateException("STREAM_ACK_FAILED"); metrics.count("stream_ack_total", "seckill", "ack", "success", "none"); }
    private void observeCapacity(String key, long alertLength, String streamType) { try { Long length = redisTemplate.opsForStream().size(key); if (length != null && length.longValue() >= Math.max(1L, alertLength)) { metrics.count("stream_capacity_alert_total", "seckill", streamType, "threshold", "none"); LOGGER.warn("Seckill {} stream capacity threshold reached, length={}", streamType, length); } } catch (Exception exception) { LOGGER.warn("Seckill {} stream capacity observation failed, errorType={}", streamType, exception.getClass().getSimpleName()); } }
    private void markReservation(MapRecord<String, Object, Object> record, String status) { markReservation(optionalText(record, "orderId"), status); }
    private void markReservation(String orderId, String status) { if (orderId != null && !orderId.isEmpty()) redisTemplate.opsForHash().put(RedisKeys.couponSeckillReservation(orderId), "status", status); }
    private boolean retryDue(String messageId) { return retryState(messageId).nextRetryAt <= System.currentTimeMillis(); }
    private RetryState retryState(String messageId) { String raw = redisTemplate.opsForValue().get(RedisKeys.couponSeckillRetry(messageId)); if (raw == null) return new RetryState(0, 0L, 0L); String[] parts = raw.split("\\|"); try { return new RetryState(Integer.parseInt(parts[0]), Long.parseLong(parts[1]), Long.parseLong(parts[2])); } catch (Exception ignored) { return new RetryState(0, 0L, 0L); } }
    private long retryDelayMillis(int attempts) { long base = Math.max(1L, retryBaseDelay.toMillis()); long cap = Math.max(base, retryMaxDelay.toMillis()); long multiplier = 1L << Math.min(20, Math.max(0, attempts - 1)); return Math.min(cap, base * multiplier); }
    private void ensureGroup() { try { redisTemplate.execute((RedisCallback<String>) connection -> connection.xGroupCreate(RedisKeys.couponSeckillStream().getBytes(StandardCharsets.UTF_8), consumerGroup, ReadOffset.from("0-0"), true)); } catch (DataAccessException exception) { if (!isBusyGroup(exception)) throw exception; } }
    private Coupon requireSeckillCoupon(Long couponId) { Coupon coupon = couponMapper.selectById(couponId); if (coupon == null) throw new BusinessException(404, "Coupon does not exist"); if (!SECKILL_COUPON_STATUS.equals(coupon.getCouponStatus())) throw new BusinessException(409, "Coupon is not a seckill activity"); return coupon; }
    private long epochMillis(LocalDateTime time) { return time.atZone(BUSINESS_ZONE).toInstant().toEpochMilli(); }
    private long longValue(Object value) { try { return Long.parseLong(String.valueOf(value)); } catch (Exception ignored) { return Long.MIN_VALUE; } }
    private Long requiredLong(MapRecord<String, Object, Object> record, String field) { try { return Long.valueOf(requiredText(record, field)); } catch (Exception exception) { throw new IllegalArgumentException("STREAM_MESSAGE_INVALID"); } }
    private String requiredText(MapRecord<String, Object, Object> record, String field) { String value = optionalText(record, field); if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("STREAM_MESSAGE_INVALID"); return value; }
    private String optionalText(MapRecord<String, Object, Object> record, String field) { Object value = record.getValue().get(field); return value == null ? "" : String.valueOf(value); }
    private boolean isBusyGroup(DataAccessException exception) { for (Throwable cause = exception; cause != null; cause = cause.getCause()) { String message = cause.getMessage(); if (message != null && BUSY_GROUP.matcher(message).find()) return true; } return false; }
    private static final class RetryState { private final int attempts; private final long firstFailedAt; private final long nextRetryAt; private RetryState(int attempts, long firstFailedAt, long nextRetryAt) { this.attempts = attempts; this.firstFailedAt = firstFailedAt; this.nextRetryAt = nextRetryAt; } }
}
