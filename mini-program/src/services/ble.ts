/**
 * BLE Service - 微信小程序 GY-BLE25T 接入
 * 负责扫描、连接、订阅通知并解析 IMU 数据。
 */

const DEFAULT_SERVICE_UUID = '0000FFE0-0000-1000-8000-00805F9B34FB';
const DEFAULT_NOTIFY_CHAR_UUID = '0000FFE4-0000-1000-8000-00805F9B34FB';
const DEFAULT_WRITE_CHAR_UUID = '0000FFE3-0000-1000-8000-00805F9B34FB';
const GY25T_FRAME_HEADER = 0xa4;

interface BleDevice {
  deviceId: string;
  name: string;
  localName?: string;
  rssi: number;
  deviceCode?: string;
  advertisServiceUUIDs?: string[];
}

interface BleScanOptions {
  timeout?: number;
  filters?: string[];
}

interface ImuSample {
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

interface GyConnectOptions {
  serviceUuid?: string;
  notifyCharUuid?: string;
  writeCharUuid?: string;
  onSample?: (sample: ImuSample) => void;
  onRaw?: (payload: { hex: string; byteLength: number; timestamp: number }) => void;
}

type BleCallback = (device: BleDevice) => void;

class BleService {
  private isScanning = false;
  private onDeviceFoundCallbacks: BleCallback[] = [];
  private notifyListenerBound = false;
  private activeDeviceId = '';
  private activeServiceUuid = DEFAULT_SERVICE_UUID;
  private activeNotifyCharUuid = DEFAULT_NOTIFY_CHAR_UUID;
  private activeWriteCharUuid = DEFAULT_WRITE_CHAR_UUID;
  private sampleCallback: ((sample: ImuSample) => void) | null = null;
  private rawCallback: GyConnectOptions['onRaw'] = null;
  private scanTimerId: ReturnType<typeof setTimeout> | null = null;
  private commandTimerId: ReturnType<typeof setInterval> | null = null;
  private notifyListener: ((res: WechatMiniprogram.OnBLECharacteristicValueChangeCallbackResult) => void) | null = null;
  private deviceFoundListener: ((res: WechatMiniprogram.OnBluetoothDeviceFoundCallbackResult) => void) | null = null;
  private noDataTimerId: ReturnType<typeof setTimeout> | null = null;
  private lastSampleTimestamp = 0;

  private hasBleApi(): boolean {
    return typeof wx !== 'undefined'
      && typeof wx.openBluetoothAdapter === 'function'
      && typeof wx.getBluetoothAdapterState === 'function'
      && typeof wx.startBluetoothDevicesDiscovery === 'function'
      && typeof wx.onBluetoothDeviceFound === 'function';
  }

  async checkAdapter(): Promise<boolean> {
    if (!this.hasBleApi()) {
      console.warn('[BLE] BLE API is unavailable in current runtime');
      return false;
    }
    try {
      await wx.openBluetoothAdapter();
      const { available } = await wx.getBluetoothAdapterState();
      return available;
    } catch (err) {
      console.error('[BLE] Adapter not available', err);
      return false;
    }
  }

  async startScan(options: BleScanOptions = {}, onFound?: BleCallback): Promise<void> {
    if (!this.hasBleApi()) {
      throw new Error('当前环境不支持蓝牙，请使用真机调试');
    }

    if (this.isScanning) {
      return;
    }
    if (this.scanTimerId) {
      clearTimeout(this.scanTimerId);
      this.scanTimerId = null;
    }

    if (onFound) {
      this.onDeviceFoundCallbacks.push(onFound);
    }

    let adapterOk = await this.checkAdapter();
    if (!adapterOk) {
      await new Promise((resolve) => setTimeout(resolve, 300));
      adapterOk = await this.checkAdapter();
    }
    if (!adapterOk) {
      throw new Error('蓝牙未初始化，请确认手机蓝牙和微信权限已开启');
    }

    this.isScanning = true;
    const timeout = options.timeout || 10000;
    const filters = (options.filters || []).map((item) => item.toLowerCase());

    if (this.deviceFoundListener && typeof wx.offBluetoothDeviceFound === 'function') {
      wx.offBluetoothDeviceFound(this.deviceFoundListener);
    }
    this.deviceFoundListener = (res) => {
      (res.devices || [])
        .map((raw) => this.parseDevice(raw))
        .filter((device): device is BleDevice => !!device)
        .filter((device) => {
          if (filters.length === 0) return true;
          const name = `${device.name || ''} ${device.localName || ''}`.toLowerCase();
          return filters.some((filter) => name.includes(filter));
        })
        .forEach((device) => {
          this.onDeviceFoundCallbacks.forEach((cb) => cb(device));
        });
    };
    wx.onBluetoothDeviceFound(this.deviceFoundListener);

    await wx.startBluetoothDevicesDiscovery({
      allowDuplicatesKey: false,
    });

    this.scanTimerId = setTimeout(() => {
      this.stopScan();
    }, timeout);
  }

