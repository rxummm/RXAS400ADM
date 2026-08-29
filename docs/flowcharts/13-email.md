# 邮件中心

`/api/v1/email/*`

## 写邮件 {#email-compose}

```mermaid
sequenceDiagram
    actor U as 用户
    participant Compose as mail/compose.vue
    participant GroupCtrl as EmailGroupController
    participant SendCtrl as EmailSendController
    participant SendSvc as IEmailService
    participant SMTP as SMTP 服务器
    participant DB as 业务数据库

    U->>Compose: 进入写邮件
    Compose->>GroupCtrl: GET /api/v1/email/groups/all<br/>🔒 EMAIL_SEND
    Note right of GroupCtrl: IEmailGroupService.listAll()<br/>📖 rx_email_group
    GroupCtrl-->>Compose: List<EmailGroupVO&gt; 收件人分组下拉

    U->>Compose: 填写收件人/主题/正文/优先级
    Compose->>SendCtrl: POST /api/v1/email/send<br/>🔒 EMAIL_SEND<br/>@OperateLog("发送邮件")
    Note right of SendCtrl: Body: EmailSendDTO<br/>{subject, text, recipients, priority}
    SendCtrl->>SendSvc: send(MailMessage.manual(subject, text, recipients, priority))
    Note right of SendSvc: 构建 MimeMessage<br/>📖 rx_config (email.*) → SMTP 配置
    SendSvc->>SMTP: 发送邮件
    SMTP-->>SendSvc: 发送结果
    Note right of SendSvc: ✏️ rx_email_log<br/>记录发送日志
    SendSvc-->>SendCtrl: void
    SendCtrl-->>Compose: 200 OK（发送成功）
```

## 邮件设置 {#email-settings}

```mermaid
sequenceDiagram
    actor U as 管理员
    participant ConfigView as system/emailConfig/index.vue
    participant ConfigCtrl as EmailConfigController
    participant ConfigSvc as IEmailService
    participant DB as 业务数据库
    participant SMTP as SMTP 服务器

    U->>ConfigView: 进入邮件设置
    ConfigView->>ConfigCtrl: GET /api/v1/email/config<br/>🔒 EMAIL_MANAGE
    ConfigCtrl->>ConfigSvc: listConfigs()
    Note right of ConfigSvc: 📖 rx_email_config<br/>host / port / username / password / ssl / timeout
    ConfigSvc-->>ConfigCtrl: List<EmailConfigVO&gt;
    ConfigCtrl-->>ConfigView: SMTP 配置列表（密码掩码）

    U->>ConfigView: 修改 SMTP 配置
    ConfigView->>ConfigCtrl: PUT /api/v1/email/config<br/>🔒 EMAIL_MANAGE<br/>@OperateLog("更新邮件配置")
    Note right of ConfigCtrl: Body: EmailConfigDTO
    ConfigCtrl->>ConfigSvc: updateConfigs(configs)
    Note right of ConfigSvc: ✏️ rx_email_config
    ConfigSvc-->>ConfigCtrl: void
    ConfigCtrl-->>ConfigView: 200 OK

    U->>ConfigView: 点击测试发送
    ConfigView->>ConfigCtrl: POST /api/v1/email/config/test-send?to=xxx@xxx.com<br/>🔒 EMAIL_MANAGE<br/>@OperateLog("发送测试邮件")
    ConfigCtrl->>ConfigSvc: sendTestEmail(to)
    Note right of ConfigSvc: 使用当前 SMTP 配置发送测试邮件
    ConfigSvc->>SMTP: 发送测试邮件
    SMTP-->>ConfigSvc: 发送结果
    ConfigSvc-->>ConfigCtrl: void
    ConfigCtrl-->>ConfigView: 测试发送成功/失败
```

## 收件人分组 {#email-groups}

