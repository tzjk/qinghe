package com.qinghe.life.service;

import com.qinghe.life.vo.SignInCalendarVO;
import com.qinghe.life.vo.SignInStatusVO;

public interface SignInService {
    SignInStatusVO signIn();
    SignInStatusVO status();
    SignInCalendarVO calendar(String month);
    Integer streak();
}
