package com.qinghe.life.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.Data;

@Data
public class GoodsCategoryStatusRequest {
    @NotNull
    @Pattern(regexp = "0|1")
    private String status;

    public Integer statusValue() {
        return Integer.valueOf(status);
    }
}
