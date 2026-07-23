package com.qinghe.life.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.entity.AssetSet;
import com.qinghe.life.entity.DormCheckin;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.AssetSetMapper;
import com.qinghe.life.mapper.DormCheckinMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/** Shared transactional kernel for every administrator checkout path. */
@Component
public class DormCheckinLifecycle {
    private final DormCheckinMapper checkins;
    private final AssetSetMapper sets;

    public DormCheckinLifecycle(DormCheckinMapper checkins, AssetSetMapper sets) {
        this.checkins = checkins;
        this.sets = sets;
    }

    public DormCheckin lockActiveCheckin(Long checkinId) {
        DormCheckin value = checkins.selectOne(Wrappers.<DormCheckin>lambdaQuery()
            .eq(DormCheckin::getId, checkinId).eq(DormCheckin::getActiveFlag, 1).last("FOR UPDATE"));
        if (value == null) throw new BusinessException("该入住记录已退宿或不存在");
        return value;
    }

    public DormCheckin lockActiveByUser(Long userId) {
        return checkins.selectOne(Wrappers.<DormCheckin>lambdaQuery()
            .eq(DormCheckin::getUserId, userId).eq(DormCheckin::getActiveFlag, 1).last("FOR UPDATE"));
    }

    public AssetSet lockAssetSet(Long assetSetId) {
        AssetSet value = sets.selectOne(Wrappers.<AssetSet>lambdaQuery().eq(AssetSet::getId, assetSetId).last("FOR UPDATE"));
        if (value == null) throw new BusinessException("入住资产套装不存在");
        return value;
    }

    public void checkout(DormCheckin checkin, String reason, Long adminId, String status) {
        String normalized = requireReason(reason);
        LocalDateTime time = LocalDateTime.now();
        int changed = checkins.update(null, Wrappers.<DormCheckin>lambdaUpdate()
            .eq(DormCheckin::getId, checkin.getId()).eq(DormCheckin::getActiveFlag, 1)
            .set(DormCheckin::getCheckoutTime, time).set(DormCheckin::getCheckoutReason, normalized)
            .set(DormCheckin::getCheckinStatus, status).set(DormCheckin::getActiveFlag, null)
            .set(DormCheckin::getOperatorAdminId, adminId));
        if (changed != 1) throw new BusinessException("入住记录更新失败，请稍后重试");
        AssetSet set = lockAssetSet(checkin.getAssetSetId());
        if (!"OCCUPIED".equals(set.getStatus())) throw new BusinessException("入住资产套装状态异常，无法办理退宿");
        set.setStatus("AVAILABLE");
        if (sets.updateById(set) != 1) throw new BusinessException("资产套装释放失败，请稍后重试");
    }

    public void releaseWithoutActiveCheckin(Long assetSetId) {
        AssetSet set = lockAssetSet(assetSetId);
        long active = checkins.selectCount(Wrappers.<DormCheckin>lambdaQuery()
            .eq(DormCheckin::getAssetSetId, assetSetId).eq(DormCheckin::getActiveFlag, 1));
        if (active > 0) throw new BusinessException("资产套装存在当前入住，不能强制释放");
        if (!"OCCUPIED".equals(set.getStatus())) throw new BusinessException("仅可释放无当前入住但仍为占用的资产套装");
        set.setStatus("AVAILABLE");
        if (sets.updateById(set) != 1) throw new BusinessException("资产套装释放失败，请稍后重试");
    }

    private String requireReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) throw new BusinessException("办理原因不能为空");
        return reason.trim();
    }
}
