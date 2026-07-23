const ADMIN_TOKEN_KEY = 'qh:admin:token'
const ADMIN_PROFILE_KEY = 'qh:admin:profile'

export const getAdminToken = () => window.localStorage.getItem(ADMIN_TOKEN_KEY) || ''
export const setAdminToken = (token) => window.localStorage.setItem(ADMIN_TOKEN_KEY, token)
export const clearAdminToken = () => window.localStorage.removeItem(ADMIN_TOKEN_KEY)
export const getStoredAdmin = () => {
  try { return JSON.parse(window.localStorage.getItem(ADMIN_PROFILE_KEY) || 'null') } catch { return null }
}
export const setStoredAdmin = (admin) => window.localStorage.setItem(ADMIN_PROFILE_KEY, JSON.stringify(admin))
export const clearAdminSession = () => { clearAdminToken(); window.localStorage.removeItem(ADMIN_PROFILE_KEY) }
