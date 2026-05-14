import request from '@/utils/request'

export function listTrainingPlan(query) {
  return request({
    url: '/iot/training/plan/list',
    method: 'get',
    params: query
  })
}

export function getTrainingPlan(prescriptionId) {
  return request({
    url: '/iot/training/plan/' + prescriptionId,
    method: 'get'
  })
}

export function addTrainingPlan(data) {
  return request({
    url: '/iot/training/plan',
    method: 'post',
    data
  })
}

export function updateTrainingPlan(data) {
  return request({
    url: '/iot/training/plan',
    method: 'put',
    data
  })
}

export function activateTrainingPlan(prescriptionId) {
  return request({
    url: '/iot/training/plan/activate/' + prescriptionId,
    method: 'put'
  })
}

export function delTrainingPlan(prescriptionId) {
  return request({
    url: '/iot/training/plan/' + prescriptionId,
    method: 'delete'
  })
}
