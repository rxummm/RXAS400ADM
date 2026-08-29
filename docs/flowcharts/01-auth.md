# 用户登录与认证

## 1.1 平台账号登录

`POST /api/v1/auth/login`

```mermaid
sequenceDiagram
    actor U as 用户
    participant Login as Login.vue
    participant Pinia as Pinia userStore
    participant Axios as Axios request.ts
    participant Auth as AuthController
    participant IpRule as IpRuleService
    participant LoginAttempt as LoginAttemptService
    participant UserSvc as SysUserService
    participant BCrypt as PasswordEncoder
    participant PermSvc as PermissionService
    participant JWT as JwtUtil
    participant AuditLog as AuditLogService
    participant MenuSvc as MenuService

    U->>Login: 输入用户名/密码
    Login->>Pinia: login(username, password)
    Pinia->>Axios: POST /api/v1/auth/login
    Axios->>Auth: LoginRequest

    Note over Auth: 阶段1: 安全前置校验
    Auth->>IpRule: checkIp(clientIp)
    Note right of IpRule: 📖 rx_ip_rule<br/>IP 黑白名单匹配
    Auth->>LoginAttempt: checkIpRate(clientIp)
    Note right of LoginAttempt: 📖 rx_login_attempt<br/>同 IP 最近失败次数 ≥ 阈值
    Auth->>LoginAttempt: checkUsernameLock(username, null)
    Note right of LoginAttempt: 📖 rx_login_attempt<br/>用户名锁定检查

    Note over Auth: 阶段2: 凭证校验
    Auth->>UserSvc: getByUsername(username)
    Note right of UserSvc: 📖 sys_user<br/>按用户名查询
    alt 用户不存在或密码错误
        Auth->>LoginAttempt: registerFailure(username, null, ip)
        Note right of LoginAttempt: ✏️ rx_login_attempt<br/>失败次数+1
        Auth->>AuditLog: auditLogin(LOGIN_FAILED)
        Note right of AuditLog: ✏️ rx_audit_log
        Auth-->>Login: 401 用户名或密码错误
    else 用户被禁用
        Auth->>AuditLog: auditLogin(LOGIN_FAILED, "用户已被禁用")
        Note right of AuditLog: ✏️ rx_audit_log
        Auth-->>Login: 403 用户已被禁用
    else 验证成功
        Auth->>LoginAttempt: clearFailure(username, null)
        Note right of LoginAttempt: ✏️ rx_login_attempt<br/>清除失败记录
    end

    Note over Auth: 阶段3: 权限加载 + JWT 签发
    Auth->>PermSvc: refresh(username)
    Note right of PermSvc: 📖 sys_user_role→sys_role→sys_role_permission→sys_permission<br/>刷新权限缓存
    Auth->>JWT: generateToken(username, permissions)
    Auth->>AuditLog: auditLogin(LOGIN_SUCCESS)
    Note right of AuditLog: ✏️ rx_audit_log
    Auth-->>Pinia: LoginResponse(token, username, permissions)

    Note over Pinia: 阶段4: 菜单加载
    Pinia->>Axios: GET /api/v1/auth/menu
    Axios->>Auth: (JWT 自动附加)
    Auth->>MenuSvc: userMenuData(username)
    Note right of MenuSvc: 📖 rx_menu, rx_role_menu, rx_user_menu, rx_permission<br/>按角色+直接授权裁剪菜单树<br/>ADMIN 角色=全部启用菜单
    Auth-->>Pinia: menus + perms + tabs

    Note over Pinia: 阶段5: 路由跳转
    Pinia->>Pinia: 存储 token/menus/permissions
    Pinia->>Login: router.push('/dashboard')
```

### 涉及数据表

