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
        <text class="scan-desc">扫描器械二维码后，自动匹配绑定传感器并开始记录</text>
      </view>
      <text class="scan-arrow">›</text>
    </view>

    <view class="demo-card" @tap="useCurrentEquipment">
      <text class="demo-title">使用当前器械</text>
      <text class="demo-desc">现场暂未扫码时，可先使用默认器械继续完成训练记录</text>
    </view>

    <view v-if="resolvedEquipment" class="resolved-card">
      <view class="resolved-head">
        <view>
          <view class="resolved-title">当前器械</view>
          <text class="resolved-name">{{ resolvedEquipment.equipmentName }}</text>
          <text class="resolved-meta">编号 {{ resolvedEquipment.equipmentCode }} · 蓝牙 {{ resolvedEquipment.bluetoothName || '-' }}</text>
        </view>
        <view class="status-pill" :class="connectionStatus">
          <text>{{ connectionStatusText }}</text>
        </view>
      </view>

      <text v-if="autoConnectStatus" class="auto-connect-text">{{ autoConnectStatus }}</text>

      <view class="live-panel">
        <view class="live-stat primary">
          <text class="live-value large">{{ counterState?.reps || 0 }}</text>
          <text class="live-label">累计次数</text>
        </view>
        <view class="live-stat">
          <text class="live-value">{{ counterState?.sets || 0 }}</text>
          <text class="live-label">完成组数</text>
        </view>
        <view class="live-stat">
          <text class="live-value">{{ counterState?.currentSetReps || 0 }}</text>
          <text class="live-label">当前组</text>
        </view>
      </view>

      <!-- 连接状态详情 -->
      <view class="connection-detail" v-if="connectionStatus !== 'connected'">
        <view class="conn-state-item" :class="connectionStatus">
          <text class="conn-state-dot"></text>
          <text class="conn-state-label">{{ connectionStatusText }}</text>
        </view>
        <view class="conn-state-hint" v-if="connectionStatus === 'idle'">
          <text>请先扫码器械二维码，系统将自动连接绑定传感器</text>
        </view>
        <view class="conn-state-hint" v-if="connectionStatus === 'connecting'">
          <text>正在建立蓝牙连接，请保持手机靠近传感器...</text>
        </view>
        <view class="conn-state-hint error" v-if="connectionStatus === 'failed'">
          <text>连接失败：请确保传感器已通电、手机蓝牙已开启，并靠近器械后重试</text>
          <view class="conn-retry-btn" @tap="retryConnection">重新连接</view>
        </view>
      </view>

      <view class="motion-card">
        <view class="motion-header">
          <text class="motion-title">姿态曲线</text>
          <text class="motion-state">{{ latestSample ? phaseLabel : '等待数据' }}</text>
        </view>
        <view class="motion-chart">
          <view class="chart-grid top"></view>
          <view class="chart-grid mid"></view>
          <view class="chart-grid bottom"></view>
          <view
            v-for="(point, idx) in imuChartPoints"
            :key="idx"
            class="chart-point"
            :style="{ left: point.left + '%', top: point.top + '%' }"
          ></view>
        </view>
        <view class="motion-meta">
          <text>主轴 {{ resolvedEquipment.countingConfig?.mainAxis || 'pitch' }}</text>
          <text>{{ counterState ? counterState.axisValue.toFixed(1) : '--' }}°</text>
          <text>幅度 {{ counterState ? counterState.rangeValue.toFixed(1) : '--' }}</text>
        </view>
      </view>

      <view class="session-strip">
        <text>通知包 {{ rawNotifyCount }} 个</text>
        <text>{{ latestSample ? `roll/pitch/yaw ${latestSample.roll.toFixed(1)} / ${latestSample.pitch.toFixed(1)} / ${latestSample.yaw.toFixed(1)}` : '等待姿态数据' }}</text>
        <text v-if="sessionStartedAt">已训练 {{ sessionDurationMin }} 分钟</text>
        <text v-if="counterState && counterState.restMs > 0">休息 {{ Math.round(counterState.restMs / 1000) }} 秒</text>
      </view>

      <view v-if="counterState && counterState.setSummaries.length > 0" class="set-summary-list">
        <text class="set-summary-title">已识别训练组</text>
        <view
          v-for="set in counterState.setSummaries"
          :key="set.setNo"
          class="set-summary-item"
        >
          <text>第{{ set.setNo }}组</text>
          <text>{{ set.reps }}次</text>
          <text>{{ set.durationSec }}秒</text>
        </view>
      </view>
      <view v-if="connectionStatus === 'connected'" class="session-actions">
        <view class="finish-btn" @tap="finishSession">结束训练并保存</view>
      </view>
      <view v-if="connectionStatus !== 'connected'" class="session-actions">
        <view class="simulate-btn" @tap="simulateTrainingSession">暂不连接，手动记录一组</view>
      </view>
    </view>

    <!-- BLE扫描入口 -->
    <view class="section-title">传感器连接</view>

    <!-- BLE扫描按钮 -->
    <view class="ble-scan-btn" @tap="toggleBleScan" :class="{ scanning: isScanning }">
      <text v-if="!isScanning">🔍 重新搜索绑定传感器</text>
      <text v-else>⏳ 正在查找 {{ expectedSensorLabel || '绑定传感器' }}... (点击停止)</text>
    </view>

    <!-- 扫描结果列表 -->
    <view v-if="isScanning || foundDevices.length > 0" class="scan-result-card">
      <view class="result-header">
        <text class="result-title">附近传感器：找到 {{ foundDevices.length }} 个设备</text>
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
        <text>若列表为空，请检查手机蓝牙、微信权限和传感器供电，并尽量靠近器械。</text>
      </view>
    </view>

    <!-- 已绑定设备 -->
    <view class="section-title" v-if="myDevices.length > 0">已绑定设备</view>
    <view v-if="myDevices.length > 0" class="bound-devices-card">
      <view
        v-for="(device, idx) in myDevices"
        :key="idx"
        class="device-item-wrap"
        :class="{ swiped: swipeDeviceId === device.deviceCode }"
      >
        <view
          class="device-item"
          :class="{ online: device.status === 'online' }"
          @tap="onBoundDeviceTap(device)"
          @touchstart="onTouchStart"
          @touchend="onTouchEnd(device.deviceCode)"
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
        <view class="device-delete-btn" @tap="onDeleteBoundDevice(device)">
          <text>删除</text>
        </view>
      </view>
    </view>

    <!-- 绑定结果提示 -->
    <view v-if="bindResult" class="bind-result" :class="bindResult.type">
      <text>{{ bindResult.message }}</text>
    </view>

    <!-- 帮助说明 -->
    <view class="help-card">
      <text class="help-title">连接不上？</text>
      <view class="help-list">
        <text class="help-item">1. 确保设备已通电并处于广播状态</text>
        <text class="help-item">2. 确保手机蓝牙已开启</text>
        <text class="help-item">3. 尝试靠近设备后重新扫描</text>
        <text class="help-item">4. 建议先扫码器械，再连接对应传感器</text>
      </view>
    </view>
  </view>
