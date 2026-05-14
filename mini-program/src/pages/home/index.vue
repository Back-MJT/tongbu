<!--
  首页 - 昕动智能小程序 (XIN-121 FE)
  功能: 今日概览、AE算法运动目标、快速开始训练入口
-->
<template>
  <view class="home-page">
    <!-- 顶部欢迎 -->
    <view class="welcome-bar">
      <text class="nickname">{{ loading ? '加载中...' : (userInfo?.nickname || '用户') }}</text>
      <text class="streak">🔥 {{ userInfo?.streakDays || 0 }}天连续训练</text>
    </view>

    <view class="error-banner" v-if="loadError">
      <text>{{ loadError }}</text>
    </view>

    <!-- 场馆卡片 -->
    <view class="venue-card" @tap="selectVenue">
      <view class="venue-info">
        <text class="venue-name">{{ currentVenue?.venueName || '智能力量站' }}</text>
        <text class="venue-status" :class="currentVenue?.status">
          {{ venueStatusText }}
        </text>
        <text class="venue-desc">{{ currentVenue?.description || '请选择当前训练场馆' }}</text>
      </view>
      <text class="arrow">›</text>
    </view>

    <!-- 今日进度 -->
    <view class="today-card">
      <text class="card-title">今日进度</text>
      <view class="progress-stats">
        <view class="stat-item">
          <text class="stat-value">{{ todayProgress?.completedSessions || 0 }}/{{ todayProgress?.plannedSessions || 0 }}</text>
          <text class="stat-label">训练组数</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">{{ todayProgress?.totalDurationMin || 0 }}分钟</text>
          <text class="stat-label">训练时长</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">{{ todayProgress?.complianceRate || 0 }}%</text>
          <text class="stat-label">完成率</text>
        </view>
      </view>
    </view>

    <!-- 今日建议 -->
    <view class="goal-card" v-if="aiSuggestion" @tap="goTrainingPlan">
      <view class="goal-header">
        <text class="goal-icon">🎯</text>
        <view class="goal-info">
          <text class="goal-title">今日建议</text>
          <text class="goal-subtitle" v-if="exerciseGoal">{{ exerciseGoal }}</text>
        </view>
        <text class="goal-arrow">›</text>
      </view>
      <text class="goal-suggestion" v-if="aiSuggestion">{{ aiSuggestion }}</text>
      <view class="goal-stage" v-if="userStage">
        <text class="stage-badge">{{ stageLabel }}</text>
      </view>
    </view>

    <!-- 快速开始 -->
    <view class="action-card" @tap="startTraining">
      <text class="action-title">开始训练</text>
      <text class="action-desc">查看今日任务，按任务扫码器械训练</text>
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
    const exerciseGoal = ref(initialPrescription.exerciseGoal || '');
    const exerciseGoalEn = ref(initialPrescription.exerciseGoalEn || '');
    const aiSuggestion = ref(initialPrescription.aiSuggestion || '');
    const userStage = ref(initialPrescription.userStage || '');
    const loading = ref(!useDemoInitialData);
    const loadError = ref('');

    const stageLabelMap = {
      beginner: '适应期',
      growth: '成长期',
      plateau: '突破期',
      advanced: '进阶期',
    };
    const stageLabel = ref(stageLabelMap[initialPrescription.userStage] || '');

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

      if (userRes.error || progRes.error || venueRes.error) {
        userInfo.value = { nickname: '用户', streakDays: 0, complianceRate: 0, totalSessions: 0, level: 1 };
        todayProgress.value = { completedSessions: 0, plannedSessions: 0, totalDurationMin: 0, complianceRate: 0 };
        currentVenue.value = null;
        exerciseGoal.value = '';
        exerciseGoalEn.value = '';
        aiSuggestion.value = '';
        userStage.value = '';
        stageLabel.value = '';
        loadError.value = '数据加载失败，请下拉刷新';
        loading.value = false;
        wx.showToast({ title: loadError.value, icon: 'none', duration: 2000 });
        return;
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
          exerciseGoal.value = rxRes.data.exerciseGoal || '';
          exerciseGoalEn.value = rxRes.data.exerciseGoalEn || '';
          aiSuggestion.value = rxRes.data.aiSuggestion || '';
          userStage.value = rxRes.data.userStage || '';
          stageLabel.value = stageLabelMap[rxRes.data.userStage] || '';
          console.log('[Home] loadHomeData: prescription loaded successfully');
        } else {
          console.warn('[Home] loadHomeData: prescription code not 200, rxRes=', rxRes);
        }
      } catch (e) {
        console.warn('[Home] AE prescription load failed, goal/suggestion unavailable', e);
      }
      loading.value = false;
    }

    function goDeviceBinding() {
      wx.navigateTo({ url: '/pages/device-binding/index' });
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
          },
        });
      } catch (e) {
        wx.hideLoading();
        console.error('[Home] selectVenue failed', e);
        wx.showToast({ title: '场馆加载失败', icon: 'none' });
      }
    }

    function goTrainingPlan() {
      wx.switchTab({ url: '/pages/daily-task/index' });
    }

    function startTraining() {
      if (!currentVenue.value?.venueId && !wx.getStorageSync('current_venue_id')) {
        wx.showToast({ title: '请先选择场馆', icon: 'none' });
        return;
      }
      wx.switchTab({ url: '/pages/daily-task/index' });
    }

    const venueStatusText = computed(() => {
      if (currentVenue.value?.status === 'open') {
        const count = currentVenue.value?.deviceCount || 0;
        return count > 0 ? `● 开放中 · ${count}台设备` : '● 开放中';
      }
      return '○ 待配置';
    });

    return {
      userInfo,
      todayProgress,
      currentVenue,
      venueList,
      exerciseGoal,
      exerciseGoalEn,
      aiSuggestion,
      userStage,
      stageLabel,
      loading,
      loadError,
      venueStatusText,
      selectVenue,
      goDeviceBinding,
      goTrainingPlan,
      startTraining,
    };
  },
});
</script>

