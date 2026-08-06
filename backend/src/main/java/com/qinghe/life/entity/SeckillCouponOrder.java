package com.qinghe.life.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Independent asynchronous order for a limited coupon; it never uses qh_order. */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qh_seckill_coupon_order")
public class SeckillCouponOrder extends BaseEntity {
    private Long userId;
    private Long couponId;
    private String status;
}
