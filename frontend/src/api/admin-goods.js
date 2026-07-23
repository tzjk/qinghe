import http from './http'

export const getAdminGoods = (params) => http.get('/admin/goods', { params })
export const getAdminGoodsCategories = (params) => http.get('/admin/goods/categories', { params })
export const getAdminGoodsDetail = (id) => http.get(`/admin/goods/${id}`)
export const createAdminGoods = (data) => http.post('/admin/goods', data)
export const updateAdminGoods = (id, data) => http.put(`/admin/goods/${id}`, data)
export const updateAdminGoodsStatus = (id, data) => http.put(`/admin/goods/${id}/status`, data)
export const updateAdminGoodsStock = (id, data) => http.put(`/admin/goods/${id}/stock`, data)
export const uploadAdminGoodsImage = (id, file) => {
  const body = new FormData()
  body.append('file', file, 'goods-image.webp')
  return http.post(`/admin/goods/${id}/image`, body)
}
export const createAdminGoodsCategory = (data) => http.post('/admin/goods/categories', data)
export const updateAdminGoodsCategory = (id, data) => http.put(`/admin/goods/categories/${id}`, data)
export const updateAdminGoodsCategoryStatus = (id, data) => http.put(`/admin/goods/categories/${id}/status`, data)
