# 昕动智能小程序代码审查报告

## 项目信息
- 项目: 昕动健康微信小程序 (xindong-mini-program)
- 框架: Taro 4.x + Vue 3 + TypeScript + Pinia
- 主要功能: 健身数据追踪、BLE设备连接、IMU动作计数、AI训练处方

---

## 问题列表 (按严重程度排序)

---

## 🔴 HIGH 严重问题

### 问题 #1: 硬编码 Demo Token 安全漏洞
**文件**: `src/config/env.ts`
**行号**: 19, 130
**问题**: 演示模式使用固定字符串 token `'mp_demo_token_2026'`，攻击者可利用此冒充演示账号
**当前代码**:
```typescript
// 第19行
return token === 'mp_demo_token_2026';

// 第130行
wx.setStorageSync('auth_token', 'mp_demo_token_2026');
```
**修复方案**: 生成随机 token 或从后端获取 demo session，示例:
```typescript
function enterDemoMode() {
  const demoToken = 'demo_' + Date.now() + '_' + Math.random().toString(36).slice(2);
  wx.setStorageSync('auth_token', demoToken);
  // ...
}
```

---

### 问题 #2: 前端生成并暴露验证码
**文件**: `src/pages/login/index.vue`
**行号**: 169-190
**问题**: 验证码由前端生成并通过 `wx.showToast` 显示，明文存储于 storage，这是严重的安全漏洞
**当前代码**:
```typescript
function onSendCode() {
  const code = String(Math.floor(100000 + Math.random() * 900000));
  wx.setStorageSync('login_code_phone', phone.value);
  wx.setStorageSync('login_code', code);
  console.log('[Login] Dev verify code:', code);
  wx.showToast({ title: `验证码 ${code}`, icon: 'none' });
  // ...
}
```
**修复方案**: 调用后端API获取验证码，前端仅传递手机号和用户输入的验证码

---

### 问题 #3: BLE断连后无法重连
**文件**: `src/services/ble.ts`
**行号**: 47, 205-222
**问题**: `notifyListenerBound` 标志在首次绑定后永不重置，导致断开后重新连接时 `bindNotifyListener` 直接返回，IMU数据不再接收
**当前代码**:
```typescript
private notifyListenerBound = false; // 第47行

private bindNotifyListener() {
  if (this.notifyListenerBound) {
    return; // 永远不会再执行
  }
  wx.onBLECharacteristicValueChange((res) => {
    if (res.deviceId !== this.activeDeviceId) {
      return;
    }
    // ...
  });
  this.notifyListenerBound = true;
}
```
**修复方案**: 在 `disconnect()` 方法中重置标志，并在 `connectGyDevice` 开始时清理旧监听器:
```typescript
async disconnect(deviceId?: string): Promise<void> {
  // ... 现有逻辑 ...

  this.activeDeviceId = '';
  this.activeNotifyCharUuid = '';
  this.notifyListenerBound = false; // 重置标志
  this.sampleCallback = null;
}
```

---

### 问题 #4: Timer未保存导致无法清理
**文件**: `src/services/ble.ts`
**行号**: 115-117
**问题**: `setTimeout` 返回值未保存，`stopScan` 被调用时无法清除之前的超时任务
**当前代码**:
```typescript
setTimeout(() => {
  this.stopScan();
}, timeout);
```
**修复方案**:
```typescript
private scanTimerId: number | null = null;

async startScan(options: BleScanOptions = {}, onFound?: BleCallback): Promise<void> {
  // ...
  this.scanTimerId = setTimeout(() => {
    this.stopScan();
  }, timeout);
}

async stopScan(): Promise<void> {
  if (this.scanTimerId !== null) {
    clearTimeout(this.scanTimerId);
    this.scanTimerId = null;
  }
  // ...
}
```

---

## 🟡 MEDIUM 中等问题

