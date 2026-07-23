package com.qinghe.life.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Data
public class PageQuery {
    @Min(1)
    private Long page = 1L;

    @Min(1)
    @Max(100)
    private Long size = 10L;
}
