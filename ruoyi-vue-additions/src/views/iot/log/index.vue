<template>
  <div class="app-container">
    <!-- 搜索 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="设备编号" prop="deviceCode">
        <el-input v-model="queryParams.deviceCode" placeholder="请输入设备编号" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="事件类型" prop="eventType">
        <el-select v-model="queryParams.eventType" placeholder="请选择" clearable style="width: 160px">
          <el-option v-for="dict in iot_log_type" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="时间范围" style="width: 300px">
        <el-date-picker v-model="dateRange" type="datetimerange" range-separator="-" start-placeholder="开始" end-placeholder="结束"
          value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['iot:log:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['iot:log:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="logList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="日志ID" align="center" key="logId" prop="logId" width="80" />
      <el-table-column label="设备编号" align="center" key="deviceCode" prop="deviceCode" width="160" />
      <el-table-column label="事件类型" align="center" key="eventType" prop="eventType" width="120">
        <template #default="scope">
          <el-tag :type="eventTypeColor(scope.row.eventType)">{{ scope.row.eventType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="事件数据" align="center" key="eventData" prop="eventData" :show-overflow-tooltip="true" />
      <el-table-column label="发生时间" align="center" key="createTime" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="80">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleView(scope.row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailOpen" title="日志详情" width="600px" append-to-body>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="日志ID">{{ currentLog?.logId }}</el-descriptions-item>
        <el-descriptions-item label="设备编号">{{ currentLog?.deviceCode }}</el-descriptions-item>
        <el-descriptions-item label="事件类型">
          <el-tag :type="eventTypeColor(currentLog?.eventType)">{{ currentLog?.eventType }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="发生时间">{{ parseTime(currentLog?.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="事件数据">
          <pre style="margin:0;white-space:pre-wrap;word-break:break-all;font-size:12px">{{ formatJson(currentLog?.eventData) }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup name="DeviceLog">
import { listDeviceLog, delDeviceLog } from "@/api/iot/log"

const { proxy } = getCurrentInstance()
const { iot_log_type } = useDict("iot_log_type")

const logList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const multiple = ref(true)
const total = ref(0)
const dateRange = ref([])
const detailOpen = ref(false)
const currentLog = ref(null)

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    deviceCode: undefined,
    eventType: undefined
  }
})

const { queryParams } = toRefs(data)

const eventTypeMap = {
  online: 'success', offline: 'info', error: 'danger',
  ota: 'warning', heartbeat: 'primary', config: 'primary'
}

function eventTypeColor(type) {
  return eventTypeMap[type] || 'info'
}

function formatJson(str) {
  if (!str) return '-'
  try { return JSON.stringify(JSON.parse(str), null, 2) } catch { return str }
}

function getList() {
  loading.value = true
  const params = { ...queryParams.value }
  if (dateRange.value && dateRange.value.length === 2) {
    params.beginTime = dateRange.value[0]
    params.endTime = dateRange.value[1]
  }
  listDeviceLog(params).then(res => {
    loading.value = false
    logList.value = res.rows || []
    total.value = res.total
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm("queryRef")
  dateRange.value = []
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.logId)
  multiple.value = !selection.length
}

function handleView(row) {
  currentLog.value = row
  detailOpen.value = true
}

function handleDelete() {
  if (!ids.value.length) return
  proxy.$modal.confirm('是否确认删除选中的日志？').then(() => {
    return delDeviceLog(ids.value.join(','))
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

function handleExport() {
  proxy.download("iot/log/export", { ...queryParams.value }, `device_log_${new Date().getTime()}.xlsx`)
}

onMounted(() => { getList() })
</script>