### 问题 #5: 类型安全缺失 - 大量any断言
**文件**: `src/services/api.ts`
**行号**: 57-62, 74
**问题**: `as any` 滥用导致TypeScript类型检查完全失效
**当前代码**:
```typescript
const responseData = (res.data || {}) as any;
const result = {
  code: typeof responseData.code === 'number' ? responseData.code : res.statusCode,
  data: responseData.data ?? responseData,
  msg: responseData.msg || responseData.message,
} as { code: number; data: T; msg?: string };
// ...
} catch (err: any) {
```
**修复方案**: 定义响应类型接口并使用类型守卫:
```typescript
interface ApiResponse<T> {
  code: number;
  data: T;
  msg?: string;
}

function isApiResponse(res: any): res is ApiResponse<unknown> {
  return typeof res?.code === 'number';
}
```

---

### 问题 #6: Store使用any类型
**文件**: `src/stores/user.ts`
**行号**: 61, 72, 82
**问题**: `setUserProfile`, `setRenderedPlan`, `setDevices` 参数类型为 `any`
**当前代码**:
```typescript
setUserProfile(profile: any) {
  // ...
}
setRenderedPlan(plan: any) {
  // ...
}
setDevices(devices: any[]) {
  // ...
}
```
**修复方案**: 从 `models/index.ts` 导入类型并使用:
```typescript
import type { HealthProfile, RenderedPlan, DeviceInfo } from '../models';

setUserProfile(profile: HealthProfile) {
  // ...
}
```

---

### 问题 #7: sessionTimer/setInterval未保存
**文件**: `src/pages/device-binding/index.vue`
**行号**: 175-176, 351-354
**问题**: `setInterval` 返回值未保存，组件销毁后timer继续运行
**当前代码**:
```typescript
let scanTimer = null;  // 类型any
let sessionTimer = null; // 类型any

// ...
sessionTimer = setInterval(() => {
  sessionElapsedMs.value = Date.now() - sessionStartedAt.value;
}, 1000);
```
**修复方案**:
```typescript
let scanTimer: ReturnType<typeof setTimeout> | null = null;
let sessionTimer: ReturnType<typeof setInterval> | null = null;

// 清理时
if (sessionTimer !== null) {
  clearInterval(sessionTimer);
  sessionTimer = null;
}
```

---

### 问题 #8: Timer清理逻辑不完整
**文件**: `src/pages/device-binding/index.vue`
**行号**: 200-205
**问题**: `onUnmounted` 中 `bleService.disconnect()` 是异步但未等待，且多个清理路径可能遗漏
**当前代码**:
```typescript
onUnmounted(() => {
  if (scanTimer) clearTimeout(scanTimer);
  if (sessionTimer) clearInterval(sessionTimer);
  bleService.stopScan();
  bleService.disconnect().catch(() => undefined); // 未等待
});
```
**修复方案**:
```typescript
onUnmounted(async () => {
  if (scanTimer !== null) {
    clearTimeout(scanTimer);
    scanTimer = null;
  }
  if (sessionTimer !== null) {
    clearInterval(sessionTimer);
    sessionTimer = null;
  }
  bleService.stopScan();
  try {
    await bleService.disconnect();
  } catch (e) {
    // ignore
  }
});
```

---

### 问题 #9: 验证码校验逻辑缺陷
**文件**: `src/pages/login/index.vue`
**行号**: 139-144
**问题**: 首次获取验证码时 storage 为空，条件直接放行但实际未发验证码
**当前代码**:
```typescript
const sentPhone = wx.getStorageSync('login_code_phone');
const sentCode = wx.getStorageSync('login_code');
if (sentPhone && sentCode && (sentPhone !== phone.value || sentCode !== verifyCode.value)) {
  wx.showToast({ title: '验证码不正确', icon: 'none' });
  return;
}
```
**修复方案**: 添加发送记录校验:
```typescript
async function onPhoneLogin() {
  if (!canSubmit.value) return;

  const sentPhone = wx.getStorageSync('login_code_phone');
  const sentCode = wx.getStorageSync('login_code');

  // 检查是否已发送过验证码
  if (!sentPhone || !sentCode) {
    wx.showToast({ title: '请先获取验证码', icon: 'none' });
    return;
  }

  // 检查手机号是否匹配
  if (sentPhone !== phone.value) {
    wx.showToast({ title: '手机号已变更，请重新获取验证码', icon: 'none' });
    return;
  }

  // 检查验证码是否正确
  if (sentCode !== verifyCode.value) {
    wx.showToast({ title: '验证码不正确', icon: 'none' });
    return;
  }

  // 清除验证码
  wx.removeStorageSync('login_code');
  wx.removeStorageSync('login_code_phone');

  // 调用登录API
  // ...
}
```

