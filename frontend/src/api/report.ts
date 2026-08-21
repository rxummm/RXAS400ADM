import request from './request'
import blobClient, { triggerBlobDownload } from './blobClient'

export interface ReportSchedule {
  id?: number
  name: string
  reportType: string
  format: string
  serverId?: number | null
  days?: number
  cronExpr: string
  recipients?: string
  enabled?: boolean
  status?: string
  lastRunTime?: string | null
  lastResult?: string | null
  createdBy?: string
}

/** 立即执行返回（{ status, message }） */
export interface ReportRunResult {
  status: string
  message: string
  [key: string]: unknown
}

/** 报表执行历史行 */
export interface ReportHistoryRow {
  runTime: string
  status: string
  message?: string
  fileBytes?: number | null
  [key: string]: unknown
}

export const listReportSchedules = (): Promise<ReportSchedule[]> => request.get('/report-schedules')

export const createReportSchedule = (data: ReportSchedule) => request.post('/report-schedules', data)

export const updateReportSchedule = (id: number, data: ReportSchedule) => request.put(`/report-schedules/${id}`, data)

export const deleteReportSchedule = (id: number) => request.delete(`/report-schedules/${id}`)

export const toggleReportSchedule = (id: number, enabled: boolean) =>
  request.put(`/report-schedules/${id}/toggle`, null, { params: { enabled } })

export const executeReportSchedule = (id: number): Promise<ReportRunResult> =>
  request.post(`/report-schedules/${id}/execute`)

export const reportScheduleHistory = (id: number): Promise<ReportHistoryRow[]> =>
  request.get(`/report-schedules/${id}/history`)

/** 报表下载：共享 blob 实例返回原始数据，不走统一 JSON 解包 */
export async function downloadReport(
  kind: 'metrics' | 'executions' | 'capacity',
  params: Record<string, unknown>,
  filename: string,
): Promise<void> {
  const res = await blobClient.get(`/reports/${kind}`, { params })
  triggerBlobDownload(res.data, filename)
}
