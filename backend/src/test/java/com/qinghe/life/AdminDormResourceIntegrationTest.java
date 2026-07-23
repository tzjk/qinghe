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
class AdminDormResourceIntegrationTest {
    private static final String MARKER="ADMIN_DORM_RESOURCE_TEST_";
    @Autowired private MockMvc mvc; @Autowired private StringRedisTemplate redis; @Autowired private CampusMapper campuses; @Autowired private BuildingMapper buildings; @Autowired private DormRoomMapper rooms; @Autowired private DormBedMapper beds; @Autowired private AssetSetMapper sets; @Autowired private AssetMapper assets; @Autowired private DormCheckinMapper checkins; @Autowired private UserMapper users; @Autowired private StudentProfileMapper profiles; @Autowired private AdminMapper admins; @Autowired private OperateLogMapper logs;
    private Campus campus; private Building dorm; private Admin admin; private String token;

    @BeforeEach void setUp() throws Exception { cleanup(); campus=new Campus(); campus.setCampusCode(MARKER+"C"+UUID.randomUUID().toString().substring(0,4)); campus.setCampusName(MARKER+"Campus"); campus.setStatus(1); campus.setSortOrder(9999); campuses.insert(campus); dorm=building("宿舍楼",1); admin=new Admin(); admin.setUsername(MARKER+"ADMIN"); admin.setDisplayName(MARKER+"Admin"); admin.setStatus(1); admin.setPasswordHash(new BCryptPasswordEncoder().encode("Password123")); admins.insert(admin); String body=mvc.perform(post("/api/admin/auth/login").contentType("application/json").content("{\"username\":\""+admin.getUsername()+"\",\"password\":\"Password123\"}")).andExpect(jsonPath("$.code").value(200)).andReturn().getResponse().getContentAsString(); token=JsonPath.read(body,"$.data.token"); }
    @AfterEach void tearDown(){ cleanup(); assertEquals(0,buildings.selectCount(Wrappers.<Building>lambdaQuery().likeRight(Building::getBuildingName,MARKER)).intValue()); }