```mermaid
sequenceDiagram
    actor U as 管理员
    participant GroupView as system/emailGroups/index.vue
    participant GroupCtrl as EmailGroupController
    participant GroupSvc as IEmailGroupService
    participant DB as 业务数据库

    U->>GroupView: 进入收件人分组
    GroupView->>GroupCtrl: GET /api/v1/email/groups?current=1&size=10&keyword=xxx<br/>🔒 EMAIL_MANAGE
    GroupCtrl->>GroupSvc: page(current, size, keyword)
    Note right of GroupSvc: 📖 rx_email_group
    GroupSvc-->>GroupCtrl: PageResult<EmailGroupVO&gt;
    GroupCtrl-->>GroupView: 分组分页列表

    U->>GroupView: 新建分组
    GroupView->>GroupCtrl: POST /api/v1/email/groups<br/>🔒 EMAIL_MANAGE<br/>@OperateLog("创建收件人分组")
    Note right of GroupCtrl: Body: EmailGroupCreateDTO<br/>{name, recipients[]}
    GroupCtrl->>GroupSvc: create(dto)
    Note right of GroupSvc: ✏️ rx_email_group<br/>✏️ rx_email_recipient
    GroupSvc-->>GroupCtrl: void
    GroupCtrl-->>GroupView: 200 OK

    U->>GroupView: 编辑分组
    GroupView->>GroupCtrl: PUT /api/v1/email/groups/{id}<br/>🔒 EMAIL_MANAGE<br/>@OperateLog("更新收件人分组")
    GroupCtrl->>GroupSvc: update(id, dto)
    Note right of GroupSvc: ✏️ rx_email_group<br/>✏️ rx_email_recipient
    GroupSvc-->>GroupCtrl: void
    GroupCtrl-->>GroupView: 200 OK

    U->>GroupView: 删除分组
    GroupView->>GroupCtrl: DELETE /api/v1/email/groups/{id}<br/>🔒 EMAIL_MANAGE<br/>@OperateLog("删除收件人分组")
    GroupCtrl->>GroupSvc: delete(id)
    Note right of GroupSvc: ✏️ rx_email_group<br/>✏️ rx_email_recipient（级联删除）
    GroupSvc-->>GroupCtrl: void
    GroupCtrl-->>GroupView: 200 OK
```

## 邮件日志 {#email-logs}

```mermaid
sequenceDiagram
    actor U as 管理员
    participant LogView as system/emailLog/index.vue
    participant LogCtrl as EmailLogController
    participant LogSvc as IEmailLogService
    participant DB as 业务数据库

    U->>LogView: 进入邮件日志
    LogView->>LogCtrl: GET /api/v1/email/logs?current=1&size=10&channel=xxx&status=xxx&keyword=xxx<br/>🔒 EMAIL_VIEW
    LogCtrl->>LogSvc: page(current, size, channel, status, keyword)
    Note right of LogSvc: 📖 rx_email_log
    LogSvc-->>LogCtrl: PageResult<EmailLogVO&gt;
    LogCtrl-->>LogView: 邮件日志分页列表

    U->>LogView: 点击查看详情
    LogView->>LogCtrl: GET /api/v1/email/logs/{id}<br/>🔒 EMAIL_VIEW
    LogCtrl->>LogSvc: detail(id)
    Note right of LogSvc: 📖 rx_email_log
    LogSvc-->>LogCtrl: EmailLogVO
    LogCtrl-->>LogView: 邮件详情（含完整发送内容）
```

### 涉及功能模块

| 模块 | 路由 | 前端组件 | 后端 Controller | Service |
|------|------|----------|-----------------|---------|
| 写邮件 | `/mail/compose` | `mail/compose.vue` | `EmailSendController` | `IEmailService` |
| 邮件设置 | `/system/email-config` | `system/emailConfig/index.vue` | `EmailConfigController` | `IEmailService` |
| 收件人分组 | `/system/email-groups` | `system/emailGroups/index.vue` | `EmailGroupController` | `IEmailGroupService` |
| 邮件日志 | `/system/email-log` | `system/emailLog/index.vue` | `EmailLogController` | `IEmailLogService` |

### 涉及数据表

| 数据表 | 操作 | 说明 |
|--------|------|------|
| `rx_email_config` | 📖✏️ | SMTP 配置（host/port/username/password/ssl/timeout） |
| `rx_email_group` | 📖✏️ | 收件人分组 |
| `rx_email_recipient` | 📖✏️ | 分组内收件人 |
| `rx_email_log` | 📖✏️ | 邮件发送日志 |
| `rx_config` | 📖 | SMTP 配置（email.* 键值对） |

### 涉及权限码

| 权限码 | 说明 | 使用位置 |
|--------|------|---------|
| `EMAIL_SEND` | 发送邮件 | 写邮件、获取分组列表 |
| `EMAIL_MANAGE` | 邮件管理 | 邮件设置、收件人分组 CRUD |
| `EMAIL_VIEW` | 邮件日志查看 | 邮件日志查询 |