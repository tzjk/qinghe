package com.qinghe.life.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qh_dorm_room")
public class DormRoom extends BaseEntity {
    private Long campusId;
    private Long buildingId;
    private String roomNo;
    private String floor;
    private Integer capacity;
    private Integer status;
    private String remark;
}
