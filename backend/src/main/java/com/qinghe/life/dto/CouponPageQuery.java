package com.qinghe.life.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Data
public class CouponPageQuery {
    @Min(1)
    private long page = 1;
    @Min(1)
    @Max(100)
    private long size = 10;
    private String status;
}
