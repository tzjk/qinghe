package com.qinghe.life.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qh_category")
public class Category extends BaseEntity {
    private String name;
    private String iconUrl;
    private Integer sortOrder;
    private Integer status;
}
