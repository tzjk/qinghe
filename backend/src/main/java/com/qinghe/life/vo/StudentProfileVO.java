package com.qinghe.life.vo;
import com.qinghe.life.entity.Campus;
import com.qinghe.life.entity.StudentProfile;
import lombok.Data;
@Data public class StudentProfileVO { private String realName; private String studentNo; private Long campusId; private String campusName; private String collegeName; private String majorName; private String className; private String contactPhone; private String studentStatus; public static StudentProfileVO from(StudentProfile p,Campus campus){ if(p==null)return null; StudentProfileVO v=new StudentProfileVO(); v.setRealName(p.getRealName());v.setStudentNo(p.getStudentNo());v.setCampusId(p.getCampusId());v.setCampusName(campus==null?null:campus.getCampusName());v.setCollegeName(p.getCollegeName());v.setMajorName(p.getMajorName());v.setClassName(p.getClassName());v.setContactPhone(p.getContactPhone());v.setStudentStatus(p.getStudentStatus());return v;} }
