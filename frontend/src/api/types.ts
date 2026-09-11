/**
 * 共享 API 类型定义
 * 统一前后端类型契约，避免重复定义
 */

/** 分页结果（对齐后端 PageResult<T>） */
export interface PaginatedResult<T> {
  records: T[]
  total: number
}

/** 分页请求参数 */
export interface PageParams {
  current?: number
  size?: number
}

/** API 统一响应格式 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T | null
}
