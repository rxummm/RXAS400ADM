import request from './request'

/** 作业依赖图节点（Mock 仿真 / JT400 真实提交依赖） */
export interface DependencyNode {
  id: string
  name: string
  type: 'PGM' | 'FILE'
  [key: string]: unknown
}

export interface DependencyLink {
  source: string
  target: string
  [key: string]: unknown
}

export interface DependencyGraph {
  nodes: DependencyNode[]
  links: DependencyLink[]
}

/** 作业依赖图（Mock 仿真 / JT400 真实提交依赖 CPF1124） */
export const fetchJobDependency = (): Promise<DependencyGraph> => request.get('/job-dependency')
