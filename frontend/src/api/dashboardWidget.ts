import request from './request'

export interface DashboardWidget {
  id?: number
  username?: string
  widgetKey: string
  enabled?: number
}

export const getDashboardWidgets = () => request.get<DashboardWidget[]>('/dashboard/widgets')

export const updateDashboardWidget = (widgetKey: string, enabled: boolean) =>
  request.put<DashboardWidget>(`/dashboard/widgets/${widgetKey}`, null, { params: { enabled } })
