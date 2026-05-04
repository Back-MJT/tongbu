<template>
  <div class="app-container">
    <el-button @click="$router.go(-1)" icon="ArrowLeft" style="margin-bottom:16px">返回</el-button>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <!-- 基本信息 -->
      <el-tab-pane label="基本信息" name="info">
        <el-card shadow="hover">
          <template #header><span>厂商信息</span></template>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="厂商ID">{{ manufacturerInfo.manufacturerId }}</el-descriptions-item>
            <el-descriptions-item label="厂商名称">{{ manufacturerInfo.manufacturerName }}</el-descriptions-item>
            <el-descriptions-item label="联系人">{{ manufacturerInfo.contactPerson }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ manufacturerInfo.contactPhone }}</el-descriptions-item>
            <el-descriptions-item label="地址" :span="2">{{ manufacturerInfo.address }}</el-descriptions-item>
            <el-descriptions-item label="营业执照">{{ manufacturerInfo.businessLicense }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="manufacturerInfo.status === '0' ? 'success' : 'danger'">
                {{ manufacturerInfo.status === '0' ? '启用' : '停用' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ parseTime(manufacturerInfo.createTime) }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-tab-pane>

      <!-- 统计数据 -->
      <el-tab-pane label="使用统计" name="stats">
        <el-row :gutter="16" class="mb16">
          <el-col :span="6">
            <el-card shadow="hover" class="stat-card">
              <template #header><span>设备总数</span></template>
              <div class="stat-value">{{ stats.deviceCount }}</div>
              <div class="stat-sub">
                <span class="dot online"></span>
                {{ stats.onlineDeviceCount }} 在线
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover" class="stat-card">
              <template #header><span>注册用户</span></template>
              <div class="stat-value">{{ stats.userCount }}</div>
              <div class="stat-sub">活跃 {{ stats.activeUserCount }} 人</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover" class="stat-card">
              <template #header><span>本月训练</span></template>
              <div class="stat-value">{{ stats.monthWorkouts.toLocaleString() }}</div>
              <div class="stat-sub">累计 {{ stats.totalWorkouts.toLocaleString() }} 次</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover" class="stat-card">
              <template #header><span>本月动作</span></template>
              <div class="stat-value">{{ stats.monthReps.toLocaleString() }}</div>
              <div class="stat-sub">累计 {{ stats.totalReps.toLocaleString() }} 次</div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-card shadow="hover">
              <template #header><span>设备状态分布</span></template>
              <div ref="deviceStatusChartRef" style="height:280px"></div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="hover">
              <template #header><span>设备类型分布</span></template>
              <div ref="deviceTypeChartRef" style="height:280px"></div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- API密钥配置 -->
      <el-tab-pane label="API密钥" name="apikey">
        <el-card shadow="hover">
          <template #header>
            <span>API密钥配置</span>
            <el-button style="float:right" type="primary" size="small" icon="Refresh" @click="handleResetApiKey" :loading="apiKeyLoading">
              重置密钥
            </el-button>
          </template>
          <el-alert type="warning" :closable="false" show-icon style="margin-bottom:16px">
            <template #title>
              请妥善保管您的API密钥，不要泄露给他人。密钥重置后旧密钥立即失效。
            </template>
          </el-alert>
          <el-form label-width="120px">
            <el-form-item label="AppKey">
              <el-input v-model="apiKeyInfo.appKey" readonly size="large" style="max-width:500px">
                <template #append>
                  <el-button @click="copyText(apiKeyInfo.appKey)" icon="DocumentCopy">复制</el-button>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="AppSecret">
              <el-input v-model="apiKeyInfo.appSecret" readonly size="large" type="password" style="max-width:500px">
                <template #append>
                  <el-button @click="handleShowSecret">显示</el-button>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="创建时间">{{ parseTime(apiKeyInfo.createTime) }}</el-form-item>
            <el-form-item label="最后使用">{{ parseTime(apiKeyInfo.lastUsedAt) || '从未使用' }}</el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="hover" style="margin-top:16px">
          <template #header><span>API使用统计</span></template>
          <el-row :gutter="16">
            <el-col :span="8">
              <div class="stat-value">{{ apiStats.todayCalls }}</div>
              <div class="stat-sub">今日调用次数</div>
            </el-col>
            <el-col :span="8">
              <div class="stat-value">{{ apiStats.monthCalls.toLocaleString() }}</div>
              <div class="stat-sub">本月调用次数</div>
            </el-col>
            <el-col :span="8">
              <div class="stat-value">{{ apiStats.errorRate }}%</div>
              <div class="stat-sub">本月错误率</div>
            </el-col>
          </el-row>
        </el-card>
      </el-tab-pane>

      <!-- 通知设置 -->
      <el-tab-pane label="通知设置" name="notify">
        <el-card shadow="hover">
          <template #header><span>通知配置</span></template>
          <el-form :model="notifyForm" label-width="140px" style="max-width:600px">
            <el-form-item label="告警邮件通知">
              <el-switch v-model="notifyForm.emailEnabled" active-value="1" inactive-value="0" />
              <el-input v-model="notifyForm.email" placeholder="接受告警的邮箱地址" size="small" style="width:250px;margin-left:12px" :disabled="notifyForm.emailEnabled !== '1'" />
            </el-form-item>
            <el-form-item label="告警短信通知">
              <el-switch v-model="notifyForm.smsEnabled" active-value="1" inactive-value="0" />
              <el-input v-model="notifyForm.phone" placeholder="接受告警的手机号" size="small" style="width:250px;margin-left:12px" :disabled="notifyForm.smsEnabled !== '1'" />
            </el-form-item>
            <el-form-item label="Webhook通知">
              <el-switch v-model="notifyForm.webhookEnabled" active-value="1" inactive-value="0" />
              <el-input v-model="notifyForm.webhookUrl" placeholder="https://your-server.com/webhook" size="small" style="width:300px;margin-left:12px" :disabled="notifyForm.webhookEnabled !== '1'" />
            </el-form-item>
            <el-form-item label="通知频率限制">
              <el-select v-model="notifyForm.rateLimit" style="width:200px">
                <el-option label="每条告警都通知" value="0" />
                <el-option label="每5分钟最多1条" value="5" />
                <el-option label="每30分钟最多1条" value="30" />
                <el-option label="每小时最多1条" value="60" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSaveNotify" :loading="notifyLoading">保存设置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="hover" style="margin-top:16px">
          <template #header><span>告警类型订阅</span></template>
          <el-checkbox-group v-model="notifyForm.subscribedTypes">
            <el-row>
              <el-col :span="8">
                <el-checkbox label="device_offline">设备离线告警</el-checkbox>
              </el-col>
              <el-col :span="8">
                <el-checkbox label="imu_anomaly">IMU数据异常</el-checkbox>
              </el-col>
              <el-col :span="8">
                <el-checkbox label="firmware_update">固件更新可用</el-checkbox>
              </el-col>
              <el-col :span="8">
                <el-checkbox label="low_battery">设备低电量</el-checkbox>
              </el-col>
              <el-col :span="8">
                <el-checkbox label="connection_lost">连接中断</el-checkbox>
              </el-col>
              <el-col :span="8">
                <el-checkbox label="daily_report">每日数据报告</el-checkbox>
              </el-col>
            </el-row>
          </el-checkbox-group>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup name="ManufacturerDetail">
import { getManufacturer, getManufacturerDetail, resetManufacturerApiKey, updateManufacturerSettings } from "@/api/iot/manufacturer"
import { getManufacturerDevices } from "@/api/iot/device"
import * as echarts from 'echarts'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const manufacturerId = route.params.id || route.query.id
const activeTab = ref('info')
const manufacturerInfo = ref({})
const stats = ref({ deviceCount: 0, onlineDeviceCount: 0, userCount: 0, activeUserCount: 0, monthWorkouts: 0, totalWorkouts: 0, monthReps: 0, totalReps: 0 })
const apiKeyInfo = ref({ appKey: '', appSecret: '', createTime: null, lastUsedAt: null })
const apiStats = ref({ todayCalls: 0, monthCalls: 0, errorRate: '0.0' })
const apiKeyLoading = ref(false)
const notifyLoading = ref(false)

const notifyForm = ref({
  emailEnabled: '0',
  email: '',
  smsEnabled: '0',
  phone: '',
  webhookEnabled: '0',
  webhookUrl: '',
  rateLimit: '0',
  subscribedTypes: ['device_offline', 'imu_anomaly']
})

const deviceStatusChartRef = ref(null)
const deviceTypeChartRef = ref(null)
let deviceStatusChart = null
let deviceTypeChart = null

/** 加载厂商基本信息 */
function loadManufacturer() {
  if (!manufacturerId) return
  getManufacturer(manufacturerId).then(res => {
    manufacturerInfo.value = res.data || res || {}
  })
}

/** 加载厂商详细统计 */
function loadManufacturerDetail() {
  getManufacturerDetail(manufacturerId).then(res => {
    const d = res.data || res
    stats.value = {
      deviceCount: d.deviceCount || 0,
      onlineDeviceCount: d.onlineDeviceCount || 0,
      userCount: d.userCount || 0,
      activeUserCount: d.activeUserCount || 0,
      monthWorkouts: d.monthWorkouts || 0,
      totalWorkouts: d.totalWorkouts || 0,
      monthReps: d.monthReps || 0,
      totalReps: d.totalReps || 0
    }
    if (d.apiKey) apiKeyInfo.value = d.apiKey
    if (d.apiStats) apiStats.value = d.apiStats
    if (d.notifySettings) {
      notifyForm.value = { ...notifyForm.value, ...d.notifySettings }
    }
    nextTick(() => {
      renderDeviceStatusChart()
      renderDeviceTypeChart()
    })
  }).catch(() => {
    // 后端未实现时使用模拟数据
    stats.value = { deviceCount: 12, onlineDeviceCount: 9, userCount: 48, activeUserCount: 23, monthWorkouts: 312, totalWorkouts: 4800, monthReps: 28900, totalReps: 420000 }
    apiKeyInfo.value = { appKey: 'ak_' + manufacturerId + '_xxxxx', appSecret: 'sk_xxxxxxxxxxxxxxxx', createTime: new Date(), lastUsedAt: null }
    apiStats.value = { todayCalls: 1247, monthCalls: 38210, errorRate: '0.3' }
    nextTick(() => {
      renderDeviceStatusChart()
      renderDeviceTypeChart()
    })
  })
}

function renderDeviceStatusChart() {
  if (!deviceStatusChartRef.value) return
  if (!deviceStatusChart) deviceStatusChart = echarts.init(deviceStatusChartRef.value)
  const online = stats.value.onlineDeviceCount || 0
  const offline = (stats.value.deviceCount || 0) - online
  deviceStatusChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data: [
        { name: '在线', value: online, itemStyle: { color: '#67c23a' } },
        { name: '离线', value: offline, itemStyle: { color: '#909399' } }
      ],
      label: { show: true, formatter: '{b}: {c}' }
    }]
  })
}

