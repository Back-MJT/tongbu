<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="传感器名称" prop="deviceName">
        <el-input v-model="queryParams.deviceName" placeholder="请输入传感器名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="传感器编号" prop="deviceCode">
        <el-input v-model="queryParams.deviceCode" placeholder="如 HB-3412" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="蓝牙广播名" prop="bluetoothName">
        <el-input v-model="queryParams.bluetoothName" placeholder="如 gy_ble25t1" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="设备类型" prop="deviceType">
        <el-select v-model="queryParams.deviceType" placeholder="请选择" clearable style="width: 200px">
          <el-option v-for="dict in iot_device_type" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width: 200px">
          <el-option v-for="dict in iot_device_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="厂商" prop="manufacturerId">
        <el-select v-model="queryParams.manufacturerId" placeholder="请选择厂商" clearable style="width: 200px" @keyup.enter="handleQuery">
          <el-option v-for="m in manufacturerOptions" :key="m.manufacturerId" :label="m.manufacturerName" :value="m.manufacturerId" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['iot:device:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['iot:device:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['iot:device:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['iot:device:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 传感器列表 -->
    <el-table v-loading="loading" :data="deviceList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="ID" align="center" key="deviceId" prop="deviceId" width="80" />
      <el-table-column label="传感器名称" align="center" key="deviceName" prop="deviceName" :show-overflow-tooltip="true" />
      <el-table-column label="传感器编号" align="center" key="deviceCode" prop="deviceCode" :show-overflow-tooltip="true" />
      <el-table-column label="蓝牙广播名" align="center" key="bluetoothName" prop="bluetoothName" min-width="150" :show-overflow-tooltip="true" />
      <el-table-column label="设备类型" align="center" key="deviceType" prop="deviceType" width="140">
        <template #default="scope">
          <el-tag v-if="scope.row.deviceType">{{ dictType[scope.row.deviceType] || scope.row.deviceType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="所属厂商" align="center" key="manufacturerName" prop="manufacturerName" :show-overflow-tooltip="true" />
      <el-table-column label="协议" align="center" key="protocol" prop="protocol" width="80" />
      <el-table-column label="固件版本" align="center" key="firmwareVersion" prop="firmwareVersion" width="100" />
      <el-table-column label="状态" align="center" key="status" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="statusType[scope.row.status] || 'info'">{{ dictType[scope.row.status] || scope.row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最后在线" align="center" key="lastSeenAt" prop="lastSeenAt" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.lastSeenAt) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-tooltip content="修改" placement="top">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['iot:device:edit']">修改</el-button>
          </el-tooltip>
          <el-tooltip content="IMU数据" placement="top">
            <el-button link type="primary" icon="DataLine" @click="handleViewImu(scope.row)" v-hasPermi="['iot:imu:query']">数据</el-button>
          </el-tooltip>
          <el-tooltip content="删除" placement="top">
            <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['iot:device:remove']">删除</el-button>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 添加或修改传感器对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form :model="form" :rules="rules" ref="deviceRef" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="传感器名称" prop="deviceName">
              <el-input v-model="form.deviceName" placeholder="请输入传感器名称" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="传感器编号" prop="deviceCode">
              <el-input v-model="form.deviceCode" placeholder="如 HB-3412" maxlength="64" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="设备类型" prop="deviceType">
              <el-select v-model="form.deviceType" placeholder="可不选" clearable style="width: 100%">
                <el-option v-for="dict in iot_device_type" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="通信协议" prop="protocol">
              <el-select v-model="form.protocol" placeholder="请选择" style="width: 100%">
                <el-option label="BLE" value="ble" />
                <el-option label="MQTT" value="mqtt" />
                <el-option label="WiFi" value="wifi" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="所属厂商" prop="manufacturerId">
              <el-select v-model="form.manufacturerId" placeholder="可不选" clearable style="width: 100%">
                <el-option v-for="m in manufacturerOptions" :key="m.manufacturerId" :label="m.manufacturerName" :value="m.manufacturerId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="固件版本" prop="firmwareVersion">
              <el-input v-model="form.firmwareVersion" placeholder="请输入固件版本" maxlength="32" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="蓝牙广播名" prop="bluetoothName">
              <el-input v-model="form.bluetoothName" placeholder="如 gy_ble25t1" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="服务UUID" prop="serviceUuid">
              <el-input v-model="form.serviceUuid" placeholder="如 0000FFE0-0000-1000-8000-00805F9B34FB" maxlength="64" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="通知UUID" prop="notifyCharUuid">
              <el-input v-model="form.notifyCharUuid" placeholder="如 0000FFE4-0000-1000-8000-00805F9B34FB" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio v-for="dict in iot_device_status" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="扩展参数" prop="metadata">
              <el-input v-model="form.metadata" type="textarea" placeholder="JSON格式扩展参数" :rows="2" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- IMU数据抽屉 -->
    <el-drawer v-model="imuDrawer" title="IMU实时数据" size="70%" direction="rtl">
      <div v-if="imuLoading" style="text-align:center;padding:40px">
        <el-icon class="is-loading"><Loading /></el-icon> 加载中...
      </div>
      <div v-else-if="!currentDevice" style="text-align:center;padding:40px;color:#999">请选择一个设备</div>
      <div v-else style="padding:0 16px">
        <el-row :gutter="16" class="mb16">
          <el-col :span="12">
            <el-card shadow="hover">
              <template #header><span>设备信息</span></template>
              <el-descriptions :column="1" size="small" border>
                <el-descriptions-item label="传感器名称">{{ currentDevice.deviceName }}</el-descriptions-item>
                <el-descriptions-item label="传感器编号">{{ currentDevice.deviceCode }}</el-descriptions-item>
                <el-descriptions-item label="蓝牙广播名">{{ currentDevice.bluetoothName }}</el-descriptions-item>
                <el-descriptions-item label="设备类型">{{ dictType[currentDevice.deviceType] || currentDevice.deviceType }}</el-descriptions-item>
                <el-descriptions-item label="固件版本">{{ currentDevice.firmwareVersion }}</el-descriptions-item>
                <el-descriptions-item label="状态"><el-tag :type="statusType[currentDevice.status]">{{ dictType[currentDevice.status] || currentDevice.status }}</el-tag></el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="hover">
              <template #header><span>最新IMU数据</span></template>
              <div v-if="latestImu">
                <el-descriptions :column="1" size="small" border>
                  <el-descriptions-item label="时间">{{ parseTime(latestImu.timestamp || latestImu.createTime) }}</el-descriptions-item>
                  <el-descriptions-item label="加速度X">{{ latestImu.accelX ?? latestImu.accel_x ?? '-' }}</el-descriptions-item>
                  <el-descriptions-item label="加速度Y">{{ latestImu.accelY ?? latestImu.accel_y ?? '-' }}</el-descriptions-item>
                  <el-descriptions-item label="加速度Z">{{ latestImu.accelZ ?? latestImu.accel_z ?? '-' }}</el-descriptions-item>
                  <el-descriptions-item label="角速度X">{{ latestImu.gyroX ?? latestImu.gyro_x ?? '-' }}</el-descriptions-item>
                  <el-descriptions-item label="角速度Y">{{ latestImu.gyroY ?? latestImu.gyro_y ?? '-' }}</el-descriptions-item>
                  <el-descriptions-item label="角速度Z">{{ latestImu.gyroZ ?? latestImu.gyro_z ?? '-' }}</el-descriptions-item>
                  <el-descriptions-item label="仰角">{{ latestImu.pitch != null ? latestImu.pitch + '°' : '-' }}</el-descriptions-item>
                  <el-descriptions-item label="横滚角">{{ latestImu.roll != null ? latestImu.roll + '°' : '-' }}</el-descriptions-item>
                  <el-descriptions-item label="偏航角">{{ latestImu.yaw != null ? latestImu.yaw + '°' : '-' }}</el-descriptions-item>
                </el-descriptions>
              </div>
              <div v-else style="color:#999;text-align:center">暂无数据（设备可能离线）</div>
            </el-card>
          </el-col>
        </el-row>
        <el-card shadow="hover">
          <template #header>
            <span>IMU历史数据（最近100条）</span>
            <el-button style="float:right" size="small" @click="loadImuHistory">刷新</el-button>
          </template>
          <div ref="imuChartRef" style="height:300px"></div>
        </el-card>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="IoTDevice">
import { listDevice, getDevice, addDevice, updateDevice, delDevice } from "@/api/iot/device"
import { listManufacturer } from "@/api/iot/manufacturer"
import { getLatestImuData, getImuHistory } from "@/api/iot/imuData"
import * as echarts from 'echarts'

const { proxy } = getCurrentInstance()
const { iot_device_type, iot_device_status } = useDict("iot_device_type", "iot_device_status")

const statusType = {
  online: 'success',
  offline: 'info',
  error: 'danger',
  maintenance: 'warning'
}
const dictType = {}
iot_device_type.value?.forEach(d => { dictType[d.value] = d.label })
iot_device_status.value?.forEach(d => { dictType[d.value] = d.label })

const deviceList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const manufacturerOptions = ref([])

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    deviceName: undefined,
    deviceCode: undefined,
    bluetoothName: undefined,
    deviceType: undefined,
    status: undefined,
    manufacturerId: undefined
  },
  rules: {
    deviceName: [{ required: true, message: "传感器名称不能为空", trigger: "blur" }],
    deviceCode: [{ required: true, message: "传感器编号不能为空", trigger: "blur" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

// 设备数据抽屉
const imuDrawer = ref(false)
const imuLoading = ref(false)
const currentDevice = ref(null)
const latestImu = ref(null)
const imuChartRef = ref(null)
let imuChart = null

/** 查询设备列表 */
function getList() {
  loading.value = true
  listDevice(queryParams.value).then(res => {
    loading.value = false
    deviceList.value = res.rows
    total.value = res.total
  })
}

/** 查询厂商列表（下拉） */
function getManufacturerList() {
  listManufacturer({ pageNum: 1, pageSize: 999 }).then(res => {
    manufacturerOptions.value = res.rows || []
  })
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 选择条数 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.deviceId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

/** 重置操作表单 */
function reset() {
  form.value = {
    deviceId: undefined,
    deviceName: undefined,
    deviceCode: undefined,
    deviceType: undefined,
    protocol: 'ble',
    manufacturerId: undefined,
    firmwareVersion: undefined,
    bluetoothName: undefined,
    serviceUuid: undefined,
    notifyCharUuid: undefined,
    status: 'offline',
    metadata: undefined
  }
  proxy.resetForm("deviceRef")
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加传感器"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const deviceId = row.deviceId || ids.value
  getDevice(deviceId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改传感器"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["deviceRef"].validate(valid => {
    if (valid) {
      if (form.value.deviceId != undefined) {
        updateDevice(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addDevice(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const deviceIds = row.deviceId || ids.value
  proxy.$modal.confirm('是否确认删除传感器编号为"' + deviceIds + '"的数据项？').then(() => {
    return delDevice(deviceIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download("iot/device/export", { ...queryParams.value }, `device_${new Date().getTime()}.xlsx`)
}

/** 查看IMU数据 */
function handleViewImu(row) {
  currentDevice.value = row
  imuDrawer.value = true
  loadImuHistory()
  loadLatestImu()
}

/** 加载最新IMU数据 */
function loadLatestImu() {
  if (!currentDevice.value?.deviceCode) return
  imuLoading.value = true
  getLatestImuData(currentDevice.value.deviceCode).then(res => {
    imuLoading.value = false
    latestImu.value = res.data || null
  }).catch(() => {
    imuLoading.value = false
    latestImu.value = null
  })
}

/** 加载IMU历史并渲染图表 */
function loadImuHistory() {
  if (!currentDevice.value?.deviceCode) return
  getImuHistory(currentDevice.value.deviceCode, 100).then(res => {
    const records = res.data || []
    renderImuChart(records)
  })
}

/** 渲染IMU曲线图 */
function renderImuChart(records) {
  if (!imuChartRef.value) return
  if (!imuChart) {
    imuChart = echarts.init(imuChartRef.value)
  }
  const times = records.map(r => parseTime(r.timestamp || r.createTime))
  const accelX = records.map(r => r.accelX ?? r.accel_x ?? null)
  const accelY = records.map(r => r.accelY ?? r.accel_y ?? null)
  const accelZ = records.map(r => r.accelZ ?? r.accel_z ?? null)

  imuChart.setOption({
    title: { text: '加速度三轴数据', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0, data: ['AccelX', 'AccelY', 'AccelZ'] },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '12%', containLabel: true },
    xAxis: { type: 'category', data: times, boundaryGap: false },
    yAxis: { type: 'value', name: 'm/s²' },
    series: [
      { name: 'AccelX', type: 'line', data: accelX, smooth: true },
      { name: 'AccelY', type: 'line', data: accelY, smooth: true },
      { name: 'AccelZ', type: 'line', data: accelZ, smooth: true }
    ]
  })
}

onMounted(() => {
  getList()
  getManufacturerList()
})

onUnmounted(() => {
  if (imuChart) {
    imuChart.dispose()
    imuChart = null
  }
})
</script>
