# AS400 工具集

## 服务器管理 {#server-manage}

`GET/POST/PUT/DELETE /api/v1/as400/systems/*`

```mermaid
sequenceDiagram
    actor U as 管理员
    participant AssetView as assets/index.vue
    participant AssetCtrl as As400SystemController
    participant AssetSvc as IAs400SystemService
    participant DB as 业务数据库

    Note over U: 服务器列表
    U->>AssetView: 进入服务器管理
    AssetView->>AssetCtrl: GET /api/v1/as400/systems<br/>🔒 AS400_VIEW
    AssetCtrl->>AssetSvc: list(noDetail=true)
    Note right of AssetSvc: 📖 rx_instance<br/>不含 username/password
    AssetSvc-->>AssetCtrl: List of IbmiSystem
    AssetCtrl-->>AssetView: 服务器列表（名称/主机/端口/环境/级别/状态）

    Note over U: 完整列表（含凭据）
    AssetView->>AssetCtrl: GET /api/v1/as400/systems/detail<br/>🔒 AS400_MANAGE
    AssetCtrl->>AssetSvc: list(noDetail=false)
    Note right of AssetSvc: 📖 rx_instance<br/>含 username 字段
    AssetSvc-->>AssetCtrl: List of IbmiSystem（含 username）
    AssetCtrl-->>AssetView: 服务器列表（含连接账号）

    Note over U: 新增服务器
    U->>AssetView: 点击"新增服务器" → 填写表单
    AssetView->>AssetCtrl: POST /api/v1/as400/systems<br/>🔒 AS400_MANAGE<br/>@OperateLog("新增AS400服务器")
    Note right of AssetView: Body: name, host, port, username, password,<br/>environment, criticalLevel, sslEnabled,<br/>defaultLibraries, ccsid, ...
    AssetCtrl->>AssetSvc: create(dto)
    Note right of AssetSvc: ✏️ rx_instance<br/>密码加密存储
    AssetSvc-->>AssetCtrl: IbmiSystem
    AssetCtrl-->>AssetView: 创建成功

    Note over U: 编辑服务器
    U->>AssetView: 点击编辑
    AssetView->>AssetCtrl: PUT /api/v1/as400/systems/{id}<br/>🔒 AS400_MANAGE<br/>@OperateLog("编辑AS400服务器")
    AssetCtrl->>AssetSvc: update(id, dto)
    Note right of AssetSvc: ✏️ rx_instance
    AssetCtrl-->>AssetView: 更新成功

    Note over U: 删除服务器
    U->>AssetView: 点击删除
    AssetView->>AssetCtrl: DELETE /api/v1/as400/systems/{id}<br/>🔒 AS400_MANAGE<br/>@OperateLog("删除AS400服务器")
    AssetCtrl->>AssetSvc: delete(id)
    Note right of AssetSvc: ✏️ rx_instance<br/>级联影响监控数据关联
    AssetCtrl-->>AssetView: 200 OK

    Note over U: 测试连接
    U->>AssetView: 点击"测试连接"
    AssetView->>AssetCtrl: POST /api/v1/as400/systems/{id}/test<br/>🔒 AS400_MANAGE
    AssetCtrl->>AssetSvc: testConnection(id)
    Note right of AssetSvc: 尝试 JTOpen 连接 → 返回成功/失败
    AssetCtrl-->>AssetView: success: true/false, message: ...
```

---

## 6.1 IFS 文件管理 {#ifs}

`GET/POST /api/v1/ifs/*`

