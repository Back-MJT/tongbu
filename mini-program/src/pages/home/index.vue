<!--
  首页 - 昕动智能小程序 (XIN-121 FE)
  功能: 今日概览、AE算法运动目标、快速开始训练入口
-->
<template>
  <view class="home-page">
    <view class="hero-card">
      <view class="hero-top">
        <view>
          <text class="eyebrow">HEALTHHUB TODAY</text>
          <text v-if="loading" class="hero-title">同步训练状态</text>
          <view v-else class="hero-title hero-title-inline">
            <text>今天好，</text>
            <text v-if="displayNickname">{{ displayNickname }}</text>
            <open-data v-else type="userNickName"></open-data>
          </view>
        </view>
        <view class="sync-pill" @tap.stop="loadHomeData">
          <text>{{ loading ? '同步中' : '刷新' }}</text>
        </view>
      </view>

      <view class="hero-progress">
        <view class="score-ring" :style="scoreRingStyle">
          <text class="score-value">{{ todayCompletionRate }}</text>
          <text class="score-unit">%</text>
        </view>
        <view class="hero-copy">
          <text class="hero-label">今日完成率</text>
          <text class="hero-desc">{{ currentPlanLabel }}</text>
        </view>
      </view>

      <view class="hero-metrics">
        <view class="metric">
          <text class="metric-value">{{ todayProgress?.completedSessions || 0 }}/{{ todayProgress?.plannedSessions || 0 }}</text>
          <text class="metric-label">任务</text>
        </view>
        <view class="metric">
          <text class="metric-value">{{ todayProgress?.totalDurationMin || 0 }}</text>
          <text class="metric-label">分钟</text>
        </view>
        <view class="metric">
          <text class="metric-value">{{ userInfo?.streakDays || 0 }}</text>
          <text class="metric-label">连续天</text>
        </view>
      </view>
    </view>

    <view class="error-banner" v-if="loadError">
      <text class="error-text">{{ loadError }}</text>
      <text class="error-action" @tap.stop="loadHomeData">重试</text>
    </view>

    <view class="venue-card" @tap="selectVenue">
      <view class="venue-mark">
        <text>GYM</text>
      </view>
      <view class="venue-info">
        <text class="card-label">当前场馆</text>
        <text class="venue-name">{{ currentVenue?.venueName || '智能力量站' }}</text>
        <text class="venue-status" :class="currentVenue?.status">
          {{ venueStatusText }}
        </text>
      </view>
      <text class="arrow">›</text>
    </view>

    <view class="action-card" :class="{ disabled: !hasVenue }" @tap="startTraining">
      <view>
        <text class="action-kicker">{{ hasVenue ? 'READY' : 'SETUP' }}</text>
        <text class="action-title">开始器械训练</text>
        <text class="action-desc">{{ hasVenue ? '扫码连接器械，自动记录组数与次数' : '先选择训练场馆后开始' }}</text>
      </view>
      <view class="scan-symbol">
        <view class="scan-line"></view>
      </view>
    </view>

    <view class="today-card">
      <view class="section-head">
        <text class="card-title">今日计划</text>
        <text class="section-meta">{{ remainingTasks }} 项待完成</text>
      </view>
      <view class="plan-track">
        <view class="plan-track-fill" :style="{ width: todayCompletionRate + '%' }"></view>
      </view>
      <view class="progress-stats">
        <view class="stat-item">
          <text class="stat-value">{{ todayProgress?.completedSessions || 0 }}</text>
          <text class="stat-label">已完成</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">{{ remainingTasks }}</text>
          <text class="stat-label">待训练</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">{{ todayProgress?.totalDurationMin || 0 }}</text>
          <text class="stat-label">累计分钟</text>
        </view>
      </view>
    </view>

    <view class="hint-card" v-if="aiSuggestion">
      <view class="coach-badge">COACH</view>
      <view class="hint-content">
        <text class="hint-label">今日建议</text>
        <text class="hint-text">{{ aiSuggestion }}</text>
      </view>
    </view>
  </view>
</template>

<script lang="ts">
import { computed, defineComponent, ref, onMounted } from 'vue';
import { useDidShow } from '@tarojs/taro';
import {
  getCurrentUser,
  getTodayProgress,
  getMiniprogramPrescription,
  getCurrentVenue,
  listVenues,
  getDemoData,
  isDemoMode,
} from '../../services/api';
import { config } from '../../config/env';

