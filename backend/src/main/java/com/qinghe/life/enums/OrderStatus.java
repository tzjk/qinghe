package com.qinghe.life.enums;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * 订单生命周期的唯一状态定义。
 *
 * <p>数据库仍以 VARCHAR(20) 存储 {@link #getCode()}；本枚举不改变既有数据值。</p>
 */
public enum OrderStatus {
    PENDING_PAY("PENDING_PAY", "待支付"),
    PAID("PAID", "已支付"),
    ACCEPTED("ACCEPTED", "已接单"),
    DELIVERING("DELIVERING", "配送中"),
    COMPLETED("COMPLETED", "已完成"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String displayName;

    OrderStatus(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean canTransitionTo(OrderStatus target) {
        return target != null && allowedTargets().contains(target);
    }

    public static OrderStatus fromCode(String code) {
        for (OrderStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知订单状态编码：" + code);
    }

    private Set<OrderStatus> allowedTargets() {
        switch (this) {
            case PENDING_PAY:
                return EnumSet.of(PAID, CANCELLED);
            case PAID:
                return EnumSet.of(ACCEPTED);
            case ACCEPTED:
                return EnumSet.of(DELIVERING);
            case DELIVERING:
                return EnumSet.of(COMPLETED);
            default:
                return Collections.emptySet();
        }
    }
}
