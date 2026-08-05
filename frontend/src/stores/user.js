import { defineStore } from 'pinia'
import { clearUserSession, getStoredUser, getToken, setStoredUser, setToken } from '../utils/token'
import { getMe } from '../api/user'
import { disconnectOrderWebSocket } from '../utils/order-websocket'
import { resetCampusAssistantConversation } from '../composables/useCampusAssistant'

export const useUserStore = defineStore('user', {
  state: () => ({ token: getToken(), profile: getStoredUser() }),
  getters: { isLoggedIn: (state) => Boolean(state.token) },
  actions: {
    setLogin(token, profile) { if (this.token && this.token !== token) resetCampusAssistantConversation(); this.token = token; this.profile = profile; setToken(token); setStoredUser(profile) },
    setProfile(profile) { this.profile = profile; setStoredUser(profile) },
    clearLogin() { disconnectOrderWebSocket('user'); resetCampusAssistantConversation(); this.token = ''; this.profile = null; clearUserSession() },
    async restoreSession() { if (!this.token) return; try { this.setProfile(await getMe()) } catch { this.clearLogin() } }
  }
})
