<!--
  我的档案页面 - 昕动智能小程序
  功能: 用户信息、设备绑定管理、当前阶段、设置入口
-->
<template>
  <view class="profile-page">
    <!-- 用户信息卡片 -->
    <view class="profile-card">
      <view class="profile-top">
        <view class="avatar-wrap" @tap="onAvatarClick">
          <image
            v-if="userInfo?.avatar"
            class="avatar-img"
            :src="userInfo.avatar"
            mode="aspectFill"
          />
          <open-data v-else class="avatar-img avatar-open-data" type="userAvatarUrl"></open-data>
          <view class="avatar-edit-icon">EDIT</view>
        </view>
        <view class="user-info">
          <text class="eyebrow">MEMBER PROFILE</text>
          <text v-if="displayNickname" class="nickname">{{ displayNickname }}</text>
          <open-data v-else class="nickname" type="userNickName"></open-data>
          <text class="user-stage">{{ stageLabel }} · {{ userInfo?.level || 1 }}级</text>
        </view>
      </view>

      <view class="stage-progress-wrap">
        <view class="stage-progress-bar">
          <view class="stage-progress-fill" :style="{ width: stageProgress + '%' }"></view>
        </view>
        <text class="stage-progress-text">{{ stageProgress }}%</text>
      </view>

      <view class="profile-metrics">
        <view class="profile-metric">
          <text>{{ stats.totalSessions }}</text>
          <text>训练</text>
        </view>
        <view class="profile-metric">
          <text>{{ stats.totalSets }}</text>
          <text>总组数</text>
        </view>
        <view class="profile-metric">
          <text>{{ stats.peakVolumeKg }}</text>
          <text>峰值kg</text>
        </view>
      </view>
    </view>

    <view class="error-banner" v-if="loadError">
      <text class="error-text">{{ loadError }}</text>
      <text class="error-action" @tap="loadProfileData">重试</text>
    </view>

    <!-- 设备绑定区域 -->
    <view class="section-card">
      <view class="section-header">
        <text class="section-title">我的设备</text>
        <view class="add-device-btn" @tap="goDeviceBinding">
          <text>+ 添加设备</text>
        </view>
      </view>

      <!-- 设备列表 -->
      <view v-if="devices.length > 0" class="device-list">
        <view
          v-for="(device, idx) in devices"
          :key="idx"
          class="device-item"
        >
          <view class="device-icon">IMU</view>
          <view class="device-info">
            <text class="device-name">{{ device.deviceName || '未知设备' }}</text>
            <text class="device-code">{{ device.deviceCode }}</text>
            <text class="device-status" :class="device.status">
              {{ device.status === 'online' ? '● 在线' : '○ 离线' }}
            </text>
          </view>
          <view class="device-actions">
            <view class="unbind-btn" @tap.stop="onUnbindDevice(device)">
              <text>解绑</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 无设备空状态 -->
      <view v-else class="no-device" @tap="goDeviceBinding">
        <text class="no-device-icon">PAIR</text>
        <text class="no-device-text">暂未绑定设备</text>
        <text class="no-device-hint">{{ loadError ? '设备暂未同步，可重试或直接添加' : '点击添加设备开始训练' }}</text>
      </view>
    </view>

    <!-- 用户数据摘要 -->
    <view class="stats-card">
      <text class="section-title">训练数据</text>
      <view class="stats-grid">
        <view class="stat-box">
          <text class="stat-val">{{ stats.totalSessions }}</text>
          <text class="stat-lbl">总训练次数</text>
        </view>
        <view class="stat-box">
          <text class="stat-val">{{ stats.totalSets }}</text>
          <text class="stat-lbl">总组数</text>
        </view>
        <view class="stat-box">
          <text class="stat-val">{{ stats.totalDurationMin }}</text>
          <text class="stat-lbl">总训练时长(分钟)</text>
        </view>
        <view class="stat-box">
          <text class="stat-val">{{ stats.peakVolumeKg }}</text>
          <text class="stat-lbl">历史最高重量(kg)</text>
        </view>
      </view>
    </view>

    <!-- 训练阶段 -->
    <view class="stage-card">
      <text class="section-title">当前阶段</text>
      <view class="stage-timeline">
        <view
          v-for="(stage, idx) in stageTimeline"
          :key="idx"
          class="stage-node"
          :class="{ active: stage.isActive, done: stage.isDone }"
        >
          <view class="stage-dot"></view>
          <text class="stage-name">{{ stage.name }}</text>
          <text class="stage-desc">{{ stage.desc }}</text>
        </view>
      </view>
    </view>

    <!-- 菜单列表 -->
    <view class="menu-card">
      <view class="menu-item" @tap="onMenuItem('settings')">
        <text class="menu-icon">SET</text>
        <text class="menu-label">设置</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="onMenuItem('privacy')">
        <text class="menu-icon">SEC</text>
        <text class="menu-label">隐私政策</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="onMenuItem('about')">
        <text class="menu-icon">INFO</text>
        <text class="menu-label">关于我们</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="logout-btn" @tap="onLogout">
      <text>退出登录</text>
    </view>
  </view>
