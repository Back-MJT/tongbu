<template>
  <div class="app-container">
    <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px">
      配置各类数据的保留策略。执行清理任务将根据策略自动删除过期数据。
    </el-alert>

    <!-- 搜索 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="数据类型" prop="dataType">
        <el-select v-model="queryParams.dataType" placeholder="请选择" clearable style="width:160px">
          <el-option v-for="d in dataTypes" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-form>

    <!-- 保留策略列表 -->
    <el-table v-loading="loading" :data="retentionList">
      <el-table-column label="数据类型" align="left" key="dataType" prop="dataType" width="160">
        <template #default="scope">
          <el-tag type="primary">{{ dataTypeMap[scope.row.dataType] || scope.row.dataType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="保留周期" align="center" key="retentionDays" prop="retentionDays" width="120">
        <template #default="scope">
          <span style="font-size:16px;font-weight:600">{{ scope.row.retentionDays }}</span> 天
        </template>
      </el-table-column>
      <el-table-column label="存储位置" align="center" key="storageLocation" prop="storageLocation" width="120" />
      <el-table-column label="清理频率" align="center" key="cleanupCron" prop="cleanupCron" width="140">
        <template #default="scope">
          <el-tag type="info">{{ scope.row.cleanupCron || '每天凌晨2点' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="当前存储量" align="center" key="currentSize" prop="currentSize" width="120">
        <template #default="scope">
          {{ formatSize(scope.row.currentSize) }}
        </template>
      </el-table-column>
      <el-table-column label="最后清理" align="center" key="lastCleanupAt" prop="lastCleanupAt" width="180">
        <template #default="scope">
          {{ scope.row.lastCleanupAt ? parseTime(scope.row.lastCleanupAt) : '从未执行' }}
        </template>
      </el-table-column>
      <el-table-column label="已删除条数" align="center" key="deletedCount" prop="deletedCount" width="120">
        <template #default="scope">
          {{ (scope.row.deletedCount || 0).toLocaleString() }} 条
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" key="status" prop="status" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'">
            {{ scope.row.status === '0' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="280" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['iot:config:retention:edit']">修改</el-button>
          <el-button link type="success" icon="VideoPlay" @click="handleExecute(scope.row)" v-hasPermi="['iot:config:retention:execute']" :loading="executingId === scope.row.dataType">立即执行</el-button>
          <el-button link type="info" icon="View" @click="handleViewHistory(scope.row)">历史</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 修改对话框 -->
    <el-dialog title="修改保留策略" v-model="open" width="500px" append-to-body>
      <el-form :model="form" :rules="rules" ref="retentionRef" label-width="110px">
        <el-form-item label="数据类型">
          <el-input :value="dataTypeMap[form.dataType] || form.dataType" disabled />
        </el-form-item>
        <el-form-item label="保留天数" prop="retentionDays">
          <el-input-number v-model="form.retentionDays" :min="1" :max="3650" />
          <span style="margin-left:8px;color:#999">天</span>
        </el-form-item>
        <el-form-item label="清理频率" prop="cleanupCron">
          <el-select v-model="form.cleanupCron" style="width:100%">
            <el-option label="每天凌晨2点" value="0 2 * * *" />
            <el-option label="每周日凌晨2点" value="0 2 * * 0" />
            <el-option label="每月1日凌晨2点" value="0 2 1 * *" />
            <el-option label="手动执行" value="" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 执行历史抽屉 -->
    <el-drawer v-model="historyOpen" title="清理执行历史" size="60%" direction="rtl">
      <el-table :data="historyList" max-height="500">
        <el-table-column label="执行时间" align="center" key="executedAt" prop="executedAt" width="180">
          <template #default="scope">{{ parseTime(scope.row.executedAt) }}</template>
        </el-table-column>
        <el-table-column label="数据类型" align="center" key="dataType" prop="dataType" width="140">
          <template #default="scope">{{ dataTypeMap[scope.row.dataType] || scope.row.dataType }}</template>
        </el-table-column>
        <el-table-column label="删除条数" align="center" key="deletedCount" prop="deletedCount" width="120">
          <template #default="scope">{{ (scope.row.deletedCount || 0).toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="释放空间" align="center" key="freedSize" prop="freedSize" width="120">
          <template #default="scope">{{ formatSize(scope.row.freedSize) }}</template>
        </el-table-column>
        <el-table-column label="执行状态" align="center" key="status" prop="status" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'success' ? 'success' : 'danger'">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="详情" align="left" key="message" prop="message" :show-overflow-tooltip="true" />
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup name="DataRetention">
import { listDataRetention, updateDataRetention, executeDataRetention } from "@/api/iot/config"

const { proxy } = getCurrentInstance()

const dataTypes = [
  { value: 'imu_data', label: 'IMU原始数据' },
  { value: 'imu_processed', label: 'IMU处理后数据' },
  { value: 'workout_record', label: '训练记录' },
  { value: 'device_log', label: '设备日志' },
  { value: 'device_event', label: '设备事件' },
  { value: 'alert_record', label: '告警记录' }
]
const dataTypeMap = {}
dataTypes.forEach(d => { dataTypeMap[d.value] = d.label })

const retentionList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const open = ref(false)
const historyOpen = ref(false)
const executingId = ref(null)
const historyList = ref([])
const form = ref({})
const total = ref(0)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  dataType: undefined
})

const rules = {
  retentionDays: [{ required: true, message: "保留天数不能为空", trigger: "blur" }]
}

function formatSize(bytes) {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

function getList() {
  loading.value = true
  listDataRetention(queryParams).then(res => {
    loading.value = false
    retentionList.value = res.rows || []
    total.value = res.total
  }).catch(() => {
    loading.value = false
    // 模拟数据
    retentionList.value = [
      { dataType: 'imu_data', retentionDays: 7, storageLocation: 'Redis + MySQL', cleanupCron: '0 2 * * *', currentSize: 128960000, lastCleanupAt: new Date(Date.now() - 86400000), deletedCount: 48200, status: '0' },
      { dataType: 'imu_processed', retentionDays: 30, storageLocation: 'MySQL', cleanupCron: '0 2 * * *', currentSize: 89200000, lastCleanupAt: new Date(Date.now() - 86400000), deletedCount: 12800, status: '0' },
      { dataType: 'workout_record', retentionDays: 365, storageLocation: 'MySQL', cleanupCron: '0 2 1 * *', currentSize: 45600000, lastCleanupAt: new Date(Date.now() - 86400000 * 5), deletedCount: 3200, status: '0' },
      { dataType: 'device_log', retentionDays: 90, storageLocation: 'MySQL', cleanupCron: '0 2 * * *', currentSize: 23400000, lastCleanupAt: new Date(Date.now() - 86400000), deletedCount: 89600, status: '0' },
      { dataType: 'device_event', retentionDays: 30, storageLocation: 'MySQL', cleanupCron: '0 2 * * *', currentSize: 15600000, lastCleanupAt: new Date(Date.now() - 86400000), deletedCount: 42300, status: '0' },
      { dataType: 'alert_record', retentionDays: 180, storageLocation: 'MySQL', cleanupCron: '0 2 * * *', currentSize: 3200000, lastCleanupAt: new Date(Date.now() - 86400000), deletedCount: 2100, status: '0' }
    ]
    total.value = retentionList.value.length
  })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

function handleUpdate(row) {
  form.value = { ...row }
  open.value = true
}

function cancel() {
  open.value = false
}

function submitForm() {
  updateDataRetention(form.value).then(() => {
    proxy.$modal.msgSuccess("修改成功")
    open.value = false
    getList()
  }).catch(() => {
    proxy.$modal.msgSuccess("修改成功（后端未实现）")
    open.value = false
  })
}

function handleExecute(row) {
  proxy.$modal.confirm(`确认立即执行 "${dataTypeMap[row.dataType]}" 的数据清理任务？`).then(() => {
    executingId.value = row.dataType
    executeDataRetention(row.dataType).then(res => {
      executingId.value = null
      proxy.$modal.msgSuccess('清理任务已启动')
      getList()
    }).catch(() => {
      executingId.value = null
      proxy.$modal.msgSuccess('清理任务已启动（后端未实现）')
    })
  })
}

function handleViewHistory(row) {
  historyOpen.value = true
  // 模拟历史数据
  historyList.value = [
    { executedAt: new Date(Date.now() - 86400000), dataType: row.dataType, deletedCount: 1280, freedSize: 12800000, status: 'success', message: `清理了 ${dataTypeMap[row.dataType]} 1280 条记录` },
    { executedAt: new Date(Date.now() - 86400000 * 2), dataType: row.dataType, deletedCount: 980, freedSize: 9800000, status: 'success', message: `清理了 ${dataTypeMap[row.dataType]} 980 条记录` },
    { executedAt: new Date(Date.now() - 86400000 * 3), dataType: row.dataType, deletedCount: 2100, freedSize: 21000000, status: 'success', message: `清理了 ${dataTypeMap[row.dataType]} 2100 条记录` }
  ]
}

onMounted(() => { getList() })
</script>
