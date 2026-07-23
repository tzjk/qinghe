package com.qinghe.life.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AdminStudentVO {
    private Long userId;
    private Long profileId;
    private String realName;
    private String maskedStudentNo;
    private String collegeName;
    private String majorName;
    private String className;
    private String studentStatus;
    private LocalDateTime effectiveTime;
    private boolean currentCheckin;
    private String dormSummary;
}
