package com.qinghe.life.vo;
import com.qinghe.life.entity.DormRoom;
import lombok.Data;
@Data public class DormRoomVO { private Long id; private Long campusId; private Long buildingId; private String roomNo; private String floor; private Integer capacity; private Integer status; private String remark; private Long bedCount; private Long currentCheckinCount; private Boolean roomNoEditable; public static DormRoomVO from(DormRoom item){ DormRoomVO vo=new DormRoomVO(); vo.id=item.getId(); vo.campusId=item.getCampusId(); vo.buildingId=item.getBuildingId(); vo.roomNo=item.getRoomNo(); vo.floor=item.getFloor(); vo.capacity=item.getCapacity(); vo.status=item.getStatus(); vo.remark=item.getRemark(); return vo; } }
