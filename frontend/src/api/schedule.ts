import request from './request'

export interface JobSchedule {
  id: number
  name: string
  serverId: number
  scheduleType: string
  command: string
  cronExpr: string
  enabled: boolean
  status: string
  lastRunTime?: string
  lastResult?: string
  createdBy?: string
}

export interface JobScheduleRequest {
  name: string
  serverId: number
  scheduleType: string
  command: string
  cronExpr: string
  enabled?: boolean
}

/** 调度执行历史行（rx_job_schedule_history） */
export interface ScheduleHistoryRow {
  runTime: string
  status: string
  message?: string
  costMs?: number
  [key: string]: unknown
}

/** 立即执行返回（{ status, message }） */
export interface ScheduleRunResult {
  status: string
  message: string
  [key: string]: unknown
}

export const listSchedules = (): Promise<JobSchedule[]> => request.get('/schedules')

export const createSchedule = (data: JobScheduleRequest) => request.post('/schedules', data)

export const updateSchedule = (id: number, data: JobScheduleRequest) =>
  request.put(`/schedules/${id}`, data)

export const deleteSchedule = (id: number) => request.delete(`/schedules/${id}`)

export const toggleSchedule = (id: number, enabled: boolean) =>
  request.post(`/schedules/${id}/toggle`, null, { params: { enabled } })

export const executeSchedule = (id: number): Promise<ScheduleRunResult> =>
  request.post(`/schedules/${id}/execute`)

export const scheduleHistory = (id: number): Promise<ScheduleHistoryRow[]> =>
  request.get(`/schedules/${id}/history`)
