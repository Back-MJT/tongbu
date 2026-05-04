/**
 * TypeScript 类型定义 - 昕动健康小程序
 */

// ─── 用户 ────────────────────────────────────────────────────────────

export interface UserInfo {
  userId?: string | number;
  nickname?: string;
  avatar?: string;
  phone?: string;
  gender?: string;
  age?: number;
  stage?: 'beginner' | 'growth' | 'plateau' | 'advanced';
  stageLabel?: string;
  stageColor?: string;
  level?: number;
}

export interface LoginResult {
  token: string;
  userId: string;
  nickname?: string;
}

// ─── 设备 ────────────────────────────────────────────────────────────

export interface DeviceInfo {
  deviceId?: string;
  deviceCode: string;
  deviceName?: string;
  deviceType?: string;
  status?: 'online' | 'offline';
  batteryLevel?: number;
  firmwareVersion?: string;
}

export interface BleDevice {
  deviceId: string;
  name?: string;
  rssi: number;
  deviceCode?: string;
  batteryLevel?: number;
  firmwareVersion?: string;
  currentReps?: number;
  currentSets?: number;
}

export interface ImuSample {
  ax: number;
  ay: number;
  az: number;
  gx: number;
  gy: number;
  gz: number;
  roll: number;
  pitch: number;
  yaw: number;
  temp: number;
  ho: number;
  timestamp: number;
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
    setRestMs?: number;
  };
}

export interface TrainingSetSummary {
  setNo: number;
  reps: number;
  durationSec: number;
  startedAt?: string;
  endedAt?: string;
}

// ─── 训练 ────────────────────────────────────────────────────────────

export interface TodayProgress {
  completedSessions: number;
  plannedSessions: number;
  totalDuration?: number;
  avgHeartRate?: number;
}

export interface TrainingHistoryResp {
  records: TrainingSession[];
  total: number;
  page: number;
  size: number;
}

export interface TrainingSession {
  sessionId: number;
  date: string;
  duration: number;
  exerciseType: string;
  completed: boolean;
  score?: number;
}

export interface SessionDetail {
  sessionId: number;
  exercises: ExerciseRecord[];
}

export interface ExerciseRecord {
  exerciseName: string;
  sets: number;
  reps: number;
  load: number;
  heartRate?: number;
}

// ─── 运动处方 (干预引擎 / AE算法) ─────────────────────────────────────

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

// ─── 健康档案 (干预引擎) ──────────────────────────────────────────────

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
  devices?: Array<{ device_id: string; device_type: string; nickname?: string }>;
}

// ─── 渲染后训练计划 (干预引擎) ────────────────────────────────────────

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

// ─── API 通用响应 ────────────────────────────────────────────────────

export interface ApiResponse<T = any> {
  code: number;
  data: T;
  msg?: string;
}
