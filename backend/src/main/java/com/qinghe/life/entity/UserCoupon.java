package com.qinghe.life.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qh_user_coupon")
public class UserCoupon extends BaseEntity {
    private Long userId;
    private Long couponId;
    private Long orderId;
    private String status;
    private LocalDateTime receiveTime;
    private LocalDateTime lockTime;
    private LocalDateTime useTime;
    private LocalDateTime expireTime;
}
