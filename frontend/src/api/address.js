import http from './http'

export const getAddresses = () => http.get('/addresses')
export const getAddress = (id) => http.get(`/addresses/${id}`)
export const getCampuses = () => http.get('/campuses')
export const getCampusBuildings = (campusId) => http.get(`/campuses/${campusId}/buildings`)
export const createAddress = (data) => http.post('/addresses', data)
export const updateAddress = (id, data) => http.put(`/addresses/${id}`, data)
export const deleteAddress = (id) => http.delete(`/addresses/${id}`)
export const setDefaultAddress = (id) => http.put(`/addresses/${id}/default`)
