<!--
  首页 - 昕动智能小程序 (XIN-121 FE)
  功能: 今日概览、AE算法运动目标、快速开始训练入口
-->
<template>
  <view class="home-page">
    <view class="welcome-bar">
      <view>
        <text class="hello">今天好</text>
        <text class="nickname">{{ loading ? '加载中...' : (userInfo?.nickname || '用户') }}</text>
      </view>
      <text class="refresh-link" @tap.stop="loadHomeData">{{ loading ? '同步中' : '刷新' }}</text>
    </view>

    <view class="error-banner" v-if="loadError">
      <text class="error-text">{{ loadError }}</text>
      <text class="error-action" @tap.stop="loadHomeData">重试</text>
    </view>

    <view class="venue-card" @tap="selectVenue">
      <view class="venue-info">
        <text class="card-label">当前场馆</text>
        <text class="venue-name">{{ currentVenue?.venueName || '智能力量站' }}</text>
        <text class="venue-status" :class="currentVenue?.status">
          {{ venueStatusText }}
        </text>
      </view>
      <text class="arrow">›</text>
    </view>

    <view class="today-card">
      <text class="card-title">今日进度</text>
      <view class="progress-stats">
        <view class="stat-item">
          <text class="stat-value">{{ todayProgress?.completedSessions || 0 }}/{{ todayProgress?.plannedSessions || 0 }}</text>
          <text class="stat-label">任务</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">{{ todayProgress?.totalDurationMin || 0 }}</text>
          <text class="stat-label">分钟</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">{{ todayProgress?.complianceRate || 0 }}%</text>
          <text class="stat-label">完成</text>
        </view>
      </view>
    </view>

    <view class="hint-card" v-if="aiSuggestion">
      <text class="hint-label">今日建议</text>
      <text class="hint-text">{{ aiSuggestion }}</text>
    </view>

    <view class="action-card" :class="{ disabled: !hasVenue }" @tap="startTraining">
      <text class="action-title">开始训练</text>
      <text class="action-desc">{{ hasVenue ? '扫码连接器械' : '先选择训练场馆' }}</text>
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
      selectVenue,
      loadHomeData,
      startTraining,
    };
  },
});
</script>

<style>
.home-page {
  padding: 32rpx 24rpx;
  background: #f4f7fb;
  min-height: 100vh;
  box-sizing: border-box;
}
.welcome-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 28rpx;
}
.hello {
  display: block;
  font-size: 24rpx;
  color: #7b8794;
  margin-bottom: 6rpx;
}
.nickname {
  display: block;
  font-size: 42rpx;
  font-weight: 800;
  color: #172033;
}
.refresh-link {
  font-size: 24rpx;
  color: #4A90E2;
  font-weight: 700;
  padding: 10rpx 0 10rpx 20rpx;
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
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  border: 1rpx solid #edf1f6;
  box-shadow: 0 10rpx 30rpx rgba(20, 38, 70, 0.04);
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
.card-label {
  display: block;
  font-size: 24rpx;
  color: #7b8794;
  margin-bottom: 8rpx;
}
.venue-name {
  font-size: 34rpx;
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
  color: #ccc;
}
.card-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #172033;
  display: block;
  margin-bottom: 24rpx;
}
.progress-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
}
.stat-item {
  text-align: center;
  min-width: 0;
}
.stat-value {
  font-size: 42rpx;
  font-weight: 800;
  color: #4A90E2;
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
  padding: 26rpx 28rpx;
}
.hint-label {
  display: block;
  font-size: 24rpx;
  color: #4A90E2;
  font-weight: 700;
  margin-bottom: 8rpx;
}
.hint-text {
  display: block;
  font-size: 26rpx;
  color: #394150;
  line-height: 1.55;
}
.action-card {
  background: #2563eb;
  border: 0;
  box-shadow: 0 18rpx 36rpx rgba(37, 99, 235, 0.18);
}
.action-card.disabled {
  background: #9aa6b2;
  box-shadow: none;
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

</style>
