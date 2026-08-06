import http from './http'

export const getCoupons = (params) => http.get('/coupons', { params })
export const claimCoupon = (id) => http.post(`/coupons/${id}/claim`)
export const seckillCoupon = (id) => http.post(`/coupons/${id}/seckill`)
export const getSeckillOrderStatus = (orderId) => http.get(`/coupons/seckill-orders/${orderId}/status`)
export const getMyCoupons = (params) => http.get('/coupons/mine', { params })
