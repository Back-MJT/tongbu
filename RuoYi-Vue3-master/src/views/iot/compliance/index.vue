<template>
  <div class="app-container">
    <!-- 概览统计 -->
    <el-row :gutter="16" class="mb16">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <template #header><span class="stat-title">高合规</span></template>
          <div class="stat-value" style="color:#67c23a">{{ stats.highComplianceCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <template #header><span class="stat-title">中等合规</span></template>
          <div class="stat-value" style="color:#e6a23c">{{ stats.mediumComplianceCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <template #header><span class="stat-title">低合规</span></template>
          <div class="stat-value" style="color:#f56c6c">{{ stats.lowComplianceCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <template #header><span class="stat-title">无干预</span></template>
          <div class="stat-value" style="color:#909399">{{ stats.noneCount }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 合规用户列表 -->
    <el-card shadow="hover" class="mb16">
      <template #header>
        <span>用户合规状态</span>
        <el-tag style="float:right">共 {{ complianceUsers.length }} 个用户</el-tag>
      </template>
      <el-table v-loading="loading" :data="complianceUsers" size="small">
        <el-table-column label="用户ID" align="center" key="userId" prop="userId" width="200">
          <template #default="scope">
            <span style="font-family:monospace;font-size:12px">{{ scope.row.userId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="合规等级" align="center" key="complianceLevel" width="120">
          <template #default="scope">
            <el-tag :type="levelType[scope.row.complianceLevel]" size="small">
              {{ levelLabel[scope.row.complianceLevel] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="无干预天数" align="center" key="daysWithoutIntervention" prop="daysWithoutIntervention" width="120">
          <template #default="scope">
            <span :style="{ color: scope.row.daysWithoutIntervention > 7 ? '#f56c6c' : '#303133' }">
              {{ scope.row.daysWithoutIntervention }}天
            </span>
          </template>
        </el-table-column>
        <el-table-column label="最近干预" align="center" key="lastInterventionDate" prop="lastInterventionDate" width="140">
          <template #default="scope">
            <span v-if="scope.row.lastInterventionDate">{{ scope.row.lastInterventionDate }}</span>
            <el-tag v-else type="info" size="small">无记录</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="活跃处方" align="center" key="activePrescriptionCount" prop="activePrescriptionCount" width="100" />
        <el-table-column label="7日合规率" align="center" key="complianceRate7d" width="140">
          <template #default="scope">
            <el-progress v-if="scope.row.complianceRate7d != null"
              :percentage="Math.round(scope.row.complianceRate7d * 100)"
              :color="complianceColor(scope.row.complianceRate7d)"
              :stroke-width="6" style="width:100px" />
            <span v-else style="color:#909399">-</span>
          </template>
        </el-table-column>
        <el-table-column label="原因" align="center" key="levelReason" prop="levelReason" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" align="center" width="120">
          <template #default="scope">
            <el-button size="small" link type="primary" @click="handleRecord(scope.row)">记录执行</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 告警列表 -->
    <el-card shadow="hover">
      <template #header>
        <span>合规告警</span>
        <el-badge :value="alerts.length" :hidden="alerts.length === 0" style="float:right;margin-top:4px" type="danger" />
      </template>
      <el-table :data="alerts" size="small" v-if="alerts.length">
        <el-table-column label="时间" align="center" key="createdAt" prop="createdAt" width="160" />
        <el-table-column label="用户" align="center" key="userId" prop="userId" width="140">
          <template #default="scope">
            <span style="font-family:monospace;font-size:12px">{{ scope.row.userId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="级别" align="center" key="severity" prop="severity" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.severity === 'high' ? 'danger' : scope.row.severity === 'medium' ? 'warning' : 'info'" size="small">
              {{ scope.row.severity }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标题" align="center" key="title" prop="title" width="160" />
        <el-table-column label="内容" align="center" key="message" prop="message" min-width="200" show-overflow-tooltip />
      </el-table>
      <el-empty v-else description="暂无告警" />
    </el-card>

    <!-- 记录执行对话框 -->
    <el-dialog v-model="recordDialogVisible" title="记录干预执行" width="500px">
      <el-form :model="recordForm" label-width="100px">
        <el-form-item label="用户ID">
          <el-input v-model="recordForm.userId" disabled />
        </el-form-item>
        <el-form-item label="干预ID">
          <el-input v-model="recordForm.interventionId" placeholder="请输入干预方案ID" />
        </el-form-item>
        <el-form-item label="执行详情">
          <el-input-number v-model="recordForm.complianceDetail" :min="0" :max="100" placeholder="完成度百分比" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRecord">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ComplianceView">
import { listComplianceUsers, listComplianceAlerts, recordExecution } from '@/api/iot/compliance'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const complianceUsers = ref([])
const alerts = ref([])
const recordDialogVisible = ref(false)
const recordForm = reactive({
  userId: '',
  interventionId: '',
  complianceDetail: 80
})

const stats = reactive({
  highComplianceCount: 0,
  mediumComplianceCount: 0,
  lowComplianceCount: 0,
  noneCount: 0
})

const levelType = { high: 'success', medium: 'warning', low: 'danger', none: 'info' }
const levelLabel = { high: '高', medium: '中', low: '低', none: '无' }

function complianceColor(rate) {
  if (rate >= 0.8) return '#67c23a'
  if (rate >= 0.5) return '#e6a23c'
  return '#f56c6c'
}

function handleRecord(row) {
  recordForm.userId = row.userId
  recordForm.interventionId = ''
  recordForm.complianceDetail = 80
  recordDialogVisible.value = true
}

function submitRecord() {
  recordExecution({
    userId: recordForm.userId,
    interventionId: recordForm.interventionId,
    complianceDetail: recordForm.complianceDetail
  }).then(() => {
    proxy.$modal.msgSuccess('执行记录已提交')
    recordDialogVisible.value = false
    loadData()
  }).catch(() => {
    proxy.$modal.msgError('提交失败')
  })
}

function loadData() {
  loading.value = true
  listComplianceUsers().then(res => {
    const data = res.data || res
    complianceUsers.value = data.users || []
    stats.highComplianceCount = data.highComplianceCount || 0
    stats.mediumComplianceCount = data.mediumComplianceCount || 0
    stats.lowComplianceCount = data.lowComplianceCount || 0
    stats.noneCount = data.noneCount || 0
  }).catch(() => {
    // Mock data for demo
    complianceUsers.value = [
      { userId: 'U-001', complianceLevel: 'high', daysWithoutIntervention: 0, lastInterventionDate: '2026-04-19', activePrescriptionCount: 2, complianceRate7d: 0.85, levelReason: '持续执行干预方案' },
      { userId: 'U-002', complianceLevel: 'medium', daysWithoutIntervention: 3, lastInterventionDate: '2026-04-16', activePrescriptionCount: 1, complianceRate7d: 0.57, levelReason: '偶尔跳过训练' },
      { userId: 'U-003', complianceLevel: 'low', daysWithoutIntervention: 10, lastInterventionDate: '2026-04-09', activePrescriptionCount: 0, complianceRate7d: 0.14, levelReason: '长时间未执行干预' },
    ]
    stats.highComplianceCount = 1
    stats.mediumComplianceCount = 1
    stats.lowComplianceCount = 1
    stats.noneCount = 0
  }).finally(() => { loading.value = false })

  listComplianceAlerts().then(res => {
    const data = res.data || res
    alerts.value = data.alerts || []
  }).catch(() => {
    alerts.value = [
      { alertId: 'A-001', userId: 'U-003', complianceLevel: 'low', daysWithoutIntervention: 10, severity: 'high', title: '用户长期未干预', message: '用户 U-003 已连续10天未执行任何干预方案', createdAt: '2026-04-19 09:00' },
    ]
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.stat-card .stat-title { font-size: 13px; color: #666; }
.stat-card .stat-value { font-size: 28px; font-weight: 600; margin: 8px 0; }
.mb16 { margin-bottom: 16px; }
</style>
