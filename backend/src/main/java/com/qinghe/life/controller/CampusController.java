package com.qinghe.life.controller;

import com.qinghe.life.common.Result;
import com.qinghe.life.service.CampusService;
import com.qinghe.life.vo.BuildingVO;
import com.qinghe.life.vo.CampusVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/campuses")
public class CampusController {
    private final CampusService campusService;

    public CampusController(CampusService campusService) {
        this.campusService = campusService;
    }

    @GetMapping
    public Result<List<CampusVO>> listEnabled() {
        return Result.success(campusService.listEnabled());
    }

    @GetMapping("/{campusId}/buildings")
    public Result<List<BuildingVO>> listEnabledBuildings(@PathVariable Long campusId) {
        return Result.success(campusService.listEnabledBuildings(campusId));
    }
}
