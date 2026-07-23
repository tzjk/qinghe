package com.qinghe.life.dto;
import lombok.Data;
import javax.validation.constraints.*;
@Data public class DormRoomSaveRequest { private Long campusId; @NotNull private Long buildingId; @NotBlank @Size(max = 32) private String roomNo; @Size(max = 20) private String floor; @Min(1) @Max(20) private Integer capacity = 4; @Min(0) @Max(1) private Integer status = 1; @Size(max = 255) private String remark; }
