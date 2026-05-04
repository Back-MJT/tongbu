import request from '@/utils/request'

// 获取设备最新IMU数据（从Redis缓存）
export function getLatestImuData(deviceCode) {
  return request({
    url: '/iot/imu/latest/' + deviceCode,
    method: 'get'
  })
}

// 获取设备缓存状态
export function getImuStatus(deviceCode) {
  return request({
    url: '/iot/imu/status/' + deviceCode,
    method: 'get'
  })
}

// MQTT连接状态检查
export function getMqttHealth() {
  return request({
    url: '/iot/imu/mqtt/health',
    method: 'get'
  })
}

// 查询设备最近N条IMU历史记录
export function getImuHistory(deviceCode, limit) {
  return request({
    url: '/iot/imu/history/' + deviceCode,
    method: 'get',
    params: { limit }
  })
}

// 按时间范围查询设备IMU历史数据
export function getImuHistoryByRange(deviceCode, beginTime, endTime) {
  return request({
    url: '/iot/imu/history/' + deviceCode + '/range',
    method: 'get',
    params: { beginTime, endTime }
  })
}

// 查询设备IMU数据统计
export function getImuStats(deviceCode, beginTime, endTime) {
  return request({
    url: '/iot/imu/history/' + deviceCode + '/stats',
    method: 'get',
    params: { beginTime, endTime }
  })
}
