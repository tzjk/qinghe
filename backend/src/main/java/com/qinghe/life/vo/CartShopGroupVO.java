package com.qinghe.life.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CartShopGroupVO {
    private Long shopId;
    private String shopName;
    private List<CartItemVO> items = new ArrayList<CartItemVO>();

    public CartShopGroupVO(Long shopId, String shopName) {
        this.shopId = shopId;
        this.shopName = shopName;
    }
}
