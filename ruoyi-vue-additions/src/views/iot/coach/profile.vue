<template>
  <div class="app-container">
    <!-- 返回 -->
    <el-page-header @back="goBack" content="用户健康档案" class="mb16" />

    <div v-loading="loading">
      <!-- 基本信息 -->
      <el-card shadow="hover" class="mb16" v-if="profile">
        <template #header><span>基本信息</span></template>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="用户ID">
            <span style="font-family:monospace">{{ profile.userId }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="年龄">{{ profile.age }}</el-descriptions-item>
          <el-descriptions-item label="性别">
            <el-tag :type="profile.gender === 'male' ? '' : profile.gender === 'female' ? 'danger' : 'info'" size="small">
              {{ profile.gender === 'male' ? '男' : profile.gender === 'female' ? '女' : '其他' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="健康目标">
            <el-tag v-for="goal in (profile.healthGoals || [])" :key="goal" size="small" style="margin-right:4px">{{ goal }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="最近运动">{{ profile.latestExerciseDate || '暂无' }}</el-descriptions-item>
          <el-descriptions-item label="活跃处方数">
            <el-badge :value="profile.activePrescriptionsCount || 0" type="primary" />
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 基线评估 -->
      <el-row :gutter="16" class="mb16" v-if="profile">
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header><span>基线评分</span></template>
            <div style="display:flex;justify-content:space-around;padding:20px 0">
              <div style="text-align:center">
                <el-progress type="dashboard" :percentage="profile.baselineScores?.exerciseCapacity || 0" :color="'#409eff'" :width="120">
                  <template #default="{ percentage }">
                    <span style="font-size:20px;font-weight:600">{{ percentage }}</span>
                  </template>
                </el-progress>
                <div style="margin-top:8px;color:#666;font-size:13px">运动能力</div>
              </div>
              <div style="text-align:center">
                <el-progress type="dashboard" :percentage="profile.baselineScores?.sleepQuality || 0" :color="'#67c23a'" :width="120">
                  <template #default="{ percentage }">
                    <span style="font-size:20px;font-weight:600">{{ percentage }}</span>
                  </template>
                </el-progress>
                <div style="margin-top:8px;color:#666;font-size:13px">睡眠质量</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header><span>风险因素</span></template>
            <div style="padding:20px 0">
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="久坐风险">
                  <el-tag :type="profile.riskFactors?.sedentary ? 'danger' : 'success'" size="small">
                    {{ profile.riskFactors?.sedentary ? '有风险' : '正常' }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="睡眠风险">
                  <el-tag :type="profile.riskFactors?.poorSleep ? 'danger' : 'success'" size="small">
                    {{ profile.riskFactors?.poorSleep ? '有风险' : '正常' }}
                  </el-tag>
                </el-descriptions-item>
              </el-descriptions>
              <div style="margin-top:16px">
                <span style="color:#666;font-size:13px">睡眠得分：</span>
                <el-progress :percentage="profile.latestSleepScore || 0" :color="sleepColor" :stroke-width="10" style="width:200px;display:inline-block;margin-left:8px" />
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 处方列表 -->
      <el-card shadow="hover">
        <template #header>
          <span>干预处方</span>
          <el-tag style="float:right">共 {{ prescriptions.length }} 条</el-tag>
        </template>
        <el-table :data="prescriptions" size="small" v-if="prescriptions.length">
          <el-table-column label="处方ID" align="center" key="prescriptionId" prop="prescriptionId" width="180">
            <template #default="scope">
              <span style="font-family:monospace;font-size:12px">{{ scope.row.prescriptionId }}</span>
            </template>
          </el-table-column>
          <el-table-column label="干预类型" align="center" key="interventionType" prop="interventionType" width="120" />
          <el-table-column label="状态" align="center" key="status" prop="status" width="100">
            <template #default="scope">
              <el-tag :type="statusType[scope.row.status] || 'info'" size="small">{{ statusLabel[scope.row.status] || scope.row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" key="createdAt" prop="createdAt" width="160" />
          <el-table-column label="持续时间" align="center" key="durationDays" prop="durationDays" width="100">
            <template #default="scope">{{ scope.row.durationDays }}天</template>
          </el-table-column>
          <el-table-column label="备注" align="center" key="notes" prop="notes" min-width="150" show-overflow-tooltip />
          <el-table-column label="操作" align="center" width="200">
            <template #default="scope">
              <el-button v-if="scope.row.status === 'active'" size="small" link type="warning" @click="handleStatusChange(scope.row.prescriptionId, 'paused')">暂停</el-button>
              <el-button v-if="scope.row.status === 'paused'" size="small" link type="success" @click="handleStatusChange(scope.row.prescriptionId, 'active')">恢复</el-button>
              <el-button v-if="scope.row.status === 'active' || scope.row.status === 'paused'" size="small" link type="info" @click="handleStatusChange(scope.row.prescriptionId, 'completed')">完成</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无处方记录" />
      </el-card>
    </div>
  </div>
</template>

<script setup name="CoachUserProfile">
import { getUserProfile, getUserPrescriptions, updatePrescription } from '@/api/iot/coach'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const loading = ref(true)
const profile = ref(null)
const prescriptions = ref([])

const statusType = { active: 'success', paused: 'warning', completed: 'info' }
const statusLabel = { active: '活跃', paused: '已暂停', completed: '已完成' }

const sleepColor = computed(() => {
  const score = profile.value?.latestSleepScore || 0
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
})

function goBack() {
  router.push('/iot/coach')
}

function handleStatusChange(prescriptionId, newStatus) {
  updatePrescription(prescriptionId, { status: newStatus }).then(res => {
    proxy.$modal.msgSuccess('状态已更新')
    prescriptions.value = prescriptions.value.map(p =>
      p.prescriptionId === prescriptionId ? { ...p, status: newStatus } : p
    )
  }).catch(() => {
    proxy.$modal.msgError('更新失败')
  })
}

function loadProfile() {
  const userId = route.params.userId
  if (!userId) return

  loading.value = true
  Promise.all([
    getUserProfile(userId),
    getUserPrescriptions(userId)
  ]).then(([profileRes, prescriptionsRes]) => {
    profile.value = profileRes.data || profileRes
    prescriptions.value = prescriptionsRes.data?.prescriptions || prescriptionsRes.prescriptions || []
  }).catch(() => {
    // Mock data for demo
    profile.value = {
      userId: userId,
      age: 35,
      gender: 'male',
      healthGoals: ['减脂', '心肺'],
      latestExerciseDate: '2026-04-19',
      latestSleepScore: 72,
      activePrescriptionsCount: 2,
      riskFactors: { sedentary: false, poorSleep: true },
      baselineScores: { exerciseCapacity: 68, sleepQuality: 55 }
    }
    prescriptions.value = [
      { prescriptionId: 'RX-001', interventionType: 'exercise', status: 'active', createdAt: '2026-04-15', durationDays: 28, notes: '每周3次中等强度有氧运动' },
      { prescriptionId: 'RX-002', interventionType: 'sleep', status: 'active', createdAt: '2026-04-18', durationDays: 14, notes: '睡前放松训练+作息调整' },
    ]
  }).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
</style>
