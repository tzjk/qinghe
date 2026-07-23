package com.qinghe.life.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qh_dorm_bed")
public class DormBed extends BaseEntity {
    private Long dormRoomId;
    private String bedNo;
    private Integer status;
    private String remark;
}
