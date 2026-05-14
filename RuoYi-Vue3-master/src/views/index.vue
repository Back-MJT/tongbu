<template>
  <div class="app-container home">
    <section class="hero-panel">
      <div class="hero-copy">
        <div class="eyebrow">XINDONG INTELLIGENT OPERATIONS</div>
        <h1>昕动智能管理系统</h1>
        <p>聚焦场馆今日训练、人员活跃与重点器械使用状态，辅助运营人员快速判断设备利用率和训练热度。</p>
      </div>
      <div class="hero-actions">
        <el-button :loading="loading" icon="Refresh" @click="loadOverview">刷新数据</el-button>
        <el-button type="primary" @click="goEquipment">查看全部器械</el-button>
      </div>
    </section>

    <section v-loading="loading" class="kpi-grid">
      <article v-for="item in kpiCards" :key="item.label" class="kpi-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.sub }}</small>
      </article>
    </section>

    <section class="main-grid">
      <article class="panel trend-panel">
        <div class="panel-head">
          <div>
            <h2>近 7 天训练趋势</h2>
            <p>训练人次与动作次数</p>
          </div>
          <el-tag effect="dark">真实训练记录</el-tag>
        </div>
        <div v-if="weeklyTrend.length" class="trend-chart">
          <div v-for="day in weeklyTrend" :key="day.date" class="trend-row">
            <span>{{ day.label }}</span>
            <div class="trend-bars">
              <i class="bar sessions" :style="{ width: `${day.sessionPercent}%` }"></i>
              <i class="bar reps" :style="{ width: `${day.repPercent}%` }"></i>
            </div>
            <em>{{ day.sessions }}人次 / {{ day.reps }}次</em>
          </div>
        </div>
        <el-empty v-else description="暂无训练趋势" />
      </article>

      <article class="panel people-panel">
        <div class="panel-head">
          <div>
            <h2>人员使用情况</h2>
            <p>活跃用户 Top 6</p>
          </div>
          <span class="panel-count">{{ activeUsers.length }}</span>
        </div>
        <div v-if="activeUsers.length" class="ranking-list">
          <div v-for="(user, index) in activeUsers" :key="user.userId" class="rank-item">
            <b>{{ index + 1 }}</b>
            <div class="rank-main">
              <strong>{{ user.name }}</strong>
              <span>最近训练 {{ formatShortTime(user.latestTime) }}</span>
            </div>
            <div class="rank-metric">
              <strong>{{ user.sessions }}</strong>
              <span>{{ user.reps }} 次</span>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无人员训练数据" />
      </article>
    </section>

    <section class="bottom-grid">
      <article class="panel">
        <div class="panel-head">
          <div>
            <h2>重点器械使用情况</h2>
            <p>按训练人次和动作次数排序，仅展示前 6 台</p>
          </div>
          <el-button link type="primary" @click="goEquipment">全部器械</el-button>
        </div>
        <div v-if="featuredEquipment.length" class="equipment-list">
          <div v-for="item in featuredEquipment" :key="item.equipmentCode" class="equipment-row">
            <div class="equipment-name">
              <strong>{{ item.equipmentName || '未命名器械' }}</strong>
              <span>{{ item.location || item.equipmentCode || '未设置位置' }}</span>
            </div>
            <div class="equipment-sensor">
              <el-tag :type="isSensorBound(item) ? 'success' : 'warning'" effect="dark" size="small">
                {{ isSensorBound(item) ? '已绑定' : '待绑定' }}
              </el-tag>
              <span>{{ item.bluetoothName || item.deviceCode || '未绑定传感器' }}</span>
            </div>
            <div class="equipment-data">
              <strong>{{ item.sessions }}</strong>
              <span>训练人次</span>
            </div>
            <div class="equipment-data">
              <strong>{{ item.reps }}</strong>
              <span>动作次数</span>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无器械数据" />
      </article>

      <article class="panel">
        <div class="panel-head">
          <div>
            <h2>最近训练动态</h2>
            <p>最新 8 条训练回传</p>
          </div>
        </div>
        <div v-if="recentSessions.length" class="activity-list">
          <div v-for="session in recentSessions" :key="session.sessionId" class="activity-item">
            <i></i>
            <div>
              <strong>{{ session.userName }}</strong>
              <span>{{ session.equipmentName }} · {{ session.totalReps || 0 }} 次 · {{ session.completedSets || 0 }} 组</span>
            </div>
            <time>{{ formatShortTime(session.sessionTime || session.createTime) }}</time>
          </div>
        </div>
        <el-empty v-else description="暂无训练动态" />
      </article>
    </section>
  </div>
