<template>
  <div class="app-container home">
    <section class="overview-panel">
      <div class="overview-copy">
        <div class="eyebrow">昕动智能器械后台</div>
        <h1>器械使用情况</h1>
        <p>手机端扫码连接蓝牙传感器后，训练组数和次数会回传到后台；这里用于快速查看每台器械和传感器的实时运营状态。</p>
      </div>
      <div class="overview-stats">
        <div class="metric">
          <span class="metric-label">器械总数</span>
          <strong>{{ summary.total }}</strong>
        </div>
        <div class="metric">
          <span class="metric-label">已绑定传感器</span>
          <strong>{{ summary.bound }}</strong>
        </div>
        <div class="metric">
          <span class="metric-label">今日训练</span>
          <strong>{{ summary.todaySessions }}</strong>
        </div>
        <div class="metric">
          <span class="metric-label">今日动作次数</span>
          <strong>{{ summary.todayReps }}</strong>
        </div>
      </div>
    </section>

    <section class="toolbar-row">
      <el-segmented v-model="statusFilter" :options="statusOptions" />
      <el-button :loading="loading" icon="Refresh" @click="loadOverview">刷新</el-button>
    </section>

    <section v-loading="loading" class="equipment-board">
      <article
        v-for="item in filteredEquipment"
        :key="item.equipmentId || item.equipmentCode"
        class="equipment-card"
        :class="{ offline: item.status !== '0' && item.status !== 'normal' }"
      >
        <div class="card-head">
          <div>
            <h2>{{ item.equipmentName || '未命名器械' }}</h2>
            <span>{{ item.location || '未设置位置' }}</span>
          </div>
          <el-tag :type="isSensorBound(item) ? 'success' : 'warning'" effect="light">
            {{ isSensorBound(item) ? '已绑定' : '待绑定' }}
          </el-tag>
        </div>

        <dl class="card-grid">
          <div>
            <dt>器械编号</dt>
            <dd>{{ item.equipmentCode || '-' }}</dd>
          </div>
          <div>
            <dt>蓝牙传感器</dt>
            <dd>{{ item.bluetoothName || item.deviceName || '-' }}</dd>
          </div>
          <div>
            <dt>今日组数</dt>
            <dd>{{ item.todaySets }}</dd>
          </div>
          <div>
            <dt>今日次数</dt>
            <dd>{{ item.todayReps }}</dd>
          </div>
        </dl>

        <div class="card-foot">
          <span>最近上报：{{ item.lastSessionTime || '暂无训练数据' }}</span>
          <el-button link type="primary" @click="goEquipment(item)">查看器械</el-button>
        </div>
      </article>

      <el-empty v-if="!filteredEquipment.length && !loading" description="暂无器械数据" />
    </section>
  </div>
</template>

<script setup name="Index">
import { listEquipment } from "@/api/iot/equipment"
import { listTrainingSession } from "@/api/iot/trainingSession"

const router = useRouter()
const loading = ref(false)
const statusFilter = ref("all")
const equipmentList = ref([])

const statusOptions = [
  { label: "全部", value: "all" },
  { label: "已绑定", value: "bound" },
  { label: "待绑定", value: "unbound" }
]

const summary = computed(() => {
  return equipmentList.value.reduce((acc, item) => {
    acc.total += 1
    if (isSensorBound(item)) acc.bound += 1
    acc.todaySessions += item.todaySessions || 0
    acc.todayReps += item.todayReps || 0
    return acc
  }, { total: 0, bound: 0, todaySessions: 0, todayReps: 0 })
})

const filteredEquipment = computed(() => {
  if (statusFilter.value === "bound") {
    return equipmentList.value.filter(item => isSensorBound(item))
  }
  if (statusFilter.value === "unbound") {
    return equipmentList.value.filter(item => !isSensorBound(item))
  }
  return equipmentList.value
})

function isSensorBound(item) {
  return !!(item?.bluetoothName || item?.deviceCode || item?.deviceName || item?.deviceId)
}

