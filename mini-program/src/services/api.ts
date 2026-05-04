/**
 * HTTP API Service - RuoYi后端 + 干预引擎通信
 * 昕动智能 HealthHub 小程序
 */

import { config } from '../config/env';

const API_BASE = config.apiBase;
const IE_BASE = config.ieBase || config.apiBase;

/**
 * 检查是否为演示模式
 */
export function isDemoMode(): boolean {
  try {
    const loginType = wx.getStorageSync('login_type');
    const demoMode = wx.getStorageSync('demo_mode');
    console.log('[API] isDemoMode check: login_type =', loginType, ', demo_mode =', demoMode);
    return loginType === 'demo' && demoMode === true;
  } catch {
    return false;
  }
}

/**
 * 通用请求封装
 */
async function request<T = any>(
  baseUrl: string,
  path: string,
  options: {
    method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
    data?: any;
    header?: Record<string, string>;
  } = {}
): Promise<{ code: number; data: T; msg?: string }> {
  const { method = 'GET', data, header = {} } = options;

  const token = wx.getStorageSync('auth_token');
  if (token) {
    header['Authorization'] = `Bearer ${token}`;
  }
  header['Content-Type'] = header['Content-Type'] || 'application/json';

  try {
    const res = await new Promise<WechatMiniprogram.RequestSuccessCallbackResult>((resolve, reject) => {
      wx.request({
        url: `${baseUrl}${path}`,
        method,
        data,
        header,
        success: resolve,
        fail: reject,
      });
    });

    const responseData = (res.data || {}) as any;
    const result = {
      code: typeof responseData.code === 'number' ? responseData.code : res.statusCode,
      data: responseData.data ?? responseData,
      msg: responseData.msg || responseData.message,
    } as { code: number; data: T; msg?: string };

    // 只有在非演示模式下，401 才重定向到登录页
    if (res.statusCode === 401 || result.code === 401) {
      const loginType = wx.getStorageSync('login_type');
      console.log('[API] Got 401, login_type =', loginType);

      // 如果不是演示模式，才重定向到登录页
      if (loginType !== 'demo') {
        console.log('[API] Not demo mode, removing token and redirecting to login');
        wx.removeStorageSync('auth_token');
        wx.reLaunch({ url: '/pages/login/index' });
      } else {
        console.log('[API] Demo mode, ignoring 401');
      }
    }

    if (res.statusCode >= 400) {
      throw new Error(result.msg || `HTTP ${res.statusCode}`);
    }

    return result;
  } catch (err: any) {
    console.error(`[API] Request failed: ${path}`, err);
    throw new Error(err.message || '网络请求失败');
  }
}

// ─── 认证 ────────────────────────────────────────────────────────────────────

export async function wxLogin(code: string) {
  return request<{ token: string; userId: number; tenantId?: number; isDemo?: boolean }>(
    API_BASE,
    '/api/mini/auth/wechat-login',
    {
    method: 'POST',
    data: { code },
    }
  );
}

export async function phoneLogin(phone: string, verifyCode: string) {
  return request<{ token: string; userId: number }>(API_BASE, '/api/auth/phone-login', {
    method: 'POST',
    data: { phone, verifyCode },
  });
}

export async function getCurrentUser() {
  if (isDemoMode()) {
    return { code: 200, data: getDemoData('user') as UserInfo };
  }
  return request<UserInfo>(API_BASE, '/api/user/current');
}

// ─── 设备 ────────────────────────────────────────────────────────────────────

export async function getMyDevices() {
  if (isDemoMode()) {
    return { code: 200, data: getDemoData('devices') as DeviceInfo[] };
  }
  return request<DeviceInfo[]>(API_BASE, '/api/device/my');
}

export async function bindDevice(deviceCode: string, deviceName?: string) {
  return request(API_BASE, '/api/device/bind', {
    method: 'POST',
    data: { deviceCode, deviceName },
  });
}

export async function resolveEquipment(code: string) {
  const normalizedCode = normalizeScanCode(code);
  if (isDemoMode() && (normalizedCode === 'EQ-000001' || String(code || '').includes('EQ-000001'))) {
    return { code: 200, data: getDemoEquipmentResolve() as EquipmentResolveResult };
  }
  return request<EquipmentResolveResult>(API_BASE, '/api/mini/equipment/resolve', {
    method: 'GET',
    data: { code: normalizedCode },
  });
}

