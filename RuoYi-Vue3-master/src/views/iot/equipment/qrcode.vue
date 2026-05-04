<template>
  <div class="qrcode-page">
    <div class="qr-hero">
      <div>
        <div class="qr-eyebrow">器械二维码</div>
        <h2>扫码绑定与自动连接</h2>
        <p>将二维码打印后贴在器械醒目位置，小程序扫码即可识别器械并进入蓝牙连接流程。</p>
      </div>
      <el-button plain icon="Back" @click="handleBack">返回列表</el-button>
    </div>

    <el-row :gutter="20" class="qr-layout">
      <el-col :xs="24" :lg="14">
        <div class="qr-display">
          <div class="qr-display-header">
            <div>
              <div class="qr-display-title">{{ form.equipmentName || '器械二维码' }}</div>
              <div class="qr-display-subtitle">{{ form.equipmentCode || '加载中' }}</div>
            </div>
            <el-tag :type="form.bluetoothName ? 'success' : 'warning'" size="large">
              {{ form.bluetoothName ? '蓝牙已绑定' : '待绑定蓝牙' }}
            </el-tag>
          </div>

          <div class="qrcode-container">
            <div class="qrcode-wrapper">
              <div class="qr-scan-title">微信扫码使用</div>
              <canvas ref="qrCanvasRef" class="qrcode-image" aria-label="器械二维码"></canvas>
              <div class="qrcode-label">{{ form.equipmentName }}</div>
              <div class="qrcode-code">{{ form.equipmentCode }}</div>
            </div>
          </div>

          <div class="action-buttons">
            <el-button type="primary" size="large" icon="Download" @click="downloadQrCode">下载二维码</el-button>
            <el-button size="large" icon="Printer" @click="printQrCode">打印二维码</el-button>
            <el-button size="large" icon="CopyDocument" @click="copyQrContent">复制内容</el-button>
          </div>
        </div>
      </el-col>

      <el-col :xs="24" :lg="10">
        <div class="qr-info-panel">
          <div class="panel-title">器械信息</div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="器械名称">{{ form.equipmentName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="器械编号">{{ form.equipmentCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="器械类型">{{ form.equipmentType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="蓝牙名称">{{ form.bluetoothName || '未绑定' }}</el-descriptions-item>
          </el-descriptions>

          <div class="panel-title content-title">二维码内容</div>
          <el-input v-model="qrContent" type="textarea" :rows="4" readonly resize="none" />

          <el-alert
            class="qr-tip"
            type="success"
            :closable="false"
            show-icon
            title="建议打印尺寸不小于 6cm x 6cm，避免贴在弯曲或反光区域。"
          />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="EquipmentQrCode">
import { ref, computed, onMounted, watch, nextTick, getCurrentInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getEquipment } from '@/api/iot/equipment'
import QRCode from 'qrcode'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const form = ref({
  equipmentId: null,
  equipmentCode: '',
  equipmentName: '',
  equipmentType: '',
  bluetoothName: '',
  qrContent: ''
})
const qrCanvasRef = ref(null)

const qrContent = computed(() => {
  const savedContent = (form.value.qrContent || '').trim()
  if (savedContent) return savedContent
  if (!form.value.equipmentCode) return ''
  return `xindong://equipment?code=${form.value.equipmentCode}`
})

onMounted(async () => {
  const equipmentId = route.query.equipmentId
  if (equipmentId) {
    await loadEquipment(equipmentId)
  }
})

watch(qrContent, () => {
  renderQrCode()
}, { immediate: true })

async function renderQrCode() {
  await nextTick()
  if (!qrCanvasRef.value || !qrContent.value) return
  try {
    await QRCode.toCanvas(qrCanvasRef.value, qrContent.value, {
      width: 360,
      margin: 3,
      errorCorrectionLevel: 'M',
      color: {
        dark: '#0F172A',
        light: '#FFFFFF'
      }
    })
  } catch (e) {
    proxy.$modal.msgError('二维码生成失败')
  }
}

async function loadEquipment(equipmentId) {
  try {
    const res = await getEquipment(equipmentId)
    form.value = res.data
  } catch (e) {
    proxy.$modal.msgError('加载器械信息失败')
  }
}

function copyQrContent() {
  if (!qrContent.value) return
  if (!navigator.clipboard) {
    proxy.$modal.msgError('当前浏览器不支持自动复制')
    return
  }
  navigator.clipboard.writeText(qrContent.value).then(() => {
    proxy.$modal.msgSuccess('二维码内容已复制')
  }).catch(() => {
    proxy.$modal.msgError('复制失败')
  })
}

async function downloadQrCode() {
  try {
    if (!qrCanvasRef.value || !qrContent.value) return
    const blob = await new Promise((resolve) => qrCanvasRef.value.toBlob(resolve, 'image/png'))
    if (!blob) throw new Error('canvas export failed')
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.download = `${form.value.equipmentCode}_qrcode.png`
    link.href = url
    link.click()
    window.URL.revokeObjectURL(url)
    proxy.$modal.msgSuccess('二维码已下载')
  } catch (e) {
    proxy.$modal.msgError('下载失败')
  }
}

function printQrCode() {
  window.print()
}

function handleBack() {
  router.push('/iot/equipment')
}
</script>

<style scoped>
.qrcode-page {
  padding: 20px;
  min-height: calc(100vh - 84px);
  background:
    linear-gradient(135deg, rgba(46, 125, 255, 0.08), rgba(20, 184, 166, 0.06)),
    #f5f7fb;
}

.qr-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  padding: 26px 30px;
  margin-bottom: 20px;
  color: #fff;
  background: linear-gradient(135deg, #1769ff 0%, #14b8a6 100%);
  border-radius: 8px;
  box-shadow: 0 14px 34px rgba(23, 105, 255, 0.18);
}

.qr-eyebrow {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 700;
  opacity: 0.86;
}

.qr-hero h2 {
  margin: 0;
  font-size: 28px;
  line-height: 1.25;
  letter-spacing: 0;
}

.qr-hero p {
  margin: 10px 0 0;
  color: rgba(255, 255, 255, 0.9);
  font-size: 15px;
}

.qr-layout {
  align-items: stretch;
}

.qr-display,
.qr-info-panel {
  height: 100%;
  background: #fff;
  border: 1px solid #dbe5f2;
  border-radius: 8px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
}

.qr-display {
  padding: 28px;
}

.qr-display-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 20px;
  border-bottom: 1px solid #edf2f7;
}

.qr-display-title {
  color: #1f2d3d;
  font-size: 24px;
  font-weight: 800;
  line-height: 1.3;
}

.qr-display-subtitle {
  margin-top: 6px;
  color: #64748b;
  font-size: 15px;
}

.qrcode-container {
  display: flex;
  justify-content: center;
  padding: 34px 0 26px;
}

.qrcode-wrapper {
  text-align: center;
  padding: 24px 28px 26px;
  background: #fff;
  border: 4px solid #1769ff;
  border-radius: 8px;
  box-shadow: 0 16px 36px rgba(23, 105, 255, 0.18);
}

.qr-scan-title {
  margin-bottom: 14px;
  color: #1769ff;
  font-size: 22px;
  font-weight: 800;
}

.qrcode-image {
  display: block;
  width: 360px;
  height: 360px;
}

.qrcode-label {
  margin-top: 18px;
  font-size: 24px;
  font-weight: 800;
  color: #0f172a;
}

.qrcode-code {
  margin-top: 8px;
  font-size: 17px;
  font-weight: 700;
  color: #475569;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
}

.qr-info-panel {
  padding: 26px;
}

.panel-title {
  margin-bottom: 14px;
  color: #1f2d3d;
  font-size: 18px;
  font-weight: 800;
}

.content-title {
  margin-top: 24px;
}

.qr-tip {
  margin-top: 18px;
}

@media (max-width: 992px) {
  .qr-info-panel {
    margin-top: 20px;
  }
}

@media (max-width: 640px) {
  .qrcode-page {
    padding: 12px;
  }

  .qr-hero {
    align-items: flex-start;
    flex-direction: column;
    padding: 20px;
  }

  .qr-hero h2 {
    font-size: 24px;
  }

  .qrcode-image {
    width: 280px;
    height: 280px;
  }

  .qrcode-wrapper {
    padding: 18px;
  }
}

@media print {
  .qr-hero,
  .qr-info-panel,
  .action-buttons {
    display: none !important;
  }

  .qrcode-page {
    padding: 0;
    background: #fff;
  }

  .qr-display {
    border: none;
    box-shadow: none;
  }

  .qr-display-header {
    display: none;
  }

  .qrcode-wrapper {
    box-shadow: none;
  }
}
</style>
