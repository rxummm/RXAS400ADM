import request from './request'

export interface OpTemplate {
  id: number
  name: string
  description?: string
  steps: string
  createdBy?: string
  createdAt?: string
  updatedBy?: string
  updatedAt?: string
}

export interface OpTemplateStep {
  type: string
  command: string
}

export interface OpTemplatePayload {
  name: string
  description?: string
  steps: string
}

export interface PageResult<T> {
  total: number
  records: T[]
}

export const listOpTemplates = (params: {
  current: number
  size: number
  keyword?: string
}): Promise<PageResult<OpTemplate>> => request.get('/op-templates', { params })

export const createOpTemplate = (data: OpTemplatePayload): Promise<OpTemplate> =>
  request.post('/op-templates', data)

export const updateOpTemplate = (id: number, data: OpTemplatePayload): Promise<OpTemplate> =>
  request.put(`/op-templates/${id}`, data)

export const deleteOpTemplate = (id: number): Promise<void> =>
  request.delete(`/op-templates/${id}`)

export const executeOpTemplate = (id: number, serverId: number): Promise<void> =>
  request.post(`/op-templates/${id}/execute`, null, { params: { serverId } })