---

### 问题 #10: 依赖不存在的生成文件
**文件**: `src/config/env.ts`
**行号**: 10
**问题**: `import { localDevConfig } from './local-dev.generated';` 该文件为编译生成，仓库中不存在
**当前代码**:
```typescript
import { localDevConfig } from './local-dev.generated';
```
**修复方案**: 提供内联默认配置:
```typescript
interface LocalDevConfig {
  apiBase: string;
  ieBase: string;
  wsUrl: string;
}

function getLocalDevConfig(): LocalDevConfig {
  // 尝试从环境变量读取，或使用默认开发地址
  return {
    apiBase: process.env.API_BASE || 'http://localhost:8080',
    ieBase: process.env.IE_BASE || 'http://localhost:8080',
    wsUrl: process.env.WS_URL || 'ws://localhost:8080/ws',
  };
}

const localDevConfig = getLocalDevConfig();
```

---

### 问题 #11: 训练数据时间戳格式不一致
**文件**: `src/pages/device-binding/index.vue`
**行号**: 387-392
**问题**: `finishSession` 传入的 `startedAt`/`endedAt` 是 number 类型，但 API 期望 string
**当前代码**:
```typescript
sets: finalState.setSummaries.map((set) => ({
  setNo: set.setNo,
  reps: set.reps,
  durationSec: set.durationSec,
  startedAt: set.startedAt,   // number (Unix ms)
  endedAt: set.endedAt,       // number (Unix ms)
})),
```
**修复方案**: 统一转换为 ISO 字符串:
```typescript
sets: finalState.setSummaries.map((set) => ({
  setNo: set.setNo,
  reps: set.reps,
  durationSec: set.durationSec,
  startedAt: new Date(set.startedAt).toISOString(),
  endedAt: new Date(set.endedAt).toISOString(),
})),
```

同时修改 `counter.ts` 的 `finalizeCurrentSet`:
```typescript
private finalizeCurrentSet(endedAt: number) {
  // ...
  this.setSummaries.push({
    setNo: this.sets,
    reps: this.currentSetReps,
    durationSec: Math.max(1, Math.round((endedAt - this.currentSetStartedAt) / 1000)),
    startedAt: this.currentSetStartedAt,
    endedAt: endedAt,
  });
}
```

---

### 问题 #12: readInt16符号扩展潜在问题
**文件**: `src/services/ble.ts`
**行号**: 260-263
**问题**: 手动位运算的符号扩展在某些JS引擎中可能行为不一致
**当前代码**:
```typescript
private readInt16(bytes: Uint8Array, offset: number): number {
  const value = (bytes[offset] << 8) | bytes[offset + 1];
  return value > 0x7fff ? value - 0x10000 : value;
}
```
**修复方案**: 使用 DataView (Taro 微信小程序环境支持):
```typescript
private readInt16(bytes: Uint8Array, offset: number): number {
  const buffer = bytes.buffer.slice(bytes.byteOffset + offset, bytes.byteOffset + offset + 2);
  const view = new DataView(buffer);
  return view.getInt16(0, false); // big-endian
}
```

---

