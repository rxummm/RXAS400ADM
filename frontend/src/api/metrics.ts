import request from './request'

/**
 * 获取 Actuator 健康信息
 */
export function fetchHealth(): Promise<any> {
  return request.get('/actuator/health')
}

/**
 * 获取单个指标详情
 */
export function fetchMetricDetail(name: string): Promise<any> {
  return request.get(`/actuator/metrics/${name}`)
}
