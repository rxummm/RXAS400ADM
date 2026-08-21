# RXAS400ADM 项目模块流程图

> 本文档使用 Mermaid 语法绘制，在 VS Code（安装 Markdown Preview Mermaid 插件）或 GitHub/GitLab 中可直接渲染。
> 涵盖从前端用户操作 → 后端 Controller → Service → AS400Client → IBM i 的完整调用链路，
> 以及每步涉及的数据表读写。

---

## 一、系统总体架构

![总体架构图](flowcharts/01-总体架构.svg)

<details>
<summary>点击展开 Mermaid 源码</summary>

```mermaid
graph TD
    subgraph Browser["浏览器 (Vue 3 + TypeScript)"]
        Login["Login.vue 登录页"]
        Layout["Layout.vue 主布局"]
        Views["各业务视图 (Dashboard/Job/Monitor/...)"]
        Pinia["Pinia Store (user / as400Server)"]
        Axios["Axios 请求层 (request.ts)"]
    end

    subgraph Security["Security Filter 链 (Spring Security)"]
        JwtFilter["JwtAuthenticationFilter (JWT 校验)"]
        CorsFilter["CorsFilter (CORS 白名单)"]
        SecurityConfig["SecurityConfig (CSP/CSRF/Session)"]
    end

    subgraph Backend["后端 Controller 层 (Spring Boot)"]
        AuthCtrl["AuthController /api/v1/auth"]
        As400Ctrl["As400Controller /api/v1/as400"]
        JobCtrl["JobController /api/v1/jobs"]
        MonitorCtrl["MonitorController /api/v1/monitor"]
        QueryCtrl["SqlQueryController /api/v1/query"]
        BizCtrl["BusinessController /api/v1/business"]
        SysCtrl["SysUser/Role/Menu/Permission/Config/... /api/v1/users|roles|menus|configs|..."]
        CompileCtrl["CompileController /api/v1/compile"]
        SourceCtrl["SourceController /api/v1/source"]
        IfsCtrl["IfsController /api/v1/ifs"]
        ObjectCtrl["ObjectController /api/v1/objects"]
        PfCtrl["PfController /api/v1/pf"]
        SubsysCtrl["SubsystemController /api/v1/subsystems"]
        ScriptCtrl["ScriptController /api/v1/scripts"]
        SchedCtrl["ScheduleController /api/v1/schedules"]
        ExecCtrl["ExecutionController /api/v1/executions"]
    end

    subgraph Service["Service 层"]
        SysUserSvc["SysUserService"]
        MenuSvc["MenuService"]
        PermissionSvc["PermissionService"]
        LoginAttemptSvc["LoginAttemptService"]
        IpRuleSvc["IpRuleService"]
        TokenBlacklistSvc["TokenBlacklistService"]
        JobSvc["JobService"]
        MetricSvc["MetricService"]
        AlertEngine["AlertEngine"]
        BusinessSvc["BusinessService"]
        SqlQuerySvc["SqlQueryService"]
        IbmiSystemSvc["IbmiSystemService"]
    end

    subgraph AS400["AS400 Client 层 (JTOpen)"]
        AS400Provider["AS400ClientProvider (按 X-AS400-Server 头路由)"]
        JTOpenClient["JTOpenAS400Client (委托模式, 12 个子接口)"]
        SqlClient["JTOpenSqlClient (PreparedStatement, DB2 for i)"]
        CommandClient["JTOpenCommandClient (CL 命令, 标识符校验)"]
    end

    subgraph Data["数据层"]
        MySQL[("MySQL / DB2 for i<br/>业务数据库")]
        IBMi[("IBM i (AS400)<br/>QSYS2 系统表")]
        Redis[("Redis (可选)<br/>缓存")]
    end

    Browser -->|"HTTP + JWT Header"| Security
    Security -->|"已认证"| Backend
    Backend --> Service
    Service --> AS400
    Service --> MySQL
    AS400 -->|"JDBC / CL Command"| IBMi

    style Browser fill:#e1f5fe
    style Security fill:#fff3e0
    style Backend fill:#e8f5e9
    style Service fill:#f3e5f5
    style AS400 fill:#fce4ec
    style Data fill:#fff9c4
```

