import request from './request'

/** 用户 profile 列表行 */
export interface UserProfileList {
  USER_NAME: string
  STATUS: string
  GROUP_PROFILE: string
  TEXT_DESCRIPTION: string
  LAST_USED_DATE: string
}

/** 查询用户 profile 列表 */
export const fetchUserProfiles = (): Promise<UserProfileList[]> =>
  request.get('/user-profiles')

/** 切换用户态 */
export const switchUser = (targetUser: string): Promise<void> =>
  request.post('/user-profiles/switch', null, { params: { targetUser } })
