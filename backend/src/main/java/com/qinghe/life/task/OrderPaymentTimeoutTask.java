package com.qinghe.life.task;

import com.qinghe.life.service.OrderTimeoutCancelService;
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
@ConditionalOnProperty(prefix = "order.timeout", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OrderPaymentTimeoutTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderPaymentTimeoutTask.class);

    private final OrderTimeoutCancelService orderTimeoutCancelService;
    private final RedissonClient redissonClient;
    private final RedisBusinessMetrics metrics;

    @Value("${order.timeout.lock-wait-seconds:0}")
    private long lockWaitSeconds;

    public OrderPaymentTimeoutTask(OrderTimeoutCancelService orderTimeoutCancelService, RedissonClient redissonClient, RedisBusinessMetrics metrics) {
        this.orderTimeoutCancelService = orderTimeoutCancelService;
        this.redissonClient = redissonClient;
        this.metrics = metrics;
    }

    @Scheduled(cron = "${order.timeout.cron:0 * * * * ?}")
    public void cancelExpiredPendingOrders() {
        RLock lock = null;
        try {
            long started = System.nanoTime(); lock = redissonClient.getLock(RedisKeys.orderTimeoutLock());
            if (!lock.tryLock(Math.max(0, lockWaitSeconds), TimeUnit.SECONDS)) {
                metrics.count("redis_lock_acquire_failure_total", "order", "timeout-cancel", "failure", "LOCK_ACQUIRE_FAILED");
                return;
            }
            metrics.lockWait("order", "timeout-cancel", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
            metrics.count("redis_lock_acquire_total", "order", "timeout-cancel", "success", "none");
            orderTimeoutCancelService.cancelExpiredOrders();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.warn("订单超时取消任务获取锁时被中断", exception);
        } catch (Exception exception) {
            LOGGER.error("订单超时取消任务执行失败，本轮已结束", exception);
        } finally {
            if (lock != null) {
                try {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                } catch (Exception exception) {
                    LOGGER.error("订单超时取消任务释放锁失败", exception);
                }
            }
        }
    }
}
