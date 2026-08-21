/**
 * 命名空间：assets / topology / as400Server
 * 用途：服务器管理（实例管理、拓扑分析、服务器选择器）
 * 使用方式：$t('assets.title') / $t('topology.title') / $t('as400Server.selectorTitle')
 */
export default {
  assets: {
    title: '服务器管理（AS400 实例）',
    name: '服务器',
    host: '主机',
    port: '端口',
    environment: '环境',
    level: '级别',
    status: '状态',
    haGroup: 'HA 组',
    username: '连接账号',
    region: '区域',
    test: '连接测试',
    add: '新增服务器',
    edit: '编辑服务器',
    password: '连接密码',
    passwordHint: '密码使用 AES-256-GCM 加密存储；编辑时留空则不修改现有密码',
    passwordPlaceholder: '留空则不修改现有密码',
    passwordRequired: '请输入连接密码',
    defaultLibraries: '默认库列表',
    sortOrder: '排序号',
    description: '描述',
    enabled: '启用',
    defaultServer: '默认',
    required: '服务器名称与主机不能为空',
    deleteConfirm: '确定删除服务器「{name}」？删除后相关监控数据将失去服务器关联。',
    command: '执行命令',
    commandResult: '执行结果',
    commandHint: '输入 CL 命令后点击执行（如 DSPLIB QGPL / WRKACTJOB）',
    execute: '执行',
    serverSyncHint: '维护的服务器会实时同步到顶栏服务器选择器 / 登录页 AS400 模式下拉 / 各业务页服务器下拉',
  },
  topology: {
    title: '调用拓扑分析（DSPPGMREF）',
    library: '库（默认 APP）',
    hint: '点击节点查看详情与引用关系；拖拽/滚轮缩放',
    empty: '暂无拓扑数据（库不可达或库中无引用关系）',
  },
  as400Server: {
    selectorTitle: '选择 AS400 服务器',
    defaultTag: '默认',
    hint: '切换后所有操作将针对所选服务器',
    latency: '延迟',
  },
}