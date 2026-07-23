package com.qinghe.life.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class DormRoomUpdateRequest {
    @NotBlank @Size(max = 32) private String roomNo;
    @Size(max = 20) private String floor;
    @NotNull @Min(1) @Max(20) private Integer capacity;
    @NotNull @Min(0) @Max(1) private Integer status;
    @Size(max = 255) private String remark;
}
