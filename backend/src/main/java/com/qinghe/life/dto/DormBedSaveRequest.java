package com.qinghe.life.dto;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
@Data public class DormBedSaveRequest { @NotBlank @Size(max = 16) private String bedNo; @Size(max = 255) private String remark; }
