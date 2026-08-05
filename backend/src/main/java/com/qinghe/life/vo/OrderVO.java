package com.qinghe.life.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO {
    private Long orderId;
    private String orderNo;
    private String shopName;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal deliveryFee;
    private BigDecimal payAmount;
    private String status;
    private String statusName;
    private LocalDateTime createTime;
    private LocalDateTime payExpireTime;
    private LocalDateTime payTime;
    private LocalDateTime cancelTime;
    private String cancelReason;
    private LocalDateTime acceptedTime;
    private LocalDateTime deliveryTime;
    private LocalDateTime completedTime;
    private String deliveryAddress;
    private String receiverName;
    private String receiverPhone;
    private String remark;
    private List<OrderItemVO> items;
}