  async stopScan(): Promise<void> {
    if (this.scanTimerId) {
      clearTimeout(this.scanTimerId);
      this.scanTimerId = null;
    }
    if (!this.hasBleApi()) {
      return;
    }
    if (!this.isScanning) {
      return;
    }
    try {
      await wx.stopBluetoothDevicesDiscovery();
    } catch (_) {
      // ignore
    }
    if (this.deviceFoundListener && typeof wx.offBluetoothDeviceFound === 'function') {
      wx.offBluetoothDeviceFound(this.deviceFoundListener);
      this.deviceFoundListener = null;
    }
    this.isScanning = false;
    this.onDeviceFoundCallbacks = [];
  }

  async connectGyDevice(deviceId: string, options: GyConnectOptions = {}): Promise<void> {
    if (!this.hasBleApi() || typeof wx.createBLEConnection !== 'function') {
      throw new Error('当前环境不支持蓝牙连接，请使用真机调试');
    }

    const serviceUuid = (options.serviceUuid || DEFAULT_SERVICE_UUID).toUpperCase();
    const notifyCharUuid = (options.notifyCharUuid || DEFAULT_NOTIFY_CHAR_UUID).toUpperCase();
    const writeCharUuid = (options.writeCharUuid || DEFAULT_WRITE_CHAR_UUID).toUpperCase();

    if (this.activeDeviceId) {
      await this.disconnect().catch(() => undefined);
    }

    await this.stopScan().catch(() => undefined);
    await wx.createBLEConnection({ deviceId });
    await this.trySetBleMtu(deviceId);
    await this.delay(500);

    const { services } = await wx.getBLEDeviceServices({ deviceId });
    console.log('[BLE] services', services.map((service) => service.uuid));
    const matchedService = services.find((service) => this.matchUuid(service.uuid, serviceUuid))
      || services.find((service) => this.normalizeUuid(service.uuid).includes('FFE0'));
    if (!matchedService) {
      throw new Error('未找到 GY-BLE25T 服务');
    }

    const { characteristics } = await wx.getBLEDeviceCharacteristics({
      deviceId,
      serviceId: matchedService.uuid,
    });
    console.log('[BLE] characteristics', characteristics.map((item) => ({
      uuid: item.uuid,
      properties: item.properties,
    })));
    const matchedCharacteristic = characteristics.find(
      (item) => this.matchUuid(item.uuid, notifyCharUuid)
    ) || characteristics.find((item) => !!(item.properties.notify || item.properties.indicate));
    if (!matchedCharacteristic) {
      throw new Error('未找到 GY-BLE25T 通知特征');
    }
    const writeCharacteristic = characteristics.find(
      (item) => this.matchUuid(item.uuid, writeCharUuid) && !!item.properties.write
    ) || characteristics.find(
      (item) => !!(item.properties.write || item.properties.writeNoResponse)
    );

    this.activeDeviceId = deviceId;
    this.activeServiceUuid = matchedService.uuid;
    this.activeNotifyCharUuid = matchedCharacteristic.uuid;
    this.activeWriteCharUuid = writeCharacteristic?.uuid || '';
    this.sampleCallback = options.onSample || null;
    this.rawCallback = options.onRaw || null;
    this.bindNotifyListener();

    await wx.notifyBLECharacteristicValueChange({
      deviceId,
      serviceId: matchedService.uuid,
      characteristicId: matchedCharacteristic.uuid,
      state: true,
    });

    if (this.activeWriteCharUuid) {
      this.startGy25tPolling();
    } else {
      console.info('[BLE] GY-BLE25T write characteristic not found; using notify-only mode');
    }
  }

