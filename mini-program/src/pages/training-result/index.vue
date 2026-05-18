<!--
  训练结果页面 - 昕动智能小程序
  功能: 展示训练完成数据、组详情、AI反馈、分享/保存
-->
<template>
  <view class="result-page">
    <!-- 页面头部 -->
    <view class="page-header">
      <text class="page-title">训练报告</text>
      <text class="page-subtitle">{{ sessionDate }}<text v-if="sessionId"> · #{{ sessionId }}</text></text>
    </view>

    <!-- 总览卡片 -->
    <view class="overview-card">
      <view class="overview-main">
        <view class="main-stat">
          <text class="main-value">{{ totalReps || 0 }}</text>
          <text class="main-label">总次数</text>
        </view>
        <view class="main-divider"></view>
        <view class="main-stat">
          <text class="main-value">{{ completedSets || 0 }}</text>
          <text class="main-label">完成组数</text>
        </view>
        <view class="main-divider"></view>
        <view class="main-stat">
          <text class="main-value">{{ durationMin || 0 }}</text>
          <text class="main-label">分钟</text>
        </view>
      </view>
      <view class="exercise-name-row" v-if="exerciseName">
        <text class="exercise-icon">MOVE</text>
        <text class="exercise-name">{{ exerciseName }}</text>
      </view>
      <view class="exercise-name-row muted" v-if="equipmentCode || deviceCode">
        <text class="exercise-name">{{ equipmentCode || '未关联器械' }}<text v-if="deviceCode"> · {{ deviceCode }}</text></text>
      </view>
    </view>

    <view class="target-card" v-if="hasTarget">
      <view class="target-header">
        <text class="card-title">目标完成</text>
        <text class="target-rate">{{ completionRate }}%</text>
      </view>
      <view class="target-bar-wrap">
        <view class="target-bar" :style="{ width: completionRate + '%' }"></view>
      </view>
      <view class="target-meta">
        <text v-if="targetSets">目标 {{ targetSets }} 组</text>
        <text v-if="targetReps">目标 {{ targetReps }} 次/组</text>
        <text v-if="targetLoadKg">{{ targetLoadKg }}kg</text>
      </view>
    </view>

    <!-- 组详情 -->
    <view class="sets-card" v-if="sets && sets.length > 0">
      <text class="card-title">组详情</text>
      <view class="set-list">
        <view
          v-for="(set, idx) in sets"
          :key="idx"
          class="set-item"
        >
          <view class="set-no">{{ set.setNo }}组</view>
          <view class="set-details">
            <view class="set-detail-item">
              <text class="set-detail-val">{{ set.reps || 0 }}</text>
              <text class="set-detail-lbl">次</text>
            </view>
            <view class="set-detail-item">
              <text class="set-detail-val">{{ set.durationSec || 0 }}</text>
              <text class="set-detail-lbl">秒</text>
            </view>
            <view class="set-detail-item" v-if="set.peakLoadKg">
              <text class="set-detail-val">{{ set.peakLoadKg }}</text>
              <text class="set-detail-lbl">kg</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 峰值数据 -->
    <view class="peak-card" v-if="hasPeakData">
      <text class="card-title">峰值表现</text>
      <view class="peak-grid">
        <view class="peak-item" v-if="peakReps">
          <text class="peak-val">{{ peakReps }}</text>
          <text class="peak-lbl">单组最高次数</text>
        </view>
        <view class="peak-item" v-if="peakDuration">
          <text class="peak-val">{{ peakDuration }}秒</text>
          <text class="peak-lbl">单组最长时长</text>
        </view>
        <view class="peak-item" v-if="peakLoadKg">
          <text class="peak-val">{{ peakLoadKg }}kg</text>
          <text class="peak-lbl">峰值重量</text>
        </view>
      </view>
    </view>

    <!-- 身体引擎反馈区域 (A Line 预留) -->
    <view class="ai-card" :class="analysisStatusClass">
      <view class="ai-header">
        <text class="ai-icon">BODY</text>
        <view class="ai-heading">
          <text class="ai-title">身体引擎反馈</text>
          <text class="ai-status">{{ analysisStatusText }}</text>
        </view>
      </view>
      <text class="ai-content" v-if="aiFeedback">{{ aiFeedback }}</text>
      <text class="ai-placeholder" v-else>{{ analysisMessage || 'A Line API 尚未接入，训练记录已保存。' }}</text>
      <view class="ai-suggestions" v-if="aiSuggestions.length">
        <text
          v-for="(item, idx) in aiSuggestions"
          :key="idx"
          class="ai-suggestion"
        >
          {{ item }}
        </text>
      </view>
      <text class="ai-next" v-if="nextRecommendation">{{ nextRecommendation }}</text>
    </view>

    <!-- 操作按钮 -->
    <view class="actions-card">
      <view class="action-btn primary" @tap="onShare">
        <text class="action-icon">SHARE</text>
        <text>分享到微信</text>
      </view>
      <view class="action-btn secondary" @tap="onSaveToAlbum">
        <text class="action-icon">SAVE</text>
        <text>保存到相册</text>
      </view>
    </view>

    <!-- 返回入口 -->
    <view class="nav-card">
      <view class="nav-item" @tap="goProgress">
        <text class="nav-icon">DATA</text>
        <text>查看训练进度</text>
        <text class="nav-arrow">›</text>
      </view>
      <view class="nav-item" @tap="goHome">
        <text class="nav-icon">HOME</text>
        <text>返回首页</text>
        <text class="nav-arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script lang="ts">
