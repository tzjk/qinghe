package com.qinghe.life.controller;

import com.qinghe.life.common.Result;
import com.qinghe.life.common.PageResult;
import com.qinghe.life.dto.OrderCreateDTO;
import com.qinghe.life.dto.OrderQuery;
import com.qinghe.life.service.OrderService;
import com.qinghe.life.vo.OrderCreateVO;
import com.qinghe.life.vo.OrderVO;
import com.qinghe.life.annotation.OperateLog;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping
    public Result<PageResult<OrderVO>> page(@Valid OrderQuery query) {
        return Result.success(orderService.page(query));
    }

    @GetMapping("/{orderId}")
    public Result<OrderVO> detail(@PathVariable Long orderId) {
        return Result.success(orderService.detail(orderId));
    }

    @PostMapping("/{orderId}/simulate-pay")
    public Result<OrderVO> simulatePay(@PathVariable Long orderId) {
        return Result.success(orderService.simulatePay(orderId));
    }

    @DeleteMapping("/{orderId}")
    public Result<Void> cancel(@PathVariable Long orderId) {
        orderService.cancel(orderId);
        return Result.success();
    }
}
