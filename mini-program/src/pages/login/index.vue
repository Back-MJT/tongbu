<!--
  登录页面 - 昕动智能小程序
  功能: 微信一键登录
-->
<template>
  <view class="login-page">
    <!-- 品牌区域 -->
    <view class="brand-section">
      <view class="logo-wrap">
        <text class="logo-icon">💪</text>
      </view>
      <text class="brand-name">昕动健康</text>
      <text class="brand-slogan">智能运动 · 个性化训练方案</text>
    </view>

    <!-- 微信一键登录 -->
    <view class="form-section">
      <view class="wx-login-btn" @tap="onWxLogin">
        <text class="wx-icon">微信</text>
        <text class="wx-text">微信一键登录</text>
      </view>

      <!-- 演示模式登录 -->
      <view class="demo-login-btn" @tap="onDemoLogin">
        <text class="demo-icon">🔧</text>
        <text class="demo-text">厂家演示模式</text>
      </view>
    </view>

    <!-- 用户协议 -->
    <view class="agreement">
      <text class="agreement-text">
        登录即代表同意
        <text class="link" @tap="onAgreement('terms')">《用户协议》</text>
        和
        <text class="link" @tap="onAgreement('privacy')">《隐私政策》</text>
      </text>
    </view>

    <!-- 加载状态 -->
    <view v-if="loading" class="loading-mask">
      <view class="loading-spinner"></view>
      <text class="loading-text">登录中...</text>
    </view>
  </view>
</template>

<script lang="ts">
import { defineComponent, ref } from 'vue';
import { wxLogin } from '../../services/api';
import { clearDemoMode, enterDemoMode } from '../../config/env';

export default defineComponent({
  setup() {
    const loading = ref(false);

    async function onDemoLogin() {
      loading.value = true;
      try {
        console.log('[Login] Entering demo mode...');
        enterDemoMode();
        console.log('[Login] Demo mode storage set, now switching to home...');

        // 使用 wx.switchTab 跳转到首页
        wx.switchTab({
          url: '/pages/home/index',
          success: () => {
            console.log('[Login] switchTab success');
            wx.showToast({ title: '演示模式登录成功', icon: 'success', duration: 1500 });
          },
          fail: (err) => {
            console.error('[Login] switchTab failed:', err);
            wx.showToast({ title: '跳转失败，请重试', icon: 'none' });
          }
        });
      } catch (e) {
        console.error('[Login] Demo login failed', e);
        wx.showToast({ title: '演示模式登录失败', icon: 'none' });
      } finally {
        loading.value = false;
      }
    }

    async function onWxLogin() {
      loading.value = true;
      try {
        clearDemoMode();
        wx.removeStorageSync('login_type');
        wx.removeStorageSync('auth_token');
        const loginRes = await new Promise((resolve, reject) => {
          wx.login({
            success: resolve,
            fail: reject,
          });
        });

        const code = loginRes.code;
        console.log('[Login] wx.login code:', code);

        const res = await wxLogin(code);
        if (res.code === 200 || res.code === 0) {
          const token = res.data?.token;
          const userId = res.data?.userId;
          const isDemo = res.data?.isDemo;
          console.log('[Login] wxLogin success: token=', token ? 'exists' : 'missing', 'userId=', userId, 'isDemo=', isDemo);
          if (token) {
            // 完全清除演示模式状态
            clearDemoMode();
            wx.setStorageSync('auth_token', token);
            wx.setStorageSync('login_type', isDemo ? 'demo' : 'wx');
            if (isDemo) {
              wx.setStorageSync('demo_mode', true);
            } else {
              wx.removeStorageSync('demo_mode');
            }
          }
          if (userId) {
            wx.setStorageSync('user_id', String(userId));
          }
          console.log('[Login] storage after login: login_type=', wx.getStorageSync('login_type'), 'demo_mode=', wx.getStorageSync('demo_mode'));
          wx.switchTab({ url: '/pages/home/index' });
        } else {
          wx.showToast({ title: res.msg || '登录失败', icon: 'none' });
        }
      } catch (e) {
        console.error('[Login] wx login failed', e);
        wx.showToast({ title: '登录失败，请重试', icon: 'none' });
      } finally {
        loading.value = false;
      }
    }

    function onAgreement(type) {
      console.log('[Login] Open agreement:', type);
      if (type === 'privacy') {
        wx.navigateTo({ url: '/pages/privacy/index' });
      } else if (type === 'terms') {
        wx.navigateTo({ url: '/pages/user-agreement/index' });
      }
    }

    return {
      loading,
      onWxLogin,
      onDemoLogin,
      onAgreement,
    };
  },
});
</script>

<style>
.login-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
  padding: 0 48rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.brand-section {
  margin-top: 240rpx;
  text-align: center;
  margin-bottom: 120rpx;
}
.logo-wrap {
  width: 140rpx;
  height: 140rpx;
  background: linear-gradient(135deg, #4A90E2, #6BB5FF);
  border-radius: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 28rpx;
}
.logo-icon {
  font-size: 64rpx;
}
.brand-name {
  font-size: 48rpx;
  font-weight: bold;
  color: #fff;
  display: block;
  margin-bottom: 12rpx;
}
.brand-slogan {
  font-size: 28rpx;
  color: rgba(255,255,255,0.6);
}
.form-section {
  width: 100%;
}
.wx-login-btn {
  background: #07C160;
  border-radius: 48rpx;
  height: 96rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.wx-icon {
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  margin-right: 12rpx;
}
.wx-text {
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
}
.demo-login-btn {
  width: 100%;
  height: 96rpx;
  background: rgba(255, 255, 255, 0.1);
  border: 2rpx solid rgba(255, 255, 255, 0.3);
  border-radius: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 24rpx;
}
.demo-icon {
  font-size: 32rpx;
  margin-right: 12rpx;
}
.demo-text {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 400;
}
.agreement {
  position: absolute;
  bottom: 60rpx;
  left: 0;
  right: 0;
  text-align: center;
}
.agreement-text {
  font-size: 22rpx;
  color: rgba(255,255,255,0.3);
}
.agreement-text .link {
  color: #4A90E2;
}
.loading-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 999;
}
.loading-spinner {
  width: 60rpx;
  height: 60rpx;
  border: 4rpx solid rgba(255,255,255,0.3);
  border-top-color: #4A90E2;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
.loading-text {
  color: #fff;
  font-size: 28rpx;
  margin-top: 16rpx;
}
</style>
