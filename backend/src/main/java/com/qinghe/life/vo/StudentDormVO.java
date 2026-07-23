package com.qinghe.life.vo;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
@Data public class StudentDormVO { private String realName; private String maskedStudentNo; private String collegeName; private String majorName; private String className; private String studentStatus; private String maskedContactPhone; private String campusName; private String buildingName; private String roomNo; private String bedNo; private String assetSetCode; private List<String> assetNames; private LocalDateTime checkInTime; private String checkInStatus; }
