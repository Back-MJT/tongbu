import request from '@/utils/request'

// 获取KPI快照数据
export function getDashboardKpi(params) {
  return request({
    url: '/api/dashboard/kpi',
    method: 'get',
    params: params
  })
}

// 获取趋势数据
export function getDashboardTrends(params) {
  return request({
    url: '/api/dashboard/trends',
    method: 'get',
    params: params
  })
}

// 获取告警列表
export function getDashboardAlerts(params) {
  return request({
    url: '/api/dashboard/alerts',
    method: 'get',
    params: params
  })
}
