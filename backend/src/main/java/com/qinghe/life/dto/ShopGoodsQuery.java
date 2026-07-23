package com.qinghe.life.dto;

import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class ShopGoodsQuery extends PageQuery {
    @Size(max = 100)
    private String keyword;
    private Long categoryId;
}
