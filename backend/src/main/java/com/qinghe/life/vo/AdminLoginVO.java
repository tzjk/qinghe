package com.qinghe.life.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminLoginVO {
    private String token;
    private AdminInfoVO admin;
}
