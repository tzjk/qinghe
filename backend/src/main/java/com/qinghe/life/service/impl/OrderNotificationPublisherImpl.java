package com.qinghe.life.service.impl;

import com.qinghe.life.entity.Order;
import com.qinghe.life.enums.OrderNotificationReason;
import com.qinghe.life.event.OrderStatusChangedEvent;
import com.qinghe.life.service.OrderNotificationPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class OrderNotificationPublisherImpl implements OrderNotificationPublisher {
    private final ApplicationEventPublisher eventPublisher;

    public OrderNotificationPublisherImpl(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void publishAfterCommit(Order order, OrderNotificationReason reason) {
        eventPublisher.publishEvent(new OrderStatusChangedEvent(order, reason));
    }
}
