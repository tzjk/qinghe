package com.qinghe.life.controller;

import com.qinghe.life.common.Result;
import com.qinghe.life.service.SignInService;
import com.qinghe.life.vo.SignInCalendarVO;
import com.qinghe.life.vo.SignInStatusVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/sign-in")
public class SignInController {
    private final SignInService signInService;
    public SignInController(SignInService signInService) { this.signInService = signInService; }
    @PostMapping public Result<SignInStatusVO> signIn() { return Result.success(signInService.signIn()); }
    @GetMapping("/status") public Result<SignInStatusVO> status() { return Result.success(signInService.status()); }
    @GetMapping("/calendar") public Result<SignInCalendarVO> calendar(@RequestParam(required = false) String month) { return Result.success(signInService.calendar(month)); }
    @GetMapping("/streak") public Result<Integer> streak() { return Result.success(signInService.streak()); }
}
