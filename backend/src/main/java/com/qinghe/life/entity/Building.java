package com.qinghe.life.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qh_building")
public class Building extends BaseEntity {
    private Long campusId;
    private String area;
    private String remark;
    private String buildingType;
    private String buildingName;
    private String buildingCode;
    private Integer status;
    private Integer sortOrder;
}
