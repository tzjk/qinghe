package com.qinghe.life.controller;

import com.qinghe.life.common.PageResult;
import com.qinghe.life.common.Result;
import com.qinghe.life.dto.AdminOrderQuery;
import com.qinghe.life.service.OrderService;
import com.qinghe.life.vo.AdminOrderVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {
    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public Result<PageResult<AdminOrderVO>> page(@Valid AdminOrderQuery query) {
        return Result.success(orderService.adminPage(query));
    }

    @GetMapping("/{orderId}")
    public Result<AdminOrderVO> detail(@PathVariable Long orderId) {
        return Result.success(orderService.adminDetail(orderId));
    }

    @PostMapping("/{orderId}/accept")
    public Result<AdminOrderVO> accept(@PathVariable Long orderId) {
        return Result.success(orderService.accept(orderId));
    }

    @PostMapping("/{orderId}/deliver")
    public Result<AdminOrderVO> deliver(@PathVariable Long orderId) {
        return Result.success(orderService.deliver(orderId));
    }

    @PostMapping("/{orderId}/complete")
    public Result<AdminOrderVO> complete(@PathVariable Long orderId) {
        return Result.success(orderService.complete(orderId));
    }
}
