package com.qinghe.life.vo;

import com.qinghe.life.entity.Campus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampusVO {
    private Long id;
    private String campusCode;
    private String campusName;
    private Integer sortOrder;

    public static CampusVO fromCampus(Campus campus) {
        return new CampusVO(campus.getId(), campus.getCampusCode(), campus.getCampusName(), campus.getSortOrder());
    }
}