</details>

---

## 二、用户登录模块

### 2.1 平台账号登录（用户名/密码）

```mermaid
sequenceDiagram
    actor User as 用户
    participant LoginVue as Login.vue
    participant UserStore as Pinia userStore
    participant Axios as Axios (request.ts)
    participant AuthCtrl as AuthController<br/>/api/v1/auth/login
    participant IpRuleSvc as IpRuleService
    participant LoginAttemptSvc as LoginAttemptService
    participant UserSvc as SysUserService
    participant BCrypt as BCryptPasswordEncoder
    participant PermissionSvc as PermissionService
    participant JwtUtil as JwtUtil
    participant AuditSvc as AuditLogService
    participant MenuSvc as MenuService
    participant Router as Vue Router

    rect rgb(225, 245, 254)
        Note over User,Router: 【阶段一】用户打开登录页
        User->>LoginVue: 输入用户名/密码，点击登录
        LoginVue->>UserStore: login(username, password)
        UserStore->>Axios: POST /api/v1/auth/login {username, password}
        Axios->>AuthCtrl: HTTP Request
    end

    rect rgb(255, 243, 224)
        Note over AuthCtrl,AuditSvc: 【阶段二】安全前置校验
        AuthCtrl->>IpRuleSvc: checkIp(clientIp)
        Note right of IpRuleSvc: 读: rx_ip_rule<br/>IP 白/黑名单匹配
        IpRuleSvc-->>AuthCtrl: OK / 拒绝

        AuthCtrl->>LoginAttemptSvc: checkIpRate(clientIp)
        Note right of LoginAttemptSvc: 读: rx_login_attempt<br/>同 IP 最近失败次数 ≥ 阈值
        LoginAttemptSvc-->>AuthCtrl: OK / 限流

        AuthCtrl->>LoginAttemptSvc: checkUsernameLock(username)
        Note right of LoginAttemptSvc: 读: rx_login_attempt<br/>该用户名是否被锁定
        LoginAttemptSvc-->>AuthCtrl: OK / 已锁定
    end

    rect rgb(232, 245, 233)
        Note over AuthCtrl,AuditSvc: 【阶段三】身份认证
        AuthCtrl->>UserSvc: getByUsername(username)
        Note right of UserSvc: 读: sys_user<br/>WHERE username = ?
        UserSvc-->>AuthCtrl: SysUser (含 password hash)

        alt 用户不存在或密码错误
            AuthCtrl->>LoginAttemptSvc: registerFailure(username, ip)
            Note right of LoginAttemptSvc: 写: rx_login_attempt<br/>failed_count++<br/>达到阈值则 locked_until=NOW()+30min
            AuthCtrl->>AuditSvc: auditLogin("LOGIN_FAILED", ...)
            Note right of AuditSvc: 写: rx_audit_log
            AuthCtrl-->>Axios: 401 LOGIN_FAILED
            Axios-->>User: 用户名或密码错误
        else 用户被禁用 (status != 'ACTIVE')
            AuthCtrl-->>Axios: 403 FORBIDDEN
            Axios-->>User: 用户已被禁用
        else 认证成功
            AuthCtrl->>BCrypt: matches(rawPassword, hash)
            BCrypt-->>AuthCtrl: true
            AuthCtrl->>LoginAttemptSvc: clearFailure(username)
            Note right of LoginAttemptSvc: 写: rx_login_attempt<br/>清空此用户失败记录
        end
    end

    rect rgb(243, 229, 245)
        Note over AuthCtrl,AuditSvc: 【阶段四】权限加载 + JWT 签发
        AuthCtrl->>PermissionSvc: refresh(username)
        Note right of PermissionSvc: 读: sys_user → sys_user_role → sys_role → sys_role_permission → sys_permission<br/>+ sys_user_menu → sys_menu<br/>合并返回权限码列表
        PermissionSvc-->>AuthCtrl: [permissions]

        AuthCtrl->>JwtUtil: generateToken(username, permissions)
        Note right of JwtUtil: 生成 JWT:<br/>- sub: username<br/>- iss: rxas400adm<br/>- aud: rxas400adm-web<br/>- jti: UUID<br/>- claims: permissions<br/>- 签名: 54字节 HS256 secret
        JwtUtil-->>AuthCtrl: token

        AuthCtrl->>AuditSvc: auditLogin("LOGIN_SUCCESS", ...)
        Note right of AuditSvc: 写: rx_audit_log
        AuthCtrl-->>Axios: 200 {token, username, permissions}
    end

    rect rgb(225, 245, 254)
        Note over User,Router: 【阶段五】前端状态初始化 + 菜单加载
        Axios-->>UserStore: LoginResponse
        UserStore->>UserStore: token → localStorage<br/>username → localStorage<br/>permissions → state
        UserStore->>Axios: GET /api/v1/auth/menu
        Axios->>AuthCtrl: HTTP Request (带 JWT)
        AuthCtrl->>MenuSvc: userMenuData(username)
        Note right of MenuSvc: 读: sys_menu (菜单树)<br/>读: sys_user_role + sys_role_menu (角色授权)<br/>读: sys_user_menu (用户专属授权)<br/>读: sys_tab (Tab 页签)<br/>ADMIN: 全部启用菜单<br/>普通用户: 按角色裁剪
        MenuSvc-->>AuthCtrl: {menus, perms, tabs}
        AuthCtrl-->>Axios: 200 {menus, perms, tabs}
        Axios-->>UserStore: MenuResponse

        UserStore->>UserStore: menus/permissions/tabs → state
        UserStore->>Router: 跳转到 /dashboard
        Router-->>User: 显示 Dashboard 页面
    end
```

