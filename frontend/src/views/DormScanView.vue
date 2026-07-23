<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import QrScanner from 'qr-scanner'
import { Camera, Check, EditPen, Refresh, Upload, VideoCamera } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { confirmDormCheckIn, getStudentProfile, resolveDormQr, saveStudentProfile } from '../api/student-dorm'
import { getCampuses } from '../api/address'
import PageHeader from '../components/PageHeader.vue'

const QR_PATTERN = /^QH-DORM-V1:[0-9a-fA-F]{64}$/
const video = ref(null)
const imageInput = ref(null)
const profile = ref(null)
const resolved = ref(null)
const acceptedQrContent = ref('')
const manualQrContent = ref('')
const loading = ref(false)
const imageLoading = ref(false)
const saving = ref(false)
const checking = ref(false)
const cameraActive = ref(false)
const campuses = ref([])
const campusLoading = ref(false)
const campusError = ref('')
const manualOpen = ref([])
const scanStatus = reactive({ state: 'waiting', message: '等待扫码：可打开摄像头、选择本地图片，或展开高级手动输入。' })
const form = reactive({ realName: '', studentNo: '', campusId: null, collegeName: '', majorName: '', className: '', contactPhone: '' })
let scanner = null
let resolving = false

function syncForm(value) { Object.keys(form).forEach((key) => { form[key] = value?.[key] || '' }) }
function setScanState(state, message) { scanStatus.state = state; scanStatus.message = message }
function statusType() { return { waiting: 'info', starting: 'warning', recognizing: 'warning', identified: 'success', verifying: 'warning', success: 'success', failed: 'error' }[scanStatus.state] || 'info' }
function statusTitle() { return { waiting: '等待扫码', starting: '摄像头启动中', recognizing: '正在识别', identified: '已识别二维码', verifying: '正在核验宿舍', success: '解析成功', failed: '解析失败' }[scanStatus.state] || '扫码状态' }
function assetSetText(value) { return { AVAILABLE: '套装可用', OCCUPIED: '套装已占用', MAINTENANCE: '套装维护中', RETIRED: '套装已停用' }[value] || '套装状态未知' }
function healthText(value) { return { NORMAL: '五件资产均正常', INCOMPLETE: '固定资产不完整', REPAIR: '存在维修资产', SCRAPPED: '存在报废资产' }[value] || '资产健康状态未知' }

async function loadProfile() {
  try { profile.value = await getStudentProfile(); syncForm(profile.value) } catch (error) { if (!error?.__qingheMessageShown) ElMessage.error(error?.message || '学生资料加载失败') }
}
async function loadCampuses() {
  campusLoading.value = true
  campusError.value = ''
  try { campuses.value = await getCampuses() } catch (error) { campusError.value = error?.message || '校区加载失败' } finally { campusLoading.value = false }
}

function invalidQrMessage(value) {
  if (!value?.trim()) return '未读取到二维码内容，请重试或使用高级手动输入。'
  return '二维码格式不正确，请确认使用本系统生成的宿舍资产二维码。'
}

async function resolveContent(value, source) {
  const content = value?.trim()
  if (!QR_PATTERN.test(content || '')) {
    acceptedQrContent.value = ''
    resolved.value = null
    setScanState('failed', invalidQrMessage(content))
    return
  }
  if (resolving) return
  resolving = true
  acceptedQrContent.value = content
  manualQrContent.value = ''
  resolved.value = null
  setScanState('identified', source === 'camera' ? '已识别床位资产二维码，正在核验宿舍信息。' : '二维码已识别，正在核验宿舍信息。')
  loading.value = true
  try {
    setScanState('verifying', '正在核验宿舍信息。')
    const data = await resolveDormQr(content)
    resolved.value = data
    setScanState('success', data.available ? '宿舍信息核验通过，可在确认后办理入住。' : (data.unavailableReason || '宿舍信息已解析，但当前不可入住。'))
  } catch (error) {
    acceptedQrContent.value = ''
    resolved.value = null
    setScanState('failed', error?.message || '二维码解析失败，请使用有效的床位资产二维码。')
  } finally {
    loading.value = false
    resolving = false
  }
}

function chooseImage() { imageInput.value?.click() }
function isSupportedImage(file) { return ['image/png', 'image/jpeg', 'image/webp'].includes(file?.type) }
function imageDecodeMessage(error) {
  const message = String(error?.message || error || '')
  if (/no qr code|not found/i.test(message)) return '未在图片中识别到二维码，请使用清晰、完整的二维码图片。'
  return '图片二维码识别失败，请确认图片未损坏且包含清晰、完整的二维码。'
}
async function decodeImage(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!isSupportedImage(file)) {
    setScanState('failed', '图片格式不支持，请选择 PNG、JPG、JPEG 或 WEBP 格式的二维码图片。')
    return
  }
  imageLoading.value = true
  setScanState('recognizing', '正在识别二维码。')
  try {
    const result = await QrScanner.scanImage(file, { returnDetailedScanResult: true })
    await resolveContent(result.data, 'image')
  } catch (error) {
    acceptedQrContent.value = ''
    resolved.value = null
    setScanState('failed', imageDecodeMessage(error))
  } finally { imageLoading.value = false }
}

