import request from './request'

/** 系统值（QSYS2.SYSTEM_VALUE_INFO） */
export interface SystemValue {
  SYSTEM_VALUE_NAME: string
  CURRENT_VALUE: string
  VALUE_DESCRIPTION: string
  SYSTEM_VALUE_TYPE: string
  [key: string]: unknown
}

export const fetchSystemValues = (keyword?: string): Promise<SystemValue[]> =>
  request.get('/system-values', { params: { keyword } })

export const updateSystemValue = (name: string, value: string) =>
  request.put(`/system-values/${name}`, { value })

/** 批量修改系统值 */
export const batchUpdateSystemValues = (entries: Array<{ name: string; value: string }>): Promise<Record<string, { success: boolean; message: string }>> =>
  request.post('/system-values/batch', { entries })