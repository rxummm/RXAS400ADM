import request from './request'

export interface TaskMethodInfo {
  method: string
  schedule: string
  enabled: boolean
}

export interface TaskBeanInfo {
  bean: string
  className: string
  methods: TaskMethodInfo[]
}

export const listTasks = () => request.get<TaskBeanInfo[]>('/tasks')

export const triggerTask = (beanName: string, methodName: string) =>
  request.post<{ triggered: boolean }>(`/tasks/${beanName}/${methodName}/trigger`)
