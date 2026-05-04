import request from '@/utils/request'

// 查询设备日志列表
export function listDeviceLog(query) {
  return request({
    url: '/iot/log/list',
    method: 'get',
    params: query
  })
}

// 查询设备日志详细
export function getDeviceLog(logId) {
  return request({
    url: '/iot/log/' + logId,
    method: 'get'
  })
}

// 删除设备日志
export function delDeviceLog(logId) {
  return request({
    url: '/iot/log/' + logId,
    method: 'delete'
  })
}

// 批量删除设备日志
export function delDeviceLogBatch(logIds) {
  return request({
    url: '/iot/log/' + logIds.join(','),
    method: 'delete'
  })
}

// 导出设备日志
export function exportDeviceLog(query) {
  return request({
    url: '/iot/log/export',
    method: 'post',
    params: query,
    responseType: 'blob'
  })
}
