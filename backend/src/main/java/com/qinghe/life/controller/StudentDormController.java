package com.qinghe.life.controller;
import com.qinghe.life.annotation.OperateLog;
import com.qinghe.life.common.Result;
import com.qinghe.life.dto.DormQrRequest;
import com.qinghe.life.dto.StudentProfileSaveRequest;
import com.qinghe.life.service.StudentDormService;
import com.qinghe.life.vo.*;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
@RestController @RequestMapping("/api/student") public class StudentDormController { private final StudentDormService service; public StudentDormController(StudentDormService service){this.service=service;} @GetMapping("/profile") public Result<StudentProfileVO> profile(){return Result.success(service.getProfile());} @PutMapping("/profile") @OperateLog(module="学生资料",action="维护本人学生资料") public Result<StudentProfileVO> save(@Valid @RequestBody StudentProfileSaveRequest r){return Result.success(service.saveProfile(r));} @PostMapping("/dorm/qr/resolve") public Result<DormQrResolveVO> resolve(@Valid @RequestBody DormQrRequest r){return Result.success(service.resolveQr(r));} @PostMapping("/dorm/check-in") @OperateLog(module="宿舍入住",action="确认扫码入住") public Result<StudentDormVO> checkIn(@Valid @RequestBody DormQrRequest r){return Result.success(service.checkIn(r));} @GetMapping("/dorm/me") public Result<StudentDormVO> me(){return Result.success(service.myDorm());} }
