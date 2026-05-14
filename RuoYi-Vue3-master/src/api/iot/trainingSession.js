import request from '@/utils/request'

// 查询训练会话列表
export function listTrainingSession(query) {
  return request({
    url: '/iot/training/session/list',
    method: 'get',
    params: query
  })
}

// 查询训练会话详情
export function getTrainingSession(sessionId) {
  return request({
    url: '/iot/training/session/' + sessionId,
    method: 'get'
  })
}

// 查询用户真实训练摘要和个性化生成依据
export function getUserTrainingSummary(userId) {
  return request({
    url: '/iot/training/session/user-summary/' + userId,
    method: 'get'
  })
}
