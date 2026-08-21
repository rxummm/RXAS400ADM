import request from './request'

/** 健康巡检报告（/health，Map 结构序列化） */
export interface HealthServer {
  name: string
  host: string
  environment: string
  connect: string
  detail: string
}

export interface HealthReport {
  generatedAt: string
  database: string
  scheduler: string
  openAlerts: number
  servers: HealthServer[]
}

export const healthReport = (): Promise<HealthReport> => request.get('/health')
