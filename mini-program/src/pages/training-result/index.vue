<!--
  训练结果页面 - 昕动智能小程序
  功能: 展示训练完成数据、组详情、AI反馈、分享/保存
-->
<template>
  <view class="result-page">
    <!-- 页面头部 -->
    <view class="page-header">
      <text class="page-title">训练报告</text>
      <text class="page-subtitle">{{ sessionDate }}</text>
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
        <text class="exercise-icon">🏋️</text>
        <text class="exercise-name">{{ exerciseName }}</text>
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

    <!-- AI反馈区域 (预留) -->
    <view class="ai-card" v-if="aiFeedback">
      <view class="ai-header">
        <text class="ai-icon">🤖</text>
        <text class="ai-title">AI 分析</text>
      </view>
      <text class="ai-content">{{ aiFeedback }}</text>
    </view>

    <view class="ai-card placeholder" v-else>
      <view class="ai-header">
        <text class="ai-icon">🤖</text>
        <text class="ai-title">AI 分析</text>
      </view>
      <text class="ai-placeholder">完成更多训练后，这里将显示AI为您生成的个性化训练反馈和建议。</text>
    </view>

    <!-- 操作按钮 -->
    <view class="actions-card">
      <view class="action-btn primary" @tap="onShare">
        <text class="action-icon">📤</text>
        <text>分享到微信</text>
      </view>
      <view class="action-btn secondary" @tap="onSaveToAlbum">
        <text class="action-icon">💾</text>
        <text>保存到相册</text>
      </view>
    </view>

    <!-- 返回入口 -->
    <view class="nav-card">
      <view class="nav-item" @tap="goProgress">
        <text class="nav-icon">📊</text>
        <text>查看训练进度</text>
        <text class="nav-arrow">›</text>
      </view>
      <view class="nav-item" @tap="goHome">
        <text class="nav-icon">🏠</text>
        <text>返回首页</text>
        <text class="nav-arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script lang="ts">
import { defineComponent, ref, computed, onMounted } from 'vue';
import { useDidShow } from '@tarojs/taro';

export default defineComponent({
  setup() {
    const exerciseName = ref('');
    const completedSets = ref(0);
    const totalReps = ref(0);
    const durationMin = ref(0);
    const sets = ref<any[]>([]);
    const aiFeedback = ref('');
    const sessionDate = ref('');

    onMounted(() => {
      loadResultData();
    });

    function loadResultData() {
      try {
        const params = Taro.getCurrentInstance()?.router?.params || {};
        if (params.data) {
          const decoded = JSON.parse(decodeURIComponent(params.data));
          exerciseName.value = decoded.exerciseName || '';
          completedSets.value = decoded.completedSets || 0;
          totalReps.value = decoded.totalReps || 0;
          durationMin.value = decoded.durationMin || 0;
          sets.value = decoded.sets || [];
          aiFeedback.value = decoded.aiFeedback || '';
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

    const hasPeakData = computed(() => {
      return peakReps.value > 0 || peakDuration.value > 0 || peakLoadKg.value > 0;
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
      completedSets,
      totalReps,
      durationMin,
      sets,
      aiFeedback,
      sessionDate,
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
  background: #f4f7fb;
  min-height: 100vh;
  box-sizing: border-box;
}
.page-header {
  margin-bottom: 24rpx;
}
.page-title {
  display: block;
  font-size: 44rpx;
  font-weight: 800;
  color: #172033;
  margin-bottom: 8rpx;
}
.page-subtitle {
  font-size: 26rpx;
  color: #7b8794;
}
.overview-card {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  border-radius: 20rpx;
  padding: 36rpx 28rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 18rpx 40rpx rgba(37, 99, 235, 0.22);
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
.exercise-icon { font-size: 28rpx; }
.exercise-name {
  font-size: 28rpx;
  color: rgba(255,255,255,0.9);
  font-weight: 600;
}
.sets-card,
.peak-card,
.ai-card,
.actions-card,
.nav-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
  border: 1rpx solid #edf1f6;
  box-shadow: 0 10rpx 30rpx rgba(20, 38, 70, 0.04);
}
.card-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #1a1a2e;
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
  font-weight: 700;
  color: #2563eb;
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
  font-weight: 700;
  color: #172033;
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
  background: #f8f9fa;
  border-radius: 12rpx;
  padding: 20rpx 16rpx;
  text-align: center;
}
.peak-val {
  display: block;
  font-size: 36rpx;
  font-weight: 800;
  color: #2563eb;
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
.ai-icon { font-size: 32rpx; }
.ai-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #1a1a2e;
}
.ai-content {
  font-size: 26rpx;
  color: #394150;
  line-height: 1.6;
}
.ai-placeholder {
  font-size: 26rpx;
  color: #bbb;
  line-height: 1.6;
  font-style: italic;
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
  border-radius: 14rpx;
  font-size: 28rpx;
  font-weight: 600;
}
.action-btn.primary {
  background: #2563eb;
  color: #fff;
  box-shadow: 0 10rpx 24rpx rgba(37, 99, 235, 0.2);
}
.action-btn.secondary {
  background: #f0f7ff;
  color: #2563eb;
  border: 1rpx solid #b3d4fc;
}
.action-icon { font-size: 32rpx; }
.nav-item {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}
.nav-item:last-child { border-bottom: none; }
.nav-icon { font-size: 32rpx; margin-right: 14rpx; }
.nav-item text:nth-child(2) {
  flex: 1;
  font-size: 30rpx;
  color: #1a1a2e;
}
.nav-arrow { font-size: 36rpx; color: #ccc; }
</style>