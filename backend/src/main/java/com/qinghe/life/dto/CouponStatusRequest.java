package com.qinghe.life.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class CouponStatusRequest {
    @NotBlank
    @Pattern(regexp = "ENABLED|DISABLED")
    private String status;
}
