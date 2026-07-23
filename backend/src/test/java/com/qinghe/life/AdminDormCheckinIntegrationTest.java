package com.qinghe.life;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jayway.jsonpath.JsonPath;
import com.qinghe.life.entity.Admin;
import com.qinghe.life.entity.Asset;
import com.qinghe.life.entity.AssetSet;
import com.qinghe.life.entity.Building;
import com.qinghe.life.entity.Campus;
import com.qinghe.life.entity.DormBed;
import com.qinghe.life.entity.DormCheckin;
import com.qinghe.life.entity.DormRoom;
import com.qinghe.life.entity.OperateLog;
import com.qinghe.life.entity.StudentProfile;
import com.qinghe.life.entity.User;
import com.qinghe.life.mapper.AdminMapper;
import com.qinghe.life.mapper.AssetMapper;
import com.qinghe.life.mapper.AssetSetMapper;
import com.qinghe.life.mapper.BuildingMapper;
import com.qinghe.life.mapper.CampusMapper;
import com.qinghe.life.mapper.DormBedMapper;
import com.qinghe.life.mapper.DormCheckinMapper;
import com.qinghe.life.mapper.DormRoomMapper;
import com.qinghe.life.mapper.OperateLogMapper;
import com.qinghe.life.mapper.StudentProfileMapper;
import com.qinghe.life.mapper.UserMapper;
import com.qinghe.life.utils.RedisKeys;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdminDormCheckinIntegrationTest {
    private static final String MARKER = "ADMIN_CHECKIN_TEST_" + UUID.randomUUID().toString().substring(0, 3);
    private static final String ADMIN_PASSWORD = "Password123";

    @Autowired private MockMvc mvc;
    @Autowired private StringRedisTemplate redis;
    @Autowired private UserMapper userMapper;
    @Autowired private StudentProfileMapper profileMapper;
    @Autowired private CampusMapper campusMapper;
    @Autowired private BuildingMapper buildingMapper;
    @Autowired private DormRoomMapper roomMapper;
    @Autowired private DormBedMapper bedMapper;
    @SpyBean private DormCheckinMapper checkinMapper;
    @SpyBean private AssetSetMapper assetSetMapper;
    @Autowired private AssetMapper assetMapper;
    @Autowired private AdminMapper adminMapper;
    @Autowired private OperateLogMapper operateLogMapper;

    private Campus campus;
    private Building building;
    private Admin firstAdmin;
    private Admin secondAdmin;
    private String firstAdminToken;
    private String secondAdminToken;
    private final Set<String> createdAdminTokens = new HashSet<String>();
    private final Set<String> createdUserTokens = new HashSet<String>();

    @BeforeEach
    void setUp() throws Exception {
        cleanup();
        campus = campus();
        building = building(campus);
        firstAdmin = admin("ONE");
        secondAdmin = admin("TWO");
        firstAdminToken = login(firstAdmin);
        secondAdminToken = login(secondAdmin);
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(checkinMapper, assetSetMapper);
        cleanup();
        assertNoResidue();
    }

    @AfterAll
    void afterAllHasNoTestResidue() {
        cleanup();
        assertNoResidue();
    }

    @Test
    void queryRequiresAdministratorAndSupportsPagingFiltersMaskingAndPrivateFields() throws Exception {
        BedAsset first = bedAsset("101", "01", 1, "AVAILABLE");
        BedAsset second = bedAsset("101", "02", 1, "AVAILABLE");
        BedAsset third = bedAsset("102", "01", 1, "AVAILABLE");
        StudentCase alpha = student("ALPHA", "13000002001", "Alpha", "S001");
        StudentCase beta = student("BETA", "13000002002", "Beta", "S002");
        StudentCase gamma = student("GAMMA", "13000002003", "Gamma", "S003");
        checkin(alpha, first, true, "ACTIVE");
        checkin(beta, second, false, "CHECKED_OUT");
        checkin(gamma, third, true, "ACTIVE");

        mvc.perform(get("/api/admin/dorm/checkins")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
        String ordinaryToken = MARKER + "ORDINARY";
        createdUserTokens.add(ordinaryToken);
        redis.opsForHash().putAll(RedisKeys.token(ordinaryToken), map("id", "999999", "nickname", "ordinary"));
        mvc.perform(get("/api/admin/dorm/checkins").header("Authorization", bearer(ordinaryToken))).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));

        String listed = mvc.perform(get("/api/admin/dorm/checkins").header("Authorization", adminAuth()).param("page", "1").param("size", "1").param("studentNo", MARKER))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.records.length()").value(1)).andExpect(jsonPath("$.data.records[0].maskedStudentNo").exists())
                .andExpect(jsonPath("$.data.records[0].maskedContactPhone").doesNotExist()).andExpect(jsonPath("$.data.records[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].qrToken").doesNotExist()).andExpect(jsonPath("$.data.records[0].activeFlag").doesNotExist()).andExpect(jsonPath("$.data.records[0].previousCheckinId").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        assertFalse(listed.contains(alpha.profile.getStudentNo()));
        assertFalse(listed.contains(alpha.phone));

        assertFilter("studentName", "Alpha", 1);
        assertFilter("studentNo", "S001", 1);
        assertFilter("campusId", String.valueOf(campus.getId()), 3);
        assertFilter("buildingId", String.valueOf(building.getId()), 3);
        assertFilter("roomNo", "101", 2);
        assertFilter("checkinStatus", "ACTIVE", 2);
        assertFilter("checkinStatus", "CHECKED_OUT", 1);
        assertFilter("studentStatus", "ENROLLED", 3);

        String detail = mvc.perform(get("/api/admin/dorm/checkins/{id}", alpha.checkin.getId()).header("Authorization", adminAuth()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.realName").value("Alpha"))
                .andExpect(jsonPath("$.data.bedNo").value("01")).andExpect(jsonPath("$.data.assetSetNo").value(first.set.getAssetSetNo()))
                .andExpect(jsonPath("$.data.maskedContactPhone").value("130****2001")).andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.qrToken").doesNotExist()).andExpect(jsonPath("$.data.activeFlag").doesNotExist()).andExpect(jsonPath("$.data.previousCheckinId").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        assertFalse(detail.contains(alpha.profile.getStudentNo()));
        assertFalse(detail.contains(alpha.phone));
        redis.delete(RedisKeys.token(ordinaryToken));
    }

    @Test
    void checkoutClosesOnlyCurrentRecordRestoresAssetAndKeepsBedDirectoryState() throws Exception {
        BedAsset source = bedAsset("201", "01", 1, "AVAILABLE");
        StudentCase student = student("CHECKOUT", "13000002011", "Checkout", "S011");
        checkin(student, source, true, "ACTIVE");

        mvc.perform(post("/api/admin/dorm/checkins/{id}/checkout", student.checkin.getId()).header("Authorization", adminAuth())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"graduation\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));

        DormCheckin old = checkinMapper.selectById(student.checkin.getId());
        assertEquals(null, old.getActiveFlag());
        assertEquals("CHECKED_OUT", old.getCheckinStatus());
        assertEquals("graduation", old.getCheckoutReason());
        assertNotNull(old.getCheckoutTime());
        assertEquals("AVAILABLE", assetSetMapper.selectById(source.set.getId()).getStatus());
        assertEquals(Integer.valueOf(1), bedMapper.selectById(source.bed.getId()).getStatus());
        mvc.perform(get("/api/student/dorm/me").header("Authorization", studentAuth(student))).andExpect(status().isOk()).andExpect(jsonPath("$.data").doesNotExist());
        assertNotNull(profileMapper.selectById(student.profile.getId()));
        StudentCase reassigned = student("CHECKOUT_REASSIGN", "13000002013", "Reassigned", "S013");
        mvc.perform(post("/api/student/dorm/check-in").header("Authorization", studentAuth(reassigned)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qrContent\":\"QH-DORM-V1:" + source.set.getQrToken() + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        assertEquals(source.bed.getId(), currentCheckin(reassigned.user).getDormBedId());

        mvc.perform(post("/api/admin/dorm/checkins/{id}/checkout", student.checkin.getId()).header("Authorization", adminAuth())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"again\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/api/admin/dorm/checkins/{id}/checkout", 999999999L).header("Authorization", adminAuth())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"missing\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/api/admin/dorm/checkins/{id}/checkout", student.checkin.getId()).header("Authorization", adminAuth())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/api/admin/dorm/checkins/{id}/checkout", student.checkin.getId()).header("Authorization", adminAuth())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"" + repeat('x', 256) + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));

        List<OperateLog> logs = operateLogMapper.selectList(Wrappers.<OperateLog>lambdaQuery().eq(OperateLog::getUserId, firstAdmin.getId()));
        assertTrue(logs.stream().anyMatch(log -> "办理退宿".equals(log.getAction())));
        for (OperateLog log : logs) assertSafeLog(log, student);
    }

    @Test
    void checkoutWriteFailuresRollbackInsteadOfPartiallyReleasingAssets() throws Exception {
        BedAsset source = bedAsset("202", "01", 1, "AVAILABLE");
        StudentCase student = student("CHECKOUT_ROLLBACK", "13000002012", "Rollback", "S012");
        checkin(student, source, true, "ACTIVE");
        Mockito.doReturn(0).when(assetSetMapper).updateById(any(AssetSet.class));
        mvc.perform(post("/api/admin/dorm/checkins/{id}/checkout", student.checkin.getId()).header("Authorization", adminAuth())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"simulate\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        assertCurrentAndOccupied(student, source);
        Mockito.reset(assetSetMapper);

        Mockito.doReturn(0).when(checkinMapper).update(Mockito.isNull(), Mockito.any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        mvc.perform(post("/api/admin/dorm/checkins/{id}/checkout", student.checkin.getId()).header("Authorization", adminAuth())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"simulate\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        assertCurrentAndOccupied(student, source);
    }

    @Test
    void transferPreservesHistoryChangesAssetsAndUpdatesStudentDormView() throws Exception {
        BedAsset source = bedAsset("301", "01", 1, "AVAILABLE");
        BedAsset target = bedAsset("302", "02", 1, "AVAILABLE");
        StudentCase student = student("TRANSFER", "13000002021", "Transfer", "S021");
        checkin(student, source, true, "ACTIVE");

        mvc.perform(post("/api/admin/dorm/checkins/{id}/transfer", student.checkin.getId()).header("Authorization", adminAuth())
                        .contentType(MediaType.APPLICATION_JSON).content(transferBody(target.bed.getId(), "room change")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));

        DormCheckin old = checkinMapper.selectById(student.checkin.getId());
        DormCheckin current = currentCheckin(student.user);
        assertEquals(null, old.getActiveFlag());
        assertEquals("TRANSFERRED", old.getCheckinStatus());
        assertEquals("AVAILABLE", assetSetMapper.selectById(source.set.getId()).getStatus());
        assertEquals("OCCUPIED", assetSetMapper.selectById(target.set.getId()).getStatus());
        assertEquals(target.bed.getId(), current.getDormBedId());
        assertEquals(target.set.getId(), current.getAssetSetId());
        assertEquals(old.getId(), current.getPreviousCheckinId());
        assertEquals(student.user.getId(), current.getUserId());
        assertEquals("ADMIN_TRANSFER", current.getCheckinSource());
        assertEquals(Integer.valueOf(1), current.getActiveFlag());
        assertNotNull(current.getCheckinTime());

        mvc.perform(get("/api/student/dorm/me").header("Authorization", studentAuth(student))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roomNo").value("302")).andExpect(jsonPath("$.data.bedNo").value("02"))
                .andExpect(jsonPath("$.data.assetSetCode").value(target.set.getAssetSetNo()));
        assertEquals(0, checkinMapper.selectCount(Wrappers.<DormCheckin>lambdaQuery().eq(DormCheckin::getDormBedId, source.bed.getId()).eq(DormCheckin::getActiveFlag, 1)).intValue());
        assertEquals(Integer.valueOf(1), bedMapper.selectById(source.bed.getId()).getStatus());
    }

    @Test
    void transferRejectsInvalidTargetAndInactiveSourceWithoutChangingCurrentState() throws Exception {
        BedAsset source = bedAsset("401", "01", 1, "AVAILABLE");
        BedAsset target = bedAsset("402", "02", 1, "AVAILABLE");
        StudentCase student = student("TRANSFER_REJECT", "13000002031", "Reject", "S031");
        checkin(student, source, true, "ACTIVE");

        assertTransferCode(student.checkin.getId(), source.bed.getId(), "same", 400);
        assertTransferCode(student.checkin.getId(), 999999999L, "missing", 404);
        target.bed.setStatus(0); bedMapper.updateById(target.bed);
        assertTransferCode(student.checkin.getId(), target.bed.getId(), "disabled", 400);
        target.bed.setStatus(1); bedMapper.updateById(target.bed);
        target.set.setStatus("OCCUPIED"); assetSetMapper.updateById(target.set);
        assertTransferCode(student.checkin.getId(), target.bed.getId(), "asset", 400);
        target.set.setStatus("AVAILABLE"); assetSetMapper.updateById(target.set);
        StudentCase occupier = student("TARGET_OCCUPIER", "13000002032", "Occupier", "S032");
        checkin(occupier, target, true, "ACTIVE");
        assertTransferCode(student.checkin.getId(), target.bed.getId(), "occupied", 400);
        assertTransferCode(student.checkin.getId(), target.bed.getId(), "", 400);
        assertTransferCode(student.checkin.getId(), target.bed.getId(), repeat('x', 256), 400);
        assertCurrentAndOccupied(student, source);
    }

    @Test
    void concurrentTransfersAllowOneTargetWinnerAndRetainNoPartialLoser() throws Exception {
        BedAsset sourceOne = bedAsset("501", "01", 1, "AVAILABLE");
        BedAsset sourceTwo = bedAsset("501", "02", 1, "AVAILABLE");
        BedAsset target = bedAsset("502", "01", 1, "AVAILABLE");
        StudentCase one = student("CONCURRENT_ONE", "13000002041", "ConcurrentOne", "S041");
        StudentCase two = student("CONCURRENT_TWO", "13000002042", "ConcurrentTwo", "S042");
        checkin(one, sourceOne, true, "ACTIVE");
        checkin(two, sourceTwo, true, "ACTIVE");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Integer> first = pool.submit(() -> transferAtSameTime(one.checkin.getId(), firstAdminToken, target.bed.getId(), ready, start));
            Future<Integer> second = pool.submit(() -> transferAtSameTime(two.checkin.getId(), secondAdminToken, target.bed.getId(), ready, start));
            ready.await();
            start.countDown();
            List<Integer> codes = Arrays.asList(first.get(), second.get());
            assertEquals(1, codes.stream().filter(code -> code == 200).count());
            assertTrue(codes.stream().anyMatch(code -> code == 400 || code == 409));
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, checkinMapper.selectCount(Wrappers.<DormCheckin>lambdaQuery().eq(DormCheckin::getDormBedId, target.bed.getId()).eq(DormCheckin::getActiveFlag, 1)).intValue());
        assertEquals("OCCUPIED", assetSetMapper.selectById(target.set.getId()).getStatus());
    }

    @Test
    void transferInsertAndTargetAssetWriteFailureRollbackTheEntireOperation() throws Exception {
        BedAsset source = bedAsset("601", "01", 1, "AVAILABLE");
        BedAsset target = bedAsset("602", "01", 1, "AVAILABLE");
        StudentCase student = student("TRANSFER_ROLLBACK", "13000002051", "TransferRollback", "S051");
        checkin(student, source, true, "ACTIVE");

        Mockito.doThrow(new org.springframework.dao.DuplicateKeyException("active bed unique")).when(checkinMapper).insert(any(DormCheckin.class));
        mvc.perform(post("/api/admin/dorm/checkins/{id}/transfer", student.checkin.getId()).header("Authorization", adminAuth())
                        .contentType(MediaType.APPLICATION_JSON).content(transferBody(target.bed.getId(), "insert failure")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(409));
        assertCurrentAndOccupied(student, source);
        assertEquals("AVAILABLE", assetSetMapper.selectById(target.set.getId()).getStatus());
        Mockito.reset(checkinMapper);

        AtomicInteger assetUpdates = new AtomicInteger();
        Mockito.doAnswer(invocation -> { if (assetUpdates.incrementAndGet() == 2) throw new RuntimeException("target asset update failure"); return 1; }).when(assetSetMapper).updateById(any(AssetSet.class));
        mvc.perform(post("/api/admin/dorm/checkins/{id}/transfer", student.checkin.getId()).header("Authorization", adminAuth())
                        .contentType(MediaType.APPLICATION_JSON).content(transferBody(target.bed.getId(), "asset failure")))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value(500));
        assertCurrentAndOccupied(student, source);
        assertEquals("AVAILABLE", assetSetMapper.selectById(target.set.getId()).getStatus());
    }

    private void assertFilter(String key, String value, int total) throws Exception {
        mvc.perform(get("/api/admin/dorm/checkins").header("Authorization", adminAuth()).param("campusId", String.valueOf(campus.getId())).param(key, value))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.total").value(total));
    }

    private void assertTransferCode(Long checkinId, Long targetBedId, String reason, int code) throws Exception {
        mvc.perform(post("/api/admin/dorm/checkins/{id}/transfer", checkinId).header("Authorization", adminAuth())
                        .contentType(MediaType.APPLICATION_JSON).content(transferBody(targetBedId, reason)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(code));
    }

    private Integer transferAtSameTime(Long checkinId, String token, Long targetBedId, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        String response = mvc.perform(MockMvcRequestBuilders.post("/api/admin/dorm/checkins/{id}/transfer", checkinId).header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(transferBody(targetBedId, "concurrent")))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.code");
    }

    private Campus campus() {
        Campus value = new Campus(); value.setCampusCode(MARKER + "C"); value.setCampusName(MARKER + "Campus"); value.setStatus(1); value.setSortOrder(9999); campusMapper.insert(value); return value;
    }

    private Building building(Campus value) {
        Building item = new Building(); item.setCampusId(value.getId()); item.setBuildingCode(MARKER + "B"); item.setBuildingName(MARKER + "Building"); item.setArea("test"); item.setBuildingType("dorm"); item.setStatus(1); item.setSortOrder(9999); buildingMapper.insert(item); return item;
    }

    private Admin admin(String suffix) {
        Admin item = new Admin(); item.setUsername(MARKER + "ADMIN_" + suffix); item.setDisplayName(MARKER + "Admin_" + suffix); item.setStatus(1); item.setPasswordHash(new BCryptPasswordEncoder().encode(ADMIN_PASSWORD)); adminMapper.insert(item); return item;
    }

    private String login(Admin item) throws Exception {
        String body = mvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + item.getUsername() + "\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.data.token");
        createdAdminTokens.add(token);
        return token;
    }

    private BedAsset bedAsset(String roomNo, String bedNo, int bedStatus, String setStatus) {
        DormRoom room = roomMapper.selectOne(Wrappers.<DormRoom>lambdaQuery().eq(DormRoom::getBuildingId, building.getId()).eq(DormRoom::getRoomNo, roomNo));
        if (room == null) { room = new DormRoom(); room.setCampusId(campus.getId()); room.setBuildingId(building.getId()); room.setRoomNo(roomNo); room.setFloor("1"); room.setCapacity(4); room.setStatus(1); roomMapper.insert(room); }
        DormBed bed = new DormBed(); bed.setDormRoomId(room.getId()); bed.setBedNo(bedNo); bed.setStatus(bedStatus); bedMapper.insert(bed);
        AssetSet set = new AssetSet(); set.setDormBedId(bed.getId()); set.setAssetSetNo(MARKER + "SET_" + roomNo + "_" + bedNo); set.setQrToken(token(set.getAssetSetNo())); set.setQrStatus(1); set.setStatus(setStatus); assetSetMapper.insert(set);
        for (String type : Arrays.asList("BED", "BED_BOARD", "DESK", "WARDROBE", "STOOL")) { Asset asset = new Asset(); asset.setAssetSetId(set.getId()); asset.setAssetType(type); asset.setAssetName(type); asset.setAssetStatus("NORMAL"); assetMapper.insert(asset); }
        return new BedAsset(room, bed, set);
    }

    private StudentCase student(String suffix, String phone, String realName, String studentNoSuffix) {
        User user = new User(); user.setUsername(MARKER + "U" + Integer.toHexString(suffix.hashCode())); user.setPhone(phone); user.setNickname(MARKER + "Nick_" + suffix); user.setStatus(1); userMapper.insert(user);
        String token = MARKER + "USER_TOKEN_" + user.getId(); createdUserTokens.add(token); redis.opsForHash().putAll(RedisKeys.token(token), map("id", String.valueOf(user.getId()), "nickname", user.getNickname()));
        StudentProfile profile = new StudentProfile(); profile.setUserId(user.getId()); profile.setRealName(realName); profile.setStudentNo(MARKER + studentNoSuffix); profile.setCampusId(campus.getId()); profile.setCollegeName("College"); profile.setMajorName("Major"); profile.setClassName("Class"); profile.setContactPhone(phone); profile.setStudentStatus("ENROLLED"); profile.setCurrentFlag(1); profileMapper.insert(profile);
        return new StudentCase(user, profile, phone, token);
    }

    private void checkin(StudentCase student, BedAsset location, boolean active, String status) {
        DormCheckin item = new DormCheckin(); item.setUserId(student.user.getId()); item.setStudentProfileId(student.profile.getId()); item.setDormBedId(location.bed.getId()); item.setAssetSetId(location.set.getId()); item.setCheckinSource("TEST"); item.setCheckinStatus(status); item.setActiveFlag(active ? 1 : null); item.setStudentNoSnapshot(student.profile.getStudentNo()); item.setStudentStatusSnapshot(student.profile.getStudentStatus()); item.setCampusNameSnapshot(campus.getCampusName()); item.setBuildingCodeSnapshot(building.getBuildingCode()); item.setBuildingNameSnapshot(building.getBuildingName()); item.setRoomNoSnapshot(location.room.getRoomNo()); item.setBedNoSnapshot(location.bed.getBedNo()); item.setAssetSetNoSnapshot(location.set.getAssetSetNo()); item.setCheckinTime(LocalDateTime.now()); if (!active) { item.setCheckoutTime(LocalDateTime.now()); item.setCheckoutReason("history"); }
        checkinMapper.insert(item); if (!active) { checkinMapper.update(null, Wrappers.<DormCheckin>lambdaUpdate().eq(DormCheckin::getId, item.getId()).set(DormCheckin::getActiveFlag, null)); item.setActiveFlag(null); } location.set.setStatus(active ? "OCCUPIED" : "AVAILABLE"); assetSetMapper.updateById(location.set); student.checkin = item;
    }

    private DormCheckin currentCheckin(User user) { return checkinMapper.selectOne(Wrappers.<DormCheckin>lambdaQuery().eq(DormCheckin::getUserId, user.getId()).eq(DormCheckin::getActiveFlag, 1)); }
    private void assertCurrentAndOccupied(StudentCase student, BedAsset source) { DormCheckin current = currentCheckin(student.user); assertNotNull(current); assertEquals(student.checkin.getId(), current.getId()); assertEquals("OCCUPIED", assetSetMapper.selectById(source.set.getId()).getStatus()); }
    private void assertSafeLog(OperateLog log, StudentCase student) { String value = String.valueOf(log.getRequestSummary()) + String.valueOf(log.getResponseSummary()) + String.valueOf(log.getExceptionSummary()); assertFalse(value.contains(student.profile.getRealName())); assertFalse(value.contains(student.profile.getStudentNo())); assertFalse(value.contains(student.phone)); assertFalse(value.contains(firstAdminToken)); }
    private String adminAuth() { return bearer(firstAdminToken); }
    private String studentAuth(StudentCase student) { return bearer(student.token); }
    private String bearer(String token) { return "Bearer " + token; }
    private String transferBody(Long targetBedId, String reason) { return "{\"targetBedId\":" + targetBedId + ",\"reason\":\"" + reason + "\"}"; }
    private String token(String seed) { String normalized = Integer.toHexString(seed.hashCode()); return (repeat('0', 64) + normalized).substring(normalized.length()); }
    private String repeat(char value, int count) { char[] values = new char[count]; Arrays.fill(values, value); return new String(values); }
    private Map<String, String> map(String firstKey, String firstValue, String secondKey, String secondValue) { Map<String, String> values = new HashMap<String, String>(); values.put(firstKey, firstValue); values.put(secondKey, secondValue); return values; }

    private void cleanup() {
        if (redis != null) { for (String token : createdAdminTokens) redis.delete(RedisKeys.adminToken(token)); for (String token : createdUserTokens) redis.delete(RedisKeys.token(token)); }
        if (userMapper != null) for (User item : userMapper.selectList(Wrappers.<User>lambdaQuery().likeRight(User::getUsername, MARKER))) { if (checkinMapper != null) { checkinMapper.update(null, Wrappers.<DormCheckin>lambdaUpdate().eq(DormCheckin::getUserId, item.getId()).set(DormCheckin::getPreviousCheckinId, null)); checkinMapper.delete(Wrappers.<DormCheckin>lambdaQuery().eq(DormCheckin::getUserId, item.getId())); } if (profileMapper != null) profileMapper.delete(Wrappers.<StudentProfile>lambdaQuery().eq(StudentProfile::getUserId, item.getId())); userMapper.deleteById(item.getId()); }
        if (buildingMapper != null) for (Building item : buildingMapper.selectList(Wrappers.<Building>lambdaQuery().likeRight(Building::getBuildingName, MARKER))) { for (DormRoom room : roomMapper.selectList(Wrappers.<DormRoom>lambdaQuery().eq(DormRoom::getBuildingId, item.getId()))) { for (DormBed bed : bedMapper.selectList(Wrappers.<DormBed>lambdaQuery().eq(DormBed::getDormRoomId, room.getId()))) { checkinMapper.delete(Wrappers.<DormCheckin>lambdaQuery().eq(DormCheckin::getDormBedId, bed.getId())); for (AssetSet set : assetSetMapper.selectList(Wrappers.<AssetSet>lambdaQuery().eq(AssetSet::getDormBedId, bed.getId()))) { assetMapper.delete(Wrappers.<Asset>lambdaQuery().eq(Asset::getAssetSetId, set.getId())); assetSetMapper.deleteById(set.getId()); } bedMapper.deleteById(bed.getId()); } roomMapper.deleteById(room.getId()); } buildingMapper.deleteById(item.getId()); }
        if (adminMapper != null) for (Admin item : adminMapper.selectList(Wrappers.<Admin>lambdaQuery().likeRight(Admin::getUsername, MARKER))) { if (operateLogMapper != null) operateLogMapper.delete(Wrappers.<OperateLog>lambdaQuery().eq(OperateLog::getUserId, item.getId())); adminMapper.deleteById(item.getId()); }
        if (campusMapper != null) campusMapper.delete(Wrappers.<Campus>lambdaQuery().likeRight(Campus::getCampusName, MARKER));
        firstAdminToken = null; secondAdminToken = null; createdAdminTokens.clear(); createdUserTokens.clear();
    }

    private void assertNoResidue() {
        assertEquals(0, userMapper.selectCount(Wrappers.<User>lambdaQuery().likeRight(User::getUsername, MARKER)).intValue());
        assertEquals(0, profileMapper.selectCount(Wrappers.<StudentProfile>lambdaQuery().likeRight(StudentProfile::getStudentNo, MARKER)).intValue());
        assertEquals(0, checkinMapper.selectCount(Wrappers.<DormCheckin>lambdaQuery().likeRight(DormCheckin::getStudentNoSnapshot, MARKER)).intValue());
        assertEquals(0, assetSetMapper.selectCount(Wrappers.<AssetSet>lambdaQuery().likeRight(AssetSet::getAssetSetNo, MARKER)).intValue());
        assertEquals(0, buildingMapper.selectCount(Wrappers.<Building>lambdaQuery().likeRight(Building::getBuildingName, MARKER)).intValue());
        assertEquals(0, adminMapper.selectCount(Wrappers.<Admin>lambdaQuery().likeRight(Admin::getUsername, MARKER)).intValue());
        assertTrue(redis.keys(RedisKeys.token(MARKER + "*")).isEmpty());
    }

    private static final class BedAsset { private final DormRoom room; private final DormBed bed; private final AssetSet set; private BedAsset(DormRoom room, DormBed bed, AssetSet set) { this.room = room; this.bed = bed; this.set = set; } }
    private static final class StudentCase { private final User user; private final StudentProfile profile; private final String phone; private final String token; private DormCheckin checkin; private StudentCase(User user, StudentProfile profile, String phone, String token) { this.user = user; this.profile = profile; this.phone = phone; this.token = token; } }
}
