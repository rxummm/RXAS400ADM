import request from './request'

/** Actuator /actuator/metrics 单指标响应（H5：替换调用处的 any） */
export interface MetricMeasurement {
  statistic: string
  value: number
}
export interface MetricDetail {
  name?: string
  measurements?: MetricMeasurement[]
  availableTags?: { tag: string; values: string[] }[]
}

/** Actuator /actuator/health 响应（H5：替换调用处的 any） */
export interface ActuatorComponent {
  status?: string
}
export interface ActuatorHealth {
  status?: string
  components?: Record<string, ActuatorComponent>
}

/**
 * 获取 Actuator 健康信息
 */
export function fetchHealth(): Promise<ActuatorHealth> {
  return request.get('/actuator/health')
}

/**
 * 获取单个指标详情
 */
export function fetchMetricDetail(name: string): Promise<MetricDetail> {
  return request.get(`/actuator/metrics/${name}`)
}
