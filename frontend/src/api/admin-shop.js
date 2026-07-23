import http from './http'

export const getAdminShops = (params) => http.get('/admin/shops', { params })
export const getAdminShop = (id) => http.get(`/admin/shops/${id}`)
export const createAdminShop = (data) => http.post('/admin/shops', data)
export const updateAdminShop = (id, data) => http.put(`/admin/shops/${id}`, data)
export const updateAdminShopStatus = (id, data) => http.put(`/admin/shops/${id}/status`, data)
export const uploadAdminShopCover = (id, file) => {
  const body = new FormData()
  body.append('file', file, 'shop-cover.webp')
  return http.post(`/admin/shops/${id}/cover`, body)
}
