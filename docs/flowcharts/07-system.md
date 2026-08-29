# 系统管理

## 7.1 用户管理 {#users}

`/api/v1/users/*`

```mermaid
sequenceDiagram
    actor U as 管理员
    participant UserView as system/Users.vue
    participant UserCtrl as SysUserController
    participant UserSvc as SysUserService
    participant UserMenuSvc as UserMenuService
    participant DB as 业务数据库

    Note over U: 用户列表
    U->>UserView: 进入用户管理
    UserView->>UserCtrl: GET /api/v1/users?current=1&size=10&keyword=xxx<br/>🔒 USER_MANAGE
    Note right of UserSvc: 📖 sys_user<br/>分页查询
    UserCtrl-->>UserView: PageResult<UserVO>

    Note over U: 创建用户
    U->>UserView: 新建用户
    UserView->>UserCtrl: POST /api/v1/users 🔒 USER_MANAGE
    Note right of UserSvc: ✏️ sys_user
    UserCtrl-->>UserView: UserVO

    Note over U: 编辑用户
    U->>UserView: 编辑用户
    UserView->>UserCtrl: PUT /api/v1/users/{id} 🔒 USER_MANAGE
    Note right of UserSvc: ✏️ sys_user
    UserCtrl-->>UserView: UserVO

    Note over U: 删除用户
    U->>UserView: 删除用户
    UserView->>UserCtrl: DELETE /api/v1/users/{id} 🔒 USER_MANAGE
    Note right of UserSvc: ✏️ sys_user (逻辑删除)
    UserCtrl-->>UserView: 200 OK

    Note over U: 用户菜单授权
    U->>UserView: 打开授权弹窗
    UserView->>UserCtrl: GET /api/v1/users/{id}/menus/manageable-tree 🔒 USER_MANAGE
    Note right of UserMenuSvc: 📖 rx_menu (排除已拥有)
    UserCtrl-->>UserView: 可分配权限树

    U->>UserView: 勾选菜单 → 保存
    UserView->>UserCtrl: POST /api/v1/users/{id}/menus/add 🔒 USER_MANAGE<br/>@OperateLog("用户菜单授权")
    Note right of UserMenuSvc: ✏️ rx_user_menu<br/>追加授权（幂等）
    UserCtrl-->>UserView: 200 OK

    U->>UserView: 移除授权
    UserView->>UserCtrl: POST /api/v1/users/{id}/menus/remove 🔒 USER_MANAGE<br/>@OperateLog("移除用户菜单授权")
    Note right of UserMenuSvc: ✏️ rx_user_menu<br/>移除（含子孙）
    UserCtrl-->>UserView: 200 OK
```

---

## 7.2 角色管理 {#roles}

`/api/v1/roles/*`

```mermaid
sequenceDiagram
    actor U as 管理员
    participant RoleView as system/roles/index.vue
    participant RoleCtrl as SysRoleController
    participant RoleSvc as RoleService
    participant DB as 业务数据库

    U->>RoleView: 进入角色管理
    RoleView->>RoleCtrl: GET /api/v1/roles 🔒 USER_MANAGE
    Note right of RoleSvc: 📖 sys_role<br/>含已授权菜单 ID
    RoleCtrl-->>RoleView: 角色列表

    U->>RoleCtrl: GET /api/v1/roles/page?current=1&size=10 🔒 ROLE_MANAGE
    Note right of RoleSvc: 📖 sys_role<br/>分页查询
    RoleCtrl-->>RoleView: PageResult

    U->>RoleCtrl: POST /api/v1/roles 🔒 ROLE_MANAGE<br/>@OperateLog("新增角色")
    Note right of RoleSvc: ✏️ sys_role
    RoleCtrl-->>RoleView: 角色详情

    U->>RoleCtrl: PUT /api/v1/roles/{id} 🔒 ROLE_MANAGE<br/>@OperateLog("修改角色")
    Note right of RoleSvc: ✏️ sys_role + sys_role_menu
    RoleCtrl-->>RoleView: 更新后角色

    U->>RoleCtrl: DELETE /api/v1/roles/{id} 🔒 ROLE_MANAGE<br/>@OperateLog("删除角色")
    Note right of RoleSvc: ✏️ sys_role (级联删除关联)
    RoleCtrl-->>RoleView: 200 OK
```

