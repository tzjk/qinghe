package com.qinghe.life.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class CartUpdateDTO {
    @NotNull
    @Min(1)
    @Max(99)
    private Integer quantity;
}
