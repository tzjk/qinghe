package com.qinghe.life.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@JsonIgnoreProperties({"userId", "addressType", "province", "city", "district", "detailAddress"})
public class AddressUpdateDTO {
    @NotBlank
    @Size(max = 64)
    private String receiverName;

    @NotBlank
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    private String receiverPhone;

    @NotNull
    private Long campusId;

    @NotNull
    private Long buildingId;

    @Size(max = 20)
    private String floor;

    @Size(max = 64)
    private String roomNo;

    @Size(max = 128)
    private String deliveryPoint;

    @Size(max = 500)
    private String detail;

    @Size(max = 32)
    private String label;

    @Size(max = 255)
    private String remark;

    private Boolean isDefault;
}
