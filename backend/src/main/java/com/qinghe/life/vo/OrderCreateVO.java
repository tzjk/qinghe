package com.qinghe.life.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderCreateVO {
    private Long orderId;
    private String orderNo;
    private Long shopId;
    private String shopName;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal deliveryFee;
    private BigDecimal payAmount;
    private String addressSummary;
}
