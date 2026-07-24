package com.qinghe.life.service;

import com.qinghe.life.common.PageResult;
import com.qinghe.life.dto.AdminOrderQuery;
import com.qinghe.life.dto.OrderCreateDTO;
import com.qinghe.life.dto.OrderQuery;
import com.qinghe.life.vo.AdminOrderVO;
import com.qinghe.life.vo.OrderCreateVO;
import com.qinghe.life.vo.OrderVO;

public interface OrderService {
    OrderCreateVO create(OrderCreateDTO request);

    PageResult<OrderVO> page(OrderQuery query);

    OrderVO detail(Long orderId);

    OrderVO simulatePay(Long orderId);

    void cancel(Long orderId);

    PageResult<AdminOrderVO> adminPage(AdminOrderQuery query);

    AdminOrderVO adminDetail(Long orderId);

    AdminOrderVO accept(Long orderId);

    AdminOrderVO deliver(Long orderId);

    AdminOrderVO complete(Long orderId);

    int cancelExpiredOrders();
}
