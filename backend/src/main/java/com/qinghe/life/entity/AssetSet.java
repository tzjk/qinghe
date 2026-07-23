package com.qinghe.life.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qh_asset_set")
public class AssetSet extends BaseEntity {
    private Long dormBedId;
    private String assetSetNo;
    private String qrToken;
    private Integer qrStatus;
    private String status;
    private LocalDateTime qrRotatedTime;
    private String remark;
}
