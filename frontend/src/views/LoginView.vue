<script setup>
import { onBeforeUnmount, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { login, passwordLogin, sendCode } from '../api/user'
import { useUserStore } from '../stores/user'

const router = useRouter()
const route = useRoute()
const store = useUserStore()
const tab = ref('code')
const form = reactive({ phone: '', code: '', password: '' })
const loading = ref(false)
const sending = ref(false)
const seconds = ref(0)
const timer = ref(null)
const validPhone = () => /^1\d{10}$/.test(form.phone)
function clearCountdown() { if (timer.value) { clearInterval(timer.value); timer.value = null } }
function countdown() { clearCountdown(); seconds.value = 60; timer.value = setInterval(() => { if (--seconds.value <= 0) { seconds.value = 0; clearCountdown() } }, 1000) }
async function requestCode() { if (!validPhone()) return ElMessage.error('请输入正确手机号'); if (sending.value || seconds.value) return; sending.value = true; try { await sendCode(form.phone); ElMessage.success('验证码已发送'); countdown() } catch (error) { if (!error?.__qingheMessageShown) ElMessage.error(error?.message || '发送失败') } finally { sending.value = false } }
async function submit() { loading.value = true; try { if (!validPhone()) return ElMessage.error('请输入正确手机号'); if (tab.value === 'code' && !form.code) return ElMessage.error('请输入验证码'); if (tab.value === 'password' && !form.password) return ElMessage.error('请输入密码'); const data = tab.value === 'code' ? await login(form.phone, form.code) : await passwordLogin(form.phone, form.password); store.setLogin(data.token, data.user); if (!data.profileCompleted) return router.replace({ name: 'profile-complete' }); const target = String(route.query.redirect || '/'); router.replace(target.startsWith('/') && !target.startsWith('/admin') ? target : '/') } catch (error) { if (!error?.__qingheMessageShown) ElMessage.error(error?.message || '登录失败') } finally { loading.value = false } }
onBeforeUnmount(clearCountdown)
</script>

<template><main class="login-page"><section class="login-panel"><div class="login-brand"><span>青禾</span><small>校园生活服务</small></div><el-card class="login-card" shadow="never"><h1>欢迎回来</h1><el-form label-position="top" @submit.prevent="submit"><el-tabs v-model="tab" class="login-tabs" stretch><el-tab-pane label="验证码登录" name="code"/><el-tab-pane label="密码登录" name="password"/></el-tabs><el-form-item label="手机号"><el-input v-model.trim="form.phone" maxlength="11" inputmode="numeric"/></el-form-item><el-form-item v-if="tab === 'code'" label="验证码"><el-input v-model.trim="form.code" maxlength="6"><template #append><el-button :disabled="sending || seconds > 0" :loading="sending" @click="requestCode">{{ seconds ? `${seconds}s` : '获取验证码' }}</el-button></template></el-input></el-form-item><el-form-item v-else label="密码"><el-input v-model="form.password" type="password" maxlength="32" show-password autocomplete="current-password" /></el-form-item><el-button native-type="submit" type="primary" :loading="loading" class="login-submit">登录</el-button></el-form></el-card></section></main></template>

<style scoped>.login-page{min-height:100vh;display:grid;place-items:center;padding:24px;background:#f8faf8}.login-panel{width:min(440px,100%)}.login-brand{display:flex;align-items:baseline;gap:10px;margin:0 0 18px 6px;color:#276749}.login-brand span{font-size:27px;font-weight:800}.login-brand small{color:#718096}.login-card{border-radius:22px;padding:10px}.login-card h1{margin:12px 8px 18px;color:#1f2933}.login-tabs{margin-bottom:18px}.login-submit{width:100%;height:42px;border-radius:10px}</style>
