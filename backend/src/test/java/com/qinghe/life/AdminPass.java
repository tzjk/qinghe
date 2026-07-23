package com.qinghe.life;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.junit.jupiter.api.Test;

public class AdminPass {
    @Test
    public void generatePassword() {
        // 1. 创建加密工具对象
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // 2. 换成你自己准备使用的管理员密码
        String rawPassword = "Qinghe@2026";

        // 3. 进行加密
        String encodedPassword = encoder.encode(rawPassword);

        // 4. 在控制台打印出加密后的密文
        System.out.println("原始密码: " + rawPassword);
        System.out.println("加密后密码: " + encodedPassword);
    }
}

