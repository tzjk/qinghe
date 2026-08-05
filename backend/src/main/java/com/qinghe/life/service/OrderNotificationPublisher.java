package com.qinghe.life.service;

import com.qinghe.life.entity.Order;
import com.qinghe.life.enums.OrderNotificationReason;

/** Registers an order notification for delivery only after transaction commit. */
public interface OrderNotificationPublisher {
    void publishAfterCommit(Order order, OrderNotificationReason reason);
}
