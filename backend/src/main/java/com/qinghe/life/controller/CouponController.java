package com.qinghe.life.controller;

import com.qinghe.life.annotation.OperateLog;
import com.qinghe.life.common.PageResult;
import com.qinghe.life.common.Result;
import com.qinghe.life.dto.CouponPageQuery;
import com.qinghe.life.service.CouponSeckillService;
import com.qinghe.life.service.CouponService;
import com.qinghe.life.vo.CouponClaimVO;
import com.qinghe.life.vo.CouponVO;
import com.qinghe.life.vo.SeckillOrderAcceptanceVO;
import com.qinghe.life.vo.SeckillOrderStatusVO;
import com.qinghe.life.vo.UserCouponVO;
import javax.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/coupons")
public class CouponController {
    private final CouponService couponService;
    private final CouponSeckillService seckillService;
    public CouponController(CouponService couponService, CouponSeckillService seckillService) { this.couponService = couponService; this.seckillService = seckillService; }
    @GetMapping public Result<PageResult<CouponVO>> page(@Valid CouponPageQuery query) { return Result.success(couponService.pageAvailable(query)); }
    @GetMapping("/mine") public Result<PageResult<UserCouponVO>> mine(@Valid CouponPageQuery query) { return Result.success(couponService.mine(query)); }
    @OperateLog(module = "优惠券", action = "领取普通优惠券")
    @PostMapping("/{couponId}/claim") public Result<CouponClaimVO> claim(@PathVariable Long couponId) { return Result.success(couponService.claim(couponId)); }
    @PostMapping("/{couponId}/seckill") public Result<SeckillOrderAcceptanceVO> seckill(@PathVariable Long couponId) { return Result.success(seckillService.seckill(couponId)); }
    @GetMapping("/seckill-orders/{orderId}/status") public Result<SeckillOrderStatusVO> seckillStatus(@PathVariable Long orderId) { return Result.success(seckillService.status(orderId)); }
    /** Kept temporarily so existing clients of the previous endpoint receive an order id. */
    @Deprecated @PostMapping("/{couponId}/seckill-claim") public Result<CouponClaimVO> legacySeckill(@PathVariable Long couponId) { return Result.success(couponService.claimSeckill(couponId)); }
}
