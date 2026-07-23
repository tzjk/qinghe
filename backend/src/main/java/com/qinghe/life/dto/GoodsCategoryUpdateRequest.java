package com.qinghe.life.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class GoodsCategoryUpdateRequest {
    @NotBlank
    @Size(max = 64)
    private String name;
    @NotNull
    @Min(0)
    private Integer sortOrder;
}
