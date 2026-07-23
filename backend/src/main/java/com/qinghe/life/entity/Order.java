package com.qinghe.life.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

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
    private String status;
    private String remark;
}
