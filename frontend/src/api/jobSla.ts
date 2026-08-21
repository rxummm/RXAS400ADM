import request from './request'

export interface JobSla {
  id?: number
  jobName: string
  scheduleName?: string
  expectedDurationSec: number
  deviationPercent?: number
  enabled?: boolean
}

/** 最近执行对比行（QSYS2.JOB_LOG_INFO 大写列名） */
export interface SlaExecution {
  JOB_NAME: string
  SCHEDULE_NAME: string
  EXPECTED_SEC: number
  ACTUAL_SEC: number
  STATUS: 'OK' | 'BREACHED'
  [key: string]: unknown
}

export const fetchSlaRules = (): Promise<JobSla[]> => request.get('/job-sla/rules')

export const createSlaRule = (data: JobSla) => request.post('/job-sla/rules', data)

export const updateSlaRule = (id: number, data: JobSla) => request.put(`/job-sla/rules/${id}`, data)

export const deleteSlaRule = (id: number) => request.delete(`/job-sla/rules/${id}`)

/** 最近执行对比（Mock 仿真 / JT400 真实作业历史 QSYS2.JOB_LOG_INFO） */
export const fetchSlaExecutions = (): Promise<SlaExecution[]> => request.get('/job-sla/executions')