<style>
.home-page {
  padding: 24rpx;
  background: #f5f5f5;
  min-height: 100vh;
}
.welcome-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}
.nickname {
  font-size: 36rpx;
  font-weight: bold;
  color: #1a1a2e;
}
.streak {
  font-size: 26rpx;
  color: #ff6b35;
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
}
.venue-card,
.today-card,
.action-card,
.goal-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
}
.venue-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.venue-info {
  flex: 1;
  min-width: 0;
}
.venue-name {
  font-size: 32rpx;
  font-weight: 600;
  color: #1a1a2e;
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
.venue-desc {
  font-size: 24rpx;
  color: #777;
  line-height: 1.5;
}
.arrow {
  font-size: 48rpx;
  color: #ccc;
}
.card-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #1a1a2e;
  display: block;
  margin-bottom: 24rpx;
}
.progress-stats {
  display: flex;
  justify-content: space-around;
}
.stat-item {
  text-align: center;
}
.stat-value {
  font-size: 40rpx;
  font-weight: bold;
  color: #4A90E2;
  display: block;
}
.stat-label {
  font-size: 24rpx;
  color: #999;
}
.action-card {
  background: linear-gradient(135deg, #4A90E2, #6BB5FF);
}
.action-title {
  font-size: 36rpx;
  font-weight: bold;
  color: #fff;
  display: block;
  margin-bottom: 8rpx;
}
.action-desc {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.8);
}

/* AI运动目标卡片 */
.goal-card {
  background: linear-gradient(135deg, #f0f7ff, #e8f4ff);
  border: 1rpx solid #b3d4fc;
}
.goal-header {
  display: flex;
  align-items: center;
}
.goal-icon {
  font-size: 40rpx;
  margin-right: 16rpx;
}
.goal-info {
  flex: 1;
}
.goal-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #1a1a2e;
  display: block;
  margin-bottom: 4rpx;
}
.goal-subtitle {
  font-size: 22rpx;
  color: #999;
}
.goal-arrow {
  font-size: 40rpx;
  color: #ccc;
}
.goal-suggestion {
  font-size: 26rpx;
  color: #666;
  line-height: 1.5;
  display: block;
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid rgba(74, 144, 226, 0.2);
}
.goal-stage {
  margin-top: 12rpx;
}
.stage-badge {
  font-size: 22rpx;
  color: #4A90E2;
  background: rgba(74, 144, 226, 0.15);
  padding: 4rpx 14rpx;
  border-radius: 12rpx;
}

</style>
