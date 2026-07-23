package com.qinghe.life.vo;

import com.qinghe.life.entity.Coupon;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponVO {
    private Long id;
    private String name;
    private String couponType;
    private BigDecimal discountAmount;
    private BigDecimal thresholdAmount;
    private Integer totalStock;
    private Integer claimedCount;
    private LocalDateTime endTime;

    public static CouponVO fromCoupon(Coupon coupon) {
        return new CouponVO(coupon.getId(), coupon.getName(), coupon.getCouponType(), coupon.getDiscountAmount(),
                coupon.getThresholdAmount(), coupon.getTotalStock(), coupon.getClaimedCount(), coupon.getEndTime());
    }
}
