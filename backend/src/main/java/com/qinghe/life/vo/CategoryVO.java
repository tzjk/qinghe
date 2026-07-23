package com.qinghe.life.vo;

import com.qinghe.life.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryVO {
    private Long id;
    private String name;
    private String iconUrl;
    private Integer sortOrder;

    public static CategoryVO fromCategory(Category category) {
        return new CategoryVO(category.getId(), category.getName(), category.getIconUrl(), category.getSortOrder());
    }
}
