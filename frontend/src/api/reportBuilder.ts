import request from './request'
import blobClient, { triggerBlobDownload } from './blobClient'

export interface DataSourceField {
  key: string
  label: string
  type: 'dimension' | 'measure' | 'date'
}

export interface DataSourceMeta {
  key: string
  label: string
  fields: DataSourceField[]
}

export interface ReportDefinition {
  id?: number
  name: string
  dataSource: string
  title?: string
  columnsJson: string
  filtersJson?: string
  sortsJson?: string
  createdBy?: string
  createdTime?: string
  updatedTime?: string
}

export interface ReportExecuteResult {
  columns: string[]
  keys: string[]
  rows: Record<string, unknown>[]
  total: number
  title: string
  dataSource: string
}

export const listDataSources = (): Promise<DataSourceMeta[]> =>
  request.get('/report-builder/datasources')

export const listReportDefinitions = (): Promise<ReportDefinition[]> =>
  request.get('/report-builder/definitions')

export const getReportDefinition = (id: number): Promise<ReportDefinition> =>
  request.get(`/report-builder/definitions/${id}`)

export const createReportDefinition = (data: ReportDefinition): Promise<ReportDefinition> =>
  request.post('/report-builder/definitions', data)

export const updateReportDefinition = (id: number, data: ReportDefinition): Promise<ReportDefinition> =>
  request.put(`/report-builder/definitions/${id}`, data)

export const deleteReportDefinition = (id: number): Promise<void> =>
  request.delete(`/report-builder/definitions/${id}`)

export const executeReport = (id: number): Promise<ReportExecuteResult> =>
  request.get(`/report-builder/definitions/${id}/execute`)

export async function exportReport(id: number, format: string, title: string): Promise<void> {
  const ext = format === 'pdf' ? 'pdf' : 'xlsx'
  const res = await blobClient.get(`/report-builder/definitions/${id}/export`, { params: { format } })
  triggerBlobDownload(res.data, `${title || 'report'}.${ext}`)
}