import { defineComponent, ref, computed, onMounted } from 'vue';
import Taro from '@tarojs/taro';
import { getAnalysisResult } from '../../services/api';

export default defineComponent({
  setup() {
    const exerciseName = ref('');
    const sessionId = ref('');
    const equipmentCode = ref('');
    const deviceCode = ref('');
    const completedSets = ref(0);
    const totalReps = ref(0);
    const durationMin = ref(0);
    const targetSets = ref(0);
    const targetReps = ref(0);
    const targetLoadKg = ref(0);
    const sets = ref<any[]>([]);
    const aiFeedback = ref('');
    const aiSuggestions = ref<string[]>([]);
    const nextRecommendation = ref('');
    const analysisTaskId = ref('');
    const analysisStatus = ref('unavailable');
    const analysisMessage = ref('');
    const analysisProgress = ref(0);
    const sessionDate = ref('');

    onMounted(async () => {
      loadResultData();
      await refreshAnalysisResult();
    });

    function loadResultData() {
      try {
        const params = Taro.getCurrentInstance()?.router?.params || {};
        if (params.data) {
          const decoded = JSON.parse(decodeURIComponent(params.data));
          sessionId.value = decoded.sessionId ? String(decoded.sessionId) : '';
          exerciseName.value = decoded.exerciseName || '';
          equipmentCode.value = decoded.equipmentCode || '';
          deviceCode.value = decoded.deviceCode || '';
          completedSets.value = decoded.completedSets || 0;
          totalReps.value = decoded.totalReps || 0;
          durationMin.value = decoded.durationMin || 0;
          targetSets.value = Number(decoded.targetSets || 0);
          targetReps.value = Number(decoded.targetReps || 0);
          targetLoadKg.value = Number(decoded.targetLoadKg || 0);
          sets.value = decoded.sets || [];
          applyAnalysisData(decoded);
        }
        if (!sessionDate.value) {
          sessionDate.value = formatDate(new Date());
        }
      } catch (e) {
        console.warn('[TrainingResult] load data failed', e);
      }
    }

    function formatDate(date: Date) {
      return `${date.getMonth() + 1}月${date.getDate()}日 ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`;
    }

    function applyAnalysisData(value: any) {
      if (!value) return;
      const data = value.data && typeof value.data === 'object' ? value.data : value;
      const feedback = data.aiFeedback || data.feedback;
      analysisTaskId.value = data.analysisTaskId || data.taskId || analysisTaskId.value || '';
      analysisStatus.value = data.analysisStatus || data.status || analysisStatus.value || 'unavailable';
      analysisMessage.value = data.analysisMessage || data.message || analysisMessage.value || '';
      analysisProgress.value = Number(data.progress || analysisProgress.value || 0);
      if (feedback) {
        aiFeedback.value = formatAiFeedback(feedback);
        aiSuggestions.value = Array.isArray(feedback.suggestions) ? feedback.suggestions : [];
        nextRecommendation.value = feedback.nextRecommendation || '';
      }
    }

    function formatAiFeedback(value: any) {
      if (!value) return '';
      if (typeof value === 'string') return value;
      if (value.summary) return value.summary;
      const suggestions = Array.isArray(value.suggestions) ? value.suggestions.join('；') : '';
      return [value.strengthLevel, suggestions].filter(Boolean).join('：');
    }

    async function refreshAnalysisResult() {
      if (!sessionId.value) return;
      if (analysisStatus.value === 'completed' && aiFeedback.value) return;
      try {
        const res = await getAnalysisResult(sessionId.value);
        applyAnalysisData(res.data);
      } catch (e) {
        console.warn('[TrainingResult] analysis result unavailable', e);
        analysisStatus.value = 'unavailable';
        analysisMessage.value = analysisMessage.value || '身体引擎接口尚未接入，训练记录已正常保存。';
      }
    }

    const hasTarget = computed(() => {
      return targetSets.value > 0 || targetReps.value > 0 || targetLoadKg.value > 0;
    });

    const completionRate = computed(() => {
      const plannedReps = targetSets.value * targetReps.value;
      if (plannedReps > 0) {
        return Math.min(100, Math.round((totalReps.value / plannedReps) * 100));
      }
      if (targetSets.value > 0) {
        return Math.min(100, Math.round((completedSets.value / targetSets.value) * 100));
      }
      return 0;
    });

    const hasPeakData = computed(() => {
      return peakReps.value > 0 || peakDuration.value > 0 || peakLoadKg.value > 0;
    });

    const analysisStatusText = computed(() => {
      if (analysisStatus.value === 'completed') return '分析完成';
      if (analysisStatus.value === 'queued') return '等待分析';
      if (analysisStatus.value === 'processing') return analysisProgress.value ? `分析中 ${analysisProgress.value}%` : '分析中';
      if (analysisStatus.value === 'failed') return '分析失败';
      return 'A Line 待接入';
    });

    const analysisStatusClass = computed(() => {
      return `status-${analysisStatus.value || 'unavailable'}`;
    });

    const peakReps = computed(() => {
      if (!sets.value.length) return 0;
      return Math.max(...sets.value.map((s: any) => Number(s.reps || 0)));
    });

    const peakDuration = computed(() => {
      if (!sets.value.length) return 0;
      return Math.max(...sets.value.map((s: any) => Number(s.durationSec || 0)));
    });

    const peakLoadKg = computed(() => {
      if (!sets.value.length) return 0;
      return Math.max(...sets.value.map((s: any) => Number(s.peakLoadKg || 0)));
    });

    function onShare() {
      wx.showShareMenu({
        withShareTicket: true,
        menus: ['shareAppMessage', 'shareTimeline'],
      });
      wx.showToast({ title: '已准备分享', icon: 'success' });
    }

    function onSaveToAlbum() {
      wx.showToast({ title: '截图后长按保存', icon: 'none' });
    }

    function goProgress() {
      wx.switchTab({ url: '/pages/progress/index' });
    }

    function goHome() {
      wx.switchTab({ url: '/pages/home/index' });
    }

    return {
      exerciseName,
      sessionId,
      equipmentCode,
      deviceCode,
      completedSets,
      totalReps,
      durationMin,
      targetSets,
      targetReps,
      targetLoadKg,
      sets,
      aiFeedback,
      aiSuggestions,
      nextRecommendation,
      analysisTaskId,
      analysisStatus,
      analysisMessage,
      analysisStatusText,
      analysisStatusClass,
      sessionDate,
      hasTarget,
      completionRate,
      hasPeakData,
      peakReps,
      peakDuration,
      peakLoadKg,
      onShare,
      onSaveToAlbum,
      goProgress,
      goHome,
    };
  },
});
</script>