</template>

<script>
import { defineComponent, ref, computed, onMounted, onUnmounted } from 'vue';
import Taro from '@tarojs/taro';
import {
  getMyDevices,
  heartbeatEquipment,
  normalizeScanCode,
  occupyEquipment,
  releaseEquipment,
  resolveEquipment,
  submitTrainingSession,
} from '../../services/api';
import { bleService } from '../../services/ble';
import { ImuCounterService } from '../../services/counter';

export default defineComponent({
  setup() {
    const isScanning = ref(false);
    const foundDevices = ref([]);
    const myDevices = ref([]);
    const swipeDeviceId = ref(null);
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
    const activeUsage = ref(null);
    const sessionStartedAt = ref(0);
    const sessionElapsedMs = ref(0);
    const imuSeries = ref([]);
    const counter = new ImuCounterService();
    let scanTimer = null;
    let sessionTimer = null;
    let heartbeatTimer = null;
    let scanCooldown = false;
    let touchStartX = 0;

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
          expectedEquipmentCode: params.expectedEquipmentCode || '',
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
      if (heartbeatTimer) clearInterval(heartbeatTimer);
      bleService.stopScan();
      bleService.disconnect().catch(() => undefined);
      releaseCurrentUsage().catch(() => undefined);
    });

    async function onScanQrCode() {
      console.log('[DeviceBinding] scan tapped');
      if (scanCooldown) {
        wx.showToast({ title: '请稍候再扫码', icon: 'none' });
        return;
      }
      scanCooldown = true;
      setTimeout(() => { scanCooldown = false; }, 2000);
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
        if (!validateScannedEquipment(resolved.data)) {
          return;
        }
        await switchToEquipment(resolved.data);
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
        if (!validateScannedEquipment(resolved.data)) {
          return;
        }
        await switchToEquipment(resolved.data, autoScanBle);
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

    function onTouchStart(e) {
      touchStartX = e.touches[0].clientX;
    }

    function onTouchEnd(deviceCode, e) {
      const diff = touchStartX - (e.changedTouches[0]?.clientX || 0);
      if (diff > 60) {
        swipeDeviceId.value = deviceCode;
      }
    }

    async function onDeleteBoundDevice(device) {
      const confirm = await new Promise((resolve) => {
        wx.showModal({
          title: '确认删除',
          content: `确定要删除 ${device.deviceName || device.deviceCode} 吗？`,
          success: (res) => resolve(res.confirm),
        });
      });
      if (!confirm) return;
      try {
        await unbindDevice(device.bindingId);
        myDevices.value = myDevices.value.filter(d => d.bindingId !== device.bindingId);
        wx.showToast({ title: '已删除', icon: 'success' });
        swipeDeviceId.value = null;
      } catch (e) {
        wx.showToast({ title: '删除失败', icon: 'none' });
      }
    }

    async function retryConnection() {
      connectionStatus.value = 'idle';
      if (resolvedEquipment.value) {
        await startBleScan(true);
      } else {
        wx.showToast({ title: '请先扫码器械', icon: 'none' });
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
        ? `正在查找 ${expectedSensorLabel.value || '绑定传感器'}`
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

    function validateScannedEquipment(equipment) {
      const expectedCode = selectedTask.value?.expectedEquipmentCode;
      if (!expectedCode || !equipment?.equipmentCode || expectedCode === equipment.equipmentCode) {
        return true;
      }
      wx.showModal({
        title: '器械不匹配',
        content: `当前任务需要扫描 ${expectedCode}，你扫描的是 ${equipment.equipmentCode}。请扫描任务对应器械二维码。`,
        showCancel: false,
        confirmText: '重新扫码',
      });
      bindResult.value = { type: 'error', message: '扫码器械与当前训练任务不匹配' };
      return false;
    }

    async function switchToEquipment(equipment, autoScanBle = true) {
      const previousEquipment = resolvedEquipment.value;
      const isDifferentEquipment = previousEquipment
        && previousEquipment.equipmentCode
        && equipment?.equipmentCode
        && previousEquipment.equipmentCode !== equipment.equipmentCode;

      if (isDifferentEquipment || connectionStatus.value === 'connected' || connectionStatus.value === 'connecting') {
        autoConnectStatus.value = `正在切换到 ${equipment.equipmentName || equipment.equipmentCode}`;
        await stopCurrentBleSession();
      }

      const usage = await occupyResolvedEquipment(equipment);
      if (!usage) {
        return;
      }
      applyResolvedEquipment(equipment);
      activeUsage.value = usage;
      startUsageHeartbeat();
      if (autoScanBle) {
        await startBleScan(true);
      }
    }

    async function stopCurrentBleSession() {
      stopBleScan();
      if (sessionTimer) clearInterval(sessionTimer);
      sessionTimer = null;
      sessionStartedAt.value = 0;
      sessionElapsedMs.value = 0;
      connectionStatus.value = 'idle';
      autoConnecting.value = false;
      await bleService.disconnect().catch((err) => {
        console.warn('[DeviceBinding] disconnect before switch ignored', err);
      });
      await releaseCurrentUsage();
    }

    async function occupyResolvedEquipment(equipment) {
      if (!equipment?.equipmentCode) {
        wx.showToast({ title: '器械编号缺失，请重新扫码', icon: 'none' });
        return null;
      }
      wx.showLoading({ title: '确认器械占用...' });
      try {
        const res = await occupyEquipment({
          equipmentCode: equipment.equipmentCode,
          venueId: equipment.venueId,
          taskId: selectedTask.value?.taskId,
        });
        wx.hideLoading();
        return res.data;
      } catch (e) {
        wx.hideLoading();
        console.error('[DeviceBinding] occupy equipment failed', e);
        bindResult.value = { type: 'error', message: e.message || '器械正在使用中，请稍后再试' };
        wx.showToast({ title: e.message || '器械正在使用中', icon: 'none' });
        return null;
      }
    }

    function startUsageHeartbeat() {
      if (heartbeatTimer) clearInterval(heartbeatTimer);
      heartbeatTimer = setInterval(async () => {
        const usage = activeUsage.value;
        const equipment = resolvedEquipment.value;
        if (!usage?.usageSessionId || !equipment?.equipmentCode) {
          return;
        }
        try {
          await heartbeatEquipment({
            equipmentCode: equipment.equipmentCode,
            usageSessionId: usage.usageSessionId,
          });
        } catch (e) {
          console.warn('[DeviceBinding] heartbeat failed', e);
          bindResult.value = { type: 'error', message: '器械占用已失效，请重新扫码连接' };
          stopCurrentBleSession();
        }
      }, 30000);
    }

    async function releaseCurrentUsage() {
      if (heartbeatTimer) {
        clearInterval(heartbeatTimer);
        heartbeatTimer = null;
      }
      const usage = activeUsage.value;
      const equipment = resolvedEquipment.value;
      activeUsage.value = null;
      if (!usage?.usageSessionId || !equipment?.equipmentCode) {
        return;
      }
      try {
        await releaseEquipment({
          equipmentCode: equipment.equipmentCode,
          usageSessionId: usage.usageSessionId,
        });
      } catch (e) {
        console.warn('[DeviceBinding] release usage ignored', e);
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
            appendImuPoint(sample);
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
      if (swipeDeviceId.value === device.deviceCode) {
        swipeDeviceId.value = null;
        return;
      }
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
          usageSessionId: activeUsage.value?.usageSessionId,
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
        await releaseCurrentUsage();
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
        wx.showToast({ title: '请先扫码或使用当前器械', icon: 'none' });
        return;
      }

      const now = Date.now();
      const simulatedSets = [
        { setNo: 1, reps: Number(selectedTask.value?.targetReps || 12), durationSec: 45 },
      ];
      wx.showLoading({ title: '保存训练记录...' });
      try {
        const result = await submitTrainingSession({
          equipmentCode: resolvedEquipment.value.equipmentCode,
          deviceCode: resolvedEquipment.value.deviceCode,
          usageSessionId: activeUsage.value?.usageSessionId,
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
          message: `训练记录已保存，记录编号 ${result.data.sessionId}`,
        };
        showSavedActions();
      } catch (e) {
        wx.hideLoading();
        console.error('[DeviceBinding] simulate session failed', e);
        wx.showToast({ title: '训练记录保存失败', icon: 'none' });
      }
    }

    function showSavedActions(sessionData) {
      const data = sessionData || {
        exerciseName: selectedTask.value?.exerciseName,
        completedSets: counterState.value?.sets || 0,
        totalReps: counterState.value?.reps || 0,
        durationMin: sessionDurationMin.value,
        sets: counterState.value?.setSummaries || [],
      };
      wx.navigateTo({
        url: `/pages/training-result/index?data=${encodeURIComponent(JSON.stringify(data))}`,
        fail: () => {
          wx.switchTab({ url: '/pages/progress/index' });
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

    const imuChartPoints = computed(() => {
      const points = imuSeries.value;
      if (!points.length) {
        return [];
      }
      const min = Math.min(...points);
      const max = Math.max(...points);
      const span = Math.max(1, max - min);
      const denominator = Math.max(1, points.length - 1);
      return points.map((value, index) => ({
        left: Math.round((index / denominator) * 1000) / 10,
        top: Math.max(6, Math.min(88, Math.round((1 - (value - min) / span) * 82 + 6))),
      }));
    });

    const expectedBluetoothName = computed(() => {
      return resolvedEquipment.value?.bluetoothName || resolvedEquipment.value?.deviceName || '';
    });

    const expectedSensorLabel = computed(() => {
      if (!resolvedEquipment.value) return '';
      return expectedBluetoothName.value || resolvedEquipment.value.deviceCode || `设备ID ${resolvedEquipment.value.deviceId}`;
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
      imuSeries.value = [];
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
      const deviceId = normalizeDeviceName(device.deviceId || '');
      let score = 0;

      if (expectedName && deviceName === expectedName) score = Math.max(score, 100);
      if (expectedName && deviceName.includes(expectedName)) score = Math.max(score, 90);
      if (expectedDeviceCode && deviceName.includes(expectedDeviceCode)) score = Math.max(score, 95);
      if (expectedDeviceCode && deviceId.includes(expectedDeviceCode)) score = Math.max(score, 90);
      if (expectedDeviceCode && normalizeDeviceName(device.deviceCode || '').includes(expectedDeviceCode)) score = Math.max(score, 95);

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

    function appendImuPoint(sample) {
      const axis = resolvedEquipment.value?.countingConfig?.mainAxis || 'pitch';
      const value = Number(sample?.[axis] ?? sample?.pitch ?? 0);
      const next = imuSeries.value.concat(Number.isFinite(value) ? value : 0);
      imuSeries.value = next.slice(-28);
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
      imuChartPoints,
      connectionStatus,
      autoConnectStatus,
      autoConnecting,
      selectedTask,
      activeUsage,
      expectedBluetoothName,
      expectedSensorLabel,
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
      retryConnection,
      swipeDeviceId,
      onDeleteBoundDevice,
    };
  },
});
</script>

<style>
.device-binding-page {
  padding: 28rpx 24rpx 48rpx;
  background: #f4f7fb;
  min-height: 100vh;
  box-sizing: border-box;
}
.page-header {
  margin-bottom: 24rpx;
}
.page-title {
  font-size: 40rpx;
  font-weight: 800;
  color: #172033;
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
  border: 1rpx solid #edf1f6;
  box-shadow: 0 10rpx 28rpx rgba(20, 38, 70, 0.04);
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
  border-radius: 20rpx;
  padding: 28rpx 32rpx;
  margin-bottom: 24rpx;
  border: 1rpx solid #edf1f6;
  box-shadow: 0 16rpx 40rpx rgba(20, 38, 70, 0.06);
}
.resolved-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20rpx;
  margin-bottom: 18rpx;
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
.auto-connect-text {
  display: block;
  padding: 14rpx 18rpx;
  border-radius: 12rpx;
  background: #f0f7ff;
  color: #2563eb;
  font-size: 24rpx;
  line-height: 1.5;
  margin-bottom: 18rpx;
}
.status-pill {
  flex: 0 0 auto;
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: #f1f5f9;
  color: #64748b;
  font-size: 24rpx;
  font-weight: 700;
}
.status-pill.connected {
  background: #e9fbf2;
  color: #0b8f55;
}
.status-pill.connecting {
  background: #fff7e6;
  color: #b56a00;
}
.status-pill.failed {
  background: #fff1f0;
  color: #d93025;
}
.live-panel {
  display: flex;
  gap: 14rpx;
  margin-bottom: 20rpx;
}
.live-stat {
  flex: 1;
  min-width: 0;
  padding: 22rpx 12rpx;
  border-radius: 16rpx;
  background: #f7fafc;
  text-align: center;
  border: 1rpx solid #edf1f6;
}
.live-stat.primary {
  background: #eef7ff;
  border-color: #d5e9ff;
}
.live-value {
  display: block;
  color: #172033;
  font-size: 44rpx;
  font-weight: 800;
  line-height: 1.1;
}
.live-value.large {
  font-size: 80rpx;
}
.live-stat.primary .live-value {
  color: #2563eb;
}
.connection-detail {
  background: #f8fafc;
  border-radius: 14rpx;
  padding: 20rpx 24rpx;
  margin-bottom: 18rpx;
  border: 1rpx solid #edf1f6;
}
.conn-state-item {
  display: flex;
  align-items: center;
  gap: 10rpx;
  margin-bottom: 10rpx;
}
.conn-state-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: #ccc;
}
.conn-state-item.idle .conn-state-dot { background: #ccc; }
.conn-state-item.connecting .conn-state-dot { background: #ffa500; animation: pulse 1s infinite; }
.conn-state-item.failed .conn-state-dot { background: #ff4d4f; }
.conn-state-item.connected .conn-state-dot { background: #52c41a; }
.conn-state-label {
  font-size: 26rpx;
  font-weight: 600;
  color: #1a1a2e;
}
.conn-state-item.connecting .conn-state-label { color: #b56a00; }
.conn-state-item.failed .conn-state-label { color: #d93025; }
.conn-state-item.connected .conn-state-label { color: #0b8f55; }
.conn-state-hint {
  font-size: 24rpx;
  color: #666;
  line-height: 1.5;
}
.conn-state-hint.error { color: #d93025; }
.conn-retry-btn {
  margin-top: 12rpx;
  display: inline-block;
  background: #ff4d4f;
  color: #fff;
  font-size: 24rpx;
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
.live-label {
  display: block;
  margin-top: 8rpx;
  color: #7b8794;
  font-size: 22rpx;
}
.motion-card {
  padding: 22rpx;
  border-radius: 18rpx;
  background: #101828;
  margin-bottom: 18rpx;
}
.motion-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18rpx;
}
.motion-title {
  color: #fff;
  font-size: 28rpx;
  font-weight: 700;
}
.motion-state {
  color: #9ee7d5;
  font-size: 26rpx;
}
.motion-chart {
  position: relative;
  height: 190rpx;
  border-radius: 14rpx;
  overflow: hidden;
  background: linear-gradient(180deg, #17233a 0%, #111827 100%);
}
.chart-grid {
  position: absolute;
  left: 0;
  right: 0;
  height: 1rpx;
  background: rgba(255,255,255,0.12);
}
.chart-grid.top { top: 24%; }
.chart-grid.mid { top: 50%; }
.chart-grid.bottom { top: 76%; }
.chart-point {
  position: absolute;
  width: 10rpx;
  height: 10rpx;
  margin-left: -5rpx;
  margin-top: -5rpx;
  border-radius: 50%;
  background: #4ade80;
  box-shadow: 0 0 14rpx rgba(74, 222, 128, 0.7);
}
.motion-meta {
  display: flex;
  justify-content: space-between;
  gap: 12rpx;
  margin-top: 16rpx;
  color: #c8d3e1;
  font-size: 22rpx;
}
.session-strip {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  padding: 18rpx;
  border-radius: 14rpx;
  background: #f8fafc;
  color: #5d6b7c;
  font-size: 23rpx;
  line-height: 1.35;
  margin-bottom: 18rpx;
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
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 24rpx;
  color: #394150;
  padding: 14rpx 0;
  border-bottom: 1rpx solid #f0f3f7;
}
.set-summary-item:last-child {
  border-bottom: 0;
}
.session-actions {
  margin-top: 20rpx;
}
.finish-btn {
  background: #13a36f;
  color: #fff;
  text-align: center;
  padding: 18rpx 24rpx;
  border-radius: 999rpx;
  font-size: 26rpx;
  font-weight: 600;
  box-shadow: 0 12rpx 24rpx rgba(19, 163, 111, 0.18);
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
.device-item-wrap {
  position: relative;
  overflow: hidden;
  border-radius: 12rpx;
}
.device-item-wrap.swiped .device-item {
  transform: translateX(-120rpx);
  transition: transform 0.2s;
}
.device-delete-btn {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 120rpx;
  background: #ff4d4f;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  border-radius: 0 12rpx 12rpx 0;
}
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
