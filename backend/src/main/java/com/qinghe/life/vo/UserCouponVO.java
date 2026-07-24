package com.qinghe.life.vo;

import com.qinghe.life.entity.Coupon;
import com.qinghe.life.entity.UserCoupon;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserCouponVO {
    private Long id;
    private String status;
    private LocalDateTime receiveTime;
    private LocalDateTime lockTime;
    private LocalDateTime useTime;
    private LocalDateTime expireTime;
    private Long orderId;
    private Long couponId;
    private String name;
    private String couponType;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private BigDecimal thresholdAmount;
    private Long shopId;
    private LocalDateTime useStartTime;

    public static UserCouponVO from(UserCoupon userCoupon, Coupon coupon) {
        UserCouponVO view = new UserCouponVO();
        view.setId(userCoupon.getId()); view.setStatus(userCoupon.getStatus()); view.setReceiveTime(userCoupon.getReceiveTime());
        view.setLockTime(userCoupon.getLockTime()); view.setUseTime(userCoupon.getUseTime()); view.setExpireTime(userCoupon.getExpireTime());
        view.setOrderId(userCoupon.getOrderId()); view.setCouponId(userCoupon.getCouponId());
        if (coupon != null) {
            view.setName(coupon.getName()); view.setCouponType(coupon.getCouponType()); view.setDiscountAmount(coupon.getDiscountAmount());
            view.setDiscountRate(coupon.getDiscountRate()); view.setThresholdAmount(coupon.getThresholdAmount());
            view.setShopId(coupon.getShopId()); view.setUseStartTime(coupon.getUseStartTime());
        }
        return view;
    }
}
