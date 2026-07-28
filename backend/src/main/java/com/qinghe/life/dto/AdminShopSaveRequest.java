package com.qinghe.life.dto;

import java.math.BigDecimal;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminShopSaveRequest {
    @NotBlank(message = "店铺名称不能为空")
    @Size(max = 100, message = "店铺名称不能超过100个字符")
    private String name;

    @NotNull(message = "请选择店铺分类")
    private Long categoryId;

    @NotBlank(message = "店铺地址不能为空")
    @Size(max = 255, message = "店铺地址不能超过255个字符")
    private String address;

    @Size(max = 20, message = "联系电话不能超过20个字符")
    @Pattern(regexp = "^$|^[0-9+() -]{6,20}$", message = "联系电话格式不正确")
    private String phone;

    @NotNull(message = "请填写店铺评分")
    @DecimalMin(value = "0.00", message = "店铺评分不能小于0")
    @DecimalMax(value = "5.00", message = "店铺评分不能大于5")
    private BigDecimal score;

    @NotNull(message = "请选择店铺状态")
    @Min(value = 0, message = "店铺状态不正确")
    @Max(value = 1, message = "店铺状态不正确")
    private Integer status;

    @NotNull(message = "请选择推荐状态")
    @Min(value = 0, message = "推荐状态不正确")
    @Max(value = 1, message = "推荐状态不正确")
    private Integer isFeatured;

    @NotNull(message = "请填写排序值")
    @Min(value = 0, message = "排序值不能小于0")
    @Max(value = 999999, message = "排序值不能超过999999")
    private Integer sortOrder;

    @DecimalMin(value = "-180.000000", message = "经度不正确")
    @DecimalMax(value = "180.000000", message = "经度不正确")
    private BigDecimal longitude;

    @DecimalMin(value = "-90.000000", message = "纬度不正确")
    @DecimalMax(value = "90.000000", message = "纬度不正确")
    private BigDecimal latitude;
}
