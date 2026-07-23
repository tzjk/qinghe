package com.qinghe.life.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class PasswordLoginRequest {
    @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") private String phone;
    @NotBlank @Size(max = 64) private String password;
}
