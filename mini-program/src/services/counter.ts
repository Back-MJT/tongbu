/**
 * 本地动作计数服务
 * 使用后台下发的计数配置，对 IMU 实时数据进行计次与自动切组。
 */

import type { ImuSample } from '../models';

export interface CountingConfig {
  mainAxis?: string;
  upThreshold?: number;
  downThreshold?: number;
  minIntervalMs?: number;
  minRange?: number;
  smoothingWindow?: number;
  setRestMs?: number;
}

export interface SetSummary {
  setNo: number;
  reps: number;
  durationSec: number;
  startedAt: number;
  endedAt: number;
}

export interface CountingState {
  reps: number;
  sets: number;
  currentSetReps: number;
  completedSetReps: number;
  phase: 'idle' | 'moving_up' | 'peak' | 'moving_down' | 'resting';
  axisValue: number;
  rangeValue: number;
  lastRepAt: number;
  restMs: number;
  setSummaries: SetSummary[];
  duration: number;
  avgRepTime: number;
  bleConnected: boolean;
  imuValid: boolean;
  sensorStatus: 'connected' | 'no_signal' | 'no_data' | 'disconnected';
}

const DEFAULT_CONFIG: Required<CountingConfig> = {
  mainAxis: 'pitch',
  upThreshold: 20,
  downThreshold: 5,
  minIntervalMs: 600,
  minRange: 15,
  smoothingWindow: 5,
  setRestMs: 25000,
};

export class ImuCounterService {
  private config: Required<CountingConfig>;
  private values: number[] = [];
  private reps = 0;
  private sets = 0;
  private phase: CountingState['phase'] = 'idle';
  private peakValue = Number.NEGATIVE_INFINITY;
  private valleyValue = Number.POSITIVE_INFINITY;
  private lastRepAt = 0;
  private currentSetReps = 0;
  private currentSetStartedAt = 0;
  private completedSetReps = 0;
  private setSummaries: SetSummary[] = [];
  private sessionStartedAt = 0;
  private bleConnected = false;
  private imuValid = false;

  constructor(config?: CountingConfig) {
    this.config = { ...DEFAULT_CONFIG, ...(config || {}) };
  }

  updateConfig(config?: CountingConfig) {
    this.config = { ...DEFAULT_CONFIG, ...(config || {}) };
    this.values = [];
    this.phase = 'idle';
    this.peakValue = Number.NEGATIVE_INFINITY;
    this.valleyValue = Number.POSITIVE_INFINITY;
  }

  reset() {
    this.values = [];
    this.reps = 0;
    this.sets = 0;
    this.phase = 'idle';
    this.peakValue = Number.NEGATIVE_INFINITY;
    this.valleyValue = Number.POSITIVE_INFINITY;
    this.lastRepAt = 0;
    this.currentSetReps = 0;
    this.currentSetStartedAt = 0;
    this.completedSetReps = 0;
    this.setSummaries = [];
    this.sessionStartedAt = 0;
    this.bleConnected = false;
    this.imuValid = false;
  }

  pushSample(sample: ImuSample): CountingState {
    const rawAxis = this.getAxisValue(sample, this.config.mainAxis);
    const axisValue = this.smooth(rawAxis);
    const restMs = this.lastRepAt > 0 ? sample.timestamp - this.lastRepAt : 0;

    // Track BLE/IMU status
    this.bleConnected = true;
    this.imuValid = this.isValidImu(sample);

    // Auto-start session timer on first sample
    if (this.sessionStartedAt === 0) {
      this.sessionStartedAt = sample.timestamp;
    }

    if (this.currentSetReps > 0 && restMs >= this.config.setRestMs) {
      this.finalizeCurrentSet(this.lastRepAt);
    }

    this.peakValue = Math.max(this.peakValue, axisValue);
    this.valleyValue = Math.min(this.valleyValue, axisValue);

    if (this.phase === 'idle' && axisValue >= this.config.upThreshold) {
      this.phase = 'moving_up';
      this.peakValue = axisValue;
      this.valleyValue = axisValue;
    } else if (this.phase === 'moving_up') {
      if (axisValue > this.peakValue) {
        this.peakValue = axisValue;
      }
      const peakRange = this.peakValue - this.valleyValue;
      if (peakRange >= this.config.minRange && axisValue < this.peakValue - this.config.downThreshold) {
        this.phase = 'peak';
      }
    } else if (this.phase === 'peak') {
      if (axisValue <= this.config.downThreshold) {
        this.phase = 'moving_down';
      }
    } else if (this.phase === 'moving_down') {
      const rangeValue = this.peakValue - this.valleyValue;
      const enoughRange = rangeValue >= this.config.minRange;
      const enoughInterval = sample.timestamp - this.lastRepAt >= this.config.minIntervalMs;
      if (axisValue <= this.config.downThreshold && enoughRange && enoughInterval) {
        this.recordRep(sample.timestamp);
        this.phase = 'resting';
        this.peakValue = Number.NEGATIVE_INFINITY;
        this.valleyValue = Number.POSITIVE_INFINITY;
      } else if (axisValue > this.config.upThreshold) {
        this.phase = 'moving_up';
        this.peakValue = axisValue;
        this.valleyValue = axisValue;
      }
    } else if (this.phase === 'resting') {
      if (restMs >= this.config.setRestMs && this.currentSetReps > 0) {
        this.finalizeCurrentSet(this.lastRepAt);
      } else if (axisValue >= this.config.upThreshold) {
        this.phase = 'moving_up';
        this.peakValue = axisValue;
        this.valleyValue = axisValue;
      }
    }

    return this.buildState(axisValue, restMs);
  }

