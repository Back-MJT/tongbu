<!--
  设备绑定页面 - 昕动智能小程序
  功能: BLE扫描、设备绑定、二维码扫码绑定
-->
<template>
  <view class="device-binding-page">
    <!-- 页面头部 -->
    <view class="page-header">
      <text class="page-title">器械训练</text>
    </view>

    <!-- 扫码入口 -->
    <view v-if="selectedTask" class="task-context-card">
      <text class="task-context-title">当前训练任务</text>
      <text class="task-context-name">{{ selectedTask.exerciseName }}</text>
      <text class="task-context-meta">
        {{ selectedTask.targetSets || '-' }}组 × {{ selectedTask.targetReps || '-' }}次
        <text v-if="selectedTask.targetLoadKg"> · {{ selectedTask.targetLoadKg }}kg</text>
      </text>
    </view>

    <view class="scan-card" @tap="onScanQrCode">
      <view class="scan-icon">📷</view>
      <view class="scan-info">
        <text class="scan-title">扫码开始训练</text>
        <text class="scan-desc">扫描器械二维码后，自动连接该器械绑定的传感器</text>
      </view>
      <text class="scan-arrow">›</text>
    </view>

    <view class="demo-card" @tap="useCurrentEquipment">
      <text class="demo-title">暂未贴二维码：使用当前器械</text>
      <text class="demo-desc">直接加载 EQ-000001，并自动连接后台绑定的传感器，用于先保证训练流程正常</text>
    </view>

    <view v-if="resolvedEquipment" class="resolved-card">
      <view class="resolved-title">当前器械</view>
      <text class="resolved-name">{{ resolvedEquipment.equipmentName }}</text>
      <text class="resolved-meta">器械编号: {{ resolvedEquipment.equipmentCode }}</text>
      <text class="resolved-meta">蓝牙名称: {{ resolvedEquipment.bluetoothName || '-' }}</text>
      <text class="resolved-meta">连接状态: {{ connectionStatusText }}</text>
      <text v-if="autoConnectStatus" class="resolved-meta">自动连接: {{ autoConnectStatus }}</text>
      <text v-if="counterState" class="resolved-metric">
        次数 {{ counterState.reps }} · 已完成 {{ counterState.sets }} 组 · 当前组 {{ counterState.currentSetReps }} 次
      </text>
      <text v-if="counterState" class="resolved-meta">
        主轴 {{ resolvedEquipment.countingConfig?.mainAxis || 'pitch' }}:
        {{ counterState.axisValue.toFixed(1) }} / 幅度 {{ counterState.rangeValue.toFixed(1) }}
      </text>
      <text v-if="counterState && counterState.restMs > 0" class="resolved-meta">
        当前休息 {{ Math.round(counterState.restMs / 1000) }} 秒 · 状态 {{ phaseLabel }}
      </text>
      <text v-if="sessionStartedAt" class="resolved-meta">
        已训练 {{ sessionDurationMin }} 分钟
      </text>
      <view class="imu-debug-panel">
        <text class="imu-debug-title">IMU 实时调试</text>
        <text class="resolved-meta">数据状态: {{ latestSample ? '已收到姿态数据' : (connectionStatus === 'connected' ? '已连接，等待通知数据' : '未连接') }}</text>
        <text class="resolved-meta">通知包: {{ rawNotifyCount }} 个</text>
        <text v-if="lastRawPacket" class="resolved-meta">最后原始包({{ lastRawPacket.byteLength }}B): {{ lastRawPacket.hex }}</text>
        <text class="resolved-meta">
          姿态 roll / pitch / yaw:
          {{ latestSample ? `${latestSample.roll.toFixed(1)} / ${latestSample.pitch.toFixed(1)} / ${latestSample.yaw.toFixed(1)}` : '- / - / -' }}
        </text>
        <text v-if="latestSample" class="resolved-meta">
          加速度 ax / ay / az:
          {{ latestSample.ax }} / {{ latestSample.ay }} / {{ latestSample.az }}
        </text>
      </view>
      <view v-if="counterState && counterState.setSummaries.length > 0" class="set-summary-list">
        <text class="set-summary-title">已识别训练组</text>
        <text
          v-for="set in counterState.setSummaries"
          :key="set.setNo"
          class="set-summary-item"
        >
          第{{ set.setNo }}组 · {{ set.reps }}次 · {{ set.durationSec }}秒
        </text>
      </view>
      <view v-if="connectionStatus === 'connected'" class="session-actions">
        <view class="finish-btn" @tap="finishSession">结束训练并保存</view>
      </view>
      <view v-if="connectionStatus !== 'connected'" class="session-actions">
        <view class="simulate-btn" @tap="simulateTrainingSession">开发调试：模拟一组训练</view>
      </view>
    </view>

    <!-- BLE扫描兜底入口 -->
    <view class="section-title">传感器连接兜底</view>

    <!-- BLE扫描按钮 -->
    <view class="ble-scan-btn" @tap="toggleBleScan" :class="{ scanning: isScanning }">
      <text v-if="!isScanning">🔍 重新搜索绑定传感器</text>
      <text v-else>⏳ 正在查找 {{ expectedBluetoothName || '绑定传感器' }}... (点击停止)</text>
    </view>

    <!-- 扫描结果列表 -->
    <view v-if="isScanning || foundDevices.length > 0" class="scan-result-card">
      <view class="result-header">
        <text class="result-title">调试扫描：找到 {{ foundDevices.length }} 个设备</text>
        <text v-if="isScanning" class="result-scanning">扫描中</text>
      </view>

      <!-- 设备列表 -->
      <view
        v-for="(device, idx) in foundDevices"
        :key="idx"
        class="device-scan-item"
        :class="{ matched: isExpectedDevice(device) }"
        @tap="onSelectDevice(device)"
      >
        <view class="device-scan-icon">🏋️</view>
        <view class="device-scan-info">
          <text class="device-scan-name">{{ device.name || device.deviceCode || '未知设备' }}</text>
          <text class="device-scan-code">localName: {{ device.localName || '-' }}</text>
          <text class="device-scan-code">deviceId: {{ shortDeviceId(device.deviceId) }}</text>
          <text class="device-scan-code" v-if="device.deviceCode">匹配: {{ device.deviceCode }}</text>
          <text v-if="isExpectedDevice(device)" class="device-scan-match">当前器械绑定传感器</text>
          <text class="device-scan-rssi">信号: {{ device.rssi }} dBm</text>
        </view>
        <view class="device-scan-action">
          <text class="bind-btn">连接</text>
        </view>
      </view>

      <!-- 扫描中动画 -->
      <view v-if="isScanning" class="scanning-tips">
        <text>正在显示所有附近 BLE 设备。若列表为空，请检查手机蓝牙、微信权限和传感器供电。</text>
      </view>
    </view>

    <!-- 已绑定设备 -->
    <view class="section-title" v-if="myDevices.length > 0">已绑定设备</view>
    <view v-if="myDevices.length > 0" class="bound-devices-card">
      <view
        v-for="(device, idx) in myDevices"
        :key="idx"
        class="device-item"
        :class="{ online: device.status === 'online' }"
        @tap="onBoundDeviceTap(device)"
      >
        <view class="device-icon">🏋️</view>
        <view class="device-info">
          <text class="device-name">{{ device.deviceName }}</text>
          <text class="device-code">{{ device.deviceCode }}</text>
          <text class="device-status" :class="device.status">
            {{ device.status === 'online' ? '● 在线' : '○ 离线' }}
          </text>
        </view>
      </view>
    </view>

    <!-- 绑定结果提示 -->
    <view v-if="bindResult" class="bind-result" :class="bindResult.type">
      <text>{{ bindResult.message }}</text>
    </view>

    <!-- 帮助说明 -->
    <view class="help-card">
      <text class="help-title">找不到设备？</text>
      <view class="help-list">
        <text class="help-item">1. 确保设备已通电并处于广播状态</text>
        <text class="help-item">2. 确保手机蓝牙已开启</text>
        <text class="help-item">3. 尝试靠近设备后重新扫描</text>
        <text class="help-item">4. 建议先扫码器械，再连接对应 GY-BLE25T</text>
      </view>
    </view>
  </view>
