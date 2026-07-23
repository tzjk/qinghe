import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import { useUserStore } from './stores/user'
import { useAdminStore } from './stores/admin'
import './assets/base.css'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia).use(router).use(ElementPlus).mount('#app')
useUserStore(pinia).restoreSession()
useAdminStore(pinia).restoreSession()
