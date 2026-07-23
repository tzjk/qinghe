<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { completeInitialProfile } from '../api/user'
import { getCampuses, getCampusBuildings } from '../api/address'
import { useUserStore } from '../stores/user'

const router = useRouter()
const store = useUserStore()
const form = reactive({ nickname: '', receiverName: '', receiverPhone: '', campusId: null, buildingId: null, floor: '', roomNo: '', deliveryPoint: '', password: '' })
const campuses = ref([])
const buildings = ref([])
const saving = ref(false)
async function loadCampuses() { try { campuses.value = await getCampuses() } catch (error) { if (!error?.__qingheMessageShown) ElMessage.error(error?.message || '校区加载失败') } }
async function loadBuildings() { form.buildingId = null; buildings.value = form.campusId ? await getCampusBuildings(form.campusId) : [] }
async function submit() {
  if (!form.nickname || !form.receiverName || !form.receiverPhone || !form.campusId || !form.buildingId || (!form.roomNo && !form.deliveryPoint)) return ElMessage.error('请完整填写必填资料和校园地址')
  saving.value = true
  try { store.setProfile(await completeInitialProfile(form)); ElMessage.success('资料已完善'); router.replace('/profile') } catch (error) { if (!error?.__qingheMessageShown) ElMessage.error(error?.message || '保存失败') } finally { saving.value = false }
}
onMounted(loadCampuses)
</script>

<template>
  <section class="complete-page"><el-card class="complete-card" shadow="never"><header><p>FIRST STEP</p><h1>完善首次资料</h1></header><el-form label-position="top" @submit.prevent="submit">
    <section class="form-section"><h2>基本资料</h2><div class="form-grid"><el-form-item label="昵称"><el-input v-model.trim="form.nickname" maxlength="64" /></el-form-item><el-form-item label="收货人"><el-input v-model.trim="form.receiverName" maxlength="64" /></el-form-item><el-form-item label="联系电话"><el-input v-model.trim="form.receiverPhone" maxlength="11" inputmode="numeric" /></el-form-item></div></section>
    <section class="form-section"><h2>校园地址</h2><div class="form-grid"><el-form-item label="校区"><el-select v-model="form.campusId" class="full" @change="loadBuildings"><el-option v-for="item in campuses" :key="item.id" :label="item.campusName" :value="item.id" /></el-select></el-form-item><el-form-item label="楼栋"><el-select v-model="form.buildingId" class="full" :disabled="!form.campusId"><el-option v-for="item in buildings" :key="item.id" :label="item.buildingName" :value="item.id" /></el-select></el-form-item><el-form-item label="房间号"><el-input v-model.trim="form.roomNo" maxlength="64" placeholder="房间号或宿舍号" /></el-form-item><el-form-item label="配送点"><el-input v-model.trim="form.deliveryPoint" maxlength="128" placeholder="与房间号至少填写一项" /></el-form-item></div></section>
    <section class="form-section optional"><h2>可选密码</h2><el-form-item label="密码"><el-input v-model="form.password" type="password" show-password maxlength="32" placeholder="8 至 32 位，含字母和数字" /></el-form-item></section>
    <el-button native-type="submit" type="primary" :loading="saving" class="save-button">保存并进入系统</el-button>
  </el-form></el-card></section>
</template>

<style scoped>
.complete-page { max-width: 760px; margin: 28px auto; padding: 0 20px 28px; }.complete-card { border: 1px solid var(--qh-border); border-radius: 18px; }.complete-card header { margin-bottom: 18px; }.complete-card header p { margin: 0 0 5px; color: var(--qh-primary); font-size: 12px; font-weight: 700; letter-spacing: 1.4px; }.complete-card h1 { margin: 0; font-size: 25px; }.form-section { padding: 16px 0; border-top: 1px solid #edf1f5; }.form-section:first-of-type { border-top: 0; padding-top: 0; }.form-section h2 { margin: 0 0 12px; font-size: 16px; }.form-section.optional { padding-bottom: 10px; }.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 16px; }.full { width: 100%; }.save-button { min-width: 168px; height: 40px; border-radius: 9px; } @media (max-width: 620px) { .complete-page { margin: 16px auto; padding: 0 14px 20px; }.form-grid { grid-template-columns: 1fr; gap: 0; }.save-button { width: 100%; } }
</style>
