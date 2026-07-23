package com.qinghe.life.dto;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
@Data public class SendCodeRequest { @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") private String phone; }
