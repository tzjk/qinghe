package com.qinghe.life.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qh_admin")
public class Admin extends BaseEntity {
    private String username;
    private String passwordHash;
    private String displayName;
    private Integer status;
}
