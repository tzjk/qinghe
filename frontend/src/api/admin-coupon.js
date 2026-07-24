import http from './http'

export const getAdminCoupons = (params) => http.get('/admin/coupons', { params })
export const createAdminCoupon = (data) => http.post('/admin/coupons', data)
export const updateAdminCoupon = (id, data) => http.put(`/admin/coupons/${id}`, data)
export const updateAdminCouponStatus = (id, data) => http.put(`/admin/coupons/${id}/status`, data)
export const getAdminCouponStats = (id) => http.get(`/admin/coupons/${id}/stats`)