    @Test void filtersNonDormBuildingsAndSafelyCorrectsEmptyUataRoom() throws Exception {
        Building teaching=building("教学楼",1); String list=mvc.perform(get("/api/admin/dorm/buildings").header("Authorization",auth()).param("campusId",campus.getId().toString())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(); assertFalse(list.contains(teaching.getBuildingName()));
        Long room=id(mvc.perform(post("/api/admin/dorm/rooms").header("Authorization",auth()).contentType("application/json").content(roomBody("UATA",1))).andExpect(jsonPath("$.code").value(200)).andReturn().getResponse().getContentAsString());
        mvc.perform(put("/api/admin/dorm/rooms/{id}",room).header("Authorization",auth()).contentType("application/json").content(roomBody("101",1))).andExpect(jsonPath("$.data.roomNo").value("101"));
        mvc.perform(delete("/api/admin/dorm/rooms/{id}",room).header("Authorization",auth())).andExpect(jsonPath("$.code").value(200));
    }

    @Test void managesBedsAndKeepsAssetSetNumbersStable() throws Exception {
        Long room=id(mvc.perform(post("/api/admin/dorm/rooms").header("Authorization",auth()).contentType("application/json").content(roomBody("201",1))).andReturn().getResponse().getContentAsString());
        Long removable=id(mvc.perform(post("/api/admin/dorm/rooms/{id}/beds",room).header("Authorization",auth()).contentType("application/json").content("{\"bedNo\":\"01\",\"remark\":\"first\"}")).andReturn().getResponse().getContentAsString());
        mvc.perform(put("/api/admin/dorm/beds/{id}",removable).header("Authorization",auth()).contentType("application/json").content("{\"bedNo\":\"02\",\"status\":0,\"remark\":\"edited\"}")).andExpect(jsonPath("$.data.bedNo").value("02"));
        mvc.perform(put("/api/admin/dorm/beds/{id}/status",removable).header("Authorization",auth()).contentType("application/json").content("{\"status\":1}")).andExpect(jsonPath("$.code").value(200)); mvc.perform(delete("/api/admin/dorm/beds/{id}",removable).header("Authorization",auth())).andExpect(jsonPath("$.code").value(200));
        Long bed=id(mvc.perform(post("/api/admin/dorm/rooms/{id}/beds",room).header("Authorization",auth()).contentType("application/json").content("{\"bedNo\":\"01\"}")).andReturn().getResponse().getContentAsString());
        String first=mvc.perform(post("/api/admin/dorm/beds/{id}/asset-set",bed).header("Authorization",auth())).andExpect(jsonPath("$.data.assets.length()").value(5)).andReturn().getResponse().getContentAsString(); Long set=id(first); mvc.perform(post("/api/admin/dorm/beds/{id}/asset-set",bed).header("Authorization",auth())).andExpect(jsonPath("$.data.id").value(set));
        assertEquals(5,assets.selectCount(Wrappers.<Asset>lambdaQuery().eq(Asset::getAssetSetId,set)).intValue()); assertEquals(5,assets.selectList(Wrappers.<Asset>lambdaQuery().eq(Asset::getAssetSetId,set)).stream().map(Asset::getAssetType).distinct().count());
        mvc.perform(put("/api/admin/dorm/rooms/{id}",room).header("Authorization",auth()).contentType("application/json").content(roomBody("202",1))).andExpect(jsonPath("$.code").value(400));
        mvc.perform(put("/api/admin/dorm/beds/{id}",bed).header("Authorization",auth()).contentType("application/json").content("{\"bedNo\":\"03\",\"status\":1}")).andExpect(jsonPath("$.code").value(400)); mvc.perform(delete("/api/admin/dorm/beds/{id}",bed).header("Authorization",auth())).andExpect(jsonPath("$.code").value(400));
        Asset asset=assets.selectOne(Wrappers.<Asset>lambdaQuery().eq(Asset::getAssetSetId,set).eq(Asset::getAssetType,"BED")); mvc.perform(put("/api/admin/dorm/assets/{id}",asset.getId()).header("Authorization",auth()).contentType("application/json").content("{\"assetStatus\":\"REPAIR\",\"remark\":\"test\"}")).andExpect(jsonPath("$.data.assetStatus").value("REPAIR")); mvc.perform(get("/api/admin/dorm/asset-sets/{id}/qrcode",set).header("Authorization",auth())).andExpect(content().contentType("image/png"));
    }

    @Test void activeCheckinBlocksRoomAndBedDisableOrDelete() throws Exception {
        DormRoom room=new DormRoom(); room.setCampusId(campus.getId()); room.setBuildingId(dorm.getId()); room.setRoomNo("301"); room.setFloor("3"); room.setCapacity(4); room.setStatus(1); rooms.insert(room); DormBed bed=new DormBed(); bed.setDormRoomId(room.getId()); bed.setBedNo("01"); bed.setStatus(1); beds.insert(bed); AssetSet set=new AssetSet(); set.setDormBedId(bed.getId()); set.setAssetSetNo(MARKER+"SET"); set.setQrToken(repeat('a',64)); set.setQrStatus(1); set.setStatus("OCCUPIED"); sets.insert(set); activeCheckin(room,bed,set);
        mvc.perform(put("/api/admin/dorm/rooms/{id}/status",room.getId()).header("Authorization",auth()).contentType("application/json").content("{\"status\":0}")).andExpect(jsonPath("$.code").value(400)); mvc.perform(delete("/api/admin/dorm/rooms/{id}",room.getId()).header("Authorization",auth())).andExpect(jsonPath("$.code").value(400)); mvc.perform(put("/api/admin/dorm/beds/{id}/status",bed.getId()).header("Authorization",auth()).contentType("application/json").content("{\"status\":0}")).andExpect(jsonPath("$.code").value(400)); mvc.perform(delete("/api/admin/dorm/beds/{id}",bed.getId()).header("Authorization",auth())).andExpect(jsonPath("$.code").value(400));
    }

    private Building building(String type,int status){ Building item=new Building(); item.setCampusId(campus.getId()); item.setBuildingCode(MARKER+UUID.randomUUID().toString().substring(0,5)); item.setBuildingName(MARKER+type+UUID.randomUUID().toString().substring(0,4)); item.setBuildingType(type); item.setStatus(status); item.setSortOrder(9999); buildings.insert(item); return item; }
    private void activeCheckin(DormRoom room,DormBed bed,AssetSet set){ User user=new User(); user.setUsername(MARKER+"USER"); user.setPhone("139"+String.format("%08d",room.getId())); user.setNickname("test"); user.setStatus(1); users.insert(user); StudentProfile p=new StudentProfile(); p.setUserId(user.getId()); p.setRealName("test"); p.setStudentNo(MARKER+"S"); p.setCampusId(campus.getId()); p.setCollegeName("c"); p.setMajorName("m"); p.setClassName("c"); p.setContactPhone(user.getPhone()); p.setStudentStatus("ENROLLED"); p.setCurrentFlag(1); profiles.insert(p); DormCheckin c=new DormCheckin(); c.setUserId(user.getId()); c.setStudentProfileId(p.getId()); c.setDormBedId(bed.getId()); c.setAssetSetId(set.getId()); c.setCheckinSource("TEST"); c.setCheckinStatus("ACTIVE"); c.setActiveFlag(1); c.setStudentNoSnapshot(p.getStudentNo()); c.setStudentStatusSnapshot("ENROLLED"); c.setCampusNameSnapshot(campus.getCampusName()); c.setBuildingCodeSnapshot(dorm.getBuildingCode()); c.setBuildingNameSnapshot(dorm.getBuildingName()); c.setRoomNoSnapshot(room.getRoomNo()); c.setBedNoSnapshot(bed.getBedNo()); c.setAssetSetNoSnapshot(set.getAssetSetNo()); c.setCheckinTime(LocalDateTime.now()); c.setOperatorAdminId(admin.getId()); checkins.insert(c); }
    private String roomBody(String no,int status){ return "{\"buildingId\":"+dorm.getId()+",\"roomNo\":\""+no+"\",\"floor\":\"1\",\"capacity\":4,\"status\":"+status+",\"remark\":\"test\"}"; }
    private Long id(String body){ return ((Number)JsonPath.read(body,"$.data.id")).longValue(); } private String auth(){return "Bearer "+token;} private String repeat(char v,int n){char[] a=new char[n];java.util.Arrays.fill(a,v);return new String(a);}
    private void cleanup(){ if(token!=null) redis.delete(RedisKeys.adminToken(token)); for(Building b:buildings.selectList(Wrappers.<Building>lambdaQuery().likeRight(Building::getBuildingName,MARKER))){ for(DormRoom r:rooms.selectList(Wrappers.<DormRoom>lambdaQuery().eq(DormRoom::getBuildingId,b.getId()))){ for(DormBed bed:beds.selectList(Wrappers.<DormBed>lambdaQuery().eq(DormBed::getDormRoomId,r.getId()))){ checkins.delete(Wrappers.<DormCheckin>lambdaQuery().eq(DormCheckin::getDormBedId,bed.getId())); for(AssetSet s:sets.selectList(Wrappers.<AssetSet>lambdaQuery().eq(AssetSet::getDormBedId,bed.getId()))){assets.delete(Wrappers.<Asset>lambdaQuery().eq(Asset::getAssetSetId,s.getId()));sets.deleteById(s.getId());} beds.deleteById(bed.getId());} rooms.deleteById(r.getId());} buildings.deleteById(b.getId());} for(User u:users.selectList(Wrappers.<User>lambdaQuery().likeRight(User::getUsername,MARKER))){profiles.delete(Wrappers.<StudentProfile>lambdaQuery().eq(StudentProfile::getUserId,u.getId()));users.deleteById(u.getId());} for(Admin a:admins.selectList(Wrappers.<Admin>lambdaQuery().likeRight(Admin::getUsername,MARKER))){logs.delete(Wrappers.<OperateLog>lambdaQuery().eq(OperateLog::getUserId,a.getId()));admins.deleteById(a.getId());} campuses.delete(Wrappers.<Campus>lambdaQuery().likeRight(Campus::getCampusName,MARKER)); token=null; }
}
