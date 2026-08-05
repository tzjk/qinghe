package com.qinghe.life.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CouponUsageSummaryVO {
    private Long couponId;
    private String couponName;
    private Long usedCount;
    private BigDecimal discountAmount;
}
