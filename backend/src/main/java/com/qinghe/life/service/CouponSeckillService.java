package com.qinghe.life.service;

import com.qinghe.life.vo.CouponClaimVO;

public interface CouponSeckillService {
    String SECKILL_COUPON_STATUS = "SECKILL";

    void preheat(Long couponId);
    CouponClaimVO claim(Long couponId, Long userId);
    void markDisabled(Long couponId);
    void consumeNewMessages();
    void recoverPendingMessages();
}
