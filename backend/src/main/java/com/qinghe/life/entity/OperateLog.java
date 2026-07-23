package com.qinghe.life.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("qh_operate_log")
public class OperateLog {
    @TableId
    private Long id;
    private Long userId;
    private String module;
    private String action;
    private String controllerClass;
    private String controllerMethod;
    private String requestPath;
    private String httpMethod;
    private String requestSummary;
    private String responseSummary;
    private Integer success;
    private String exceptionSummary;
    private Long durationMs;
    private String ip;
    private LocalDateTime operateTime;
}
