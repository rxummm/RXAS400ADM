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

export interface ExecutionStats {
  totalExecutions: number
  successCount: number
  failedCount: number
  successRate: number
  avgCostMs: number
}

export const listExecutions = (params: {
  type?: string
  status?: string
  keyword?: string
  current?: number
  size?: number
  limit?: number
} = {}) => request.get<{ total: number; records: ExecutionRecord[] }>('/executions', { params })

export const getExecutionStats = (): Promise<ExecutionStats> => request.get('/executions/stats')
