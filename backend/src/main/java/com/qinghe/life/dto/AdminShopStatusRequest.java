package com.qinghe.life.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminShopStatusRequest {
    @NotNull(message = "请选择店铺状态")
    @Min(value = 0, message = "店铺状态不正确")
    @Max(value = 1, message = "店铺状态不正确")
    private Integer status;
}
