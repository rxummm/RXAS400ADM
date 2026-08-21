import request from './request'

/** 菜单类型常量 */
export const MenuType = {
  DIR: 1,
  MENU: 2,
  BUTTON: 3,
  TAB: 4,
} as const

export type MenuTypeValue = (typeof MenuType)[keyof typeof MenuType]

export interface SysMenu {
  /**
   * ⚠️ 可选：树节点 id 可能为 undefined（后端树/接口未保证必返）。
   * 使用前必须守卫：`data.id != null && set.has(data.id)`、`if (row.id) ...`。
   * 树插槽中 `node-key="id"` 依赖 id 存在，遍历/回显前先过滤。
   */
  id?: number
  parentId?: number | null
  menuName: string
  menuType: number // MenuType 常量：DIR=1/MENU=2/BUTTON=3/TAB=4
  title: string
  path?: string
  component?: string
  perms?: string
  icon?: string
  sort?: number
  visible?: number
  status?: number
  adminOnly?: number
  children?: SysMenu[]
}

export const getMenuTree = () => request.get<SysMenu[]>('/menus/tree')

export const createMenu = (data: SysMenu) => request.post<SysMenu>('/menus', data)

export const updateMenu = (id: number, data: SysMenu) => request.put<SysMenu>(`/menus/${id}`, data)

export const toggleMenuStatus = (id: number, status: number) =>
  request.put<SysMenu>(`/menus/${id}/status`, null, { params: { status } })

export const deleteMenu = (id: number) => request.delete<void>(`/menus/${id}`)