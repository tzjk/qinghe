package com.qinghe.life;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.qinghe.life.entity.*;
import com.qinghe.life.mapper.*;
import com.qinghe.life.utils.RedisKeys;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;
import javax.imageio.ImageIO;
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
class DormAssetIntegrationTest {
    private static final String MARKER="DORM_ASSET_TEST_";
    @Autowired private MockMvc mvc; @Autowired private StringRedisTemplate redis; @Autowired private CampusMapper campusMapper; @Autowired private BuildingMapper buildingMapper; @Autowired private DormRoomMapper roomMapper; @Autowired private DormBedMapper bedMapper; @Autowired private AssetSetMapper setMapper; @Autowired private AssetMapper assetMapper; @Autowired private AdminMapper adminMapper;
    private Campus campus; private Building building; private Admin admin; private String adminToken;
    @BeforeEach void setUp() throws Exception { cleanup(); campus=new Campus(); campus.setCampusCode(MARKER+"C"); campus.setCampusName(MARKER+"Campus"); campus.setStatus(1); campus.setSortOrder(9999); campusMapper.insert(campus); building=new Building(); building.setCampusId(campus.getId()); building.setArea("Test"); building.setBuildingType("宿舍楼"); building.setBuildingName(MARKER+"Building"); building.setStatus(1); building.setSortOrder(9999); buildingMapper.insert(building); admin=new Admin(); admin.setUsername(MARKER+"ADMIN"); admin.setDisplayName(MARKER+"Admin"); admin.setPasswordHash(new BCryptPasswordEncoder().encode("Password123")); admin.setStatus(1); adminMapper.insert(admin); String body=mvc.perform(post("/api/admin/auth/login").contentType("application/json").content("{\"username\":\""+admin.getUsername()+"\",\"password\":\"Password123\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andReturn().getResponse().getContentAsString(); adminToken=body.replaceAll("(?s).*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1"); }
    @AfterEach void tearDown(){ cleanup(); }
    @Test void administratorMaintainsUniqueRoomBedAndCreatesIdempotentAssetSetWithPrivateQr() throws Exception {
        String header="Bearer "+adminToken;
        mvc.perform(put("/api/admin/dorm/buildings/{id}/code",building.getId()).header("Authorization",header).contentType("application/json").content("{\"buildingCode\":\"JA\"}")).andExpect(status().isOk());
        String roomBody=mvc.perform(post("/api/admin/dorm/rooms").header("Authorization",header).contentType("application/json").content("{\"campusId\":"+campus.getId()+",\"buildingId\":"+building.getId()+",\"roomNo\":\"101\",\"floor\":\"1\",\"capacity\":4}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Long roomId=id(roomBody);
        mvc.perform(post("/api/admin/dorm/rooms").header("Authorization",header).contentType("application/json").content("{\"campusId\":"+campus.getId()+",\"buildingId\":"+building.getId()+",\"roomNo\":\"101\",\"capacity\":4}")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(409));
        assertEquals(1,roomMapper.selectCount(Wrappers.<DormRoom>lambdaQuery().eq(DormRoom::getBuildingId,building.getId()).eq(DormRoom::getRoomNo,"101")).intValue());
        String bedBody=mvc.perform(post("/api/admin/dorm/rooms/{id}/beds",roomId).header("Authorization",header).contentType("application/json").content("{\"bedNo\":\"01\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Long bedId=id(bedBody);
        mvc.perform(post("/api/admin/dorm/rooms/{id}/beds",roomId).header("Authorization",header).contentType("application/json").content("{\"bedNo\":\"01\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(409));
        assertEquals(1,bedMapper.selectCount(Wrappers.<DormBed>lambdaQuery().eq(DormBed::getDormRoomId,roomId).eq(DormBed::getBedNo,"01")).intValue());
        String first=mvc.perform(post("/api/admin/dorm/beds/{id}/asset-set",bedId).header("Authorization",header)).andExpect(status().isOk()).andExpect(jsonPath("$.data.assetSetNo").value("JA101-01")).andExpect(jsonPath("$.data.assets.length()").value(5)).andReturn().getResponse().getContentAsString();
        Long setId=id(first);
        String second=mvc.perform(post("/api/admin/dorm/beds/{id}/asset-set",bedId).header("Authorization",header)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(setId,id(second));
        assertEquals(1,setMapper.selectCount(Wrappers.<AssetSet>lambdaQuery().eq(AssetSet::getDormBedId,bedId)).intValue());
        assertEquals(5,assetMapper.selectCount(Wrappers.<Asset>lambdaQuery().eq(Asset::getAssetSetId,setId)).intValue());
        String secondBed=mvc.perform(post("/api/admin/dorm/rooms/{id}/beds",roomId).header("Authorization",header).contentType("application/json").content("{\"bedNo\":\"02\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Long secondBedId=id(secondBed);
        mvc.perform(post("/api/admin/dorm/beds/{id}/asset-set",secondBedId).header("Authorization",header)).andExpect(status().isOk());
        AssetSet firstSet=setMapper.selectById(setId);
        AssetSet other=setMapper.selectOne(Wrappers.<AssetSet>lambdaQuery().eq(AssetSet::getDormBedId,secondBedId));
        assertNotEquals(firstSet.getQrToken(),other.getQrToken());
        assertEquals(64,firstSet.getQrToken().length());
        byte[] png=mvc.perform(get("/api/admin/dorm/asset-sets/{id}/qrcode",setId).header("Authorization",header)).andExpect(status().isOk()).andExpect(content().contentType("image/png")).andReturn().getResponse().getContentAsByteArray();
        BufferedImage image=ImageIO.read(new ByteArrayInputStream(png));
        String decoded=new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)))).getText();
        assertEquals("QH-DORM-V1:"+firstSet.getQrToken(),decoded);
        assertFalse(decoded.contains(MARKER));
    }
    @Test void regularUserTokenCannotAccessDormAdminEndpoints() throws Exception { String token=MARKER+"USER"; Map<String,String> session=new HashMap<String,String>(); session.put("id","9988"); session.put("username","ordinary"); redis.opsForHash().putAll(RedisKeys.token(token),session); mvc.perform(get("/api/admin/dorm/rooms").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401)); mvc.perform(get("/api/admin/dorm/asset-sets/{id}/qrcode",999L).header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401)); }
    private Long id(String json){ return Long.valueOf(json.replaceAll("(?s).*?\\\"data\\\":\\{\\\"id\\\":(\\d+).*", "$1")); }
    private void cleanup(){ if(adminToken!=null)redis.delete(RedisKeys.adminToken(adminToken)); redis.delete(RedisKeys.token(MARKER+"USER")); for(Admin item:adminMapper.selectList(Wrappers.<Admin>lambdaQuery().likeRight(Admin::getUsername,MARKER)))adminMapper.deleteById(item); for(Building item:buildingMapper.selectList(Wrappers.<Building>lambdaQuery().likeRight(Building::getBuildingName,MARKER))){ for(DormRoom room:roomMapper.selectList(Wrappers.<DormRoom>lambdaQuery().eq(DormRoom::getBuildingId,item.getId()))){ for(DormBed bed:bedMapper.selectList(Wrappers.<DormBed>lambdaQuery().eq(DormBed::getDormRoomId,room.getId()))){ for(AssetSet set:setMapper.selectList(Wrappers.<AssetSet>lambdaQuery().eq(AssetSet::getDormBedId,bed.getId()))){ assetMapper.delete(Wrappers.<Asset>lambdaQuery().eq(Asset::getAssetSetId,set.getId())); setMapper.deleteById(set.getId()); } bedMapper.deleteById(bed.getId()); } roomMapper.deleteById(room.getId()); } buildingMapper.deleteById(item.getId()); } campusMapper.delete(Wrappers.<Campus>lambdaQuery().likeRight(Campus::getCampusName,MARKER)); adminToken=null; }
}
