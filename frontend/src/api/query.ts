import request from './request'

export interface QueryResult {
  columns: string[]
  rows: Record<string, unknown>[]
  rowsReturned: number
  costMs: number
}

/** 查询历史行（rx_sql_history） */
export interface QueryHistoryRow {
  sqlText: string
  rowsReturned: number
  costMs: number
  operator: string
  createdTime: string
  [key: string]: unknown
}

export const executeSql = (sql: string): Promise<QueryResult> =>
  request.post('/query/execute', { sql })

export const fetchQueryHistory = (limit = 20): Promise<QueryHistoryRow[]> =>
  request.get('/query/history', { params: { limit } })
