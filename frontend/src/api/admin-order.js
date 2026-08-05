import http from './http'

export const getAdminOrders = (params) => http.get('/admin/orders', { params })
export const getAdminOrderDetail = (id) => http.get(`/admin/orders/${id}`)
export const acceptAdminOrder = (id) => http.post(`/admin/orders/${id}/accept`)
export const deliverAdminOrder = (id) => http.post(`/admin/orders/${id}/deliver`)
export const completeAdminOrder = (id) => http.post(`/admin/orders/${id}/complete`)
