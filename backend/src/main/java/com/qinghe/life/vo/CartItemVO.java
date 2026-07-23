package com.qinghe.life.vo;

import com.qinghe.life.entity.Cart;
import com.qinghe.life.entity.Goods;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemVO {
    private Long cartId;
    private Long shopId;
    private Long goodsId;
    private String goodsName;
    private String goodsImage;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;
    private Integer stock;
    private Boolean selected;
    private String saleStatus;

    public static CartItemVO from(Cart cart, Goods goods) {
        CartItemVO item = new CartItemVO();
        item.setCartId(cart.getId());
        item.setShopId(cart.getShopId());
        item.setGoodsId(cart.getGoodsId());
        item.setGoodsName(goods.getName());
        item.setGoodsImage(goods.getCoverImage());
        item.setPrice(goods.getPrice());
        item.setQuantity(cart.getQuantity());
        item.setSubtotal(goods.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
        item.setStock(goods.getStock());
        item.setSelected(Integer.valueOf(1).equals(cart.getSelected()));
        item.setSaleStatus(goods.getSaleStatus());
        return item;
    }
}
