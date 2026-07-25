package com.qinghe.life.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class GoodsSalesRankingVO {
    private Long goodsId;
    private String goodsName;
    private Long salesQuantity;
    private BigDecimal salesAmount;
}
