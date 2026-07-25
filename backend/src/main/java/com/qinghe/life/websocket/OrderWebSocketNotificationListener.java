package com.qinghe.life.websocket;

import com.qinghe.life.entity.Order;
import com.qinghe.life.enums.OrderNotificationReason;
import com.qinghe.life.enums.OrderStatus;
import com.qinghe.life.event.OrderStatusChangedEvent;
import com.qinghe.life.vo.OrderWebSocketMessageVO;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Converts committed order events to safe user/admin WebSocket messages. */
@Component
public class OrderWebSocketNotificationListener {
    private final OrderWebSocketNotifier notifier;

    public OrderWebSocketNotificationListener(OrderWebSocketNotifier notifier) {
        this.notifier = notifier;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        Order order = event.getOrder();
        OrderNotificationReason reason = event.getReason();
        notifier.notifyUser(order.getUserId(), message(userType(reason), order, summary(reason)));
        notifier.notifyAdmins(message(adminType(reason), order, summary(reason)));
    }

    private String userType(OrderNotificationReason reason) {
        switch (reason) {
            case CREATED: return "ORDER_CREATED";
            case PAID: return "ORDER_PAID";
            case USER_CANCELLED: return "ORDER_CANCELLED";
            case TIMEOUT_CANCELLED: return "ORDER_TIMEOUT_CANCELLED";
            case ACCEPTED: return "ORDER_ACCEPTED";
            case DELIVERING: return "ORDER_DELIVERING";
            case COMPLETED: return "ORDER_COMPLETED";
            default: throw new IllegalArgumentException("Unsupported notification reason");
        }
    }

    private String adminType(OrderNotificationReason reason) {
        if (reason == OrderNotificationReason.CREATED) {
            return "ADMIN_NEW_ORDER";
        }
        if (reason == OrderNotificationReason.USER_CANCELLED || reason == OrderNotificationReason.TIMEOUT_CANCELLED) {
            return "ADMIN_ORDER_CANCELLED";
        }
        return "ADMIN_ORDER_STATUS_CHANGED";
    }

    private String summary(OrderNotificationReason reason) {
        switch (reason) {
            case CREATED: return "订单已创建";
            case PAID: return "订单已支付";
            case USER_CANCELLED: return "订单已取消";
            case TIMEOUT_CANCELLED: return "订单因支付超时已取消";
            case ACCEPTED: return "订单已接单";
            case DELIVERING: return "订单开始配送";
            case COMPLETED: return "订单已完成";
            default: return "订单状态已更新";
        }
    }

    private OrderWebSocketMessageVO message(String type, Order order, String summary) {
        OrderWebSocketMessageVO value = new OrderWebSocketMessageVO();
        value.setMessageType(type);
        value.setOrderId(order.getId());
        value.setOrderNo(order.getOrderNo());
        value.setOrderStatus(order.getStatus());
        value.setStatusText(OrderStatus.fromCode(order.getStatus()).getDisplayName());
        value.setOccurredAt(LocalDateTime.now());
        value.setSummary(summary);
        return value;
    }
}
