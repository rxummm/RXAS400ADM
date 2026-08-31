import request from './request'

/** 用户Profile列表行 */
export interface UserProfileListRow {
  USER_NAME: string
  STATUS: string
  GROUP_PROFILE: string
  TEXT_DESCRIPTION: string
  LAST_USED_DATE: string
}

/** 用户Profile详情 */
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

/** 创建用户Profile请求 */
export interface UserProfileCreateRequest {
  userName: string
  password: string
  description?: string
  groupProfile?: string
  initialMenu?: string
  specialAuthorities?: string[]
  emailNotification?: string
  recipientEmail?: string
}

/** 更新用户Profile请求 */
export interface UserProfileUpdateRequest {
  description?: string
  groupProfile?: string
  status?: string
  initialMenu?: string
  specialAuthorities?: string[]
  newPassword?: string
}

/** 创建用户Profile响应 */
export interface UserProfileCreateResult {
  success: boolean
  message: string
  userName: string
  createdTime: string
}

/** 获取用户Profile列表 */
export const fetchUserProfileList = (): Promise<UserProfileListRow[]> =>
  request.get('/as400/user-profiles')

/** 获取用户Profile详情 */
export const getUserProfileDetail = (userName: string): Promise<UserProfileDetail> =>
  request.get(`/as400/user-profiles/${userName}`)

/** 创建用户Profile */
export const createUserProfile = (data: UserProfileCreateRequest): Promise<UserProfileCreateResult> =>
  request.post('/as400/user-profiles', data)

/** 更新用户Profile */
export const updateUserProfile = (userName: string, data: UserProfileUpdateRequest): Promise<void> =>
  request.put(`/as400/user-profiles/${userName}`, data)

/** 删除用户Profile */
export const deleteUserProfile = (userName: string): Promise<void> =>
  request.delete(`/as400/user-profiles/${userName}`)
