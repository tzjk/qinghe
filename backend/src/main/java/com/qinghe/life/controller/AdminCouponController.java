package com.qinghe.life.controller;

import com.qinghe.life.annotation.OperateLog;
import com.qinghe.life.common.PageResult;
import com.qinghe.life.common.Result;
import com.qinghe.life.dto.AdminCouponSaveRequest;
import com.qinghe.life.dto.CouponPageQuery;
import com.qinghe.life.dto.CouponStatusRequest;
import com.qinghe.life.service.AdminCouponService;
import com.qinghe.life.vo.CouponStatsVO;
import com.qinghe.life.vo.CouponVO;
import org.springframework.validation.annotation.Validated;
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
@RequestMapping("/api/admin/coupons")
public class AdminCouponController {
    private final AdminCouponService couponService;
    public AdminCouponController(AdminCouponService couponService) { this.couponService = couponService; }
    @GetMapping public Result<PageResult<CouponVO>> page(@Valid CouponPageQuery query) { return Result.success(couponService.page(query)); }
    @OperateLog(module = "优惠券管理", action = "新增普通优惠券")
    @PostMapping public Result<CouponVO> create(@Valid @RequestBody AdminCouponSaveRequest request) { return Result.success(couponService.create(request)); }
    @OperateLog(module = "优惠券管理", action = "编辑未开始优惠券")
    @PutMapping("/{couponId}") public Result<CouponVO> update(@PathVariable Long couponId, @Valid @RequestBody AdminCouponSaveRequest request) { return Result.success(couponService.update(couponId, request)); }
    @OperateLog(module = "优惠券管理", action = "调整优惠券状态")
    @PutMapping("/{couponId}/status") public Result<Void> status(@PathVariable Long couponId, @Valid @RequestBody CouponStatusRequest request) { couponService.changeStatus(couponId, request.getStatus()); return Result.success(); }
    @GetMapping("/{couponId}/stats") public Result<CouponStatsVO> stats(@PathVariable Long couponId) { return Result.success(couponService.stats(couponId)); }
}
