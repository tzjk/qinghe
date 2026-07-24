package com.qinghe.life.vo;

import com.qinghe.life.entity.Coupon;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponVO {
    private Long id;
    private String name;
    private String couponType;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private BigDecimal thresholdAmount;
    private Integer totalStock;
    private Integer availableStock;
    private LocalDateTime receiveStartTime;
    private LocalDateTime receiveEndTime;
    private LocalDateTime useStartTime;
    private LocalDateTime useEndTime;
    private Long shopId;
    private Integer perUserLimit;
    private String status;
    private boolean claimed;
    private Long userCouponId;
    private String userCouponStatus;

    public static CouponVO from(Coupon coupon) {
        CouponVO view = new CouponVO();
        view.setId(coupon.getId()); view.setName(coupon.getName()); view.setCouponType(coupon.getCouponType());
        view.setDiscountAmount(coupon.getDiscountAmount()); view.setDiscountRate(coupon.getDiscountRate());
        view.setThresholdAmount(coupon.getThresholdAmount()); view.setTotalStock(coupon.getTotalStock());
        view.setAvailableStock(coupon.getAvailableStock()); view.setReceiveStartTime(coupon.getReceiveStartTime());
        view.setReceiveEndTime(coupon.getReceiveEndTime()); view.setUseStartTime(coupon.getUseStartTime());
        view.setUseEndTime(coupon.getUseEndTime()); view.setShopId(coupon.getShopId());
        view.setPerUserLimit(coupon.getPerUserLimit()); view.setStatus(coupon.getStatus());
        return view;
    }

    public void markClaimed(Long userCouponId, String userCouponStatus) {
        this.claimed = userCouponId != null;
        this.userCouponId = userCouponId;
        this.userCouponStatus = userCouponStatus;
    }
}
