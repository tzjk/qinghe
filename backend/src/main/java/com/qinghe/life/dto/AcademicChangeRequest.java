package com.qinghe.life.dto;

import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class AcademicChangeRequest {
    private String collegeName;
    private String majorName;
    private String className;
    @NotBlank(message = "异动原因不能为空") @Size(max = 255, message = "异动原因不能超过255个字符")
    private String reason;
    private Boolean checkoutDorm;
    private LocalDateTime effectiveTime;
}
