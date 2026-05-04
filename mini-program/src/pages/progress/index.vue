<!--
  进度追踪页面 - 昕动智能小程序
  功能: 依从率、连续天数、趋势图、历史记录
-->
<template>
  <view class="progress-page">
    <!-- 页面头部 -->
    <view class="page-header">
      <text class="page-title">训练进度</text>
      <text class="period-label" @tap="refreshProgress">近30天 · 刷新</text>
    </view>

    <!-- 概览统计卡片 -->
    <view class="overview-card">
      <view class="stat-row">
        <view class="stat-item">
          <text class="stat-value">{{ streakDays }}</text>
          <text class="stat-label">连续训练</text>
          <text class="stat-unit">天</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-value">{{ complianceRate }}</text>
          <text class="stat-label">依从率</text>
          <text class="stat-unit">%</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-value">{{ totalSessions }}</text>
          <text class="stat-label">累计训练</text>
          <text class="stat-unit">次</text>
        </view>
      </view>
    </view>

    <!-- 本周趋势 -->
    <view class="week-trend-card">
      <text class="card-title">本周趋势</text>
      <view class="week-bars">
        <view
          v-for="(day, idx) in weekDays"
          :key="idx"
          class="day-col"
        >
          <view class="bar-wrap">
            <view
              class="bar"
              :style="{ height: day.minutes > 0 ? (day.minutes / maxWeekMinutes * 100) + '%' : '4rpx' }"
              :class="{ active: day.isToday, done: day.minutes > 0 }"
            ></view>
          </view>
          <text class="day-label" :class="{ today: day.isToday }">{{ day.label }}</text>
        </view>
      </view>
      <!-- 趋势指示 -->
      <view class="trend-indicator" v-if="trendDirection !== 0">
        <text class="trend-arrow">{{ trendDirection > 0 ? '↑' : '↓' }}</text>
        <text class="trend-text">
          {{ Math.abs(trendPct) }}% vs上周
        </text>
      </view>
    </view>

    <!-- 依从率详情 -->
    <view class="compliance-card">
      <view class="compliance-header">
        <text class="card-title">依从率详情</text>
        <text class="compliance-rate-large">{{ complianceRate }}%</text>
      </view>
      <view class="compliance-bar-wrap">
        <view class="compliance-bar" :style="{ width: complianceRate + '%' }"></view>
      </view>
      <view class="compliance-legend">
        <view class="legend-item">
          <view class="legend-dot" style="background:#52c41a"></view>
          <text>达标 {{ doneTasks }} 个任务</text>
        </view>
        <view class="legend-item">
          <view class="legend-dot" style="background:#ff4d4f"></view>
          <text>未完成 {{ missedTasks }} 个任务</text>
        </view>
      </view>
    </view>

    <!-- 成就徽章区 -->
    <view class="badges-card" v-if="badges.length > 0">
      <text class="card-title">已获得徽章</text>
      <view class="badge-list">
        <view
          v-for="(badge, idx) in badges"
          :key="idx"
          class="badge-item"
        >
          <text class="badge-icon">{{ badge.icon }}</text>
          <text class="badge-name">{{ badge.name }}</text>
        </view>
      </view>
    </view>

    <!-- 历史记录列表 -->
    <view class="history-section">
      <text class="section-title">训练记录</text>
      <view class="history-list">
        <view
          v-for="(record, idx) in historyRecords"
          :key="idx"
          class="history-item"
          @tap="onRecordTap(record)"
        >
          <view class="history-date-col">
            <text class="history-date">{{ record.dateStr }}</text>
            <text class="history-weekday">{{ record.weekday }}</text>
          </view>
          <view class="history-content">
            <text class="history-exercise">{{ record.exerciseName }}</text>
            <text class="history-detail">
              {{ record.totalSets }}组 × {{ record.totalReps }}次
              <text v-if="record.peakLoadKg"> | {{ record.peakLoadKg }}kg</text>
            </text>
          </view>
          <view class="history-meta">
            <text class="history-duration">{{ record.durationMin }}分钟</text>
          </view>
        </view>
      </view>

      <!-- 加载更多 -->
      <view v-if="hasMore" class="load-more" @tap="loadMoreHistory">
        <text>{{ loadingMore ? '加载中...' : '加载更多' }}</text>
      </view>
      <view v-else class="no-more">
        <text>— 已加载全部 —</text>
      </view>
    </view>

    <!-- 空状态 -->
    <view v-if="!loading && historyRecords.length === 0" class="empty-state">
      <text class="empty-icon">📊</text>
      <text class="empty-title">暂无训练记录</text>
      <text class="empty-desc">开始训练后数据将在这里展示</text>
      <view class="empty-action" @tap="goTraining">
        <text>去开始训练</text>
      </view>
    </view>
  </view>