</template>

<script lang="ts">
import { defineComponent, ref, computed, onMounted } from 'vue';
import { useDidShow } from '@tarojs/taro';
import { getCurrentUser, getMyDevices, getMyTrainingStats, unbindDevice } from '../../services/api';
import { enterDemoMode } from '../../config/env';

export default defineComponent({
  setup() {
    const userInfo = ref(null);
    const devices = ref([]);
    const loadError = ref('');
    const stats = ref({ totalSessions: 0, totalSets: 0, totalDurationMin: 0, peakVolumeKg: 0 });
    const stageDefs = [
      { key: 'beginner', name: '适应期', desc: '建立训练习惯' },
      { key: 'growth', name: '进步期', desc: '提升基础力量' },
      { key: 'plateau', name: '强化期', desc: '突破重量瓶颈' },
      { key: 'advanced', name: '巩固期', desc: '稳定高水平' },
    ];

    onMounted(async () => {
      await loadProfileData();
    });

    // 修复：Tab页切换时 useDidShow 会触发，重新加载数据
    useDidShow(() => {
      console.log('[Profile] useDidShow triggered, checking login...');
      const token = wx.getStorageSync('auth_token');
      if (!token) {
        wx.reLaunch({ url: '/pages/login/index' });
        return;
      }
      // 刷新用户数据和设备列表
      loadProfileData().catch(e => console.warn('[Profile] refresh data failed', e));
    });

    async function loadProfileData() {
      loadError.value = '';
      try {
        const [userRes, devRes] = await Promise.all([
          getCurrentUser().catch((error) => ({ data: null, error })),
          getMyDevices().catch((error) => ({ data: [], error })),
        ]);
        const failedParts = [
          userRes.error ? '用户信息' : '',
          devRes.error ? '设备列表' : '',
        ].filter(Boolean);
        if (failedParts.length) {
          loadError.value = `后台连接异常，${failedParts.join('、')}暂未同步`;
        }
        userInfo.value = userRes.data || getEmptyUser();
        const localAvatar = wx.getStorageSync('avatar_url');
        if (localAvatar) {
          userInfo.value.avatar = localAvatar;
        }
        const localNickname = wx.getStorageSync('nickname');
        if (localNickname && localNickname !== '微信用户' && localNickname !== '用户') {
          userInfo.value.nickname = localNickname;
        }
        // 确保 devices 是数组
        if (Array.isArray(devRes.data)) {
          devices.value = devRes.data;
        } else if (devRes.data && typeof devRes.data === 'object') {
          devices.value = [devRes.data];
        } else {
          devices.value = [];
        }
        try {
          const statRes = await getMyTrainingStats();
          stats.value = normalizeStats(statRes.data);
        } catch (statsError) {
          console.warn('[Profile] getMyTrainingStats failed', statsError);
          stats.value = { totalSessions: 0, totalSets: 0, totalDurationMin: 0, peakVolumeKg: 0 };
          loadError.value = loadError.value || '后台连接异常，训练数据暂未同步';
        }
      } catch (e) {
        console.warn('[Profile] Load failed', e);
        userInfo.value = userInfo.value || getEmptyUser();
        devices.value = [];
        stats.value = { totalSessions: 0, totalSets: 0, totalDurationMin: 0, peakVolumeKg: 0 };
        loadError.value = '后台连接异常，个人数据暂未同步';
      }
    }

    function getEmptyUser() {
      return {
        userId: 0,
        nickname: '',
        avatar: '',
        level: 1,
        stage: 'beginner',
        streakDays: 0,
      };
    }

    function normalizeStats(raw) {
      const numberValue = (value) => {
        const n = Number(value);
        return Number.isFinite(n) ? n : 0;
      };
      return {
        totalSessions: numberValue(raw?.totalSessions),
        totalSets: numberValue(raw?.totalSets),
        totalDurationMin: numberValue(raw?.totalDurationMin),
        peakVolumeKg: Math.round(numberValue(raw?.peakVolumeKg) * 10) / 10,
      };
    }

    const stageLabel = computed(() => {
      const stageMap = {
        beginner: '适应期',
        growth: '进步期',
        plateau: '强化期',
        advanced: '巩固期',
      };
      return stageMap[userInfo.value?.stage] || userInfo.value?.stageLabel || '适应期';
    });
    const stageProgress = computed(() => {
      const level = userInfo.value?.level || 1;
      return Math.min(100, level * 20);
    });

    const displayNickname = computed(() => {
      const name = String(userInfo.value?.nickname || '').trim();
      if (!name || name === '微信用户' || name === '用户') {
        return '';
      }
      return name;
    });

    const stageTimeline = computed(() => {
      const currentKey = userInfo.value?.stage || 'beginner';
      const activeIndex = Math.max(0, stageDefs.findIndex(s => s.key === currentKey));
      return stageDefs.map((stage, idx) => ({
        ...stage,
        isDone: idx < activeIndex,
        isActive: idx === activeIndex,
      }));
    });

    function goDeviceBinding() {
      console.log('[Profile] goDeviceBinding tapped');
      wx.navigateTo({
        url: '/pages/device-binding/index',
        fail: (err) => {
          console.error('[Profile] navigate device-binding failed', err);
          wx.showToast({ title: '打开器械训练失败', icon: 'none' });
        },
      });
    }

    async function onUnbindDevice(device) {
      console.log('[Profile] unbind tapped', device);
      if (!device.bindingId) {
        wx.showToast({ title: '该设备暂无绑定记录', icon: 'none' });
        return;
      }

      const confirm = await new Promise((resolve) => {
        wx.showModal({
          title: '确认解绑',
          content: `确定要解绑 ${device.deviceName} 吗？`,
          success: (res) => resolve(res.confirm),
        });
      });
      if (!confirm) return;

      try {
        await unbindDevice(device.bindingId);
        devices.value = devices.value.filter(d => d.bindingId !== device.bindingId);
        wx.showToast({ title: '已解绑', icon: 'success' });
      } catch (e) {
        console.error('[Profile] Unbind failed', e);
        wx.showToast({ title: '解绑失败', icon: 'error' });
      }
    }

    async function onAvatarClick() {
      console.log('[Profile] avatar tapped');
      try {
        const choose = wx.chooseMedia || wx.chooseImage;
        if (!choose) {
          wx.showToast({ title: '当前环境不支持选择头像', icon: 'none' });
          return;
        }

        const res = await new Promise((resolve, reject) => {
          if (wx.chooseMedia) {
            wx.chooseMedia({
              count: 1,
              mediaType: ['image'],
              sourceType: ['album', 'camera'],
              success: resolve,
              fail: reject,
            });
          } else {
            wx.chooseImage({
              count: 1,
              sourceType: ['album', 'camera'],
              success: resolve,
              fail: reject,
            });
          }
        });

        const avatarUrl = res.tempFiles?.[0]?.tempFilePath || res.tempFilePaths?.[0];
        if (!avatarUrl) return;

        wx.setStorageSync('avatar_url', avatarUrl);
        userInfo.value = {
          ...(userInfo.value || getEmptyUser()),
          avatar: avatarUrl,
        };
        wx.showToast({ title: '头像已更新', icon: 'success' });
      } catch (e) {
        if (e?.errMsg && e.errMsg.includes('cancel')) return;
        console.error('[Profile] choose avatar failed', e);
        wx.showToast({ title: '头像选择失败', icon: 'none' });
      }
    }

    function onMenuItem(item) {
      console.log('[Profile] menu tapped', item);
      if (item === 'settings') {
        wx.showActionSheet({
          itemList: ['清除登录状态', '切换演示模式'],
          success: (res) => {
            if (res.tapIndex === 0) {
              onLogout();
            }
            if (res.tapIndex === 1) {
              enterDemoMode();
              wx.showToast({ title: '已切换演示模式', icon: 'success' });
              setTimeout(() => {
                wx.switchTab({ url: '/pages/home/index' });
              }, 500);
            }
          },
        });
      } else if (item === 'privacy') {
        wx.navigateTo({
          url: '/pages/privacy/index',
          fail: (err) => {
            console.error('[Profile] navigate privacy failed', err);
            wx.showToast({ title: '打开隐私政策失败', icon: 'none' });
          },
        });
      } else if (item === 'about') {
        wx.navigateTo({
          url: '/pages/user-agreement/index',
          fail: (err) => {
            console.error('[Profile] navigate agreement failed', err);
            wx.showToast({ title: '打开关于页面失败', icon: 'none' });
          },
        });
      }
    }

    function onLogout() {
      console.log('[Profile] logout tapped');
      wx.showModal({
        title: '确认退出',
        content: '确定要退出登录吗？',
        success: (res) => {
          if (res.confirm) {
            wx.removeStorageSync('auth_token');
            wx.removeStorageSync('login_type');
            wx.removeStorageSync('demo_mode');
            wx.removeStorageSync('user_id');
            wx.removeStorageSync('user_age');
            wx.removeStorageSync('user_gender');
            wx.removeStorageSync('device_type');
            wx.removeStorageSync('sessions_30d');
            wx.removeStorageSync('resting_hr');
            wx.removeStorageSync('avatar_url');
            wx.reLaunch({ url: '/pages/login/index' });
          }
        },
      });
    }

    return {
      userInfo,
      devices,
      loadError,
      stats,
      stageLabel,
      stageProgress,
      displayNickname,
      stageTimeline,
      goDeviceBinding,
      loadProfileData,
      onUnbindDevice,
      onAvatarClick,
      onMenuItem,
      onLogout,
    };
  },
});
</script>

