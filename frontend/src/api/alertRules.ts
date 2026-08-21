import request from './request'

export interface AlertRule {
  id?: number
  metricName: string
  operator: string
  threshold: number
  durationSeconds?: number
  level: string
  enabled?: boolean
  channel: string
  serverId?: number | null
  description?: string
}

export const listAlertRules = (): Promise<AlertRule[]> => request.get('/alert-rules')

export const createAlertRule = (data: AlertRule) => request.post('/alert-rules', data)

export const updateAlertRule = (id: number, data: AlertRule) => request.put(`/alert-rules/${id}`, data)

export const deleteAlertRule = (id: number) => request.delete(`/alert-rules/${id}`)

export const toggleAlertRule = (id: number, enabled: boolean) =>
  request.put(`/alert-rules/${id}/toggle`, null, { params: { enabled } })
