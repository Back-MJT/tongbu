<!--
  每日任务页面 - 昕动智能小程序 (XIN-121 FE)
  功能: 今日训练计划列表、AE算法训练建议、实时组数进度、教练提示
-->
<template>
  <view class="daily-task-page">
    <view class="plan-hero">
      <view class="page-header">
        <view>
          <text class="eyebrow">TODAY PLAN</text>
          <text class="page-title">今日训练</text>
        </view>
        <view class="refresh-pill" @tap="loadDailyTask">{{ loading ? '同步中' : '刷新' }}</view>
      </view>

      <view class="hero-meta">
        <text>{{ today }}</text>
        <text>{{ venueName || '未选择场馆' }}</text>
      </view>

      <view class="session-card">
        <view class="session-info">
          <text class="session-status" :class="sessionStatusClass">{{ sessionStatusText }}</text>
          <text class="goal-label" v-if="exerciseGoal">{{ exerciseGoal }}</text>
        </view>
        <view class="session-progress">
          <view>
            <text class="progress-current">{{ completedSets }}</text>
            <text class="progress-total"> / {{ totalSets }} 组</text>
          </view>
          <text class="reps-text">{{ completedReps }} / {{ totalReps }} 次</text>
        </view>
        <view class="progress-bar-wrap">
          <view class="progress-bar" :style="{ width: progressPercent + '%' }"></view>
        </view>
      </view>

      <view class="venue-action" v-if="!hasVenue" @tap="goHome">
        <text>去首页选择场馆</text>
      </view>
    </view>

    <view class="plan-summary">
      <view class="summary-item">
        <text class="summary-value">{{ tasks.length }}</text>
        <text class="summary-label">动作</text>
      </view>
      <view class="summary-item">
        <text class="summary-value">{{ totalSets }}</text>
        <text class="summary-label">目标组</text>
      </view>
      <view class="summary-item">
        <text class="summary-value">{{ progressPercent }}%</text>
        <text class="summary-label">进度</text>
      </view>
    </view>

    <view class="ai-suggestion-card" v-if="aiSuggestion">
      <view class="ai-header">
        <text class="ai-icon">AE</text>
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
      <view class="section-head">
        <text class="section-title">训练任务</text>
        <text class="section-subtitle">按顺序完成，系统会记录真实组数</text>
      </view>
      <view class="task-list">
        <view
          v-for="(task, index) in tasks"
          :key="index"
          class="task-item"
          :class="task.status"
          @tap="onTaskClick(task)"
        >
          <view class="task-status-icon">
            <text v-if="task.status === 'completed'">OK</text>
            <text v-else>{{ index + 1 }}</text>
          </view>
          <view class="task-content">
            <view class="task-head">
              <text class="intensity-tag" v-if="task.intensityLabel" :class="getIntensityClass(task.intensityLabel)">
                {{ task.intensityLabel }}
              </text>
              <text class="task-action">
                <text v-if="task.status === 'completed'" class="tag-done">已完成</text>
                <text v-else-if="task.status === 'in_progress'" class="tag-active">进行中</text>
                <text v-else class="tag-pending">开始</text>
              </text>
            </view>
            <text class="task-name">{{ task.exerciseName }}</text>
            <view class="task-metrics">
              <text>{{ task.targetSets }}组</text>
              <text>{{ task.targetReps }}次</text>
              <text v-if="task.targetLoadKg">{{ task.targetLoadKg }}kg</text>
              <text v-if="task.restSeconds">休{{ task.restSeconds }}s</text>
            </view>
            <text class="task-tip" v-if="task.coachingTip">{{ task.coachingTip }}</text>
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
          <text class="tip-icon">{{ tip.category === 'nutrition' ? 'NUTR' : 'MOVE' }}</text>
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
      <view class="empty-icon">PLAN</view>
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
        `prescriptionId=${encodeURIComponent(task.prescriptionId || '')}`,
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
  padding: 28rpx 24rpx 56rpx;
  background: #edf2f7;
  min-height: 100vh;
  box-sizing: border-box;
}
.plan-hero {
  background: linear-gradient(145deg, #101828, #1f2a44 58%, #173d42);
  border-radius: 28rpx;
  padding: 32rpx 30rpx 30rpx;
  color: #fff;
  margin-bottom: 18rpx;
  box-shadow: 0 24rpx 48rpx rgba(16, 24, 40, 0.2);
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 22rpx;
}
.eyebrow {
  display: block;
  color: rgba(255,255,255,0.58);
  font-size: 20rpx;
  font-weight: 900;
  margin-bottom: 8rpx;
}
.page-title {
  font-size: 44rpx;
  font-weight: 900;
  color: #fff;
  display: block;
}
.refresh-pill {
  min-width: 104rpx;
  height: 56rpx;
  border-radius: 999rpx;
  background: rgba(255,255,255,0.12);
  border: 1rpx solid rgba(255,255,255,0.16);
  color: rgba(255,255,255,0.88);
  font-size: 24rpx;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}
.hero-meta {
  display: flex;
  justify-content: space-between;
  color: rgba(255,255,255,0.66);
  font-size: 24rpx;
  margin-bottom: 24rpx;
}
.session-card {
  background: rgba(255,255,255,0.1);
  border: 1rpx solid rgba(255,255,255,0.14);
  border-radius: 24rpx;
  padding: 26rpx;
}
.session-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 18rpx;
}
.session-status {
  height: 48rpx;
  padding: 0 20rpx;
  border-radius: 999rpx;
  background: rgba(255,255,255,0.14);
  color: rgba(255,255,255,0.86);
  font-size: 24rpx;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.session-status.active { background: rgba(49,212,160,0.18); color: #31d4a0; }
.session-status.done { background: rgba(49,212,160,0.24); color: #31d4a0; }
.session-progress {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 16rpx;
}
.progress-current {
  font-size: 58rpx;
  font-weight: 900;
  color: #fff;
}
.progress-total {
  font-size: 28rpx;
  color: rgba(255,255,255,0.62);
  font-weight: 700;
}
.reps-text {
  font-size: 24rpx;
  color: rgba(255,255,255,0.72);
  padding-bottom: 10rpx;
}
.progress-bar-wrap {
  height: 14rpx;
  background: rgba(255,255,255,0.14);
  border-radius: 999rpx;
  overflow: hidden;
}
.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #31d4a0, #63e6be);
  border-radius: 999rpx;
  transition: width 0.3s ease;
}
.goal-label {
  flex: 1;
  min-width: 0;
  font-size: 24rpx;
  color: rgba(255,255,255,0.72);
  line-height: 1.35;
  text-align: right;
}
.venue-action {
  margin-top: 20rpx;
  height: 72rpx;
  border-radius: 999rpx;
  background: rgba(255,255,255,0.12);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  font-weight: 800;
}
.plan-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14rpx;
  margin-bottom: 18rpx;
}
.summary-item {
  background: #fff;
  border-radius: 22rpx;
  padding: 22rpx 12rpx;
  text-align: center;
  border: 1rpx solid rgba(222, 228, 236, 0.9);
  box-shadow: 0 12rpx 28rpx rgba(20, 38, 70, 0.05);
}
.summary-value {
  display: block;
  color: #101828;
  font-size: 36rpx;
  font-weight: 900;
  line-height: 1.1;
}
.summary-label {
  display: block;
  margin-top: 8rpx;
  color: #7b8794;
  font-size: 22rpx;
}
.ai-suggestion-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
  border: 1rpx solid rgba(222, 228, 236, 0.9);
  box-shadow: 0 14rpx 34rpx rgba(20, 38, 70, 0.06);
}
.ai-header {
  display: flex;
  align-items: center;
  margin-bottom: 12rpx;
}
.ai-icon {
  width: 58rpx;
  height: 40rpx;
  border-radius: 999rpx;
  background: #e9fbf6;
  color: #0f9f7a;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20rpx;
  font-weight: 900;
  margin-right: 12rpx;
}
.ai-title {
  font-size: 28rpx;
  font-weight: 800;
  color: #101828;
  flex: 1;
}
.ai-algo {
  font-size: 20rpx;
  color: #fff;
  background: #101828;
  padding: 4rpx 12rpx;
  border-radius: 999rpx;
}
.ai-content {
  font-size: 28rpx;
  color: #344054;
  line-height: 1.6;
  display: block;
  margin-bottom: 12rpx;
}
.ai-reasoning {
  background: #f5f8fb;
  border-radius: 18rpx;
  padding: 18rpx;
}
.reasoning-label {
  font-size: 22rpx;
  color: #0f9f7a;
  font-weight: 800;
  display: block;
  margin-bottom: 4rpx;
}
.reasoning-text {
  font-size: 24rpx;
  color: #667085;
  line-height: 1.5;
}
.tasks-section { margin-top: 8rpx; }
.section-head {
  margin-bottom: 18rpx;
}
.section-title {
  font-size: 32rpx;
  font-weight: 900;
  color: #101828;
  display: block;
}
.section-subtitle {
  display: block;
  margin-top: 6rpx;
  color: #7b8794;
  font-size: 24rpx;
}
.task-item {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 16rpx;
  display: flex;
  align-items: flex-start;
  gap: 20rpx;
  border: 1rpx solid rgba(222, 228, 236, 0.9);
  box-shadow: 0 12rpx 28rpx rgba(20, 38, 70, 0.05);
}
.task-item.in_progress {
  border-color: rgba(49, 212, 160, 0.55);
}
.task-item.completed {
  opacity: 0.75;
}
.task-status-icon {
  width: 64rpx;
  height: 64rpx;
  border-radius: 20rpx;
  background: #101828;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  font-weight: 900;
  flex-shrink: 0;
}
.task-item.completed .task-status-icon { background: #e9fbf6; color: #0f9f7a; }
.task-item.in_progress .task-status-icon { background: #13b5a5; color: #fff; }
.task-content {
  flex: 1;
  min-width: 0;
}
.task-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10rpx;
  gap: 12rpx;
}
.task-name {
  font-size: 32rpx;
  font-weight: 900;
  color: #101828;
  display: block;
  margin-bottom: 14rpx;
}
.task-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  margin-bottom: 12rpx;
}
.task-metrics text {
  height: 42rpx;
  padding: 0 16rpx;
  border-radius: 999rpx;
  background: #f2f5f8;
  color: #475467;
  font-size: 22rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
}
.task-tip {
  font-size: 24rpx;
  color: #667085;
  display: block;
  line-height: 1.5;
}
.intensity-tag {
  height: 38rpx;
  padding: 0 14rpx;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  flex-shrink: 0;
  font-size: 20rpx;
  font-weight: 800;
}
.intensity-tag.intensity-low { background: #e9fbf6; color: #0f9f7a; }
.intensity-tag.intensity-medium { background: #fff7e6; color: #b56a00; }
.intensity-tag.intensity-medium-high { background: #fff1f3; color: #c01048; }
.intensity-tag.intensity-high { background: #fee4e2; color: #b42318; }
.task-action { flex-shrink: 0; font-size: 22rpx; font-weight: 800; }
.tag-done { color: #0f9f7a; }
.tag-active { color: #13b5a5; }
.tag-pending { color: #2563eb; }

/* 健康提示 */
.health-tips {
  margin-top: 16rpx;
  margin-bottom: 24rpx;
}
.tip-card {
  background: #fff;
  border-radius: 22rpx;
  padding: 24rpx;
  margin-bottom: 14rpx;
  border: 1rpx solid rgba(222, 228, 236, 0.9);
}
.tip-header {
  display: flex;
  align-items: center;
  margin-bottom: 8rpx;
}
.tip-icon {
  min-width: 72rpx;
  height: 36rpx;
  border-radius: 999rpx;
  background: #f2f5f8;
  color: #475467;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18rpx;
  font-weight: 900;
  margin-right: 12rpx;
}
.tip-title {
  font-size: 28rpx;
  font-weight: 800;
  color: #101828;
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
  color: #7b8794;
  font-size: 28rpx;
}
.empty-state {
  text-align: center;
  padding: 100rpx 40rpx;
  background: #fff;
  border-radius: 26rpx;
  border: 1rpx solid rgba(222, 228, 236, 0.9);
}
.empty-icon {
  width: 104rpx;
  height: 104rpx;
  border-radius: 28rpx;
  background: #101828;
  color: #31d4a0;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 22rpx;
  font-size: 22rpx;
  font-weight: 900;
}
.empty-title {
  font-size: 32rpx;
  font-weight: 900;
  color: #101828;
  display: block;
  margin-bottom: 12rpx;
}
.empty-desc {
  font-size: 26rpx;
  color: #7b8794;
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
  background: #101828;
  color: #fff;
}
</style>