export default defineComponent({
  setup() {
    const useDemoInitialData = config.demoMode || isDemoMode();
    const initialUser = useDemoInitialData ? getDemoData('user') : null;
    const initialProgress = useDemoInitialData ? getDemoData('todayProgress') : null;
    const initialVenue = useDemoInitialData ? getDemoData('venue') : null;
    const initialPrescription = useDemoInitialData ? getDemoData('miniPrescription') : {};

    const userInfo = ref(initialUser);
    const todayProgress = ref(initialProgress);
    const currentVenue = ref(initialVenue);
    const venueList = ref([]);
    const aiSuggestion = ref(initialPrescription.aiSuggestion || '');
    const loading = ref(!useDemoInitialData);
    const loadError = ref('');

    // 合并后端返回数据与默认值的辅助函数
    function mergeWithDefaults(data: any, defaults: any): any {
      if (!data || typeof data !== 'object') return defaults;
      return { ...defaults, ...data };
    }

    onMounted(async () => {
      await loadHomeData();
    });

    // 修复：Tab页切换时 useDidShow 会触发，重新加载数据
    useDidShow(() => {
      console.log('[Home] useDidShow triggered, checking login and loading data...');
      const token = wx.getStorageSync('auth_token');
      const loginType = wx.getStorageSync('login_type');

      console.log('[Home] useDidShow: token =', token ? 'exists' : 'missing', ', loginType =', loginType);

      // 如果没有 token 且不是演示模式，跳转到登录页
      if (!token && loginType !== 'demo') {
        console.log('[Home] No token and not demo mode, redirecting to login...');
        wx.reLaunch({ url: '/pages/login/index' });
        return;
      }

      loadHomeData();
    });

    async function loadHomeData() {
      console.log('[Home] loadHomeData: config.demoMode =', config.demoMode);
      console.log('[Home] loadHomeData: token =', wx.getStorageSync('auth_token') ? 'exists' : 'missing');
      console.log('[Home] loadHomeData: login_type =', wx.getStorageSync('login_type'));
      console.log('[Home] loadHomeData: demo_mode =', wx.getStorageSync('demo_mode'));
      console.log('[Home] loadHomeData: user_id =', wx.getStorageSync('user_id'));

      if (config.demoMode || isDemoMode()) {
        console.log('[Home] Demo mode, using initial data');
        loading.value = false;
        return;
      }

      loading.value = true;
      loadError.value = '';
      const token = wx.getStorageSync('auth_token');
      const loginType = wx.getStorageSync('login_type');
      if (!token && loginType !== 'demo') {
        console.log('[Home] loadHomeData: no token, redirecting to login before API calls');
        wx.reLaunch({ url: '/pages/login/index' });
        return;
      }

      console.log('[Home] loadHomeData: start loading user data...');

      // 并行加载用户信息和今日进度
      const [userRes, progRes, venueRes] = await Promise.all([
        getCurrentUser().catch((e) => {
          console.error('[Home] getCurrentUser failed:', e);
          return { data: null, error: e };
        }),
        getTodayProgress().catch((e) => {
          console.error('[Home] getTodayProgress failed:', e);
          return { data: null, error: e };
        }),
        getCurrentVenue().catch((e) => {
          console.error('[Home] getCurrentVenue failed:', e);
          return { data: null, error: e };
        }),
      ]);

      const failedParts = [
        userRes.error ? '用户信息' : '',
        progRes.error ? '今日进度' : '',
        venueRes.error ? '场馆' : '',
      ].filter(Boolean);
      if (failedParts.length) {
        loadError.value = `后台连接异常，${failedParts.join('、')}暂未同步，可重试`;
        wx.showToast({ title: '后台连接异常，请重试', icon: 'none', duration: 2000 });
      }

      // 打印 API 返回的原始数据
      console.log('[Home] API userRes:', JSON.stringify(userRes).slice(0, 200));
      console.log('[Home] API progRes:', JSON.stringify(progRes).slice(0, 200));
      console.log('[Home] API venueRes:', JSON.stringify(venueRes).slice(0, 200));

      // 合并后端返回数据与默认值，确保字段完整
      const userInfoDefaults = {
        nickname: '用户',
        streakDays: 0,
        complianceRate: 0,
        totalSessions: 0,
        level: 1,
      };
      const todayProgressDefaults = {
        completedSessions: 0,
        plannedSessions: 0,
        totalDurationMin: 0,
        complianceRate: 0,
      };

      userInfo.value = mergeWithDefaults(userRes.data, userInfoDefaults);
      todayProgress.value = mergeWithDefaults(progRes.data, todayProgressDefaults);
      currentVenue.value = mergeWithDefaults(venueRes.data, {
        venueName: '智能力量站',
        description: '请选择当前训练场馆',
        deviceCount: 0,
        status: 'pending',
      });
      const savedVenueId = wx.getStorageSync('current_venue_id');
      if (savedVenueId && currentVenue.value?.venueId && String(savedVenueId) !== String(currentVenue.value.venueId)) {
        currentVenue.value.venueId = Number(savedVenueId);
        currentVenue.value.venueName = wx.getStorageSync('current_venue_name') || currentVenue.value.venueName;
      } else if (currentVenue.value?.venueId) {
        wx.setStorageSync('current_venue_id', currentVenue.value.venueId);
        wx.setStorageSync('current_venue_name', currentVenue.value.venueName || '');
      }

      console.log('[Home] loadHomeData: userInfo=', userInfo.value);
      console.log('[Home] loadHomeData: todayProgress=', todayProgress.value);
      console.log('[Home] loadHomeData: currentVenue=', currentVenue.value);

      // 加载AE算法运动处方 (目标和建议)
      try {
        const userId = wx.getStorageSync('user_id');
        const age = wx.getStorageSync('user_age') || userInfo.value?.age || 30;

        const rxRes = await getMiniprogramPrescription({
          user_id: userId,
          age: age,
          gender: wx.getStorageSync('user_gender') || undefined,
          device_type: wx.getStorageSync('device_type') || undefined,
          venueId: currentVenue.value?.venueId || wx.getStorageSync('current_venue_id') || undefined,
          sessions_last_30_days: wx.getStorageSync('sessions_30d') || undefined,
        });

        if (rxRes.code === 200 && rxRes.data) {
          aiSuggestion.value = rxRes.data.aiSuggestion || '';
          console.log('[Home] loadHomeData: prescription loaded successfully');
        } else {
          console.warn('[Home] loadHomeData: prescription code not 200, rxRes=', rxRes);
        }
      } catch (e) {
        console.warn('[Home] AE prescription load failed, goal/suggestion unavailable', e);
      }
      loading.value = false;
    }

    async function selectVenue() {
      try {
        wx.showLoading({ title: '加载场馆...' });
        const res = await listVenues();
        wx.hideLoading();
        const venues = Array.isArray(res.data) ? res.data : [];
        venueList.value = venues;
        if (!venues.length) {
          wx.showToast({ title: '暂无场馆，请先在后台设备分组维护', icon: 'none' });
          return;
        }
        wx.showActionSheet({
          itemList: venues.map((venue) => venue.venueName || '未命名场馆'),
          success: ({ tapIndex }) => {
            const venue = venues[tapIndex];
            currentVenue.value = venue;
            wx.setStorageSync('current_venue_id', venue.venueId || '');
            wx.setStorageSync('current_venue_name', venue.venueName || '');
            loadHomeData();
          },
        });
      } catch (e) {
        wx.hideLoading();
        console.error('[Home] selectVenue failed', e);
        wx.showToast({ title: '场馆加载失败', icon: 'none' });
      }
    }

    function startTraining() {
      if (!currentVenue.value?.venueId && !wx.getStorageSync('current_venue_id')) {
        wx.showToast({ title: '请先选择场馆', icon: 'none' });
        return;
      }
      wx.navigateTo({ url: '/pages/device-binding/index' });
    }

    const venueStatusText = computed(() => {
      if (currentVenue.value?.status === 'open') {
        const count = currentVenue.value?.deviceCount || 0;
        return count > 0 ? `● 开放中 · ${count}台设备` : '● 开放中';
      }
      return '○ 待配置';
    });

    const hasVenue = computed(() => {
      return !!(currentVenue.value?.venueId || wx.getStorageSync('current_venue_id'));
    });

    const todayCompletionRate = computed(() => {
      const completed = Number(todayProgress.value?.completedSessions || 0);
      const planned = Number(todayProgress.value?.plannedSessions || 0);
      if (planned > 0) {
        return Math.min(100, Math.round((completed / planned) * 100));
      }
      return Math.min(100, Number(todayProgress.value?.complianceRate || 0));
    });

    const remainingTasks = computed(() => {
      const completed = Number(todayProgress.value?.completedSessions || 0);
      const planned = Number(todayProgress.value?.plannedSessions || 0);
      return Math.max(0, planned - completed);
    });

    const currentPlanLabel = computed(() => {
      if (!hasVenue.value) return '选择场馆后生成今日训练计划';
      if (remainingTasks.value === 0 && Number(todayProgress.value?.plannedSessions || 0) > 0) {
        return '今日计划已完成，适合做恢复拉伸';
      }
      return remainingTasks.value > 0
        ? `还剩 ${remainingTasks.value} 项训练，建议先完成主力动作`
        : '扫码器械即可开始记录真实训练';
    });

    const displayNickname = computed(() => {
      const name = String(userInfo.value?.nickname || '').trim();
      if (!name || name === '微信用户' || name === '用户') {
        return '';
      }
      return name;
    });

    const scoreRingStyle = computed(() => {
      const rate = Math.max(0, Math.min(100, todayCompletionRate.value));
      return {
        background: `radial-gradient(circle at center, #101828 54%, transparent 55%), conic-gradient(#31d4a0 0%, #31d4a0 ${rate}%, rgba(255,255,255,0.16) ${rate}%, rgba(255,255,255,0.16) 100%)`,
      };
    });

    return {
      userInfo,
      todayProgress,
      currentVenue,
      venueList,
      aiSuggestion,
      loading,
      loadError,
      venueStatusText,
      hasVenue,
      todayCompletionRate,
      scoreRingStyle,
      remainingTasks,
      currentPlanLabel,
      displayNickname,
      selectVenue,
      loadHomeData,
      startTraining,
    };
  },
});
</script>

