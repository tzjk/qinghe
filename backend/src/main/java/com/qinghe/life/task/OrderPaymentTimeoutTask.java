package com.qinghe.life.task;

import com.qinghe.life.service.OrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderPaymentTimeoutTask {
    private final OrderService orderService;

    public OrderPaymentTimeoutTask(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(cron = "0 * * * * ?")
    public void cancelExpiredPendingOrders() {
        orderService.cancelExpiredOrders();
    }
}