### 2.2 涉及数据表汇总（登录流程）

| 阶段 | 数据表 | 操作 | 说明 |
|------|--------|------|------|
| 安全前置 | `rx_ip_rule` | **R** | IP 白/黑名单匹配 |
| 安全前置 | `rx_login_attempt` | **R** | 检查 IP 限流、用户名锁定 |
| 身份认证 | `sys_user` | **R** | 查询用户（username, password, status） |
| 认证失败 | `rx_login_attempt` | **W** | 记录失败次数 + 锁定 |
| 认证失败 | `rx_audit_log` | **W** | 审计记录 LOGIN_FAILED |
| 认证成功 | `rx_login_attempt` | **W** | 清空失败记录 |
| 认证成功 | `rx_audit_log` | **W** | 审计记录 LOGIN_SUCCESS |
| 权限加载 | `sys_user` → `sys_user_role` → `sys_role` → `sys_role_permission` → `sys_permission` | **R** | 角色→权限链 |
| 权限加载 | `sys_user_menu` → `sys_menu` | **R** | 用户专属菜单授权 |
| 菜单加载 | `sys_menu` | **R** | 菜单树（含 status/admin_only） |
| 菜单加载 | `sys_role_menu` | **R** | 角色→菜单授权 |
| 菜单加载 | `sys_tab` | **R** | Tab 页签定义 |

### 2.3 AS400 用户画像登录

