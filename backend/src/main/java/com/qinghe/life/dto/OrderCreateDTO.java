package com.qinghe.life.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderCreateDTO {
    @NotEmpty(message = "请选择至少一项购物车商品")
    @Size(max = 100, message = "单次下单商品数量不能超过100项")
    private List<@NotNull(message = "购物车项不能为空") Long> cartItemIds;

    @NotNull(message = "请选择收货地址")
    private Long addressId;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;
}
