package com.qinghe.life.enums;

/**
 * 订单通知的业务原因；它不参与订单状态机，也不持久化。
 */
public enum OrderNotificationReason {
    CREATED,
    PAID,
    USER_REMINDER,
    USER_CANCELLED,
    TIMEOUT_CANCELLED,
    ACCEPTED,
    DELIVERING,
    COMPLETED
}
