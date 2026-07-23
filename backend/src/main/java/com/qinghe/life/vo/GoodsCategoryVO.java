package com.qinghe.life.vo;

import com.qinghe.life.entity.GoodsCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoodsCategoryVO {
    private Long id;
    private Long shopId;
    private String name;
    private Integer sortOrder;
    private Integer status;
    private Long goodsCount;

    public static GoodsCategoryVO from(GoodsCategory category, Long goodsCount) {
        return new GoodsCategoryVO(category.getId(), category.getShopId(), category.getName(),
                category.getSortOrder(), category.getStatus(), goodsCount);
    }
}
