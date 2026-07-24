import http from './http'

export const getCoupons = (params) => http.get('/coupons', { params })
export const claimCoupon = (id) => http.post(`/coupons/${id}/claim`)
export const getMyCoupons = (params) => http.get('/coupons/mine', { params })
