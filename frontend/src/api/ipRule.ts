import request from './request'
import type { PaginatedResult } from './types'

export interface IpRule {
  id?: number
  ip: string
  type: 'BLACK' | 'WHITE'
  description?: string
  enabled?: number
  createdBy?: string
  createdTime?: string
}

export const listIpRules = (params: {
  current?: number
  size?: number
  type?: string
  keyword?: string
} = {}): Promise<PaginatedResult<IpRule>> => request.get('/ip-rules/page', { params })

export const createIpRule = (data: IpRule) => request.post<IpRule>('/ip-rules', data)

export const updateIpRule = (id: number, data: IpRule) => request.put<IpRule>(`/ip-rules/${id}`, data)

export const deleteIpRule = (id: number) => request.delete<void>(`/ip-rules/${id}`)