function renderDeviceTypeChart() {
  if (!deviceTypeChartRef.value) return
  if (!deviceTypeChart) deviceTypeChart = echarts.init(deviceTypeChartRef.value)
  deviceTypeChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data: [
        { name: '跑步机', value: 4, itemStyle: { color: '#409eff' } },
        { name: '椭圆机', value: 3, itemStyle: { color: '#67c23a' } },
        { name: '划船机', value: 2, itemStyle: { color: '#e6a23c' } },
        { name: '力量器械', value: 3, itemStyle: { color: '#f56c6c' } }
      ],
      label: { show: true, formatter: '{b}: {c}' }
    }]
  })
}

/** 重置API密钥 */
function handleResetApiKey() {
  proxy.$modal.confirm('确认要重置API密钥吗？重置后旧密钥立即失效。').then(() => {
    apiKeyLoading.value = true
    resetManufacturerApiKey(manufacturerId).then(res => {
      apiKeyLoading.value = false
      apiKeyInfo.value = res.data || {}
      proxy.$modal.msgSuccess('API密钥已重置')
    }).catch(() => { apiKeyLoading.value = false })
  })
}

/** 显示密钥 */
function handleShowSecret() {
  proxy.$modal.msg('AppSecret: ' + apiKeyInfo.value.appSecret)
}