export function normalizeScanCode(rawCode: string): string {
  let value = String(rawCode || '').trim();
  try {
    value = decodeURIComponent(value);
  } catch {
    // Keep raw value when scanned content is not URL encoded.
  }

  const queryStart = value.indexOf('?');
  if (queryStart >= 0) {
    const query = value.slice(queryStart + 1).split('#')[0];
    const params = query.split('&');
    for (const param of params) {
      const [key, ...rest] = param.split('=');
      const normalizedKey = String(key || '').toLowerCase();
      if (['code', 'equipmentcode', 'equipment_code'].includes(normalizedKey)) {
        return rest.join('=').trim();
      }
    }
  }

  const directMatch = value.match(/(?:equipmentCode|equipment_code|code)=([^&#]+)/i);
  if (directMatch?.[1]) {
    return directMatch[1].trim();
  }

  return value.split('#')[0].trim();
}

export async function unbindDevice(bindingId: number) {
  return request(API_BASE, `/api/device/unbind/${bindingId}`, {
    method: 'DELETE',
  });
}

// ─── 训练数据 (RuoYi后端) ────────────────────────────────────────────────────

export async function getTodayProgress() {
  console.log('[API] getTodayProgress called');
  console.log('[API] isDemoMode():', isDemoMode());

  if (isDemoMode()) {
    return { code: 200, data: getDemoData('todayProgress') as TodayProgress };
  }

  try {
    console.log('[API] getTodayProgress: calling real API...');
    const res = await request<TodayProgress>(API_BASE, '/api/training/progress/today');
    console.log('[API] getTodayProgress response:', JSON.stringify(res).slice(0, 300));

    const normalized = normalizeTodayProgress(res.data);
    if (res.code === 200 && normalized) {
      console.log('[API] getTodayProgress: using real data');
      return { code: 200, data: normalized };
    }
    throw new Error(`今日进度接口返回格式异常: ${JSON.stringify(res).slice(0, 200)}`);
  } catch (e) {
    console.warn('[API] getTodayProgress: real API failed:', e);
    throw e;
  }
}

function pickNumber(source: any, keys: string[], fallback = 0): number {
  for (const key of keys) {
    const value = source?.[key];
    if (value === null || value === undefined || value === '') continue;
    const numberValue = Number(value);
    if (Number.isFinite(numberValue)) return numberValue;
  }
  return fallback;
}

function normalizeTodayProgress(raw: any): TodayProgress | null {
  const data = raw?.data && typeof raw.data === 'object' ? raw.data : raw;
  if (!data || typeof data !== 'object') return null;

  const completedSessions = pickNumber(data, ['completedSessions', 'completed_sessions', 'completed']);
  const plannedSessions = pickNumber(data, ['plannedSessions', 'planned_sessions', 'planned'], 0);
  const totalDurationMin = pickNumber(data, ['totalDurationMin', 'total_duration_min', 'totalDurationMinutes', 'totalDuration', 'durationMin']);
  const complianceRate = pickNumber(data, ['complianceRate', 'compliance_rate'], plannedSessions > 0 ? Math.round((completedSessions / plannedSessions) * 100) : 0);

  if (!Number.isFinite(completedSessions) || !Number.isFinite(plannedSessions)) {
    return null;
  }

  return {
    ...(data || {}),
    completedSessions,
    plannedSessions,
    totalDurationMin,
    complianceRate,
  } as TodayProgress;
}

export async function getTrainingHistory(params: { page: number; size: number }) {
  if (isDemoMode()) {
    const records = getDemoTrainingSessions();
    const start = Math.max(0, (params.page - 1) * params.size);
    return {
      code: 200,
      data: {
        total: records.length,
        records: records.slice(start, start + params.size),
      } as TrainingHistoryResp,
    };
  }
  return request<TrainingHistoryResp>(API_BASE, '/api/training/history', {
    method: 'GET',
    data: params,
  });
}

export async function getSessionDetail(sessionId: number) {
  return request<SessionDetail>(API_BASE, `/api/training/session/${sessionId}`);
}

export async function submitTrainingSession(payload: {
  equipmentCode?: string;
  deviceCode?: string;
  exerciseType?: string;
  taskId?: string | number;
  exerciseName?: string;
  completedSets: number;
  totalReps: number;
  durationMin: number;
  totalVolumeKg?: number;
  stage?: string;
  sets?: Array<{
    setNo: number;
    reps: number;
    durationSec: number;
    startedAt?: string;
    endedAt?: string;
  }>;
}) {
  if (isDemoMode()) {
    const sessionId = Date.now();
    const now = new Date();
    const record = {
      sessionId,
      exerciseName: payload.exerciseName || '演示器械训练',
      exerciseType: payload.exerciseType || 'strength',
      equipmentCode: payload.equipmentCode || 'EQ-000001',
      deviceCode: payload.deviceCode || 'HB-3412',
      completedSets: payload.completedSets,
      totalReps: payload.totalReps,
      totalVolumeKg: payload.totalVolumeKg || 0,
      durationMin: payload.durationMin,
      sessionDate: now.toISOString().split('T')[0],
      sessionTime: now.toISOString(),
    };
    saveDemoTrainingSession(record);
    return { code: 200, data: { sessionId, status: 'recorded' } };
  }
  return request<{ sessionId: number; status: string }>(API_BASE, '/api/training/session', {
    method: 'POST',
    data: payload,
  });
}

export async function getTrainingPlan(params: {
  deviceCode: string;
  exerciseType: string;
  recentSessions?: number;
}) {
  if (isDemoMode()) {
    return {
      code: 200,
      data: {
        planId: 1,
        title: '今日训练建议',
        description: getDemoMiniprogramPrescription().aiSuggestion,
        targetSets: 4,
        targetRepsMin: 8,
        targetRepsMax: 12,
        suggestedRestSec: 90,
        aiModel: 'XIN-AE-demo',
      } as TrainingPlan,
    };
  }
  return request<TrainingPlan>(API_BASE, '/api/training/plan', {
    method: 'POST',
    data: params,
  });
}

// ─── 干预引擎 (Intervention Engine) ──────────────────────────────────────────

export async function getHealthProfile(userId: string) {
  return request<HealthProfile>(IE_BASE, `/api/profiles/${userId}`);
}

export async function createHealthProfile(profile: Partial<HealthProfile>) {
  return request<HealthProfile>(IE_BASE, '/api/profiles', {
    method: 'POST',
    data: profile,
  });
}

export async function getExercisePrescription(params: {
  userId: string;
  goal?: string;
  exerciseType?: string;
}) {
  return request<ExercisePrescription>(IE_BASE, '/api/prescriptions/exercise', {
    method: 'POST',
    data: params,
  });
}

export async function getSleepPrescription(params: {
  userId: string;
  sleepIssue?: string;
}) {
  return request<any>(IE_BASE, '/api/prescriptions/sleep', {
    method: 'POST',
    data: params,
  });
}

export async function submitDeviceData(data: {
  deviceId: string;
  userId: string;
  metrics: Record<string, number>;
}) {
  return request(IE_BASE, '/api/integration/data', {
    method: 'POST',
    data,
  });
}

// ─── 训练方案 (干预引擎 - 基于AE算法) ────────────────────────────────────────

export async function getRenderedTrainingPlan(profile: {
  userId: string;
  name?: string;
  age?: number;
  gender?: string;
  goal?: string;
  sessionsLast30Days?: number;
  avgDurationMinutes?: number;
  sessionsLastWeek?: number;
  sessionsPreviousWeek?: number;
  deviceType?: string;
}) {
  if (isDemoMode()) {
    return { code: 200, data: getDemoRenderedPlan() };
  }
  // 使用干预引擎的处方API
  return request<RenderedPlan>(IE_BASE, '/api/prescriptions/exercise', {
    method: 'POST',
    data: {
      userId: profile.userId,
      goal: profile.goal || '增强体质',
      exerciseType: profile.deviceType || '综合训练',
    },
  });
}

export async function getUserStage(userId: string) {
  if (isDemoMode()) {
    return { code: 200, data: { stage: 'growth', label: '成长期', color: '#2196F3' } };
  }
  try {
    const profile = await getHealthProfile(userId);
    return { code: 200, data: profile.data?.stage || { stage: 'beginner', label: '初学期', color: '#4CAF50' } };
  } catch (e) {
    return { code: 200, data: { stage: 'beginner', label: '初学期', color: '#4CAF50' } };
  }
}

// ─── 小程序运动处方 (AE算法 XIN-120) ─────────────────────────────────────────

export interface MiniProgramTask {
  taskId: number;
  exerciseName: string;
  exerciseType: string;
  targetSets: number;
  targetReps: number;
  targetLoadKg: number;
  targetHr: number | null;
  intensityLabel: string;
  restSeconds: number;
  status: 'pending' | 'in_progress' | 'completed' | 'skipped';
  coachingTip: string;
}

export interface HealthTip {
  title: string;
  content: string;
  evidenceSource: string;
  category: string;
}

export interface TargetHrZone {
  low: number;
  high: number;
  label: string;
}

export interface MiniProgramPrescription {
  date: string;
  plannedSessions: number;
  completedSessions: number;
  tasks: MiniProgramTask[];
  aiSuggestion: string;
  coachingReasoning: string;
  exerciseGoal: string;
  exerciseGoalEn: string;
  userStage: string;
  targetHrZone: TargetHrZone | null;
  healthTips: HealthTip[];
  algorithmVersion: string;
  evidenceRefs: string[];
}

export interface DeviceExerciseTask {
  taskId: number;
  exerciseName: string;
  exerciseType: string;
  targetSets: number;
  targetReps: number;
  targetLoadKg: number;
  restSeconds: number;
  intensityLabel: string;
  coachingTip: string;
}

export interface DeviceTasksResponse {
  device_type: string;
  tasks: DeviceExerciseTask[];
  total_exercises: number;
  muscle_groups: string[];
}

export interface EquipmentResolveResult {
  equipmentId: number;
  equipmentCode: string;
  equipmentName: string;
  equipmentType: string;
  location?: string;
  deviceId?: number;
  deviceCode?: string;
  bluetoothName?: string;
  serviceUuid?: string;
  notifyCharUuid?: string;
  countingConfig?: {
    mainAxis: string;
    upThreshold: number;
    downThreshold: number;
    minIntervalMs: number;
    minRange: number;
    smoothingWindow: number;
  };
}

export async function getMiniprogramPrescription(params: {
  user_id: string;
  age: number;
  gender?: string;
  device_type?: string;
  resting_hr?: number;
  sessions_last_30_days?: number;
  hypertension?: boolean;
}): Promise<{ code: number; data: MiniProgramPrescription }> {
  console.log('[API] getMiniprogramPrescription called, params:', JSON.stringify(params));
  console.log('[API] isDemoMode():', isDemoMode());

  if (isDemoMode()) {
    return { code: 200, data: getDemoMiniprogramPrescription() };
  }

  try {
    console.log('[API] getMiniprogramPrescription: calling real API...');
    const res = await request(
      API_BASE, '/api/training/prescription', {
      method: 'POST',
      data: params,
    });
    console.log('[API] getMiniprogramPrescription response code:', res.code);
    console.log('[API] getMiniprogramPrescription response data keys:', res.data ? Object.keys(res.data) : 'null');

    // Direct prescription in res.data (fallback mode when IE unavailable)
    if (res.code === 200 && res.data && (res.data as any).tasks) {
      console.log('[API] getMiniprogramPrescription: returning prescription with tasks');
      return { code: 200, data: (res.data as any) as MiniProgramPrescription };
    }
    throw new Error('训练处方接口返回格式异常');
  } catch (e) {
    console.warn('[API] getMiniprogramPrescription: real API failed:', e);
    throw e;
  }
}

export async function getMiniprogramDeviceTasks(params: {
  user_id: string;
  age: number;
  device_type: string;
  gender?: string;
}): Promise<{ code: number; data: DeviceTasksResponse }> {
  if (isDemoMode()) {
    return { code: 200, data: getDemoDeviceTasks() };
  }
  // Route through RuoYi backend for auth + tenant + proxy to :4001
  const res = await request<{ success: boolean; data: DeviceTasksResponse }>(
    API_BASE, '/api/mini/device-tasks', {
    method: 'POST',
    data: params,
  });
  if (res.code === 200 && res.data?.data) {
    return { code: 200, data: res.data.data as DeviceTasksResponse };
  }
  throw new Error('Device tasks unavailable');
}

// ─── 演示数据 ─────────────────────────────────────────────────────────────────

function getDemoRenderedPlan(): RenderedPlan {
  return {
    plan_id: 'demo-plan-001',
    user_id: 'demo-user',
    stage_label: '成长期',
    stage_icon: 'trending-up',
    color_primary: '#2196F3',
    color_secondary: '#90CAF9',
    motivation: '坚持带来改变',
    recommendations: [
      {
        index: 1,
        title: '渐进式上肢力量',
        content: '本周建议重点训练胸背肌群。采用平板卧推4组×10次（40kg）+ 高位下拉4组×10次（35kg）的组合，组间休息60-90秒。',
        reason: '你的上肢力量数据稳定增长，已适应当前负荷，建议增重5%突破平台。',
      },
      {
        index: 2,
        title: '有氧耐力提升',
        content: '搭配2次有氧训练，每次30分钟中等强度（心率130-145bpm），推荐椭圆机或划船机。',
        reason: '近期训练数据显示你的恢复能力提升，可以承受更高的有氧训练量。',
      },
      {
        index: 3,
        title: '恢复与柔韧性',
        content: '训练日后增加10分钟动态拉伸和泡沫轴放松，重点关注肩袖和胸椎活动度。',
        reason: 'IMU数据显示你卧推时肩关节活动范围偏小，柔韧性训练可预防运动损伤。',
      },
    ],
    generated_at: new Date().toISOString(),
    status: 'generated',
    share_title: '我的本周训练计划',
    share_description: '成长期 · 3条专属建议',
  };
}

function getDemoMiniprogramPrescription(): MiniProgramPrescription {
  return {
    date: new Date().toISOString().split('T')[0],
    plannedSessions: 4,
    completedSessions: 1,
    tasks: [
      {
        taskId: 1,
        exerciseName: '平板杠铃卧推',
        exerciseType: 'bench_press',
        targetSets: 4,
        targetReps: 10,
        targetLoadKg: 40,
        targetHr: null,
        intensityLabel: '中等',
        restSeconds: 90,
        status: 'completed',
        coachingTip: '注意控制下放速度，胸部充分拉伸后发力推起',
      },
      {
        taskId: 2,
        exerciseName: '哑铃上斜卧推',
        exerciseType: 'incline_press',
        targetSets: 3,
        targetReps: 12,
        targetLoadKg: 16,
        targetHr: null,
        intensityLabel: '中等',
        restSeconds: 75,
        status: 'in_progress',
        coachingTip: '保持上背贴紧凳面，肘部约45度角',
      },
      {
        taskId: 3,
        exerciseName: '高位下拉',
        exerciseType: 'lat_pulldown',
        targetSets: 4,
        targetReps: 10,
        targetLoadKg: 35,
        targetHr: null,
        intensityLabel: '中高',
        restSeconds: 90,
        status: 'pending',
        coachingTip: '想象用肘部引导下拉，顶峰收缩停留1秒',
      },
      {
        taskId: 4,
        exerciseName: '坐姿划船',
        exerciseType: 'seated_row',
        targetSets: 3,
        targetReps: 12,
        targetLoadKg: 30,
        targetHr: null,
        intensityLabel: '中等',
        restSeconds: 75,
        status: 'pending',
        coachingTip: '保持核心收紧，肩胛骨后缩发力',
      },
    ],
    aiSuggestion: '根据本周训练数据，建议今日进行胸背复合训练，组间休息75-90秒。注意保持动作标准，避免借力。',
    coachingReasoning: '用户处于成长期，近期上肢力量稳定增长。AE算法推荐渐进式负荷增加5%以突破平台期。',
    exerciseGoal: '渐进式力量提升',
    exerciseGoalEn: 'Progressive Strength Gain',
    userStage: 'growth',
    targetHrZone: null,
    healthTips: [
      {
        title: '训练后恢复',
        content: '建议训练后30分钟内补充20-30g蛋白质，促进肌肉修复。',
        evidenceSource: 'ISS Position Stand: Protein & Exercise, 2017',
        category: 'nutrition',
      },
      {
        title: '组间休息',
        content: '力量训练组间休息2-3分钟可最大化力量输出，短休息(60s)更适合肌肥大目标。',
        evidenceSource: 'Schoenfeld et al., J Strength Cond Res, 2016',
        category: 'training',
      },
    ],
    algorithmVersion: 'XIN-AE-v0.1',
    evidenceRefs: [
      'ACSM Resistance Training Guidelines (2021)',
      'ISS Position Stand: Protein & Exercise (2017)',
      'Garber et al., Med Sci Sports Exerc (2011)',
    ],
  };
}

function getDemoDeviceTasks(): DeviceTasksResponse {
  return {
    device_type: 'strength_station',
    tasks: [
      { taskId: 1, exerciseName: '平板卧推', exerciseType: 'bench_press', targetSets: 4, targetReps: 10, targetLoadKg: 45, restSeconds: 90, intensityLabel: '中高', coachingTip: '控制下放速度2秒' },
      { taskId: 2, exerciseName: '高位下拉', exerciseType: 'lat_pulldown', targetSets: 4, targetReps: 10, targetLoadKg: 38, restSeconds: 90, intensityLabel: '中等', coachingTip: '肘部引导发力' },
      { taskId: 3, exerciseName: '坐姿划船', exerciseType: 'seated_row', targetSets: 3, targetReps: 12, targetLoadKg: 32, restSeconds: 75, intensityLabel: '中等', coachingTip: '核心收紧' },
    ],
    total_exercises: 3,
    muscle_groups: ['胸大肌', '背阔肌', '菱形肌'],
  };
}

function getDemoEquipmentResolve(): EquipmentResolveResult {
  return {
    equipmentId: 1,
    equipmentCode: 'EQ-000001',
    equipmentName: '坐姿推胸训练器',
    equipmentType: 'chest_press',
    location: 'A区-01',
    deviceId: 1,
    deviceCode: 'HB-3412',
    bluetoothName: 'gy_ble25t1',
    serviceUuid: '0000FFE0-0000-1000-8000-00805F9B34FB',
    notifyCharUuid: '0000FFE4-0000-1000-8000-00805F9B34FB',
    countingConfig: {
      mainAxis: 'pitch',
      upThreshold: 20,
      downThreshold: 5,
      minIntervalMs: 600,
      minRange: 15,
      smoothingWindow: 5,
    },
  };
}

function getDemoTrainingSessions(): any[] {
  try {
    const stored = wx.getStorageSync('demo_training_sessions');
    return Array.isArray(stored) ? stored : [];
  } catch {
    return [];
  }
}

function saveDemoTrainingSession(record: any) {
  try {
    const records = getDemoTrainingSessions();
    wx.setStorageSync('demo_training_sessions', [record, ...records].slice(0, 50));
  } catch (e) {
    console.warn('[API] save demo training session failed', e);
  }
}

function getDemoHealthProfile(): HealthProfile {
  return {
    user_id: 'demo-user',
    name: '张先生',
    age: 35,
    gender: '男',
    stage: 'growth',
    stage_label: '成长期',
    goal: '增强体质',
    health_score: 78,
    streak_days: 7,
    compliance_rate: 72,
    total_sessions: 23,
    sessions_last_30_days: 12,
    avg_duration_minutes: 45,
    devices: [
      {
        device_id: 'HB-3412',
        device_name: '智能力量站 Pro',
        status: 'online',
        last_seen: new Date().toISOString(),
      },
    ],
  };
}

// ─── 演示模式统一数据入口 ────────────────────────────────────────────────────

export function getDemoData(key: string): any {
  const demoStore: Record<string, any> = {
    user: {
      userId: 'demo-user',
      nickname: '张先生',
      avatar: '',
      level: 3,
      streakDays: 7,
      complianceRate: 72,
      totalSessions: 23,
      stage: 'growth',
      stageLabel: '成长期',
    },
    devices: [
      {
        bindingId: 1,
        deviceId: 1,
        deviceCode: 'HB-3412',
        deviceName: '智能力量站 Pro',
        firmwareVersion: 'v1.2.0',
        status: 'online',
        lastSeenAt: new Date().toISOString(),
        totalSessions: 23,
      },
    ],
    todayProgress: {
      date: new Date().toISOString().split('T')[0],
      plannedSessions: 4,
      completedSessions: 1,
      totalDurationMin: 45,
      totalSets: 14,
      totalReps: 140,
      complianceRate: 72,
      tasks: [
        { taskId: 1, exerciseName: '平板杠铃卧推', exerciseType: 'bench_press', targetSets: 4, targetReps: 10, targetLoadKg: 40, status: 'completed' },
        { taskId: 2, exerciseName: '哑铃上斜卧推', exerciseType: 'incline_press', targetSets: 3, targetReps: 12, targetLoadKg: 16, status: 'in_progress' },
        { taskId: 3, exerciseName: '高位下拉', exerciseType: 'lat_pulldown', targetSets: 4, targetReps: 10, targetLoadKg: 35, status: 'pending' },
        { taskId: 4, exerciseName: '坐姿划船', exerciseType: 'seated_row', targetSets: 3, targetReps: 12, targetLoadKg: 30, status: 'pending' },
      ],
    },
    miniPrescription: getDemoMiniprogramPrescription(),
    renderedPlan: getDemoRenderedPlan(),
    healthProfile: getDemoHealthProfile(),
  };
  return demoStore[key];
}

// ─── 数据模型 ────────────────────────────────────────────────────────────────

export interface UserInfo {
  userId: number;
  nickname: string;
  avatar?: string;
  level?: number;
  streakDays: number;
  complianceRate?: number;
  totalSessions?: number;
  stage?: string;
  stageLabel?: string;
}

export interface DeviceInfo {
  bindingId: number;
  deviceId: number;
  deviceCode: string;
  deviceName: string;
  firmwareVersion: string;
  status: 'online' | 'offline';
  lastSeenAt: string;
  totalSessions: number;
}

export interface TodayProgress {
  date: string;
  plannedSessions: number;
  completedSessions: number;
  totalDurationMin: number;
  totalSets: number;
  totalReps: number;
  complianceRate: number;
  tasks: TrainingTask[];
}

export interface TrainingTask {
  taskId: number;
  exerciseName?: string;
  exerciseType: string;
  targetSets: number;
  targetReps: number;
  targetLoadKg: number;
  status: 'pending' | 'in_progress' | 'completed';
}

export interface TrainingHistoryResp {
  total: number;
  records: SessionSummary[];
}

export interface SessionSummary {
  sessionId: number;
  date: string;
  exerciseType: string;
  totalSets: number;
  totalReps: number;
  durationMin: number;
  volumeKg: number;
}

export interface SessionDetail {
  sessionId: number;
  startTime: string;
  endTime?: string;
  exerciseType: string;
  sets: SetDetail[];
  aiFeedback?: string;
}

export interface SetDetail {
  setNumber: number;
  repCount: number;
  avgLoadKg: number;
  peakLoadKg: number;
  qualityScore?: number;
}

export interface TrainingPlan {
  planId: number;
  title: string;
  description: string;
  targetSets: number;
  targetRepsMin: number;
  targetRepsMax: number;
  suggestedRestSec: number;
  aiModel: string;
}

export interface HealthProfile {
  user_id: string;
  name?: string;
  age?: number;
  gender?: string;
  stage?: string;
  stage_label?: string;
  goal?: string;
  health_score?: number;
  streak_days?: number;
  compliance_rate?: number;
  total_sessions?: number;
  sessions_last_30_days?: number;
  avg_duration_minutes?: number;
  devices?: Array<{
    device_id: string;
    device_name: string;
    status: string;
    last_seen: string;
  }>;
}

export interface ExercisePrescription {
  user_id: string;
  exercise_type: string;
  frequency_per_week: number;
  duration_minutes: number;
  intensity: string;
  sets: number;
  reps_range: [number, number];
  rest_seconds: number;
  notes: string;
}

export interface RenderedPlan {
  plan_id: string;
  user_id: string;
  stage_label: string;
  stage_icon: string;
  color_primary: string;
  color_secondary: string;
  motivation: string;
  recommendations: Array<{
    index: number;
    title: string;
    content: string;
    reason: string;
  }>;
  generated_at: string;
  status: string;
  share_title: string;
  share_description: string;
}
