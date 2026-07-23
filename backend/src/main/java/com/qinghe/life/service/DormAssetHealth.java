package com.qinghe.life.service;

import com.qinghe.life.entity.Asset;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 资产套装的派生健康状态；只读取既有明细，不持久化额外字段。 */
public final class DormAssetHealth {
    private static final Set<String> CORE_TYPES = new LinkedHashSet<String>(Arrays.asList("BED", "BED_BOARD", "DESK", "WARDROBE", "STOOL"));
    private DormAssetHealth() { }

    public static Result inspect(List<Asset> assets) {
        Set<String> present = new LinkedHashSet<String>();
        boolean repair = false;
        boolean scrapped = false;
        for (Asset asset : assets) {
            if (!CORE_TYPES.contains(asset.getAssetType())) continue;
            present.add(asset.getAssetType());
            repair = repair || "REPAIR".equals(asset.getAssetStatus());
            scrapped = scrapped || "SCRAPPED".equals(asset.getAssetStatus());
        }
        if (present.size() != CORE_TYPES.size()) return new Result("INCOMPLETE", "核心固定资产不完整，暂不可入住");
        if (scrapped) return new Result("SCRAPPED", "核心固定资产存在报废资产，暂不可入住");
        if (repair) return new Result("REPAIR", "核心固定资产存在维修资产，暂不可入住");
        for (Asset asset : assets) if (CORE_TYPES.contains(asset.getAssetType()) && !"NORMAL".equals(asset.getAssetStatus())) return new Result("INCOMPLETE", "核心固定资产状态异常，暂不可入住");
        return new Result("NORMAL", null);
    }

    public static final class Result {
        private final String status;
        private final String unavailableReason;
        private Result(String status, String unavailableReason) { this.status = status; this.unavailableReason = unavailableReason; }
        public String getStatus() { return status; }
        public String getUnavailableReason() { return unavailableReason; }
        public boolean isAbnormal() { return !"NORMAL".equals(status); }
    }
}
