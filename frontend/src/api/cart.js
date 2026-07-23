import http from './http'

export const getCart = () => http.get('/cart')
export const addCart = (data) => http.post('/cart', data)
export const updateCartQuantity = (id, data) => http.put(`/cart/${id}`, data)
export const updateCartSelected = (id, data) => http.put(`/cart/${id}/selected`, data)
export const deleteCartItem = (id) => http.delete(`/cart/${id}`)
export const clearCart = () => http.delete('/cart')
