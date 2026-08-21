/**
 * 作业标识归一化（P3）：作业表行（小写 jobName/jobUser/jobNumber）与
 * MSGW 抽屉行（大写 JOB_NAME/JOB_USER/JOB_NUMBER）字段名不同，
 * 统一归一化为小写标识供应答/日志/确认框使用。
 */
export interface JobIdentity {
  jobName: string
  jobUser: string
  jobNumber: string
}

export function normalizeJobIdentity(row: Record<string, unknown> | JobIdentity): JobIdentity {
  const r = row as Record<string, unknown>
  return {
    jobName: String(r.jobName ?? r.JOB_NAME ?? ''),
    jobUser: String(r.jobUser ?? r.JOB_USER ?? ''),
    jobNumber: String(r.jobNumber ?? r.JOB_NUMBER ?? ''),
  }
}