<style>
.profile-page {
  padding: 28rpx 24rpx 56rpx;
  background: #edf2f7;
  min-height: 100vh;
  box-sizing: border-box;
}
.profile-card {
  background: linear-gradient(145deg, #101828 0%, #1f2a44 58%, #123f3a 100%);
  border-radius: 28rpx;
  padding: 32rpx 30rpx 30rpx;
  margin-bottom: 24rpx;
  color: #fff;
  box-shadow: 0 24rpx 48rpx rgba(16, 24, 40, 0.2);
}
.profile-top {
  display: flex;
  align-items: center;
  margin-bottom: 28rpx;
}
.avatar-wrap {
  position: relative;
  margin-right: 28rpx;
}
.avatar-img {
  width: 112rpx;
  height: 112rpx;
  border-radius: 50%;
  background: rgba(255,255,255,0.3);
  border: 4rpx solid rgba(255,255,255,0.36);
  overflow: hidden;
}
.avatar-open-data {
  display: block;
}
.avatar-edit-icon {
  position: absolute;
  bottom: -4rpx;
  right: -8rpx;
  min-width: 60rpx;
  height: 34rpx;
  padding: 0 10rpx;
  background: #fff;
  border-radius: 999rpx;
  font-size: 16rpx;
  font-weight: 900;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #101828;
}
.user-info { flex: 1; }
.eyebrow {
  display: block;
  color: rgba(255,255,255,0.58);
  font-size: 20rpx;
  font-weight: 900;
  margin-bottom: 8rpx;
}
.nickname {
  font-size: 40rpx;
  font-weight: 900;
  color: #fff;
  display: block;
  margin-bottom: 8rpx;
}
.user-stage {
  font-size: 26rpx;
  color: rgba(255,255,255,0.72);
  display: block;
}
.stage-progress-wrap {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 24rpx;
}
.stage-progress-bar {
  flex: 1;
  height: 14rpx;
  background: rgba(255,255,255,0.14);
  border-radius: 999rpx;
  overflow: hidden;
}
.stage-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #31d4a0, #63e6be);
  border-radius: 999rpx;
  transition: width 0.3s;
}
.stage-progress-text {
  font-size: 22rpx;
  color: rgba(255,255,255,0.72);
  font-weight: 800;
}
.profile-metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14rpx;
}
.profile-metric {
  min-height: 112rpx;
  border-radius: 20rpx;
  padding: 18rpx 10rpx;
  background: rgba(255,255,255,0.1);
  border: 1rpx solid rgba(255,255,255,0.12);
  text-align: center;
}
.profile-metric text:first-child {
  display: block;
  color: #fff;
  font-size: 34rpx;
  font-weight: 900;
  line-height: 1.15;
}
.profile-metric text:last-child {
  display: block;
  margin-top: 8rpx;
  color: rgba(255,255,255,0.58);
  font-size: 22rpx;
}
.error-banner {
  background: #fff7e6;
  border: 1rpx solid #ffd591;
  border-radius: 14rpx;
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
  font-weight: 700;
}
.section-card,
.stats-card,
.stage-card,
.menu-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
  border: 1rpx solid rgba(222, 228, 236, 0.9);
  box-shadow: 0 14rpx 34rpx rgba(20, 38, 70, 0.06);
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}
.section-title {
  font-size: 30rpx;
  font-weight: 900;
  color: #101828;
}
.add-device-btn {
  background: #101828;
  color: #fff;
  font-size: 24rpx;
  font-weight: 800;
  padding: 10rpx 20rpx;
  border-radius: 999rpx;
}
.device-item {
  display: flex;
  align-items: center;
  padding: 22rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}
