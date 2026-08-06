package com.qinghe.life;

import com.qinghe.life.entity.Order;
import com.qinghe.life.entity.User;
import com.qinghe.life.enums.OrderNotificationReason;
import com.qinghe.life.enums.OrderStatus;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.BuildingMapper;
import com.qinghe.life.mapper.CampusMapper;
import com.qinghe.life.mapper.CartMapper;
import com.qinghe.life.mapper.GoodsMapper;
import com.qinghe.life.mapper.OperateLogMapper;
import com.qinghe.life.mapper.OrderItemMapper;
import com.qinghe.life.mapper.OrderMapper;
import com.qinghe.life.mapper.ShopMapper;
import com.qinghe.life.mapper.UserAddressMapper;
import com.qinghe.life.mapper.UserMapper;
import com.qinghe.life.service.CouponService;
import com.qinghe.life.service.OrderNotificationPublisher;
import com.qinghe.life.service.OrderTimeoutCancelService;
import com.qinghe.life.service.impl.OrderCancellationService;
import com.qinghe.life.service.impl.OrderServiceImpl;
import com.qinghe.life.utils.UserContext;
import com.qinghe.life.vo.UserDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderReminderServiceTest {
    private final OrderMapper orderMapper = mock(OrderMapper.class);
    private final OrderNotificationPublisher publisher = mock(OrderNotificationPublisher.class);
    private final OrderServiceImpl service = new OrderServiceImpl(
            mock(CartMapper.class), mock(GoodsMapper.class), mock(ShopMapper.class), mock(UserAddressMapper.class),
            mock(CampusMapper.class), mock(BuildingMapper.class), orderMapper, mock(OrderItemMapper.class), mock(UserMapper.class),
            mock(OrderCancellationService.class), mock(OrderTimeoutCancelService.class), mock(CouponService.class),
            mock(OperateLogMapper.class), publisher);

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void ownerCanRemindAnActiveOrder() {
        UserContext.setUser(user(101L));
        Order order = order(OrderStatus.ACCEPTED);
        when(orderMapper.selectOne(any())).thenReturn(order);

        service.remind(order.getId());

        verify(publisher).publishAfterCommit(order, OrderNotificationReason.USER_REMINDER);
    }

    @Test
    void reminderRejectsNonActiveOrUnownedOrders() {
        UserContext.setUser(user(101L));
        Order pending = order(OrderStatus.PENDING_PAY);
        when(orderMapper.selectOne(any())).thenReturn(pending);

        assertThrows(BusinessException.class, () -> service.remind(pending.getId()));
        verify(publisher, never()).publishAfterCommit(any(), any());

        when(orderMapper.selectOne(any())).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.remind(999L));
    }

    private UserDTO user(Long id) {
        User user = new User();
        user.setId(id);
        user.setNickname("reminder-test");
        return UserDTO.fromUser(user);
    }

    private Order order(OrderStatus status) {
        Order order = new Order();
        order.setId(1001L);
        order.setUserId(101L);
        order.setOrderNo("QHREMINDER1001");
        order.setStatus(status.getCode());
        return order;
    }
}