function releaseVideoTracks() {
  const mediaStream = video.value?.srcObject
  if (mediaStream?.getTracks) mediaStream.getTracks().forEach((track) => track.stop())
  if (video.value) video.value.srcObject = null
}
function stopCamera() {
  if (scanner) { scanner.stop(); scanner.destroy(); scanner = null }
  releaseVideoTracks()
  cameraActive.value = false
}
function cameraErrorMessage(error) {
  switch (error?.name) {
    case 'NotAllowedError': case 'SecurityError': return '您已拒绝摄像头权限，请在浏览器设置中允许后重试。'
    case 'NotFoundError': case 'DevicesNotFoundError': return '未检测到可用摄像头，请使用二维码图片或高级手动输入。'
    case 'NotReadableError': case 'TrackStartError': return '摄像头正被其他程序占用，请关闭占用程序后重试。'
    case 'OverconstrainedError': return '当前设备无法按后置摄像头要求启动，请使用二维码图片或高级手动输入。'
    default: return '视频流启动失败，请检查浏览器权限和摄像头设备后重试。'
  }
}
async function startCamera() {
  stopCamera()
  if (!navigator.mediaDevices?.getUserMedia) {
    setScanState('failed', '浏览器不支持摄像头 API，请使用二维码图片或高级手动输入。')
    return
  }
  setScanState('starting', '摄像头启动中，请在浏览器提示中允许访问摄像头。')
  try {
    if (!(await QrScanner.hasCamera())) {
      setScanState('failed', '未检测到可用摄像头，请使用二维码图片或高级手动输入。')
      return
    }
    scanner = new QrScanner(video.value, async (result) => {
      if (!cameraActive.value || resolving) return
      stopCamera()
      await resolveContent(result.data, 'camera')
    }, {
      preferredCamera: 'environment',
      maxScansPerSecond: 5,
      returnDetailedScanResult: true,
      onDecodeError: () => { if (cameraActive.value) setScanState('recognizing', '正在识别二维码，暂未识别到二维码。') }
    })
    await scanner.start()
    cameraActive.value = true
    setScanState('recognizing', '摄像头已启动，正在识别二维码。')
  } catch (error) {
    stopCamera()
    setScanState('failed', cameraErrorMessage(error))
  }
}
async function submitManual() { await resolveContent(manualQrContent.value, 'manual') }

async function saveProfile() {
  if (!profile.value && !form.campusId) return ElMessage.warning('请选择所属校区')
  saving.value = true
  try { profile.value = await saveStudentProfile({ ...form }); syncForm(profile.value); ElMessage.success('学生资料已保存') } catch (error) { if (!error?.__qingheMessageShown) ElMessage.error(error?.message || '保存失败') } finally { saving.value = false }
}
async function checkIn() {
  if (!resolved.value?.available || !acceptedQrContent.value) return ElMessage.warning(resolved.value?.unavailableReason || '请先解析可入住的二维码')
  if (!profile.value) return ElMessage.warning('请先完成学生实名资料')
  checking.value = true
  try {
    await confirmDormCheckIn(acceptedQrContent.value)
    resolved.value = { ...resolved.value, available: false, assetSetStatus: 'OCCUPIED', unavailableReason: '已完成入住' }
    acceptedQrContent.value = ''
    setScanState('success', '入住确认成功。')
    ElMessage.success('入住确认成功')
  } catch (error) { setScanState('failed', error?.message || '入住失败，请重新核验二维码。') } finally { checking.value = false }
}

onMounted(() => { loadProfile(); loadCampuses() })
onBeforeUnmount(stopCamera)
</script>