.device-item:last-child { border-bottom: none; }
.device-icon {
  width: 68rpx;
  height: 68rpx;
  border-radius: 20rpx;
  background: #101828;
  color: #31d4a0;
  font-size: 20rpx;
  font-weight: 900;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 18rpx;
  flex-shrink: 0;
}
.device-info { flex: 1; }
.device-name {
  font-size: 30rpx;
  font-weight: 900;
  color: #101828;
  display: block;
}
.device-code {
  font-size: 24rpx;
  color: #999;
  display: block;
}
.device-status { font-size: 24rpx; }
.device-status.online { color: #0f9f7a; }
.device-status.offline { color: #999; }
.unbind-btn {
  border: 1rpx solid #fecdca;
  color: #b42318;
  font-size: 24rpx;
  font-weight: 800;
  padding: 8rpx 18rpx;
  border-radius: 999rpx;
}
.no-device {
  text-align: center;
  padding: 40rpx 20rpx;
}
.no-device-icon {
  width: 104rpx;
  height: 104rpx;
  border-radius: 28rpx;
  background: #101828;
  color: #31d4a0;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 18rpx;
  font-size: 22rpx;
  font-weight: 900;
}
.no-device-text { font-size: 30rpx; color: #101828; font-weight: 900; display: block; margin-bottom: 8rpx; }
.no-device-hint { font-size: 24rpx; color: #999; }
.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20rpx;
  margin-top: 8rpx;
}
.stat-box {
  background: #f5f8fb;
  border-radius: 20rpx;
  padding: 20rpx;
  text-align: center;
}
.stat-val {
  font-size: 40rpx;
  font-weight: 900;
  color: #101828;
  display: block;
  margin-bottom: 6rpx;
}
.stat-lbl { font-size: 22rpx; color: #999; }
.stage-timeline {
  display: flex;
  justify-content: space-between;
  position: relative;
  margin-top: 8rpx;
}
.stage-timeline::before {
  content: '';
  position: absolute;
  top: 16rpx;
  left: 10%;
  right: 10%;
  height: 4rpx;
  background: #e0e0e0;
}
.stage-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  position: relative;
  z-index: 1;
}
.stage-dot {
  width: 32rpx;
  height: 32rpx;
  border-radius: 50%;
  background: #e0e0e0;
  margin-bottom: 10rpx;
}
.stage-node.done .stage-dot { background: #52c41a; }
.stage-node.active .stage-dot { background: #4A90E2; box-shadow: 0 0 0 6rpx rgba(74,144,226,0.2); }
.stage-name {
  font-size: 24rpx;
  font-weight: 600;
  color: #999;
  display: block;
}
.stage-node.active .stage-name { color: #101828; }
.stage-node.done .stage-name { color: #0f9f7a; }
.stage-desc {
  font-size: 20rpx;
  color: #999;
  text-align: center;
  margin-top: 4rpx;
}
.menu-item {
  display: flex;
  align-items: center;
  padding: 28rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}
.menu-item:last-child { border-bottom: none; }
.menu-icon {
  min-width: 70rpx;
  height: 38rpx;
  border-radius: 999rpx;
  background: #f2f5f8;
  color: #475467;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18rpx;
  font-weight: 900;
  margin-right: 16rpx;
}
.menu-label { flex: 1; font-size: 30rpx; color: #101828; font-weight: 800; }
.menu-arrow { font-size: 36rpx; color: #ccc; }
.logout-btn {
  background: #fff;
  border-radius: 24rpx;
  padding: 32rpx;
  text-align: center;
  color: #b42318;
  font-size: 30rpx;
  font-weight: 800;
  margin-bottom: 40rpx;
  border: 1rpx solid rgba(222, 228, 236, 0.9);
}
</style>