```mermaid
sequenceDiagram
    actor User as 用户
    participant LoginVue as Login.vue
    participant UserStore as Pinia userStore
    participant Axios as Axios
    participant AuthCtrl as AuthController<br/>/api/v1/auth/as400-login
    participant As400LoginSvc as As400LoginService
    participant AS400 as AS400Client (IBM i)
    participant UserSvc as SysUserService
    participant JwtUtil as JwtUtil
    participant AuditSvc as AuditLogService

    User->>LoginVue: 选择服务器 + 输入 AS400 用户名/密码
    LoginVue->>UserStore: as400Login(serverId, username, password)
    UserStore->>Axios: POST /api/v1/auth/as400-login<br/>{serverId, username, password}

    rect rgb(255, 243, 224)
        Note over AuthCtrl,As400LoginSvc: 安全前置（同平台登录）
        AuthCtrl->>AuthCtrl: IP 检查 + 限流 + 锁定检查
    end

    rect rgb(232, 245, 233)
        Note over AuthCtrl,AuditSvc: AS400 委托认证
        AuthCtrl->>As400LoginSvc: login(request)
        As400LoginSvc->>AS400: 尝试连接 IBM i<br/>验证 user profile 密码
        Note right of AS400: 读: IBM i 系统用户表<br/>通过 AS400 连接验证

        alt AS400 认证失败
            AS400-->>As400LoginSvc: 连接/密码错误
            As400LoginSvc-->>AuthCtrl: BusinessException
            AuthCtrl->>AuthCtrl: registerFailure(serverId)
            AuthCtrl->>AuditSvc: AS400_LOGIN_FAILED
            AuthCtrl-->>Axios: 401
            Axios-->>User: AS400 登录失败
        else AS400 认证成功
            AS400-->>As400LoginSvc: OK
            As400LoginSvc->>UserSvc: 自动创建/映射本地用户
            Note right of UserSvc: 写: sys_user<br/>(若不存在则 INSERT<br/>status=ACTIVE)
            As400LoginSvc-->>AuthCtrl: LoginResponse
            AuthCtrl->>JwtUtil: generateToken
            AuthCtrl->>AuditSvc: AS400_LOGIN_SUCCESS
            AuthCtrl-->>Axios: 200 {token, username, permissions}
        end
    end

    rect rgb(225, 245, 254)
        Note over User,Router: 后续流程同平台登录
        Axios-->>UserStore: LoginResponse
        UserStore->>UserStore: applyLogin → fetchMenus
        UserStore->>Router: 跳转 /dashboard
    end
```

### 2.4 登出流程

```mermaid
sequenceDiagram
    actor User as 用户
    participant Layout as Layout.vue
    participant UserStore as Pinia userStore
    participant Axios as Axios
    participant AuthCtrl as AuthController<br/>/api/v1/auth/logout
    participant TokenBlacklistSvc as TokenBlacklistService
    participant JwtUtil as JwtUtil
    participant Router as Vue Router

    User->>Layout: 点击"退出登录"
    Layout->>UserStore: logout()

    rect rgb(255, 243, 224)
        Note over UserStore,TokenBlacklistSvc: 服务端吊销
        UserStore->>Axios: POST /api/v1/auth/logout<br/>Authorization: Bearer {token}
        Axios->>AuthCtrl: HTTP Request
        AuthCtrl->>JwtUtil: getJti(token) / getRemainingMs(token)
        AuthCtrl->>TokenBlacklistSvc: blacklist(jti, username, remainingMs)
        Note right of TokenBlacklistSvc: 写: rx_token_blacklist<br/>INSERT jti + expiry
        TokenBlacklistSvc-->>AuthCtrl: OK
        AuthCtrl-->>Axios: 200
    end

    rect rgb(225, 245, 254)
        Note over UserStore,Router: 前端清理
        UserStore->>UserStore: token='' / username=''<br/>permissions=[] / menus=[]<br/>localStorage.remove()
        UserStore->>UserStore: 重置 AS400 服务器选择<br/>清空模块级表格缓存
        UserStore->>Router: 跳转 /login
        Router-->>User: 显示登录页
    end
```

### 2.5 涉及数据表汇总（登出流程）

