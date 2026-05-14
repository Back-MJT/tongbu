<template>
  <div class="app-container training-plan-page">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" class="query-form">
      <el-form-item label="用户" prop="userId">
        <el-select
          v-model="queryParams.userId"
          filterable
          clearable
          placeholder="选择用户"
          style="width: 220px"
          @change="handleQuery"
        >
          <el-option
            v-for="user in userOptions"
            :key="user.userId"
            :label="userLabel(user)"
            :value="String(user.userId)"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" clearable placeholder="全部" style="width: 140px" @change="handleQuery">
          <el-option label="启用中" value="active" />
          <el-option label="未启用" value="inactive" />
          <el-option label="草稿" value="draft" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd">新增计划</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="planList">
      <el-table-column label="计划编号" prop="prescriptionNo" min-width="150" show-overflow-tooltip />
      <el-table-column label="用户" min-width="180" show-overflow-tooltip>
        <template #default="scope">
          {{ userNameMap[scope.row.userId] || `用户ID ${scope.row.userId}` }}
        </template>
      </el-table-column>
      <el-table-column label="周期" align="center" width="90">
        <template #default="scope">{{ scope.row.durationDays || 7 }}天</template>
      </el-table-column>
      <el-table-column label="开始日期" prop="startDate" align="center" width="120" />
      <el-table-column label="结束日期" prop="endDate" align="center" width="120" />
      <el-table-column label="状态" align="center" width="100">
        <template #default="scope">
          <el-tag :type="statusTag(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="任务数" align="center" width="90">
        <template #default="scope">{{ parseTasks(scope.row.recommendations).length }}</template>
      </el-table-column>
      <el-table-column label="备注" prop="notes" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" align="center" width="250" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleView(scope.row)">查看</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)">编辑</el-button>
          <el-button v-if="scope.row.status !== 'active'" link type="success" icon="CircleCheck" @click="handleActivate(scope.row)">启用</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog :title="title" v-model="open" width="980px" append-to-body>
      <el-form ref="planRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="用户" prop="userId">
              <el-select v-model="form.userId" filterable placeholder="请选择用户" style="width: 100%">
                <el-option
                  v-for="user in userOptions"
                  :key="user.userId"
                  :label="userLabel(user)"
                  :value="String(user.userId)"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="启用中" value="active" />
                <el-option label="未启用" value="inactive" />
                <el-option label="草稿" value="draft" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="周期天数" prop="durationDays">
              <el-input-number v-model="form.durationDays" :min="1" :max="60" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开始日期" prop="startDate">
              <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期" prop="endDate">
              <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">训练任务</el-divider>
        <div class="task-toolbar">
          <el-button type="primary" plain icon="Plus" @click="addTask">添加任务</el-button>
          <el-button plain icon="DocumentCopy" @click="applyDefaultTasks">套用力量康复模板</el-button>
        </div>
        <el-table :data="taskList" border size="small" class="task-table">
          <el-table-column label="器械名称" min-width="150">
            <template #default="scope">
              <el-input v-model="scope.row.equipmentName" placeholder="如 推胸训练器" />
            </template>
          </el-table-column>
          <el-table-column label="器械编号" width="130">
            <template #default="scope">
              <el-input v-model="scope.row.equipmentCode" placeholder="EQ-000001" />
            </template>
          </el-table-column>
          <el-table-column label="类型" width="150">
            <template #default="scope">
              <el-input v-model="scope.row.equipmentType" placeholder="chest_press" />
            </template>
          </el-table-column>
          <el-table-column label="组数" width="100">
            <template #default="scope">
              <el-input-number v-model="scope.row.targetSets" :min="1" :max="10" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="次数" width="100">
            <template #default="scope">
              <el-input-number v-model="scope.row.targetReps" :min="1" :max="50" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="负重kg" width="110">
            <template #default="scope">
              <el-input-number v-model="scope.row.targetLoadKg" :min="0" :max="300" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="休息秒" width="110">
            <template #default="scope">
              <el-input-number v-model="scope.row.restSeconds" :min="15" :max="300" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="scope">
              <el-button link type="danger" icon="Delete" @click="taskList.splice(scope.$index, 1)" />
            </template>
          </el-table-column>
        </el-table>

        <el-form-item label="教练备注" prop="notes" class="mt16">
          <el-input v-model="form.notes" type="textarea" :rows="3" placeholder="如动作注意事项、禁忌、康复重点" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="训练计划详情" v-model="detailOpen" width="760px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户">{{ userNameMap[detail.userId] || `用户ID ${detail.userId || '-'}` }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="开始日期">{{ detail.startDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="结束日期">{{ detail.endDate || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="parseTasks(detail.recommendations)" border size="small" class="mt16">
        <el-table-column label="器械" prop="equipmentName" />
        <el-table-column label="编号" prop="equipmentCode" width="120" />
        <el-table-column label="组数" prop="targetSets" width="80" />
        <el-table-column label="次数" prop="targetReps" width="80" />
        <el-table-column label="负重" prop="targetLoadKg" width="90" />
      </el-table>
      <div class="detail-notes">{{ detail.notes || '暂无备注' }}</div>
    </el-dialog>
  </div>
</template>

<script setup name="TrainingPlan">
import { listUser } from '@/api/system/user'
import { addTrainingPlan, activateTrainingPlan, delTrainingPlan, getTrainingPlan, listTrainingPlan, updateTrainingPlan } from '@/api/iot/trainingPlan'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const showSearch = ref(true)
const open = ref(false)
const detailOpen = ref(false)
const title = ref('')
const total = ref(0)
const planList = ref([])
const userOptions = ref([])
const taskList = ref([])
const detail = ref({})

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userId: undefined,
    status: undefined
  },
  form: {},
  rules: {
    userId: [{ required: true, message: '请选择用户', trigger: 'change' }],
    status: [{ required: true, message: '请选择状态', trigger: 'change' }],
    startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

const userNameMap = computed(() => {
  const map = {}
  userOptions.value.forEach(user => {
    map[String(user.userId)] = userLabel(user)
  })
  return map
})

function userLabel(user) {
  const name = user.nickName || user.userName || `用户ID ${user.userId}`
  return `${name}（${user.userId}）`
}

function today() {
  return new Date().toISOString().slice(0, 10)
}

function statusLabel(status) {
  return { active: '启用中', inactive: '未启用', draft: '草稿' }[status] || status || '-'
}

function statusTag(status) {
  return status === 'active' ? 'success' : status === 'draft' ? 'info' : 'warning'
}

function parseTasks(recommendations) {
  if (!recommendations) return []
  try {
    const parsed = JSON.parse(recommendations)
    if (Array.isArray(parsed)) return parsed
    if (Array.isArray(parsed.tasks)) return parsed.tasks
  } catch (e) {
    return []
  }
  return []
}

function buildRecommendations() {
  return JSON.stringify({
    source: 'admin',
    tasks: taskList.value.map((task, index) => ({
      taskId: index + 1,
      exerciseName: task.equipmentName || task.exerciseName,
      exerciseType: task.equipmentType || 'strength',
      equipmentCode: task.equipmentCode,
      equipmentName: task.equipmentName,
      equipmentType: task.equipmentType || 'strength',
      equipmentCategory: 'strength',
      targetSets: Number(task.targetSets || 3),
      targetReps: Number(task.targetReps || 12),
      targetLoadKg: Number(task.targetLoadKg || 0),
      restSeconds: Number(task.restSeconds || 75),
      intensityLabel: task.intensityLabel || '中等强度',
      coachingTip: task.coachingTip || '扫码器械二维码后开始，保持动作轨迹稳定。'
    }))
  })
}

function getList() {
  loading.value = true
  listTrainingPlan(queryParams.value).then(res => {
    planList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function loadUsers() {
  listUser({ pageNum: 1, pageSize: 200 }).then(res => {
    userOptions.value = res.rows || []
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function reset() {
  form.value = {
    prescriptionId: undefined,
    userId: undefined,
    status: 'active',
    durationDays: 7,
    startDate: today(),
    endDate: undefined,
    notes: ''
  }
  taskList.value = []
  proxy.resetForm('planRef')
}

function handleAdd() {
  reset()
  applyDefaultTasks()
  open.value = true
  title.value = '新增训练计划'
}

function handleUpdate(row) {
  reset()
  getTrainingPlan(row.prescriptionId).then(res => {
    const plan = res.data || {}
    form.value = {
      ...plan,
      userId: String(plan.userId || ''),
      status: plan.status || 'active',
      durationDays: plan.durationDays || 7
    }
    taskList.value = parseTasks(plan.recommendations)
    open.value = true
    title.value = '编辑训练计划'
  })
}

function handleView(row) {
  getTrainingPlan(row.prescriptionId).then(res => {
    detail.value = res.data || {}
    detailOpen.value = true
  })
}

function submitForm() {
  proxy.$refs.planRef.validate(valid => {
    if (!valid) return
    if (!taskList.value.length) {
      proxy.$modal.msgError('请至少添加一个训练任务')
      return
    }
    const payload = {
      ...form.value,
      recommendations: buildRecommendations()
    }
    const request = payload.prescriptionId ? updateTrainingPlan(payload) : addTrainingPlan(payload)
    request.then(() => {
      proxy.$modal.msgSuccess(payload.prescriptionId ? '修改成功' : '新增成功')
      open.value = false
      getList()
    })
  })
}

function handleActivate(row) {
  proxy.$modal.confirm(`确认启用 ${userNameMap.value[row.userId] || row.userId} 的这份训练计划吗？`).then(() => {
    return activateTrainingPlan(row.prescriptionId)
  }).then(() => {
    proxy.$modal.msgSuccess('启用成功')
    getList()
  }).catch(() => {})
}

function handleDelete(row) {
  proxy.$modal.confirm(`确认删除训练计划 ${row.prescriptionNo || row.prescriptionId} 吗？`).then(() => {
    return delTrainingPlan(row.prescriptionId)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

function cancel() {
  open.value = false
  reset()
}

function addTask() {
  taskList.value.push({
    equipmentName: '',
    equipmentCode: '',
    equipmentType: 'strength',
    targetSets: 3,
    targetReps: 12,
    targetLoadKg: 20,
    restSeconds: 75
  })
}

function applyDefaultTasks() {
  taskList.value = [
    { equipmentName: '推胸训练器', equipmentCode: 'EQ-000001', equipmentType: 'chest_press', targetSets: 3, targetReps: 12, targetLoadKg: 20, restSeconds: 75 },
    { equipmentName: '上斜训练器', equipmentCode: 'EQ-000002', equipmentType: 'incline_press', targetSets: 3, targetReps: 12, targetLoadKg: 20, restSeconds: 75 },
    { equipmentName: '伸屈腿训练器', equipmentCode: 'EQ-000003', equipmentType: 'leg_extension_curl', targetSets: 3, targetReps: 12, targetLoadKg: 15, restSeconds: 75 }
  ]
}

onMounted(() => {
  loadUsers()
  getList()
})
</script>

<style scoped>
.training-plan-page {
  background: #f5f7fb;
  min-height: calc(100vh - 84px);
}
.query-form,
.task-toolbar {
  margin-bottom: 14px;
}
.task-table :deep(.el-input-number) {
  width: 100%;
}
.mt16 {
  margin-top: 16px;
}
.detail-notes {
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: 6px;
  background: #f6f8fb;
  color: #606266;
  line-height: 1.7;
}
</style>