</template>

<script setup name="Index">
import { listEquipment } from "@/api/iot/equipment"
import { listTrainingSession } from "@/api/iot/trainingSession"
import { listUser } from "@/api/system/user"

const router = useRouter()
const loading = ref(false)
const equipmentList = ref([])
const sessionList = ref([])
const userMap = ref({})

const todaySessions = computed(() => sessionList.value.filter(row => isToday(row.sessionTime || row.createTime)))

const activeUsers = computed(() => {
  const map = {}
  sessionList.value.forEach(row => {
    const userId = row.userId || "unknown"
    if (!map[userId]) {
      map[userId] = {
        userId,
        name: getUserName(userId),
        sessions: 0,
        reps: 0,
        sets: 0,
        latestTime: ""
      }
    }
    map[userId].sessions += 1
    map[userId].reps += Number(row.totalReps || 0)
    map[userId].sets += Number(row.completedSets || 0)
    const time = row.sessionTime || row.createTime || ""
    if (time > map[userId].latestTime) map[userId].latestTime = time
  })
  return Object.values(map)
    .sort((a, b) => b.sessions - a.sessions || b.reps - a.reps)
    .slice(0, 6)
})

const featuredEquipment = computed(() => {
  const usage = buildEquipmentUsage()
  return equipmentList.value
    .map(item => {
      const stats = usage[item.equipmentCode] || usage[item.deviceCode] || {}
      return {
        ...item,
        sessions: stats.sessions || 0,
        reps: stats.reps || 0,
        sets: stats.sets || 0,
        latestTime: stats.latestTime || ""
      }
    })
    .sort((a, b) => b.sessions - a.sessions || b.reps - a.reps || Number(isSensorBound(b)) - Number(isSensorBound(a)))
    .slice(0, 6)
})

const recentSessions = computed(() => {
  const equipmentNameMap = equipmentList.value.reduce((map, item) => {
    if (item.equipmentCode) map[item.equipmentCode] = item.equipmentName
    if (item.deviceCode) map[item.deviceCode] = item.equipmentName
    return map
  }, {})
  return [...sessionList.value]
    .sort((a, b) => String(b.sessionTime || b.createTime || "").localeCompare(String(a.sessionTime || a.createTime || "")))
    .slice(0, 8)
    .map(row => ({
      ...row,
      userName: getUserName(row.userId),
      equipmentName: equipmentNameMap[row.equipmentCode] || equipmentNameMap[row.deviceCode] || row.equipmentCode || "未知器械"
    }))
})

const weeklyTrend = computed(() => {
  const days = getLastSevenDays()
  const maxSessions = Math.max(1, ...days.map(day => day.sessions))
  const maxReps = Math.max(1, ...days.map(day => day.reps))
  return days.map(day => ({
    ...day,
    sessionPercent: Math.max(4, Math.round((day.sessions / maxSessions) * 100)),
    repPercent: Math.max(4, Math.round((day.reps / maxReps) * 100))
  }))
})

const kpiCards = computed(() => {
  const bound = equipmentList.value.filter(item => isSensorBound(item)).length
  const today = todaySessions.value
  const todayUsers = new Set(today.map(row => row.userId).filter(Boolean)).size
  const todayReps = today.reduce((sum, row) => sum + Number(row.totalReps || 0), 0)
  return [
    { label: "今日训练", value: today.length, sub: "手机端训练回传人次" },
    { label: "今日活跃人员", value: todayUsers, sub: `近 7 天活跃 ${activeUsers.value.length} 人` },
    { label: "今日动作次数", value: todayReps, sub: "累计完成动作次数" },
    { label: "绑定器械", value: `${bound}/${equipmentList.value.length}`, sub: "已绑定蓝牙传感器" }
  ]
})

