package com.qinghe.life.task;

import com.qinghe.life.service.CouponSeckillService;
import com.qinghe.life.redis.RedisBusinessMetrics;
import com.qinghe.life.utils.RedisKeys;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(prefix = "coupon.seckill", name = "consume-enabled", havingValue = "true", matchIfMissing = true)
public class CouponSeckillStreamTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(CouponSeckillStreamTask.class);
    private final CouponSeckillService couponSeckillService;
    private final RedissonClient redissonClient;
    private final RedisBusinessMetrics metrics;
    @Value("${coupon.seckill.pending-lock-wait-seconds:0}") private long lockWaitSeconds;
    public CouponSeckillStreamTask(CouponSeckillService couponSeckillService, RedissonClient redissonClient, RedisBusinessMetrics metrics) { this.couponSeckillService = couponSeckillService; this.redissonClient = redissonClient; this.metrics = metrics; }
    @Scheduled(fixedDelayString = "${coupon.seckill.consume-delay-millis:500}")
    public void consumeNewMessages() { try { couponSeckillService.consumeNewMessages(); } catch (Exception exception) { LOGGER.error("Coupon seckill stream consumption failed", exception); } }
    @Scheduled(cron = "${coupon.seckill.pending-recovery-cron:*/15 * * * * ?}")
    public void recoverPendingMessages() {
        RLock lock = null;
        try { long started = System.nanoTime(); lock = redissonClient.getLock(RedisKeys.couponSeckillRecoveryLock()); if (!lock.tryLock(Math.max(0, lockWaitSeconds), TimeUnit.SECONDS)) { metrics.count("redis_lock_acquire_failure_total", "seckill", "pending-recovery", "failure", "LOCK_ACQUIRE_FAILED"); return; } metrics.lockWait("seckill", "pending-recovery", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)); metrics.count("redis_lock_acquire_total", "seckill", "pending-recovery", "success", "none"); couponSeckillService.recoverPendingMessages(); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); LOGGER.warn("Coupon seckill pending recovery interrupted", exception); }
        catch (Exception exception) { LOGGER.error("Coupon seckill pending recovery failed", exception); }
        finally { if (lock != null && lock.isHeldByCurrentThread()) try { lock.unlock(); } catch (Exception exception) { LOGGER.error("Coupon seckill recovery lock release failed", exception); } }
    }
}
