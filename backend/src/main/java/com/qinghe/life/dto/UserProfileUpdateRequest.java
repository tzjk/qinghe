package com.qinghe.life.dto;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
@Data public class UserProfileUpdateRequest { @NotBlank @Size(max = 64) private String nickname; @Size(max = 255) private String avatarUrl; }