<style>
.result-page {
  padding: 28rpx 24rpx 60rpx;
  background: #edf2f7;
  min-height: 100vh;
  box-sizing: border-box;
}
.page-header {
  margin-bottom: 24rpx;
}
.page-title {
  display: block;
  font-size: 44rpx;
  font-weight: 900;
  color: #101828;
  margin-bottom: 8rpx;
}
.page-subtitle {
  font-size: 26rpx;
  color: #7b8794;
}
.overview-card {
  background: linear-gradient(145deg, #101828 0%, #1f2a44 58%, #123f3a 100%);
  border-radius: 28rpx;
  padding: 36rpx 28rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 24rpx 48rpx rgba(16, 24, 40, 0.22);
}
.overview-main {
  display: flex;
  align-items: center;
  justify-content: space-around;
  margin-bottom: 20rpx;
}
.main-stat {
  text-align: center;
  flex: 1;
}
.main-value {
  display: block;
  font-size: 64rpx;
  font-weight: 800;
  color: #fff;
  line-height: 1.1;
  margin-bottom: 6rpx;
}
.main-label {
  font-size: 24rpx;
  color: rgba(255,255,255,0.7);
}
.main-divider {
  width: 1rpx;
  height: 70rpx;
  background: rgba(255,255,255,0.25);
}
.exercise-name-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
  justify-content: center;
}
.exercise-icon {
  min-width: 72rpx;
  height: 38rpx;
  border-radius: 999rpx;
  background: rgba(49,212,160,0.18);
  color: #31d4a0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18rpx;
  font-weight: 900;
}
.exercise-name {
  font-size: 28rpx;
  color: rgba(255,255,255,0.9);
  font-weight: 600;
}
.exercise-name-row.muted {
  margin-top: 10rpx;
}
.exercise-name-row.muted .exercise-name {
  font-size: 22rpx;
  color: rgba(255,255,255,0.68);
}
.sets-card,
.peak-card,
.ai-card,
.actions-card,
.nav-card,
.target-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
  border: 1rpx solid rgba(222, 228, 236, 0.9);
  box-shadow: 0 14rpx 34rpx rgba(20, 38, 70, 0.06);
}
.target-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.target-rate {
  color: #13b5a5;
  font-size: 34rpx;
  font-weight: 800;
}
.target-bar-wrap {
  height: 14rpx;
  border-radius: 999rpx;
  background: #edf2f7;
  overflow: hidden;
}
.target-bar {
  height: 100%;
  border-radius: 999rpx;
  background: linear-gradient(90deg, #2563eb, #13b5a5);
}
.target-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-top: 16rpx;
  color: #7b8794;
  font-size: 24rpx;
}
.card-title {
  font-size: 30rpx;
  font-weight: 900;
  color: #101828;
  display: block;
  margin-bottom: 16rpx;
}
.set-list {}
.set-item {
  display: flex;
  align-items: center;
  padding: 18rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}