<style>
.home-page {
  padding: 28rpx 24rpx 56rpx;
  background: #edf2f7;
  min-height: 100vh;
  box-sizing: border-box;
}
.hero-card {
  background: linear-gradient(145deg, #101828 0%, #1f2a44 58%, #123f3a 100%);
  border-radius: 28rpx;
  padding: 34rpx 30rpx 30rpx;
  margin-bottom: 24rpx;
  color: #fff;
  box-shadow: 0 24rpx 48rpx rgba(16, 24, 40, 0.22);
}
.hero-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20rpx;
}
.eyebrow {
  display: block;
  font-size: 20rpx;
  color: rgba(255,255,255,0.58);
  font-weight: 800;
  letter-spacing: 0;
  margin-bottom: 12rpx;
}
.hero-title {
  display: block;
  font-size: 42rpx;
  font-weight: 800;
  color: #fff;
  line-height: 1.18;
}
.hero-title-inline {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}
.sync-pill {
  min-width: 104rpx;
  height: 56rpx;
  border-radius: 999rpx;
  background: rgba(255,255,255,0.12);
  border: 1rpx solid rgba(255,255,255,0.16);
  color: rgba(255,255,255,0.86);
  font-size: 24rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
.hero-progress {
  display: flex;
  align-items: center;
  gap: 24rpx;
  margin-top: 38rpx;
}
.score-ring {
  width: 158rpx;
  height: 158rpx;
  border-radius: 50%;
  background: radial-gradient(circle at center, #101828 54%, transparent 55%),
    conic-gradient(#31d4a0 0%, #31d4a0 65%, rgba(255,255,255,0.16) 65%, rgba(255,255,255,0.16) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}
.score-value {
  color: #fff;
  font-size: 48rpx;
  font-weight: 900;
}
.score-unit {
  color: rgba(255,255,255,0.66);
  font-size: 22rpx;
  margin-top: 20rpx;
}
.hero-copy {
  flex: 1;
  min-width: 0;
}
.hero-label {
  display: block;
  color: #31d4a0;
  font-size: 24rpx;
  font-weight: 800;
  margin-bottom: 8rpx;
}
.hero-desc {
  display: block;
  color: rgba(255,255,255,0.82);
  font-size: 28rpx;
  line-height: 1.45;
}
.hero-metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14rpx;
  margin-top: 32rpx;
}
.metric {
  min-height: 118rpx;
  padding: 20rpx 14rpx;
  border-radius: 20rpx;
  background: rgba(255,255,255,0.1);
  border: 1rpx solid rgba(255,255,255,0.12);
}
.metric-value {
  display: block;
  color: #fff;
  font-size: 34rpx;
  font-weight: 900;
  line-height: 1.15;
}
.metric-label {
  display: block;
  margin-top: 10rpx;
  color: rgba(255,255,255,0.58);
  font-size: 22rpx;
}
.error-banner {
  background: #fff7e6;
  border: 1rpx solid #ffd591;
  border-radius: 12rpx;
  color: #ad6800;
  font-size: 26rpx;
  line-height: 1.5;
  padding: 20rpx 24rpx;
  margin-bottom: 24rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.error-text {
  flex: 1;
}
.error-action {
  flex-shrink: 0;
  color: #4A90E2;
  font-weight: 600;
}
.venue-card,
.today-card,
.action-card,
.hint-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 30rpx;
  margin-bottom: 24rpx;
  border: 1rpx solid rgba(222, 228, 236, 0.9);
  box-shadow: 0 14rpx 34rpx rgba(20, 38, 70, 0.06);
}
.venue-card {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.venue-mark {
  width: 84rpx;
  height: 84rpx;
  border-radius: 24rpx;
  background: #101828;
  color: #31d4a0;
  font-size: 22rpx;
  font-weight: 900;
  display: flex;
  align-items: center;
  justify-content: center;
}
.venue-info {
  flex: 1;
  min-width: 0;
}
.card-label {
  display: block;
  font-size: 24rpx;
  color: #7b8794;
  margin-bottom: 8rpx;
}
.venue-name {
  font-size: 32rpx;
  font-weight: 800;
  color: #172033;
  display: block;
  margin-bottom: 8rpx;
}
.venue-status {
  font-size: 26rpx;
  color: #999;
  display: block;
  margin-bottom: 8rpx;
}
.venue-status.open {
  color: #52c41a;
}
.arrow {
  font-size: 48rpx;
  color: #9aa6b2;
}
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18rpx;
}
.card-title {
  font-size: 30rpx;
  font-weight: 800;
  color: #172033;
  display: block;
}
.section-meta {
  color: #7b8794;
  font-size: 24rpx;
  font-weight: 700;
}
.plan-track {
  height: 14rpx;
  border-radius: 999rpx;
  background: #e8eef5;
  overflow: hidden;
  margin-bottom: 26rpx;
}
.plan-track-fill {
  height: 100%;
  border-radius: 999rpx;
  background: linear-gradient(90deg, #31d4a0, #2f80ed);
}
.progress-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
}
.stat-item {
  text-align: center;
  min-width: 0;
  padding: 18rpx 10rpx;
  border-radius: 18rpx;
  background: #f6f8fb;
}
.stat-value {
  font-size: 38rpx;
  font-weight: 800;
  color: #101828;
  display: block;
  line-height: 1.15;
}
.stat-label {
  font-size: 24rpx;
  color: #7b8794;
  margin-top: 8rpx;
  display: block;
}
.hint-card {
  display: flex;
  gap: 20rpx;
  align-items: flex-start;
  padding: 28rpx;
}
.coach-badge {
  flex-shrink: 0;
  min-width: 88rpx;
  height: 48rpx;
  border-radius: 999rpx;
  background: #e9fbf6;
  color: #0f9f7a;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20rpx;
  font-weight: 900;
}
.hint-content {
  flex: 1;
  min-width: 0;
}
.hint-label {
  display: block;
  font-size: 24rpx;
  color: #101828;
  font-weight: 800;
  margin-bottom: 8rpx;
}
.hint-text {
  display: block;
  font-size: 26rpx;
  color: #394150;
  line-height: 1.55;
}
.action-card {
  background: linear-gradient(135deg, #2563eb, #13b5a5);
  border: 0;
  box-shadow: 0 20rpx 42rpx rgba(37, 99, 235, 0.2);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24rpx;
}
.action-card.disabled {
  background: #9aa6b2;
  box-shadow: none;
}
.action-kicker {
  display: block;
  color: rgba(255,255,255,0.68);
  font-size: 20rpx;
  font-weight: 900;
  margin-bottom: 8rpx;
}
.action-title {
  font-size: 36rpx;
  font-weight: 900;
  color: #fff;
  display: block;
  margin-bottom: 8rpx;
}
.action-desc {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.45;
}
.scan-symbol {
  width: 104rpx;
  height: 104rpx;
  border-radius: 28rpx;
  border: 2rpx solid rgba(255,255,255,0.54);
  position: relative;
  flex-shrink: 0;
}
.scan-line {
  position: absolute;
  left: 18rpx;
  right: 18rpx;
  top: 50rpx;
  height: 4rpx;
  border-radius: 4rpx;
  background: #fff;
}

</style>
