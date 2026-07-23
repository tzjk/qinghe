<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { adminLogin } from '../api/admin-auth'
import { useAdminStore } from '../stores/admin'

const router = useRouter()
const adminStore = useAdminStore()
const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function submit() {
  if (!form.username || !form.password) return ElMessage.error('请输入管理员账号和密码')
  loading.value = true
  try {
    const data = await adminLogin(form)
    adminStore.setLogin(data.token, data.admin)
    router.replace('/admin')
  } catch (error) {
    if (!error?.__qingheMessageShown) ElMessage.error(error?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="admin-login-page">
    <section class="admin-login-panel">
      <div class="brand"><span>青禾</span><small>管理端</small></div>
      <el-card shadow="never" class="login-card">
        <h1>管理员登录</h1>
        <el-form label-position="top" @submit.prevent="submit">
          <el-form-item label="管理员账号"><el-input v-model.trim="form.username" maxlength="32" autocomplete="username" /></el-form-item>
          <el-form-item label="密码"><el-input v-model="form.password" type="password" maxlength="64" show-password autocomplete="current-password" /></el-form-item>
          <el-button native-type="submit" type="primary" :loading="loading" class="submit">登录</el-button>
        </el-form>
      </el-card>
    </section>
  </main>
</template>

<style scoped>
.admin-login-page { min-height: 100vh; display: grid; place-items: center; padding: 24px; background: #f4f7fb; }
.admin-login-panel { width: min(420px, 100%); }.brand { display: flex; gap: 10px; align-items: baseline; margin: 0 0 18px 6px; color: #1f4b7a; }.brand span { font-size: 27px; font-weight: 800; }.brand small { color: #718096; }.login-card { border-radius: 20px; padding: 10px; }.login-card h1 { margin: 12px 8px 18px; color: #1f2933; }.submit { width: 100%; height: 42px; border-radius: 10px; }
</style>
