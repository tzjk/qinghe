package com.qinghe.life.controller;

import com.qinghe.life.common.Result;
import com.qinghe.life.annotation.OperateLog;
import com.qinghe.life.dto.AddressCreateDTO;
import com.qinghe.life.dto.AddressUpdateDTO;
import com.qinghe.life.service.AddressService;
import com.qinghe.life.vo.AddressVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/addresses")
public class AddressController {
    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public Result<List<AddressVO>> list() {
        return Result.success(addressService.listCurrentUser());
    }

    @GetMapping("/{id}")
    public Result<AddressVO> detail(@PathVariable Long id) {
        return Result.success(addressService.getCurrentUserAddress(id));
    }

    @OperateLog(module = "地址管理", action = "新增地址")
    @PostMapping
    public Result<AddressVO> create(@Valid @RequestBody AddressCreateDTO request) {
        return Result.success(addressService.create(request));
    }

    @OperateLog(module = "地址管理", action = "修改地址")
    @PutMapping("/{id}")
    public Result<AddressVO> update(@PathVariable Long id, @Valid @RequestBody AddressUpdateDTO request) {
        return Result.success(addressService.update(id, request));
    }

    @OperateLog(module = "地址管理", action = "删除地址")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.delete(id);
        return Result.success();
    }

    @OperateLog(module = "地址管理", action = "设为默认地址")
    @PutMapping("/{id}/default")
    public Result<AddressVO> setDefault(@PathVariable Long id) {
        return Result.success(addressService.setDefault(id));
    }
}
