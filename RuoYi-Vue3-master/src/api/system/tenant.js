/**
 * 租户管理 API
 * 与后端 /system/tenant 接口对接
 */
import request from '@/utils/request'

// 获取租户列表（当前用户可访问的租户）
export function getTenantList() {
  return request({
    url: '/system/tenant/list',
    method: 'get'
  })
}

// 获取当前租户信息
export function getCurrentTenant() {
  return request({
    url: '/system/tenant/current',
    method: 'get'
  })
}

// 切换租户
export function switchTenant(tenantId) {
  return request({
    url: '/system/tenant/switch',
    method: 'put',
    data: { tenantId }
  })
}

// 校验用户是否有权访问指定租户
export function checkTenantAccess(tenantId) {
  return request({
    url: `/system/tenant/check/${tenantId}`,
    method: 'get'
  })
}
