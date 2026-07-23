package com.qinghe.life.controller;

import com.qinghe.life.common.Result;
import com.qinghe.life.annotation.OperateLog;
import com.qinghe.life.dto.CartAddDTO;
import com.qinghe.life.dto.CartSelectedDTO;
import com.qinghe.life.dto.CartUpdateDTO;
import com.qinghe.life.service.CartService;
import com.qinghe.life.vo.CartItemVO;
import com.qinghe.life.vo.CartSummaryVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public Result<CartSummaryVO> summary() {
        return Result.success(cartService.summary());
    }

    @OperateLog(module = "购物车", action = "新增购物车项")
    @PostMapping
    public Result<CartItemVO> add(@Valid @RequestBody CartAddDTO request) {
        return Result.success(cartService.add(request));
    }

    @OperateLog(module = "购物车", action = "修改购买数量")
    @PutMapping("/{id}")
    public Result<CartItemVO> updateQuantity(@PathVariable Long id, @Valid @RequestBody CartUpdateDTO request) {
        return Result.success(cartService.updateQuantity(id, request));
    }

    @PutMapping("/{id}/selected")
    public Result<CartItemVO> updateSelected(@PathVariable Long id, @Valid @RequestBody CartSelectedDTO request) {
        return Result.success(cartService.updateSelected(id, request));
    }

    @OperateLog(module = "购物车", action = "删除购物车项")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        cartService.delete(id);
        return Result.success();
    }

    @OperateLog(module = "购物车", action = "清空购物车")
    @DeleteMapping
    public Result<Void> clear() {
        cartService.clear();
        return Result.success();
    }
}