</template>

<script lang="ts">
import { defineComponent, ref, computed, onMounted } from 'vue';
import { useDidShow } from '@tarojs/taro';
import { getTrainingHistory, getCurrentUser, isDemoMode } from '../../services/api';

export default defineComponent({
  setup() {
    const loading = ref(true);
    const loadingMore = ref(false);
    const streakDays = ref(0);
    const complianceRate = ref(0);
    const totalSessions = ref(0);
    const doneTasks = ref(0);
    const missedTasks = ref(0);
    const badges = ref([]);
    const historyRecords = ref([]);
    const weekDays = ref([]);
    const maxWeekMinutes = ref(1);
    const trendDirection = ref(0);
    const trendPct = ref(0);
    const hasMore = ref(false);
    const historyTotal = ref(0);
    const page = ref(1);
    const pageSize = 10;

    onMounted(async () => {
      loading.value = true;
      try {
        await Promise.all([loadUserStats(), loadHistory(true)]);
        computeStatsFromHistory();
        computeWeekDays();
      } catch (err) {
        console.error('[Progress] Load failed', err);
        if (isDemoMode()) {
          fillMockData();
        } else {
          resetProgressData();
          wx.showToast({ title: '进度加载失败', icon: 'none' });
        }
      } finally {
        loading.value = false;
      }
    });

    // 修复：Tab页切换时 useDidShow 会触发，重新加载数据
    useDidShow(() => {
      console.log('[Progress] useDidShow triggered, checking login...');
      const token = wx.getStorageSync('auth_token');
      if (!token) {
        wx.reLaunch({ url: '/pages/login/index' });
        return;
      }
      // 刷新用户统计数据（不重置历史列表）
      loadUserStats().catch(e => console.warn('[Progress] refresh user stats failed', e));
    });

    async function loadUserStats() {
      try {
        const res = await getCurrentUser();
        const user = res.data || {};
        streakDays.value = user.streakDays || 0;
        complianceRate.value = user.complianceRate || 72;
        totalSessions.value = user.totalSessions || 0;
      } catch (e) {
        console.warn('[Progress] getCurrentUser failed', e);
        if (isDemoMode()) {
          streakDays.value = 7;
          complianceRate.value = 72;
          totalSessions.value = 23;
          doneTasks.value = 18;
          missedTasks.value = 7;
          badges.value = [
            { icon: '🔥', name: '连续7天' },
            { icon: '💪', name: '力量新手' },
            { icon: '📅', name: '坚持30天' },
          ];
          return;
        }
        streakDays.value = 0;
        complianceRate.value = 0;
        totalSessions.value = 0;
        doneTasks.value = 0;
        missedTasks.value = 0;
        badges.value = [];
      }
    }

    async function loadHistory(reset = false) {
      try {
        if (reset) {
          page.value = 1;
        }
        const res = await getTrainingHistory({ page: page.value, size: pageSize });
        const data = res.data || {};
        const records = data.records || [];
        historyTotal.value = data.total || records.length;
        const formatted = records.map(r => formatRecord(r));
        historyRecords.value = reset ? formatted : historyRecords.value.concat(formatted);
        hasMore.value = historyTotal.value > page.value * pageSize;
      } catch (e) {
        console.warn('[Progress] getTrainingHistory failed', e);
        historyRecords.value = isDemoMode() ? getMockHistory() : [];
        historyTotal.value = historyRecords.value.length;
        hasMore.value = false;
      }
    }

    async function loadMoreHistory() {
      if (loadingMore.value) return;
      loadingMore.value = true;
      page.value++;
      try {
        await loadHistory(false);
        computeStatsFromHistory();
        computeWeekDays();
      } catch (e) {
        console.warn('[Progress] loadMore failed', e);
        hasMore.value = false;
        wx.showToast({ title: '加载失败', icon: 'none' });
      } finally {
        loadingMore.value = false;
      }
    }

    function computeWeekDays() {
      const days = [];
      const now = new Date();
      const labels = ['日', '一', '二', '三', '四', '五', '六'];
      const minutesByDate = historyRecords.value.reduce((acc, record) => {
        if (!record.dateKey) return acc;
        acc[record.dateKey] = (acc[record.dateKey] || 0) + Number(record.durationMin || 0);
        return acc;
      }, {});

      for (let i = 6; i >= 0; i--) {
        const d = new Date(now);
        d.setDate(d.getDate() - i);
        const dateStr = `${d.getMonth() + 1}/${d.getDate()}`;
        const isToday = i === 0;
        const minutes = minutesByDate[toDateKey(d)] || 0;
        days.push({ label: labels[d.getDay()], dateStr, isToday, minutes });
      }
      weekDays.value = days;
      maxWeekMinutes.value = Math.max(...days.map(d => d.minutes), 1);
      const recent = days.slice(4).reduce((sum, d) => sum + d.minutes, 0);
      const previous = days.slice(0, 3).reduce((sum, d) => sum + d.minutes, 0);
      if (previous === 0 && recent === 0) {
        trendDirection.value = 0;
        trendPct.value = 0;
      } else if (previous === 0) {
        trendDirection.value = 1;
        trendPct.value = 100;
      } else {
        const diffPct = Math.round(((recent - previous) / previous) * 100);
        trendDirection.value = diffPct === 0 ? 0 : diffPct > 0 ? 1 : -1;
        trendPct.value = Math.abs(diffPct);
      }
    }

    function fillMockData() {
      streakDays.value = 7;
      complianceRate.value = 72;
      totalSessions.value = 23;
      doneTasks.value = 18;
      missedTasks.value = 7;
      badges.value = [
        { icon: '🔥', name: '连续7天' },
        { icon: '💪', name: '力量新手' },
      ];
      computeWeekDays();
    }

    function resetProgressData() {
      streakDays.value = 0;
      complianceRate.value = 0;
      totalSessions.value = 0;
      doneTasks.value = 0;
      missedTasks.value = 0;
      badges.value = [];
      historyRecords.value = [];
      historyTotal.value = 0;
      hasMore.value = false;
      computeWeekDays();
    }

    function getMockHistory() {
      const records = [];
      const now = new Date();
      const exercises = [
        { name: '平板杠铃卧推', sets: 4, reps: 10, load: 40 },
        { name: '哑铃上斜卧推', sets: 3, reps: 12, load: 16 },
        { name: '高位下拉', sets: 4, reps: 10, load: 35 },
        { name: '坐姿划船', sets: 3, reps: 12, load: 30 },
        { name: '平板卧推', sets: 4, reps: 8, load: 50 },
        { name: '哑铃飞鸟', sets: 3, reps: 15, load: 12 },
      ];
      const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];

      for (let i = 0; i < 8; i++) {
        const d = new Date(now);
        d.setDate(d.getDate() - i * 2);
        const ex = exercises[i % exercises.length];
        records.push({
          dateStr: `${d.getMonth() + 1}月${d.getDate()}日`,
          dateKey: toDateKey(d),
          weekday: weekdays[d.getDay()],
          exerciseName: ex.name,
          totalSets: ex.sets,
          totalReps: ex.reps,
          peakLoadKg: ex.load,
          durationMin: Math.floor(Math.random() * 30 + 30),
        });
      }
      return records;
    }

    function formatRecord(r) {
      const rawDate = r.date || r.sessionTime || r.session_time || r.createTime || r.create_time || r.startedAt || r.started_at || Date.now();
      const d = new Date(rawDate);
      const safeDate = Number.isNaN(d.getTime()) ? new Date() : d;
      const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
      const completedSets = Number(r.totalSets ?? r.completedSets ?? r.completed_sets ?? r.sets ?? 0);
      const totalRepsValue = Number(r.totalReps ?? r.total_reps ?? r.reps ?? 0);
      const volumeValue = Number(r.peakLoadKg ?? r.peak_load_kg ?? r.totalVolumeKg ?? r.total_volume_kg ?? r.loadKg ?? 0);
      const durationValue = Number(r.durationMin ?? r.duration_min ?? r.duration ?? 0);
      return {
        sessionId: r.sessionId || r.session_id || r.id,
        dateStr: `${safeDate.getMonth() + 1}月${safeDate.getDate()}日`,
        dateKey: toDateKey(safeDate),
        weekday: weekdays[safeDate.getDay()],
        exerciseName: r.exerciseName || r.exercise_name || r.equipmentName || r.equipment_name || r.exerciseType || r.exercise_type || '综合训练',
        equipmentCode: r.equipmentCode || r.equipment_code || '',
        deviceCode: r.deviceCode || r.device_code || '',
        totalSets: completedSets,
        totalReps: totalRepsValue,
        peakLoadKg: volumeValue,
        durationMin: durationValue,
        raw: r,
      };
    }

    function toDateKey(date) {
      const y = date.getFullYear();
      const m = String(date.getMonth() + 1).padStart(2, '0');
      const d = String(date.getDate()).padStart(2, '0');
      return `${y}-${m}-${d}`;
    }

    function computeStatsFromHistory() {
      const records = historyRecords.value;
      const trainDates = new Set(records.map(r => r.dateKey).filter(Boolean));
      totalSessions.value = Math.max(historyTotal.value || 0, records.length);
      doneTasks.value = trainDates.size;
      missedTasks.value = Math.max(0, 30 - doneTasks.value);
      complianceRate.value = Math.min(100, Math.round((doneTasks.value / 30) * 100));
      streakDays.value = computeStreakDays(trainDates);
      badges.value = buildBadges();
    }

    function computeStreakDays(trainDates) {
      let streak = 0;
      const cursor = new Date();
      for (let i = 0; i < 60; i++) {
        if (trainDates.has(toDateKey(cursor))) {
          streak++;
          cursor.setDate(cursor.getDate() - 1);
        } else if (i === 0) {
          cursor.setDate(cursor.getDate() - 1);
        } else {
          break;
        }
      }
      return streak;
    }

    function buildBadges() {
      const result = [];
      if (streakDays.value >= 3) result.push({ icon: '🔥', name: `连续${streakDays.value}天` });
      if (totalSessions.value >= 1) result.push({ icon: '💪', name: '完成首训' });
      if (totalSessions.value >= 10) result.push({ icon: '📅', name: '坚持训练' });
      return result;
    }

    async function refreshProgress() {
      loading.value = true;
      try {
        await Promise.all([loadUserStats(), loadHistory(true)]);
        computeStatsFromHistory();
        computeWeekDays();
        wx.showToast({ title: '已刷新', icon: 'success' });
      } catch (e) {
        console.error('[Progress] refresh failed', e);
        wx.showToast({ title: '刷新失败', icon: 'none' });
      } finally {
        loading.value = false;
      }
    }

    function onRecordTap(record) {
      const lines = [
        `训练: ${record.exerciseName}`,
        `组数/次数: ${record.totalSets}组 × ${record.totalReps}次`,
        `时长: ${record.durationMin || 0}分钟`,
      ];
      if (record.equipmentCode) lines.push(`器械: ${record.equipmentCode}`);
      if (record.deviceCode) lines.push(`IMU: ${record.deviceCode}`);
      wx.showModal({
        title: record.dateStr,
        content: lines.join('\n'),
        confirmText: '去训练',
        cancelText: '关闭',
        success: (res) => {
          if (res.confirm) {
            goTraining(record);
          }
        },
      });
    }

    function goTraining(record) {
      const query = record?.equipmentCode
        ? `?equipmentCode=${encodeURIComponent(record.equipmentCode)}&exerciseName=${encodeURIComponent(record.exerciseName || '')}`
        : '';
      wx.navigateTo({
        url: `/pages/device-binding/index${query}`,
        fail: () => wx.switchTab({ url: '/pages/training/index' }),
      });
    }

    return {
      loading,
      loadingMore,
      streakDays,
      complianceRate,
      totalSessions,
      doneTasks,
      missedTasks,
      badges,
      historyRecords,
      weekDays,
      maxWeekMinutes,
      trendDirection,
      trendPct,
      hasMore,
      loadMoreHistory,
      refreshProgress,
      onRecordTap,
      goTraining,
    };
  },
});
</script>

