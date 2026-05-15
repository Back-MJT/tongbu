<!--
  每日任务页面 - 昕动智能小程序 (XIN-121 FE)
  功能: 今日训练计划列表、AE算法训练建议、实时组数进度、教练提示
-->
<template>
  <view class="daily-task-page">
    <!-- 页面头部 -->
    <view class="page-header">
      <text class="page-title">今日训练</text>
      <view class="page-meta">
        <text class="date">{{ today }}</text>
        <text class="refresh-link" @tap="loadDailyTask">{{ loading ? '同步中' : '刷新' }}</text>
      </view>
    </view>

    <!-- 训练状态卡片 -->
    <view class="session-card">
      <view class="session-info">
        <text class="device-name">{{ venueName || '未选择场馆' }}</text>
        <text class="session-status" :class="sessionStatusClass">
          {{ sessionStatusText }}
        </text>
      </view>
      <view class="session-progress">
        <text class="progress-text">
          <text class="progress-current">{{ completedSets }}</text>
          <text class="progress-sep">/</text>
          <text class="progress-total">{{ totalSets }}组</text>
        </text>
        <text class="reps-text">{{ completedReps }}次 / {{ totalReps }}次</text>
      </view>
      <!-- 进度条 -->
      <view class="progress-bar-wrap">
        <view class="progress-bar" :style="{ width: progressPercent + '%' }"></view>
      </view>
      <!-- 运动目标 -->
      <view class="exercise-goal" v-if="exerciseGoal">
        <text class="goal-label">🎯 {{ exerciseGoal }}</text>
      </view>
      <view class="venue-action" v-if="!hasVenue" @tap="goHome">
        <text>去首页选择场馆</text>
      </view>
    </view>

    <!-- AI训练建议 (AE算法) -->
    <view class="ai-suggestion-card" v-if="aiSuggestion">
      <view class="ai-header">
        <text class="ai-icon">💡</text>
        <text class="ai-title">AI训练建议</text>
        <text class="ai-algo" v-if="algoVersion">AE</text>
      </view>
      <text class="ai-content">{{ aiSuggestion }}</text>
      <view v-if="coachingReasoning" class="ai-reasoning">
        <text class="reasoning-label">决策依据</text>
        <text class="reasoning-text">{{ coachingReasoning }}</text>
      </view>
    </view>

    <!-- 训练任务列表 -->
    <view class="tasks-section" v-if="tasks.length > 0">
      <text class="section-title">训练任务</text>
      <view class="task-list">
        <view
          v-for="(task, index) in tasks"
          :key="index"
          class="task-item"
          :class="task.status"
          @tap="onTaskClick(task)"
        >
          <view class="task-status-icon">
            <text v-if="task.status === 'completed'">✓</text>
            <text v-else-if="task.status === 'in_progress'">▶</text>
            <text v-else>○</text>
          </view>
          <view class="task-content">
            <text class="task-name">{{ task.exerciseName }}</text>
            <text class="task-detail">
              {{ task.targetSets }}组 × {{ task.targetReps }}次
              <text v-if="task.targetLoadKg"> | {{ task.targetLoadKg }}kg</text>
              <text v-if="task.restSeconds"> | 休{{ task.restSeconds }}s</text>
            </text>
            <!-- 教练提示 -->
            <text class="task-tip" v-if="task.coachingTip">💡 {{ task.coachingTip }}</text>
            <!-- 强度标签 -->
            <text class="intensity-tag" v-if="task.intensityLabel" :class="getIntensityClass(task.intensityLabel)">
              {{ task.intensityLabel }}
            </text>
          </view>
          <view class="task-action">
            <text v-if="task.status === 'completed'" class="tag-done">已完成</text>
            <text v-else-if="task.status === 'in_progress'" class="tag-active">进行中</text>
            <text v-else class="tag-pending">开始</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 健康提示 -->
    <view class="health-tips" v-if="healthTips.length > 0">
      <text class="section-title">健康提示</text>
      <view
        v-for="(tip, idx) in healthTips"
        :key="idx"
        class="tip-card"
      >
        <view class="tip-header">
          <text class="tip-icon">{{ tip.category === 'nutrition' ? '🍎' : '💪' }}</text>
          <text class="tip-title">{{ tip.title }}</text>
        </view>
        <text class="tip-content">{{ tip.content }}</text>
      </view>
    </view>

    <!-- 加载状态 -->
    <view v-if="loading" class="loading-mask">
      <text>AI教练正在生成方案...</text>
    </view>

    <!-- 空状态 -->
    <view v-if="!loading && tasks.length === 0" class="empty-state">
      <text class="empty-icon">🏋️</text>
      <text class="empty-title">暂无今日训练任务</text>
      <text class="empty-desc">{{ loadError || '选择场馆后自动获取今日训练任务' }}</text>
      <view class="empty-actions">
        <view class="empty-action primary" @tap="loadDailyTask">
          <text>重新加载</text>
        </view>
        <view class="empty-action" @tap="goHome">
          <text>选择场馆</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { defineComponent, ref, computed, onMounted } from 'vue';