  async disconnect(deviceId?: string): Promise<void> {
    this.clearNoDataTimer();
    const targetDeviceId = deviceId || this.activeDeviceId;
    if (!targetDeviceId) {
      return;
    }

    try {
      if (this.activeServiceUuid && this.activeNotifyCharUuid) {
        await wx.notifyBLECharacteristicValueChange({
          deviceId: targetDeviceId,
          serviceId: this.activeServiceUuid,
          characteristicId: this.activeNotifyCharUuid,
          state: false,
        });
      }
    } catch (_) {
      // ignore
    }

    try {
      await wx.closeBLEConnection({ deviceId: targetDeviceId });
    } catch (_) {
      // ignore
    }

    this.activeDeviceId = '';
    this.activeServiceUuid = DEFAULT_SERVICE_UUID;
    this.activeNotifyCharUuid = DEFAULT_NOTIFY_CHAR_UUID;
    this.activeWriteCharUuid = DEFAULT_WRITE_CHAR_UUID;
    this.sampleCallback = null;
    this.rawCallback = null;
    if (this.commandTimerId) {
      clearInterval(this.commandTimerId);
      this.commandTimerId = null;
    }
    if (this.notifyListener && typeof wx.offBLECharacteristicValueChange === 'function') {
      wx.offBLECharacteristicValueChange(this.notifyListener);
    }
    this.notifyListener = null;
    this.notifyListenerBound = false;
  }

  private clearNoDataTimer(): void {
    if (this.noDataTimerId) {
      clearTimeout(this.noDataTimerId);
      this.noDataTimerId = null;
    }
  }

  private startNoDataTimer(): void {
    this.clearNoDataTimer();
    this.noDataTimerId = setTimeout(() => {
      console.warn('[BLE] No IMU data for 5 minutes, auto disconnecting');
      this.disconnect().catch((err) => {
        console.warn('[BLE] auto disconnect failed', err);
      });
    }, 5 * 60 * 1000);
  }

  private recordSampleReceived(): void {
    this.lastSampleTimestamp = Date.now();
    if (this.activeDeviceId && !this.noDataTimerId) {
      this.startNoDataTimer();
    }
  }

  private bindNotifyListener() {
    if (this.notifyListenerBound) {
      return;
    }
    if (typeof wx.onBLECharacteristicValueChange !== 'function') {
      throw new Error('当前环境不支持蓝牙通知监听，请使用真机调试');
    }
    this.notifyListener = (res) => {
      if (res.deviceId !== this.activeDeviceId) {
        return;
      }
      const raw = this.formatRawPacket(res.value);
      console.log('[BLE] notify raw', raw);
      if (this.rawCallback) {
        this.rawCallback(raw);
      }
      const sample = this.parseImuData(res.value);
      if (sample && this.sampleCallback) {
        this.sampleCallback(sample);
      }
      this.recordSampleReceived();
    };
    wx.onBLECharacteristicValueChange(this.notifyListener);
    this.notifyListenerBound = true;
  }

  private startGy25tPolling() {
    if (this.commandTimerId) {
      clearInterval(this.commandTimerId);
      this.commandTimerId = null;
    }

    const readPoseCommand = this.buildCommand([GY25T_FRAME_HEADER, 0x03, 0x08, 0x1c]);
    this.writeCommand(readPoseCommand).catch((err) => {
      console.warn('[BLE] initial GY-BLE25T command failed', err);
    });
    this.commandTimerId = setInterval(() => {
      this.writeCommand(readPoseCommand).catch((err) => {
        console.warn('[BLE] polling GY-BLE25T command failed', err);
      });
    }, 200);
  }

  private async writeCommand(bytes: number[]): Promise<void> {
    if (!this.activeDeviceId || !this.activeServiceUuid || !this.activeWriteCharUuid) {
      return;
    }
    const buffer = new ArrayBuffer(bytes.length);
    const view = new Uint8Array(buffer);
    bytes.forEach((item, index) => {
      view[index] = item & 0xff;
    });
    console.log('[BLE] write command', bytes.map((item) => item.toString(16).padStart(2, '0')).join(' '));
    await wx.writeBLECharacteristicValue({
      deviceId: this.activeDeviceId,
      serviceId: this.activeServiceUuid,
      characteristicId: this.activeWriteCharUuid,
      value: buffer,
    });
  }

  private buildCommand(bytesWithoutChecksum: number[]): number[] {
    const checksum = bytesWithoutChecksum.reduce((sum, item) => (sum + item) & 0xff, 0);
    return [...bytesWithoutChecksum, checksum];
  }

