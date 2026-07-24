package com.qinghe.life.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qh_order")
public class Order extends BaseEntity {
    private String orderNo;
    private Long userId;
    private Long shopId;
    private Long addressId;
    private String receiverName;
    private String receiverPhone;
    private String deliveryAddress;
    private Long campusId;
    private String campusName;
    private String addressArea;
    private Long buildingId;
    private String buildingType;
    private String buildingName;
    private String floor;
    private String roomNo;
    private String deliveryPoint;
    private String addressDetail;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal deliveryFee;
    private BigDecimal payAmount;
    private LocalDateTime payTime;
    private LocalDateTime acceptedTime;
    private LocalDateTime deliveryTime;
    private LocalDateTime payExpireTime;
    private LocalDateTime cancelTime;
    private String cancelReason;
    private LocalDateTime completedTime;
    private String status;
    private String remark;
}
