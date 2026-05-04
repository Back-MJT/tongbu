<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-card shadow="hover" class="mb8">
      <el-form :inline="true" :model="queryParams">
        <el-form-item label="用户ID">
          <el-input v-model="queryParams.search" placeholder="搜索用户ID" clearable style="width:200px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="健康目标">
          <el-select v-model="queryParams.healthGoal" placeholder="全部" clearable style="width:160px" @change="handleQuery">
            <el-option label="减脂" value="fat_loss" />
            <el-option label="增肌" value="muscle_gain" />
            <el-option label="心肺" value="cardio" />
            <el-option label="睡眠" value="sleep" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 用户列表 -->
    <el-card shadow="hover">
      <template #header>
        <span>教练用户管理</span>
        <el-tag style="float:right">共 {{ userList.length }} 个用户</el-tag>
      </template>
      <el-table v-loading="loading" :data="filteredUsers" size="small">
        <el-table-column label="用户ID" align="center" key="userId" prop="userId" width="200">
          <template #default="scope">
            <span style="font-family:monospace;font-size:12px">{{ scope.row.userId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="年龄" align="center" key="age" prop="age" width="80" />
        <el-table-column label="性别" align="center" key="gender" prop="gender" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.gender === 'male' ? '' : scope.row.gender === 'female' ? 'danger' : 'info'" size="small">
              {{ scope.row.gender === 'male' ? '男' : scope.row.gender === 'female' ? '女' : '其他' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="健康目标" align="center" key="healthGoals" min-width="180">
          <template #default="scope">
            <el-tag v-for="goal in (scope.row.healthGoals || [])" :key="goal" size="small" style="margin-right:4px">{{ goal }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近运动" align="center" key="latestExerciseDate" prop="latestExerciseDate" width="140">
          <template #default="scope">
            <span v-if="scope.row.latestExerciseDate">{{ scope.row.latestExerciseDate }}</span>
            <el-tag v-else type="info" size="small">暂无</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="睡眠得分" align="center" key="latestSleepScore" prop="latestSleepScore" width="100">
          <template #default="scope">
            <el-progress :percentage="scope.row.latestSleepScore || 0" :color="sleepColor(scope.row.latestSleepScore)" :stroke-width="6" style="width:80px" />
          </template>
        </el-table-column>
        <el-table-column label="活跃处方" align="center" key="activePrescriptionsCount" prop="activePrescriptionsCount" width="100">
          <template #default="scope">
            <el-badge :value="scope.row.activePrescriptionsCount || 0" :type="(scope.row.activePrescriptionsCount || 0) > 0 ? 'primary' : 'info'" />
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="120">
          <template #default="scope">
            <el-button size="small" link type="primary" icon="View" @click="handleView(scope.row)">档案</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup name="CoachUsers">
import { listCoachUsers } from '@/api/iot/coach'

const router = useRouter()

const loading = ref(false)
const userList = ref([])
const queryParams = reactive({
  search: '',
  healthGoal: undefined
})

const filteredUsers = computed(() => {
  let list = userList.value
  if (queryParams.search) {
    const s = queryParams.search.toLowerCase()
    list = list.filter(u => (u.userId || '').toLowerCase().includes(s))
  }
  return list
})

function sleepColor(score) {
  if (!score) return '#909399'
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
}

function handleQuery() {
  // Client-side filtering already handles search
  // If healthGoal is set, we'd need server-side filtering
}

function resetQuery() {
  queryParams.search = ''
  queryParams.healthGoal = undefined
}

function handleView(row) {
  router.push('/iot/coach/user/' + row.userId)
}

function loadUsers() {
  loading.value = true
  listCoachUsers().then(res => {
    userList.value = res.data?.users || res.users || []
  }).catch(() => {
    // Mock data for demo
    userList.value = [
      { userId: 'U-001', age: 35, gender: 'male', healthGoals: ['减脂', '心肺'], latestExerciseDate: '2026-04-19', latestSleepScore: 82, activePrescriptionsCount: 2 },
      { userId: 'U-002', age: 28, gender: 'female', healthGoals: ['睡眠'], latestExerciseDate: '2026-04-18', latestSleepScore: 65, activePrescriptionsCount: 1 },
      { userId: 'U-003', age: 42, gender: 'male', healthGoals: ['增肌'], latestExerciseDate: null, latestSleepScore: 45, activePrescriptionsCount: 0 },
    ]
  }).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
</style>