  private async trySetBleMtu(deviceId: string): Promise<void> {
    const wxApi = wx as typeof wx & {
      setBLEMTU?: (options: { deviceId: string; mtu: number }) => Promise<unknown>;
    };
    if (typeof wxApi.setBLEMTU !== 'function') {
      return;
    }
    try {
      await wxApi.setBLEMTU({ deviceId, mtu: 64 });
    } catch (err) {
      console.warn('[BLE] set MTU ignored', err);
    }
  }

  private delay(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  private normalizeUuid(uuid = ''): string {
    return uuid.replace(/-/g, '').toUpperCase();
  }

  private matchUuid(actual: string, expected: string): boolean {
    const normalizedActual = this.normalizeUuid(actual);
    const normalizedExpected = this.normalizeUuid(expected);
    return normalizedActual === normalizedExpected
      || normalizedActual.includes(normalizedExpected.slice(0, 8));
  }

  private parseDevice(device: WechatMiniprogram.BlueToothDevice): BleDevice | null {
    const name = device.name || device.localName || '';
    if (!name && !device.deviceId) {
      return null;
    }

    return {
      deviceId: device.deviceId,
      name: name || '未知设备',
      localName: device.localName || '',
      rssi: device.RSSI || 0,
      deviceCode: name.toLowerCase().includes('gy_ble25t') ? name : undefined,
      advertisServiceUUIDs: device.advertisServiceUUIDs || [],
    };
  }

  private parseImuData(buffer: ArrayBuffer): ImuSample | null {
    const payload = this.extractImuPayload(buffer);
    if (!payload) {
      return null;
    }

    const bytes = payload;
    return {
      ax: this.readInt16(bytes, 0),
      ay: this.readInt16(bytes, 2),
      az: this.readInt16(bytes, 4),
      gx: this.readInt16(bytes, 6),
      gy: this.readInt16(bytes, 8),
      gz: this.readInt16(bytes, 10),
      roll: this.readInt16(bytes, 12) / 100,
      pitch: this.readInt16(bytes, 14) / 100,
      yaw: this.readInt16(bytes, 16) / 100,
      temp: this.readInt16(bytes, 18) / 100,
      ho: this.readInt16(bytes, 20),
      timestamp: Date.now(),
    };
  }

  private extractImuPayload(buffer: ArrayBuffer): Uint8Array | null {
    if (!buffer || buffer.byteLength < 22) {
      return null;
    }

    const bytes = new Uint8Array(buffer);
    if (bytes.byteLength === 22 && bytes[0] !== GY25T_FRAME_HEADER) {
      return bytes;
    }

    const frameStart = bytes.findIndex((item) => item === GY25T_FRAME_HEADER);
    if (frameStart < 0 || bytes.byteLength - frameStart < 5) {
      return bytes.byteLength >= 22 ? bytes.slice(0, 22) : null;
    }

    const frame = bytes.slice(frameStart);
    const command = frame[1];
    const startReg = frame[2];
    const length = frame[3];
    const frameLength = length + 5;
    if (command !== 0x03 || frame.byteLength < frameLength) {
      return frame.byteLength >= 22 ? frame.slice(0, 22) : null;
    }

    const checksum = frame
      .slice(0, frameLength - 1)
      .reduce((sum, item) => (sum + item) & 0xff, 0);
    if (checksum !== frame[frameLength - 1]) {
      console.warn('[BLE] GY-BLE25T checksum mismatch', checksum, frame[frameLength - 1]);
      return null;
    }

    const dataStart = 4;
    if (startReg === 0x00 && length >= 22) {
      return frame.slice(dataStart, dataStart + 22);
    }

    if (startReg <= 0x08 && startReg + length >= 0x12) {
      const offset = 0x08 - startReg;
      const payload = new Uint8Array(22);
      const poseBytes = frame.slice(dataStart + offset, dataStart + offset + 10);
      payload.set(poseBytes, 12);
      return payload;
    }

    return null;
  }

  private formatRawPacket(buffer: ArrayBuffer): { hex: string; byteLength: number; timestamp: number } {
    const bytes = new Uint8Array(buffer || new ArrayBuffer(0));
    const hex = Array.from(bytes)
      .map((item) => item.toString(16).padStart(2, '0'))
      .join(' ');
    return {
      hex,
      byteLength: bytes.byteLength,
      timestamp: Date.now(),
    };
  }

  private readInt16(bytes: Uint8Array, offset: number): number {
    const value = (bytes[offset] << 8) | bytes[offset + 1];
    return value > 0x7fff ? value - 0x10000 : value;
  }
}

export const bleService = new BleService();
export type { BleDevice, BleScanOptions, GyConnectOptions, ImuSample };
