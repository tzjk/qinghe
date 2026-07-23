package com.qinghe.life.controller;

import com.qinghe.life.common.Result;
import com.qinghe.life.dto.AdminLoginRequest;
import com.qinghe.life.service.AdminAuthService;
import com.qinghe.life.vo.AdminInfoVO;
import com.qinghe.life.vo.AdminLoginVO;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {
    private final AdminAuthService adminAuthService;
    public AdminAuthController(AdminAuthService adminAuthService) { this.adminAuthService = adminAuthService; }
    @PostMapping("/login") public Result<AdminLoginVO> login(@Valid @RequestBody AdminLoginRequest request) { return Result.success(adminAuthService.login(request)); }
    @GetMapping("/me") public Result<AdminInfoVO> current() { return Result.success(adminAuthService.current()); }
    @PostMapping("/logout") public Result<Void> logout() { adminAuthService.logout(); return Result.success(); }
}
