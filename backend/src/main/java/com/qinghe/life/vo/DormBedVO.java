package com.qinghe.life.vo;
import com.qinghe.life.entity.DormBed;
import lombok.Data;
@Data public class DormBedVO { private Long id; private Long dormRoomId; private String bedNo; private Integer status; private String remark; private Boolean currentCheckin; private Long assetSetId; private String assetSetNo; private String assetSetStatus; private Integer qrStatus; private String assetHealthStatus; private Boolean assetAbnormal; private Boolean bedNoEditable; public static DormBedVO from(DormBed item){ DormBedVO vo=new DormBedVO(); vo.id=item.getId(); vo.dormRoomId=item.getDormRoomId(); vo.bedNo=item.getBedNo(); vo.status=item.getStatus(); vo.remark=item.getRemark(); return vo; } }
