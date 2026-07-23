package com.qinghe.life.dto;

import lombok.Data;

@Data
public class ShopQuery extends PageQuery {
    private Long categoryId;
    private String keyword;
    private String sort;
}
