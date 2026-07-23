const TOKEN_KEY = 'qh:token'
const USER_KEY = 'qh:user'

export const getToken = () => window.localStorage.getItem(TOKEN_KEY) || ''
export const setToken = (token) => window.localStorage.setItem(TOKEN_KEY, token)
export const clearToken = () => window.localStorage.removeItem(TOKEN_KEY)
export const getStoredUser = () => { try { return JSON.parse(window.localStorage.getItem(USER_KEY) || 'null') } catch { return null } }
export const setStoredUser = (user) => window.localStorage.setItem(USER_KEY, JSON.stringify(user))
export const clearUserSession = () => { clearToken(); window.localStorage.removeItem(USER_KEY) }
