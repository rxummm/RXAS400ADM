import request, { withNoDedupe } from './request'
import blobClient, { triggerBlobDownload } from './blobClient'

export interface MetricOverview {
  cpu: number
  memory: number
  disk: number
  msgw: number
  lckw: number
  jobs: number
}

export interface MetricPoint {
  metricName: string
  metricValue: number
  instanceId: number
  timestamp: string
}

export interface CapacityPoint {
  date: string
  avg: number
}

export interface CapacityPrediction {
  date: string
  value: number
}

export interface CapacityResponse {
  points: CapacityPoint[]
  prediction: CapacityPrediction[]
  daysToThreshold?: number | null
}

/**
 * 服务器对比快照（后端为 Map 序列化，与 QSYS2/指标快照字段一致）：
 * id/name/host/environment/status + cpu/memory/disk/jobs/msgw/lckw
 */
export interface CompareSnapshot {
  id: number
  name: string
  host: string
  environment: string
  status: string
  cpu: number
  memory: number
  disk: number
  jobs: number
  msgw: number
  lckw: number
  [key: string]: unknown
}

/** 巡检检查项（Map 序列化） */
export interface InspectionCheck {
  name: string
  value: string
  status: string
  [key: string]: unknown
}

/** 巡检问题项（Map 序列化） */
export interface InspectionIssue {
  item: string
  level: 'CRITICAL' | 'WARNING' | 'OK'
  detail: string
  detailParams?: Record<string, string>
  [key: string]: unknown
}

/** 巡检报告（后端为 Map 序列化） */
export interface InspectionReport {
  serverId: number
  serverName: string
  host: string
  environment: string
  status: string
  generatedAt: string
  score: number
  grade: string
  checks: InspectionCheck[]
  issues: InspectionIssue[]
}

// P2-23：轮询请求 noDedupe=true——10s 轮询不被其它同 URL 调用方（如仪表盘）取消，
// 也不因上一轮慢响应被新一轮轮询 abort 掉。
export const fetchOverview = (instanceId: number, noDedupe = false): Promise<MetricOverview> =>
  request.get(`/monitor/overview/${instanceId}`, noDedupe ? withNoDedupe() : undefined)

export const fetchMetrics = (instanceId: number, limit = 50, noDedupe = false): Promise<MetricPoint[]> =>
  request.get(`/monitor/metrics/${instanceId}`, noDedupe ? withNoDedupe({ params: { limit } }) : { params: { limit } })

export const fetchCapacity = (instanceId: number, days = 60): Promise<CapacityResponse> =>
  request.get('/monitor/capacity', { params: { instanceId, days } })

/** 服务器对比：多服务器当前指标快照并排 */
export const fetchCompare = (ids: number[]): Promise<CompareSnapshot[]> =>
  request.get('/monitor/compare', { params: { ids: ids.join(',') } })

/** 巡检报告：综合健康巡检生成 */
export const fetchInspection = (serverId: number): Promise<InspectionReport> =>
  request.get('/inspection/generate', { params: { serverId } })

/** 巡检报告导出（xlsx / pdf）：共享 blob 实例返回原始数据，不走统一 JSON 解包 */
export async function exportInspection(
  serverId: number,
  format: 'xlsx' | 'pdf' = 'xlsx',
): Promise<void> {
  const res = await blobClient.get('/inspection/export', { params: { serverId, format } })
  triggerBlobDownload(res.data, `inspection-${serverId}-${Date.now()}.${format}`)
}