package com.qinghe.life.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminDormBuildingUpdateRequest {
    @NotBlank @Size(max = 32) private String buildingCode;
    @NotBlank @Size(max = 100) private String buildingName;
    @Size(max = 64) private String area;
    @Size(max = 255) private String remark;
    @NotNull @Min(0) @Max(1) private Integer status;
}
