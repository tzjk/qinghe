package com.qinghe.life.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public abstract class BaseEntity {
    @TableId
    private Long id;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
