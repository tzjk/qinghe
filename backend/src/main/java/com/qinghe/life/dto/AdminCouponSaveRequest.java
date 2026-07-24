package com.qinghe.life.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminCouponSaveRequest {
    @NotBlank
    @Size(max = 100)
    private String name;
    @NotBlank
    @Pattern(regexp = "CASH|DISCOUNT")
    private String couponType;
    @DecimalMin("0.01")
    private BigDecimal discountAmount;
    @DecimalMin("0.0001")
    private BigDecimal discountRate;
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal thresholdAmount;
    @NotNull
    @Min(1)
    private Integer totalStock;
    @NotNull
    private LocalDateTime receiveStartTime;
    @NotNull
    private LocalDateTime receiveEndTime;
    @NotNull
    private LocalDateTime useStartTime;
    @NotNull
    private LocalDateTime useEndTime;
    @NotNull
    private Long shopId;
    @NotNull
    @Min(1)
    @Max(1)
    private Integer perUserLimit;
    @NotBlank
    @Pattern(regexp = "ENABLED|DISABLED")
    private String status;
}
