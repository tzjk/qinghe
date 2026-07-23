package com.qinghe.life.dto;

import lombok.Data;

@Data
public class AdminShopQuery extends PageQuery {
    private Long categoryId;
    private Integer status;
    private String keyword;
}
