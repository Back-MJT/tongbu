<!--
  登录页面 - 昕动智能小程序
  功能: 微信一键登录
-->
<template>
  <view class="login-page">
    <!-- 品牌区域 -->
    <view class="brand-section">
      <view class="logo-wrap">
        <text class="logo-icon">XD</text>
      </view>
      <text class="brand-name">昕动健康</text>
      <text class="brand-slogan">今日训练已经就绪</text>
    </view>

    <!-- 微信一键登录 -->
    <view class="form-section">
      <view class="wx-login-btn" @tap="onWxLogin">
        <text class="wx-icon">微信</text>
        <text class="wx-text">微信一键登录</text>
      </view>

      <!-- 演示模式登录 -->
      <view class="demo-login-btn" @tap="onDemoLogin">
        <text class="demo-icon">TRY</text>
        <text class="demo-text">产品体验模式</text>
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

const TAB_PAGES = [
  '/pages/home/index',
  '/pages/daily-task/index',
  '/pages/progress/index',
  '/pages/profile/index',
];

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

    async function requestWechatProfile() {
      if (!wx.getUserProfile) {
        return {};
      }

      try {
        const profileRes: any = await new Promise((resolve, reject) => {
          wx.getUserProfile({
            desc: '用于展示会员头像昵称',
            success: resolve,
            fail: reject,
          });
        });
        const userInfo = profileRes?.userInfo || {};
        const nickname = String(userInfo.nickName || '').trim();
        const avatar = String(userInfo.avatarUrl || '').trim();
        const hasRealNickname = nickname && nickname !== '微信用户' && nickname !== '用户';
        return {
          ...(hasRealNickname ? { nickname } : {}),
          ...(avatar ? { avatar } : {}),
        };
      } catch (e) {
        console.warn('[Login] getUserProfile unavailable or cancelled, fallback to wx.login only', e);
        return {};
      }
    }

    function redirectAfterLogin() {
      const target = wx.getStorageSync('post_login_redirect');
      wx.removeStorageSync('post_login_redirect');
      if (!target || String(target).includes('/pages/login/index')) {
        wx.switchTab({ url: '/pages/home/index' });
        return;
      }

      const url = String(target).startsWith('/') ? String(target) : `/${target}`;
      const path = url.split('?')[0];
      if (TAB_PAGES.includes(path)) {
        wx.switchTab({ url: path });
        return;
      }
      wx.redirectTo({
        url,
        fail: (err) => {
          console.warn('[Login] redirect after login failed:', err);
          wx.switchTab({ url: '/pages/home/index' });
        },
      });
    }

    async function onWxLogin() {
      loading.value = true;
      try {
        clearDemoMode();
        wx.removeStorageSync('login_type');
        wx.removeStorageSync('auth_token');
        wx.removeStorageSync('wechat_openid');
        wx.removeStorageSync('wechat_openid_masked');
        wx.removeStorageSync('wechat_unionid');
        wx.removeStorageSync('nickname');
        wx.removeStorageSync('avatar_url');
        const profile = await requestWechatProfile();
        const loginRes = await new Promise((resolve, reject) => {
          wx.login({
            success: resolve,
            fail: reject,
          });
        });

        const code = loginRes.code;
        console.log('[Login] wx.login code:', code);

        const res = await wxLogin(code, profile);
        if (res.code === 200 || res.code === 0) {
          const token = res.data?.token;
          const userId = res.data?.userId;
          const isDemo = res.data?.isDemo;
          const wechatOpenId = res.data?.wechatOpenId || res.data?.openId;
          const wechatOpenIdMasked = res.data?.wechatOpenIdMasked || res.data?.openid;
          const wechatUnionId = res.data?.wechatUnionId || res.data?.unionId;
          console.log('[Login] wxLogin success: token=', token ? 'exists' : 'missing', 'userId=', userId, 'isDemo=', isDemo, 'wechatOpenId=', wechatOpenIdMasked || (wechatOpenId ? 'exists' : 'missing'));
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
          const nickname = res.data?.nickname || profile.nickname;
          const avatar = res.data?.avatar || profile.avatar;
          if (nickname && nickname !== '微信用户' && nickname !== '用户') {
            wx.setStorageSync('nickname', String(nickname));
          } else {
            wx.removeStorageSync('nickname');
          }
          if (avatar) {
            wx.setStorageSync('avatar_url', String(avatar));
          } else {
            wx.removeStorageSync('avatar_url');
          }
          if (wechatOpenId) {
            wx.setStorageSync('wechat_openid', String(wechatOpenId));
          }
          if (wechatOpenIdMasked) {
            wx.setStorageSync('wechat_openid_masked', String(wechatOpenIdMasked));
          }
          if (wechatUnionId) {
            wx.setStorageSync('wechat_unionid', String(wechatUnionId));
          }
          console.log('[Login] storage after login: login_type=', wx.getStorageSync('login_type'), 'demo_mode=', wx.getStorageSync('demo_mode'));
          redirectAfterLogin();
        } else {
          wx.showToast({ title: res.msg || '登录失败', icon: 'none' });
        }
      } catch (e) {
        console.error('[Login] wx login failed', e);
        const message = e?.message || e?.errMsg || '登录失败，请重试';
        wx.showToast({ title: message.slice(0, 18), icon: 'none', duration: 3000 });
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

.login-page {
  min-height: 100vh;
  background: linear-gradient(155deg, #0f172a 0%, #172033 52%, #0f3f3d 100%);
  padding: 0 48rpx;
  box-sizing: border-box;
}

.brand-section {
  width: 100%;
  margin-top: 188rpx;
  margin-bottom: 112rpx;
  text-align: left;
}

.logo-wrap {
  width: 132rpx;
  height: 132rpx;
  border-radius: 30rpx;
  background: rgba(255, 255, 255, 0.1);
  border: 1rpx solid rgba(255, 255, 255, 0.14);
  box-shadow: 0 18rpx 50rpx rgba(0, 0, 0, 0.22);
  margin: 0 0 32rpx;
}

.logo-icon {
  font-size: 34rpx;
  font-weight: 900;
  color: #fff;
  letter-spacing: 0;
}

.brand-name {
  font-size: 58rpx;
  line-height: 1.1;
  font-weight: 900;
  color: #fff;
}

.brand-slogan {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.64);
  margin-top: 14rpx;
  display: block;
}

.form-section {
  width: 100%;
}

.wx-login-btn {
  height: 98rpx;
  background: #19bf73;
  border-radius: 999rpx;
  box-shadow: 0 16rpx 34rpx rgba(25, 191, 115, 0.26);
}

.wx-icon {
  font-size: 26rpx;
  font-weight: 800;
  margin-right: 16rpx;
}

.wx-text {
  font-size: 30rpx;
  font-weight: 800;
}

.demo-login-btn {
  height: 92rpx;
  background: rgba(255, 255, 255, 0.08);
  border: 1rpx solid rgba(255, 255, 255, 0.14);
  border-radius: 999rpx;
  margin-top: 22rpx;
}

.demo-icon {
  min-width: 58rpx;
  height: 34rpx;
  padding: 0 8rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.76);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18rpx;
  font-weight: 900;
}

.demo-text {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.72);
}

.agreement {
  bottom: 54rpx;
}

.agreement-text {
  color: rgba(255, 255, 255, 0.42);
}

.agreement-text .link {
  color: #9ee6ce;
}

.loading-spinner {
  border-top-color: #9ee6ce;
}
</style>
