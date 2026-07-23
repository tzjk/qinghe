package com.qinghe.life.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.dto.AdminLoginRequest;
import com.qinghe.life.entity.Admin;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.AdminMapper;
import com.qinghe.life.service.AdminAuthService;
import com.qinghe.life.utils.AdminContext;
import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.vo.AdminInfoVO;
import com.qinghe.life.vo.AdminLoginVO;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthServiceImpl implements AdminAuthService {
    private final AdminMapper adminMapper;
    private final StringRedisTemplate redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    public AdminAuthServiceImpl(AdminMapper adminMapper, StringRedisTemplate redisTemplate) { this.adminMapper = adminMapper; this.redisTemplate = redisTemplate; }
    @Override public AdminLoginVO login(AdminLoginRequest request) {
        Admin admin = adminMapper.selectOne(Wrappers.<Admin>lambdaQuery().eq(Admin::getUsername, request.getUsername()));
        if (admin == null || !Integer.valueOf(1).equals(admin.getStatus()) || !passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) throw new BusinessException(401, "管理员账号或密码错误");
        String token = UUID.randomUUID().toString().replace("-", "");
        AdminInfoVO adminInfo = AdminInfoVO.fromAdmin(admin);
        Map<String, String> session = new HashMap<String, String>(); session.put("adminId", String.valueOf(adminInfo.getId())); session.put("username", adminInfo.getUsername()); session.put("displayName", adminInfo.getDisplayName());
        redisTemplate.opsForHash().putAll(RedisKeys.adminToken(token), session);
        redisTemplate.expire(RedisKeys.adminToken(token), RedisKeys.ADMIN_TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
        return new AdminLoginVO(token, adminInfo);
    }

    @Override public AdminInfoVO current() { return AdminContext.getAdmin(); }

    @Override public void logout() {
        String token = AdminContext.getToken();
        if (token != null) redisTemplate.delete(RedisKeys.adminToken(token));
    }
}
