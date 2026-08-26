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

/** 监控指标历史记录 */
export interface MetricHistory {
  id: number
  instanceId: number
  metricType: string
  metricName: string
  metricValue: number
  collectTime: string
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

/**
 * 获取 AS400 服务器指标历史（用于趋势图）
 */
export function fetchMetricsHistory(instanceId: number, limit = 50): Promise<MetricHistory[]> {
  return request.get(`/monitor/metrics/${instanceId}`, { params: { limit } })
}
