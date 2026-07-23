package com.qinghe.life.vo;
import com.qinghe.life.entity.AssetSet;
import java.util.List;
import lombok.Data;
@Data public class AssetSetDetailVO { private Long id; private Long dormBedId; private String assetSetNo; private Integer qrStatus; private String status; private Boolean currentCheckin; private String healthStatus; private Boolean assetAbnormal; private List<AssetVO> assets; public static AssetSetDetailVO from(AssetSet item,List<AssetVO> assets){ AssetSetDetailVO vo=new AssetSetDetailVO(); vo.id=item.getId(); vo.dormBedId=item.getDormBedId(); vo.assetSetNo=item.getAssetSetNo(); vo.qrStatus=item.getQrStatus(); vo.status=item.getStatus(); vo.assets=assets; return vo; } }