```mermaid
sequenceDiagram
    actor U as 用户
    participant IfsView as ifs/index.vue
    participant IfsCtrl as IfsController
    participant IfsSvc as IfsService
    participant AS400Client as AS400ClientProvider
    participant IBMi as IBM i (AS400)

    Note over U: 浏览 IFS 目录
    U->>IfsView: 进入 IFS 管理
    IfsView->>IfsCtrl: GET /api/v1/ifs?path=/QOpenSys/rxas400<br/>🔒 IFS_VIEW
    IfsCtrl->>IfsSvc: list(normalized)
    IfsSvc->>AS400Client: JTOpenIfsClient.list(path)
    Note right of AS400Client: CL: DSPLNK 或 JTOpen IFS API
    AS400Client->>IBMi: 列出目录
    IBMi-->>IfsView: 文件/目录列表

    Note over U: 查看文件内容
    U->>IfsView: 点击文件
    IfsView->>IfsCtrl: GET /api/v1/ifs/content?path=xxx<br/>🔒 IFS_VIEW
    IfsCtrl->>IfsSvc: read(normalized)
    IfsSvc->>AS400Client: JTOpenIfsClient.read(path)
    AS400Client->>IBMi: 读取文件
    IBMi-->>IfsView: 文件内容

    Note over U: 上传/写入文件
    U->>IfsView: 上传文档
    IfsView->>IfsCtrl: POST /api/v1/ifs/write<br/>🔒 DOC_MANAGE<br/>@OperateLog("上传文档到 IFS")
    IfsCtrl->>IfsSvc: write(path, content)
    IfsSvc->>AS400Client: JTOpenIfsClient.write(path, content)
    AS400Client->>IBMi: 写入 IFS

    Note over U: 上传二进制文件
    IfsView->>IfsCtrl: POST /api/v1/ifs/upload<br/>🔒 IFS_MANAGE (multipart/form-data)
    Note over IfsCtrl: 校验大小 ≤ 100MB

    Note over U: 新建目录/删除
    IfsView->>IfsCtrl: POST /api/v1/ifs/mkdir 🔒 IFS_MANAGE
    IfsView->>IfsCtrl: DELETE /api/v1/ifs 🔒 IFS_MANAGE
```

---

## 6.2 对象管理 {#objects}

`GET /api/v1/objects/*`

```mermaid
sequenceDiagram
    actor U as 用户
    participant ObjView as objects/index.vue
    participant ObjCtrl as ObjectController
    participant ObjSvc as ObjectService
    participant AS400Client as AS400ClientProvider
    participant IBMi as IBM i (AS400)

    Note over U: 对象搜索
    U->>ObjView: 搜索对象
    ObjView->>ObjCtrl: GET /api/v1/objects?library=APP&type=*PGM&keyword=xxx&current=1&size=20<br/>🔒 OBJECT_VIEW
    ObjCtrl->>ObjSvc: searchObjects(library, type, keyword, page, pageSize)
    ObjSvc->>AS400Client: JTOpenCommandClient.execute("DSPOBJD ...")
    Note right of AS400Client: CL: DSPOBJD OBJ(library/*ALL) OBJTYPE(*ALL)<br/>标识符校验: requireIdentifier()
    AS400Client->>IBMi: 搜索对象
    IBMi-->>ObjView: 分页对象列表

    Note over U: 对象详情
    U->>ObjView: 点击对象
    ObjView->>ObjCtrl: GET /api/v1/objects/{library}/{name}/detail<br/>🔒 OBJECT_VIEW
    ObjCtrl->>ObjSvc: objectDetail(library, name)
    ObjSvc->>AS400Client: DSPOBJD + DSPFD
    AS400Client->>IBMi: 查询详情
    IBMi-->>ObjView: 对象详情

    Note over U: 引用分析
    ObjView->>ObjCtrl: GET /api/v1/objects/{library}/{name}/references?direction=IN<br/>🔒 OBJECT_VIEW
    ObjCtrl->>ObjSvc: objectReferences(library, name, direction)
    ObjSvc->>AS400Client: DSPPGMREF
    IBMi-->>ObjView: 引用关系

    Note over U: 权限查看
    ObjView->>ObjCtrl: GET /api/v1/objects/{library}/{name}/authorities<br/>🔒 OBJECT_VIEW
    ObjCtrl->>ObjSvc: objectAuthorities(library, name)
    ObjSvc->>AS400Client: DSPOBJAUT
    IBMi-->>ObjView: 权限列表
```

---

## 6.3 PF 物理文件浏览 {#pf}

`GET /api/v1/pf/*`

```mermaid
sequenceDiagram
    actor U as 用户
    participant PfView as pf/index.vue
    participant PfCtrl as PfController
    participant PfSvc as PfService
    participant AS400Client as AS400ClientProvider
    participant IBMi as IBM i (AS400)

    U->>PfView: 选择库名
    PfView->>PfCtrl: GET /api/v1/pf/files?library=APP 🔒 PF_VIEW
    PfCtrl->>PfSvc: files(library)
    PfSvc->>AS400Client: QSYS2.SYSTABLES (TABLE_TYPE='P')
    IBMi-->>PfView: PF 文件列表

    U->>PfView: 选择 PF 文件
    PfView->>PfCtrl: GET /api/v1/pf/columns?library=APP&file=xxx 🔒 PF_VIEW
    PfCtrl->>PfSvc: columns(library, file)
    PfSvc->>AS400Client: QSYS2.SYSCOLUMNS
    IBMi-->>PfView: 字段定义列表

    U->>PfView: 查看数据
    PfView->>PfCtrl: GET /api/v1/pf/data?library=APP&file=xxx&limit=20 🔒 PF_VIEW
    PfCtrl->>PfSvc: data(library, file, limit)
    PfSvc->>AS400Client: SELECT * FROM library.file LIMIT n
    IBMi-->>PfView: 数据行
```

