package com.qinghe.life;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jayway.jsonpath.JsonPath;
import com.qinghe.life.entity.Building;
import com.qinghe.life.entity.Campus;
import com.qinghe.life.entity.OperateLog;
import com.qinghe.life.entity.User;
import com.qinghe.life.entity.UserAddress;
import com.qinghe.life.mapper.BuildingMapper;
import com.qinghe.life.mapper.CampusMapper;
import com.qinghe.life.mapper.OperateLogMapper;
import com.qinghe.life.mapper.UserAddressMapper;
import com.qinghe.life.mapper.UserMapper;
import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.utils.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AddressIntegrationTest {
    private static final String TEST_MARKER = "CAMPUS_ADDR_TEST_";
    private static final String OWNER_PHONE = "13900009871";
    private static final String OTHER_PHONE = "13900009872";

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UserMapper userMapper;
    @Autowired private UserAddressMapper userAddressMapper;
    @Autowired private CampusMapper campusMapper;
    @Autowired private BuildingMapper buildingMapper;
    @Autowired private OperateLogMapper operateLogMapper;
    @Autowired private StringRedisTemplate redis;

    private String ownerToken;
    private String otherToken;
    private Long ownerUserId;
    private Long enabledCampusId;
    private Long enabledBuildingId;
    private Long temporaryCampusId;
    private Long temporaryBuildingId;
    private Long disabledBuildingId;

    @BeforeEach
    void setUp() {
        cleanupTestUser(OWNER_PHONE);
        cleanupTestUser(OTHER_PHONE);
        cleanupCatalogFixtures();
        Campus campus = campusMapper.selectOne(Wrappers.<Campus>lambdaQuery().eq(Campus::getStatus, 1)
                .orderByAsc(Campus::getSortOrder).last("LIMIT 1"));
        Building building = campus == null ? null : buildingMapper.selectOne(Wrappers.<Building>lambdaQuery()
                .eq(Building::getCampusId, campus.getId()).eq(Building::getStatus, 1)
                .orderByAsc(Building::getSortOrder).last("LIMIT 1"));
        assertNotNull(campus);
        assertNotNull(building);
        enabledCampusId = campus.getId();
        enabledBuildingId = building.getId();
        UserContext.clear();
    }

    @AfterEach
    void tearDown() {
        cleanupTestUser(OWNER_PHONE);
        cleanupTestUser(OTHER_PHONE);
        cleanupCatalogFixtures();
        clearRedisTestKeys();
        UserContext.clear();
        assertNull(UserContext.getUser());
    }

    @Test
    void publicCampusAndBuildingQueriesReturnOnlyEnabledDirectoryData() throws Exception {
        mvc.perform(get("/api/campuses"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(enabledCampusId));
        mvc.perform(get("/api/campuses/{campusId}/buildings", enabledCampusId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].campusId").value(enabledCampusId))
                .andExpect(jsonPath("$.data[0].buildingName").isNotEmpty());
    }

    @Test
    void campusAddressCrudOwnershipDefaultHistoryAndOperateLogFlow() throws Exception {
        mvc.perform(get("/api/addresses")).andExpect(status().isUnauthorized());
        ownerToken = login(OWNER_PHONE, "OWNER");
        ownerUserId = userIdByPhone(OWNER_PHONE);
        String ownerHeader = bearer(ownerToken);

        Long firstAddressId = createCampusAddress(ownerHeader, "CAMPUS_ADDR_TEST_FIRST", "13900001111", false,
                enabledCampusId, enabledBuildingId, "302", "");
        assertEquals(1, queryDefault(firstAddressId));
        assertEquals("CAMPUS", queryString("SELECT address_type FROM qh_user_address WHERE id = ?", firstAddressId));
        OperateLog createLog = operateLogMapper.selectOne(Wrappers.<OperateLog>lambdaQuery()
                .eq(OperateLog::getUserId, ownerUserId).eq(OperateLog::getAction, "新增地址")
                .orderByDesc(OperateLog::getId).last("LIMIT 1"));
        assertNotNull(createLog);
        assertFalse(createLog.getRequestSummary().contains("13900001111"));
        assertEquals(1, createLog.getSuccess().intValue());

        Long secondAddressId = createCampusAddress(ownerHeader, "CAMPUS_ADDR_TEST_SECOND", "13900002222", false,
                enabledCampusId, enabledBuildingId, "", "图书馆一楼服务台");
        mvc.perform(put("/api/addresses/{id}/default", secondAddressId).header("Authorization", ownerHeader))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.isDefault").value(1));
        assertEquals(0, queryDefault(firstAddressId));
        assertEquals(1, queryDefault(secondAddressId));

        mvc.perform(put("/api/addresses/{id}", secondAddressId).header("Authorization", ownerHeader)
                        .contentType("application/json")
                        .content(campusAddressJson("CAMPUS_ADDR_TEST_UPDATED", "13900003333", enabledCampusId,
                                enabledBuildingId, "3", "305", "", true)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.addressType").value("CAMPUS"))
                .andExpect(jsonPath("$.data.formattedAddress").value(org.hamcrest.Matchers.containsString("青禾主校区")));

        Long historicalId = insertHistoricalAddress(ownerUserId);
        mvc.perform(get("/api/addresses/{id}", historicalId).header("Authorization", ownerHeader))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.addressType").value("HISTORICAL"))
                .andExpect(jsonPath("$.data.formattedAddress").value(org.hamcrest.Matchers.containsString("历史省")))
                .andExpect(jsonPath("$.data.maskedReceiverPhone").value("139****5555"));
        mvc.perform(put("/api/addresses/{id}", historicalId).header("Authorization", ownerHeader)
                        .contentType("application/json")
                        .content(campusAddressJson("CAMPUS_ADDR_TEST_HISTORY_UPDATED", "13900005555", enabledCampusId,
                                enabledBuildingId, "2", "201", "", false)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.addressType").value("CAMPUS"));
        assertEquals("历史省", queryString("SELECT province FROM qh_user_address WHERE id = ?", historicalId));
        assertEquals("历史详细地址", queryString("SELECT detail_address FROM qh_user_address WHERE id = ?", historicalId));

        otherToken = login(OTHER_PHONE, "OTHER");
        String otherHeader = bearer(otherToken);
        Long otherAddressId = createCampusAddress(otherHeader, "CAMPUS_ADDR_TEST_OTHER", "13900004444", false,
                enabledCampusId, enabledBuildingId, "101", "");
        String ownerList = mvc.perform(get("/api/addresses").header("Authorization", ownerHeader))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Number> ownerIds = JsonPath.read(ownerList, "$.data[*].id");
        assertFalse(ownerIds.stream().map(Number::longValue).anyMatch(id -> id.equals(otherAddressId)));
        mvc.perform(get("/api/addresses/{id}", otherAddressId).header("Authorization", ownerHeader))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(404));
        mvc.perform(put("/api/addresses/{id}", secondAddressId).header("Authorization", otherHeader)
                        .contentType("application/json")
                        .content(campusAddressJson("CAMPUS_ADDR_TEST_FORBIDDEN", "13900006666", enabledCampusId,
                                enabledBuildingId, "1", "102", "", false)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(404));

        mvc.perform(delete("/api/addresses/{id}", secondAddressId).header("Authorization", ownerHeader))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM qh_user_address WHERE user_id = ? AND is_default = 1",
                Integer.class, ownerUserId).intValue());
    }

    @Test
    void rejectsInvalidCampusBuildingRoomAndDisabledDirectory() throws Exception {
        ownerToken = login(OWNER_PHONE, "VALIDATION");
        String ownerHeader = bearer(ownerToken);
        mvc.perform(post("/api/addresses").header("Authorization", ownerHeader).contentType("application/json")
                        .content(campusAddressJson("CAMPUS_ADDR_TEST_EMPTY", "13900007777", enabledCampusId,
                                enabledBuildingId, "", "", "", false)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/api/addresses").header("Authorization", ownerHeader).contentType("application/json")
                        .content(campusAddressJson("CAMPUS_ADDR_TEST_INVALID", "13900007778", enabledCampusId,
                                999999999L, "1", "101", "", false)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        createCatalogFixtures();
        mvc.perform(post("/api/addresses").header("Authorization", ownerHeader).contentType("application/json")
                        .content(campusAddressJson("CAMPUS_ADDR_TEST_DISABLED", "13900007779", temporaryCampusId,
                                temporaryBuildingId, "1", "101", "", false)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/api/addresses").header("Authorization", ownerHeader).contentType("application/json")
                        .content(campusAddressJson("CAMPUS_ADDR_TEST_DISABLED_BUILDING", "13900007781", enabledCampusId,
                                disabledBuildingId, "1", "101", "", false)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/api/addresses").header("Authorization", ownerHeader).contentType("application/json")
                        .content(campusAddressJson("CAMPUS_ADDR_TEST_MISMATCH", "13900007780", enabledCampusId,
                                temporaryBuildingId, "1", "101", "", false)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
    }

    private String login(String phone, String suffix) throws Exception {
        mvc.perform(post("/api/user/code").contentType("application/json").content("{\"phone\":\"" + phone + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        String code = redis.opsForValue().get(RedisKeys.code(phone));
        assertNotNull(code);
        String response = mvc.perform(post("/api/user/login").contentType("application/json")
                        .content("{\"phone\":\"" + phone + "\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        User user = userMapper.selectById(userIdByPhone(phone));
        user.setNickname(TEST_MARKER + suffix);
        userMapper.updateById(user);
        return JsonPath.read(response, "$.data.token");
    }

    private Long createCampusAddress(String authorization, String name, String phone, boolean isDefault, Long campusId,
                                     Long buildingId, String roomNo, String deliveryPoint) throws Exception {
        String response = mvc.perform(post("/api/addresses").header("Authorization", authorization)
                        .contentType("application/json").content(campusAddressJson(name, phone, campusId, buildingId,
                                "3", roomNo, deliveryPoint, isDefault)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.addressType").value("CAMPUS"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.data.id")).longValue();
    }

    private String campusAddressJson(String name, String phone, Long campusId, Long buildingId, String floor,
                                     String roomNo, String deliveryPoint, boolean isDefault) {
        return "{\"receiverName\":\"" + name + "\",\"receiverPhone\":\"" + phone
                + "\",\"campusId\":" + campusId + ",\"buildingId\":" + buildingId
                + ",\"floor\":\"" + floor + "\",\"roomNo\":\"" + roomNo
                + "\",\"deliveryPoint\":\"" + deliveryPoint + "\",\"detail\":\"" + TEST_MARKER
                + "位置说明\",\"label\":\"宿舍\",\"remark\":\"请提前联系\",\"isDefault\":" + isDefault + "}";
    }

    private Long insertHistoricalAddress(Long userId) {
        UserAddress address = new UserAddress();
        address.setUserId(userId);
        address.setReceiverName(TEST_MARKER + "HISTORY");
        address.setReceiverPhone("13900005555");
        address.setProvince("历史省"); address.setCity("历史市"); address.setDistrict("历史区");
        address.setDetailAddress("历史详细地址"); address.setDetail("历史省 历史市 历史区 历史详细地址");
        address.setAddressType("HISTORICAL"); address.setIsDefault(0);
        userAddressMapper.insert(address);
        return address.getId();
    }

    private void createCatalogFixtures() {
        Campus campus = new Campus();
        campus.setCampusCode(TEST_MARKER + "DISABLED"); campus.setCampusName(TEST_MARKER + "停用校区");
        campus.setStatus(0); campus.setSortOrder(9999); campusMapper.insert(campus); temporaryCampusId = campus.getId();
        Building building = new Building();
        building.setCampusId(temporaryCampusId); building.setArea("测试区"); building.setBuildingType("测试楼");
        building.setBuildingName(TEST_MARKER + "跨校区楼栋"); building.setStatus(1); building.setSortOrder(9999);
        buildingMapper.insert(building); temporaryBuildingId = building.getId();
        Building disabledBuilding = new Building();
        disabledBuilding.setCampusId(enabledCampusId); disabledBuilding.setArea("测试区"); disabledBuilding.setBuildingType("测试楼");
        disabledBuilding.setBuildingName(TEST_MARKER + "停用楼栋"); disabledBuilding.setStatus(0); disabledBuilding.setSortOrder(9998);
        buildingMapper.insert(disabledBuilding); disabledBuildingId = disabledBuilding.getId();
    }

    private void cleanupCatalogFixtures() {
        buildingMapper.delete(Wrappers.<Building>lambdaQuery().likeRight(Building::getBuildingName, TEST_MARKER));
        campusMapper.delete(Wrappers.<Campus>lambdaQuery().likeRight(Campus::getCampusName, TEST_MARKER));
        temporaryCampusId = null; temporaryBuildingId = null; disabledBuildingId = null;
    }

    private Long userIdByPhone(String phone) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, phone));
        return user == null ? null : user.getId();
    }

    private int queryDefault(Long addressId) { return jdbc.queryForObject("SELECT is_default FROM qh_user_address WHERE id = ?", Integer.class, addressId); }
    private String queryString(String sql, Long id) { return jdbc.queryForObject(sql, String.class, id); }
    private String bearer(String token) { return "Bearer " + token; }

    private void cleanupTestUser(String phone) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, phone));
        if (user != null && user.getNickname() != null && user.getNickname().startsWith(TEST_MARKER)) {
            operateLogMapper.delete(Wrappers.<OperateLog>lambdaQuery().eq(OperateLog::getUserId, user.getId()));
            userAddressMapper.delete(Wrappers.<UserAddress>lambdaQuery().eq(UserAddress::getUserId, user.getId()));
            userMapper.deleteById(user.getId());
        }
    }

    private void clearRedisTestKeys() {
        List<String> keys = new java.util.ArrayList<String>(Arrays.asList(RedisKeys.code(OWNER_PHONE), RedisKeys.code(OTHER_PHONE)));
        if (ownerToken != null) { keys.add(RedisKeys.token(ownerToken)); }
        if (otherToken != null) { keys.add(RedisKeys.token(otherToken)); }
        redis.delete(keys);
    }
}
