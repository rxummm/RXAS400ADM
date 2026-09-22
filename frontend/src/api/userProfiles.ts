import request from './request'
import type { PaginatedResult } from './types'

/** 用户 profile 列表行 */
export interface UserProfileList {
  USER_NAME: string
  STATUS: string
  GROUP_PROFILE: string
  TEXT_DESCRIPTION: string
  LAST_USED_DATE: string
}

/** 查询用户 profile 列表（分页） */
export const fetchUserProfiles = (params?: { current?: number; size?: number }): Promise<PaginatedResult<UserProfileList>> =>
  request.get('/user-profiles', { params })

/** 切换用户态 */
export const switchUser = (targetUser: string): Promise<void> =>
  request.post('/user-profiles/switch', null, { params: { targetUser } })
