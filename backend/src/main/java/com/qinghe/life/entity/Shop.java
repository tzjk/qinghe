package com.qinghe.life.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qh_shop")
public class Shop extends BaseEntity {
    private Long categoryId;
    private String name;
    private String address;
    private String phone;
    private BigDecimal score;
    private Integer status;
    private Integer isFeatured;
    private String coverImage;
    private Integer sortOrder;
    private BigDecimal longitude;
    private BigDecimal latitude;
}
