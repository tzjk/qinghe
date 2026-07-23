package com.qinghe.life.vo;

import lombok.Data;

@Data
public class DormBuildingStats {
    private Long buildingId;
    private Long roomCount;
    private Long bedCount;
    private Long assetSetCount;
    private Long checkinHistoryCount;
    private Long currentCheckinCount;
}
