import http from './http'
export const getHomeSummary = () => http.get('/home/summary')