---

## 6.4 子系统管理 {#subsystems}

`GET/POST /api/v1/subsystems/*`

```mermaid
sequenceDiagram
    actor U as 用户
    participant SubsysView as subsystems/index.vue
    participant SubsysCtrl as SubsystemController
    participant SubsysSvc as SubsystemService
    participant AS400Client as AS400ClientProvider
    participant IBMi as IBM i (AS400)

    U->>SubsysView: 查看子系统
    SubsysView->>SubsysCtrl: GET /api/v1/subsystems 🔒 SUBSYSTEM_VIEW
    SubsysCtrl->>SubsysSvc: list()
    SubsysSvc->>AS400Client: QSYS2.SUBSYSTEM_INFO
    AS400Client->>IBMi: 查询子系统状态
    IBMi-->>SubsysView: 子系统列表

    U->>SubsysView: 启动子系统
    SubsysView->>SubsysCtrl: POST /api/v1/subsystems/{name}/start<br/>🔒 SUBSYSTEM_MANAGE<br/>@OperateLog("启动子系统")
    SubsysCtrl->>SubsysSvc: start(name)
    SubsysSvc->>AS400Client: JTOpenCommandClient.execute("STRSBS ...")
    Note right of AS400Client: CL: STRSBS SBSD(name)<br/>标识符校验: requireIdentifier()
    AS400Client->>IBMi: 启动子系统
    IBMi-->>SubsysView: CommandResult

    U->>SubsysView: 停止子系统
    SubsysView->>SubsysCtrl: POST /api/v1/subsystems/{name}/end<br/>🔒 SUBSYSTEM_MANAGE<br/>@OperateLog("停止子系统")
    SubsysCtrl->>SubsysSvc: end(name)
    SubsysSvc->>AS400Client: ENDSBS SBSD(name)
    AS400Client->>IBMi: 停止子系统
```

---

## 6.5 命令脚本中心 {#scripts}

`GET/POST /api/v1/scripts/*`

```mermaid
sequenceDiagram
    actor U as 用户
    participant ScriptView as scripts/index.vue
    participant ScriptCtrl as ScriptController
    participant ScriptSvc as CommandScriptService
    participant AS400Client as AS400ClientProvider
    participant IBMi as IBM i (AS400)
    participant DB as 业务数据库

    Note over U: 脚本列表
    U->>ScriptView: 进入脚本中心
    ScriptView->>ScriptCtrl: GET /api/v1/scripts?favorite=true&tag=xxx 🔒 SCRIPT_VIEW
    Note right of ScriptSvc: 📖 rx_command_script
    ScriptCtrl-->>ScriptView: 脚本列表

    Note over U: 新建脚本
    U->>ScriptView: 新建脚本
    ScriptView->>ScriptCtrl: POST /api/v1/scripts 🔒 SCRIPT_MANAGE<br/>@OperateLog("新建脚本")
    Note right of ScriptSvc: ✏️ rx_command_script
    ScriptCtrl-->>ScriptView: 脚本详情

    Note over U: 执行脚本
    U->>ScriptView: 点击执行
    ScriptView->>ScriptCtrl: POST /api/v1/scripts/{id}/execute 🔒 SCRIPT_MANAGE<br/>@OperateLog("执行脚本")
    ScriptCtrl->>ScriptSvc: execute(id)
    ScriptSvc->>AS400Client: JTOpenCommandClient.execute(script)
    Note right of AS400Client: CL 命令执行<br/>标识符校验: requireIdentifier()
    AS400Client->>IBMi: 执行脚本
    Note right of ScriptSvc: ✏️ rx_command_script_execution<br/>执行记录
    ScriptCtrl-->>ScriptView: CommandResult

    Note over U: 标签分类
    ScriptView->>ScriptCtrl: GET /api/v1/scripts/tags 🔒 SCRIPT_VIEW
    ScriptCtrl-->>ScriptView: 标签列表

    Note over U: 收藏切换
    ScriptView->>ScriptCtrl: POST /api/v1/scripts/{id}/favorite?favorite=true 🔒 SCRIPT_MANAGE
    Note right of ScriptSvc: ✏️ rx_command_script<br/>is_favorite 字段
```