function isSensorBound(item) {
  return !!(item?.bluetoothName || item?.deviceCode || item?.deviceName || item?.deviceId)
}

function getUserName(userId) {
  const user = userMap.value[userId]
  return user?.nickName || user?.userName || `用户${userId || '-'}`
}

function buildEquipmentUsage() {
  return sessionList.value.reduce((map, row) => {
    const key = row.equipmentCode || row.deviceCode
    if (!key) return map
    if (!map[key]) map[key] = { sessions: 0, reps: 0, sets: 0, latestTime: "" }
    map[key].sessions += 1
    map[key].reps += Number(row.totalReps || 0)
    map[key].sets += Number(row.completedSets || 0)
    const time = row.sessionTime || row.createTime || ""
    if (time > map[key].latestTime) map[key].latestTime = time
    return map
  }, {})
}

function getLastSevenDays() {
  const today = new Date()
  const days = []
  for (let i = 6; i >= 0; i--) {
    const date = new Date(today)
    date.setDate(today.getDate() - i)
    const dateKey = formatDate(date)
    days.push({ date: dateKey, label: `${date.getMonth() + 1}/${date.getDate()}`, sessions: 0, reps: 0 })
  }
  sessionList.value.forEach(row => {
    const time = row.sessionTime || row.createTime
    if (!time) return
    const key = String(time).slice(0, 10)
    const day = days.find(item => item.date === key)
    if (!day) return
    day.sessions += 1
    day.reps += Number(row.totalReps || 0)
  })
  return days
}

function getRange(days) {
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - days + 1)
  start.setHours(0, 0, 0, 0)
  end.setHours(23, 59, 59, 999)
  return {
    beginTime: formatDateTime(start),
    endTime: formatDateTime(end)
  }
}

