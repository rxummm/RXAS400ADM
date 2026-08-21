import request from './request'

export interface ExecutionRecord {
  source: 'SCHEDULE' | 'SCRIPT'
  id: number
  taskId: number
  name: string
  type: string
  serverId?: number | null
  user?: string | null
  runTime?: string
  status: string
  message?: string | null
  costMs?: number | null
  runCount?: number | null
}

export const listExecutions = (params: {
  type?: string
  status?: string
  keyword?: string
  current?: number
  size?: number
  limit?: number
} = {}) => request.get<{ total: number; records: ExecutionRecord[] }>('/executions', { params })
