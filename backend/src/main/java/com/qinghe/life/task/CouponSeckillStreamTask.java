package com.qinghe.life.task;

import com.qinghe.life.service.CouponSeckillService;
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
    @Value("${coupon.seckill.pending-lock-wait-seconds:0}") private long lockWaitSeconds;
    @Value("${coupon.seckill.pending-lock-lease-seconds:10}") private long lockLeaseSeconds;
    public CouponSeckillStreamTask(CouponSeckillService couponSeckillService, RedissonClient redissonClient) { this.couponSeckillService = couponSeckillService; this.redissonClient = redissonClient; }
    @Scheduled(fixedDelayString = "${coupon.seckill.consume-delay-millis:500}")
    public void consumeNewMessages() { try { couponSeckillService.consumeNewMessages(); } catch (Exception exception) { LOGGER.error("Coupon seckill stream consumption failed", exception); } }
    @Scheduled(cron = "${coupon.seckill.pending-recovery-cron:*/15 * * * * ?}")
    public void recoverPendingMessages() {
        RLock lock = null;
        try { lock = redissonClient.getLock(RedisKeys.couponSeckillRecoveryLock()); if (!lock.tryLock(Math.max(0, lockWaitSeconds), Math.max(1, lockLeaseSeconds), TimeUnit.SECONDS)) return; couponSeckillService.recoverPendingMessages(); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); LOGGER.warn("Coupon seckill pending recovery interrupted", exception); }
        catch (Exception exception) { LOGGER.error("Coupon seckill pending recovery failed", exception); }
        finally { if (lock != null && lock.isHeldByCurrentThread()) try { lock.unlock(); } catch (Exception exception) { LOGGER.error("Coupon seckill recovery lock release failed", exception); } }
    }
}
