package com.qinghe.life.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DormDirectoryStatusRequest {
    @NotNull @Min(0) @Max(1) private Integer status;
}
