import http from './http'
export const getGoods = (params) => http.get('/goods', { params })
export const getGoodsDetail = (id) => http.get(`/goods/${id}`)
