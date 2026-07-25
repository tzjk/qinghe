import http from './http'

export const getBusinessOverview = () => http.get('/admin/reports/overview')
export const getBusinessTrend = (params) => http.get('/admin/reports/trend', { params })
export const getShopSalesRanking = (params) => http.get('/admin/reports/shop-ranking', { params })
export const getGoodsSalesRanking = (params) => http.get('/admin/reports/goods-ranking', { params })
export const getCouponUsageSummary = (params) => http.get('/admin/reports/coupon-summary', { params })