import { useDidShow } from '@tarojs/taro';
import {
  getCurrentVenue,
  getMiniprogramPrescription,
} from '../../services/api';

export default defineComponent({
  setup() {
    const loading = ref(true);
    const loadError = ref('');
    const today = ref('');
    const tasks = ref([]);
    const completedSets = ref(0);
    const completedReps = ref(0);
    const totalSets = ref(0);
    const totalReps = ref(0);
    const aiSuggestion = ref('');
    const coachingReasoning = ref('');
    const exerciseGoal = ref('');
    const algoVersion = ref('');
    const healthTips = ref([]);
    const venueName = ref('');
    const venueId = ref('');
    const exerciseType = ref('strength');

    function initToday() {
      const now = new Date();
      today.value = `${now.getMonth() + 1}月${now.getDate()}日 ${['日','一','二','三','四','五','六'][now.getDay()]}`;
    }

    onMounted(async () => {
      initToday();
      await loadDailyTask();
    });

    useDidShow(() => {
      const token = wx.getStorageSync('auth_token');
      const loginType = wx.getStorageSync('login_type');
      if (!token && loginType !== 'demo') {
        wx.reLaunch({ url: '/pages/login/index' });
        return;
      }
      initToday();
      loadDailyTask();
    });

    function resetPlanState() {
      tasks.value = [];
      completedSets.value = 0;
      completedReps.value = 0;
      totalSets.value = 0;
      totalReps.value = 0;
      aiSuggestion.value = '';
      coachingReasoning.value = '';
      exerciseGoal.value = '';
      algoVersion.value = '';
      healthTips.value = [];
    }

    async function loadDailyTask() {
      loading.value = true;
      loadError.value = '';
      resetPlanState();

      try {
        venueId.value = wx.getStorageSync('current_venue_id') || '';
        venueName.value = wx.getStorageSync('current_venue_name') || '智能力量站';
        if (!venueId.value) {
          try {
            const venueRes = await getCurrentVenue();
            if (venueRes.data?.venueId) {
              venueId.value = String(venueRes.data.venueId);
              venueName.value = venueRes.data.venueName || venueName.value;
              wx.setStorageSync('current_venue_id', venueId.value);
              wx.setStorageSync('current_venue_name', venueName.value);
            }
          } catch (e) {
            console.warn('[DailyTask] getCurrentVenue failed', e);
          }
        }
        if (!venueId.value) {
          loadError.value = '请先在首页选择场馆';
          return;
        }

        try {
          const userId = wx.getStorageSync('user_id') || 'demo-user';
          const age = wx.getStorageSync('user_age') || 30;

          const rxRes = await getMiniprogramPrescription({
            user_id: userId,
            age: age,
            gender: wx.getStorageSync('user_gender') || undefined,
            device_type: exerciseType.value,
            venueId: venueId.value,
            sessions_last_30_days: wx.getStorageSync('sessions_30d') || undefined,
          });

          if (rxRes.code === 200 && rxRes.data) {
            const rx = rxRes.data;
            tasks.value = rx.tasks || [];
            aiSuggestion.value = rx.aiSuggestion || '';
            coachingReasoning.value = rx.coachingReasoning || '';
            exerciseGoal.value = rx.exerciseGoal || '';
            algoVersion.value = rx.algorithmVersion || '';
            healthTips.value = rx.healthTips || [];

            // 计算总数
            totalSets.value = tasks.value.reduce((sum, t) => sum + (t.targetSets || 0), 0);
            totalReps.value = tasks.value.reduce((sum, t) => sum + (t.targetReps || 0), 0);
            completedSets.value = rx.completedSessions || 0;
            completedReps.value = rx.totalReps || 0;
            if (tasks.value.length === 0) {
              loadError.value = '当前场馆暂未配置今日训练任务';
            }
          }
        } catch (e) {
          console.warn('[DailyTask] prescription failed', e);
          resetPlanState();
          loadError.value = '后台连接异常，训练方案暂未同步，请重试';
        }
      } catch (err) {
        console.error('[DailyTask] Load failed', err);
        loadError.value = '今日训练加载失败，请稍后重试';
      } finally {
        loading.value = false;
      }
    }

    const progressPercent = computed(() => {
      if (totalSets.value === 0) return 0;
      return Math.min(100, Math.round((completedSets.value / totalSets.value) * 100));
    });

    const sessionStatusClass = computed(() => {
      if (completedSets.value === 0) return 'pending';
      if (completedSets.value >= totalSets.value) return 'done';
      return 'active';
    });

    const sessionStatusText = computed(() => {
      if (!hasVenue.value) return '请选择场馆';
      if (completedSets.value === 0) return '等待开始';
      if (completedSets.value >= totalSets.value) return '今日已完成 ✓';
      return '训练中';
    });

    const hasVenue = computed(() => {
      return !!venueId.value;
    });

    function onTaskClick(task) {
      if (task.status === 'completed') return;
      const params = [
        `taskId=${encodeURIComponent(task.taskId || '')}`,
        `exerciseName=${encodeURIComponent(task.exerciseName || '')}`,
        `exerciseType=${encodeURIComponent(task.exerciseType || 'strength')}`,
        `expectedEquipmentCode=${encodeURIComponent(task.equipmentCode || '')}`,
        `targetSets=${encodeURIComponent(task.targetSets || '')}`,
        `targetReps=${encodeURIComponent(task.targetReps || '')}`,
        `targetLoadKg=${encodeURIComponent(task.targetLoadKg || '')}`,
      ].join('&');
      wx.navigateTo({ url: `/pages/device-binding/index?${params}` });
    }

    function goHome() {
      wx.switchTab({ url: '/pages/home/index' });
    }

    function getIntensityClass(label) {
      const classMap = {
        '低': 'intensity-low',
        '中等': 'intensity-medium',
        '中高': 'intensity-medium-high',
        '高': 'intensity-high',
      };
      return classMap[label] || 'intensity-medium';
    }

    return {
      loading,
      loadError,
      today,
      tasks,
      completedSets,
      completedReps,
      totalSets,
      totalReps,
      aiSuggestion,
      coachingReasoning,
      exerciseGoal,
      algoVersion,
      healthTips,
      venueName,
      progressPercent,
      sessionStatusClass,
      sessionStatusText,
      hasVenue,
      loadDailyTask,
      onTaskClick,
      getIntensityClass,
      goHome,
    };
  },
});
</script>