<style>
.progress-page {
  padding: 24rpx;
  background: #f5f5f5;
  min-height: 100vh;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}
.page-title {
  font-size: 40rpx;
  font-weight: bold;
  color: #1a1a2e;
}
.period-label {
  font-size: 26rpx;
  color: #999;
  background: #f0f0f0;
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
}
.overview-card {
  background: linear-gradient(135deg, #4A90E2, #6BB5FF);
  border-radius: 16rpx;
  padding: 36rpx 24rpx;
  margin-bottom: 24rpx;
}
.stat-row {
  display: flex;
  align-items: center;
  justify-content: space-around;
}
.stat-item {
  text-align: center;
  flex: 1;
}
.stat-value {
  font-size: 52rpx;
  font-weight: bold;
  color: #fff;
  display: inline-block;
}
.stat-label {
  font-size: 22rpx;
  color: rgba(255,255,255,0.7);
  display: block;
  margin-top: 4rpx;
}
.stat-unit {
  font-size: 22rpx;
  color: rgba(255,255,255,0.7);
}
.stat-divider {
  width: 1rpx;
  height: 60rpx;
  background: rgba(255,255,255,0.3);
}
.week-trend-card,
.compliance-card,
.badges-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
}
.card-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #1a1a2e;
  display: block;
  margin-bottom: 20rpx;
}
.week-bars {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  height: 120rpx;
  padding: 0 8rpx;
}
.day-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}
.bar-wrap {
  flex: 1;
  width: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.bar {
  width: 32rpx;
  min-height: 4rpx;
  background: #e0e0e0;
  border-radius: 4rpx 4rpx 0 0;
  transition: height 0.3s ease;
}
.bar.done { background: #b3d4fc; }
.bar.active { background: #4A90E2; }
.day-label {
  font-size: 22rpx;
  color: #999;
  margin-top: 8rpx;
}
.day-label.today { color: #4A90E2; font-weight: 600; }
.trend-indicator {
  display: flex;
  align-items: center;
  margin-top: 16rpx;
  justify-content: flex-end;
}
.trend-arrow { font-size: 28rpx; color: #52c41a; margin-right: 6rpx; }
.trend-text { font-size: 24rpx; color: #52c41a; }
.compliance-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}
.compliance-rate-large {
  font-size: 36rpx;
  font-weight: bold;
  color: #4A90E2;
}
.compliance-bar-wrap {
  height: 16rpx;
  background: #ff4d4f;
  border-radius: 8rpx;
  overflow: hidden;
  margin-bottom: 16rpx;
}
.compliance-bar {
  height: 100%;
  background: #52c41a;
  border-radius: 8rpx;
  transition: width 0.3s ease;
}
.compliance-legend {
  display: flex;
  gap: 24rpx;
}
.legend-item {
  display: flex;
  align-items: center;
  font-size: 24rpx;
  color: #666;
}
.legend-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  margin-right: 8rpx;
}
.badge-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}
.badge-item {
  background: #f0f7ff;
  border-radius: 12rpx;
  padding: 16rpx 20rpx;
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.badge-icon { font-size: 28rpx; }
.badge-name { font-size: 24rpx; color: #4A90E2; }
.history-section { margin-top: 8rpx; }
.section-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1a1a2e;
  display: block;
  margin-bottom: 16rpx;
}
.history-list {}
.history-item {
  background: #fff;
  border-radius: 12rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  display: flex;
  align-items: center;
}
.history-date-col {
  margin-right: 20rpx;
  min-width: 100rpx;
}
.history-date {
  font-size: 26rpx;
  font-weight: 600;
  color: #1a1a2e;
  display: block;
}
.history-weekday {
  font-size: 22rpx;
  color: #999;
}
.history-content { flex: 1; }
.history-exercise {
  font-size: 30rpx;
  font-weight: 600;
  color: #1a1a2e;
  display: block;
  margin-bottom: 6rpx;
}
.history-detail {
  font-size: 24rpx;
  color: #999;
}
.history-meta { text-align: right; }
.history-duration {
  font-size: 26rpx;
  color: #4A90E2;
  font-weight: 600;
}
.history-item:active,
.load-more:active,
.period-label:active,
.empty-action:active {
  opacity: 0.75;
}
.load-more {
  text-align: center;
  padding: 24rpx;
  color: #4A90E2;
  font-size: 28rpx;
}
.no-more {
  text-align: center;
  padding: 24rpx;
  color: #ccc;
  font-size: 24rpx;
}
.empty-state {
  text-align: center;
  padding: 100rpx 40rpx;
}
.empty-icon { font-size: 80rpx; display: block; margin-bottom: 20rpx; }
.empty-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1a1a2e;
  display: block;
  margin-bottom: 12rpx;
}
.empty-desc { font-size: 26rpx; color: #999; }
.empty-action {
  display: inline-flex;
  margin-top: 28rpx;
  padding: 16rpx 36rpx;
  border-radius: 999rpx;
  background: #4A90E2;
  color: #fff;
  font-size: 28rpx;
  font-weight: 600;
}
</style>
