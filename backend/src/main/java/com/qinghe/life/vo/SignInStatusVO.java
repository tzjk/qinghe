package com.qinghe.life.vo;

import lombok.Data;

@Data
public class SignInStatusVO {
    private Boolean signedToday;
    private Integer monthDays;
    private Integer streakDays;
}