---

## 7.3 菜单管理 {#menus}

`/api/v1/menus/*`

```mermaid
sequenceDiagram
    actor U as 管理员
    participant MenuView as system/menus/index.vue
    participant MenuCtrl as SysMenuController
    participant MenuSvc as MenuService
    participant DB as 业务数据库

    U->>MenuView: 进入菜单管理
    MenuView->>MenuCtrl: GET /api/v1/menus/tree 🔒 MENU_MANAGE
    Note right of MenuSvc: 📖 rx_menu<br/>全量菜单树
    MenuCtrl-->>MenuView: 菜单树

    U->>MenuView: 新建菜单
    MenuView->>MenuCtrl: POST /api/v1/menus 🔒 MENU_MANAGE<br/>@OperateLog("新增菜单")
    Note right of MenuSvc: ✏️ rx_menu
    MenuCtrl-->>MenuView: 菜单详情

    U->>MenuView: 编辑菜单
    MenuView->>MenuCtrl: PUT /api/v1/menus/{id} 🔒 MENU_MANAGE<br/>@OperateLog("修改菜单")
    Note right of MenuSvc: ✏️ rx_menu
    MenuCtrl-->>MenuView: 更新后菜单

    U->>MenuView: 切换显示/隐藏
    MenuView->>MenuCtrl: PUT /api/v1/menus/{id}/status?status=1 🔒 MENU_MANAGE<br/>@OperateLog("切换菜单状态")
    Note right of MenuSvc: ✏️ rx_menu.status
    MenuCtrl-->>MenuView: 更新后菜单

    U->>MenuView: 删除菜单
    MenuView->>MenuCtrl: DELETE /api/v1/menus/{id} 🔒 MENU_MANAGE<br/>@OperateLog("删除菜单")
    Note right of MenuSvc: ✏️ rx_menu (级联删除)
    MenuCtrl-->>MenuView: 200 OK
```

---

## 7.4 权限码管理 {#permissions}

`/api/v1/permissions/*`

```mermaid
sequenceDiagram
    actor U as 管理员
    participant PermView as system/permissions/index.vue
    participant PermCtrl as PermissionController
    participant PermSvc as PermissionManageService
    participant DB as 业务数据库

    U->>PermView: 进入权限码管理
    PermView->>PermCtrl: GET /api/v1/permissions?current=1&size=20&keyword=xxx&module=xxx<br/>🔒 PERMISSION_MANAGE
    Note right of PermSvc: 📖 rx_permission<br/>分页查询
    PermCtrl-->>PermView: PageResult

    U->>PermCtrl: GET /api/v1/permissions/all
    Note right of PermSvc: 📖 rx_permission<br/>全部权限码（下拉字典）
    PermCtrl-->>PermView: 权限码列表

    U->>PermCtrl: GET /api/v1/permissions/suggest?menuTitle=xxx 🔒 MENU_MANAGE
    Note right of PermSvc: 📖 rx_permission<br/>按菜单业务域过滤建议码
    PermCtrl-->>PermView: 建议码列表

    U->>PermCtrl: POST /api/v1/permissions 🔒 PERMISSION_MANAGE
    Note right of PermSvc: ✏️ rx_permission
    PermCtrl-->>PermView: 权限码详情

    U->>PermCtrl: PUT /api/v1/permissions/{id} 🔒 PERMISSION_MANAGE
    Note right of PermSvc: ✏️ rx_permission
    PermCtrl-->>PermView: 更新后权限码

    U->>PermCtrl: DELETE /api/v1/permissions/{id} 🔒 PERMISSION_MANAGE
    Note right of PermSvc: ✏️ rx_permission<br/>（被引用则拒绝）
    PermCtrl-->>PermView: 200 OK
```

