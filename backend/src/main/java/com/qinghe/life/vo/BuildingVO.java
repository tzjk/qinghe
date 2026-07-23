package com.qinghe.life.vo;

import com.qinghe.life.entity.Building;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuildingVO {
    private Long id;
    private Long campusId;
    private String area;
    private String buildingType;
    private String buildingName;
    private Integer sortOrder;

    public static BuildingVO fromBuilding(Building building) {
        return new BuildingVO(building.getId(), building.getCampusId(), building.getArea(), building.getBuildingType(),
                building.getBuildingName(), building.getSortOrder());
    }
}
