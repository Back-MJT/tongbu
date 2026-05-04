<template>
  <div class="app-container">
    <!-- 搜索 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="设备编号" prop="deviceCode">
        <el-input v-model="queryParams.deviceCode" placeholder="请输入设备编号" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="时间范围" style="width: 320px">
        <el-date-picker v-model="dateRange" type="datetimerange" range-separator="-" start-placeholder="开始时间" end-placeholder="结束时间"
          value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" @change="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Refresh" @click="loadHistory" v-hasPermi="['iot:imu:query']">刷新数据</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="loadHistory"></right-toolbar>
    </el-row>

    <!-- 实时数据 -->
    <el-row :gutter="16" class="mb16">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span>最新姿态角</span></template>
          <div v-if="latestImu" style="text-align:center">
            <el-row :gutter="8">
              <el-col :span="8">
                <div class="imu-val">{{ latestImu.pitch != null ? latestImu.pitch.toFixed(1) + '°' : '-' }}</div>
                <div class="imu-label">俯仰角(Pitch)</div>
              </el-col>
              <el-col :span="8">
                <div class="imu-val">{{ latestImu.roll != null ? latestImu.roll.toFixed(1) + '°' : '-' }}</div>
                <div class="imu-label">横滚角(Roll)</div>
              </el-col>
              <el-col :span="8">
                <div class="imu-val">{{ latestImu.yaw != null ? latestImu.yaw.toFixed(1) + '°' : '-' }}</div>
                <div class="imu-label">偏航角(Yaw)</div>
              </el-col>
            </el-row>
          </div>
          <el-empty v-else description="暂无实时数据" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span>加速度 (m/s²)</span></template>
          <div v-if="latestImu" style="text-align:center">
            <el-row :gutter="8">
              <el-col :span="8">
                <div class="imu-val">{{ accelVal(latestImu.accelX) }}</div>
                <div class="imu-label">X轴</div>
              </el-col>
              <el-col :span="8">
                <div class="imu-val">{{ accelVal(latestImu.accelY) }}</div>
                <div class="imu-label">Y轴</div>
              </el-col>
              <el-col :span="8">
                <div class="imu-val">{{ accelVal(latestImu.accelZ) }}</div>
                <div class="imu-label">Z轴</div>
              </el-col>
            </el-row>
          </div>
          <el-empty v-else description="暂无实时数据" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span>角速度 (°/s)</span></template>
          <div v-if="latestImu" style="text-align:center">
            <el-row :gutter="8">
              <el-col :span="8">
                <div class="imu-val">{{ gyroVal(latestImu.gyroX) }}</div>
                <div class="imu-label">X轴</div>
              </el-col>
              <el-col :span="8">
                <div class="imu-val">{{ gyroVal(latestImu.gyroY) }}</div>
                <div class="imu-label">Y轴</div>
              </el-col>
              <el-col :span="8">
                <div class="imu-val">{{ gyroVal(latestImu.gyroZ) }}</div>
                <div class="imu-label">Z轴</div>
              </el-col>
            </el-row>
          </div>
          <el-empty v-else description="暂无实时数据" />
        </el-card>
      </el-col>
    </el-row>

    <!-- IMU 曲线图 -->
    <el-card shadow="hover" class="mb16">
      <template #header>
        <span>IMU时序数据</span>
        <el-select v-model="queryParams.deviceCode" placeholder="选择设备" style="float:right;width:200px" @change="loadHistory" clearable>
          <el-option v-for="d in deviceOptions" :key="d.deviceCode" :label="d.deviceName + '(' + d.deviceCode + ')'" :value="d.deviceCode" />
        </el-select>
      </template>
      <div ref="imuChartRef" style="height:400px"></div>
    </el-card>

    <!-- 历史数据表格 -->
    <el-card shadow="hover">
      <template #header><span>历史记录 (最近100条)</span></template>
      <el-table v-loading="loading" :data="imuHistory" max-height="400">
        <el-table-column label="时间" align="center" key="createTime" prop="createTime" width="180" />
        <el-table-column label="设备编号" align="center" key="deviceCode" prop="deviceCode" width="140" />
        <el-table-column label="AccelX" align="center" prop="accelX" width="100" />
        <el-table-column label="AccelY" align="center" prop="accelY" width="100" />
        <el-table-column label="AccelZ" align="center" prop="accelZ" width="100" />
        <el-table-column label="GyroX" align="center" prop="gyroX" width="100" />
        <el-table-column label="GyroY" align="center" prop="gyroY" width="100" />
        <el-table-column label="GyroZ" align="center" prop="gyroZ" width="100" />
        <el-table-column label="Pitch" align="center" prop="pitch" width="80" />
        <el-table-column label="Roll" align="center" prop="roll" width="80" />
        <el-table-column label="Yaw" align="center" prop="yaw" width="80" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup name="ImuData">
