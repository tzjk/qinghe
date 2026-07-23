package com.qinghe.life.service;

import com.qinghe.life.vo.BuildingVO;
import com.qinghe.life.vo.CampusVO;

import java.util.List;

public interface CampusService {
    List<CampusVO> listEnabled();
    List<BuildingVO> listEnabledBuildings(Long campusId);
}
