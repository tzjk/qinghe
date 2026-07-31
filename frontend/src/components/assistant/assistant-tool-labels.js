export const assistantToolLabels = {
  get_today_promotions: '正在查询今日优惠',
  get_active_coupons: '正在查询可领取优惠券',
  search_shops: '正在查询校园商铺',
  get_shop_detail: '正在查询商铺详情',
  get_nearby_shops: '正在查询附近商铺',
  get_hot_explore_posts: '正在查询热门探店',
  get_my_profile: '正在查询个人资料',
  get_my_dorm_info: '正在查询宿舍信息',
  get_my_coupon_wallet: '正在查询你的优惠券',
  get_my_recent_orders: '正在查询最近订单',
  get_my_order_detail: '正在查询订单详情',
  get_my_default_address: '正在查询默认地址'
}

export const getAssistantToolLabel = (toolName) => assistantToolLabels[toolName] || '正在查询校园信息'
