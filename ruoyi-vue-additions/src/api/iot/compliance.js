import request from '@/utils/request'

// 查询合规用户列表
export function listComplianceUsers(query) {
  return request({
    url: '/api/compliance/users',
    method: 'get',
    params: query
  })
}

// 获取合规告警列表
export function listComplianceAlerts(query) {
  return request({
    url: '/api/compliance/alerts',
    method: 'get',
    params: query
  })
}

// 记录干预执行
export function recordExecution(data) {
  return request({
    url: '/api/compliance/execution',
    method: 'post',
    data: data
  })
}
