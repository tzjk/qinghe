package com.qinghe.life.task;

import com.qinghe.life.service.CouponSeckillService;
import com.qinghe.life.utils.RedisKeys;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Owns one blocking Redis Stream consumer and releases it when Spring shuts down. */
@Component
@ConditionalOnProperty(prefix = "coupon.seckill", name = "consume-enabled", havingValue = "true", matchIfMissing = true)
public class CouponSeckillStreamTask implements SmartLifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(CouponSeckillStreamTask.class);
    private final CouponSeckillService seckillService;
    private final RedissonClient redisson;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile ExecutorService executor;
    @Value("${coupon.seckill.pending-lock-wait-seconds:0}") private long lockWaitSeconds;

    public CouponSeckillStreamTask(CouponSeckillService seckillService, RedissonClient redisson) { this.seckillService = seckillService; this.redisson = redisson; }

    @Override public void start() {
        if (!running.compareAndSet(false, true)) return;
        seckillService.initializeConsumerGroup();
        executor = Executors.newSingleThreadExecutor(r -> { Thread thread = new Thread(r, "coupon-seckill-stream"); thread.setDaemon(true); return thread; });
        executor.submit(() -> { while (running.get() && !Thread.currentThread().isInterrupted()) { try { seckillService.consumeNewMessages(); } catch (Exception ex) { if (running.get()) LOG.error("秒杀 Stream 消费循环异常", ex); } } });
    }
    @Override public void stop() { running.set(false); ExecutorService value = executor; if (value != null) value.shutdownNow(); }
    @Override public boolean isRunning() { return running.get(); }
    @Override public boolean isAutoStartup() { return true; }
    @Override public int getPhase() { return Integer.MAX_VALUE - 100; }
    @Override public void stop(Runnable callback) { stop(); callback.run(); }

    @Scheduled(cron = "${coupon.seckill.pending-recovery-cron:*/15 * * * * ?}")
    public void recoverPendingMessages() {
        if (!running.get()) return;
        RLock lock = redisson.getLock(RedisKeys.couponSeckillRecoveryLock()); boolean acquired = false;
        try { acquired = lock.tryLock(Math.max(0L, lockWaitSeconds), TimeUnit.SECONDS); if (acquired) seckillService.recoverPendingMessages(); }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
        catch (Exception ex) { LOG.error("秒杀 Stream Pending 恢复失败", ex); }
        finally { if (acquired && lock.isHeldByCurrentThread()) lock.unlock(); }
    }
}
