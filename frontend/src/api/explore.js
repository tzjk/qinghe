import http from './http'

export const getExplorePosts = (params) => http.get('/explore/posts', { params })
export const getExplorePost = (id) => http.get(`/explore/posts/${id}`)
export const createExplorePost = (data) => http.post('/explore/posts', data)
export const uploadExploreImage = (file, onUploadProgress) => { const form = new FormData(); form.append('file', file, file.name); return http.post('/explore/images', form, { onUploadProgress }) }
export const updateExplorePost = (id, data) => http.put(`/explore/posts/${id}`, data)
export const deleteExplorePost = (id) => http.delete(`/explore/posts/${id}`)
export const likeExplorePost = (id) => http.post(`/explore/posts/${id}/like`)
export const unlikeExplorePost = (id) => http.delete(`/explore/posts/${id}/like`)
export const getExploreComments = (id, params) => http.get(`/explore/posts/${id}/comments`, { params })
export const createExploreComment = (id, data) => http.post(`/explore/posts/${id}/comments`, data)
export const getNearbyShops = (params) => http.get('/explore/shops/nearby', { params })
