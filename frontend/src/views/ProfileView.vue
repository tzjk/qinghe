<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { Avatar, Camera, EditPen, Location, ShoppingCart } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { getAddresses } from '../api/address'
import { getMe, updateProfile, uploadAvatar } from '../api/user'
import { getStudentProfile } from '../api/student-dorm'
import { getSignInCalendar, getSignInStatus, signIn } from '../api/social'
import { useUserStore } from '../stores/user'
import { formatDisplayName, text } from '../utils/display-name'
import PageHeader from '../components/PageHeader.vue'

const router = useRouter()
const user = useUserStore()
const profile = computed(() => user.profile || {})
const phone = computed(() => profile.value.phoneMasked || maskPhone(profile.value.phone))
const addresses = ref([])
const studentProfile = ref(null)
const studentProfileState = ref('loading')
const profileLoadError = ref(false)
const editing = ref(false)
const saving = ref(false)
const fileInput = ref(null)
const avatarBlob = ref(null)
const previewUrl = ref('')
let previewObjectUrl = ''
const form = reactive({ nickname: '' })
const signStatus = ref({ signedToday: false, monthDays: 0, streakDays: 0 })
const signCalendar = ref([])
const signLoading = ref(false)
const currentAddress = computed(() => addresses.value.find((item) => item.isDefault) || addresses.value[0])
const realName = computed(() => text(studentProfile.value?.realName) || text(profile.value.realName))
const displayName = computed(() => formatDisplayName({ realName: realName.value, nickname: profile.value.nickname, phoneMasked: phone.value }))
const hasStudentProfile = computed(() => Boolean(studentProfile.value || profile.value.hasStudentProfile))
const realNameCardValue = computed(() => {
  if (studentProfileState.value === 'loading') return '正在加载学生资料'
  if (studentProfileState.value === 'error') return '学生资料加载失败，请重试'
  if (!hasStudentProfile.value) return '尚未建档'
  return realName.value || '真实姓名暂未填写，请联系管理员'
})
const realNameHint = computed(() => hasStudentProfile.value ? '学籍资料由管理员维护' : '完成学生资料建档后将显示真实姓名')
function maskPhone(value) { return String(value || '').replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2') }
function clearPreview() { if (previewObjectUrl) URL.revokeObjectURL(previewObjectUrl); previewObjectUrl = ''; previewUrl.value = ''; avatarBlob.value = null }
function openEditor() { form.nickname = profile.value.nickname || ''; clearPreview(); editing.value = true }
function closeEditor() { editing.value = false; clearPreview() }
function chooseAvatar() { fileInput.value?.click() }
function imageFromFile(file) { return new Promise((resolve, reject) => { const url = URL.createObjectURL(file); const image = new Image(); image.onload = () => { URL.revokeObjectURL(url); resolve(image) }; image.onerror = () => { URL.revokeObjectURL(url); reject(new Error('图片无法读取')) }; image.src = url }) }
function canvasBlob(canvas, quality) { return new Promise((resolve) => canvas.toBlob(resolve, 'image/webp', quality)) }
async function cropAndCompress(file) {
  const image = await imageFromFile(file)
  const crop = Math.min(image.naturalWidth, image.naturalHeight)
  if (!crop) throw new Error('图片尺寸无效')
  const sourceX = (image.naturalWidth - crop) / 2
  const sourceY = (image.naturalHeight - crop) / 2
  let finalBlob = null
  for (let size = Math.max(128, Math.min(512, Math.floor(crop))); size >= 128; size -= 64) {
    const canvas = document.createElement('canvas'); canvas.width = size; canvas.height = size
    canvas.getContext('2d').drawImage(image, sourceX, sourceY, crop, crop, 0, 0, size, size)
    for (const quality of [0.84, 0.74, 0.64, 0.54]) {
      const blob = await canvasBlob(canvas, quality)
      if (!blob) continue
      finalBlob = blob
      if (blob.size < 300 * 1024) return blob
    }
  }
  if (!finalBlob || finalBlob.size >= 300 * 1024) throw new Error('图片压缩后仍超过 300KB，请选择更简单的图片')
  return finalBlob
}
async function handleFile(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) return ElMessage.error('仅支持 jpg、jpeg、png 或 webp 图片')
  if (file.size > 2 * 1024 * 1024) return ElMessage.error('原图不能超过 2MB')
  try {
    const blob = await cropAndCompress(file)
    clearPreview(); avatarBlob.value = blob; previewObjectUrl = URL.createObjectURL(blob); previewUrl.value = previewObjectUrl
    ElMessage.success('已生成 1:1 本地预览，保存时才会上传')
  } catch (error) { ElMessage.error(error?.message || '图片处理失败') }
}
async function saveProfile() {
  if (!form.nickname.trim()) return ElMessage.error('请输入昵称')
  saving.value = true
  try {
    let latest = await updateProfile({ nickname: form.nickname.trim(), avatarUrl: profile.value.avatarUrl || '' })
    if (avatarBlob.value) latest = await uploadAvatar(new File([avatarBlob.value], 'avatar.webp', { type: 'image/webp' }))
    user.setProfile(latest); closeEditor(); ElMessage.success('资料已更新')
  } catch (error) { if (!error?.__qingheMessageShown) ElMessage.error(error?.message || '保存失败') } finally { saving.value = false }
}
async function loadAddresses() { try { addresses.value = await getAddresses() } catch { addresses.value = [] } }
async function loadProfileData() {
  const [account, student] = await Promise.allSettled([getMe(), getStudentProfile()])
  profileLoadError.value = account.status === 'rejected'
  if (account.status === 'fulfilled') user.setProfile(account.value)
  if (student.status === 'fulfilled') { studentProfile.value = student.value; studentProfileState.value = 'loaded' } else { studentProfileState.value = 'error' }
}
function businessMonth() { const parts = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit' }).formatToParts(new Date()); return `${parts.find((part) => part.type === 'year')?.value}-${parts.find((part) => part.type === 'month')?.value}` }
async function loadSignIn() { try { const [status, calendar] = await Promise.all([getSignInStatus(), getSignInCalendar(businessMonth())]); signStatus.value = status || signStatus.value; signCalendar.value = calendar?.days || [] } catch (error) { ElMessage.warning(error?.message || '签到服务暂不可用') } }
async function submitSignIn() { signLoading.value = true; try { signStatus.value = await signIn(); await loadSignIn(); ElMessage.success(signStatus.value.signedToday ? '今日已签到' : '签到成功') } catch (error) { ElMessage.error(error?.message || '签到服务暂不可用') } finally { signLoading.value = false } }
watch(() => user.profile, (value) => { if (!editing.value) form.nickname = value?.nickname || '' }, { immediate: true })
onMounted(() => { loadAddresses(); loadProfileData(); loadSignIn() })
onBeforeUnmount(clearPreview)
</script>

<template>
  <section class="page-shell"><PageHeader title="个人中心" description="管理个人资料与校园服务" /><section class="profile-hero"><el-avatar :size="76" :src="profile.avatarUrl" class="profile-avatar"><el-icon><Avatar /></el-icon></el-avatar><div class="profile-main"><h2>{{ displayName }}</h2><p>{{ phone || '当前登录账号' }}</p><div class="tag-row"><el-tag v-if="profile.nickname" effect="light">账号昵称：{{ profile.nickname }}</el-tag><el-tag :type="profile.hasPassword ? 'success' : 'info'" effect="light">{{ profile.hasPassword ? '已设置密码' : '仅验证码登录' }}</el-tag></div></div><el-button type="primary" plain :icon="EditPen" @click="openEditor">编辑资料</el-button></section>
    <section class="profile-summary"><article><span class="label">真实姓名</span><strong>{{ realNameCardValue }}</strong><small>{{ realNameHint }}</small><el-button v-if="studentProfileState === 'loaded' && !hasStudentProfile" text type="primary" @click="router.push('/dorm/scan')">去学生资料建档</el-button><el-button v-else-if="studentProfileState === 'error'" text type="primary" @click="loadProfileData">重新加载</el-button></article><article><span class="label">账号昵称</span><strong>{{ profile.nickname || '未设置' }}</strong><small>可在编辑资料中修改</small></article><article><span class="label">登录手机号</span><strong>{{ phone || '未绑定' }}</strong><small>手机号不可修改</small></article><article class="address-summary"><span class="label">校园地址</span><strong>{{ currentAddress?.formattedAddress || '尚未填写校园地址' }}</strong><el-button text type="primary" @click="router.push('/profile/addresses')">管理地址</el-button></article></section>
    <p v-if="profileLoadError" class="profile-load-note">账号资料刷新失败，当前展示的是已缓存的安全资料；请稍后刷新页面重试。</p>
    <section class="sign-card"><div><h2>每日签到</h2><p>本月已签到 {{ signStatus.monthDays || 0 }} 天，连续 {{ signStatus.streakDays || 0 }} 天。当前签到不发放积分或优惠券。</p></div><el-button type="primary" :loading="signLoading" :disabled="signStatus.signedToday" @click="submitSignIn">{{ signStatus.signedToday ? '今日已签到' : '立即签到' }}</el-button><div class="sign-calendar"><span v-for="(signed, index) in signCalendar" :key="index" :class="{ signed }">{{ index + 1 }}</span></div></section>
    <section><div class="section-heading"><div><h2>常用服务</h2><p>当前已完成并可使用的功能</p></div></div><div class="profile-links"><button class="profile-link" type="button" @click="router.push('/profile/addresses')"><span class="link-icon"><el-icon><Location /></el-icon></span><span><strong>地址管理</strong><small>新增、编辑与设置默认地址</small></span><span class="link-arrow">›</span></button><button class="profile-link" type="button" @click="router.push('/cart')"><span class="link-icon"><el-icon><ShoppingCart /></el-icon></span><span><strong>购物车</strong><small>查看已选商品和实时金额</small></span><span class="link-arrow">›</span></button></div></section>
    <el-dialog v-model="editing" title="编辑资料" width="min(520px, calc(100% - 28px))" :close-on-click-modal="false" @closed="clearPreview"><el-form label-position="top" @submit.prevent="saveProfile"><div class="avatar-editor"><el-avatar :size="88" :src="previewUrl || profile.avatarUrl"><el-icon><Avatar /></el-icon></el-avatar><div><el-button :icon="Camera" @click="chooseAvatar">选择头像</el-button><p>本地中心裁剪为 1:1，压缩为 WebP；确认保存时只上传一次。</p><input ref="fileInput" class="file-input" type="file" accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp" @change="handleFile" /></div></div><el-form-item label="账号昵称"><el-input v-model.trim="form.nickname" maxlength="64" /></el-form-item><el-form-item label="真实姓名"><el-input :model-value="realNameCardValue" readonly /><p class="field-note">学籍资料由管理员维护</p></el-form-item><el-form-item label="登录手机号"><el-input :model-value="phone" readonly /></el-form-item><div class="dialog-actions"><el-button @click="closeEditor">取消</el-button><el-button native-type="submit" type="primary" :loading="saving">确认保存</el-button></div></el-form></el-dialog>
  </section>
</template>

<style scoped>
.profile-hero { display: flex; align-items: center; gap: 18px; padding: 26px; border: 1px solid #d9e6f9; border-radius: var(--qh-radius-lg); background: linear-gradient(135deg, #f0f6ff, #fff); }.profile-avatar { flex: none; color: var(--qh-primary); background: #fff; border: 3px solid #d5e5fb; }.profile-main { min-width: 0; }.profile-main h2 { margin: 0; font-size: 24px; }.profile-main p { margin: 6px 0 9px; color: var(--qh-text-secondary); }.tag-row { display: flex; flex-wrap: wrap; gap: 8px; }.profile-hero > .el-button { margin-left: auto; }.profile-summary { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; margin: 18px 0 12px; }.profile-summary article { min-width: 0; padding: 16px; border: 1px solid var(--qh-border); border-radius: 14px; background: #fff; }.profile-summary .label, .profile-summary small { display: block; color: var(--qh-text-secondary); font-size: 13px; }.profile-summary strong { display: block; overflow: hidden; margin: 7px 0 5px; text-overflow: ellipsis; white-space: nowrap; }.address-summary .el-button { padding: 0; }.profile-load-note, .field-note { margin: 0 0 28px; color: var(--qh-text-secondary); font-size: 13px; line-height: 1.6; }.field-note { margin-top: 6px; }.profile-links { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }.profile-link { display: grid; grid-template-columns: 46px minmax(0, 1fr) auto; gap: 13px; align-items: center; width: 100%; padding: 18px; border: 1px solid var(--qh-border); border-radius: var(--qh-radius-lg); color: var(--qh-text); background: #fff; box-shadow: var(--qh-shadow-card); cursor: pointer; text-align: left; transition: transform .16s ease, border-color .16s ease; }.profile-link:hover { border-color: #bdd2f3; transform: translateY(-2px); }.link-icon { display: grid; place-items: center; width: 46px; height: 46px; border-radius: 14px; color: var(--qh-primary); background: var(--qh-primary-soft); font-size: 22px; }.profile-link strong, .profile-link small { display: block; }.profile-link small { margin-top: 5px; color: var(--qh-text-secondary); line-height: 1.5; }.link-arrow { color: var(--qh-primary); font-size: 26px; font-weight: 300; }.avatar-editor { display: flex; align-items: center; gap: 16px; margin-bottom: 20px; }.avatar-editor p { max-width: 300px; margin: 8px 0 0; color: var(--qh-text-secondary); font-size: 12px; line-height: 1.6; }.file-input { display: none; }.dialog-actions { display: flex; justify-content: flex-end; gap: 10px; } @media (max-width: 980px) { .profile-summary { grid-template-columns: repeat(2, minmax(0, 1fr)); } } @media (max-width: 720px) { .profile-summary, .profile-links { grid-template-columns: 1fr; }.profile-hero { align-items: flex-start; flex-wrap: wrap; padding: 22px; }.profile-hero > .el-button { margin-left: 0; }.address-summary strong { white-space: normal; } } @media (max-width: 440px) { .avatar-editor { align-items: flex-start; }.dialog-actions { flex-direction: column-reverse; }.dialog-actions .el-button { width: 100%; margin: 0; } }
</style>

<style scoped>
.sign-card{display:grid;grid-template-columns:1fr auto;gap:14px;margin:18px 0;padding:18px;border:1px solid #d9e6f9;border-radius:14px;background:#f8fbff}.sign-card h2{margin:0;font-size:18px}.sign-card p{margin:7px 0 0;color:var(--qh-text-secondary);font-size:13px}.sign-calendar{grid-column:1/-1;display:flex;flex-wrap:wrap;gap:6px}.sign-calendar span{display:grid;place-items:center;width:28px;height:28px;border-radius:50%;color:#718198;background:#edf2f8;font-size:12px}.sign-calendar span.signed{color:#fff;background:var(--qh-primary)}
</style>
