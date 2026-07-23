import http from './http'

export const adminLogin = (data) => http.post('/admin/auth/login', data)
export const getAdminMe = () => http.get('/admin/auth/me')
export const adminLogout = () => http.post('/admin/auth/logout')
