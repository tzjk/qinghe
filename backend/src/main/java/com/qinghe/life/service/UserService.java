package com.qinghe.life.service;
import com.qinghe.life.dto.SendCodeRequest;
import com.qinghe.life.dto.UserLoginRequest;
import com.qinghe.life.dto.PasswordLoginRequest;
import com.qinghe.life.dto.InitialProfileCompleteRequest;
import com.qinghe.life.dto.UserProfileUpdateRequest;
import com.qinghe.life.dto.UserRegisterRequest;
import com.qinghe.life.vo.LoginVO;
import com.qinghe.life.vo.UserDTO;
import org.springframework.web.multipart.MultipartFile;
public interface UserService { String sendCode(SendCodeRequest request); void register(UserRegisterRequest request); LoginVO login(UserLoginRequest request); LoginVO passwordLogin(PasswordLoginRequest request); UserDTO completeInitialProfile(InitialProfileCompleteRequest request); UserDTO currentUser(); UserDTO updateProfile(UserProfileUpdateRequest request); UserDTO uploadAvatar(MultipartFile file); void logout(); }
