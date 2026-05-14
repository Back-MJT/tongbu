/**
 * 环境配置 - 昕动智能小程序
 *
 * 根据构建环境自动切换API地址:
 * - 开发: 自动写入电脑局域网IP，便于微信真机调试访问本机后端
 * - 演示: 部署服务器IP
 * - 生产: 正式域名
 */

import { localDevConfig } from './local-dev';

type AppEnv = 'development' | 'staging' | 'production';

function resolveRuntimeEnv(): AppEnv {
  try {
    const envVersion = wx.getAccountInfoSync?.().miniProgram?.envVersion;
    if (envVersion === 'trial') return 'staging';
    if (envVersion === 'release') return 'production';
  } catch {}
  return 'development';
}

const ENV = resolveRuntimeEnv();

interface EnvConfig {
  apiBase: string;      // RuoYi后端统一入口
  ieBase: string;       // 兼容别名，默认与 apiBase 保持一致
  wsUrl: string;        // WebSocket (未来BLE数据流)
  demoMode: boolean;    // 演示模式开关
  version: string;      // 版本号
  logLevel: 'debug' | 'info' | 'warn' | 'error';
}

const configs: Record<string, EnvConfig> = {
  development: {
    apiBase: 'https://api.black-mjt.cn',
    ieBase: 'https://api.black-mjt.cn',
    wsUrl: 'wss://api.black-mjt.cn/ws',
    demoMode: false,
    version: '1.0.0-dev',
    logLevel: 'debug',
  },
  staging: {
    apiBase: 'https://api.black-mjt.cn',
    ieBase: 'https://api.black-mjt.cn',
    wsUrl: 'wss://api.black-mjt.cn/ws',
    demoMode: false,
    version: '1.0.0-rc',
    logLevel: 'info',
  },
  production: {
    // 生产环境使用微信小程序合法域名
    // 需在微信公众平台配置: request合法域名
    apiBase: 'https://api.black-mjt.cn',
    ieBase: 'https://api.black-mjt.cn',
    wsUrl: 'wss://api.black-mjt.cn/ws',
    demoMode: false,
    version: '1.0.0',
    logLevel: 'warn',
  },
};

// 允许通过本地存储覆盖 (方便BD演示)
function getOverrideConfig(): Partial<EnvConfig> | null {
  try {
    const override = wx.getStorageSync('env_override');
    if (override && typeof override === 'object') {
      return override as Partial<EnvConfig>;
    }
  } catch {}
  return null;
}

function getConfig(): EnvConfig {
  const base = configs[ENV] || configs.development;
  const override = getOverrideConfig();

  if (override) {
    return { ...base, ...override };
  }

  return base;
}

export const config = getConfig();
export const isDemoMode = config.demoMode;
export const APP_VERSION = config.version;

/**
 * 日志工具
 */
export function log(level: string, ...args: any[]) {
  const levels = ['debug', 'info', 'warn', 'error'];
  const currentLevel = levels.indexOf(config.logLevel);
  const msgLevel = levels.indexOf(level);

  if (msgLevel >= currentLevel) {
    const prefix = `[Xindong v${config.version}]`;
    if (level === 'error') {
      console.error(prefix, ...args);
    } else if (level === 'warn') {
      console.warn(prefix, ...args);
    } else {
      console.log(prefix, ...args);
    }
  }
}

/**
 * 设置API地址覆盖 (BD演示用)
 * 默认小程序所有 HTTP 请求都走 RuoYi 后端统一入口。
 */
export function setApiOverride(apiBase: string, ieBase?: string) {
  const override: Record<string, string> = {
    apiBase,
    ieBase: ieBase || apiBase,
  };
  wx.setStorageSync('env_override', override);
  log('info', '[Config] API override set:', override);
}

/**
 * Demo模式用户档案数据 (登录演示账号时写入storage)
 */
export const DEMO_USER_PROFILE = {
  user_id: 'demo-user',
  nickname: '张先生',
  age: 35,
  gender: 'male',
  device_type: 'strength_station',
  sessions_30d: 12,
  resting_hr: 68,
  hypertension: false,
};

const DEMO_TOKEN_PREFIX = 'mp_demo_';

/**
 * 进入演示模式：设置demo token + 用户档案
 */
export function enterDemoMode() {
  clearDemoMode();
  const demoToken = `${DEMO_TOKEN_PREFIX}${Date.now()}_${Math.random().toString(36).slice(2)}`;
  wx.setStorageSync('auth_token', demoToken);
  wx.setStorageSync('login_type', 'demo');
  wx.setStorageSync('demo_mode', true);
  // 写入用户档案，供AE处方API使用
  wx.setStorageSync('user_id', DEMO_USER_PROFILE.user_id);
  wx.setStorageSync('user_age', DEMO_USER_PROFILE.age);
  wx.setStorageSync('user_gender', DEMO_USER_PROFILE.gender);
  wx.setStorageSync('device_type', DEMO_USER_PROFILE.device_type);
  wx.setStorageSync('sessions_30d', DEMO_USER_PROFILE.sessions_30d);
  wx.setStorageSync('resting_hr', DEMO_USER_PROFILE.resting_hr);
  log('info', '[Config] Demo mode entered with user profile:', DEMO_USER_PROFILE);
}

/**
 * 退出演示模式：微信真实登录前后都要清理，避免 demo 数据污染真实账号。
 */
export function clearDemoMode() {
  [
    'demo_mode',
    'demo_training_sessions',
    'user_age',
    'user_gender',
    'device_type',
    'sessions_30d',
    'resting_hr',
  ].forEach((key) => wx.removeStorageSync(key));
}

/**
 * 清除覆盖
 */
export function clearApiOverride() {
  wx.removeStorageSync('env_override');
  log('info', '[Config] API override cleared');
}
