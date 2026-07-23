package com.qinghe.life.controller;
import com.qinghe.life.common.Result;
import com.qinghe.life.dto.SendCodeRequest;
import com.qinghe.life.dto.UserLoginRequest;
import com.qinghe.life.dto.PasswordLoginRequest;
import com.qinghe.life.dto.InitialProfileCompleteRequest;
import com.qinghe.life.dto.UserProfileUpdateRequest;
import com.qinghe.life.dto.UserRegisterRequest;
import com.qinghe.life.service.UserService;
import com.qinghe.life.annotation.OperateLog;
import com.qinghe.life.vo.LoginVO;
import com.qinghe.life.vo.UserDTO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.validation.Valid;
@Validated @RestController @RequestMapping("/api") public class UserController {
 private final UserService userService; public UserController(UserService userService){this.userService=userService;}
 @PostMapping("/user/code") public Result<String> sendCode(@Valid @RequestBody SendCodeRequest request){return Result.success(userService.sendCode(request));}
 @PostMapping("/auth/register") public Result<Void> register(@Valid @RequestBody UserRegisterRequest request){userService.register(request);return Result.success();}
 @PostMapping("/user/login") public Result<LoginVO> login(@Valid @RequestBody UserLoginRequest request){return Result.success(userService.login(request));}
 @PostMapping("/user/login/password") public Result<LoginVO> passwordLogin(@Valid @RequestBody PasswordLoginRequest request){return Result.success(userService.passwordLogin(request));}
 @PostMapping("/user/profile/complete") @OperateLog(module = "个人资料", action = "完成首次资料与校园地址") public Result<UserDTO> completeProfile(@Valid @RequestBody InitialProfileCompleteRequest request){return Result.success(userService.completeInitialProfile(request));}
 @GetMapping("/user/me") public Result<UserDTO> me(){return Result.success(userService.currentUser());}
 @PutMapping("/user/profile") @OperateLog(module = "个人资料", action = "修改个人资料") public Result<UserDTO> profile(@Valid @RequestBody UserProfileUpdateRequest request){return Result.success(userService.updateProfile(request));}
 @PostMapping(value = "/user/avatar", consumes = "multipart/form-data") @OperateLog(module = "个人资料", action = "上传头像") public Result<UserDTO> avatar(@RequestParam("file") MultipartFile file){return Result.success(userService.uploadAvatar(file));}
 @PostMapping("/user/logout") public Result<Void> logout(){userService.logout();return Result.success();}
}
