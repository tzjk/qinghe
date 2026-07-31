package com.qinghe.life.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.entity.Order;
import com.qinghe.life.enums.OrderStatus;
import com.qinghe.life.mapper.OrderMapper;
import com.qinghe.life.service.OrderTimeoutCancelService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderTimeoutCancelServiceImpl implements OrderTimeoutCancelService {
    private final OrderMapper orderMapper;
    private final OrderCancellationService orderCancellationService;

    @Value("${order.timeout.batch-size:100}")
    private int batchSize;
    @Value("${order.timeout.max-batches-per-run:10}")
    private int maxBatchesPerRun;

    public OrderTimeoutCancelServiceImpl(OrderMapper orderMapper, OrderCancellationService orderCancellationService) {
        this.orderMapper = orderMapper;
        this.orderCancellationService = orderCancellationService;
    }

    @Override
    public int cancelExpiredOrders() {
        int cancelled = 0;
        for (int batch = 0; batch < Math.max(1, maxBatchesPerRun); batch++) {
            LocalDateTime now = LocalDateTime.now();
            List<Order> expiredOrders = orderMapper.selectList(Wrappers.<Order>lambdaQuery()
                    .eq(Order::getStatus, OrderStatus.PENDING_PAY.getCode())
                    .le(Order::getPayExpireTime, now)
                    .orderByAsc(Order::getPayExpireTime).orderByAsc(Order::getId)
                    .last("LIMIT " + Math.max(1, batchSize)));
            if (expiredOrders.isEmpty()) {
                return cancelled;
            }
            for (Order order : expiredOrders) {
                if (orderCancellationService.cancelExpiredOrder(order.getId(), now)) {
                    cancelled++;
                }
            }
        }
        return cancelled;
    }
}
