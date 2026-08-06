package com.qinghe.life.controller;

import com.qinghe.life.common.Result;
import com.qinghe.life.seckilltest.LocalSeckillTestService;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/local-seckill-test")
@Profile("local-seckill-test")
@ConditionalOnProperty(prefix = "seckill.test", name = "enabled", havingValue = "true")
public class LocalSeckillTestController {
    private final LocalSeckillTestService service;
    public LocalSeckillTestController(LocalSeckillTestService service){this.service=service;}
    @PostMapping("/runs") public Result<Map<String,Object>> setup(){return Result.success(service.setup());}
    @PostMapping("/runs/{runId}/id-check") public Result<Map<String,Object>> idCheck(@PathVariable String runId){return Result.success(service.idCheck(runId));}
    @PostMapping("/runs/{runId}/reliability-probe") public Result<Map<String,Object>> reliabilityProbe(@PathVariable String runId) throws Exception{return Result.success(service.reliabilityProbe(runId));}
    @GetMapping("/runs/{runId}/snapshot") public Result<Map<String,Object>> snapshot(@PathVariable String runId){return Result.success(service.snapshot(runId));}
    @DeleteMapping("/runs/{runId}") public Result<Void> cleanup(@PathVariable String runId){service.cleanup(runId);return Result.success();}
}
