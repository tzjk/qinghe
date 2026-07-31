<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { Avatar, Compass, House, Location, ShoppingCart, Shop, Tickets, UserFilled, OfficeBuilding } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { getCart } from '../api/cart'
import { logout as logoutRequest } from '../api/user'
import { useUserStore } from '../stores/user'
import { formatDisplayName } from '../utils/display-name'
import FloatingCampusAssistant from '../components/assistant/FloatingCampusAssistant.vue'

const router = useRouter()
const user = useUserStore()
const cartCount = ref(0)
const displayName = computed(() => formatDisplayName(user.profile))
const navItems = [
  { path: '/', label: '首页', icon: House },
  { path: '/shops', label: '商铺', icon: Shop },
  { path: '/cart', label: '购物车', icon: ShoppingCart },
  { path: '/orders', label: '订单', icon: Tickets },
  { path: '/coupons', label: '优惠券', icon: Tickets },
  { path: '/blogs', label: '探店', icon: Compass },
  { path: '/dorm/me', label: '宿舍', icon: OfficeBuilding },
  { path: '/profile', label: '我的', icon: UserFilled }
]

async function loadCartCount() {
  if (!user.isLoggedIn) { cartCount.value = 0; return }
  try { cartCount.value = Number((await getCart()).totalCount || 0) } catch { cartCount.value = 0 }
}

async function logout() {
  try {
    await logoutRequest()
    ElMessage.success('已退出登录')
  } catch (error) {
    if (!error?.__qingheMessageShown) ElMessage.error(error?.message || '退出失败')
  } finally {
    user.clearLogin()
    cartCount.value = 0
    router.replace('/')
  }
}

watch(() => user.isLoggedIn, loadCartCount, { immediate: true })
onMounted(loadCartCount)
</script>

<template>
  <el-container class="user-layout">
    <el-header class="user-header">
      <div class="header-inner">
        <router-link class="brand" to="/"><span class="brand-mark">Q</span><span>青禾生活</span></router-link>
        <nav class="nav-scroll" aria-label="主导航">
          <router-link v-for="item in navItems" :key="item.path" class="nav-link" :to="item.path">
            <el-icon><component :is="item.icon" /></el-icon><span>{{ item.label }}</span>
            <el-badge v-if="item.path === '/cart' && cartCount" :value="cartCount" :max="99" class="cart-badge" />
          </router-link>
        </nav>
        <div class="user-actions">
          <template v-if="user.isLoggedIn">
            <el-button class="profile-button" text @click="router.push('/profile')"><el-avatar :size="28" :src="user.profile?.avatarUrl"><el-icon><Avatar /></el-icon></el-avatar><span>{{ displayName }}</span></el-button>
            <el-button text @click="logout">退出</el-button>
          </template>
          <el-button v-else type="primary" plain @click="router.push('/login')">登录</el-button>
        </div>
      </div>
    </el-header>
    <el-main class="user-main"><router-view /></el-main>
    <footer class="user-footer"><div><strong>青禾生活</strong><span>校园生活服务平台</span></div><span><el-icon><Location /></el-icon> 发现身边的校园好去处</span></footer>
    <FloatingCampusAssistant />
  </el-container>
</template>

<style scoped>
.user-layout { min-height: 100vh; }
.user-header { height: auto; padding: 0; background: rgba(255, 255, 255, .94); border-bottom: 1px solid var(--qh-border); backdrop-filter: blur(12px); }
.header-inner { display: flex; align-items: center; gap: 22px; width: min(100%, var(--qh-page-max)); min-height: 68px; margin: 0 auto; padding: 0 20px; }
.brand { display: inline-flex; align-items: center; gap: 9px; color: var(--qh-text); font-size: 19px; font-weight: 800; white-space: nowrap; }
.brand-mark { display: grid; place-items: center; width: 30px; height: 30px; border-radius: 10px; color: #fff; background: var(--qh-primary); box-shadow: 0 5px 12px rgba(61, 120, 216, .24); }
.nav-scroll { display: flex; flex: 1; align-self: stretch; align-items: stretch; gap: 3px; min-width: 0; }
.nav-link { position: relative; display: inline-flex; align-items: center; gap: 5px; padding: 0 10px; color: var(--qh-text-secondary); font-size: 14px; white-space: nowrap; }
.nav-link::after { position: absolute; right: 11px; bottom: 0; left: 11px; height: 3px; border-radius: 3px 3px 0 0; background: transparent; content: ''; transition: background .16s ease; }
.nav-link:hover, .nav-link.router-link-exact-active { color: var(--qh-primary); }
.nav-link.router-link-exact-active::after { background: var(--qh-primary); }
.cart-badge { position: absolute; top: 11px; right: 2px; }
.user-actions { display: flex; align-items: center; gap: 4px; white-space: nowrap; }
.profile-button { display: inline-flex; gap: 7px; color: var(--qh-text); }
.user-main { padding: 0; overflow: visible; }
.user-footer { display: flex; justify-content: space-between; gap: 16px; padding: 20px max(20px, calc((100% - var(--qh-page-max)) / 2)); color: var(--qh-text-secondary); border-top: 1px solid var(--qh-border); background: #fff; font-size: 13px; }
.user-footer div, .user-footer > span { display: inline-flex; align-items: center; gap: 8px; }
.user-footer strong { color: var(--qh-text); }
@media (max-width: 880px) { .header-inner { gap: 14px; } .nav-scroll { overflow-x: auto; scrollbar-width: none; } .nav-scroll::-webkit-scrollbar { display: none; } .nav-link { padding: 0 8px; } .nav-link span { display: none; } .nav-link::after { right: 8px; left: 8px; } }
@media (max-width: 560px) { .header-inner { padding: 0 14px; } .brand { font-size: 16px; } .brand-mark { width: 27px; height: 27px; } .user-actions .profile-button span, .user-actions > .el-button:last-child { display: none; } .user-footer { align-items: flex-start; flex-direction: column; padding: 20px 16px; } }
</style>
