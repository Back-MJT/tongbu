import request from '@/utils/request'

// ========== MQTT Topic 配置 ==========
export function listMqttTopic(query) {
  return request({
    url: '/iot/config/mqtt/topic/list',
    method: 'get',
    params: query
  })
}

export function getMqttTopic(topicId) {
  return request({
    url: '/iot/config/mqtt/topic/' + topicId,
    method: 'get'
  })
}

export function addMqttTopic(data) {
  return request({
    url: '/iot/config/mqtt/topic',
    method: 'post',
    data: data
  })
}

export function updateMqttTopic(data) {
  return request({
    url: '/iot/config/mqtt/topic',
    method: 'put',
    data: data
  })
}

export function delMqttTopic(topicId) {
  return request({
    url: '/iot/config/mqtt/topic/' + topicId,
    method: 'delete'
  })
}

// ========== 告警规则配置 ==========
export function listAlertRule(query) {
  return request({
    url: '/iot/config/alert/rule/list',
    method: 'get',
    params: query
  })
}

export function getAlertRule(ruleId) {
  return request({
    url: '/iot/config/alert/rule/' + ruleId,
    method: 'get'
  })
}

export function addAlertRule(data) {
  return request({
    url: '/iot/config/alert/rule',
    method: 'post',
    data: data
  })
}

export function updateAlertRule(data) {
  return request({
    url: '/iot/config/alert/rule',
    method: 'put',
    data: data
  })
}

export function delAlertRule(ruleId) {
  return request({
    url: '/iot/config/alert/rule/' + ruleId,
    method: 'delete'
  })
}

export function toggleAlertRule(ruleId, enabled) {
  return request({
    url: '/iot/config/alert/rule/' + ruleId + '/toggle',
    method: 'put',
    data: { enabled }
  })
}

// ========== 数据保留策略 ==========
export function listDataRetention(query) {
  return request({
    url: '/iot/config/retention/list',
    method: 'get',
    params: query
  })
}

export function getDataRetention(id) {
  return request({
    url: '/iot/config/retention/' + id,
    method: 'get'
  })
}

export function updateDataRetention(data) {
  return request({
    url: '/iot/config/retention',
    method: 'put',
    data: data
  })
}

export function executeDataRetention(id) {
  return request({
    url: '/iot/config/retention/' + id + '/execute',
    method: 'post'
  })
}
