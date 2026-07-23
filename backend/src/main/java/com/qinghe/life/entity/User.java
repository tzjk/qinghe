package com.qinghe.life.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qh_user")
public class User extends BaseEntity {
    private String phone;
    private String username;
    private String passwordHash;
    private Integer profileCompleted;
    private String nickname;
    private String avatarUrl;
    private Integer gender;
    private Integer status;
}
