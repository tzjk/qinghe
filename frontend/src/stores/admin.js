import { defineStore } from 'pinia'
import { getAdminMe } from '../api/admin-auth'
import { clearAdminSession, getAdminToken, getStoredAdmin, setAdminToken, setStoredAdmin } from '../utils/admin-session'

export const useAdminStore = defineStore('admin', {
  state: () => ({ token: getAdminToken(), profile: getStoredAdmin() }),
  getters: { isLoggedIn: (state) => Boolean(state.token) },
  actions: {
    setLogin(token, profile) { this.token = token; this.profile = profile; setAdminToken(token); setStoredAdmin(profile) },
    setProfile(profile) { this.profile = profile; setStoredAdmin(profile) },
    clearLogin() { this.token = ''; this.profile = null; clearAdminSession() },
    async restoreSession() { if (!this.token) return; try { this.setProfile(await getAdminMe()) } catch { this.clearLogin() } }
  }
})
