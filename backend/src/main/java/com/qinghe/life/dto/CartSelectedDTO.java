package com.qinghe.life.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class CartSelectedDTO {
    @NotNull
    private Boolean selected;
}
