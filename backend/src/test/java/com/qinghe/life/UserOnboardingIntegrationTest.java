package com.qinghe.life;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jayway.jsonpath.JsonPath;
import com.qinghe.life.entity.Building;
import com.qinghe.life.entity.Campus;
import com.qinghe.life.entity.User;
import com.qinghe.life.entity.UserAddress;
import com.qinghe.life.mapper.BuildingMapper;
import com.qinghe.life.mapper.CampusMapper;
import com.qinghe.life.mapper.OperateLogMapper;
import com.qinghe.life.mapper.UserAddressMapper;
import com.qinghe.life.mapper.UserMapper;
import com.qinghe.life.utils.RedisKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class UserOnboardingIntegrationTest {
    private static final String PHONE="13900009761", OLD_PHONE="13900009762", PASS="Campus2026";
    @Autowired MockMvc mvc; @Autowired StringRedisTemplate redis; @Autowired UserMapper users; @Autowired UserAddressMapper addresses; @Autowired OperateLogMapper logs; @Autowired CampusMapper campuses; @Autowired BuildingMapper buildings;
    private String token;
    @AfterEach void cleanup(){ for(String phone:new String[]{PHONE,OLD_PHONE}){User u=users.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPhone,phone));if(u!=null){logs.delete(Wrappers.lambdaQuery(com.qinghe.life.entity.OperateLog.class).eq(com.qinghe.life.entity.OperateLog::getUserId,u.getId()));addresses.delete(Wrappers.<UserAddress>lambdaQuery().eq(UserAddress::getUserId,u.getId()));users.deleteById(u.getId());}redis.delete(RedisKeys.code(phone));} if(token!=null)redis.delete(RedisKeys.token(token));}
    @Test void verificationPasswordAndProfileCompletionFlow() throws Exception {
        Campus campus=campuses.selectOne(Wrappers.<Campus>lambdaQuery().eq(Campus::getStatus,1).last("LIMIT 1")); Building building=buildings.selectOne(Wrappers.<Building>lambdaQuery().eq(Building::getCampusId,campus.getId()).eq(Building::getStatus,1).last("LIMIT 1"));
        redis.opsForValue().set(RedisKeys.code(PHONE),"246810"); String body=mvc.perform(post("/api/user/login").contentType("application/json").content("{\"phone\":\""+PHONE+"\",\"code\":\"246810\"}")) .andExpect(status().isOk()).andExpect(jsonPath("$.data.newUser").value(true)).andExpect(jsonPath("$.data.profileCompleted").value(false)).andExpect(jsonPath("$.data.hasPassword").value(false)).andReturn().getResponse().getContentAsString(); token=JsonPath.read(body,"$.data.token");
        String payload="{\"nickname\":\"ONBOARD_TEST\",\"receiverName\":\"测试收件人\",\"receiverPhone\":\"13900001111\",\"campusId\":"+campus.getId()+",\"buildingId\":"+building.getId()+",\"roomNo\":\"301\",\"password\":\""+PASS+"\"}";
        mvc.perform(post("/api/user/profile/complete").header("Authorization","Bearer "+token).contentType("application/json").content(payload)).andExpect(status().isOk()).andExpect(jsonPath("$.data.profileCompleted").value(true)).andExpect(jsonPath("$.data.hasPassword").value(true));
        User user=users.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPhone,PHONE)); assertTrue(new BCryptPasswordEncoder().matches(PASS,user.getPasswordHash())); assertEquals(1,user.getProfileCompleted().intValue()); assertEquals(1,addresses.selectCount(Wrappers.<UserAddress>lambdaQuery().eq(UserAddress::getUserId,user.getId())).intValue()); assertEquals("true",String.valueOf(redis.opsForHash().entries(RedisKeys.token(token)).get("profileCompleted")));
        mvc.perform(post("/api/user/login/password").contentType("application/json").content("{\"phone\":\""+PHONE+"\",\"password\":\""+PASS+"\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.profileCompleted").value(true));
        mvc.perform(post("/api/user/login/password").contentType("application/json").content("{\"phone\":\""+PHONE+"\",\"password\":\"wrong\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.message").value("手机号或密码错误"));
    }
}