| 数据表 | 操作 | 说明 |
|--------|------|------|
| `rx_token_blacklist` | **W** | 写入 jti + expiry，JWT 有效期内该 token 失效 |

---

## 三、Axios 请求拦截器链（全局）

```mermaid
sequenceDiagram
    participant View as Vue 组件
    participant AxiosReq as Axios 请求拦截器
    participant AxiosRes as Axios 响应拦截器
    participant Backend as Spring Security Filter 链
    participant JwtFilter as JwtAuthenticationFilter

    rect rgb(225, 245, 254)
        Note over View,AxiosReq: 请求发送前
        View->>AxiosReq: 发起 API 调用
        AxiosReq->>AxiosReq: ① 重复请求去重 (同 URL 请求取消前一个)
        AxiosReq->>AxiosReq: ② 附加 Authorization: Bearer {token}
        AxiosReq->>AxiosReq: ③ 附加 X-AS400-Server: {serverId}
        Note right of AxiosReq: 从 Pinia as400ServerStore 读取<br/>当前选中的 IBM i 服务器 ID
        AxiosReq->>Backend: HTTP Request
    end

    rect rgb(255, 243, 224)
        Note over Backend,JwtFilter: 后端安全过滤
        Backend->>JwtFilter: JwtAuthenticationFilter.doFilter()
        JwtFilter->>JwtFilter: ① 提取 Authorization header
        JwtFilter->>JwtFilter: ② 解析 JWT (iss/aud/exp 校验)
        JwtFilter->>JwtFilter: ③ 检查黑名单 (rx_token_blacklist)
        JwtFilter->>JwtFilter: ④ 设置 SecurityContext
        JwtFilter-->>Backend: 放行 / 401
        Backend->>Backend: ⑤ @PreAuthorize 权限校验
        Backend-->>AxiosRes: HTTP Response
    end

    rect rgb(232, 245, 233)
        Note over AxiosRes,View: 响应处理
        AxiosRes->>AxiosRes: ① 解析 ApiResponse {code, message, data}
        alt code === 200
            AxiosRes-->>View: 返回 data
        else code === 401
            AxiosRes->>AxiosRes: 清除 token + 跳转 /login
            AxiosRes-->>View: ElMessage 提示
        else code === 403
            AxiosRes-->>View: ElMessage "无权限"
        else 其他业务错误码
            AxiosRes->>AxiosRes: 按 ERROR_CODE_I18N_MAP 映射 i18n 文案
            AxiosRes-->>View: ElMessage 提示
        end
    end
```

---

> **完整模块流程图请参阅：** [modules-mermaid-source.md](flowcharts/modules-mermaid-source.md)
>
> 该文档包含以下所有模块的完整 Mermaid 时序图源码：
>
> - 一、用户登录与认证（平台登录 / AS400登录 / 登出 / 菜单加载）
> - 二、Dashboard 仪表盘
> - 三、Job 作业中心（活动作业 / 作业控制 / SPOOL / MSGW应答 / SLA / 依赖）
> - 四、Monitor 监控中心（概览 / 指标历史 / 容量规划 / 基线 / 服务器对比 / 告警 / 巡检）
> - 五、SQL 查询与业务数据（查询执行 / 安全校验 / 业务数据浏览）
> - 六、AS400 工具集（IFS / 对象 / PF / 子系统 / 脚本 / 调度 / 执行历史 / 编译 / 源码 / 拓扑 / 消息文件 / 系统值 / 表字段）
> - 七、系统管理（用户 / 角色 / 菜单 / 权限码 / 配置 / 国际化 / 字典 / Webhook / 通知 / 公告 / 审计 / 缓存 / 文档 / 报表）
> - 八、定时任务与后台采集（CollectorScheduler / 分布式锁 / 并行采集 / 告警触发）
> - 九、请求拦截器链（前端 Axios / 后端 Security Filter）
> - 十、数据表总览（业务数据表 / IBM i QSYS2系统表 / 权限码汇总）