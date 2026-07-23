package com.qinghe.life.controller;

import com.qinghe.life.annotation.OperateLog;
import com.qinghe.life.common.PageResult;
import com.qinghe.life.common.Result;
import com.qinghe.life.dto.AdminShopQuery;
import com.qinghe.life.dto.AdminShopSaveRequest;
import com.qinghe.life.dto.AdminShopStatusRequest;
import com.qinghe.life.service.ShopService;
import com.qinghe.life.vo.AdminShopVO;
import javax.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/admin/shops")
public class AdminShopController {
    private final ShopService shopService;

    public AdminShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping
    public Result<PageResult<AdminShopVO>> page(@Valid AdminShopQuery query) {
        return Result.success(shopService.adminPage(query));
    }

    @GetMapping("/{id}")
    public Result<AdminShopVO> detail(@PathVariable Long id) {
        return Result.success(shopService.adminDetail(id));
    }

    @PostMapping
    @OperateLog(module = "店铺管理", action = "新增店铺")
    public Result<AdminShopVO> create(@Valid @RequestBody AdminShopSaveRequest request) {
        return Result.success(shopService.createAdminShop(request));
    }

    @PutMapping("/{id}")
    @OperateLog(module = "店铺管理", action = "修改店铺资料")
    public Result<AdminShopVO> update(@PathVariable Long id, @Valid @RequestBody AdminShopSaveRequest request) {
        return Result.success(shopService.updateAdminShop(id, request));
    }

    @PutMapping("/{id}/status")
    @OperateLog(module = "店铺管理", action = "调整店铺状态")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody AdminShopStatusRequest request) {
        shopService.updateAdminShopStatus(id, request);
        return Result.success();
    }

    @PostMapping(value = "/{id}/cover", consumes = "multipart/form-data")
    @OperateLog(module = "店铺管理", action = "上传店铺封面")
    public Result<AdminShopVO> uploadCover(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return Result.success(shopService.uploadAdminShopCover(id, file));
    }
}
