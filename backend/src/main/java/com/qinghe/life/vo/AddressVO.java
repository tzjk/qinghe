package com.qinghe.life.vo;

import com.qinghe.life.entity.UserAddress;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressVO {
    private Long id;
    private String receiverName;
    private String receiverPhone;
    private String maskedReceiverPhone;
    private Long campusId;
    private String campusName;
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
    private String formattedAddress;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private Integer isDefault;

    public static AddressVO fromAddress(UserAddress address, String campusName) {
        AddressVO view = new AddressVO();
        view.setId(address.getId());
        view.setReceiverName(address.getReceiverName());
        view.setReceiverPhone(address.getReceiverPhone());
        view.setMaskedReceiverPhone(maskPhone(address.getReceiverPhone()));
        view.setCampusId(address.getCampusId());
        view.setCampusName(campusName);
        view.setArea(address.getArea());
        view.setBuildingId(address.getBuildingId());
        view.setBuildingType(address.getBuildingType());
        view.setBuildingName(address.getBuildingName());
        view.setFloor(address.getFloor());
        view.setRoomNo(address.getRoomNo());
        view.setDeliveryPoint(address.getDeliveryPoint());
        view.setDetail(address.getDetail());
        view.setLabel(address.getLabel());
        view.setRemark(address.getRemark());
        view.setAddressType(address.getAddressType());
        view.setProvince(address.getProvince());
        view.setCity(address.getCity());
        view.setDistrict(address.getDistrict());
        view.setDetailAddress(address.getDetailAddress());
        view.setIsDefault(address.getIsDefault());
        view.setFormattedAddress(formattedAddress(view));
        return view;
    }

    private static String formattedAddress(AddressVO view) {
        StringBuilder value = new StringBuilder();
        append(value, view.getCampusName());
        append(value, view.getArea());
        append(value, view.getBuildingType());
        append(value, view.getBuildingName());
        append(value, view.getFloor());
        append(value, view.getRoomNo());
        append(value, view.getDeliveryPoint());
        append(value, view.getDetail());
        if (value.length() == 0) {
            append(value, view.getProvince());
            append(value, view.getCity());
            append(value, view.getDistrict());
            append(value, view.getDetailAddress());
        }
        return value.toString();
    }

    private static void append(StringBuilder builder, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(value.trim());
    }

    private static String maskPhone(String phone) {
        return phone == null ? null : phone.replaceFirst("^(\\d{3})\\d{4}(\\d{4})$", "$1****$2");
    }
}
