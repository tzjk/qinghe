package com.qinghe.life.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qh_coupon")
public class Coupon extends BaseEntity {
    private String name;
    private String couponType;
    private BigDecimal discountAmount;
    private BigDecimal thresholdAmount;
    private Integer totalStock;
    private Integer claimedCount;
    private String couponStatus;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
