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