import { getLatestImuData, getImuHistory, getImuStatus, getMqttHealth } from "@/api/iot/imuData"
import { listDevice } from "@/api/iot/device"
import * as echarts from 'echarts'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const showSearch = ref(true)
const queryParams = reactive({ deviceCode: '', limit: 100 })
const dateRange = ref([])
const latestImu = ref(null)
const imuHistory = ref([])
const deviceOptions = ref([])

const imuChartRef = ref(null)
let imuChart = null

function accelVal(v) { return v != null ? v.toFixed(3) : '-' }
function gyroVal(v) { return v != null ? v.toFixed(2) : '-' }

function handleQuery() { loadHistory() }

function resetQuery() {
  proxy.resetForm("queryRef")
  dateRange.value = []
  loadHistory()
}

function loadDevices() {
  listDevice({ pageNum: 1, pageSize: 999 }).then(res => {
    deviceOptions.value = res.rows || []
  })
}

function loadHistory() {
  if (!queryParams.deviceCode) {
    imuHistory.value = []
    return
  }
  loading.value = true
  getImuHistory(queryParams.deviceCode, 100).then(res => {
    loading.value = false
    imuHistory.value = res.data || []
    renderChart(imuHistory.value)
  }).catch(() => { loading.value = false })

  // 加载最新
  getLatestImuData(queryParams.deviceCode).then(res => {
    latestImu.value = res.data || null
  }).catch(() => { latestImu.value = null })
}

function renderChart(records) {
  if (!imuChartRef.value) return
  if (!imuChart) imuChart = echarts.init(imuChartRef.value)

  const times = records.map(r => parseTime(r.timestamp || r.createTime))
  const accelX = records.map(r => r.accelX ?? r.accel_x ?? null)
  const accelY = records.map(r => r.accelY ?? r.accel_y ?? null)
  const accelZ = records.map(r => r.accelZ ?? r.accel_z ?? null)
  const gyroX = records.map(r => r.gyroX ?? r.gyro_x ?? null)
  const gyroY = records.map(r => r.gyroY ?? r.gyro_y ?? null)
  const gyroZ = records.map(r => r.gyroZ ?? r.gyro_z ?? null)

  imuChart.setOption({
    title: { text: 'IMU传感器数据', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '18%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: times, boundaryGap: false },
    yAxis: [
      { type: 'value', name: '加速度 m/s²', scale: true },
      { type: 'value', name: '角速度 °/s', scale: true }
    ],
    series: [
      { name: 'AccelX', type: 'line', data: accelX, smooth: true },
      { name: 'AccelY', type: 'line', data: accelY, smooth: true },
      { name: 'AccelZ', type: 'line', data: accelZ, smooth: true },
      { name: 'GyroX', type: 'line', yAxisIndex: 1, data: gyroX, smooth: true },
      { name: 'GyroY', type: 'line', yAxisIndex: 1, data: gyroY, smooth: true },
      { name: 'GyroZ', type: 'line', yAxisIndex: 1, data: gyroZ, smooth: true }
    ]
  })
}

onMounted(() => {
  loadDevices()
  window.addEventListener('resize', () => { imuChart?.resize() })
})

onUnmounted(() => {
  imuChart?.dispose()
  imuChart = null
})
</script>

<style scoped>
.imu-val { font-size: 20px; font-weight: 600; color: #303133; }
.imu-label { font-size: 12px; color: #909399; margin-top: 4px; }
.mb16 { margin-bottom: 16px; }
</style>
