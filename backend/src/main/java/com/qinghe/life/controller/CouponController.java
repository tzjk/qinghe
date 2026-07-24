package com.qinghe.life.controller;

import com.qinghe.life.annotation.OperateLog;
import com.qinghe.life.common.PageResult;
import com.qinghe.life.common.Result;
import com.qinghe.life.dto.CouponPageQuery;
import com.qinghe.life.service.CouponService;
import com.qinghe.life.vo.CouponVO;
import com.qinghe.life.vo.CouponClaimVO;
import com.qinghe.life.vo.UserCouponVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/coupons")
public class CouponController {
    private final CouponService couponService;
    public CouponController(CouponService couponService) { this.couponService = couponService; }
    @GetMapping public Result<PageResult<CouponVO>> page(@Valid CouponPageQuery query) { return Result.success(couponService.pageAvailable(query)); }
    @GetMapping("/mine") public Result<PageResult<UserCouponVO>> mine(@Valid CouponPageQuery query) { return Result.success(couponService.mine(query)); }
    @OperateLog(module = "优惠券", action = "领取普通优惠券")
    @PostMapping("/{couponId}/claim") public Result<CouponClaimVO> claim(@PathVariable Long couponId) { return Result.success(couponService.claim(couponId)); }
    @PostMapping("/{couponId}/seckill-claim") public Result<CouponClaimVO> claimSeckill(@PathVariable Long couponId) { return Result.success(couponService.claimSeckill(couponId)); }
}
