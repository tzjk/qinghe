package com.qinghe.life.vo;
import lombok.AllArgsConstructor;
import lombok.Data;
@Data @AllArgsConstructor public class LoginVO { private String token; private Boolean newUser; private Boolean profileCompleted; private Boolean hasPassword; private UserDTO user; }
