package com.qinghe.life.vo;

import com.qinghe.life.entity.Shop;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopVO {
    private Long id;
    private Long categoryId;
    private String name;
    private String address;
    private String phone;
    private BigDecimal score;
    private String coverImage;
    private Integer sortOrder;

    public static ShopVO fromShop(Shop shop) {
        return new ShopVO(shop.getId(), shop.getCategoryId(), shop.getName(), shop.getAddress(), shop.getPhone(),
                shop.getScore(), shop.getCoverImage(), shop.getSortOrder());
    }
}
