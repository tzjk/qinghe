package com.qinghe.life.dto;

import lombok.Data;

@Data
public class AdminStudentQuery extends PageQuery {
    private String studentName;
    private String studentNo;
    private String collegeName;
    private String majorName;
    private String className;
    private String studentStatus;
    /** ALL, IN_DORM, NOT_IN_DORM */
    private String accommodationStatus;
}
