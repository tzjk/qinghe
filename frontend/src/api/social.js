import http from './http'

export const signIn = () => http.post('/user/sign-in')
export const getSignInStatus = () => http.get('/user/sign-in/status')
export const getSignInCalendar = (month) => http.get('/user/sign-in/calendar', { params: { month } })
export const getFollowStatus = (userId) => http.get(`/follows/${userId}/status`)
export const followUser = (userId) => http.post(`/follows/${userId}`)
export const unfollowUser = (userId) => http.delete(`/follows/${userId}`)
export const getMyFollowing = (params) => http.get('/follows/me/following', { params })
export const getMyFollowers = (params) => http.get('/follows/me/followers', { params })
export const getCommonFollowings = (userId) => http.get(`/follows/${userId}/common`)
