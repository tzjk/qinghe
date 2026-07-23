import http from './http'
export const getStudentProfile = () => http.get('/student/profile')
export const saveStudentProfile = (data) => http.put('/student/profile', data)
export const resolveDormQr = (qrContent) => http.post('/student/dorm/qr/resolve', { qrContent })
export const confirmDormCheckIn = (qrContent) => http.post('/student/dorm/check-in', { qrContent })
export const getMyDorm = () => http.get('/student/dorm/me')