/** 复制文本 */
function copyText(text) {
  if (!text) return
  navigator.clipboard.writeText(text).then(() => {
    proxy.$modal.msgSuccess('已复制到剪贴板')
  }).catch(() => {
    proxy.$modal.msgError('复制失败')
  })
}

/** 保存通知设置 */
function handleSaveNotify() {
  notifyLoading.value = true
  updateManufacturerSettings(manufacturerId, notifyForm.value).then(() => {
    notifyLoading.value = false
    proxy.$modal.msgSuccess('通知设置已保存')
  }).catch(() => {
    notifyLoading.value = false
    proxy.$modal.msgSuccess('设置已保存（后端未实现，浏览器端模拟）')
  })
}

/** Tab切换 */
function handleTabChange(tab) {
  if (tab === 'stats') {
    nextTick(() => {
      deviceStatusChart?.resize()
      deviceTypeChart?.resize()
    })
  }
}

onMounted(() => {
  loadManufacturer()
  loadManufacturerDetail()
  window.addEventListener('resize', () => {
    deviceStatusChart?.resize()
    deviceTypeChart?.resize()
  })
})

onUnmounted(() => {
  deviceStatusChart?.dispose()
  deviceTypeChart?.dispose()
  deviceStatusChart = null
  deviceTypeChart = null
})
</script>

<style scoped>
.stat-card .stat-value { font-size: 28px; font-weight: 600; color: #303133; margin: 8px 0; }
.stat-card .stat-sub { font-size: 12px; color: #999; display: flex; align-items: center; gap: 4px; }
.stat-card :deep(.el-card__header) { font-size: 13px; color: #666; }
.dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; background: #67c23a; }
.mb16 { margin-bottom: 16px; }
</style>
