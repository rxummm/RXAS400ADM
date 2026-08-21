import request from './request'

/** 库内文件/表清单（QSYS2.SYSTABLES） */
export interface BizTable {
  TABLE_NAME: string
  TABLE_TEXT?: string | null
  [key: string]: unknown
}

/** 文件字段定义（QSYS2.SYSCOLUMNS，含长度/描述） */
export interface BizColumn {
  ORDINAL_POSITION: number
  COLUMN_NAME: string
  DATA_TYPE: string
  LENGTH: number
  SCALE: number
  IS_NULLABLE: string
  COLUMN_DEFAULT: string | null
  COLUMN_TEXT: string | null
  [key: string]: unknown
}

export const listBizTables = (params: { library?: string; keyword?: string }): Promise<BizTable[]> =>
  request.get('/business/tables', { params })

export const bizColumns = (library: string, table: string): Promise<BizColumn[]> =>
  request.get('/business/columns', { params: { library, table } })

/** 业务数据分页浏览（关键词模糊查询） */
export const bizData = (params: {
  library: string
  table: string
  keyword?: string
  page?: number
  size?: number
}) => request.get<{ rows: Record<string, unknown>[]; total: number; columns: BizColumn[] }>('/business/data', { params })
