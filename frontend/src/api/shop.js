import http from './http'
export const getShops = (params) => http.get('/shops', { params })
export const getShop = (id) => http.get(`/shops/${id}`)
export const getShopGoods = (id, params) => http.get(`/shops/${id}/goods`, { params })
export const getShopGoodsCategories = (id) => http.get(`/shops/${id}/goods-categories`)
export const getShopComments = (id, params) => http.get(`/shops/${id}/comments`, { params })
