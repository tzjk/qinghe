package com.qinghe.life.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.entity.Coupon;
import com.qinghe.life.entity.SeckillCouponOrder;
import com.qinghe.life.enums.CouponStatus;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.CouponMapper;
import com.qinghe.life.mapper.SeckillCouponOrderMapper;
import com.qinghe.life.redis.SeckillStreamErrorClassifier;
import com.qinghe.life.service.CouponSeckillService;
import com.qinghe.life.utils.RedisIdWorker;
import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.utils.SeckillTestContext;
import com.qinghe.life.utils.UserContext;
import com.qinghe.life.vo.SeckillOrderAcceptanceVO;
import com.qinghe.life.vo.SeckillOrderStatusVO;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
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
    private static final Logger LOG = LoggerFactory.getLogger(CouponSeckillServiceImpl.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final Pattern BUSY_GROUP = Pattern.compile("(^|\\s)BUSYGROUP(?:\\s|$)");
    private final CouponMapper couponMapper;
    private final SeckillCouponOrderMapper orderMapper;
    private final CouponSeckillPersistenceService persistenceService;
    private final StringRedisTemplate redis;
    private final RedisIdWorker idWorker;
    private final RedissonClient redisson;
    private final SeckillStreamErrorClassifier errorClassifier;
    private final DefaultRedisScript<Long> seckillScript = new DefaultRedisScript<Long>();
    private final DefaultRedisScript<Long> compensateScript = new DefaultRedisScript<Long>();
    @Value("${coupon.seckill.consumer-group:coupon-seckill-group}") private String consumerGroup;
    @Value("${coupon.seckill.consumer-name:coupon-seckill-local}") private String consumerName;
    @Value("${coupon.seckill.consume-batch-size:20}") private long consumeBatchSize;
    @Value("${coupon.seckill.stream-block-millis:2000}") private long streamBlockMillis;
    @Value("${coupon.seckill.pending-min-idle-seconds:30}") private long pendingMinIdleSeconds;
    @Value("${coupon.seckill.pending-batch-size:20}") private long pendingBatchSize;
    @Value("${coupon.seckill.max-retries:3}") private int maxRetries;
    @Value("${coupon.seckill.retry-base-delay:2s}") private Duration retryBaseDelay;
    @Value("${coupon.seckill.retry-max-delay:30s}") private Duration retryMaxDelay;
    @Value("${coupon.seckill.retry-ttl-seconds:86400}") private long retryTtlSeconds;
    @Value("${coupon.seckill.order-status-ttl-seconds:604800}") private long statusTtlSeconds;

    public CouponSeckillServiceImpl(CouponMapper couponMapper, SeckillCouponOrderMapper orderMapper,
            CouponSeckillPersistenceService persistenceService, StringRedisTemplate redis, RedisIdWorker idWorker,
            RedissonClient redisson, SeckillStreamErrorClassifier errorClassifier) {
        this.couponMapper = couponMapper; this.orderMapper = orderMapper; this.persistenceService = persistenceService;
        this.redis = redis; this.idWorker = idWorker; this.redisson = redisson; this.errorClassifier = errorClassifier;
    }

    @PostConstruct void loadScripts() {
        seckillScript.setLocation(new ClassPathResource("lua/seckill_coupon.lua")); seckillScript.setResultType(Long.class);
        compensateScript.setLocation(new ClassPathResource("lua/seckill_coupon_compensate.lua")); compensateScript.setResultType(Long.class);
    }

    @Override public void preheat(Long couponId) { initializeIfAbsent(couponId); }

    @Override public void markDisabled(Long couponId) {
        if (couponId != null && Boolean.TRUE.equals(redis.hasKey(RedisKeys.seckillMeta(couponId)))) {
            redis.opsForHash().put(RedisKeys.seckillMeta(couponId), "status", "DISABLED");
        }
    }

    @Override public SeckillOrderAcceptanceVO seckill(Long couponId) {
        Long userId = UserContext.getUserId();
        if (userId == null) throw new BusinessException(401, "请先登录");
        if (couponId == null) throw new BusinessException(400, "优惠券不能为空");
        initializeIfAbsent(couponId);
        long orderId = idWorker.nextSeckillOrderId(); String testRunId = SeckillTestContext.getTestRunId();
        Long result;
        try {
            result = redis.execute(seckillScript, Arrays.asList(RedisKeys.seckillStock(couponId), RedisKeys.seckillUsers(couponId),
                    RedisKeys.seckillMeta(couponId), RedisKeys.seckillStream(), RedisKeys.seckillStatus(orderId)),
                    String.valueOf(userId), String.valueOf(couponId), String.valueOf(System.currentTimeMillis()),
                    String.valueOf(orderId), String.valueOf(Math.max(60L, statusTtlSeconds)), testRunId == null ? "" : testRunId);
        } catch (DataAccessException ex) { throw new BusinessException(503, "秒杀服务暂时不可用"); }
        if (result == null) throw new BusinessException(503, "秒杀服务暂时不可用");
        switch (result.intValue()) {
            case 0: recordTestAcceptance(testRunId, orderId); return SeckillOrderAcceptanceVO.accepted(orderId);
            case 1: throw new BusinessException(409, "优惠券库存不足");
            case 2: throw new BusinessException(409, "每位用户只能抢购一次");
            case 3: throw new BusinessException(409, "秒杀活动未开始或已结束");
            default: throw new BusinessException(409, "秒杀活动状态异常");
        }
    }

    @Override public SeckillOrderStatusVO status(Long orderId) {
        Long userId = UserContext.getUserId();
        if (userId == null) throw new BusinessException(401, "请先登录");
        if (orderId == null) throw new BusinessException(400, "订单号不能为空");
        Map<Object, Object> state = redis.opsForHash().entries(RedisKeys.seckillStatus(orderId));
        if (!state.isEmpty()) {
            if (!String.valueOf(userId).equals(String.valueOf(state.get("userId")))) throw new BusinessException(403, "无权查询该秒杀订单");
            return SeckillOrderStatusVO.of(orderId, String.valueOf(state.get("status")));
        }
        SeckillCouponOrder order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException(404, "秒杀订单不存在或已过期");
        if (!userId.equals(order.getUserId())) throw new BusinessException(403, "无权查询该秒杀订单");
        return SeckillOrderStatusVO.of(orderId, order.getStatus());
    }

    @Override public void initializeConsumerGroup() { ensureGroup(); }

    @Override public void consumeNewMessages() {
        ensureGroup();
        List<MapRecord<String, Object, Object>> records = redis.opsForStream().read(Consumer.from(consumerGroup, consumerName),
                StreamReadOptions.empty().count(Math.max(1L, consumeBatchSize)).block(Duration.ofMillis(Math.max(100L, streamBlockMillis))),
                StreamOffset.create(RedisKeys.seckillStream(), ReadOffset.lastConsumed()));
        if (records != null) for (MapRecord<String, Object, Object> record : records) process(record);
    }

    @Override public void recoverPendingMessages() {
        ensureGroup();
        PendingMessages pending = redis.opsForStream().pending(RedisKeys.seckillStream(), consumerGroup, Range.unbounded(), Math.max(1L, pendingBatchSize));
        for (PendingMessage message : pending) {
            if (message.getElapsedTimeSinceLastDelivery().compareTo(Duration.ofSeconds(Math.max(1L, pendingMinIdleSeconds))) < 0 || !retryDue(message.getId().getValue())) continue;
            List<MapRecord<String, Object, Object>> claimed = redis.opsForStream().claim(RedisKeys.seckillStream(), consumerGroup, consumerName,
                    Duration.ofSeconds(Math.max(1L, pendingMinIdleSeconds)), message.getId());
            if (claimed != null) for (MapRecord<String, Object, Object> record : claimed) process(record);
        }
    }

    private void initializeIfAbsent(Long couponId) {
        if (Boolean.TRUE.equals(redis.hasKey(RedisKeys.seckillStock(couponId))) && Boolean.TRUE.equals(redis.hasKey(RedisKeys.seckillMeta(couponId)))) return;
        RLock lock = redisson.getLock(RedisKeys.seckillInitLock(couponId)); boolean acquired = false;
        try {
            acquired = lock.tryLock(0, TimeUnit.SECONDS);
            if (!acquired) throw new BusinessException(503, "秒杀活动正在初始化");
            if (Boolean.TRUE.equals(redis.hasKey(RedisKeys.seckillStock(couponId))) && Boolean.TRUE.equals(redis.hasKey(RedisKeys.seckillMeta(couponId)))) return;
            Coupon coupon = couponMapper.selectById(couponId);
            if (coupon == null || !SECKILL_COUPON_STATUS.equals(coupon.getCouponStatus()) || coupon.getAvailableStock() == null
                    || coupon.getReceiveStartTime() == null || coupon.getReceiveEndTime() == null || !coupon.getReceiveStartTime().isBefore(coupon.getReceiveEndTime())) {
                throw new BusinessException(409, "秒杀活动状态异常");
            }
            if (!Boolean.TRUE.equals(redis.hasKey(RedisKeys.seckillStock(couponId)))) {
                redis.opsForValue().set(RedisKeys.seckillStock(couponId), String.valueOf(Math.max(0, coupon.getAvailableStock())));
                redis.delete(RedisKeys.seckillUsers(couponId));
                List<SeckillCouponOrder> orders = orderMapper.selectList(Wrappers.<SeckillCouponOrder>lambdaQuery()
                        .eq(SeckillCouponOrder::getCouponId, couponId).ne(SeckillCouponOrder::getStatus, "FAILED"));
                for (SeckillCouponOrder order : orders) redis.opsForSet().add(RedisKeys.seckillUsers(couponId), String.valueOf(order.getUserId()));
            }
            Map<String, String> meta = new HashMap<String, String>();
            meta.put("couponId", String.valueOf(couponId)); meta.put("status", CouponStatus.ENABLED.name().equals(coupon.getStatus()) ? "ENABLED" : "DISABLED");
            meta.put("startAt", String.valueOf(epochMillis(coupon.getReceiveStartTime()))); meta.put("endAt", String.valueOf(epochMillis(coupon.getReceiveEndTime())));
            redis.opsForHash().putAll(RedisKeys.seckillMeta(couponId), meta);
        } catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new BusinessException(503, "秒杀活动初始化被中断"); }
        finally { if (acquired && lock.isHeldByCurrentThread()) lock.unlock(); }
    }

    private void process(MapRecord<String, Object, Object> record) {
        String messageId = record.getId().getValue(); if (!retryDue(messageId)) return;
        Long orderId = null; Long couponId = null; Long userId = null; String testRunId = null; RLock lock = null; boolean acquired = false;
        try {
            orderId = requiredLong(record, "orderId"); couponId = requiredLong(record, "couponId"); userId = requiredLong(record, "userId");
            testRunId = optionalText(record, "testRunId");
            lock = redisson.getLock(RedisKeys.seckillLock(couponId, userId));
            acquired = lock.tryLock(0, TimeUnit.SECONDS);
            if (!acquired) throw new IllegalStateException("SECKILL_CONSUMER_LOCK_UNAVAILABLE");
            if (hasText(testRunId)) redis.opsForSet().add(RedisKeys.seckillTestRunLockKeys(testRunId), RedisKeys.seckillLock(couponId, userId));
            updateStatus(orderId, "PROCESSING");
            persistenceService.persist(orderId, couponId, userId);
            updateStatus(orderId, "SUCCESS");
            acknowledge(record); redis.delete(RedisKeys.couponSeckillRetry(messageId));
            if (hasText(testRunId)) redis.opsForSet().add(RedisKeys.seckillTestRunAckIds(testRunId), messageId);
        } catch (Exception ex) { handleFailure(record, ex, orderId, couponId, userId, testRunId); }
        finally { if (acquired && lock != null && lock.isHeldByCurrentThread()) lock.unlock(); }
    }

    private void handleFailure(MapRecord<String, Object, Object> record, Exception ex, Long orderId, Long couponId, Long userId, String testRunId) {
        String messageId = record.getId().getValue(); SeckillStreamErrorClassifier.Classification type = errorClassifier.classify(ex);
        RetryState prior = retryState(messageId); int attempts = prior.attempts + 1; long now = System.currentTimeMillis();
        if (!type.isRetryable() || attempts >= Math.max(1, maxRetries)) {
            if (moveToDlq(record, attempts, type.getCode(), now, testRunId)) {
                if (orderId != null && couponId != null && userId != null && !persistenceService.exists(orderId)) compensate(orderId, couponId, userId);
                try { acknowledge(record); redis.delete(RedisKeys.couponSeckillRetry(messageId)); }
                catch (Exception ackError) { LOG.error("秒杀死信已写入但 ACK 失败，messageId={}", messageId, ackError); }
            }
            return;
        }
        long next = now + retryDelayMillis(attempts);
        redis.opsForValue().set(RedisKeys.couponSeckillRetry(messageId), attempts + "|" + next, Math.max(60L, retryTtlSeconds), TimeUnit.SECONDS);
        LOG.warn("秒杀订单保留在 Pending 等待重试，messageId={}, attempt={}, type={}", messageId, attempts, type.getCode());
    }

    private boolean moveToDlq(MapRecord<String, Object, Object> record, int attempts, String failureCode, long now, String testRunId) {
        try {
            Map<String, String> deadLetter = new HashMap<String, String>();
            deadLetter.put("originalMessageId", record.getId().getValue()); deadLetter.put("retryCount", String.valueOf(attempts)); deadLetter.put("failureCode", failureCode); deadLetter.put("failedAt", String.valueOf(now));
            deadLetter.put("orderId", text(record, "orderId")); deadLetter.put("couponId", text(record, "couponId")); deadLetter.put("userId", text(record, "userId"));
            if (hasText(testRunId)) deadLetter.put("testRunId", testRunId);
            org.springframework.data.redis.connection.stream.RecordId dlqId = redis.opsForStream().add(RedisKeys.couponSeckillDlqStream(), deadLetter);
            if (hasText(testRunId) && dlqId != null) redis.opsForSet().add(RedisKeys.seckillTestRunDlqIds(testRunId), dlqId.getValue());
            return true;
        } catch (Exception failure) { LOG.error("秒杀死信写入失败，原消息保持 Pending，messageId={}", record.getId().getValue(), failure); return false; }
    }

    private void compensate(Long orderId, Long couponId, Long userId) {
        Long compensated = redis.execute(compensateScript, Arrays.asList(RedisKeys.seckillStock(couponId), RedisKeys.seckillUsers(couponId), RedisKeys.seckillStatus(orderId)), String.valueOf(userId), String.valueOf(couponId));
        if (compensated == null) LOG.error("秒杀失败补偿未确认，orderId={}", orderId);
    }
    private void updateStatus(Long orderId, String status) { redis.opsForHash().put(RedisKeys.seckillStatus(orderId), "status", status); }
    private void acknowledge(MapRecord<String, Object, Object> record) {
        Long count = redis.opsForStream().acknowledge(RedisKeys.seckillStream(), consumerGroup, record.getId());
        if (count == null || count.longValue() != 1L) throw new IllegalStateException("STREAM_ACK_FAILED");
        String testRunId = optionalText(record, "testRunId");
        if (hasText(testRunId)) redis.opsForSet().add(RedisKeys.seckillTestRunAckIds(testRunId), record.getId().getValue());
    }
    private boolean retryDue(String messageId) { return retryState(messageId).nextRetryAt <= System.currentTimeMillis(); }
    private RetryState retryState(String messageId) {
        String state = redis.opsForValue().get(RedisKeys.couponSeckillRetry(messageId)); if (state == null) return new RetryState(0, 0L);
        String[] parts = state.split("\\|"); try { return new RetryState(Integer.parseInt(parts[0]), Long.parseLong(parts[1])); } catch (Exception ignored) { return new RetryState(0, 0L); }
    }
    private long retryDelayMillis(int attempts) { long base = Math.max(1L, retryBaseDelay.toMillis()); return Math.min(Math.max(base, retryMaxDelay.toMillis()), base * (1L << Math.min(20, Math.max(0, attempts - 1)))); }
    private void ensureGroup() {
        try { redis.execute((RedisCallback<String>) connection -> connection.xGroupCreate(RedisKeys.seckillStream().getBytes(StandardCharsets.UTF_8), consumerGroup, ReadOffset.from("0-0"), true)); }
        catch (DataAccessException ex) { if (!busyGroup(ex)) throw ex; }
    }
    private boolean busyGroup(DataAccessException ex) { for (Throwable cause = ex; cause != null; cause = cause.getCause()) { if (cause.getMessage() != null && BUSY_GROUP.matcher(cause.getMessage()).find()) return true; } return false; }
    private long epochMillis(LocalDateTime value) { return value.atZone(ZONE).toInstant().toEpochMilli(); }
    private Long requiredLong(MapRecord<String, Object, Object> record, String field) { try { return Long.valueOf(text(record, field)); } catch (Exception ex) { throw new IllegalArgumentException("STREAM_MESSAGE_INVALID"); } }
    private String text(MapRecord<String, Object, Object> record, String field) { Object value = record.getValue().get(field); if (value == null || String.valueOf(value).trim().isEmpty()) throw new IllegalArgumentException("STREAM_MESSAGE_INVALID"); return String.valueOf(value); }
    private String optionalText(MapRecord<String, Object, Object> record, String field) { Object value = record.getValue().get(field); return value == null ? null : String.valueOf(value); }
    private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
    private void recordTestAcceptance(String testRunId, long orderId) {
        if (!hasText(testRunId)) return;
        String statusKey = RedisKeys.seckillStatus(orderId);
        Object streamMessageId = redis.opsForHash().get(statusKey, "streamMessageId");
        redis.opsForSet().add(RedisKeys.seckillTestRunOrderIds(testRunId), String.valueOf(orderId));
        redis.opsForSet().add(RedisKeys.seckillTestRunStatusKeys(testRunId), statusKey);
        if (streamMessageId != null) redis.opsForSet().add(RedisKeys.seckillTestRunStreamIds(testRunId), String.valueOf(streamMessageId));
    }
    private static final class RetryState { private final int attempts; private final long nextRetryAt; private RetryState(int attempts, long nextRetryAt) { this.attempts = attempts; this.nextRetryAt = nextRetryAt; } }
}