---

## 7.5 系统配置、国际化、数据字典 {#config}

```mermaid
sequenceDiagram
    actor U as 管理员
    participant ConfigCtrl as ConfigController
    participant I18nCtrl as I18nController
    participant DictCtrl as DictController
    participant DB as 业务数据库

    Note over U: 系统参数管理
    U->>ConfigCtrl: GET /api/v1/configs 🔒 SYS_CONFIG_MANAGE
    Note right of ConfigCtrl: 📖 rx_config
    ConfigCtrl-->>U: 参数列表（含掩码处理）

    U->>ConfigCtrl: PUT /api/v1/configs/{key} 🔒 SYS_CONFIG_MANAGE
    Note right of ConfigCtrl: ✏️ rx_config
    ConfigCtrl-->>U: 更新后参数

    Note over U: 国际化管理
    U->>I18nCtrl: GET /api/v1/i18n 🔒 I18N_MANAGE
    Note right of I18nCtrl: 📖 rx_i18n
    I18nCtrl-->>U: i18n 键值列表

    U->>I18nCtrl: PUT /api/v1/i18n/{key} 🔒 I18N_MANAGE
    Note right of I18nCtrl: ✏️ rx_i18n
    I18nCtrl-->>U: 更新后翻译

    Note over U: 数据字典
    U->>DictCtrl: GET /api/v1/dicts/types
    Note right of DictCtrl: 📖 rx_dict_type
    DictCtrl-->>U: 字典类型列表

    U->>DictCtrl: GET /api/v1/dicts/items?typeCode=xxx
    Note right of DictCtrl: 📖 rx_dict_item
    DictCtrl-->>U: 字典项列表

    U->>DictCtrl: GET /api/v1/dicts/items/enabled?typeCode=xxx
    Note right of DictCtrl: 📖 rx_dict_item (启用中)
    DictCtrl-->>U: 下拉引用数据

    U->>DictCtrl: POST/PUT/DELETE /api/v1/dicts/types/* 🔒 DICT_MANAGE
    Note right of DictCtrl: ✏️ rx_dict_type / rx_dict_item
    DictCtrl-->>U: 操作结果
```

---

## 7.6 其他功能 {#other}

Webhook / 通知 / 公告 / 审计日志 / 登录日志 / 缓存管理 / 文档 / 报表 / 收藏 / 日历 / Dashboard 小组件

