<script setup>
import { useRouter } from 'vue-router'
import { adminLogout } from '../api/admin-auth'
import { useAdminStore } from '../stores/admin'

const router = useRouter()
const adminStore = useAdminStore()
async function logout() { try { await adminLogout() } finally { adminStore.clearLogin(); router.replace('/admin/login') } }
</script>

<template>
  <el-container class="admin-shell">
    <el-aside width="220px"><h2>青禾后台</h2><el-menu :default-active="$route.path" router><el-menu-item index="/admin">管理首页</el-menu-item><el-menu-item index="/admin/shops">店铺管理</el-menu-item><el-menu-item index="/admin/goods/categories">商品分类</el-menu-item><el-menu-item index="/admin/goods">商品管理</el-menu-item><el-menu-item index="/admin/orders">订单管理</el-menu-item><el-menu-item index="/admin/dorm/students">学生学籍</el-menu-item><el-menu-item index="/admin/dorm">宿舍管理</el-menu-item><el-menu-item index="/admin/dorm/checkins">入住管理</el-menu-item></el-menu></el-aside>
    <el-container>
      <el-header class="admin-header"><span>{{ adminStore.profile?.displayName || '管理员' }}</span><el-space><el-button text @click="$router.push('/')">返回用户端</el-button><el-button text type="danger" @click="logout">退出登录</el-button></el-space></el-header>
      <el-main><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin-shell { min-height: 100vh; }.el-aside { padding: 22px 12px; background: #1f2d45; color: #fff; }.el-aside h2 { margin: 0 10px 20px; }.admin-header { display: flex; justify-content: space-between; align-items: center; background: #fff; border-bottom: 1px solid #e7edf5; }
</style>