<template>
  <section class="page-shell">
    <PageHeader title="扫码入住" description="先核对宿舍信息，再由您确认入住" />
    <div class="grid">
      <section class="card">
        <h2><el-icon><Camera /></el-icon> 扫描二维码</h2>
        <video ref="video" autoplay muted playsinline class="camera" :class="{ active: cameraActive }" />
        <el-alert class="scan-status" :title="statusTitle()" :description="scanStatus.message" :type="statusType()" :closable="false" show-icon />
        <input ref="imageInput" class="file-input" type="file" accept="image/png,image/jpeg,image/webp" @change="decodeImage" />
        <div class="actions">
          <el-button :icon="VideoCamera" :loading="scanStatus.state === 'starting'" @click="startCamera">打开摄像头</el-button>
          <el-button v-if="cameraActive" @click="stopCamera">关闭摄像头</el-button>
          <el-button :icon="Upload" :loading="imageLoading" @click="chooseImage">选择二维码图片</el-button>
        </div>
        <el-collapse v-model="manualOpen" class="qr-form">
          <el-collapse-item title="高级兜底：手动输入二维码内容" name="manual">
            <el-form label-position="top" @submit.prevent="submitManual">
              <el-form-item label="二维码内容"><el-input v-model.trim="manualQrContent" type="textarea" :rows="3" placeholder="QH-DORM-V1:…" /></el-form-item>
              <el-button native-type="submit" type="primary" :loading="loading" :icon="Refresh">解析二维码</el-button>
            </el-form>
          </el-collapse-item>
        </el-collapse>
      </section>
      <section class="card">
        <h2>宿舍确认信息</h2>
        <el-empty v-if="!resolved" description="解析后显示校区、楼栋、资产套装与入住条件" />
        <template v-else>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="校区">{{ resolved.campusName }}</el-descriptions-item>
            <el-descriptions-item label="楼栋">{{ resolved.buildingName }}</el-descriptions-item>
            <el-descriptions-item label="寝室">{{ resolved.roomNo }} 室</el-descriptions-item>
            <el-descriptions-item label="床位">{{ resolved.bedNo }} 床</el-descriptions-item>
            <el-descriptions-item label="资产套装编号">{{ resolved.assetSetCode }}</el-descriptions-item>
            <el-descriptions-item label="套装可用状态">{{ assetSetText(resolved.assetSetStatus) }}</el-descriptions-item>
            <el-descriptions-item label="资产健康状态">{{ healthText(resolved.assetHealthStatus) }}</el-descriptions-item>
            <el-descriptions-item label="五件固定资产">{{ resolved.assetNames?.join('、') || '暂无明细' }}</el-descriptions-item>
            <el-descriptions-item label="是否允许入住"><el-tag :type="resolved.available ? 'success' : 'danger'">{{ resolved.available ? '允许入住' : '暂不允许入住' }}</el-tag></el-descriptions-item>
          </el-descriptions>
          <p v-if="!resolved.available" class="hint">{{ resolved.unavailableReason }}</p>
          <el-button class="confirm" type="primary" :icon="Check" :disabled="!resolved.available || !profile" :loading="checking" @click="checkIn">确认入住</el-button>
          <p v-if="!profile" class="hint">请先完成下方学生实名资料，才可确认入住。</p>
        </template>
      </section>
    </div>
    <section class="card profile-card">
      <div class="section-title"><div><h2>学生实名资料</h2><p>首次填写后，实名和学籍资料将由管理员维护；您可修改联系电话。</p></div><el-tag :type="profile ? 'success' : 'warning'">{{ profile ? '已建档' : '待填写' }}</el-tag></div>
      <el-form label-position="top" class="profile-form" @submit.prevent="saveProfile">
        <el-form-item label="真实姓名" required><el-input v-model.trim="form.realName" :readonly="Boolean(profile)" maxlength="50" /></el-form-item>
        <el-form-item label="学号" required><el-input v-model.trim="form.studentNo" :readonly="Boolean(profile)" maxlength="32" /></el-form-item>
        <el-form-item label="所属校区" required><el-input v-if="profile" :model-value="profile.campusName || '校区信息已归档'" readonly /><template v-else><el-select v-model="form.campusId" class="full-width" placeholder="请选择启用校区" :loading="campusLoading" :disabled="campusLoading"><el-option v-for="item in campuses" :key="item.id" :label="item.campusName" :value="item.id" /><template #empty><el-empty v-if="!campusLoading && !campusError" description="暂无启用校区" :image-size="56" /></template></el-select><div v-if="campusError" class="field-error">{{ campusError }} <el-button link type="primary" @click="loadCampuses">重试</el-button></div></template></el-form-item>
        <el-form-item label="学院" required><el-input v-model.trim="form.collegeName" :readonly="Boolean(profile)" maxlength="100" /></el-form-item>
        <el-form-item label="专业" required><el-input v-model.trim="form.majorName" :readonly="Boolean(profile)" maxlength="100" /></el-form-item>
        <el-form-item label="班级" required><el-input v-model.trim="form.className" :readonly="Boolean(profile)" maxlength="100" /></el-form-item>
        <el-form-item label="联系电话" required><el-input v-model.trim="form.contactPhone" maxlength="20" /></el-form-item>
        <el-button native-type="submit" type="primary" :icon="EditPen" :loading="saving">{{ profile ? '保存联系电话' : '保存学生资料' }}</el-button>
      </el-form>
    </section>
  </section>
</template>

<style scoped>
.grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:18px}.card{padding:22px;border:1px solid var(--qh-border);border-radius:var(--qh-radius-lg);background:#fff;box-shadow:var(--qh-shadow-card)}h2{display:flex;align-items:center;gap:8px;margin:0 0 14px;font-size:18px}.camera{width:100%;min-height:180px;border-radius:12px;background:#152235;object-fit:cover}.camera:not(.active){display:none}.scan-status{margin:0 0 14px}.actions{display:flex;flex-wrap:wrap;gap:10px}.hint{color:var(--qh-text-secondary);font-size:13px;line-height:1.6}.qr-form{margin-top:18px}.file-input{display:none}.confirm{margin-top:18px}.profile-card{margin-top:18px}.section-title{display:flex;justify-content:space-between;gap:14px}.section-title p{margin:-7px 0 15px;color:var(--qh-text-secondary);font-size:13px}.profile-form{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:0 16px}.profile-form .el-button{grid-column:1/-1;justify-self:start}@media(max-width:720px){.grid,.profile-form{grid-template-columns:1fr}}
</style>
