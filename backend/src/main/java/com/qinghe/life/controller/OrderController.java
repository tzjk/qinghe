package com.qinghe.life.controller;

import com.qinghe.life.annotation.OperateLog;
import com.qinghe.life.common.Result;
import com.qinghe.life.dto.OrderCreateDTO;
import com.qinghe.life.service.OrderService;
import com.qinghe.life.vo.OrderCreateVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @OperateLog(module = "订单", action = "创建普通订单")
    @PostMapping
    public Result<OrderCreateVO> create(@Valid @RequestBody OrderCreateDTO request) {
        return Result.success(orderService.create(request));
    }
}
