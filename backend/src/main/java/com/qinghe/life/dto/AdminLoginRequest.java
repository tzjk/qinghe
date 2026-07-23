package com.qinghe.life.dto;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
@Data public class AdminLoginRequest { @NotBlank @Size(max = 32) private String username; @NotBlank @Size(max = 64) private String password; }
