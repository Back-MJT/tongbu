import request from '@/utils/request'

// 查询厂商列表
export function listManufacturer(query) {
  return request({
    url: '/iot/manufacturer/list',
    method: 'get',
    params: query
  })
}

// 查询厂商详细
export function getManufacturer(manufacturerId) {
  return request({
    url: '/iot/manufacturer/' + manufacturerId,
    method: 'get'
  })
}

// 新增厂商
export function addManufacturer(data) {
  return request({
    url: '/iot/manufacturer',
    method: 'post',
    data: data
  })
}

// 修改厂商
export function updateManufacturer(data) {
  return request({
    url: '/iot/manufacturer',
    method: 'put',
    data: data
  })
}

// 删除厂商
export function delManufacturer(manufacturerId) {
  return request({
    url: '/iot/manufacturer/' + manufacturerId,
    method: 'delete'
  })
}

// 批量删除厂商
export function delManufacturerBatch(manufacturerIds) {
  return request({
    url: '/iot/manufacturer/' + manufacturerIds.join(','),
    method: 'delete'
  })
}

// 导出厂商
export function exportManufacturer(query) {
  return request({
    url: '/iot/manufacturer/export',
    method: 'post',
    params: query,
    responseType: 'blob'
  })
}

// 获取厂商详情（含统计数据）
export function getManufacturerDetail(manufacturerId) {
  return request({
    url: '/iot/manufacturer/detail/' + manufacturerId,
    method: 'get'
  })
}

// 生成/重置厂商API密钥
export function resetManufacturerApiKey(manufacturerId) {
  return request({
    url: '/iot/manufacturer/apikey/' + manufacturerId,
    method: 'put'
  })
}

// 更新厂商通知设置
export function updateManufacturerSettings(manufacturerId, data) {
  return request({
    url: '/iot/manufacturer/settings/' + manufacturerId,
    method: 'put',
    data: data
  })
}

// 获取厂商设备列表
export function getManufacturerEquipment(manufacturerId, params) {
  return request({
    url: '/api/manufacturer/' + manufacturerId + '/equipment',
    method: 'get',
    params: params
  })
}

// 获取厂商产线列表
export function getManufacturerLines(manufacturerId, params) {
  return request({
    url: '/api/manufacturer/' + manufacturerId + '/lines',
    method: 'get',
    params: params
  })
}
