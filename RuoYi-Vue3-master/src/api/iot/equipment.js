import request from '@/utils/request'

// 查询器械列表
export function listEquipment(query) {
  return request({
    url: '/iot/equipment/list',
    method: 'get',
    params: query
  })
}

// 查询器械详情
export function getEquipment(equipmentId) {
  return request({
    url: '/iot/equipment/' + equipmentId,
    method: 'get'
  })
}

// 根据器械编号查询详情
export function getEquipmentByCode(equipmentCode) {
  return request({
    url: '/iot/equipment/code/' + equipmentCode,
    method: 'get'
  })
}

// 新增器械
export function addEquipment(data) {
  return request({
    url: '/iot/equipment',
    method: 'post',
    data: data
  })
}

// 修改器械
export function updateEquipment(data) {
  return request({
    url: '/iot/equipment',
    method: 'put',
    data: data
  })
}

// 删除器械
export function delEquipment(equipmentId) {
  return request({
    url: '/iot/equipment/' + equipmentId,
    method: 'delete'
  })
}

// 批量删除器械
export function delEquipmentBatch(equipmentIds) {
  return request({
    url: '/iot/equipment/' + equipmentIds.join(','),
    method: 'delete'
  })
}

// 导出器械
export function exportEquipment(query) {
  return request({
    url: '/iot/equipment/export',
    method: 'post',
    params: query,
    responseType: 'blob'
  })
}
