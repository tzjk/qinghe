package com.qinghe.life;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jayway.jsonpath.JsonPath;
import com.qinghe.life.entity.*;
import com.qinghe.life.mapper.*;
import com.qinghe.life.utils.RedisKeys;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class AdminDormBuildingIntegrationTest {
    private static final String MARKER="ADMIN_DORM_BUILDING_TEST_";
    @Autowired private MockMvc mvc; @Autowired private StringRedisTemplate redis; @Autowired private CampusMapper campuses; @Autowired private BuildingMapper buildings; @Autowired private DormRoomMapper rooms; @Autowired private DormBedMapper beds; @Autowired private AssetSetMapper sets; @Autowired private AssetMapper assets; @Autowired private DormCheckinMapper checkins; @Autowired private UserMapper users; @Autowired private StudentProfileMapper profiles; @Autowired private AdminMapper admins; @Autowired private OperateLogMapper logs;
    private Campus campus; private Building building; private Admin admin; private String adminToken;

    @BeforeEach void setUp() throws Exception { cleanup(); campus=campus(1); building=building(campus,MARKER+"BASE",MARKER+"Base",1); admin=admin(); adminToken=login(admin); }
    @AfterEach void tearDown(){ cleanup(); assertNoResidue(); }

    @Test void adminOnlyListsFiltersAndCreatesBuildingsWithAreaAndRemark() throws Exception {
        mvc.perform(get("/api/admin/dorm/buildings")).andExpect(status().isUnauthorized());
        String userToken=MARKER+"USER"; Map<String,String> session=new HashMap<String,String>(); session.put("id","9912"); session.put("username","ordinary"); redis.opsForHash().putAll(RedisKeys.token(userToken),session);
        mvc.perform(get("/api/admin/dorm/buildings").header("Authorization",bearer(userToken))).andExpect(status().isUnauthorized());
        String created=mvc.perform(post("/api/admin/dorm/buildings").header("Authorization",auth()).contentType("application/json").content(buildingBody(campus.getId()," n-a1 ",MARKER+"North","生活区",repeat('r',255),1))).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.buildingCode").value("N-A1")).andExpect(jsonPath("$.data.buildingType").value("宿舍楼")).andExpect(jsonPath("$.data.area").value("生活区")).andExpect(jsonPath("$.data.remark").value(repeat('r',255))).andReturn().getResponse().getContentAsString();
        Long id=((Number)JsonPath.read(created,"$.data.id")).longValue();
        mvc.perform(post("/api/admin/dorm/buildings").header("Authorization",auth()).contentType("application/json").content(buildingBody(campus.getId(),"N-A1",MARKER+"Other","生活区","",1))).andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/api/admin/dorm/buildings").header("Authorization",auth()).contentType("application/json").content(buildingBody(campus.getId(),"N-A2",MARKER+"North","生活区","",1))).andExpect(jsonPath("$.code").value(400));
        Campus disabled=campus(0);
        mvc.perform(post("/api/admin/dorm/buildings").header("Authorization",auth()).contentType("application/json").content(buildingBody(disabled.getId(),"D1",MARKER+"Disabled","生活区","",1))).andExpect(jsonPath("$.code").value(400));
        mvc.perform(get("/api/admin/dorm/buildings").header("Authorization",auth()).param("campusId",campus.getId().toString()).param("status","1").param("keyword","N-A1")).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1)).andExpect(jsonPath("$.data.records[0].id").value(id));
        assertFalse(logs.selectList(Wrappers.<OperateLog>lambdaQuery().eq(OperateLog::getUserId,admin.getId()).eq(OperateLog::getModule,"校区楼栋")).isEmpty());
    }

    @Test void roomUsesBuildingCampusAndResourcesFreezeTheCode() throws Exception {
        mvc.perform(put("/api/admin/dorm/buildings/{id}",building.getId()).header("Authorization",auth()).contentType("application/json").content(updateBody(" changed ",building.getBuildingName(),"生活区","first",1))).andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.buildingCode").value("CHANGED"));
        building=buildings.selectById(building.getId());
        String room=mvc.perform(post("/api/admin/dorm/rooms").header("Authorization",auth()).contentType("application/json").content("{\"campusId\":999999,\"buildingId\":"+building.getId()+",\"roomNo\":\"101\",\"floor\":\"1\",\"capacity\":4,\"status\":1}")).andExpect(jsonPath("$.code").value(200)).andReturn().getResponse().getContentAsString();
        Long roomId=((Number)JsonPath.read(room,"$.data.id")).longValue(); assertEquals(campus.getId(),rooms.selectById(roomId).getCampusId());
        mvc.perform(put("/api/admin/dorm/buildings/{id}",building.getId()).header("Authorization",auth()).contentType("application/json").content(updateBody("NEXT",MARKER+"Renamed","教学区","still editable",1))).andExpect(jsonPath("$.code").value(400)).andExpect(jsonPath("$.message").value("该楼栋已生成宿舍资源，不能修改楼栋编码。"));
        mvc.perform(put("/api/admin/dorm/buildings/{id}",building.getId()).header("Authorization",auth()).contentType("application/json").content(updateBody("CHANGED",MARKER+"Renamed","教学区","still editable",1))).andExpect(jsonPath("$.code").value(200));
        Building saved=buildings.selectById(building.getId()); assertEquals("CHANGED",saved.getBuildingCode()); assertEquals("教学区",saved.getArea()); assertEquals("still editable",saved.getRemark());
    }

    @Test void activeCheckinBlocksDisableAndAvailableDirectoryFollowsStatus() throws Exception {
        DormRoom room=room("201"); DormBed bed=bed(room,"01"); AssetSet set=assetSet(bed); DormCheckin checkin=activeCheckin(bed,set);
        mvc.perform(put("/api/admin/dorm/buildings/{id}/status",building.getId()).header("Authorization",auth()).contentType("application/json").content("{\"status\":0}")).andExpect(jsonPath("$.code").value(400));
        checkins.update(null,Wrappers.<DormCheckin>lambdaUpdate().eq(DormCheckin::getId,checkin.getId()).set(DormCheckin::getActiveFlag,null).set(DormCheckin::getCheckinStatus,"CHECKED_OUT").set(DormCheckin::getCheckoutTime,LocalDateTime.now()));
        mvc.perform(put("/api/admin/dorm/buildings/{id}/status",building.getId()).header("Authorization",auth()).contentType("application/json").content("{\"status\":0}")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/api/admin/dorm/buildings/available").header("Authorization",auth()).param("campusId",campus.getId().toString())).andExpect(jsonPath("$.data[?(@.id == " + building.getId() + ")]").isEmpty());
        assertNotNull(checkins.selectById(checkin.getId())); assertEquals(set.getQrToken(),sets.selectById(set.getId()).getQrToken());
        mvc.perform(put("/api/admin/dorm/buildings/{id}/status",building.getId()).header("Authorization",auth()).contentType("application/json").content("{\"status\":1}")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/api/admin/dorm/buildings/available").header("Authorization",auth()).param("campusId",campus.getId().toString())).andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test void onlyDormBuildingsAreListedAndDeleteIsStrictlyConditional() throws Exception {
        Building teaching=new Building(); teaching.setCampusId(campus.getId()); teaching.setBuildingCode(MARKER+"TEACH"); teaching.setBuildingName(MARKER+"Teaching"); teaching.setBuildingType("教学楼"); teaching.setStatus(1); teaching.setSortOrder(9999); buildings.insert(teaching);
        mvc.perform(get("/api/admin/dorm/buildings").header("Authorization",auth()).param("campusId",campus.getId().toString())).andExpect(jsonPath("$.data.records[?(@.id == "+teaching.getId()+")]").isEmpty()).andExpect(jsonPath("$.data.records[0].bedCount").value(0));
        Building empty=building(campus,MARKER+"EMPTY",MARKER+"Empty",1);
        mvc.perform(delete("/api/admin/dorm/buildings/{id}",empty.getId()).header("Authorization",auth())).andExpect(jsonPath("$.code").value(200)); assertNull(buildings.selectById(empty.getId()));
        DormRoom room=room("301"); DormBed bed=bed(room,"01"); AssetSet set=assetSet(bed); DormCheckin history=activeCheckin(bed,set);
        checkins.update(null,Wrappers.<DormCheckin>lambdaUpdate().eq(DormCheckin::getId,history.getId()).set(DormCheckin::getActiveFlag,null).set(DormCheckin::getCheckinStatus,"CHECKED_OUT").set(DormCheckin::getCheckoutTime,LocalDateTime.now()));
        mvc.perform(delete("/api/admin/dorm/buildings/{id}",building.getId()).header("Authorization",auth())).andExpect(jsonPath("$.code").value(400)).andExpect(jsonPath("$.message").value("该楼栋已有入住历史，不能物理删除；请停用。"));
        assertNotNull(rooms.selectById(room.getId())); assertNotNull(beds.selectById(bed.getId())); assertNotNull(sets.selectById(set.getId())); assertNotNull(checkins.selectById(history.getId()));
    }

    private Campus campus(int status){ Campus item=new Campus(); item.setCampusCode(MARKER+"C"+status+UUID.randomUUID().toString().substring(0,4)); item.setCampusName(MARKER+"Campus"+UUID.randomUUID().toString().substring(0,4)); item.setStatus(status); item.setSortOrder(9999); campuses.insert(item); return item; }
    private Building building(Campus value,String code,String name,int status){ Building item=new Building(); item.setCampusId(value.getId()); item.setBuildingCode(code); item.setBuildingName(name); item.setBuildingType("宿舍楼"); item.setArea("生活区"); item.setRemark("base"); item.setStatus(status); item.setSortOrder(9999); buildings.insert(item); return item; }
    private Admin admin(){ Admin item=new Admin(); item.setUsername(MARKER+"ADMIN"); item.setDisplayName(MARKER+"Admin"); item.setStatus(1); item.setPasswordHash(new BCryptPasswordEncoder().encode("Password123")); admins.insert(item); return item; }
    private String login(Admin item) throws Exception { String body=mvc.perform(post("/api/admin/auth/login").contentType("application/json").content("{\"username\":\""+item.getUsername()+"\",\"password\":\"Password123\"}")).andExpect(jsonPath("$.code").value(200)).andReturn().getResponse().getContentAsString(); return JsonPath.read(body,"$.data.token"); }
    private DormRoom room(String no){ DormRoom item=new DormRoom(); item.setCampusId(campus.getId()); item.setBuildingId(building.getId()); item.setRoomNo(no); item.setFloor("2"); item.setCapacity(4); item.setStatus(1); rooms.insert(item); return item; }
    private DormBed bed(DormRoom room,String no){ DormBed item=new DormBed(); item.setDormRoomId(room.getId()); item.setBedNo(no); item.setStatus(1); beds.insert(item); return item; }
    private AssetSet assetSet(DormBed bed){ AssetSet item=new AssetSet(); item.setDormBedId(bed.getId()); item.setAssetSetNo(MARKER+"SET"+bed.getId()); item.setQrToken(repeat('a',60)+String.format("%04d",bed.getId()%10000)); item.setQrStatus(1); item.setStatus("OCCUPIED"); sets.insert(item); return item; }
    private DormCheckin activeCheckin(DormBed bed,AssetSet set){ User user=new User(); user.setUsername(MARKER+"USER"); user.setPhone("139"+String.format("%08d",building.getId())); user.setNickname("Dorm test"); user.setStatus(1); users.insert(user); StudentProfile profile=new StudentProfile(); profile.setUserId(user.getId()); profile.setRealName("DormTest"); profile.setStudentNo(MARKER+"S"); profile.setCampusId(campus.getId()); profile.setCollegeName("College"); profile.setMajorName("Major"); profile.setClassName("Class"); profile.setContactPhone(user.getPhone()); profile.setStudentStatus("ENROLLED"); profile.setCurrentFlag(1); profiles.insert(profile); DormCheckin item=new DormCheckin(); item.setUserId(user.getId()); item.setStudentProfileId(profile.getId()); item.setDormBedId(bed.getId()); item.setAssetSetId(set.getId()); item.setCheckinSource("TEST"); item.setCheckinStatus("ACTIVE"); item.setActiveFlag(1); item.setStudentNoSnapshot(profile.getStudentNo()); item.setStudentStatusSnapshot("ENROLLED"); item.setCampusNameSnapshot(campus.getCampusName()); item.setBuildingCodeSnapshot(building.getBuildingCode()); item.setBuildingNameSnapshot(building.getBuildingName()); item.setRoomNoSnapshot("201"); item.setBedNoSnapshot(bed.getBedNo()); item.setAssetSetNoSnapshot(set.getAssetSetNo()); item.setCheckinTime(LocalDateTime.now()); item.setOperatorAdminId(admin.getId()); checkins.insert(item); return item; }
    private String buildingBody(Long campusId,String code,String name,String area,String remark,int status){ return "{\"campusId\":"+campusId+",\"buildingCode\":\""+code+"\",\"buildingName\":\""+name+"\",\"area\":\""+area+"\",\"remark\":\""+remark+"\",\"status\":"+status+"}"; }
    private String updateBody(String code,String name,String area,String remark,int status){ return "{\"buildingCode\":\""+code+"\",\"buildingName\":\""+name+"\",\"area\":\""+area+"\",\"remark\":\""+remark+"\",\"status\":"+status+"}"; }
    private String bearer(String value){ return "Bearer "+value; } private String auth(){ return bearer(adminToken); } private String repeat(char value,int count){ char[] chars=new char[count]; java.util.Arrays.fill(chars,value); return new String(chars); }
    private void cleanup(){ if(adminToken!=null) redis.delete(RedisKeys.adminToken(adminToken)); redis.delete(RedisKeys.token(MARKER+"USER")); for(Building item:buildings.selectList(Wrappers.<Building>lambdaQuery().likeRight(Building::getBuildingName,MARKER))){ for(DormRoom room:rooms.selectList(Wrappers.<DormRoom>lambdaQuery().eq(DormRoom::getBuildingId,item.getId()))){ for(DormBed bed:beds.selectList(Wrappers.<DormBed>lambdaQuery().eq(DormBed::getDormRoomId,room.getId()))){ checkins.delete(Wrappers.<DormCheckin>lambdaQuery().eq(DormCheckin::getDormBedId,bed.getId())); for(AssetSet set:sets.selectList(Wrappers.<AssetSet>lambdaQuery().eq(AssetSet::getDormBedId,bed.getId()))){ assets.delete(Wrappers.<Asset>lambdaQuery().eq(Asset::getAssetSetId,set.getId())); sets.deleteById(set.getId()); } beds.deleteById(bed.getId()); } rooms.deleteById(room.getId()); } buildings.deleteById(item.getId()); } for(User item:users.selectList(Wrappers.<User>lambdaQuery().likeRight(User::getUsername,MARKER))){ checkins.delete(Wrappers.<DormCheckin>lambdaQuery().eq(DormCheckin::getUserId,item.getId())); profiles.delete(Wrappers.<StudentProfile>lambdaQuery().eq(StudentProfile::getUserId,item.getId())); logs.delete(Wrappers.<OperateLog>lambdaQuery().eq(OperateLog::getUserId,item.getId())); users.deleteById(item.getId()); } for(Admin item:admins.selectList(Wrappers.<Admin>lambdaQuery().likeRight(Admin::getUsername,MARKER))){ logs.delete(Wrappers.<OperateLog>lambdaQuery().eq(OperateLog::getUserId,item.getId())); admins.deleteById(item.getId()); } campuses.delete(Wrappers.<Campus>lambdaQuery().likeRight(Campus::getCampusName,MARKER)); adminToken=null; }
    private void assertNoResidue(){ assertEquals(0,buildings.selectCount(Wrappers.<Building>lambdaQuery().likeRight(Building::getBuildingName,MARKER)).intValue()); assertEquals(0,users.selectCount(Wrappers.<User>lambdaQuery().likeRight(User::getUsername,MARKER)).intValue()); assertEquals(0,admins.selectCount(Wrappers.<Admin>lambdaQuery().likeRight(Admin::getUsername,MARKER)).intValue()); assertEquals(0,campuses.selectCount(Wrappers.<Campus>lambdaQuery().likeRight(Campus::getCampusName,MARKER)).intValue()); }
}
