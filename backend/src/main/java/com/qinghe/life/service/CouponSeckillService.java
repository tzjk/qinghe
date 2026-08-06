package com.qinghe.life.service;

import com.qinghe.life.vo.SeckillOrderAcceptanceVO;
import com.qinghe.life.vo.SeckillOrderStatusVO;

public interface CouponSeckillService {
    String SECKILL_COUPON_STATUS = "SECKILL";

    void preheat(Long couponId);
    void markDisabled(Long couponId);
    SeckillOrderAcceptanceVO seckill(Long couponId);
    SeckillOrderStatusVO status(Long orderId);
    void initializeConsumerGroup();
    void consumeNewMessages();
    void recoverPendingMessages();
}