.set-item:last-child { border-bottom: none; }
.set-no {
  font-size: 28rpx;
  font-weight: 900;
  color: #13b5a5;
  min-width: 80rpx;
}
.set-details {
  flex: 1;
  display: flex;
  gap: 24rpx;
}
.set-detail-item {
  display: flex;
  align-items: baseline;
  gap: 4rpx;
}
.set-detail-val {
  font-size: 32rpx;
  font-weight: 900;
  color: #101828;
}
.set-detail-lbl {
  font-size: 22rpx;
  color: #999;
}
.peak-grid {
  display: flex;
  gap: 20rpx;
}
.peak-item {
  flex: 1;
  background: #f5f8fb;
  border-radius: 20rpx;
  padding: 20rpx 16rpx;
  text-align: center;
}
.peak-val {
  display: block;
  font-size: 36rpx;
  font-weight: 800;
  color: #101828;
  margin-bottom: 6rpx;
}
.peak-lbl {
  font-size: 22rpx;
  color: #7b8794;
}
.ai-header {
  display: flex;
  align-items: center;
  gap: 10rpx;
  margin-bottom: 14rpx;
}
.ai-icon {
  width: 78rpx;
  height: 44rpx;
  border-radius: 999rpx;
  background: #101828;
  color: #31d4a0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18rpx;
  font-weight: 900;
}
.ai-heading {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}
.ai-title {
  font-size: 30rpx;
  font-weight: 900;
  color: #101828;
}
.ai-status {
  font-size: 22rpx;
  color: #667085;
}
.ai-card.status-unavailable {
  background: #fffdf8;
  border-color: #f3dfb6;
}
.ai-card.status-processing,
.ai-card.status-queued {
  border-color: #c5f2e6;
}
.ai-card.status-completed {
  border-color: #b7eadf;
}
.ai-content {
  font-size: 26rpx;
  color: #394150;
  line-height: 1.6;
  display: block;
}
.ai-placeholder {
  font-size: 26rpx;
  color: #8a6d3b;
  line-height: 1.6;
  display: block;
}
.ai-suggestions {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
  margin-top: 18rpx;
}
.ai-suggestion {
  background: #f6f8fb;
  border-radius: 14rpx;
  padding: 14rpx 16rpx;
  color: #344054;
  font-size: 24rpx;
  line-height: 1.45;
}
.ai-next {
  display: block;
  margin-top: 16rpx;
  color: #0f9f7a;
  font-size: 24rpx;
  font-weight: 700;
}
.actions-card {
  display: flex;
  gap: 16rpx;
}
.action-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  padding: 24rpx;
  border-radius: 18rpx;
  font-size: 28rpx;
  font-weight: 800;
}
.action-btn.primary {
  background: #101828;
  color: #fff;
  box-shadow: 0 10rpx 24rpx rgba(16, 24, 40, 0.16);
}
.action-btn.secondary {
  background: #e9fbf6;
  color: #0f9f7a;
  border: 1rpx solid #c5f2e6;
}
.action-icon {
  font-size: 18rpx;
  font-weight: 900;
  opacity: 0.76;
}
.nav-item {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}
.nav-item:last-child { border-bottom: none; }
.nav-icon {
  min-width: 78rpx;
  height: 38rpx;
  border-radius: 999rpx;
  background: #f2f5f8;
  color: #475467;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18rpx;
  font-weight: 900;
  margin-right: 14rpx;
}
.nav-item text:nth-child(2) {
  flex: 1;
  font-size: 30rpx;
  color: #101828;
  font-weight: 800;
}
.nav-arrow { font-size: 36rpx; color: #ccc; }
</style>
