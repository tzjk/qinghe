package com.qinghe.life.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class DormBedUpdateRequest {
    @NotBlank @Size(max = 16) private String bedNo;
    @NotNull @Min(0) @Max(1) private Integer status;
    @Size(max = 255) private String remark;
}
