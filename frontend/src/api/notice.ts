import request from './request'
import type { PaginatedResult } from './types'

export interface Notice {
  id?: number
  title: string
  content: string
  status?: number
  publishedTime?: string
  createdBy?: string
  createdTime?: string
}

export const listPublishedNotices = () => request.get<Notice[]>('/notices')

export const listNotices = (params: {
  current?: number
  size?: number
  keyword?: string
  status?: number
} = {}): Promise<PaginatedResult<Notice>> => request.get('/notices/page', { params })

export const createNotice = (data: Notice) => request.post<Notice>('/notices', data)

export const updateNotice = (id: number, data: Notice) => request.put<Notice>(`/notices/${id}`, data)

export const deleteNotice = (id: number) => request.delete<void>(`/notices/${id}`)
