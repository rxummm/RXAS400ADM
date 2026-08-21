import request from './request'

export interface WebhookConfig {
  id?: number
  name: string
  url: string
  secret?: string
  enabled?: number
  description?: string
  createdBy?: string
  createdTime?: string
  updatedTime?: string
}

export interface WebhookLog {
  id?: number
  webhookId?: number
  webhookName?: string
  title?: string
  message?: string
  success?: number
  attempts?: number
  errorMsg?: string
  createdTime?: string
}

export const listWebhooks = () => request.get<WebhookConfig[]>('/webhooks')

export const listWebhookLogs = (params: {
  current?: number
  size?: number
  success?: number
  webhookName?: string
} = {}) => request.get<{ total: number; records: WebhookLog[] }>('/webhooks/logs', { params })

export const createWebhook = (data: WebhookConfig) => request.post<WebhookConfig>('/webhooks', data)

export const updateWebhook = (id: number, data: WebhookConfig) =>
  request.put<WebhookConfig>(`/webhooks/${id}`, data)

export const deleteWebhook = (id: number) => request.delete<void>(`/webhooks/${id}`)

export const toggleWebhook = (id: number, enabled: number) =>
  request.put<WebhookConfig>(`/webhooks/${id}/enabled`, null, { params: { enabled } })

export const testWebhook = (id: number, title?: string, content?: string) =>
  request.post<{ success: boolean }>(`/webhooks/${id}/test`, null, { params: { title, content } })

export const cleanWebhookLogs = (keepDays = 30) =>
  request.delete<{ deleted: number }>('/webhooks/logs', { params: { keepDays } })
