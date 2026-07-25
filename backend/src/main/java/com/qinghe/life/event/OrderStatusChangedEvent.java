package com.qinghe.life.event;

import com.qinghe.life.entity.Order;
import com.qinghe.life.enums.OrderNotificationReason;

/** Immutable snapshot published inside the successful order transaction. */
public final class OrderStatusChangedEvent {
    private final Order order;
    private final OrderNotificationReason reason;

    public OrderStatusChangedEvent(Order order, OrderNotificationReason reason) {
        this.order = order;
        this.reason = reason;
    }

    public Order getOrder() {
        return order;
    }

    public OrderNotificationReason getReason() {
        return reason;
    }
}
