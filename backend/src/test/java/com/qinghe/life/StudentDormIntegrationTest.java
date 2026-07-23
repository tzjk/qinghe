package com.qinghe.life;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jayway.jsonpath.JsonPath;
import com.qinghe.life.entity.*;
import com.qinghe.life.mapper.*;
import com.qinghe.life.utils.RedisKeys;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StudentDormIntegrationTest {
    private static final String MARKER = "STUDENT_DORM_TEST_";
    private static final String TOKEN = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    @Autowired private MockMvc mvc;
    @Autowired private StringRedisTemplate redis;
    @Autowired private UserMapper userMapper;
    @Autowired private StudentProfileMapper profileMapper;
    @SpyBean private DormCheckinMapper checkinMapper;
    @Autowired private CampusMapper campusMapper;
    @Autowired private BuildingMapper buildingMapper;
    @Autowired private DormRoomMapper roomMapper;
    @Autowired private DormBedMapper bedMapper;
    @SpyBean private AssetSetMapper assetSetMapper;
    @Autowired private AssetMapper assetMapper;
    @Autowired private OperateLogMapper operateLogMapper;
    private Campus campus;
    private Building building;
    private DormRoom room;
    private DormBed bed;
    private AssetSet assetSet;

    @BeforeEach
    void setUp() {
        cleanup();
        campus = campusMapper.selectOne(Wrappers.<Campus>lambdaQuery().eq(Campus::getStatus, 1).last("LIMIT 1"));
        assertNotNull(campus, "学生资料测试需要一条既有启用校区");
        building = new Building(); building.setCampusId(campus.getId()); building.setArea("TEST"); building.setBuildingType("DORM"); building.setBuildingName(MARKER + "BUILDING"); building.setBuildingCode("TD"); building.setStatus(1); building.setSortOrder(9999); buildingMapper.insert(building);
        room = new DormRoom(); room.setCampusId(campus.getId()); room.setBuildingId(building.getId()); room.setRoomNo("901"); room.setFloor("9"); room.setCapacity(2); room.setStatus(1); roomMapper.insert(room);
        bed = new DormBed(); bed.setDormRoomId(room.getId()); bed.setBedNo("01"); bed.setStatus(1); bedMapper.insert(bed);
        assetSet = new AssetSet(); assetSet.setDormBedId(bed.getId()); assetSet.setAssetSetNo(MARKER + "SET"); assetSet.setQrToken(TOKEN); assetSet.setQrStatus(1); assetSet.setStatus("AVAILABLE"); assetSetMapper.insert(assetSet);
        for (String type : Arrays.asList("BED", "BED_BOARD", "DESK", "WARDROBE", "STOOL")) { Asset asset = new Asset(); asset.setAssetSetId(assetSet.getId()); asset.setAssetType(type); asset.setAssetName(MARKER + "ASSET_" + type); asset.setAssetStatus("NORMAL"); assetMapper.insert(asset); }
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(assetSetMapper);
        Mockito.reset(checkinMapper);
        cleanup();
    }

    @AfterAll
    void markedTestDataAndRedisSessionsAreFullyCleared() {
        assertEquals(0, userMapper.selectCount(Wrappers.<User>lambdaQuery().likeRight(User::getUsername, MARKER)).intValue());
        assertEquals(0, profileMapper.selectCount(Wrappers.<StudentProfile>lambdaQuery().likeRight(StudentProfile::getStudentNo, MARKER)).intValue());
        assertEquals(0, checkinMapper.selectCount(Wrappers.<DormCheckin>lambdaQuery().likeRight(DormCheckin::getStudentNoSnapshot, MARKER)).intValue());
        assertEquals(0, assetSetMapper.selectCount(Wrappers.<AssetSet>lambdaQuery().likeRight(AssetSet::getAssetSetNo, MARKER)).intValue());
        assertEquals(0, buildingMapper.selectCount(Wrappers.<Building>lambdaQuery().likeRight(Building::getBuildingName, MARKER)).intValue());
        assertTrue(redis.keys("qh:login:token:" + MARKER + "*").isEmpty());
    }

    @Test
    void profileCreateUpdateIsolationAndPublicSerializationAreSafe() throws Exception {
        User first = user("A", "13900001001", "NICKNAME_A");
        User second = user("B", "13900001002", "NICKNAME_B");
        mvc.perform(get("/api/student/profile").header("Authorization", auth(first))).andExpect(status().isOk()).andExpect(jsonPath("$.data").doesNotExist());
        String created = mvc.perform(put("/api/student/profile").header("Authorization", auth(first)).contentType(MediaType.APPLICATION_JSON)
                        .content(profileBody("测试甲", MARKER + "NO_A", "13800001001")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.studentStatus").value("ENROLLED"))
                .andExpect(jsonPath("$.data.campusId").value(campus.getId())).andExpect(jsonPath("$.data.campusName").value(campus.getCampusName()))
                .andExpect(jsonPath("$.data.currentFlag").doesNotExist()).andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.activeFlag").doesNotExist()).andExpect(jsonPath("$.data.qrToken").doesNotExist())
                .andExpect(jsonPath("$.data.id").doesNotExist()).andReturn().getResponse().getContentAsString();
        assertFalse(created.contains("currentFlag")); assertFalse(created.contains("passwordHash")); assertFalse(created.contains("activeFlag")); assertFalse(created.contains("qrToken"));
        StudentProfile saved = currentProfile(first); assertEquals(Integer.valueOf(1), saved.getCurrentFlag()); assertEquals("ENROLLED", saved.getStudentStatus());
        assertEquals("测试甲", saved.getRealName()); assertEquals("13800001001", saved.getContactPhone()); assertNotEquals(first.getNickname(), saved.getRealName()); assertNotEquals(first.getPhone(), saved.getContactPhone());
        mvc.perform(put("/api/student/profile").header("Authorization", auth(first)).contentType(MediaType.APPLICATION_JSON)
                        .content(profileBody("测试乙", MARKER + "NO_CHANGED", "13800001009")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.realName").value("测试甲")).andExpect(jsonPath("$.data.studentNo").value(MARKER + "NO_A"))
                .andExpect(jsonPath("$.data.contactPhone").value("13800001009"));
        mvc.perform(get("/api/student/profile").header("Authorization", auth(second))).andExpect(status().isOk()).andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void profileValidationAndCurrentStudentNumberUniquenessAreEnforced() throws Exception {
        User first = user("A", "13900001011", "NICKNAME_A");
        User second = user("B", "13900001012", "NICKNAME_B");
        mvc.perform(put("/api/student/profile").header("Authorization", auth(first)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"realName\":\"x\",\"studentNo\":\"bad\",\"contactPhone\":\"123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(put("/api/student/profile").header("Authorization", auth(first)).contentType(MediaType.APPLICATION_JSON)
                        .content(profileBody("测试甲", MARKER + "DUP", "13800001011")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mvc.perform(put("/api/student/profile").header("Authorization", auth(second)).contentType(MediaType.APPLICATION_JSON)
                        .content(profileBody("测试乙", MARKER + "DUP", "13800001012")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void profileCreationUsesSubmittedEnabledCampusAndRejectsDisabledCampus() throws Exception {
        Campus secondCampus = campus("SECOND", 1);
        User first = user("CAMPUS_A", "13900001013", "NICKNAME_CAMPUS_A");
        String payload = profileBody("测试丙", MARKER + "CAMPUS_A", "13800001013", secondCampus.getId())
                .replace("}", ",\"campusName\":\"伪造校区\"}");
        mvc.perform(put("/api/student/profile").header("Authorization", auth(first)).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.campusId").value(secondCampus.getId()))
                .andExpect(jsonPath("$.data.campusName").value(secondCampus.getCampusName()));
        assertEquals(secondCampus.getId(), currentProfile(first).getCampusId());

        Campus disabledCampus = campus("DISABLED", 0);
        User second = user("CAMPUS_B", "13900001014", "NICKNAME_CAMPUS_B");
        mvc.perform(put("/api/student/profile").header("Authorization", auth(second)).contentType(MediaType.APPLICATION_JSON)
                        .content(profileBody("测试丁", MARKER + "CAMPUS_B", "13800001014", disabledCampus.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        assertNull(currentProfile(second));
    }

    @Test
    void qrResolutionValidatesFormatAvailabilityAndDoesNotLeakTokenOrStudentData() throws Exception {
        User user = user("A", "13900001021", "NICKNAME_A");
        User other = user("QR_OTHER", "13900001023", "NICKNAME_QR_OTHER");
        profile(other, MARKER + "NO_QR_OTHER", "ENROLLED", "13800001023");
        String response = mvc.perform(post("/api/student/dorm/qr/resolve").header("Authorization", auth(user)).contentType(MediaType.APPLICATION_JSON).content(qrBody(qr())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.bedNo").value("01")).andExpect(jsonPath("$.data.assetSetStatus").value("AVAILABLE")).andExpect(jsonPath("$.data.assetHealthStatus").value("NORMAL")).andExpect(jsonPath("$.data.assetNames.length()").value(5))
                .andExpect(jsonPath("$.data.qrToken").doesNotExist()).andExpect(jsonPath("$.data.studentNo").doesNotExist()).andExpect(jsonPath("$.data.realName").doesNotExist())
                .andExpect(jsonPath("$.data.contactPhone").doesNotExist()).andExpect(jsonPath("$.data.currentFlag").doesNotExist()).andExpect(jsonPath("$.data.activeFlag").doesNotExist()).andExpect(jsonPath("$.data.id").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        assertFalse(response.contains(TOKEN)); assertFalse(response.contains(MARKER + "NO_QR_OTHER")); assertFalse(response.contains("13800001023"));
        for (String invalid : Arrays.asList("BAD:" + TOKEN, "QH-DORM-V1:123", "QH-DORM-V1:" + TOKEN.substring(0, 63) + "g")) {
            mvc.perform(post("/api/student/dorm/qr/resolve").header("Authorization", auth(user)).contentType(MediaType.APPLICATION_JSON).content(qrBody(invalid)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        }
        mvc.perform(post("/api/student/dorm/qr/resolve").header("Authorization", auth(user)).contentType(MediaType.APPLICATION_JSON).content(qrBody("QH-DORM-V1:" + repeat('f', 64))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(404));
        assetSet.setQrStatus(0); assetSetMapper.updateById(assetSet);
        mvc.perform(post("/api/student/dorm/qr/resolve").header("Authorization", auth(user)).contentType(MediaType.APPLICATION_JSON).content(qrBody(qr())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        assetSet.setQrStatus(1); assetSet.setStatus("DISABLED"); assetSetMapper.updateById(assetSet);
        mvc.perform(post("/api/student/dorm/qr/resolve").header("Authorization", auth(user)).contentType(MediaType.APPLICATION_JSON).content(qrBody(qr())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        assetSet.setStatus("OCCUPIED"); assetSetMapper.updateById(assetSet);
        mvc.perform(post("/api/student/dorm/qr/resolve").header("Authorization", auth(user)).contentType(MediaType.APPLICATION_JSON).content(qrBody(qr())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.available").value(false))
                .andExpect(jsonPath("$.data.qrToken").doesNotExist()).andExpect(jsonPath("$.data.studentNo").doesNotExist()).andExpect(jsonPath("$.data.realName").doesNotExist());
        assetSet.setStatus("AVAILABLE"); assetSetMapper.updateById(assetSet); Asset repair=assetMapper.selectOne(Wrappers.<Asset>lambdaQuery().eq(Asset::getAssetSetId,assetSet.getId()).eq(Asset::getAssetType,"BED")); repair.setAssetStatus("REPAIR"); assetMapper.updateById(repair);
        mvc.perform(post("/api/student/dorm/qr/resolve").header("Authorization", auth(user)).contentType(MediaType.APPLICATION_JSON).content(qrBody(qr())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.available").value(false)).andExpect(jsonPath("$.data.assetHealthStatus").value("REPAIR")).andExpect(jsonPath("$.data.unavailableReason").value("核心固定资产存在维修资产，暂不可入住"));
    }

    @Test
    void checkInRequiresEligibleProfileAndPreservesBedDirectoryStatus() throws Exception {
        User user = user("A", "13900001031", "NICKNAME_A");
        mvc.perform(post("/api/student/dorm/check-in").header("Authorization", auth(user)).contentType(MediaType.APPLICATION_JSON).content(qrBody(qr())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        StudentProfile profile = profile(user, MARKER + "NO_C", "SUSPENDED", "13800001031");
        mvc.perform(post("/api/student/dorm/check-in").header("Authorization", auth(user)).contentType(MediaType.APPLICATION_JSON).content(qrBody(qr())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        profile.setStudentStatus("ENROLLED"); profileMapper.updateById(profile);
        mvc.perform(post("/api/student/dorm/check-in").header("Authorization", auth(user)).contentType(MediaType.APPLICATION_JSON).content(qrBody(qr())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.activeFlag").doesNotExist())
                .andExpect(jsonPath("$.data.qrToken").doesNotExist()).andExpect(jsonPath("$.data.currentFlag").doesNotExist());
        DormCheckin checkin = checkinMapper.selectOne(Wrappers.<DormCheckin>lambdaQuery().eq(DormCheckin::getUserId, user.getId()).eq(DormCheckin::getActiveFlag, 1));
        assertNotNull(checkin); assertEquals(MARKER + "NO_C", checkin.getStudentNoSnapshot()); assertEquals("OCCUPIED", assetSetMapper.selectById(assetSet.getId()).getStatus()); assertEquals(Integer.valueOf(1), bedMapper.selectById(bed.getId()).getStatus());
    }

    @Test
    void duplicateUserAndOccupiedBedAreRejectedWithoutExtraCheckins() throws Exception {
        User first = user("A", "13900001041", "NICKNAME_A"); User second = user("B", "13900001042", "NICKNAME_B");
        profile(first, MARKER + "NO_D1", "ENROLLED", "13800001041"); profile(second, MARKER + "NO_D2", "ENROLLED", "13800001042");
        mvc.perform(post("/api/student/dorm/check-in").header("Authorization", auth(first)).contentType(MediaType.APPLICATION_JSON).content(qrBody(qr()))).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mvc.perform(post("/api/student/dorm/check-in").header("Authorization", auth(first)).contentType(MediaType.APPLICATION_JSON).content(qrBody(qr()))).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/api/student/dorm/check-in").header("Authorization", auth(second)).contentType(MediaType.APPLICATION_JSON).content(qrBody(qr()))).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        assertEquals(1, checkinMapper.selectCount(Wrappers.<DormCheckin>lambdaQuery().eq(DormCheckin::getDormBedId, bed.getId()).eq(DormCheckin::getActiveFlag, 1)).intValue());
    }

    @Test
    void uniqueIndexConflictBecomesFriendlyBusinessErrorWithoutAssetMutation() throws Exception {
        User user = user("UNIQUE", "13900001046", "NICKNAME_UNIQUE");
        profile(user, MARKER + "NO_UNIQUE", "ENROLLED", "13800001046");
        doThrow(new org.springframework.dao.DuplicateKeyException("uk_qh_dorm_checkin_bed_active"))
                .when(checkinMapper).insert(any(DormCheckin.class));
        String response = mvc.perform(post("/api/student/dorm/check-in").header("Authorization", auth(user)).contentType(MediaType.APPLICATION_JSON).content(qrBody(qr())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(409)).andReturn().getResponse().getContentAsString();
        assertFalse(response.contains("uk_qh_dorm_checkin_bed_active"));
        assertEquals(0, checkinMapper.selectCount(Wrappers.<DormCheckin>lambdaQuery().eq(DormCheckin::getUserId, user.getId()).eq(DormCheckin::getActiveFlag, 1)).intValue());
        assertEquals("AVAILABLE", assetSetMapper.selectById(assetSet.getId()).getStatus());
    }

    @Test
    void concurrentBedClaimsAllowExactlyOneActiveCheckin() throws Exception {
        User first = user("A", "13900001051", "NICKNAME_A"); User second = user("B", "13900001052", "NICKNAME_B");
        profile(first, MARKER + "NO_E1", "ENROLLED", "13800001051"); profile(second, MARKER + "NO_E2", "ENROLLED", "13800001052");
        ExecutorService pool = Executors.newFixedThreadPool(2); CountDownLatch ready = new CountDownLatch(2); CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Integer> one = pool.submit(() -> submitAtSameTime(first, ready, start)); Future<Integer> two = pool.submit(() -> submitAtSameTime(second, ready, start));
            ready.await(); start.countDown(); List<Integer> codes = Arrays.asList(one.get(), two.get());
            assertEquals(1, codes.stream().filter(value -> value == 200).count());
            assertTrue(codes.stream().anyMatch(value -> value == 400 || value == 409));
        } finally { pool.shutdownNow(); }
        assertEquals(1, checkinMapper.selectCount(Wrappers.<DormCheckin>lambdaQuery().eq(DormCheckin::getDormBedId, bed.getId()).eq(DormCheckin::getActiveFlag, 1)).intValue());
    }

    @Test
    void assetUpdateFailureRollsBackInsertedCheckinAndSensitiveLogValues() throws Exception {
        User user = user("A", "13900001061", "NICKNAME_A"); profile(user, MARKER + "NO_F", "ENROLLED", "13800001061");
        doThrow(new RuntimeException("simulated asset update failure")).when(assetSetMapper).updateById(any(AssetSet.class));
        mvc.perform(post("/api/student/dorm/check-in").header("Authorization", auth(user)).contentType(MediaType.APPLICATION_JSON).content(qrBody(qr())))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value(500));
        assertEquals(0, checkinMapper.selectCount(Wrappers.<DormCheckin>lambdaQuery().eq(DormCheckin::getUserId, user.getId()).eq(DormCheckin::getActiveFlag, 1)).intValue());
        assertEquals("AVAILABLE", assetSetMapper.selectById(assetSet.getId()).getStatus());
        for (OperateLog log : operateLogMapper.selectList(Wrappers.<OperateLog>lambdaQuery().eq(OperateLog::getUserId, user.getId()))) {
            String text = String.valueOf(log.getRequestSummary()) + String.valueOf(log.getResponseSummary()) + String.valueOf(log.getExceptionSummary());
            assertFalse(text.contains("测试甲")); assertFalse(text.contains(MARKER + "NO_F")); assertFalse(text.contains("13800001061")); assertFalse(text.contains(TOKEN));
        }
    }

    @Test
    void myDormIsPrivateMaskedAndEmptyBeforeCheckin() throws Exception {
        User first = user("A", "13900001071", "NICKNAME_A"); User second = user("B", "13900001072", "NICKNAME_B");
        profile(first, MARKER + "NO_G1", "ENROLLED", "13800001071"); profile(second, MARKER + "NO_G2", "ENROLLED", "13800001072");
        mvc.perform(get("/api/student/dorm/me").header("Authorization", auth(second))).andExpect(status().isOk()).andExpect(jsonPath("$.data").doesNotExist());
        mvc.perform(post("/api/student/dorm/check-in").header("Authorization", auth(first)).contentType(MediaType.APPLICATION_JSON).content(qrBody(qr()))).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        String body = mvc.perform(get("/api/student/dorm/me").header("Authorization", auth(first))).andExpect(status().isOk()).andExpect(jsonPath("$.data.campusName").value(campus.getCampusName()))
                .andExpect(jsonPath("$.data.buildingName").value(building.getBuildingName())).andExpect(jsonPath("$.data.roomNo").value("901")).andExpect(jsonPath("$.data.bedNo").value("01"))
                .andExpect(jsonPath("$.data.assetSetCode").value(assetSet.getAssetSetNo())).andExpect(jsonPath("$.data.maskedStudentNo").exists()).andExpect(jsonPath("$.data.maskedContactPhone").value("138****1071"))
                .andExpect(jsonPath("$.data.currentFlag").doesNotExist()).andExpect(jsonPath("$.data.activeFlag").doesNotExist()).andExpect(jsonPath("$.data.qrToken").doesNotExist()).andReturn().getResponse().getContentAsString();
        assertFalse(body.contains(TOKEN)); assertFalse(body.contains(MARKER + "NO_G1"));
        mvc.perform(get("/api/student/dorm/me").header("Authorization", auth(second))).andExpect(status().isOk()).andExpect(jsonPath("$.data").doesNotExist());
    }

    private Integer submitAtSameTime(User user, CountDownLatch ready, CountDownLatch start) throws Exception { ready.countDown(); start.await(); String body = mvc.perform(post("/api/student/dorm/check-in").header("Authorization", auth(user)).contentType(MediaType.APPLICATION_JSON).content(qrBody(qr()))).andReturn().getResponse().getContentAsString(); return JsonPath.read(body, "$.code"); }
    private User user(String suffix, String phone, String nickname) { User user = new User(); user.setPhone(phone); user.setUsername(MARKER + suffix); user.setNickname(nickname); user.setStatus(1); userMapper.insert(user); Map<String, String> session = new HashMap<String, String>(); session.put("id", String.valueOf(user.getId())); session.put("nickname", nickname); redis.opsForHash().putAll(RedisKeys.token(token(user)), session); return user; }
    private StudentProfile profile(User user, String studentNo, String studentStatus, String contactPhone) { StudentProfile profile = new StudentProfile(); profile.setUserId(user.getId()); profile.setRealName("测试甲"); profile.setStudentNo(studentNo); profile.setCampusId(campus.getId()); profile.setCollegeName("测试学院"); profile.setMajorName("测试专业"); profile.setClassName("测试班"); profile.setContactPhone(contactPhone); profile.setStudentStatus(studentStatus); profile.setCurrentFlag(1); profileMapper.insert(profile); return profile; }
    private StudentProfile currentProfile(User user) { return profileMapper.selectOne(Wrappers.<StudentProfile>lambdaQuery().eq(StudentProfile::getUserId, user.getId()).eq(StudentProfile::getCurrentFlag, 1)); }
    private String auth(User user) { return "Bearer " + token(user); }
    private String token(User user) { return MARKER + "TOKEN_" + user.getId(); }
    private String profileBody(String name, String number, String phone) { return profileBody(name, number, phone, campus.getId()); }
    private String profileBody(String name, String number, String phone, Long campusId) { return "{\"realName\":\"" + name + "\",\"studentNo\":\"" + number + "\",\"campusId\":" + campusId + ",\"collegeName\":\"测试学院\",\"majorName\":\"测试专业\",\"className\":\"测试班\",\"contactPhone\":\"" + phone + "\"}"; }
    private Campus campus(String suffix, int status) { Campus item = new Campus(); item.setCampusCode(MARKER + suffix); item.setCampusName(MARKER + suffix); item.setStatus(status); item.setSortOrder(9999); campusMapper.insert(item); return item; }
    private String qr() { return "QH-DORM-V1:" + TOKEN; }
    private String qrBody(String value) { return "{\"qrContent\":\"" + value + "\"}"; }
    private String repeat(char value, int count) { char[] values = new char[count]; Arrays.fill(values, value); return new String(values); }
    private void cleanup() {
        if (userMapper != null) for (User user : userMapper.selectList(Wrappers.<User>lambdaQuery().likeRight(User::getUsername, MARKER))) { if (redis != null) redis.delete(RedisKeys.token(token(user))); if (checkinMapper != null) checkinMapper.delete(Wrappers.<DormCheckin>lambdaQuery().eq(DormCheckin::getUserId, user.getId())); if (profileMapper != null) profileMapper.delete(Wrappers.<StudentProfile>lambdaQuery().eq(StudentProfile::getUserId, user.getId())); if (operateLogMapper != null) operateLogMapper.delete(Wrappers.<OperateLog>lambdaQuery().eq(OperateLog::getUserId, user.getId())); userMapper.deleteById(user.getId()); }
        if (buildingMapper != null) for (Building item : buildingMapper.selectList(Wrappers.<Building>lambdaQuery().likeRight(Building::getBuildingName, MARKER))) { for (DormRoom itemRoom : roomMapper.selectList(Wrappers.<DormRoom>lambdaQuery().eq(DormRoom::getBuildingId, item.getId()))) { for (DormBed itemBed : bedMapper.selectList(Wrappers.<DormBed>lambdaQuery().eq(DormBed::getDormRoomId, itemRoom.getId()))) { checkinMapper.delete(Wrappers.<DormCheckin>lambdaQuery().eq(DormCheckin::getDormBedId, itemBed.getId())); for (AssetSet itemSet : assetSetMapper.selectList(Wrappers.<AssetSet>lambdaQuery().eq(AssetSet::getDormBedId, itemBed.getId()))) { assetMapper.delete(Wrappers.<Asset>lambdaQuery().eq(Asset::getAssetSetId, itemSet.getId())); assetSetMapper.deleteById(itemSet.getId()); } bedMapper.deleteById(itemBed.getId()); } roomMapper.deleteById(itemRoom.getId()); } buildingMapper.deleteById(item.getId()); }
        if (campusMapper != null) campusMapper.delete(Wrappers.<Campus>lambdaQuery().likeRight(Campus::getCampusCode, MARKER));
    }
}
