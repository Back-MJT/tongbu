<!--
  AI训练方案页面 - 昕动智能小程序 (XIN-121 FE)
  功能: 展示AE算法生成的运动处方，含健康提示、AI教练建议、循证参考
-->
<template>
  <view class="training-plan-page">
    <!-- 加载状态 -->
    <view v-if="loading" class="loading-state">
      <view class="loading-mark">AI</view>
      <text class="loading-text">AI教练正在分析数据...</text>
    </view>

    <!-- 训练目标卡片 -->
    <view v-if="!loading && prescription" class="plan-hero">
      <view class="hero-topline">
        <text class="hero-eyebrow">TRAINING RX</text>
        <text class="algo-badge">AE v{{ prescription?.algorithmVersion?.replace('XIN-AE-', '') || '0.1' }}</text>
      </view>
      <view class="goal-header">
        <view class="goal-info">
          <text class="goal-title">{{ prescription.exerciseGoal }}</text>
          <text class="goal-subtitle">{{ prescription.exerciseGoalEn || 'Personalized training prescription' }}</text>
        </view>
        <view class="stage-tag" :style="{ backgroundColor: stageColor }">
          <text class="stage-text">{{ stageLabel }}</text>
        </view>
      </view>
      <view class="hero-progress">
        <view class="progress-copy">
          <text class="progress-label">今日完成度</text>
          <text class="progress-value">{{ completedCount }}/{{ prescription.tasks.length }}</text>
        </view>
        <view class="progress-track">
          <view class="progress-fill" :style="{ width: progressPercent + '%' }"></view>
        </view>
      </view>
    </view>

    <!-- AI教练建议 -->
    <view v-if="!loading && prescription" class="coach-card">
      <view class="coach-header">
        <text class="coach-icon">AI</text>
        <text class="coach-title">AI教练建议</text>
      </view>
      <text class="coach-content">{{ prescription.aiSuggestion }}</text>
      <view class="coach-reasoning" v-if="prescription.coachingReasoning">
        <text class="reasoning-label">决策依据</text>
        <text class="reasoning-text">{{ prescription.coachingReasoning }}</text>
      </view>
    </view>

    <!-- 训练任务列表 -->
    <view v-if="!loading && prescription" class="tasks-section">
      <view class="section-header">
        <text class="section-title">今日训练计划</text>
        <text class="task-count">{{ completedCount }}/{{ prescription.tasks.length }} 完成</text>
      </view>
      <view class="task-list">
        <view
          v-for="(task, idx) in prescription.tasks"
          :key="idx"
          class="task-item"
          :class="task.status"
          @tap="onTaskClick(task)"
        >
          <view class="task-left">
            <view class="task-status-dot">
              <text v-if="task.status === 'completed'" class="dot-check">✓</text>
              <text v-else-if="task.status === 'in_progress'" class="dot-play">▶</text>
              <text v-else class="dot-num">{{ idx + 1 }}</text>
            </view>
            <view class="task-info">
              <text class="task-name">{{ task.exerciseName }}</text>
              <text class="task-detail">
                {{ task.targetSets }}组 × {{ task.targetReps }}次 · {{ task.targetLoadKg }}kg
              </text>
              <text class="task-intensity" :class="getIntensityClass(task.intensityLabel)">
                {{ task.intensityLabel }}强度
              </text>
            </view>
          </view>
          <view class="task-right">
            <text class="task-rest">休{{ task.restSeconds }}s</text>
            <view class="task-action-btn">
              <text v-if="task.status === 'completed'" class="action-done">✓ 完成</text>
              <text v-else-if="task.status === 'in_progress'" class="action-active">进行中</text>
              <text v-else class="action-start">开始</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- AI教练提示 (展开式) -->
    <view v-if="!loading && prescription" class="tips-section">
      <text class="section-title">训练提示</text>
      <view class="tip-list">
        <view
          v-for="(tip, idx) in prescription.healthTips"
          :key="idx"
          class="tip-item"
          @tap="toggleTip(idx)"
        >
          <view class="tip-header">
            <text class="tip-category" :class="tip.category">{{ tip.category === 'nutrition' ? 'NUTR' : 'MOVE' }}</text>
            <text class="tip-title">{{ tip.title }}</text>
            <text class="tip-expand">{{ expandedTips[idx] ? '▾' : '▸' }}</text>
          </view>
          <view v-if="expandedTips[idx]" class="tip-body">
            <text class="tip-content">{{ tip.content }}</text>
            <text class="tip-evidence">参考文献: {{ tip.evidenceSource }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 心率区间 -->
    <view v-if="!loading && prescription?.targetHrZone" class="hr-zone-card">
      <view class="section-header compact">
        <text class="section-title">目标心率区间</text>
        <text class="section-tag">HR</text>
      </view>
      <view class="hr-display">
        <text class="hr-range">{{ prescription.targetHrZone.low }} - {{ prescription.targetHrZone.high }} bpm</text>
        <text class="hr-label">{{ prescription.targetHrZone.label }}</text>
      </view>
    </view>

    <!-- 循证参考 -->
    <view v-if="!loading && prescription?.evidenceRefs?.length" class="evidence-section">
      <view class="section-header compact">
        <text class="section-title">循证参考</text>
        <text class="section-tag">REF</text>
      </view>
      <view class="evidence-list">
        <text
          v-for="(ref, idx) in prescription.evidenceRefs"
          :key="idx"
          class="evidence-item"
        >
          {{ idx + 1 }}. {{ ref }}
        </text>
      </view>
    </view>

    <!-- 空状态 -->
    <view v-if="!loading && !prescription" class="empty-state">
      <text class="empty-icon">PLAN</text>
      <text class="empty-title">暂无训练方案</text>
      <text class="empty-desc">请先绑定设备并完善个人资料</text>
      <view class="empty-action" @tap="goDeviceBinding">
        <text>去绑定设备</text>
      </view>
    </view>

    <!-- 错误状态 -->
    <view v-if="error" class="error-state">
      <text class="error-icon">!</text>
      <text class="error-text">{{ error }}</text>
      <view class="retry-btn" @tap="loadPrescription">
        <text>重新加载</text>
      </view>
    </view>
  </view>
</template>

<script>
import { defineComponent, ref, computed, onMounted, reactive } from 'vue';
import { getMiniprogramPrescription } from '../../services/api';

export default defineComponent({
  setup() {
    const loading = ref(true);
    const error = ref('');
    const prescription = ref(null);
    const expandedTips = reactive({});

    const stageLabel = computed(() => {
      const stageMap = {
        beginner: '适应期',
        growth: '成长期',
        plateau: '突破期',
        advanced: '进阶期',
      };
      return stageMap[prescription.value?.userStage] || '成长期';
    });

    const stageColor = computed(() => {
      const colorMap = {
        beginner: '#4CAF50',
        growth: '#2196F3',
        plateau: '#FF9800',
        advanced: '#9C27B0',
      };
      return colorMap[prescription.value?.userStage] || '#2196F3';
    });

    const completedCount = computed(() => {
      if (!prescription.value?.tasks) return 0;
      return prescription.value.tasks.filter(t => t.status === 'completed').length;
    });

    const progressPercent = computed(() => {
      const tasks = prescription.value?.tasks || [];
      if (!tasks.length) return 0;
      return Math.round((completedCount.value / tasks.length) * 100);
    });

    onMounted(async () => {
      await loadPrescription();
    });

    async function loadPrescription() {
      loading.value = true;
      error.value = '';
      try {
        // 尝试从storage获取用户信息
        const userId = wx.getStorageSync('user_id');
        const age = wx.getStorageSync('user_age') || 30;

        const res = await getMiniprogramPrescription({
          user_id: userId,
          age: age,
          gender: wx.getStorageSync('user_gender') || undefined,
          device_type: wx.getStorageSync('device_type') || undefined,
          sessions_last_30_days: wx.getStorageSync('sessions_30d') || undefined,
        });

        if (res.code === 200 && res.data) {
          prescription.value = res.data;
        } else {
          prescription.value = null;
          error.value = '无法获取训练方案';
        }
      } catch (e) {
        console.error('[TrainingPlan] Load failed', e);
        prescription.value = null;
        error.value = '加载失败，请检查网络连接';
      } finally {
        loading.value = false;
      }
    }

    function toggleTip(idx) {
      expandedTips[idx] = !expandedTips[idx];
    }

    function onTaskClick(task) {
      if (task.status === 'completed') return;
      const params = [
        `taskId=${encodeURIComponent(task.taskId || '')}`,
        `prescriptionId=${encodeURIComponent(task.prescriptionId || '')}`,
        `exerciseName=${encodeURIComponent(task.exerciseName || '')}`,
        `exerciseType=${encodeURIComponent(task.exerciseType || 'strength')}`,
        `targetSets=${encodeURIComponent(task.targetSets || '')}`,
        `targetReps=${encodeURIComponent(task.targetReps || '')}`,
        `targetLoadKg=${encodeURIComponent(task.targetLoadKg || '')}`,
      ].join('&');
      wx.navigateTo({ url: `/pages/device-binding/index?${params}` });
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

    function goDeviceBinding() {
      wx.navigateTo({ url: '/pages/device-binding/index' });
    }

    return {
      loading,
      error,
      prescription,
      expandedTips,
      stageLabel,
      stageColor,
      completedCount,
      progressPercent,
      toggleTip,
      onTaskClick,
      getIntensityClass,
      goDeviceBinding,
      loadPrescription,
    };
  },
});
</script>

<style>
.training-plan-page {
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
.algo-badge {
  font-size: 22rpx;
  color: #4A90E2;
  background: #e3f2fd;
  padding: 4rpx 14rpx;
  border-radius: 16rpx;
}

/* 目标卡片 */
.goal-card {
  background: linear-gradient(135deg, #4A90E2, #357ABD);
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
}
.goal-header {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
}
.goal-icon {
  font-size: 48rpx;
  margin-right: 16rpx;
}
.goal-info {
  flex: 1;
}
.goal-title {
  font-size: 36rpx;
  font-weight: bold;
  color: #fff;
  display: block;
  margin-bottom: 4rpx;
}
.goal-subtitle {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.7);
}
.stage-tag {
  display: inline-block;
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
}
.stage-text {
  font-size: 24rpx;
  color: #fff;
}

/* AI教练卡片 */
.coach-card {
  background: linear-gradient(135deg, #f0f7ff, #e8f4ff);
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
  border-left: 6rpx solid #4A90E2;
}
.coach-header {
  display: flex;
  align-items: center;
  margin-bottom: 12rpx;
}
.coach-icon {
  font-size: 32rpx;
  margin-right: 10rpx;
}
.coach-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #4A90E2;
}
.coach-content {
  font-size: 28rpx;
  color: #333;
  line-height: 1.6;
  display: block;
  margin-bottom: 16rpx;
}
.coach-reasoning {
  background: rgba(74, 144, 226, 0.08);
  border-radius: 12rpx;
  padding: 16rpx;
}
.reasoning-label {
  font-size: 24rpx;
  color: #4A90E2;
  font-weight: 600;
  display: block;
  margin-bottom: 6rpx;
}
.reasoning-text {
  font-size: 24rpx;
  color: #666;
  line-height: 1.5;
}

/* 任务列表 */
.tasks-section {
  margin-bottom: 24rpx;
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}
.section-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1a1a2e;
  display: block;
  margin-bottom: 16rpx;
}
.section-header .section-title {
  margin-bottom: 0;
}
.task-count {
  font-size: 26rpx;
  color: #4A90E2;
}
.task-list {}
.task-item {
  background: #fff;
  border-radius: 12rpx;
  padding: 24rpx;
  margin-bottom: 12rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.task-item.completed {
  opacity: 0.7;
}
.task-item.in_progress {
  border-left: 4rpx solid #4A90E2;
}
.task-left {
  display: flex;
  align-items: center;
  flex: 1;
}
.task-status-dot {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16rpx;
  flex-shrink: 0;
  font-size: 24rpx;
}
.task-item.completed .task-status-dot {
  background: #e8f5e9;
  color: #52c41a;
}
.task-item.in_progress .task-status-dot {
  background: #e3f2fd;
  color: #4A90E2;
}
.dot-check { color: #52c41a; font-weight: bold; }
.dot-play { color: #4A90E2; }
.dot-num { color: #999; }
.task-info {
  flex: 1;
}
.task-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #1a1a2e;
  display: block;
  margin-bottom: 4rpx;
}
.task-detail {
  font-size: 24rpx;
  color: #666;
  display: block;
  margin-bottom: 4rpx;
}
.task-intensity {
  font-size: 22rpx;
  padding: 2rpx 10rpx;
  border-radius: 8rpx;
  display: inline-block;
}
.task-intensity.intensity-low { background: #e8f5e9; color: #4caf50; }
.task-intensity.intensity-medium { background: #fff3e0; color: #ff9800; }
.task-intensity.intensity-medium-high { background: #fce4ec; color: #e91e63; }
.task-intensity.intensity-high { background: #ffebee; color: #f44336; }
.task-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  margin-left: 12rpx;
}
.task-rest {
  font-size: 22rpx;
  color: #999;
  margin-bottom: 8rpx;
}
.task-action-btn {}
.action-done { font-size: 24rpx; color: #52c41a; }
.action-active { font-size: 24rpx; color: #4A90E2; font-weight: 600; }
.action-start {
  font-size: 24rpx;
  color: #fff;
  background: #4A90E2;
  padding: 6rpx 20rpx;
  border-radius: 20rpx;
}

/* 健康提示 */
.tips-section {
  margin-bottom: 24rpx;
}
.tip-list {}
.tip-item {
  background: #fff;
  border-radius: 12rpx;
  padding: 20rpx 24rpx;
  margin-bottom: 10rpx;
}
.tip-header {
  display: flex;
  align-items: center;
}
.tip-category {
  font-size: 28rpx;
  margin-right: 10rpx;
}
.tip-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #1a1a2e;
  flex: 1;
}
.tip-expand {
  font-size: 24rpx;
  color: #999;
}
.tip-body {
  padding-top: 12rpx;
  border-top: 1rpx solid #f0f0f0;
  margin-top: 12rpx;
}
.tip-content {
  font-size: 26rpx;
  color: #333;
  line-height: 1.6;
  display: block;
  margin-bottom: 8rpx;
}
.tip-evidence {
  font-size: 22rpx;
  color: #999;
}

/* 心率区间 */
.hr-zone-card {
  background: linear-gradient(135deg, #fce4ec, #f8bbd0);
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
}
.hr-display {
  display: flex;
  align-items: center;
  margin-top: 12rpx;
}
.hr-range {
  font-size: 40rpx;
  font-weight: bold;
  color: #e91e63;
  margin-right: 16rpx;
}
.hr-label {
  font-size: 26rpx;
  color: #c2185b;
}

/* 循证参考 */
.evidence-section {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 40rpx;
}
.evidence-list {}
.evidence-item {
  font-size: 24rpx;
  color: #666;
  line-height: 1.8;
  display: block;
}

/* 加载状态 */
.loading-state {
  text-align: center;
  padding: 120rpx 40rpx;
}
.loading-text {
  font-size: 28rpx;
  color: #999;
}

/* 空状态 */
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
  display: block;
  margin-bottom: 32rpx;
}
.empty-action {
  background: #4A90E2;
  color: #fff;
  font-size: 28rpx;
  padding: 16rpx 40rpx;
  border-radius: 24rpx;
  display: inline-block;
}

/* 错误状态 */
.error-state {
  text-align: center;
  padding: 60rpx 40rpx;
}
.error-icon { font-size: 60rpx; display: block; margin-bottom: 16rpx; }
.error-text {
  font-size: 28rpx;
  color: #ff4d4f;
  display: block;
  margin-bottom: 24rpx;
}
.retry-btn {
  background: #fff;
  border: 1rpx solid #4A90E2;
  color: #4A90E2;
  font-size: 28rpx;
  padding: 12rpx 36rpx;
  border-radius: 24rpx;
  display: inline-block;
}

.training-plan-page {
  padding: 28rpx;
  background: #f3f6f8;
  min-height: 100vh;
  box-sizing: border-box;
}

.plan-hero {
  background: linear-gradient(145deg, #101827 0%, #1d2b3f 52%, #244d4a 100%);
  border-radius: 28rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  color: #fff;
  box-shadow: 0 18rpx 42rpx rgba(17, 24, 39, 0.16);
}

.hero-topline,
.goal-header,
.progress-copy,
.section-header.compact {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.hero-eyebrow {
  font-size: 22rpx;
  letter-spacing: 0;
  color: rgba(255, 255, 255, 0.58);
  font-weight: 700;
}

.algo-badge {
  font-size: 22rpx;
  color: #b9f4df;
  background: rgba(255, 255, 255, 0.1);
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
}

.goal-header {
  margin: 36rpx 0 28rpx;
}

.goal-title {
  font-size: 44rpx;
  line-height: 1.16;
  font-weight: 800;
  color: #fff;
  display: block;
  margin-bottom: 10rpx;
}

.goal-subtitle {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.66);
}

.stage-tag {
  min-width: 112rpx;
  height: 52rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 18rpx;
}

.stage-text {
  font-size: 24rpx;
  color: #fff;
  font-weight: 700;
}

.hero-progress {
  background: rgba(255, 255, 255, 0.1);
  border: 1rpx solid rgba(255, 255, 255, 0.1);
  border-radius: 18rpx;
  padding: 18rpx;
}

.progress-label {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.68);
}

.progress-value {
  font-size: 28rpx;
  color: #fff;
  font-weight: 800;
}

.progress-track {
  height: 10rpx;
  background: rgba(255, 255, 255, 0.16);
  border-radius: 999rpx;
  overflow: hidden;
  margin-top: 14rpx;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #4ee0a6, #62b5ff);
  border-radius: 999rpx;
}

.coach-card,
.evidence-section,
.hr-zone-card,
.tip-item,
.task-item {
  border-radius: 22rpx;
  box-shadow: 0 10rpx 28rpx rgba(15, 23, 42, 0.06);
}

.coach-card {
  background: #fff;
  padding: 28rpx;
  margin-bottom: 26rpx;
  border-left: 0;
}

.coach-icon {
  width: 56rpx;
  height: 56rpx;
  border-radius: 16rpx;
  background: #111827;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  font-weight: 800;
  margin-right: 14rpx;
}

.coach-title {
  color: #111827;
  font-size: 30rpx;
}

.coach-content,
.tip-content {
  color: #334155;
}

.coach-reasoning {
  background: #f6f8fb;
  border-radius: 18rpx;
  padding: 18rpx;
}

.reasoning-label,
.task-count {
  color: #0f766e;
}

.section-title {
  color: #111827;
  font-size: 32rpx;
  font-weight: 800;
}

.task-item {
  background: #fff;
  padding: 26rpx;
  margin-bottom: 14rpx;
}

.task-item.in_progress {
  border-left: 0;
  box-shadow: 0 12rpx 30rpx rgba(31, 111, 235, 0.12);
}

.task-status-dot {
  width: 54rpx;
  height: 54rpx;
  background: #eef2f7;
  color: #64748b;
  font-weight: 800;
  margin-right: 18rpx;
}

.task-name {
  color: #101827;
  font-size: 30rpx;
}

.task-detail {
  color: #64748b;
}

.task-intensity {
  font-weight: 700;
  padding: 5rpx 12rpx;
  border-radius: 999rpx;
}

.task-rest {
  color: #94a3b8;
}

.action-start {
  background: #101827;
  border-radius: 999rpx;
}

.tips-section {
  margin-bottom: 26rpx;
}

.tip-item {
  background: #fff;
  padding: 24rpx;
  margin-bottom: 14rpx;
}

.tip-category {
  width: 76rpx;
  height: 42rpx;
  border-radius: 999rpx;
  background: #eef8f3;
  color: #0f766e;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20rpx;
  font-weight: 800;
  margin-right: 14rpx;
}

.tip-category.nutrition {
  background: #fff4e6;
  color: #b45309;
}

.tip-title {
  color: #111827;
}

.section-tag {
  font-size: 20rpx;
  font-weight: 800;
  color: #64748b;
  background: #eef2f7;
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
}

.hr-zone-card {
  background: #fff;
  padding: 28rpx;
}

.hr-range {
  color: #be123c;
}

.hr-label {
  color: #64748b;
}

.evidence-section {
  background: #fff;
  padding: 28rpx;
}

.loading-state,
.empty-state,
.error-state {
  background: #fff;
  border-radius: 28rpx;
  margin-top: 48rpx;
  box-shadow: 0 10rpx 28rpx rgba(15, 23, 42, 0.06);
}

.loading-mark,
.empty-icon,
.error-icon {
  width: 92rpx;
  height: 92rpx;
  border-radius: 28rpx;
  background: #101827;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  font-weight: 900;
  margin: 0 auto 22rpx;
}

.empty-action {
  background: #101827;
  border-radius: 999rpx;
}

.retry-btn {
  border-color: #101827;
  color: #101827;
  border-radius: 999rpx;
}
</style>
