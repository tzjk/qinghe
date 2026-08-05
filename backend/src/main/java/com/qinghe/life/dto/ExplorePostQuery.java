package com.qinghe.life.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import lombok.Data;

@Data
public class ExplorePostQuery {
    @Min(value = 1, message = "页码必须大于0") private Long page = 1L;
    @Min(value = 1, message = "每页数量必须大于0") @Max(value = 50, message = "每页数量不能超过50") private Long size = 10L;
    private String sort = "latest";
    private Long shopId;
}
