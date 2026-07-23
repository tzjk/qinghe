package com.qinghe.life.service;
import com.qinghe.life.dto.AdminLoginRequest;
import com.qinghe.life.vo.AdminInfoVO;
import com.qinghe.life.vo.AdminLoginVO;
public interface AdminAuthService {
    AdminLoginVO login(AdminLoginRequest request);
    AdminInfoVO current();
    void logout();
}
