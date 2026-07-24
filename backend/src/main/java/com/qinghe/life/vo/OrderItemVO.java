package com.qinghe.life.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemVO {
    private Long goodsId;
    private String goodsName;
    private String goodsImage;
    private BigDecimal goodsPrice;
    private Integer quantity;
    private BigDecimal subtotal;
}
