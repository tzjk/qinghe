package com.qinghe.life.task;

import com.qinghe.life.service.OrderTimeoutCancelService;
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
    private static final String LOCK_NAME = "qh:lock:order:timeout-cancel";

    private final OrderTimeoutCancelService orderTimeoutCancelService;
    private final RedissonClient redissonClient;

    @Value("${order.timeout.lock-wait-seconds:0}")
    private long lockWaitSeconds;

    @Value("${order.timeout.lock-lease-seconds:55}")
    private long lockLeaseSeconds;

    public OrderPaymentTimeoutTask(OrderTimeoutCancelService orderTimeoutCancelService, RedissonClient redissonClient) {
        this.orderTimeoutCancelService = orderTimeoutCancelService;
        this.redissonClient = redissonClient;
    }

    @Scheduled(cron = "${order.timeout.cron:0 * * * * ?}")
    public void cancelExpiredPendingOrders() {
        RLock lock = null;
        try {
            lock = redissonClient.getLock(LOCK_NAME);
            if (!lock.tryLock(Math.max(0, lockWaitSeconds), Math.max(1, lockLeaseSeconds), TimeUnit.SECONDS)) {
                return;
            }
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
