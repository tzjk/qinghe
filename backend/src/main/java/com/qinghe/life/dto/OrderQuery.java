package com.qinghe.life.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

@Data
public class OrderQuery {
    @Min(value = 1, message = "页码必须大于0")
    private Long page = 1L;

    @Min(value = 1, message = "每页数量必须大于0")
    @Max(value = 100, message = "每页数量不能超过100")
    private Long size = 10L;

    @Size(max = 20, message = "订单状态格式不正确")
    private String status;
}
