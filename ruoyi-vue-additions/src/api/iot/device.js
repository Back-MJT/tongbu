import request from '@/utils/request'

// 查询设备列表
export function listDevice(query) {
  return request({
    url: '/iot/device/list',
    method: 'get',
    params: query
  })
}

// 查询设备详细
export function getDevice(deviceId) {
  return request({
    url: '/iot/device/' + deviceId,
    method: 'get'
  })
}

// 根据设备编号获取详情
export function getDeviceByCode(deviceCode) {
  return request({
    url: '/iot/device/code/' + deviceCode,
    method: 'get'
  })
}

// 新增设备
export function addDevice(data) {
  return request({
    url: '/iot/device',
    method: 'post',
    data: data
  })
}

// 修改设备
export function updateDevice(data) {
  return request({
    url: '/iot/device',
    method: 'put',
    data: data
  })
}

// 删除设备
export function delDevice(deviceId) {
  return request({
    url: '/iot/device/' + deviceId,
    method: 'delete'
  })
}

// 批量删除设备
export function delDeviceBatch(deviceIds) {
  return request({
    url: '/iot/device/' + deviceIds.join(','),
    method: 'delete'
  })
}

// 导出设备
export function exportDevice(query) {
  return request({
    url: '/iot/device/export',
    method: 'post',
    params: query,
    responseType: 'blob'
  })
}

// 修改设备状态
export function changeDeviceStatus(deviceId, status) {
  return request({
    url: '/iot/device/status/' + deviceId,
    method: 'put',
    data: { status }
  })
}

// 查询厂商下设备数量
export function countDeviceByManufacturer(manufacturerId) {
  return request({
    url: '/iot/device/count/' + manufacturerId,
    method: 'get'
  })
}
