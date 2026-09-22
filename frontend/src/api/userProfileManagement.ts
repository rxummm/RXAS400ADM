// AS400 用户 Profile 管理 API
import request from '@/api/request'
import type { PaginatedResult } from '@/api/types'

export interface UserProfileListRow {
  USER_NAME: string
  STATUS: string
  GROUP_PROFILE: string
  TEXT_DESCRIPTION: string
  LAST_USED_DATE: string
}

export interface UserProfileDetail {
  userName: string
  status: string
  groupProfile: string
  description: string
  initialMenu: string
  specialAuthorities: string[]
  lastUsedDate: string
  passwordExpireDate: string
}

export interface UserProfileCreateDTO {
  userName: string
  password: string
  description?: string
  groupProfile?: string
  initialMenu?: string
  specialAuthorities?: string[]
  emailNotification?: string
  recipientEmail?: string
}

export interface UserProfileUpdateDTO {
  description?: string
  groupProfile?: string
  status?: string
  initialMenu?: string
  specialAuthorities?: string[]
  newPassword?: string
}

export interface UserProfileDeleteParams {
  deleteReason: string
  deletionType: string
}

export interface UserProfileLog {
  id: number
  userName: string
  action: string
  operator: string
  detail: string
  deleteReason: string
  deletionType: string
  createdTime: string
}

export interface UserProfileDeleteStats {
  totalDeletes: number
  manualDeletes: number
  inactiveDeletes: number
  resignedDeletes: number
  recentRecords: UserProfileLog[]
}

export interface UserProfileBatchDeleteResult {
  total: number
  successCount: number
  failCount: number
  failDetails: string
}

export function fetchUserProfileList(params: Record<string, unknown>): Promise<PaginatedResult<UserProfileListRow>> {
  return request.get('/api/v1/as400/user-profiles', { params })
}

export function getUserProfileDetail(userName: string): Promise<UserProfileDetail> {
  return request.get(`/api/v1/as400/user-profiles/${encodeURIComponent(userName)}`)
}

export function createUserProfile(dto: UserProfileCreateDTO): Promise<{ userName: string }> {
  return request.post('/api/v1/as400/user-profiles', dto)
}

export function updateUserProfile(userName: string, dto: UserProfileUpdateDTO): Promise<void> {
  return request.put(`/api/v1/as400/user-profiles/${encodeURIComponent(userName)}`, dto)
}

export function deleteUserProfile(userName: string, params?: UserProfileDeleteParams): Promise<void> {
  return request.delete(`/api/v1/as400/user-profiles/${encodeURIComponent(userName)}`, { params })
}

export function getUserProfileLogs(userName: string, params?: Record<string, unknown>): Promise<PaginatedResult<UserProfileLog>> {
  return request.get(`/api/v1/as400/user-profiles/${encodeURIComponent(userName)}/logs`, { params })
}

export function batchDeleteUserProfiles(dto: { userNames: string[]; deleteReason: string; deletionType: string }): Promise<UserProfileBatchDeleteResult> {
  return request.post('/api/v1/as400/user-profiles/batch-delete', dto)
}

export function getDeleteStats(): Promise<UserProfileDeleteStats> {
  return request.get('/api/v1/as400/user-profiles/delete-stats')
}