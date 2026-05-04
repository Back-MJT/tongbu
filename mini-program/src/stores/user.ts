/**
 * User Store - 用户状态管理
 * Pinia store for global user state
 */
import { defineStore } from 'pinia';

export const useUserStore = defineStore('user', {
  state: () => ({
    token: '',
    userId: '',
    nickname: '',
    avatar: '',
    stage: 'beginner',
    stageLabel: '初学期',
    stageColor: '#4CAF50',
    streakDays: 0,
    complianceRate: 0,
    totalSessions: 0,
    isDemoMode: false,
    devices: [],
    renderedPlan: null,
    healthProfile: null,
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    stageMotivation: (state) => {
      const motivations = {
        beginner: '每一步都是开始',
        growth: '坚持带来改变',
        plateau: '突破就在前方',
        advanced: '超越昨天的自己',
      };
      return motivations[state.stage] || motivations.beginner;
    },
  },

  actions: {
    loadFromStorage() {
      try {
        this.token = wx.getStorageSync('auth_token') || '';
        this.userId = wx.getStorageSync('user_id') || '';
        this.nickname = wx.getStorageSync('nickname') || '';
        this.isDemoMode = wx.getStorageSync('demo_mode') === true;
      } catch (e) {
        console.warn('[Store] loadFromStorage failed', e);
      }
    },

    setLoginInfo(data: { token: string; userId: string; nickname?: string }) {
      this.token = data.token;
      this.userId = data.userId;
      this.nickname = data.nickname || '';
      wx.setStorageSync('auth_token', data.token);
      wx.setStorageSync('user_id', data.userId);
      if (data.nickname) {
        wx.setStorageSync('nickname', data.nickname);
      }
    },

    setUserProfile(profile: any) {
      this.nickname = profile.name || profile.nickname || this.nickname;
      this.avatar = profile.avatar || '';
      this.stage = profile.stage || 'beginner';
      this.stageLabel = profile.stage_label || '初学期';
      this.streakDays = profile.streak_days || 0;
      this.complianceRate = profile.compliance_rate || 0;
      this.totalSessions = profile.total_sessions || 0;
      this.healthProfile = profile;
    },

    setRenderedPlan(plan: any) {
      this.renderedPlan = plan;
      if (plan?.color_primary) {
        this.stageColor = plan.color_primary;
      }
      if (plan?.stage_label) {
        this.stageLabel = plan.stage_label;
      }
    },

    setDevices(devices: any[]) {
      this.devices = devices;
    },

    logout() {
      this.token = '';
      this.userId = '';
      this.nickname = '';
      this.stage = 'beginner';
      this.stageLabel = '初学期';
      this.streakDays = 0;
      this.complianceRate = 0;
      this.totalSessions = 0;
      this.isDemoMode = false;
      this.devices = [];
      this.renderedPlan = null;
      this.healthProfile = null;
      try {
        wx.removeStorageSync('auth_token');
        wx.removeStorageSync('user_id');
        wx.removeStorageSync('nickname');
        wx.removeStorageSync('demo_mode');
        wx.removeStorageSync('login_type');
      } catch (e) {
        console.warn('[Store] logout cleanup failed', e);
      }
    },
  },
});
