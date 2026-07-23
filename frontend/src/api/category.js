import http from './http'
export const getCategories = () => http.get('/categories')
