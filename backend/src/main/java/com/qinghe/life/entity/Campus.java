package com.qinghe.life.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qh_campus")
public class Campus extends BaseEntity {
    private String campusCode;
    private String campusName;
    private Integer status;
    private Integer sortOrder;
}
