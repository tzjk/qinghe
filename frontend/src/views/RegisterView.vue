<script setup>
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { Hide, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { register, sendCode } from '../api/user'

const router = useRouter()
const formRef = ref()
const form = reactive({ username: '', phone: '', code: '', password: '', confirmPassword: '' })
const submitting = ref(false)
const sendingCode = ref(false)
const seconds = ref(0)
const timer = ref(null)
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const phonePattern = /^1\d{10}$/
const usernamePattern = /^[A-Za-z0-9_]{4,20}$/
const codePattern = /^\d{6}$/
const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d).{8,32}$/
const validPhone = computed(() => phonePattern.test(form.phone))
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }, { pattern: usernamePattern, message: '用户名为4至20位字母、数字或下划线', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }, { pattern: phonePattern, message: '请输入正确手机号', trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }, { pattern: codePattern, message: '验证码必须为6位数字', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { pattern: passwordPattern, message: '密码为8至32位且需同时包含字母和数字', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请再次输入密码', trigger: 'blur' }, { validator: (_rule, value, callback) => value === form.password ? callback() : callback(new Error('两次密码不一致')), trigger: 'blur' }]
}

function clearCountdown() { if (timer.value) { clearInterval(timer.value); timer.value = null } }
function startCountdown() { clearCountdown(); seconds.value = 60; timer.value = setInterval(() => { seconds.value -= 1; if (seconds.value <= 0) { seconds.value = 0; clearCountdown() } }, 1000) }
async function getCode() {
  if (!validPhone.value) { ElMessage.error('请输入正确手机号'); return }
  if (sendingCode.value || seconds.value > 0) return
  sendingCode.value = true
  try { await sendCode(form.phone); ElMessage.success('验证码已发送'); startCountdown() } catch (error) { if (!error?.__qingheMessageShown) ElMessage.error(error?.message || '验证码发送失败，请稍后重试') } finally { sendingCode.value = false }
}
async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid || submitting.value) return
  submitting.value = true
  try {
    await register({ username: form.username, phone: form.phone, code: form.code, password: form.password, confirmPassword: form.confirmPassword })
    const phone = form.phone
    const username = form.username
    form.code = ''
    form.password = ''
    form.confirmPassword = ''
    ElMessage.success('注册成功，请使用手机号验证码登录')
    router.replace({ name: 'login', query: { phone, username } })
  } catch (error) { if (!error?.__qingheMessageShown) ElMessage.error(error?.message || '注册失败，请稍后重试') } finally { submitting.value = false }
}
onBeforeUnmount(clearCountdown)
</script>

<template>
  <section class="register-page"><el-card class="register-card ui-card"><p class="page-eyebrow">CREATE ACCOUNT</p><h1>注册账号</h1><p class="subtle">注册后仍使用手机号验证码登录，不会自动登录。</p><el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit"><el-form-item label="用户名" prop="username"><el-input v-model.trim="form.username" maxlength="20" placeholder="4至20位字母、数字或下划线" autocomplete="username" /></el-form-item><el-form-item label="手机号" prop="phone"><el-input v-model.trim="form.phone" maxlength="11" placeholder="请输入手机号" autocomplete="tel" /></el-form-item><el-form-item label="验证码" prop="code"><el-input v-model.trim="form.code" maxlength="6" placeholder="请输入验证码" autocomplete="one-time-code"><template #append><el-button native-type="button" :loading="sendingCode" :disabled="sendingCode || seconds > 0" @click="getCode">{{ seconds ? `${seconds}s` : '获取验证码' }}</el-button></template></el-input></el-form-item><el-form-item label="密码" prop="password"><el-input v-model="form.password" :type="showPassword ? 'text' : 'password'" maxlength="32" placeholder="8至32位，包含字母和数字" autocomplete="new-password"><template #suffix><el-button link native-type="button" :icon="showPassword ? Hide : View" @click="showPassword=!showPassword" /></template></el-input></el-form-item><el-form-item label="确认密码" prop="confirmPassword"><el-input v-model="form.confirmPassword" :type="showConfirmPassword ? 'text' : 'password'" maxlength="32" placeholder="请再次输入密码" autocomplete="new-password"><template #suffix><el-button link native-type="button" :icon="showConfirmPassword ? Hide : View" @click="showConfirmPassword=!showConfirmPassword" /></template></el-input></el-form-item><el-button type="primary" native-type="submit" :loading="submitting" class="submit-button">注册账号</el-button></el-form><div class="register-actions"><span>已有账号？</span><el-button text type="primary" @click="router.push('/login')">返回登录</el-button></div></el-card></section>
</template>

<style scoped>
.register-page { display: grid; min-height: 100vh; place-items: center; padding: 28px 16px; background: linear-gradient(145deg, #f1f6ff, #f8fafc); }.register-card { width: min(460px, 100%); }.register-card h1 { margin: 0; font-size: 28px; }.submit-button { width: 100%; margin-top: 4px; }.register-actions { display: flex; align-items: center; justify-content: center; gap: 2px; margin-top: 16px; color: var(--qh-text-secondary); font-size: 14px; }
</style>