</template>

<script>
import { defineComponent, ref, computed, onMounted, onUnmounted } from 'vue';
import Taro from '@tarojs/taro';
import { getMyDevices, normalizeScanCode, resolveEquipment, submitTrainingSession } from '../../services/api';
import { bleService } from '../../services/ble';
import { ImuCounterService } from '../../services/counter';

export default defineComponent({
  setup() {
    const isScanning = ref(false);
    const foundDevices = ref([]);
    const myDevices = ref([]);
    const bindResult = ref(null);
    const resolvedEquipment = ref(null);
    const latestSample = ref(null);
    const rawNotifyCount = ref(0);
    const lastRawPacket = ref(null);
    const counterState = ref(null);
    const connectionStatus = ref('idle');
    const autoConnectStatus = ref('');
    const autoConnecting = ref(false);
    const selectedTask = ref(null);
    const sessionStartedAt = ref(0);
    const sessionElapsedMs = ref(0);
    const counter = new ImuCounterService();
    let scanTimer = null;
    let sessionTimer = null;

    onMounted(async () => {
      const params = Taro.getCurrentInstance()?.router?.params || {};
      if (params.taskId || params.exerciseName) {
        selectedTask.value = {
          taskId: params.taskId || '',
          exerciseName: decodeURIComponent(params.exerciseName || '训练任务'),
          exerciseType: decodeURIComponent(params.exerciseType || 'strength'),
          targetSets: params.targetSets || '',
          targetReps: params.targetReps || '',
          targetLoadKg: params.targetLoadKg || '',
        };
      }
      if (params.equipmentCode) {
        await loadEquipment(params.equipmentCode, true);
        if (params.exerciseName) {
          selectedTask.value = {
            ...(selectedTask.value || {}),
            exerciseName: decodeURIComponent(params.exerciseName),
          };
        }
      }

      // 加载已绑定设备
      try {
        const res = await getMyDevices();
        myDevices.value = res.data || [];
      } catch (e) {
        console.warn('[DeviceBinding] getMyDevices failed', e);
      }
    });

    onUnmounted(() => {
      if (scanTimer) clearTimeout(scanTimer);
      if (sessionTimer) clearInterval(sessionTimer);
      bleService.stopScan();
      bleService.disconnect().catch(() => undefined);
    });

    async function onScanQrCode() {
      console.log('[DeviceBinding] scan tapped');
      if (typeof wx.scanCode !== 'function') {
        wx.showToast({ title: '当前环境不支持扫码，已使用演示器械', icon: 'none' });
        await useCurrentEquipment();
        return;
      }

      try {
        const res = await wx.scanCode({ onlyFromCamera: false });
        const code = normalizeScanCode(res.result || res.code);
        wx.showLoading({ title: '识别器械...' });
        const resolved = await resolveEquipment(code);
        wx.hideLoading();
        applyResolvedEquipment(resolved.data);
        await startBleScan(true);
      } catch (e) {
        wx.hideLoading();
        console.error('[DeviceBinding] scanCode failed', e);
        wx.showToast({ title: '器械识别失败，请重试', icon: 'none' });
      }
    }

    async function useCurrentEquipment() {
      console.log('[DeviceBinding] current equipment tapped');
      await loadEquipment('EQ-000001', true);
    }

    async function loadEquipment(code, autoScanBle = true) {
      try {
        wx.showLoading({ title: '加载器械...' });
        const resolved = await resolveEquipment(code);
        wx.hideLoading();
        applyResolvedEquipment(resolved.data);
        if (autoScanBle) {
          await startBleScan(true);
        }
      } catch (e) {
        wx.hideLoading();
        console.error('[DeviceBinding] load equipment failed', e);
        wx.showToast({ title: '器械加载失败，请检查后端', icon: 'none' });
      }
    }

    async function toggleBleScan() {
      console.log('[DeviceBinding] BLE scan toggled', isScanning.value);
      if (isScanning.value) {
        stopBleScan();
      } else {
        await startBleScan(false);
      }
    }

    async function startBleScan(autoConnect = true) {
      if (isScanning.value) {
        stopBleScan();
      }
      isScanning.value = true;
      foundDevices.value = [];
      autoConnecting.value = autoConnect;
      autoConnectStatus.value = resolvedEquipment.value && autoConnect
        ? `正在查找 ${expectedBluetoothName.value || '绑定传感器'}`
        : '';
      bindResult.value = resolvedEquipment.value
        ? null
        : { type: 'success', message: '请先扫码器械，系统会自动连接该器械绑定的传感器' };

      try {
        const available = await bleService.checkAdapter();
        if (!available) {
          wx.showToast({ title: '蓝牙不可用，请使用真机调试', icon: 'none' });
          isScanning.value = false;
          return;
        }

        await bleService.startScan(
          {
            timeout: 15000,
            filters: [],
          },
          (device) => {
            // 避免重复添加
            const exists = foundDevices.value.find(d => d.deviceId === device.deviceId);
            if (!exists) {
              console.log('[DeviceBinding] BLE found', device);
              foundDevices.value.push(device);
              foundDevices.value.sort((a, b) => getDeviceMatchScore(b) - getDeviceMatchScore(a));
            }
            if (autoConnect && resolvedEquipment.value && getDeviceMatchScore(device) >= 70 && connectionStatus.value !== 'connecting' && connectionStatus.value !== 'connected') {
              autoConnectStatus.value = `已找到 ${getDeviceDisplayName(device)}，正在连接`;
              onSelectDevice(device, true);
            }
          }
        );

        // 15秒后自动停止
        scanTimer = setTimeout(() => {
          if (autoConnect && connectionStatus.value !== 'connected') {
            const bestDevice = foundDevices.value[0];
            autoConnectStatus.value = bestDevice
              ? `未精确匹配绑定传感器，最接近设备为 ${getDeviceDisplayName(bestDevice)}，可手动确认连接`
              : '未发现附近 BLE 设备，请检查传感器供电、手机蓝牙和微信权限';
          }
          stopBleScan();
        }, 20000);
      } catch (e) {
        console.error('[DeviceBinding] BLE scan failed', e);
        wx.showToast({ title: e.message || '蓝牙扫描失败', icon: 'none' });
        isScanning.value = false;
      }
    }

    function stopBleScan() {
      bleService.stopScan();
      isScanning.value = false;
      if (scanTimer) {
        clearTimeout(scanTimer);
        scanTimer = null;
      }
    }

    async function onSelectDevice(device, fromAuto = false) {
      console.log('[DeviceBinding] select device tapped', device);
      if (!resolvedEquipment.value) {
        wx.showToast({ title: '请先扫码识别器械', icon: 'none' });
        return;
      }

      const matchScore = getDeviceMatchScore(device);
      if (!fromAuto && matchScore < 70) {
        wx.showToast({ title: `未精确匹配，仍可手动连接`, icon: 'none' });
      }

      wx.showLoading({ title: fromAuto ? '自动连接中...' : '连接中...' });
      connectionStatus.value = 'connecting';
      rawNotifyCount.value = 0;
      lastRawPacket.value = null;
      latestSample.value = null;
      try {
        await bleService.connectGyDevice(device.deviceId, {
          serviceUuid: resolvedEquipment.value.serviceUuid,
          notifyCharUuid: resolvedEquipment.value.notifyCharUuid,
          onRaw: (payload) => {
            rawNotifyCount.value += 1;
            lastRawPacket.value = payload;
          },
          onSample: (sample) => {
            console.log('[DeviceBinding] IMU sample', sample);
            latestSample.value = sample;
            counterState.value = counter.pushSample(sample);
          },
        });
        wx.hideLoading();
        connectionStatus.value = 'connected';
        sessionStartedAt.value = Date.now();
        sessionElapsedMs.value = 0;
        if (sessionTimer) clearInterval(sessionTimer);
        sessionTimer = setInterval(() => {
          sessionElapsedMs.value = Date.now() - sessionStartedAt.value;
        }, 1000);
        bindResult.value = { type: 'success', message: 'IMU 已连接，正在接收实时数据' };
        autoConnectStatus.value = fromAuto ? `已自动连接绑定传感器: ${getDeviceDisplayName(device)}` : '';
        autoConnecting.value = false;
        stopBleScan();
      } catch (e) {
        wx.hideLoading();
        connectionStatus.value = 'failed';
        console.error('[DeviceBinding] connect failed', e);
        bindResult.value = { type: 'error', message: `IMU 连接失败: ${e.message || e.errMsg || '请检查服务UUID/特征值'}` };
        autoConnectStatus.value = fromAuto ? '自动连接失败，可重新搜索或手动选择' : '';
        autoConnecting.value = false;
      }
    }

    function onBoundDeviceTap(device) {
      console.log('[DeviceBinding] bound device tapped', device);
      const equipmentCode = device.equipmentCode || device.equipment_code;
      wx.showModal({
        title: device.deviceName || device.deviceCode || '已绑定设备',
        content: [
          `设备编号: ${device.deviceCode || '-'}`,
          `状态: ${device.status === 'online' ? '在线' : '离线'}`,
          equipmentCode ? `器械编号: ${equipmentCode}` : '暂无器械编号，请先扫码器械二维码',
        ].join('\n'),
        confirmText: equipmentCode ? '加载器械' : '去扫码',
        cancelText: '关闭',
        success: async (res) => {
          if (!res.confirm) return;
          if (equipmentCode) {
            await loadEquipment(equipmentCode, true);
          } else {
            await onScanQrCode();
          }
        },
      });
    }

    async function finishSession() {
      console.log('[DeviceBinding] finish session tapped');
      if (!resolvedEquipment.value || !counterState.value || !sessionStartedAt.value) {
        wx.showToast({ title: '当前没有可保存的训练会话', icon: 'none' });
        return;
      }

      const durationMs = Date.now() - sessionStartedAt.value;
      const durationMin = Math.max(1, Math.round(durationMs / 60000));
      const finalState = counter.finalizeSession(Date.now());

      wx.showLoading({ title: '保存中...' });
      try {
        const result = await submitTrainingSession({
          equipmentCode: resolvedEquipment.value.equipmentCode,
          deviceCode: resolvedEquipment.value.deviceCode,
          exerciseType: selectedTask.value?.exerciseType || resolvedEquipment.value.equipmentType || 'strength',
          taskId: selectedTask.value?.taskId,
          exerciseName: selectedTask.value?.exerciseName,
          completedSets: finalState.sets,
          totalReps: finalState.reps,
          durationMin,
          totalVolumeKg: 0,
          sets: finalState.setSummaries.map((set) => ({
            setNo: set.setNo,
            reps: set.reps,
            durationSec: set.durationSec,
            startedAt: new Date(set.startedAt).toISOString(),
            endedAt: new Date(set.endedAt).toISOString(),
          })),
        });
        wx.hideLoading();
        bindResult.value = {
          type: 'success',
          message: `训练已保存，记录编号 ${result.data.sessionId}`,
        };
        showSavedActions();
        await bleService.disconnect();
        if (sessionTimer) clearInterval(sessionTimer);
        sessionTimer = null;
        connectionStatus.value = 'idle';
        sessionStartedAt.value = 0;
        sessionElapsedMs.value = 0;
        counter.reset();
        counterState.value = finalState;
        latestSample.value = null;
      } catch (e) {
        wx.hideLoading();
        console.error('[DeviceBinding] finish session failed', e);
        wx.showToast({ title: '训练保存失败', icon: 'none' });
      }
    }

    async function simulateTrainingSession() {
      console.log('[DeviceBinding] simulate session tapped');
      if (!resolvedEquipment.value) {
        wx.showToast({ title: '请先识别器械', icon: 'none' });
        return;
      }

      const now = Date.now();
      const simulatedSets = [
        { setNo: 1, reps: Number(selectedTask.value?.targetReps || 12), durationSec: 45 },
      ];
      wx.showLoading({ title: '保存模拟训练...' });
      try {
        const result = await submitTrainingSession({
          equipmentCode: resolvedEquipment.value.equipmentCode,
          deviceCode: resolvedEquipment.value.deviceCode,
          exerciseType: selectedTask.value?.exerciseType || resolvedEquipment.value.equipmentType || 'strength',
          taskId: selectedTask.value?.taskId,
          exerciseName: selectedTask.value?.exerciseName,
          completedSets: simulatedSets.length,
          totalReps: simulatedSets.reduce((sum, set) => sum + set.reps, 0),
          durationMin: 1,
          totalVolumeKg: Number(selectedTask.value?.targetLoadKg || 0) * simulatedSets[0].reps,
          sets: simulatedSets.map((set) => ({
            ...set,
            startedAt: new Date(now - set.durationSec * 1000).toISOString(),
            endedAt: new Date(now).toISOString(),
          })),
        });
        wx.hideLoading();
        bindResult.value = {
          type: 'success',
          message: `模拟训练已保存，记录编号 ${result.data.sessionId}`,
        };
        showSavedActions();
      } catch (e) {
        wx.hideLoading();
        console.error('[DeviceBinding] simulate session failed', e);
        wx.showToast({ title: '模拟训练保存失败', icon: 'none' });
      }
    }

    function showSavedActions() {
      wx.showModal({
        title: '训练已保存',
        content: '要去进度页查看训练记录吗？',
        confirmText: '去查看',
        cancelText: '继续训练',
        success: (res) => {
          if (res.confirm) {
            wx.switchTab({
              url: '/pages/progress/index',
              fail: (err) => {
                console.error('[DeviceBinding] switch progress failed', err);
                wx.showToast({ title: '请从底部进度页查看记录', icon: 'none' });
              },
            });
          }
        },
      });
    }

    const connectionStatusText = computed(() => {
      if (connectionStatus.value === 'connected') return '已连接';
      if (connectionStatus.value === 'connecting') return '连接中';
      if (connectionStatus.value === 'failed') return '连接失败';
      return '未连接';
    });

    const phaseLabel = computed(() => {
      const phase = counterState.value?.phase;
      if (phase === 'moving_up') return '发力中';
      if (phase === 'peak') return '峰值';
      if (phase === 'moving_down') return '回程';
      if (phase === 'resting') return '休息中';
      return '待命';
    });

    const sessionDurationMin = computed(() => {
      if (!sessionStartedAt.value) {
        return 0;
      }
      return Math.max(1, Math.round(sessionElapsedMs.value / 60000));
    });

    const expectedBluetoothName = computed(() => {
      return resolvedEquipment.value?.bluetoothName || resolvedEquipment.value?.deviceName || '';
    });

    function applyResolvedEquipment(equipment) {
      if (sessionTimer) clearInterval(sessionTimer);
      sessionTimer = null;
      resolvedEquipment.value = equipment;
      connectionStatus.value = 'idle';
      autoConnectStatus.value = '';
      rawNotifyCount.value = 0;
      lastRawPacket.value = null;
      counter.reset();
      counter.updateConfig(equipment.countingConfig || {});
      counterState.value = null;
      latestSample.value = null;
      sessionStartedAt.value = 0;
      sessionElapsedMs.value = 0;
      bindResult.value = {
        type: 'success',
        message: `已识别 ${equipment.equipmentName}，正在自动连接 ${equipment.bluetoothName || equipment.deviceCode || '绑定传感器'}`,
      };
    }

    function normalizeText(value) {
      return String(value || '').trim().toLowerCase();
    }

    function normalizeUuid(value) {
      return String(value || '').replace(/-/g, '').toUpperCase();
    }

    function normalizeDeviceName(value) {
      return normalizeText(value).replace(/[\s:_-]/g, '');
    }

    function getDeviceDisplayName(device) {
      return device?.name || device?.localName || device?.deviceCode || shortDeviceId(device?.deviceId) || '未知设备';
    }

    function getDeviceMatchScore(device) {
      if (!resolvedEquipment.value || !device) return 0;
      const expectedName = normalizeDeviceName(expectedBluetoothName.value);
      const deviceName = normalizeDeviceName(`${device.name || ''} ${device.localName || ''}`);
      const expectedDeviceCode = normalizeDeviceName(resolvedEquipment.value.deviceCode || '');
      let score = 0;

      if (expectedName && deviceName === expectedName) score = Math.max(score, 100);
      if (expectedName && deviceName.includes(expectedName)) score = Math.max(score, 90);
      if (expectedDeviceCode && deviceName.includes(expectedDeviceCode)) score = Math.max(score, 85);

      const serviceUuid = normalizeUuid(resolvedEquipment.value.serviceUuid);
      const advertised = (device.advertisServiceUUIDs || []).map(normalizeUuid);
      if (serviceUuid && advertised.some(uuid => uuid === serviceUuid)) score = Math.max(score, 75);
      if (serviceUuid && advertised.some(uuid => uuid.includes(serviceUuid.slice(4, 8)))) score = Math.max(score, 65);

      return score;
    }

    function isExpectedDevice(device) {
      return getDeviceMatchScore(device) >= 70;
    }

    function shortDeviceId(deviceId) {
      if (!deviceId) return '-';
      const text = String(deviceId);
      return text.length > 18 ? `${text.slice(0, 8)}...${text.slice(-6)}` : text;
    }

    return {
      isScanning,
      foundDevices,
      myDevices,
      bindResult,
      resolvedEquipment,
      latestSample,
      rawNotifyCount,
      lastRawPacket,
      counterState,
      connectionStatus,
      autoConnectStatus,
      autoConnecting,
      selectedTask,
      expectedBluetoothName,
      connectionStatusText,
      phaseLabel,
      sessionStartedAt,
      sessionDurationMin,
      onScanQrCode,
      toggleBleScan,
      onSelectDevice,
      onBoundDeviceTap,
      finishSession,
      useCurrentEquipment,
      simulateTrainingSession,
      isExpectedDevice,
      shortDeviceId,
    };
  },
});
</script>

