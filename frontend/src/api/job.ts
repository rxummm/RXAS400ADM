import request from './request'

/** 活动作业（后端 JobInfo VO：ACTIVE_JOB_INFO，字段以 VO 为准） */
export interface JobInfo {
  jobName: string
  jobUser: string
  jobNumber: string
  jobStatus: string
  jobProgram: string
  cpuTime: string
  temporaryStorage: string
}

/** SPOOL 文件行（后端 SpoolRow：OUTPUT_QUEUE_ENTRIES，大写列名原样保留） */
export interface SpoolFile {
  SPOOLED_FILE_NAME: string
  JOB_NAME: string
  JOB_USER: string
  JOB_NUMBER: string
  OUTPUT_QUEUE: string
  SPOOLED_FILE_STATUS: string
  NUMBER_OF_PAGES: number
  USER_DATA: string
}

/** 作业队列行（后端 JobQueueRow：JOB_QUEUE_INFO，大写列名原样保留） */
export interface JobQueueInfo {
  JOB_QUEUE_NAME: string
  JOB_QUEUE_LIBRARY: string
  JOB_QUEUE_STATUS: string
  NUMBER_OF_JOBS: number
  JOB_QUEUE_TYPE: string
}

export const fetchJobs = (status?: string): Promise<JobInfo[]> =>
  request.get('/jobs', { params: { status } })

export const fetchMsgwJobs = (): Promise<JobInfo[]> => request.get('/jobs/msgw')

export const fetchLckwJobs = (): Promise<JobInfo[]> => request.get('/jobs/lckw')

export const endJob = (jobName: string, jobUser: string, jobNumber: string): Promise<void> =>
  request.post('/jobs/end', null, { params: { jobName, jobUser, jobNumber } })

export const batchEndJobs = (jobs: { jobName: string; jobUser: string; jobNumber: string }[]): Promise<void> =>
  request.post('/jobs/batch-end', jobs)

export const holdJob = (jobName: string, jobUser: string, jobNumber: string): Promise<void> =>
  request.post('/jobs/hold', null, { params: { jobName, jobUser, jobNumber } })

export const releaseJob = (jobName: string, jobUser: string, jobNumber: string): Promise<void> =>
  request.post('/jobs/release', null, { params: { jobName, jobUser, jobNumber } })

/** 作业日志行（后端 QSYS2.JOBLOG_INFO，大写列名） */
export interface JobLogRow {
  ORDINAL_POSITION?: number
  MESSAGE_ID?: string
  MESSAGE_TYPE?: string
  MESSAGE_TEXT?: string
  MESSAGE_TIMESTAMP?: string
  [key: string]: unknown
}

/** 作业日志行列表 */
export const fetchJobLog = (jobName: string, jobUser: string, jobNumber: string): Promise<JobLogRow[]> =>
  request.get('/jobs/log', { params: { jobName, jobUser, jobNumber } })

/** MSGW 待应答消息（大写列名） */
export interface MsgwMessage {
  JOB_NAME?: string
  JOB_USER?: string
  JOB_NUMBER?: string
  MESSAGE_ID?: string
  MESSAGE_TYPE?: string
  MESSAGE_TEXT?: string
  REPLY_STATUS?: string
  [key: string]: unknown
}

export const fetchMsgwMessages = (): Promise<MsgwMessage[]> => request.get('/jobs/msgw/messages')

export const replyMsg = (jobName: string, jobUser: string, jobNumber: string): Promise<void> =>
  request.post('/jobs/reply', null, { params: { jobName, jobUser, jobNumber } })

export const fetchJobQueues = (): Promise<JobQueueInfo[]> => request.get('/jobs/queues')

export const fetchSpoolFiles = (params: { jobName?: string; jobUser?: string; jobNumber?: string } = {}): Promise<SpoolFile[]> =>
  request.get('/jobs/spool', { params })