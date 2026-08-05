import http from './http'

export const createOrder = (data) => http.post('/orders', data)
export const getOrders = (params) => http.get('/orders', { params })
export const getOrderDetail = (id) => http.get(`/orders/${id}`)
export const simulateOrderPayment = (id) => http.post(`/orders/${id}/simulate-pay`)
export const cancelOrder = (id) => http.delete(`/orders/${id}`)
