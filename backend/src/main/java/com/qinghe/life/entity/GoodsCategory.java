package com.qinghe.life.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qh_goods_category")
public class GoodsCategory extends BaseEntity {
    private Long shopId;
    private String name;
    private Integer sortOrder;
    private Integer status;
}
