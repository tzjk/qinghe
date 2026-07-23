package com.qinghe.life.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qh_user_address")
public class UserAddress extends BaseEntity {
    private Long userId;
    private String receiverName;
    private String receiverPhone;
    private Long campusId;
    private String area;
    private Long buildingId;
    private String buildingType;
    private String buildingName;
    private String floor;
    private String roomNo;
    private String deliveryPoint;
    private String detail;
    private String label;
    private String remark;
    private String addressType;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private Integer isDefault;
}
