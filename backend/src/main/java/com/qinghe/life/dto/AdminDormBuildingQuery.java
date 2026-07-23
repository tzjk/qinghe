package com.qinghe.life.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminDormBuildingQuery extends PageQuery {
    @NotNull @Min(1) private Long campusId;
    @Min(0) @Max(1) private Integer status;
    private String keyword;
}
