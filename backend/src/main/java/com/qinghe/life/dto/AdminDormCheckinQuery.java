package com.qinghe.life.dto;
import lombok.Data;
@Data public class AdminDormCheckinQuery extends PageQuery { private String studentName; private String studentNo; private Long campusId; private Long buildingId; private String roomNo; private String checkinStatus; private String studentStatus; }