---

## 6.6 作业调度中心 {#schedules}

`GET/POST /api/v1/schedules/*`

```mermaid
sequenceDiagram
    actor U as 用户
    participant SchedView as schedule/index.vue
    participant SchedCtrl as ScheduleController
    participant SchedSvc as JobScheduleService
    participant AS400Client as AS400ClientProvider
    participant IBMi as IBM i (AS400)
    participant DB as 业务数据库

    Note over U: 调度列表
    U->>SchedView: 进入调度中心
    SchedView->>SchedCtrl: GET /api/v1/schedules 🔒 SCHEDULE_VIEW
    Note right of SchedSvc: 📖 rx_job_schedule
    SchedCtrl-->>SchedView: 调度任务列表

    Note over U: 创建调度任务
    U->>SchedView: 新建调度
    SchedView->>SchedCtrl: POST /api/v1/schedules 🔒 SCHEDULE_MANAGE<br/>@OperateLog("创建调度任务")
    Note right of SchedSvc: ✏️ rx_job_schedule
    SchedCtrl-->>SchedView: 任务详情

    Note over U: 立即执行
    U->>SchedView: 点击立即执行
    SchedView->>SchedCtrl: POST /api/v1/schedules/{id}/execute 🔒 SCHEDULE_MANAGE
    SchedCtrl->>SchedSvc: executeNow(id)
    SchedSvc->>AS400Client: 执行 CL 命令 / SQL
    AS400Client->>IBMi: 执行任务
    Note right of SchedSvc: ✏️ rx_job_schedule_history<br/>执行记录
    SchedCtrl-->>SchedView: 执行结果

    Note over U: 启停调度
    SchedView->>SchedCtrl: POST /api/v1/schedules/{id}/toggle?enabled=true 🔒 SCHEDULE_MANAGE
    Note right of SchedSvc: ✏️ rx_job_schedule<br/>enabled 字段
    SchedCtrl-->>SchedView: 更新状态

    Note over U: 执行历史
    SchedView->>SchedCtrl: GET /api/v1/schedules/{id}/history 🔒 SCHEDULE_VIEW
    Note right of SchedSvc: 📖 rx_job_schedule_history
    SchedCtrl-->>SchedView: 历史记录
```

---

## 6.7 执行历史审计 {#executions}

`GET /api/v1/executions`

```mermaid
sequenceDiagram
    actor U as 用户
    participant ExecView as executions/index.vue
    participant ExecCtrl as ExecutionController
    participant ExecSvc as ExecutionService
    participant DB as 业务数据库

    U->>ExecView: 进入执行历史
    ExecView->>ExecCtrl: GET /api/v1/executions?type=schedule&status=SUCCESS&keyword=xxx&current=1&size=20<br/>🔒 EXECUTION_VIEW
    ExecCtrl->>ExecSvc: scheduleExecutions / scriptExecutions
    Note right of ExecSvc: 📖 rx_job_schedule_history<br/>📖 rx_command_script_execution<br/>合并排序（按 runTime DESC）<br/>后端分页
    ExecCtrl-->>ExecView: 合并执行历史
```

---

## 6.8 编译中心 {#compile}

`POST /api/v1/compile`

```mermaid
sequenceDiagram
    actor U as 用户
    participant CompileView as compile/index.vue
    participant CompileCtrl as CompileController
    participant CompileSvc as CompileService
    participant AS400Client as AS400ClientProvider
    participant IBMi as IBM i (AS400)
    participant DB as 业务数据库

    U->>CompileView: 提交编译请求
    CompileView->>CompileCtrl: POST /api/v1/compile 🔒 COMPILE_EXECUTE<br/>@OperateLog("编译成员")
    Note over CompileCtrl: @Valid CompileRequest
    CompileCtrl->>CompileSvc: compile(request)
    CompileSvc->>AS400Client: JTOpenCommandClient.execute(compile command)
    Note right of AS400Client: CL: CRTBNDRPG / CRTSQLRPGI / CRTCLPGM<br/>CL 命令白名单校验
    AS400Client->>IBMi: 执行编译
    Note right of CompileSvc: ✏️ rx_compile_record<br/>编译记录
    CompileSvc-->>CompileCtrl: CompileRecord
    CompileCtrl-->>CompileView: 编译结果

    U->>CompileView: 查看编译历史
    CompileView->>CompileCtrl: GET /api/v1/compile/history 🔒 COMPILE_EXECUTE
    Note right of CompileSvc: 📖 rx_compile_record
    CompileCtrl-->>CompileView: 编译历史
```

