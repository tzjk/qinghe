package com.qinghe.life.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class CartSummaryVO {
    private List<CartShopGroupVO> shopGroups = new ArrayList<CartShopGroupVO>();
    private Integer selectedCount = 0;
    private BigDecimal selectedAmount = BigDecimal.ZERO;
    private Integer totalCount = 0;
}
