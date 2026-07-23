package com.qinghe.life.dto;

import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminGoodsCategoryQuery {
    @NotNull
    private Long shopId;
    private Integer status;
}
