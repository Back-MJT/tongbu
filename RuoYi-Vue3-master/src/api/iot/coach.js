import request from '@/utils/request'

// 查询教练用户列表
export function listCoachUsers(query) {
  return request({
    url: '/api/coach/users',
    method: 'get',
    params: query
  })
}

// 获取用户健康档案
export function getUserProfile(userId) {
  return request({
    url: '/api/coach/users/' + userId + '/profile',
    method: 'get'
  })
}

// 获取用户处方列表
export function getUserPrescriptions(userId) {
  return request({
    url: '/api/coach/users/' + userId + '/prescriptions',
    method: 'get'
  })
}

// 更新处方状态
export function updatePrescription(prescriptionId, data) {
  return request({
    url: '/api/coach/prescriptions/' + prescriptionId,
    method: 'put',
    data: data
  })
}
