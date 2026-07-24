package com.qinghe.life.service;

import com.qinghe.life.common.PageResult;
import com.qinghe.life.dto.CouponPageQuery;
import com.qinghe.life.vo.CouponVO;
import com.qinghe.life.vo.CouponClaimVO;
import com.qinghe.life.vo.UserCouponVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface CouponService {
    PageResult<CouponVO> pageAvailable(CouponPageQuery query);
    PageResult<UserCouponVO> mine(CouponPageQuery query);
    CouponClaimVO claim(Long couponId);
    BigDecimal lockForOrder(Long userId, Long userCouponId, Long shopId, BigDecimal totalAmount, Long orderId, LocalDateTime now);
    void redeemForOrder(Long orderId, LocalDateTime now);
    void releaseForCancelledOrder(Long orderId, LocalDateTime now);
}
