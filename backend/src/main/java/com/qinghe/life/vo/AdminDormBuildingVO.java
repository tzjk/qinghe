package com.qinghe.life.vo;

import lombok.Data;

@Data
public class AdminDormBuildingVO {
    private Long id;
    private Long campusId;
    private String campusName;
    private String buildingCode;
    private String buildingName;
    private String buildingType;
    private String area;
    private String remark;
    private Integer status;
    private Long roomCount;
    private Long bedCount;
    private Long currentCheckinCount;
    private Boolean codeEditable;
}
