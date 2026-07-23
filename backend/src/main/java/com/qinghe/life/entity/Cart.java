package com.qinghe.life.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qh_cart")
public class Cart extends BaseEntity {
    private Long userId;
    private Long shopId;
    private Long goodsId;
    private Integer quantity;
    private Integer selected;
}