```mermaid
sequenceDiagram
    actor U as 管理员/用户
    participant WebhookCtrl as WebhookController
    participant NotifCtrl as NotificationController
    participant NoticeCtrl as NoticeController
    participant AuditCtrl as AuditLogController
    participant LoginLogCtrl as LoginLogController
    participant CacheCtrl as CacheController
    participant DocCtrl as DocController
    participant ReportCtrl as ReportController
    participant HealthCtrl as HealthController
    participant FavCtrl as FavoriteController
    participant CalCtrl as CalendarController
    participant WidgetCtrl as DashboardWidgetController
    participant DB as 业务数据库

    Note over U: Webhook 管理
    U->>WebhookCtrl: GET /api/v1/webhooks 🔒 WEBHOOK_MANAGE
    Note right of WebhookCtrl: 📖 rx_webhook
    WebhookCtrl-->>U: Webhook 配置列表

    U->>WebhookCtrl: POST /api/v1/webhooks 🔒 WEBHOOK_MANAGE
    Note right of WebhookCtrl: ✏️ rx_webhook
    WebhookCtrl-->>U: 新建配置

    Note over U: 站内通知
    U->>NotifCtrl: GET /api/v1/notifications/unread-count
    Note right of NotifCtrl: 📖 rx_notification
    NotifCtrl-->>U: 未读角标数

    U->>NotifCtrl: GET /api/v1/notifications/mine?unreadOnly=true
    Note right of NotifCtrl: 📖 rx_notification
    NotifCtrl-->>U: 通知列表

    U->>NotifCtrl: POST /api/v1/notifications/{id}/read
    Note right of NotifCtrl: ✏️ rx_notification
    NotifCtrl-->>U: 标记已读

    Note over U: 公告管理
    U->>NoticeCtrl: GET /api/v1/notices 🔒 NOTICE_VIEW
    Note right of NoticeCtrl: 📖 rx_notice
    NoticeCtrl-->>U: 公告列表

    Note over U: 审计日志
    U->>AuditCtrl: GET /api/v1/audit-logs?module=xxx&username=xxx&action=xxx<br/>🔒 AUDIT_VIEW
    Note right of AuditCtrl: 📖 rx_audit_log
    AuditCtrl-->>U: 分页审计日志

    Note over U: 登录日志
    U->>LoginLogCtrl: GET /api/v1/login-logs 🔒 USER_MANAGE
    Note right of LoginLogCtrl: 📖 rx_audit_log
    LoginLogCtrl-->>U: 登录日志列表

    Note over U: 缓存管理
    U->>CacheCtrl: GET /api/v1/cache 🔒 SYS_CONFIG_MANAGE
    Note right of CacheCtrl: Redis 缓存信息
    CacheCtrl-->>U: 缓存统计

    U->>CacheCtrl: DELETE /api/v1/cache/{key} 🔒 SYS_CONFIG_MANAGE
    Note right of CacheCtrl: Redis DEL
    CacheCtrl-->>U: 清除缓存

    Note over U: 文档管理
    U->>DocCtrl: GET /api/v1/docs 🔒 DOC_VIEW
    Note right of DocCtrl: 📖 rx_doc
    DocCtrl-->>U: 文档列表

    Note over U: 报表
    U->>ReportCtrl: GET /api/v1/reports 🔒 REPORT_VIEW
    Note right of ReportCtrl: 📖 rx_report
    ReportCtrl-->>U: 报表列表

    Note over U: 健康检查
    U->>HealthCtrl: GET /api/v1/health
    Note over HealthCtrl: 无需认证
    HealthCtrl-->>U: 系统状态

    Note over U: 收藏夹
    U->>FavCtrl: GET /api/v1/favorites 🔒 FAVORITE_VIEW
    Note right of FavCtrl: 📖 rx_favorite
    FavCtrl-->>U: 收藏列表

    Note over U: 日历
    U->>CalCtrl: GET /api/v1/calendars 🔒 CALENDAR_VIEW
    Note right of CalCtrl: 📖 rx_calendar
    CalCtrl-->>U: 日历数据

    Note over U: Dashboard 小组件
    U->>WidgetCtrl: GET /api/v1/dashboard-widgets 🔒 DASHBOARD_VIEW
    Note right of WidgetCtrl: 📖 rx_dashboard_widget
    WidgetCtrl-->>U: 小组件配置
```

### 涉及权限码

| 权限码 | 模块 |
|--------|------|
| `USER_MANAGE` | 用户管理 |
| `ROLE_MANAGE` | 角色管理 |
| `MENU_MANAGE` | 菜单管理 |
| `PERMISSION_MANAGE` | 权限码管理 |
| `SYS_CONFIG_MANAGE` | 系统配置 |
| `I18N_MANAGE` | 国际化 |
| `DICT_MANAGE` | 数据字典 |
| `WEBHOOK_MANAGE` | Webhook |
| `AUDIT_VIEW` | 审计日志 |
| `DOC_VIEW` / `DOC_MANAGE` | 文档管理 |
| `REPORT_VIEW` | 报表查看 |
| `FAVORITE_VIEW` | 收藏夹 |
| `CALENDAR_VIEW` | 日历 |
| `DASHBOARD_VIEW` | 仪表盘小组件 |