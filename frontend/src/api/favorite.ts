import request from './request'

export interface Favorite {
  id: number
  username: string
  title: string
  path: string
  icon?: string
  createdTime?: string
}

export const listMyFavorites = () => request.get<Favorite[]>('/favorites/mine')

export const checkFavorite = (path: string) => request.get<boolean>('/favorites/check', { params: { path } })

export const toggleFavorite = (data: { title: string; path: string; icon?: string }) =>
  request.post<{ favorited: boolean }>('/favorites/toggle', data)
