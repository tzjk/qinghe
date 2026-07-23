package com.qinghe.life.controller;

import com.qinghe.life.annotation.OperateLog;
import com.qinghe.life.common.Result;
import com.qinghe.life.dto.AdminGoodsCategoryQuery;
import com.qinghe.life.dto.GoodsCategorySaveRequest;
import com.qinghe.life.dto.GoodsCategoryStatusRequest;
import com.qinghe.life.dto.GoodsCategoryUpdateRequest;
import com.qinghe.life.service.AdminGoodsCategoryService;
import com.qinghe.life.vo.GoodsCategoryVO;
import java.util.List;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/goods/categories")
public class AdminGoodsCategoryController {
    private final AdminGoodsCategoryService service;

    public AdminGoodsCategoryController(AdminGoodsCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public Result<List<GoodsCategoryVO>> list(@Valid AdminGoodsCategoryQuery query) {
        return Result.success(service.list(query));
    }

    @PostMapping
    @OperateLog(module = "商品分类", action = "新增商品分类")
    public Result<GoodsCategoryVO> create(@Valid @RequestBody GoodsCategorySaveRequest request) {
        return Result.success(service.create(request));
    }

    @PutMapping("/{id}")
    @OperateLog(module = "商品分类", action = "修改商品分类")
    public Result<GoodsCategoryVO> update(@PathVariable Long id, @Valid @RequestBody GoodsCategoryUpdateRequest request) {
        return Result.success(service.update(id, request));
    }

    @PutMapping("/{id}/status")
    @OperateLog(module = "商品分类", action = "调整商品分类状态")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody GoodsCategoryStatusRequest request) {
        service.updateStatus(id, request);
        return Result.success();
    }
}
