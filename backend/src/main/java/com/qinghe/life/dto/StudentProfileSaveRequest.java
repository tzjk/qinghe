package com.qinghe.life.dto;
import lombok.Data;
import javax.validation.constraints.*;
@Data public class StudentProfileSaveRequest { @Size(max=50) @Pattern(regexp="^[\\u4e00-\\u9fa5A-Za-z·]{2,50}$") private String realName; @Pattern(regexp="^[A-Za-z0-9_-]{4,32}$") private String studentNo; private Long campusId; @Size(max=100) private String collegeName; @Size(max=100) private String majorName; @Size(max=100) private String className; @NotBlank @Pattern(regexp="^1\\d{10}$") private String contactPhone; }
