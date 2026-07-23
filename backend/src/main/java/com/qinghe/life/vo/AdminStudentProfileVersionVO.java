package com.qinghe.life.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AdminStudentProfileVersionVO {
    private Long id;
    private String collegeName;
    private String majorName;
    private String className;
    private String studentStatus;
    private LocalDateTime effectiveTime;
    private LocalDateTime endTime;
    private String changeReason;
    private Long operatorAdminId;
    private boolean current;
}
