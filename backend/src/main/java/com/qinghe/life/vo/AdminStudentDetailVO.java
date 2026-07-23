package com.qinghe.life.vo;

import java.util.List;
import lombok.Data;

@Data
public class AdminStudentDetailVO {
    private AdminStudentVO profile;
    private String maskedContactPhone;
    private String dormSummary;
    private List<AdminStudentProfileVersionVO> history;
}
