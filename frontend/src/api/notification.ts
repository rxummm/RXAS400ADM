import request from './request'
import type { PaginatedResult } from './types'

export interface Notification {
  id?: number
  username?: string
  type: string
  title?: string
  content?: string
  readFlag?: number
  createdTime?: string
}

export const listMyNotifications = (params: {
  current?: number
  size?: number
  unreadOnly?: boolean
} = {}): Promise<PaginatedResult<Notification>> => request.get('/notifications/mine', { params })

export const getUnreadCount = () => request.get<{ count: number }>('/notifications/unread-count')

export const markNotificationRead = (id: number) => request.post<void>(`/notifications/${id}/read`)

export const markAllNotificationsRead = () => request.post<void>('/notifications/read-all')

export const deleteNotification = (id: number) => request.delete<void>(`/notifications/${id}`)

export const deleteNotifications = (ids: number[]) =>
  request.post<{ deleted: number }>('/notifications/batch-delete', { ids })
