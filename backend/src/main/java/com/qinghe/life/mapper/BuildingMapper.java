package com.qinghe.life.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qinghe.life.entity.Building;
import com.qinghe.life.vo.DormBuildingStats;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BuildingMapper extends BaseMapper<Building> {
    @Select("<script>SELECT r.building_id AS buildingId, COUNT(DISTINCT r.id) AS roomCount, COUNT(DISTINCT b.id) AS bedCount, COUNT(DISTINCT s.id) AS assetSetCount, COUNT(DISTINCT c.id) AS checkinHistoryCount, COUNT(DISTINCT CASE WHEN c.active_flag = 1 THEN c.id END) AS currentCheckinCount FROM qh_dorm_room r LEFT JOIN qh_dorm_bed b ON b.dorm_room_id = r.id LEFT JOIN qh_asset_set s ON s.dorm_bed_id = b.id LEFT JOIN qh_dorm_checkin c ON c.dorm_bed_id = b.id WHERE r.building_id IN <foreach collection='buildingIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> GROUP BY r.building_id</script>")
    List<DormBuildingStats> selectDormBuildingStats(@Param("buildingIds") Collection<Long> buildingIds);
}
