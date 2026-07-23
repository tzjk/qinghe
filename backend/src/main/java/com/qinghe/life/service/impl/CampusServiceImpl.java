package com.qinghe.life.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.entity.Building;
import com.qinghe.life.entity.Campus;
import com.qinghe.life.mapper.BuildingMapper;
import com.qinghe.life.mapper.CampusMapper;
import com.qinghe.life.service.CampusService;
import com.qinghe.life.vo.BuildingVO;
import com.qinghe.life.vo.CampusVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CampusServiceImpl implements CampusService {
    private final CampusMapper campusMapper;
    private final BuildingMapper buildingMapper;

    public CampusServiceImpl(CampusMapper campusMapper, BuildingMapper buildingMapper) {
        this.campusMapper = campusMapper;
        this.buildingMapper = buildingMapper;
    }

    @Override
    public List<CampusVO> listEnabled() {
        return campusMapper.selectList(Wrappers.<Campus>lambdaQuery()
                        .eq(Campus::getStatus, 1)
                        .orderByAsc(Campus::getSortOrder)
                        .orderByAsc(Campus::getId))
                .stream().map(CampusVO::fromCampus).collect(Collectors.toList());
    }

    @Override
    public List<BuildingVO> listEnabledBuildings(Long campusId) {
        return buildingMapper.selectList(Wrappers.<Building>lambdaQuery()
                        .eq(Building::getCampusId, campusId)
                        .eq(Building::getStatus, 1)
                        .orderByAsc(Building::getSortOrder)
                        .orderByAsc(Building::getId))
                .stream().map(BuildingVO::fromBuilding).collect(Collectors.toList());
    }
}
