package com.qinghe.life.vo;

import com.qinghe.life.entity.Goods;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoodsVO {
    private Long id;
    private Long shopId;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private Integer salesCount;
    private String saleStatus;
    private String coverImage;

    public static GoodsVO fromGoods(Goods goods) {
        return fromGoods(goods, null);
    }

    public static GoodsVO fromGoods(Goods goods, String categoryName) {
        return new GoodsVO(goods.getId(), goods.getShopId(), goods.getCategoryId(), categoryName, goods.getName(),
                goods.getDescription(), goods.getPrice(), goods.getStock(), goods.getSalesCount(), goods.getSaleStatus(), goods.getCoverImage());
    }
}
