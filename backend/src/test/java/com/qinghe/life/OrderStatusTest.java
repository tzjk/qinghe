package com.qinghe.life;

import com.qinghe.life.enums.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStatusTest {
    @Test
    void exposesStableCodesAndChineseNames() {
        assertEquals("PENDING_PAY", OrderStatus.PENDING_PAY.getCode());
        assertEquals("待支付", OrderStatus.PENDING_PAY.getDisplayName());
        assertEquals(OrderStatus.ACCEPTED, OrderStatus.fromCode("ACCEPTED"));
    }

    @Test
    void permitsOnlyDefinedTransitions() {
        assertTrue(OrderStatus.PENDING_PAY.canTransitionTo(OrderStatus.PAID));
        assertTrue(OrderStatus.PENDING_PAY.canTransitionTo(OrderStatus.CANCELLED));
        assertTrue(OrderStatus.PAID.canTransitionTo(OrderStatus.ACCEPTED));
        assertTrue(OrderStatus.ACCEPTED.canTransitionTo(OrderStatus.DELIVERING));
        assertTrue(OrderStatus.DELIVERING.canTransitionTo(OrderStatus.COMPLETED));
        assertFalse(OrderStatus.PENDING_PAY.canTransitionTo(OrderStatus.ACCEPTED));
        assertFalse(OrderStatus.PAID.canTransitionTo(OrderStatus.CANCELLED));
        assertFalse(OrderStatus.COMPLETED.canTransitionTo(OrderStatus.CANCELLED));
    }
}
