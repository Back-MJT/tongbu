import request from '@/utils/request'

// 查询训练完成率统计
export function getTrainingCompletionRates(params) {
  return request({
    url: '/iot/training/completion-rates',
    method: 'get',
    params: params
  })
}

// 查询AI训练计划采纳率指标
export function getAiPlanAdoption(params) {
  return request({
    url: '/iot/training/ai-plan-adoption',
    method: 'get',
    params: params
  })
}

// 查询用户参与度指标（日活、平均会话时长、留存率）
export function getUserEngagement(params) {
  return request({
    url: '/iot/training/user-engagement',
    method: 'get',
    params: params
  })
}

// 查询运动类型分布
export function getExerciseDistribution(params) {
  return request({
    url: '/iot/training/exercise-distribution',
    method: 'get',
    params: params
  })
}

// 查询厂商训练指标对比
export function getManufacturerComparison(params) {
  return request({
    url: '/iot/training/manufacturer-comparison',
    method: 'get',
    params: params
  })
}

// 查询实时训练状态
export function getRealtimeWorkoutStatus(params) {
  return request({
    url: '/iot/training/realtime-status',
    method: 'get',
    params: params
  })
}

// 查询训练趋势（按设备/厂商）
export function getTrainingTrends(params) {
  return request({
    url: '/iot/training/trends',
    method: 'get',
    params: params
  })
}

// 查询每日训练汇总
export function getDailyTrainingSummary(params) {
  return request({
    url: '/iot/training/daily-summary',
    method: 'get',
    params: params
  })
}

// 导出训练分析报告
export function exportTrainingReport(params) {
  return request({
    url: '/iot/training/export',
    method: 'post',
    params: params,
    responseType: 'blob'
  })
}
