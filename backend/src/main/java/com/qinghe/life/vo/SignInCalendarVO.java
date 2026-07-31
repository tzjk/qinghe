package com.qinghe.life.vo;

import java.util.List;
import lombok.Data;

@Data
public class SignInCalendarVO {
    private String month;
    private Integer monthDays;
    private List<Boolean> days;
}
