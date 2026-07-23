-- 青禾校园生活服务系统 V1.0：校园目录初始演示数据（仅供 DataGrip 人工审核并执行）
-- 前提：qh_campus、qh_building 已存在，且由 campus_address_increment.sql 创建的约束有效。
-- 安全性：只插入缺失的校区和楼栋；不 UPDATE、DELETE、TRUNCATE、DROP 或修改现有记录。
-- 执行前请先确认当前环境允许写入演示目录数据；本项目不会自动导入此文件。

START TRANSACTION;

INSERT INTO qh_campus (campus_code, campus_name, status, sort_order)
SELECT 'QH_MAIN', '青禾主校区', 1, 10
WHERE NOT EXISTS (
    SELECT 1 FROM qh_campus WHERE campus_code = 'QH_MAIN'
)
AND NOT EXISTS (
    SELECT 1 FROM qh_campus WHERE campus_name = '青禾主校区'
);

INSERT INTO qh_building (campus_id, area, building_type, building_name, status, sort_order)
SELECT c.id, '生活区', '宿舍楼', '松园1号宿舍楼', 1, 10
FROM qh_campus c
WHERE c.campus_code = 'QH_MAIN'
  AND NOT EXISTS (
      SELECT 1 FROM qh_building b
      WHERE b.campus_id = c.id AND b.building_name = '松园1号宿舍楼'
  );

INSERT INTO qh_building (campus_id, area, building_type, building_name, status, sort_order)
SELECT c.id, '生活区', '宿舍楼', '桂园2号宿舍楼', 1, 20
FROM qh_campus c
WHERE c.campus_code = 'QH_MAIN'
  AND NOT EXISTS (
      SELECT 1 FROM qh_building b
      WHERE b.campus_id = c.id AND b.building_name = '桂园2号宿舍楼'
  );

INSERT INTO qh_building (campus_id, area, building_type, building_name, status, sort_order)
SELECT c.id, '教学区', '教学楼', '知行教学楼', 1, 30
FROM qh_campus c
WHERE c.campus_code = 'QH_MAIN'
  AND NOT EXISTS (
      SELECT 1 FROM qh_building b
      WHERE b.campus_id = c.id AND b.building_name = '知行教学楼'
  );

INSERT INTO qh_building (campus_id, area, building_type, building_name, status, sort_order)
SELECT c.id, '公共服务区', '图书馆', '明理图书馆', 1, 40
FROM qh_campus c
WHERE c.campus_code = 'QH_MAIN'
  AND NOT EXISTS (
      SELECT 1 FROM qh_building b
      WHERE b.campus_id = c.id AND b.building_name = '明理图书馆'
  );

COMMIT;

-- 人工执行后只读复核：应至少返回 1 个启用校区和 4 个启用楼栋。
SELECT c.id, c.campus_code, c.campus_name, c.status,
       b.id AS building_id, b.area, b.building_type, b.building_name, b.status AS building_status
FROM qh_campus c
LEFT JOIN qh_building b ON b.campus_id = c.id
WHERE c.campus_code = 'QH_MAIN'
ORDER BY c.sort_order, b.sort_order, b.id;
