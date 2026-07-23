package com.qinghe.life.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qh_asset")
public class Asset extends BaseEntity {
    private Long assetSetId;
    private String assetType;
    private String assetName;
    private String assetStatus;
    private String remark;
}
