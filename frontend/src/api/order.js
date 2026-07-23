import http from './http'

export const createOrder = (data) => http.post('/orders', data)
