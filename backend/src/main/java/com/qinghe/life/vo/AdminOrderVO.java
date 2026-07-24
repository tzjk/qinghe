package com.qinghe.life.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminOrderVO {
    private Long orderId;
    private String orderNo;
    private String shopName;
    private String userDisplayName;
    private String maskedUserPhone;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private String status;
    private String statusName;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime acceptedTime;
    private LocalDateTime deliveryTime;
    private LocalDateTime completedTime;
    private LocalDateTime cancelTime;
    private String deliveryAddress;
    private String receiverName;
    private String receiverPhone;
    private List<OrderItemVO> items;
}