function getTodayRange() {
  const now = new Date()
  const start = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0)
  const end = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59)
  return {
    beginTime: formatDateTime(start),
    endTime: formatDateTime(end)
  }
}

function formatDateTime(date) {
  const pad = value => String(value).padStart(2, "0")
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function buildTrainingMap(rows) {
  return rows.reduce((map, row) => {
    const key = row.equipmentCode || row.deviceCode
    if (!key) return map
    if (!map[key]) {
      map[key] = { todaySessions: 0, todaySets: 0, todayReps: 0, lastSessionTime: "" }
    }
    map[key].todaySessions += 1
    map[key].todaySets += Number(row.completedSets || 0)
    map[key].todayReps += Number(row.totalReps || 0)
    const sessionTime = row.sessionTime || row.createTime || ""
    if (sessionTime && sessionTime > map[key].lastSessionTime) {
      map[key].lastSessionTime = sessionTime
    }
    return map
  }, {})
}

async function loadOverview() {
  loading.value = true
  try {
    const [equipmentRes, sessionRes] = await Promise.all([
      listEquipment({ pageNum: 1, pageSize: 200 }),
      listTrainingSession({ pageNum: 1, pageSize: 500, params: getTodayRange() })
    ])
    const trainingMap = buildTrainingMap(sessionRes.rows || [])
    equipmentList.value = (equipmentRes.rows || []).map(item => {
      const stats = trainingMap[item.equipmentCode] || trainingMap[item.deviceCode] || {}
      return {
        ...item,
        todaySessions: stats.todaySessions || 0,
        todaySets: stats.todaySets || 0,
        todayReps: stats.todayReps || 0,
        lastSessionTime: stats.lastSessionTime || ""
      }
    })
  } finally {
    loading.value = false
  }
}

function goEquipment() {
  router.push("/iot/equipment")
}

onMounted(loadOverview)
</script>

<style scoped>
.home {
  background: #f5f7fb;
  min-height: calc(100vh - 84px);
}

.overview-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.9fr);
  gap: 24px;
  padding: 28px;
  border: 1px solid #dfe7f3;
  border-radius: 8px;
  background: linear-gradient(135deg, #ffffff 0%, #eef6f4 100%);
  box-shadow: 0 12px 32px rgba(31, 45, 61, 0.08);
}

.overview-copy .eyebrow {
  color: #0f766e;
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 10px;
}

.overview-copy h1 {
  margin: 0 0 12px;
  color: #1f2d3d;
  font-size: 30px;
  font-weight: 700;
}

.overview-copy p {
  max-width: 760px;
  margin: 0;
  color: #52616f;
  line-height: 1.8;
}

.overview-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.metric {
  min-height: 92px;
  padding: 16px;
  border: 1px solid #dce8e4;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.82);
}

.metric-label {
  display: block;
  color: #657786;
  font-size: 13px;
  margin-bottom: 10px;
}

.metric strong {
  color: #17233d;
  font-size: 30px;
  line-height: 1;
}

.toolbar-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin: 18px 0;
}

.equipment-board {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  min-height: 240px;
}

.equipment-card {
  padding: 18px;
  border: 1px solid #dfe7f3;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 8px 20px rgba(31, 45, 61, 0.06);
}

.equipment-card.offline {
  border-color: #eadfd2;
}

.card-head,
.card-foot {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.card-head h2 {
  margin: 0 0 6px;
  color: #1f2d3d;
  font-size: 18px;
  font-weight: 700;
}

.card-head span,
.card-foot span,
.card-grid dt {
  color: #7a8998;
  font-size: 13px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin: 18px 0;
}

.card-grid div {
  min-width: 0;
  padding: 12px;
  border-radius: 8px;
  background: #f7f9fc;
}

.card-grid dt {
  margin-bottom: 8px;
}

.card-grid dd {
  overflow: hidden;
  margin: 0;
  color: #263445;
  font-size: 18px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-foot {
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #eef2f7;
}

@media (max-width: 900px) {
  .overview-panel {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .overview-panel {
    padding: 20px;
  }

  .overview-stats,
  .card-grid {
    grid-template-columns: 1fr;
  }

  .toolbar-row {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
