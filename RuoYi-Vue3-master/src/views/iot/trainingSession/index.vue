<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="器械编号" prop="equipmentCode">
        <el-input v-model="queryParams.equipmentCode" placeholder="请输入器械编号" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item v-if="activeEquipmentCode" label="快捷筛选">
        <el-tag closable type="success" @close="clearEquipmentFilter">
          器械 {{ activeEquipmentCode }}
        </el-tag>
      </el-form-item>
      <el-form-item label="设备编号" prop="deviceCode">
        <el-input v-model="queryParams.deviceCode" placeholder="请输入设备编号" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="动作类型" prop="exerciseType">
        <el-input v-model="queryParams.exerciseType" placeholder="如 chest_press" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="用户ID" prop="userId">
        <el-input v-model="queryParams.userId" placeholder="请输入用户ID" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="时间范围" style="width: 320px">
        <el-date-picker
          v-model="dateRange"
          type="datetimerange"
          range-separator="-"
          start-placeholder="开始"
          end-placeholder="结束"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="success" plain icon="User" :disabled="!queryParams.userId" @click="handleUserSummary">
          用户摘要
        </el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="sessionList">
      <el-table-column label="会话ID" align="center" prop="sessionId" width="100" />
      <el-table-column label="用户ID" align="center" prop="userId" width="100" />
      <el-table-column label="器械编号" align="center" width="180">
        <template #default="scope">
          <el-button
            v-if="scope.row.equipmentCode"
            link
            type="primary"
            @click="handleViewEquipment(scope.row.equipmentCode)"
          >
            {{ scope.row.equipmentCode }}
          </el-button>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="设备编号" align="center" prop="deviceCode" width="160" />
      <el-table-column label="动作类型" align="center" prop="exerciseType" width="140" />
      <el-table-column label="组数" align="center" prop="completedSets" width="80" />
      <el-table-column label="次数" align="center" prop="totalReps" width="80" />
      <el-table-column label="时长(分钟)" align="center" prop="durationMinutes" width="110" />
      <el-table-column label="阶段" align="center" prop="stage" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.stage" type="info">{{ scope.row.stage }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="训练时间" align="center" prop="sessionTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.sessionTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="100">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleView(scope.row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="训练详情" size="50%" direction="rtl">
      <div v-if="detailLoading" class="detail-loading">
        <el-icon class="is-loading"><Loading /></el-icon> 加载中...
      </div>
      <template v-else-if="currentDetail && currentDetail.session">
        <el-card shadow="never" class="mb16">
          <template #header><span>会话摘要</span></template>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="会话ID">{{ currentDetail.session.sessionId }}</el-descriptions-item>
            <el-descriptions-item label="用户ID">{{ currentDetail.session.userId }}</el-descriptions-item>
            <el-descriptions-item label="器械编号">{{ currentDetail.session.equipmentCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="设备编号">{{ currentDetail.session.deviceCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="动作类型">{{ currentDetail.session.exerciseType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="训练阶段">{{ currentDetail.session.stage || '-' }}</el-descriptions-item>
            <el-descriptions-item label="完成组数">{{ currentDetail.session.completedSets || 0 }}</el-descriptions-item>
            <el-descriptions-item label="总次数">{{ currentDetail.session.totalReps || 0 }}</el-descriptions-item>
            <el-descriptions-item label="时长">{{ currentDetail.session.durationMinutes || 0 }} 分钟</el-descriptions-item>
            <el-descriptions-item label="训练时间">{{ parseTime(currentDetail.session.sessionTime) }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card shadow="never">
          <template #header><span>训练组明细</span></template>
          <el-table :data="currentDetail.sets || []" size="small">
            <el-table-column label="组序号" align="center" prop="setNo" width="90" />
            <el-table-column label="次数" align="center" prop="reps" width="90" />
            <el-table-column label="时长(秒)" align="center" prop="durationSec" width="100" />
            <el-table-column label="开始时间" align="center" prop="startedAt" width="180">
              <template #default="scope">
                <span>{{ parseTime(scope.row.startedAt) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="结束时间" align="center" prop="endedAt" width="180">
              <template #default="scope">
                <span>{{ parseTime(scope.row.endedAt) }}</span>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!(currentDetail.sets || []).length" description="暂无训练组明细" />
        </el-card>
      </template>
      <el-empty v-else description="暂无详情数据" />
    </el-drawer>

    <el-drawer v-model="summaryOpen" title="用户真实训练数据" size="42%" direction="rtl">
      <div v-if="summaryLoading" class="detail-loading">
        <el-icon class="is-loading"><Loading /></el-icon> 加载中...
      </div>
      <template v-else-if="userSummary">
        <el-card shadow="never" class="mb16">
          <template #header><span>真实使用数据</span></template>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="用户ID">{{ userSummary.userId }}</el-descriptions-item>
            <el-descriptions-item label="总训练次数">{{ userSummary.totalSessions || 0 }}</el-descriptions-item>
            <el-descriptions-item label="总组数">{{ userSummary.totalSets || 0 }}</el-descriptions-item>
            <el-descriptions-item label="总次数">{{ userSummary.totalReps || 0 }}</el-descriptions-item>
            <el-descriptions-item label="总时长">{{ userSummary.totalDurationMin || 0 }} 分钟</el-descriptions-item>
            <el-descriptions-item label="最高训练量">{{ userSummary.peakVolumeKg || 0 }} kg</el-descriptions-item>
            <el-descriptions-item label="最近训练">{{ parseTime(userSummary.latestTrainingTime) || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
        <el-card shadow="never">
          <template #header><span>个性化生成依据</span></template>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="算法版本">{{ userSummary.algorithmVersion || '-' }}</el-descriptions-item>
            <el-descriptions-item label="生成用户">{{ userSummary.generatedForUserId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="近7天训练">{{ userSummary.generationBasis?.sessionsLast7Days || 0 }} 次</el-descriptions-item>
            <el-descriptions-item label="近30天训练">{{ userSummary.generationBasis?.sessionsLast30Days || 0 }} 次</el-descriptions-item>
            <el-descriptions-item label="今日已完成组数">{{ userSummary.generationBasis?.completedSetsToday || 0 }} 组</el-descriptions-item>
            <el-descriptions-item label="用户种子">{{ userSummary.generationBasis?.userSeed ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="设备/动作类型">{{ userSummary.generationBasis?.deviceType || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </template>
      <el-empty v-else description="请输入用户ID后查看摘要" />
    </el-drawer>
  </div>
</template>

<script setup name="IoTTrainingSession">
import { Loading } from '@element-plus/icons-vue'
import { listTrainingSession, getTrainingSession, getUserTrainingSummary } from "@/api/iot/trainingSession"

const router = useRouter()
const route = useRoute()
const { proxy } = getCurrentInstance()

const sessionList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const dateRange = ref([])
const detailOpen = ref(false)
const detailLoading = ref(false)
const currentDetail = ref(null)
const summaryOpen = ref(false)
const summaryLoading = ref(false)
const userSummary = ref(null)

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    equipmentCode: undefined,
    deviceCode: undefined,
    exerciseType: undefined,
    userId: undefined
  }
})

const { queryParams } = toRefs(data)

const activeEquipmentCode = computed(() => queryParams.value.equipmentCode || '')

function getList() {
  loading.value = true
  const params = { ...queryParams.value }
  if (dateRange.value && dateRange.value.length === 2) {
    params.beginTime = dateRange.value[0]
    params.endTime = dateRange.value[1]
  }
  listTrainingSession(params).then(res => {
    sessionList.value = res.rows || []
    total.value = res.total || 0
    loading.value = false
  }).catch(() => {
    loading.value = false
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

function clearEquipmentFilter() {
  queryParams.value.equipmentCode = undefined
  handleQuery()
}

function handleView(row) {
  detailOpen.value = true
  detailLoading.value = true
  currentDetail.value = null
  getTrainingSession(row.sessionId).then(res => {
    currentDetail.value = res.data
    detailLoading.value = false
  }).catch(() => {
    detailLoading.value = false
  })
}

function handleUserSummary() {
  if (!queryParams.value.userId) {
    proxy.$modal.msgWarning('请先输入用户ID')
    return
  }
  summaryOpen.value = true
  summaryLoading.value = true
  userSummary.value = null
  getUserTrainingSummary(queryParams.value.userId).then(res => {
    userSummary.value = res.data
    summaryLoading.value = false
  }).catch(() => {
    summaryLoading.value = false
  })
}

onMounted(() => {
  if (route.query.equipmentCode) {
    queryParams.value.equipmentCode = route.query.equipmentCode
  }
  getList()
})

function handleViewEquipment(equipmentCode) {
  router.push({ path: '/iot/equipment', query: { equipmentCode } })
}
</script>

<style scoped>
.detail-loading {
  text-align: center;
  padding: 40px;
  color: var(--el-text-color-secondary);
}

.mb16 {
  margin-bottom: 16px;
}
</style>