| 阶段 | 数据表 | 操作 | 说明 |
|------|--------|------|------|
| 安全前置 | `rx_ip_rule` | 📖 | IP 黑白名单匹配 |
| 安全前置 | `rx_login_attempt` | 📖 | 同 IP 失败次数 / 用户名锁定 |
| 凭证校验 | `sys_user` | 📖 | 按用户名查询用户信息 |
| 登录失败 | `rx_login_attempt` | ✏️ | 失败次数 +1 |
| 登录失败 | `rx_audit_log` | ✏️ | 记录失败审计 |
| 登录成功 | `rx_login_attempt` | ✏️ | 清除失败记录 |
| 权限加载 | `sys_user_role` | 📖 | 用户角色关联 |
| 权限加载 | `sys_role` | 📖 | 角色信息 |
| 权限加载 | `sys_role_permission` | 📖 | 角色权限关联 |
| 权限加载 | `sys_permission` | 📖 | 权限码列表 |
| 登录成功 | `rx_audit_log` | ✏️ | 记录成功审计 |
| 菜单加载 | `rx_menu` | 📖 | 菜单树 + 按钮权限码 |
| 菜单加载 | `rx_role_menu` | 📖 | 角色菜单授权 |
| 菜单加载 | `rx_user_menu` | 📖 | 用户直接菜单授权 |
| 菜单加载 | `rx_tab` | 📖 | 标签页定义 |

---

## 1.2 AS400 用户画像登录

`POST /api/v1/auth/as400-login`

```mermaid
sequenceDiagram
    actor U as 用户
    participant Login as Login.vue
    participant Auth as AuthController
    participant IpRule as IpRuleService
    participant LoginAttempt as LoginAttemptService
    participant As400Login as As400LoginService
    participant AS400Client as AS400ClientProvider
    participant IBMi as IBM i (AS400)
    participant UserSvc as SysUserService
    participant PermSvc as PermissionService
    participant JWT as JwtUtil
    participant AuditLog as AuditLogService

    U->>Login: 输入 AS400 用户名/密码 + 选择服务器
    Login->>Auth: POST /api/v1/auth/as400-login

    Note over Auth: 安全前置（同平台登录）
    Auth->>IpRule: checkIp(clientIp)
    Auth->>LoginAttempt: checkIpRate(clientIp)
    Auth->>LoginAttempt: checkUsernameLock(username, serverId)

    Auth->>As400Login: login(request)
    As400Login->>AS400Client: JTOpenAS400Client 连接
    AS400Client->>IBMi: 委托 IBM i 认证
    Note right of IBMi: 🗄️ IBM i 用户画像<br/>QSYS2.USER_INFO
    IBMi-->>AS400Client: 认证结果
    AS400Client-->>As400Login: 连接成功

    alt 认证失败
        As400Login-->>Auth: throw BusinessException
        Auth->>LoginAttempt: registerFailure(username, serverId, ip)
        Auth->>AuditLog: auditLogin(AS400_LOGIN_FAILED)
        Auth-->>Login: 登录失败
    else 认证成功
        As400Login->>UserSvc: 自动创建/映射本地用户
        Note right of UserSvc: ✏️ sys_user<br/>若不存在则自动创建
        As400Login-->>Auth: LoginResponse
        Auth->>LoginAttempt: clearFailure(username, serverId)
        Auth->>AuditLog: auditLogin(AS400_LOGIN_SUCCESS)
    end

    Auth->>PermSvc: refresh(username)
    Auth->>JWT: generateToken(username, permissions)
    Auth-->>Login: LoginResponse
```

---

## 1.3 登出流程

`POST /api/v1/auth/logout`

```mermaid
sequenceDiagram
    actor U as 用户
    participant Login as Login.vue
    participant Pinia as Pinia userStore
    participant Axios as Axios
    participant Auth as AuthController
    participant JWT as JwtUtil
    participant Blacklist as TokenBlacklistService

    U->>Login: 点击登出
    Login->>Pinia: logout()
    Pinia->>Axios: POST /api/v1/auth/logout<br/>🔒 isAuthenticated()
    Axios->>Auth: Authorization: Bearer {token}

    Auth->>JWT: isValid(token)
    Auth->>JWT: getJti(token) / getUsername(token) / getRemainingMs(token)
    Auth->>Blacklist: blacklist(jti, username, remainingMs)
    Note right of Blacklist: ✏️ rx_token_blacklist<br/>JWT jti 吊销，有效期内不可再认证

    Auth-->>Pinia: 200 OK
    Pinia->>Pinia: 清除 localStorage token/menus/permissions
    Pinia->>Login: router.push('/login')
```