---

## 6.9 源码浏览 {#source}

`GET /api/v1/source/*`

```mermaid
sequenceDiagram
    actor U as 用户
    participant SourceView as Source.vue
    participant SourceCtrl as SourceController
    participant SourceSvc as SourceService
    participant AS400Client as AS400ClientProvider
    participant IBMi as IBM i (AS400)

    Note over SourceCtrl: 类级别 🔒 SOURCE_VIEW

    U->>SourceView: 浏览源码库
    SourceView->>SourceCtrl: GET /api/v1/source/libraries
    SourceCtrl->>SourceSvc: listLibraries()
    SourceSvc->>AS400Client: QSYS2.SYSTABLES (TABLE_TYPE='S')
    IBMi-->>SourceView: 源码库列表

    U->>SourceView: 选择源码文件
    SourceView->>SourceCtrl: GET /api/v1/source/libraries/{lib}/files
    SourceSvc->>AS400Client: QSYS2.SYSTABLES
    IBMi-->>SourceView: 源码文件列表

    U->>SourceView: 选择成员
    SourceView->>SourceCtrl: GET /api/v1/source/libraries/{lib}/files/{file}/members
    SourceSvc->>AS400Client: QSYS2.SYSPARTITIONSTAT
    IBMi-->>SourceView: 成员列表

    U->>SourceView: 查看源码
    SourceView->>SourceCtrl: GET /api/v1/source/libraries/{lib}/files/{file}/members/{mbr}
    SourceSvc->>AS400Client: 读取源码成员内容
    IBMi-->>SourceView: 源码内容
```

---

## 6.10 其他工具 {#other-tools}

拓扑图 / 消息文件 / 系统值 / 表字段

```mermaid
sequenceDiagram
    actor U as 用户
    participant TopologyCtrl as TopologyController
    participant MsgFileCtrl as MessageFileController
    participant SysvalCtrl as SystemValueController
    participant AS400Client as AS400ClientProvider
    participant IBMi as IBM i (AS400)
    participant DB as 业务数据库

    Note over U: 拓扑图
    U->>TopologyCtrl: GET /api/v1/topology 🔒 TOPOLOGY_VIEW
    Note right of TopologyCtrl: 📖 rx_topology_config
    TopologyCtrl-->>U: 拓扑数据

    Note over U: 消息文件
    U->>MsgFileCtrl: GET /api/v1/message-files 🔒 MSGF_VIEW
    MsgFileCtrl->>AS400Client: DSPMSGD
    AS400Client->>IBMi: 查询消息文件
    IBMi-->>U: 消息列表

    Note over U: 系统值
    U->>SysvalCtrl: GET /api/v1/sysvals 🔒 SYSVAL_VIEW
    SysvalCtrl->>AS400Client: QSYS2.SYSTEM_VALUE_INFO
    AS400Client->>IBMi: 查询系统值
    IBMi-->>U: 系统值列表

    Note over U: 表字段详情
    U->>MsgFileCtrl: GET /api/v1/table-fields 🔒 QUERY_EXECUTE
    Note right of AS400Client: QSYS2.SYSCOLUMNS<br/>扩展字段详情
```

### 涉及权限码汇总

| 权限码 | 功能 |
|--------|------|
| `IFS_VIEW` / `IFS_MANAGE` | IFS 文件管理 |
| `OBJECT_VIEW` | 对象查看 |
| `PF_VIEW` | PF 文件查看 |
| `SUBSYSTEM_VIEW` / `SUBSYSTEM_MANAGE` | 子系统管理 |
| `SCRIPT_VIEW` / `SCRIPT_MANAGE` | 脚本管理 |
| `SCHEDULE_VIEW` / `SCHEDULE_MANAGE` | 调度管理 |
| `EXECUTION_VIEW` | 执行历史 |
| `COMPILE_EXECUTE` | 编译执行 |
| `SOURCE_VIEW` | 源码查看 |
| `TOPOLOGY_VIEW` | 拓扑查看 |
| `MSGF_VIEW` | 消息文件 |
| `SYSVAL_VIEW` | 系统值 |
| `DOC_MANAGE` | 文档管理（上传到 IFS） |