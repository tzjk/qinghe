import http from './http'

export const getAdminExplorePosts = (params) => http.get('/admin/explore/posts', { params })
export const getAdminExplorePost = (id) => http.get(`/admin/explore/posts/${id}`)
export const getAdminExploreComments = (id, params) => http.get(`/admin/explore/posts/${id}/comments`, { params })
export const updateAdminExplorePostStatus = (id, data) => http.put(`/admin/explore/posts/${id}/status`, data)
export const updateAdminExploreCommentStatus = (id, data) => http.put(`/admin/explore/comments/${id}/status`, data)
