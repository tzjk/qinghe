package com.qinghe.life.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ExploreStatusRequest {
    @NotBlank @Pattern(regexp = "PUBLISHED|DISABLED", message = "状态不正确") private String status;
}
