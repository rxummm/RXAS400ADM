import request from './request'

/** 拓扑图节点 */
export interface TopologyNode {
  id?: string
  name?: string
  type?: string
  [key: string]: unknown
}

/** 拓扑图连线 */
export interface TopologyLink {
  source?: string
  target?: string
  [key: string]: unknown
}

export interface TopologyGraph {
  nodes: TopologyNode[]
  links: TopologyLink[]
}

export const topologyGraph = (library: string): Promise<TopologyGraph> =>
  request.get('/topology', { params: { library } })
