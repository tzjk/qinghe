package com.qinghe.life.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class AssetUpdateRequest {
    @NotBlank @Size(max = 16) private String assetStatus;
    @Size(max = 255) private String remark;
}
