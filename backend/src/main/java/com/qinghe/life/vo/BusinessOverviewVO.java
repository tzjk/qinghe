package com.qinghe.life.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BusinessOverviewVO {
    private Long orderCount;
    private Long paidOrderCount;
    private Long completedOrderCount;
    private Long cancelledOrderCount;
    private BigDecimal salesAmount;
    private BigDecimal discountAmount;
    private Long newUserCount;
    private Long activeShopCount;
    private Long pendingPayOrderCount;
    private Long paidPendingOrderCount;
    private Long acceptedOrderCount;
    private Long deliveringOrderCount;
}
