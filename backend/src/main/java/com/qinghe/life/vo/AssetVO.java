package com.qinghe.life.vo;
import com.qinghe.life.entity.Asset;
import lombok.Data;
@Data public class AssetVO { private Long id; private String assetType; private String assetName; private String assetStatus; private String remark; public static AssetVO from(Asset item){ AssetVO vo=new AssetVO(); vo.id=item.getId(); vo.assetType=item.getAssetType(); vo.assetName=item.getAssetName(); vo.assetStatus=item.getAssetStatus(); vo.remark=item.getRemark(); return vo; } }
