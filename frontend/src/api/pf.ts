import request from './request'

/** 物理文件清单（QSYS2.SYSTABLES） */
export interface PfFile {
  TABLE_NAME: string
  TABLE_TEXT?: string | null
  [key: string]: unknown
}

/** 物理文件字段定义 */
export interface PfColumn {
  COLUMN_NAME: string
  COLUMN_TYPE: string
  LENGTH: number
  NULLABLE: string
  [key: string]: unknown
}

export const pfFiles = (library: string): Promise<PfFile[]> =>
  request.get('/pf/files', { params: { library } })

export const pfColumns = (library: string, file: string): Promise<PfColumn[]> =>
  request.get('/pf/columns', { params: { library, file } })

export const pfData = (library: string, file: string, limit = 20): Promise<Record<string, unknown>[]> =>
  request.get('/pf/data', { params: { library, file, limit } })

/** PF 统计信息 */
export interface PfStats {
  recordCount: number
  storageSize: number
  indexCount: number
  indexNames: string[]
  memberCount: number
}

export const pfStats = (library: string, file: string): Promise<PfStats> =>
  request.get('/pf/stats', { params: { library, file } })
