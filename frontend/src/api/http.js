import axios from 'axios'
import { clearUserSession, getToken } from '../utils/token'
import { clearAdminSession, getAdminToken } from '../utils/admin-session'
import { ElMessage } from 'element-plus'

const http = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL || '/api', timeout: 10000 })
const isAdminRequest = (config) => String(config?.url || '').startsWith('/admin/')

http.interceptors.request.use((config) => {
  const token = isAdminRequest(config) ? getAdminToken() : getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

let redirecting = false
function showErrorOnce(error, fallbackMessage) {
  if (!error.__qingheMessageShown) {
    error.__qingheMessageShown = true
    ElMessage.error(fallbackMessage || error.message || '请求失败')
  }
  return error
}

http.interceptors.response.use((response) => {
  if (response.config.responseType === 'blob') return response.data
  const payload = response.data
  if (payload?.code === 200) return payload.data
  const error = new Error(payload?.message || '请求失败')
  return Promise.reject(showErrorOnce(error, error.message))
}, (error) => {
  if (error.response?.status === 401 && !redirecting) {
    const adminRequest = isAdminRequest(error.config)
    const loginPath = adminRequest ? '/admin/login' : '/login'
    if (adminRequest) clearAdminSession(); else clearUserSession()
    showErrorOnce(error, '登录已失效，请重新登录')
    if (window.location.pathname !== loginPath) {
      redirecting = true
      const redirect = window.location.pathname + window.location.search
      window.location.assign(`${loginPath}?redirect=${encodeURIComponent(redirect)}`)
      setTimeout(() => { redirecting = false }, 500)
    }
  } else {
    showErrorOnce(error, error.response?.data?.message)
  }
  return Promise.reject(error)
})

export default http
