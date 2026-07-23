package com.qinghe.life.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
@Data @EqualsAndHashCode(callSuper=true) @TableName("qh_dorm_checkin")
public class DormCheckin extends BaseEntity { private Long userId; private Long studentProfileId; private Long dormBedId; private Long assetSetId; private Long previousCheckinId; private String checkinSource; private String checkinStatus; private Integer activeFlag; private String studentNoSnapshot; private String studentStatusSnapshot; private String campusNameSnapshot; private String buildingCodeSnapshot; private String buildingNameSnapshot; private String roomNoSnapshot; private String bedNoSnapshot; private String assetSetNoSnapshot; private LocalDateTime checkinTime; private LocalDateTime checkoutTime; private String checkoutReason; private String remark; private Long operatorAdminId; }