<style>
.device-binding-page {
  padding: 24rpx;
  background: #f5f5f5;
  min-height: 100vh;
}
.page-header {
  margin-bottom: 24rpx;
}
.page-title {
  font-size: 40rpx;
  font-weight: bold;
  color: #1a1a2e;
}
.task-context-card {
  background: #fff;
  border: 1rpx solid #d6e8ff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
}
.task-context-title {
  display: block;
  color: #4A90E2;
  font-size: 24rpx;
  font-weight: 600;
  margin-bottom: 8rpx;
}
.task-context-name {
  display: block;
  color: #1a1a2e;
  font-size: 34rpx;
  font-weight: 700;
  margin-bottom: 8rpx;
}
.task-context-meta {
  color: #666;
  font-size: 26rpx;
}
.scan-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  display: flex;
  align-items: center;
}
.scan-icon { font-size: 56rpx; margin-right: 20rpx; }
.scan-info { flex: 1; }
.scan-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1a1a2e;
  display: block;
  margin-bottom: 8rpx;
}
.scan-desc { font-size: 26rpx; color: #999; }
.scan-arrow { font-size: 48rpx; color: #ccc; }
.demo-card {
  background: #eef7ff;
  border: 1rpx solid #b9dcff;
  border-radius: 16rpx;
  padding: 24rpx 28rpx;
  margin-bottom: 24rpx;
}
.demo-title {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: #1677d2;
  margin-bottom: 8rpx;
}
.demo-desc {
  display: block;
  font-size: 24rpx;
  color: #4b6f91;
  line-height: 1.5;
}
.resolved-card {
  background: #ffffff;
  border-radius: 16rpx;
  padding: 28rpx 32rpx;
  margin-bottom: 24rpx;
}
.resolved-title {
  font-size: 24rpx;
  color: #888;
  margin-bottom: 8rpx;
}
.resolved-name {
  display: block;
  font-size: 32rpx;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 12rpx;
}
.resolved-metric {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: #0b8f55;
  margin-bottom: 10rpx;
}
.resolved-meta {
  display: block;
  font-size: 24rpx;
  color: #666;
  line-height: 1.8;
}
.imu-debug-panel {
  margin-top: 18rpx;
  padding: 18rpx;
  background: #f7fbff;
  border: 1rpx solid #cfe6ff;
  border-radius: 12rpx;
}
.imu-debug-title {
  display: block;
  font-size: 26rpx;
  font-weight: 600;
  color: #1677d2;
  margin-bottom: 8rpx;
}
.set-summary-list {
  margin-top: 18rpx;
  padding-top: 18rpx;
  border-top: 1rpx solid #eef1f4;
}
.set-summary-title {
  display: block;
  font-size: 24rpx;
  color: #888;
  margin-bottom: 8rpx;
}
.set-summary-item {
  display: block;
  font-size: 24rpx;
  color: #394150;
  line-height: 1.8;
}
.session-actions {
  margin-top: 20rpx;
}
.finish-btn {
  background: #0b8f55;
  color: #fff;
  text-align: center;
  padding: 18rpx 24rpx;
  border-radius: 999rpx;
  font-size: 26rpx;
  font-weight: 600;
}
.simulate-btn {
  background: #eef6ff;
  border: 1rpx solid #b3d4fc;
  color: #2f80d1;
  text-align: center;
  padding: 18rpx 24rpx;
  border-radius: 999rpx;
  font-size: 26rpx;
  font-weight: 600;
}
.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #666;
  margin-bottom: 16rpx;
  margin-top: 8rpx;
}
.ble-scan-btn {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  text-align: center;
  font-size: 30rpx;
  color: #4A90E2;
  margin-bottom: 24rpx;
  border: 2rpx dashed #4A90E2;
}
.ble-scan-btn.scanning {
  background: #f0f7ff;
  color: #666;
  border-color: #999;
}
.scan-result-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;
}
.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}
.result-title { font-size: 28rpx; font-weight: 600; color: #1a1a2e; }
.result-scanning { font-size: 24rpx; color: #4A90E2; }
.device-scan-item {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}
.device-scan-item.matched {
  background: #f0fff7;
  border-radius: 12rpx;
  padding-left: 16rpx;
  padding-right: 16rpx;
}
.device-scan-item:last-child { border-bottom: none; }
.device-scan-icon { font-size: 36rpx; margin-right: 16rpx; }
.device-scan-info { flex: 1; }
.device-scan-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #1a1a2e;
  display: block;
  margin-bottom: 4rpx;
}
.device-scan-code { font-size: 24rpx; color: #999; display: block; }
.device-scan-match {
  display: block;
  font-size: 24rpx;
  color: #0b8f55;
  margin-top: 4rpx;
}
.device-scan-rssi { font-size: 24rpx; color: #bbb; display: block; }
.bind-btn {
  background: #4A90E2;
  color: #fff;
  font-size: 26rpx;
  padding: 10rpx 24rpx;
  border-radius: 24rpx;
}
.scanning-tips {
  text-align: center;
  padding: 16rpx;
  font-size: 24rpx;
  color: #999;
}
.bound-devices-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 20rpx;
  margin-bottom: 24rpx;
}
.device-item {
  display: flex;
  align-items: center;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}
.device-item:last-child { border-bottom: none; }
.device-icon { font-size: 36rpx; margin-right: 16rpx; }
.device-info { flex: 1; }
.device-name { font-size: 30rpx; font-weight: 600; color: #1a1a2e; display: block; }
.device-code { font-size: 24rpx; color: #999; display: block; }
.device-status { font-size: 24rpx; }
.device-status.online { color: #52c41a; }
.device-status.offline { color: #999; }
.bind-result {
  border-radius: 12rpx;
  padding: 24rpx;
  text-align: center;
  font-size: 28rpx;
  margin-bottom: 24rpx;
}
.bind-result.success { background: #f6ffed; color: #52c41a; border: 1rpx solid #b7eb8f; }
.bind-result.error { background: #fff2f0; color: #ff4d4f; border: 1rpx solid #ffccc7; }
.help-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-top: 8rpx;
}
.help-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #1a1a2e;
  display: block;
  margin-bottom: 16rpx;
}
.help-list {}
.help-item {
  font-size: 26rpx;
  color: #666;
  display: block;
  margin-bottom: 10rpx;
  line-height: 1.5;
}
</style>
