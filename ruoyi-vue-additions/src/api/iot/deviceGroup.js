import request from '@/utils/request'

// 查询设备分组列表
export function listDeviceGroup(query) {
  return request({
    url: '/iot/group/list',
    method: 'get',
    params: query
  })
}

// 查询设备分组详细
export function getDeviceGroup(groupId) {
  return request({
    url: '/iot/group/' + groupId,
    method: 'get'
  })
}

// 新增设备分组
export function addDeviceGroup(data) {
  return request({
    url: '/iot/group',
    method: 'post',
    data: data
  })
}

// 修改设备分组
export function updateDeviceGroup(data) {
  return request({
    url: '/iot/group',
    method: 'put',
    data: data
  })
}

// 删除设备分组
export function delDeviceGroup(groupId) {
  return request({
    url: '/iot/group/' + groupId,
    method: 'delete'
  })
}

// 批量删除设备分组
export function delDeviceGroupBatch(groupIds) {
  return request({
    url: '/iot/group/' + groupIds.join(','),
    method: 'delete'
  })
}

// 添加设备到分组
export function addDeviceToGroup(groupId, deviceId) {
  return request({
    url: '/iot/group/device/' + groupId + '/' + deviceId,
    method: 'post'
  })
}

// 从分组移除设备
export function removeDeviceFromGroup(groupId, deviceId) {
  return request({
    url: '/iot/group/device/' + groupId + '/' + deviceId,
    method: 'delete'
  })
}

// 查询分组下所有设备ID
export function getGroupDevices(groupId) {
  return request({
    url: '/iot/group/devices/' + groupId,
    method: 'get'
  })
}

// 查询设备所属分组
export function getDeviceGroups(deviceId) {
  return request({
    url: '/iot/group/device/' + deviceId + '/groups',
    method: 'get'
  })
}
