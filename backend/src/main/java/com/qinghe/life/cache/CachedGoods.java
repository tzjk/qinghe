package com.qinghe.life.cache;

import com.qinghe.life.entity.Goods;
import com.qinghe.life.vo.GoodsVO;
import java.math.BigDecimal;

/** Immutable catalog fields. Stock and sales are always refreshed from MySQL. */
public class CachedGoods {
    private Long id;
    private Long shopId;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String description;
    private BigDecimal price;
    private String coverImage;

    public CachedGoods() { }

    public static CachedGoods from(Goods goods, String categoryName) {
        CachedGoods value = new CachedGoods();
        value.id = goods.getId();
        value.shopId = goods.getShopId();
        value.categoryId = goods.getCategoryId();
        value.categoryName = categoryName;
        value.name = goods.getName();
        value.description = goods.getDescription();
        value.price = goods.getPrice();
        value.coverImage = goods.getCoverImage();
        return value;
    }

    public GoodsVO toGoodsVO(Goods current) {
        return new GoodsVO(id, shopId, categoryId, categoryName, name,
                description == null ? current.getDescription() : description,
                price == null ? current.getPrice() : price,
                current.getStock(), current.getSalesCount(), current.getSaleStatus(),
                coverImage == null ? current.getCoverImage() : coverImage);
    }

    public Long getId() { return id; }
    public Long getShopId() { return shopId; }
    public Long getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public String getCoverImage() { return coverImage; }
}
