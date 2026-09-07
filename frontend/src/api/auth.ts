import axios from 'axios'
import request, { API_BASE } from './request'

export interface LoginResponse {
  token: string
  refreshToken?: string
  expireMs?: number
  username: string
  permissions: string[]
}

export const login = (username: string, password: string) =>
  request.post<LoginResponse>('/auth/login', { username, password })

export const as400Login = (serverId: number, username: string, password: string) =>
  request.post<LoginResponse>('/auth/as400-login', { serverId, username, password })

/** 当前登录用户资料（/auth/profile） */
export interface UserProfile {
  username: string
  email?: string
  permissions?: string[]
  [key: string]: unknown
}

export const getProfile = (): Promise<UserProfile> => request.get('/auth/profile')

export interface MenuNode {
  path: string
  title: string
  icon?: string
  cached?: boolean
  cacheName?: string
  children?: MenuNode[]
}

export interface TabItem {
  page: string
  title: string
  menuName: string
  status?: number
  perms?: string
}

export interface MenuResponse {
  menus: MenuNode[]
  perms: string[]
  tabs: TabItem[]
}

export const getMenu = () => request.get<MenuResponse>('/auth/menu')

/**
 * P2-1：登出吊销——把当前 token 的 jti 加入服务端吊销名单。
 * 用独立 axios 调用（不带 Authorization 自动注入之外的共享拦截器）并吞掉错误：
 * 失败不影响本地登出，也不会触发 401 拦截器递归。
 */
export function revokeToken(token: string): Promise<void> {
  return axios
    .post(`${API_BASE}/auth/logout`, null, {
      headers: { Authorization: `Bearer ${token}` },
      timeout: 3000,
    })
    .then(() => undefined)
    .catch(() => undefined)
}

export const changePassword = (oldPassword: string, newPassword: string) =>
  request.post<void>('/auth/change-password', { oldPassword, newPassword })

/** P2: Refresh token 端点——用 refresh token 换取新 access token */
export interface RefreshResponse {
  token: string
  refreshToken: string
  expireMs?: number
}

export const refreshAccessToken = (refreshToken: string): Promise<RefreshResponse> =>
  request.post<RefreshResponse>('/auth/refresh', { refreshToken })