### 问题 #13: 峰值检测阈值过小
**文件**: `src/services/counter.ts`
**行号**: 109
**问题**: `peakValue - 1` 作为回程判断阈值过小，IMU噪声易误触发
**当前代码**:
```typescript
} else if (this.phase === 'moving_up') {
  if (axisValue > this.peakValue) {
    this.peakValue = axisValue;
  }
  if (axisValue < this.peakValue - 1) { // 阈值过小
    this.phase = 'peak';
  }
}
```
**修复方案**: 引入最小峰值幅度要求:
```typescript
const MIN_PEAK_RANGE = 10; // 最小峰值幅度

if (axisValue < this.peakValue - this.config.downThreshold && (this.peakValue - this.valleyValue) >= MIN_PEAK_RANGE) {
  this.phase = 'peak';
}
```

---

## 🟢 LOW 低优先级问题

### 问题 #14: smooth方法性能优化
**文件**: `src/services/counter.ts`
**行号**: 194-201
**问题**: `Array.shift()` 为 O(n) 操作，可用环形缓冲区优化
**修复方案**:
```typescript
private values: number[] = [];
private valueIndex = 0;

private smooth(value: number): number {
  if (this.values.length < this.config.smoothingWindow) {
    this.values.push(value);
  } else {
    this.values[this.valueIndex] = value;
    this.valueIndex = (this.valueIndex + 1) % this.config.smoothingWindow;
  }
  const sum = this.values.reduce((acc, item) => acc + item, 0);
  return sum / this.values.length;
}
```

---

### 问题 #15: 401判断竞态条件
**文件**: `src/services/api.ts`
**行号**: 64-71
**问题**: 401判断依赖于响应体中的code字段，但网络错误时不会触发
**修复方案**: 同时检查 `res.statusCode`:
```typescript
if (res.statusCode === 401 || result.code === 401) {
  wx.removeStorageSync('auth_token');
  wx.reLaunch({ url: '/pages/login/index' });
}

if (res.statusCode >= 400) {
  throw new Error(result.msg || `HTTP ${res.statusCode}`);
}
```

---

### 问题 #16: ApiResponse默认类型为any
**文件**: `src/models/index.ts`
**行号**: 239
**问题**: `ApiResponse<T = any>` 默认值应改为 `unknown`
**修复方案**:
```typescript
export interface ApiResponse<T = unknown> {
  code: number;
  data: T;
  msg?: string;
}
```

---

### 问题 #17: counterState空值处理
**文件**: `src/pages/device-binding/index.vue`
**行号**: 479-486
**问题**: `counterState.value?.phase` 为 undefined 时逻辑不正确
**修复方案**:
```typescript
const phaseLabel = computed(() => {
  const phase = counterState.value?.phase ?? 'idle'; // 默认idle
  // ...
});
```

---

### 问题 #18: rangeValue为0时phase未重置
**文件**: `src/services/counter.ts`
**行号**: 185-187
**问题**: `peakValue` 或 `valleyValue` 为无穷值时 `rangeValue` 为0，但 `phase` 可能仍为 `'peak'`
**修复方案**: 在 `buildState` 中添加状态一致性检查，或在模板中使用 `Number.NEGATIVE_INFINITY` 判断

---

## 文件修改优先级

### 优先级1 (必须修复)
1. `src/config/env.ts` - 问题 #1
2. `src/pages/login/index.vue` - 问题 #2
3. `src/services/ble.ts` - 问题 #3, #4
4. `src/pages/device-binding/index.vue` - 问题 #7, #8

### 优先级2 (建议修复)
5. `src/services/api.ts` - 问题 #3, #15
6. `src/stores/user.ts` - 问题 #6
7. `src/services/counter.ts` - 问题 #9, #13, #14, #18
8. `src/models/index.ts` - 问题 #16
9. `src/config/env.ts` - 问题 #10

### 优先级3 (可选优化)
10. `src/services/ble.ts` - 问题 #12

---

## 修复验证清单

- [ ] demo token 不再硬编码
- [ ] 验证码由后端下发
- [ ] BLE断开后能正常重连
- [ ] setTimeout/setInterval timer正确清理
- [ ] TypeScript类型安全，无 `as any`
- [ ] 训练数据时间戳格式一致 (ISO string)
- [ ] 所有timer在组件卸载时正确清理