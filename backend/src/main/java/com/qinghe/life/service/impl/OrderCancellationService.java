package com.qinghe.life.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.entity.OperateLog;
import com.qinghe.life.entity.Order;
import com.qinghe.life.entity.OrderItem;
import com.qinghe.life.enums.OrderStatus;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.GoodsMapper;
import com.qinghe.life.mapper.OperateLogMapper;
import com.qinghe.life.mapper.OrderItemMapper;
import com.qinghe.life.mapper.OrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 订单取消的唯一资源释放内核。每个公开入口均由其他 Spring Bean 调用，以保证 REQUIRED 事务代理生效。
 */
@Service
public class OrderCancellationService {
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final GoodsMapper goodsMapper;
    private final OperateLogMapper operateLogMapper;

    public OrderCancellationService(OrderMapper orderMapper, OrderItemMapper orderItemMapper,
                                    GoodsMapper goodsMapper, OperateLogMapper operateLogMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.goodsMapper = goodsMapper;
        this.operateLogMapper = operateLogMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean cancelByUser(Long orderId, Long userId) {
        return cancelPendingOrder(orderId, userId, null, "USER_CANCEL", userId,
                "用户取消订单", "cancel");
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean cancelExpiredOrder(Long orderId, LocalDateTime now) {
        return cancelPendingOrder(orderId, null, now, "PAYMENT_TIMEOUT", null,
                "订单支付超时取消", "cancelExpiredOrder");
    }

    private boolean cancelPendingOrder(Long orderId, Long expectedUserId, LocalDateTime expiredAtOrBefore,
                                       String reason, Long logActorId, String action, String method) {
        LocalDateTime cancelTime = LocalDateTime.now();
        int updated = orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .eq(Order::getId, orderId)
                .eq(expectedUserId != null, Order::getUserId, expectedUserId)
                .eq(Order::getStatus, OrderStatus.PENDING_PAY.getCode())
                .le(expiredAtOrBefore != null, Order::getPayExpireTime, expiredAtOrBefore)
                .set(Order::getStatus, OrderStatus.CANCELLED.getCode())
                .set(Order::getCancelTime, cancelTime)
                .set(Order::getCancelReason, reason));
        if (updated != 1) {
            return false;
        }
        List<OrderItem> items = orderItemMapper.selectByOrderIds(Collections.singletonList(orderId));
        if (items.isEmpty()) {
            throw new BusinessException(409, "订单明细不存在，取消已回滚");
        }
        for (OrderItem item : items) {
            if (goodsMapper.restoreStockAfterOrderCancellation(item.getGoodsId(), item.getQuantity()) != 1) {
                throw new BusinessException(409, "商品库存恢复失败，取消已回滚");
            }
        }
        Long actualActorId = logActorId;
        if (actualActorId == null) {
            Order cancelled = orderMapper.selectById(orderId);
            actualActorId = cancelled == null ? null : cancelled.getUserId();
        }
        writeOperationLog(actualActorId, action, orderId, method);
        return true;
    }

    private void writeOperationLog(Long actorId, String action, Long orderId, String method) {
        OperateLog log = new OperateLog();
        log.setUserId(actorId);
        log.setModule("订单");
        log.setAction(action);
        log.setControllerClass("OrderTimeoutCancelService");
        log.setControllerMethod(method);
        log.setRequestPath("/internal/orders/" + orderId + "/cancel");
        log.setHttpMethod("POST");
        log.setRequestSummary("{\"orderId\":" + orderId + "}");
        log.setResponseSummary("{\"status\":\"success\"}");
        log.setSuccess(1);
        log.setDurationMs(0L);
        log.setOperateTime(LocalDateTime.now());
        if (operateLogMapper.insert(log) != 1) {
            throw new BusinessException(500, "订单操作日志写入失败，事务已回滚");
        }
    }
}
