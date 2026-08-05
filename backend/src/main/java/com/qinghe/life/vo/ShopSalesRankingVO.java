package com.qinghe.life.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ShopSalesRankingVO {
    private Long shopId;
    private String shopName;
    private Long paidOrderCount;
    private BigDecimal salesAmount;
}
