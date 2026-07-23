package com.qinghe.life.vo;

import com.qinghe.life.entity.Shop;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminShopVO {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String address;
    private String phone;
    private BigDecimal score;
    private Integer status;
    private Integer isFeatured;
    private String coverImage;
    private Integer sortOrder;

    public static AdminShopVO fromShop(Shop shop, String categoryName) {
        return new AdminShopVO(shop.getId(), shop.getCategoryId(), categoryName, shop.getName(), shop.getAddress(),
                shop.getPhone(), shop.getScore(), shop.getStatus(), shop.getIsFeatured(), shop.getCoverImage(), shop.getSortOrder());
    }
}