  finalizeSession(endedAt: number): CountingState {
    if (this.currentSetReps > 0) {
      this.finalizeCurrentSet(endedAt);
    }
    return this.buildState(0, 0);
  }

  setConnectionStatus(connected: boolean) {
    this.bleConnected = connected;
  }

  private isValidImu(sample: ImuSample): boolean {
    const pitch = sample.pitch ?? NaN;
    const roll = sample.roll ?? NaN;
    const yaw = sample.yaw ?? NaN;
    return (
      typeof pitch === 'number' && !Number.isNaN(pitch) &&
      typeof roll === 'number' && !Number.isNaN(roll) &&
      typeof yaw === 'number' && !Number.isNaN(yaw)
    );
  }

  private recordRep(timestamp: number) {
    this.reps += 1;
    this.currentSetReps += 1;
    this.lastRepAt = timestamp;
    if (this.currentSetStartedAt === 0) {
      this.currentSetStartedAt = timestamp;
    }
  }

  private finalizeCurrentSet(endedAt: number) {
    if (this.currentSetReps <= 0) {
      return;
    }
    this.sets += 1;
    this.completedSetReps = this.currentSetReps;
    this.setSummaries.push({
      setNo: this.sets,
      reps: this.currentSetReps,
      durationSec: Math.max(1, Math.round((endedAt - this.currentSetStartedAt) / 1000)),
      startedAt: this.currentSetStartedAt,
      endedAt,
    });
    this.currentSetReps = 0;
    this.currentSetStartedAt = 0;
    this.phase = 'idle';
  }

  private buildState(axisValue: number, restMs: number): CountingState {
    const rangeReady = this.peakValue !== Number.NEGATIVE_INFINITY
      && this.valleyValue !== Number.POSITIVE_INFINITY
      && Number.isFinite(this.peakValue)
      && Number.isFinite(this.valleyValue);
    const rangeValue = rangeReady ? Math.max(0, this.peakValue - this.valleyValue) : 0;
    const phase = rangeReady || this.currentSetReps > 0 ? this.phase : 'idle';

    // duration: seconds since session started
    const now = this.lastRepAt > 0 ? this.lastRepAt : (this.sessionStartedAt > 0 ? Date.now() : 0);
    const duration = this.sessionStartedAt > 0 ? Math.max(0, Math.round((now - this.sessionStartedAt) / 1000)) : 0;

    // avgRepTime: seconds per rep (total reps in current session)
    const avgRepTime = this.reps > 0 ? Math.round(duration / this.reps) : 0;

    // sensorStatus: derived from bleConnected and imuValid
    let sensorStatus: CountingState['sensorStatus'] = 'disconnected';
    if (!this.bleConnected) {
      sensorStatus = 'disconnected';
    } else if (!this.imuValid) {
      sensorStatus = 'no_data';
    } else if (this.bleConnected && this.imuValid) {
      sensorStatus = 'connected';
    }

    return {
      reps: this.reps,
      sets: this.sets,
      currentSetReps: this.currentSetReps,
      completedSetReps: this.completedSetReps,
      phase,
      axisValue,
      rangeValue,
      lastRepAt: this.lastRepAt,
      restMs,
      setSummaries: [...this.setSummaries],
      duration,
      avgRepTime,
      bleConnected: this.bleConnected,
      imuValid: this.imuValid,
      sensorStatus,
    };
  }

  private smooth(value: number): number {
    this.values.push(value);
    if (this.values.length > this.config.smoothingWindow) {
      this.values.shift();
    }
    const sum = this.values.reduce((acc, item) => acc + item, 0);
    return sum / this.values.length;
  }

  private getAxisValue(sample: ImuSample, axis: string): number {
    const record = sample as unknown as Record<string, number>;
    const value = record[axis];
    if (typeof value === 'number' && !Number.isNaN(value)) {
      return value;
    }
    return sample.pitch;
  }
}
