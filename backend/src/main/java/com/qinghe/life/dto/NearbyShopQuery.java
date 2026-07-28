package com.qinghe.life.dto;

import java.math.BigDecimal;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NearbyShopQuery {
    @NotNull @DecimalMin(value = "-180.000000", message = "经度不正确") @DecimalMax(value = "180.000000", message = "经度不正确") private BigDecimal longitude;
    @NotNull @DecimalMin(value = "-90.000000", message = "纬度不正确") @DecimalMax(value = "90.000000", message = "纬度不正确") private BigDecimal latitude;
    @NotNull @DecimalMin(value = "0.1", message = "搜索半径至少0.1公里") @DecimalMax(value = "20", message = "搜索半径不能超过20公里") private BigDecimal radius;
    @Min(value = 1, message = "页码必须大于0") private Long page = 1L;
    @Min(value = 1, message = "每页数量必须大于0") @Max(value = 50, message = "每页数量不能超过50") private Long size = 10L;
    private Long categoryId;
}
