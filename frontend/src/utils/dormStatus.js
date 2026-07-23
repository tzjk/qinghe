const maps = {
  buildingType: { DORM: '宿舍楼', '宿舍楼': '宿舍楼', '教学楼': '教学楼', '办公楼': '办公楼', '实验楼': '实验楼', '图书馆': '图书馆', '其他': '其他' },
  enabled: { 1: '启用', 0: '停用', true: '启用', false: '停用' },
  checkin: { true: '已入住', false: '空闲' },
  assetSet: { AVAILABLE: '可用', OCCUPIED: '使用中', MAINTENANCE: '维护中', RETIRED: '已停用' },
  qr: { 1: '启用', 0: '停用' },
  assetType: { BED: '床', BED_BOARD: '床板', DESK: '书桌', WARDROBE: '衣柜', STOOL: '凳子' },
  assetStatus: { NORMAL: '正常', REPAIR: '维修中', SCRAPPED: '已报废' },
  health: { NORMAL: '资产正常', REPAIR: '存在维修资产', SCRAPPED: '存在报废资产', INCOMPLETE: '资产不完整' }
}
export const dormText = (type, value) => maps[type]?.[String(value)] ?? '未知状态'
export const dormTagType = (type, value) => {
  if (type === 'assetStatus' || type === 'health') return value === 'NORMAL' ? 'success' : value === 'REPAIR' ? 'warning' : 'danger'
  if (type === 'assetSet') return value === 'AVAILABLE' ? 'success' : value === 'OCCUPIED' ? 'warning' : 'info'
  return value === 1 || value === true ? 'success' : 'info'
}