<style>
.daily-task-page {
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
.date {
  font-size: 26rpx;
  color: #999;
}
.page-meta {
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.refresh-link {
  font-size: 24rpx;
  color: #4A90E2;
  font-weight: 600;
}
.session-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
}
.session-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}
.device-name {
  font-size: 32rpx;
  font-weight: 600;
  color: #1a1a2e;
}
.session-status {
  font-size: 26rpx;
}
.session-status.pending { color: #999; }
.session-status.active { color: #4A90E2; }
.session-status.done { color: #52c41a; }
.session-progress {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16rpx;
}
.progress-text {
  font-size: 48rpx;
  font-weight: bold;
}
.progress-current { color: #4A90E2; }
.progress-sep { color: #ccc; }
.progress-total { color: #1a1a2e; }
.reps-text {
  font-size: 28rpx;
  color: #666;
  line-height: 80rpx;
}
.progress-bar-wrap {
  height: 12rpx;
  background: #eee;
  border-radius: 6rpx;
  overflow: hidden;
  margin-bottom: 16rpx;
}
.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #4A90E2, #6BB5FF);
  border-radius: 6rpx;
  transition: width 0.3s ease;
}
.exercise-goal {
  margin-top: 12rpx;
  padding-top: 12rpx;
  border-top: 1rpx solid #f0f0f0;
}
.goal-label {
  font-size: 26rpx;
  color: #4A90E2;
  font-weight: 500;
}
.venue-action {
  margin-top: 16rpx;
  height: 72rpx;
  border-radius: 36rpx;
  background: #eef7ff;
  color: #2f80d1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  font-weight: 600;
}
.ai-suggestion-card {
  background: linear-gradient(135deg, #f0f7ff, #e8f4ff);
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
  border-left: 6rpx solid #4A90E2;
}
.ai-header {
  display: flex;
  align-items: center;
  margin-bottom: 12rpx;
}
.ai-icon { font-size: 32rpx; margin-right: 10rpx; }
.ai-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #4A90E2;
  flex: 1;
}
.ai-algo {
  font-size: 20rpx;
  color: #fff;
  background: #4A90E2;
  padding: 2rpx 10rpx;
  border-radius: 10rpx;
}
.ai-content {
  font-size: 28rpx;
  color: #333;
  line-height: 1.6;
  display: block;
  margin-bottom: 12rpx;
}
.ai-reasoning {
  background: rgba(74, 144, 226, 0.08);
  border-radius: 12rpx;
  padding: 14rpx;
}
.reasoning-label {
  font-size: 22rpx;
  color: #4A90E2;
  font-weight: 600;
  display: block;
  margin-bottom: 4rpx;
}
.reasoning-text {
  font-size: 24rpx;
  color: #666;
  line-height: 1.5;
}
.tasks-section { margin-top: 8rpx; }
.section-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1a1a2e;
  display: block;
  margin-bottom: 16rpx;
}
.task-list {}
.task-item {
  background: #fff;
  border-radius: 12rpx;
  padding: 28rpx;
  margin-bottom: 16rpx;
  display: flex;
  align-items: center;
}
.task-item.in_progress {
  border-left: 4rpx solid #4A90E2;
}
.task-item.completed {
  opacity: 0.75;
}
.task-status-icon {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
  font-size: 26rpx;
  flex-shrink: 0;
}
.task-item.completed .task-status-icon { background: #e8f5e9; color: #52c41a; }
.task-item.in_progress .task-status-icon { background: #e3f2fd; color: #4A90E2; }
.task-content { flex: 1; }
.task-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #1a1a2e;
  display: block;
  margin-bottom: 6rpx;
}
.task-detail {
  font-size: 24rpx;
  color: #999;
  display: block;
  margin-bottom: 4rpx;
}
.task-tip {
  font-size: 22rpx;
  color: #666;
  display: block;
  margin-bottom: 4rpx;
  line-height: 1.4;
}
.intensity-tag {
  font-size: 20rpx;
  padding: 2rpx 10rpx;
  border-radius: 8rpx;
  display: inline-block;
  margin-right: 8rpx;
}
.intensity-tag.intensity-low { background: #e8f5e9; color: #4caf50; }
.intensity-tag.intensity-medium { background: #fff3e0; color: #ff9800; }
.intensity-tag.intensity-medium-high { background: #fce4ec; color: #e91e63; }
.intensity-tag.intensity-high { background: #ffebee; color: #f44336; }
.task-action { flex-shrink: 0; }
.tag-done { font-size: 24rpx; color: #52c41a; }
.tag-active { font-size: 24rpx; color: #4A90E2; font-weight: 600; }
.tag-pending { font-size: 24rpx; color: #999; }

/* 健康提示 */
.health-tips {
  margin-top: 16rpx;
  margin-bottom: 24rpx;
}
.tip-card {
  background: #fff;
  border-radius: 12rpx;
  padding: 20rpx 24rpx;
  margin-bottom: 12rpx;
}
.tip-header {
  display: flex;
  align-items: center;
  margin-bottom: 8rpx;
}
.tip-icon { font-size: 28rpx; margin-right: 10rpx; }
.tip-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #1a1a2e;
}
.tip-content {
  font-size: 26rpx;
  color: #666;
  line-height: 1.5;
  display: block;
}

.loading-mask {
  text-align: center;
  padding: 60rpx;
  color: #999;
  font-size: 28rpx;
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
.empty-desc {
  font-size: 26rpx;
  color: #999;
  line-height: 1.5;
  display: block;
  margin-bottom: 28rpx;
}
.empty-actions {
  display: flex;
  justify-content: center;
  gap: 18rpx;
}
.empty-action {
  min-width: 180rpx;
  height: 72rpx;
  padding: 0 28rpx;
  border-radius: 36rpx;
  background: #eef7ff;
  color: #2f80d1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  font-weight: 600;
}
.empty-action.primary {
  background: #4A90E2;
  color: #fff;
}
</style>
