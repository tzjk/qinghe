package com.qinghe.life.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
@Data @EqualsAndHashCode(callSuper=true) @TableName("qh_student_profile")
public class StudentProfile extends BaseEntity { private Long userId; private String realName; private String studentNo; private Long campusId; private String collegeName; private String majorName; private String className; private String contactPhone; private String studentStatus; private LocalDateTime effectiveTime; private LocalDateTime endTime; private Integer currentFlag; private String changeReason; private Long operatorAdminId; }