function formatDate(date) {
  const pad = value => String(value).padStart(2, "0")
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function formatDateTime(date) {
  const pad = value => String(value).padStart(2, "0")
  return `${formatDate(date)} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function isToday(value) {
  return value && String(value).slice(0, 10) === formatDate(new Date())
}

function formatShortTime(value) {
  if (!value) return "暂无"
  const text = String(value)
  const date = text.slice(5, 10).replace("-", "/")
  const time = text.slice(11, 16)
  return time ? `${date} ${time}` : date
}

async function loadUsers() {
  try {
    const res = await listUser({ pageNum: 1, pageSize: 500 })
    userMap.value = (res.rows || []).reduce((map, user) => {
      map[user.userId] = user
      return map
    }, {})
  } catch (e) {
    userMap.value = {}
  }
}

async function loadOverview() {
  loading.value = true
  try {
    const range = getRange(7)
    const [equipmentRes, sessionRes] = await Promise.all([
      listEquipment({ pageNum: 1, pageSize: 200 }),
      listTrainingSession({ pageNum: 1, pageSize: 500, params: range }),
      loadUsers()
    ])
    equipmentList.value = equipmentRes.rows || []
    sessionList.value = sessionRes.rows || []
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
  min-height: calc(100vh - 84px);
  padding: 20px;
  background:
    radial-gradient(circle at 18% 0%, rgba(28, 120, 255, 0.22), transparent 30%),
    radial-gradient(circle at 85% 12%, rgba(38, 214, 179, 0.16), transparent 28%),
    linear-gradient(135deg, #07111f 0%, #0a1629 48%, #08111c 100%);
  color: #eaf2ff;
}

.hero-panel,
.panel,
.kpi-card {
  border: 1px solid rgba(132, 179, 255, 0.16);
  border-radius: 8px;
  background: rgba(10, 23, 42, 0.78);
  box-shadow: 0 20px 55px rgba(0, 0, 0, 0.24);
  backdrop-filter: blur(12px);
}

.hero-panel {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 30px;
  overflow: hidden;
}

.eyebrow {
  margin-bottom: 10px;
  color: #54e6c0;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
}

.hero-copy h1 {
  margin: 0 0 10px;
  font-size: 34px;
  font-weight: 800;
}

.hero-copy p {
  max-width: 760px;
  margin: 0;
  color: #9fb0c8;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex-shrink: 0;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin: 18px 0;
}

.kpi-card {
  min-height: 124px;
  padding: 18px;
}

.kpi-card span,
.panel-head p,
.equipment-name span,
.equipment-sensor span,
.equipment-data span,
.rank-main span,
.rank-metric span,
.activity-item span,
.activity-item time,
.trend-row span,
.trend-row em {
  color: #8ea2bd;
  font-size: 13px;
}

.kpi-card strong {
  display: block;
  margin: 12px 0 8px;
  color: #ffffff;
  font-size: 34px;
  line-height: 1;
}

.kpi-card small {
  color: #55d8bf;
}

.main-grid,
.bottom-grid {
  display: grid;
  gap: 16px;
}

.main-grid {
  grid-template-columns: minmax(0, 1.55fr) minmax(340px, 0.8fr);
}

.bottom-grid {
  grid-template-columns: minmax(0, 1.2fr) minmax(360px, 0.8fr);
  margin-top: 16px;
}

.panel {
  min-height: 320px;
  padding: 18px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
}

.panel-head h2 {
  margin: 0 0 6px;
  color: #f7fbff;
  font-size: 18px;
}

.panel-head p {
  margin: 0;
}

.panel-count {
  color: #54e6c0;
  font-size: 24px;
  font-weight: 800;
}

.trend-chart,
.ranking-list,
.equipment-list,
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.trend-row {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) 112px;
  align-items: center;
  gap: 12px;
}

.trend-bars {
  position: relative;
  height: 28px;
  overflow: hidden;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.06);
}

.bar {
  position: absolute;
  left: 0;
  height: 11px;
  border-radius: 8px;
}

.bar.sessions {
  top: 3px;
  background: linear-gradient(90deg, #36a3ff, #63e7ff);
}

.bar.reps {
  bottom: 3px;
  background: linear-gradient(90deg, #25d6a4, #9cffd9);
}

.rank-item,
.equipment-row,
.activity-item {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
  padding: 12px;
  border: 1px solid rgba(132, 179, 255, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.045);
}

.rank-item b {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border-radius: 8px;
  background: rgba(55, 155, 255, 0.16);
  color: #67d9ff;
}

.rank-main,
.equipment-name,
.activity-item div {
  min-width: 0;
  flex: 1;
}

.rank-main strong,
.equipment-name strong,
.activity-item strong {
  display: block;
  overflow: hidden;
  color: #f8fbff;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-metric,
.equipment-data {
  min-width: 68px;
  text-align: right;
}

.rank-metric strong,
.equipment-data strong {
  display: block;
  color: #54e6c0;
  font-size: 20px;
}

.equipment-row {
  display: grid;
  grid-template-columns: minmax(150px, 1fr) minmax(150px, 0.8fr) 88px 88px;
}

.equipment-sensor {
  min-width: 0;
}

.equipment-sensor span {
  display: block;
  overflow: hidden;
  margin-top: 6px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.activity-item i {
  width: 8px;
  height: 42px;
  border-radius: 8px;
  background: linear-gradient(180deg, #36a3ff, #25d6a4);
}

.activity-item time {
  flex-shrink: 0;
}

:deep(.el-empty__description p) {
  color: #8ea2bd;
}

@media (max-width: 1200px) {
  .kpi-grid,
  .main-grid,
  .bottom-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .home {
    padding: 14px;
  }

  .hero-panel,
  .hero-actions {
    flex-direction: column;
  }

  .kpi-grid,
  .main-grid,
  .bottom-grid,
  .equipment-row {
    grid-template-columns: 1fr;
  }

  .trend-row {
    grid-template-columns: 42px minmax(0, 1fr);
  }

  .trend-row em {
    grid-column: 2;
  }
}
</style>
