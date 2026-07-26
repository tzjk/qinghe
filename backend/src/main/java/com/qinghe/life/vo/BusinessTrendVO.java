package com.qinghe.life.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BusinessTrendVO {
    private String reportDate;
    private Long orderCount;
    private Long paidOrderCount;
    private Long completedOrderCount;
    private Long cancelledOrderCount;
    private BigDecimal salesAmount;
    private BigDecimal discountAmount;
}
