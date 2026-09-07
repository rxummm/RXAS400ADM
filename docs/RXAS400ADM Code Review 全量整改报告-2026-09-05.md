# RXAS400ADM Code Review 全量整改报告

**项目：RXAS400ADM**

**Review 版本：RXAS400ADM-0905**

**Review 范围：Backend + Frontend + DB Migration + Build/Quality Gates**

**Review 类型：源码级 Static Code Review / Architecture Review / Security Review / Code Quality Review**

---

# 1. Executive Summary

## 1.1 总体评价

RXAS400ADM 当前不是一个“架构混乱、不可维护”的项目。

相反，从代码结构看，项目已经具备比较明显的企业级系统雏形：

```text
                    RXAS400ADM
                        │
        ┌───────────────┼────────────────┐
        │               │                │
     Security         System           AS400
        │               │                │
       JWT             RBAC          JT400/Mock
       CORS            User           SQL
       RateLimit       Role           CL
       Audit           Menu           IFS
        │               │                │
        └───────────────┼────────────────┘
                        │
                     App/API
                        │
                     Vue3
```

特别值得肯定的地方：

- Controller / Service / Mapper 分层基本清晰
- AS400 Client 做了领域能力拆分
- JWT + DB permission model 已经比较成熟
- IBM i connection pool / lifecycle 设计较好
- SQL 参数化意识较强
- IFS 已经加入路径沙箱
- Webhook 已考虑 SSRF
- 项目有比较完整的静态质量门禁
- Flyway migration 管理比较规范
- 前端 API 层已经集中处理认证、错误码、语言、AS400 Server routing
- 前端已有较完整 TypeScript 化
- 很多 N+1、查询数量、内存读取问题已经被主动优化

但是：

> 当前最大的风险不是“代码写得不好”，而是**多个跨步骤业务操作没有事务/状态机保护，以及 IBM i 执行边界还没有形成统一的安全策略。**

可以概括成：

```text
                     当前最大风险
                           │
             ┌─────────────┴─────────────┐
             │                           │
       Transaction                  AS400 Execution
       Consistency                     Boundary
             │                           │
       multi-table                    CL command
       race condition                 IFS
       partial update                 Job
       approval                       UserProfile
       RBAC                           DataArea
                                      SystemValue
```

---

# 2. Review 结论等级

综合源码质量：

| 领域 | 评价 |
|---|---:|
| 总体架构 | 8/10 |
| Security 基础设施 | 8/10 |
| RBAC | 7.5/10 |
| IBM i Integration | 7.5/10 |
| API Layer | 8/10 |
| DB/Migration | 8/10 |
| Transaction Consistency | 5/10 |
| AS400 Command Security | 6/10 |
| Error Handling | 6.5/10 |
| Backend Code Quality | 7/10 |
| Frontend Architecture | 7/10 |
| Frontend Maintainability | 6.5/10 |
| Testability | 6.5/10 |
| Production Readiness | 6.5/10 |

### 综合：

# **约 7.3 / 10**

不是需要推倒重来的项目。

正确策略应该是：

> **保留现有架构，集中解决 P0/P1 问题，再做模块化重构。**

---

# 3. Findings 总览

本次 Review 最终归纳出：

| 等级 | 数量级 | 主要问题 |
|---|---:|---|
| P0 Critical | 5 | Token rotation、权限审批、用户同步、事务一致性等 |
| P1 High | 18+ | AS400 command、IFS、RBAC、调度、权限模型 |
| P2 Medium | 25+ | Cache、性能、并发、日志、配置 |
| P3 Low | 15+ | Maintainability、命名、重构 |
| Good Design | 20+ | Security、JT400、分层、静态门禁等 |

---

# 4. P0 Critical Findings

---

## CR-001 — Refresh Token Rotation 并发竞态

### 文件

```text
backend/rxas400adm-security/
src/main/java/com/rxas400adm/security/service/AuthService.java
```

### Class

```text
AuthService
```

### Method

```text
refreshToken()
```

### Line

```text
31-51
```

核心逻辑：

```java
if (tokenBlacklistService.isBlacklisted(jti)) {
    throw ...
}

tokenBlacklistService.blacklist(jti, ...);

generateToken();
generateRefreshToken();
```

同时：

```text
TokenBlacklistService.java
64-84
```

对 `DuplicateKeyException`：

```java
catch (DuplicateKeyException e) {
    cache.put(jti, true);
}
```

### 问题

存在：

```text
check → insert
```

TOCTOU race。

两个并发 refresh：

```text
Request A                 Request B

check=false               check=false

insert OK                 insert duplicate

issue R2                  issue R3
```

因此：

```text
R1
├── R2
└── R3
```

违反 Refresh Token Rotation 的“一次消费”语义。

### 风险

攻击者或浏览器多 Tab 可以同时使用同一个 refresh token。

### 整改

增加：

```text
consumeRefreshToken(jti)
```

必须原子完成。

推荐：

```sql
INSERT INTO rx_token_blacklist(...)
VALUES(...)
```

成功：

```text
allow
```

Duplicate：

```text
reject
```

而不是继续生成 token。

### 优先级

**P0**

---

# CR-002 — Permission Request Approval 非原子

### 文件

```text
backend/rxas400adm-system/
src/main/java/com/rxas400adm/system/service/PermissionRequestService.java
```

### Method

```text
approve()
grantPermission()
```

### Line

```text
approve: 139-160
grantPermission: 250-294
```

流程：

```text
permission_request
      ↓
permission
      ↓
role
      ↓
role_permission
      ↓
user_role
      ↓
request = APPROVED
```

没有 transaction。

可能产生：

```text
权限已经授予
+
申请仍然 PENDING
```

或者：

```text
role 创建成功
role_permission 失败
user_role 不存在
```

### 另外

两个管理员可以同时：

```text
requirePending()
```

两边都读到：

```text
PENDING
```

然后同时审批。

### 整改

首先 CAS：

```sql
UPDATE rx_permission_request
SET status = 'APPROVED'
WHERE id = ?
AND status = 'PENDING'
```

必须：

```text
affectedRows == 1
```

然后 transaction 内完成权限变更。

推荐：

```text
CAS claim
   ↓
MySQL Transaction
   ↓
permission
role
role_permission
user_role
   ↓
COMMIT
   ↓
afterCommit Event
```

### 优先级

**P0**

---

# CR-003 — AS400 User Sync 5000 行限制可能导致误删除

### 文件

```text
backend/rxas400adm-security/
src/main/java/com/rxas400adm/security/service/As400LoginSyncService.java
```

### Method

```text
loadAllProfiles()
```

### Line

```text
122-135
```

使用：

```text
queryListCheckedBounded(..., 5000)
```

后面又使用：

```text
missing
→ cleanupMissing()
```

### 风险

如果：

```text
IBM i USER_INFO > 5000
```

那么第 5001 个以后用户不会进入结果集。

本地：

```text
user exists
```

IBM i 返回：

```text
not in first 5000
```

系统可能判断：

```text
missing
```

然后删除本地用户。

### 这是严重的数据一致性问题。

### 正确方式

不要：

```text
全量用户扫描 + 5000 上限
```

改成：

```text
分页读取
```

或者：

```text
本地用户集合
      ↓
批量 IN 查询 IBM i
      ↓
500/1000 一批
```

### 优先级

**P0**

---

# CR-004 — User CRUD 非原子

### 文件

```text
SysUserServiceImpl.java
```

### create

```text
88-112
```

流程：

```text
insert user
resolve roles
bind roles
```

### update

```text
116-142
```

流程：

```text
update user
delete roles
insert roles
publish event
```

### delete

```text
146-156
```

流程：

```text
delete roles
delete user
publish event
```

任何一步失败都会形成 partial state。

### 例如

```text
DELETE roles = success
INSERT new roles = fail
```

结果：

```text
User = ACTIVE
Roles = 0
```

### 根因

项目：

```text
scripts/check-transactional.sh
```

强制：

```text
0 @Transactional
```

这是一个架构策略问题。

### 建议

把：

> “禁止事务”

改成：

> “禁止跨 IBM i / MySQL 的分布式事务，但允许本地 MySQL Transaction”。

即：

```text
MySQL Transaction
+
Outbox
+
Async IBM i
```

而不是：

```text
XA MySQL + IBM i
```

### 优先级

**P0/P1**

---

# CR-005 — AS400 Login Role Assignment 非原子

### 文件

```text
As400LoginService.java
```

### Methods

```text
createUser
applyRoles
```

### Line

约：

```text
106-119
148-173
```

流程：

```text
create local user
    ↓
delete old roles
    ↓
insert roles
```

中间任何失败都会形成：

```text
user exists
+
role incomplete
```

### 优先级

**P1**

---

# 5. Security Findings

---

## SEC-001 — Refresh disabled user 仍可能签发 JWT

### 文件

```text
AuthService.java
31-51
```

当前：

```text
refresh token
 ↓
loadPermissions
 ↓
generate token
```

禁用用户最终可能得到：

```text
JWT
+
empty authority
```

不是直接越权，但认证语义不干净。

### 建议

Refresh 时显式：

```text
user exists
AND status = ACTIVE
```

再签发。

---

# SEC-002 — Permission Cache 为 JVM-local

### 文件

```text
PermissionService.java
27-30
```

```java
Caffeine
expireAfterWrite(60s)
```

单节点很好。

多节点：

```text
Node A cache
Node B cache
Node C cache
```

权限修改只能即时影响当前节点。

### 建议

如果未来多节点：

```text
Redis L2
+
Caffeine L1
+
permission changed event
```

---

# SEC-003 — Permission 加载存在降级语义问题

### 文件

```text
PermissionService.java
```

如果：

```text
user permissions
```

加载成功，但：

```text
menu permissions
```

失败：

```java
catch (Exception e) {
    log.error(...);
}
```

继续返回部分权限。

建议明确：

```text
FAIL CLOSED
```

或者明确标记：

```text
DEGRADED
```

不能仅依赖日志。

---

# SEC-004 — Proxy IP Resolver 重复实现

出现于：

```text
AuthController
RateLimitFilter
OperateLogAspect
```

当前至少三处重复：

```text
X-Forwarded-For
X-Real-IP
trusted proxy
```

### 建议

统一：

```text
ClientIpResolver
```

所有组件只调用：

```text
resolve(request)
```

---

# SEC-005 — RateLimit 仅 JVM-local

### 文件

```text
RateLimitFilter.java
```

使用：

```text
Caffeine<String, Bucket>
```

多节点：

```text
5/min/node
```

实际可能变成：

```text
5 × N
```

### 建议

登录至少：

```text
IP
+
Username
+
IP+Username
```

多节点再使用 Redis。

---

# SEC-006 — DangerousClCommandValidator 是黑名单模型

### 文件

```text
DangerousClCommandValidator.java
```

目前核心思路：

```text
first command verb
→ blacklist
```

问题是：

```text
CALL
```

可以间接执行：

```text
QCMDEXC
```

因此：

```text
Blacklist
```

不能作为真正安全边界。

### 推荐

改成：

```text
Command Policy
```

即：

```text
Allowlist
+
structured command parser
+
parameter validation
```

尤其识别：

```text
CALL
QCMDEXC
RUNSQL
RUNSQLSTM
SBMJOB
```

### 优先级

**P1**

---

# SEC-007 — localStorage XOR 不是加密

### 文件

```text
frontend/src/composables/useStorage.ts
```

### Line

```text
21-36
```

```text
TOKEN_XOR_KEY = 0xa3
```

只是：

```text
obfuscation
```

不是：

```text
encryption
```

XSS 仍然可以：

```text
localStorage
→ token
```

### 推荐

理想架构：

```text
Access Token
    ↓
Memory

Refresh Token
    ↓
HttpOnly + Secure + SameSite Cookie
```

如果暂时不改：

```text
短 access token
+
严格 CSP
+
XSS hardening
```

---

# 6. System / RBAC Findings

---

# SYS-001 — Role Update 非原子

### 文件

```text
RoleService.java
```

### Line

```text
108-130
```

流程：

```text
update role
delete role_menu
insert role_menu
```

如果 insert 失败：

```text
Role exists
but all menu permissions lost
```

虽然代码已经先执行：

```text
validatedMenuIds()
```

这是正确优化。

但是：

> Validation 不能替代 Transaction。

---

# SYS-002 — Role Delete 非原子

### Line

```text
134-145
```

```text
delete role_menu
delete user_role
delete role
```

应使用本地 transaction。

---

# SYS-003 — User role response 有逻辑缺陷

### 文件

```text
SysUserServiceImpl.java
```

### Line

```text
116-142
```

最后：

```java
rolesByIds(
    resolveRoleIds(dto.getRoleIds(), dto.getRoleCodes())
)
```

如果 update 请求：

```text
roleIds = null
roleCodes = null
```

意味着：

> 不修改角色

但 `resolveRoleIds()` 返回：

```text
empty
```

所以返回的 VO 可能：

```text
roles = []
```

而 DB 中角色实际上没有变化。

### 这是 API Response correctness 问题。

---

# SYS-004 — Permission Request 可以动态创建任意 permission code

### 文件

```text
PermissionRequestService.java
```

### Line

```text
252-259
```

如果 permission 不存在：

```text
new SysPermission()
permissionCode = requestedCode
insert
```

意味着：

```text
用户/申请人输入
        ↓
permission registry
```

### 推荐

申请只能从：

```text
existing rx_permission
```

选择。

不存在：

```text
400 BAD_REQUEST
```

不能动态创建。

---

# SYS-005 — Permission Request Check-Then-Insert

### Line

```text
75-90
```

：

```text
SELECT pending
↓
INSERT
```

并发：

```text
A SELECT 0
B SELECT 0
A INSERT
B INSERT
```

产生两个 pending request。

需要 DB uniqueness / CAS / lock。

---

# SYS-006 — UserMenuService 同样存在 TOCTOU

### 文件

```text
UserMenuService.java
```

### Line

```text
108-137
```

：

```text
select existing
↓
insert missing
```

并发可能：

```text
duplicate key
```

建议：

```text
UNIQUE(user_id, menu_id)
```

并使用：

```text
INSERT IGNORE
```

或者：

```text
ON DUPLICATE KEY UPDATE
```

---

# SYS-007 — Dictionary 删除类型不是原子

### 文件

```text
DictService.java
```

### Line

```text
74-81
```

：

```text
delete dict items
delete dict type
```

应使用 transaction。

---

# SYS-008 — Audit Log 同步写 DB

### 文件

```text
OperateLogAspect.java
```

业务请求：

```text
Business
 ↓
Audit INSERT
 ↓
Response
```

高并发会增加 DB latency。

但当前：

```text
audit failure
!=
business failure
```

这一点是正确的。

推荐：

```text
Business
 ↓
ApplicationEvent
 ↓
Async Audit Writer
 ↓
DB
```

并增加：

```text
queue depth
failure count
drop count
```

---

# SYS-009 — Notification 广播可能形成瞬时放大

### 文件

```text
NotificationService.java
```

### Line

```text
57-77
```

当前：

```text
SELECT all active users
↓
insert batch
↓
forEach
↓
WebSocket broadcast
```

10,000 用户：

```text
10,000 DB-related/user notification operations
+
10,000 WebSocket messages
```

建议异步 worker。

---

# 7. AS400 Infrastructure Findings

---

# AS400-001 — AS400Client Interface 拆分设计优秀

### 文件

```text
AS400Client.java
```

当前已经拆成：

```text
CommandClient
SqlClient
AuthClient
SourceClient
ObjectClient
IfsClient
JobClient
SubsystemClient
SysvalClient
MessageFileClient
PfClient
DataAreaClient
```

这是非常合理的。

应该保留。

---

# AS400-002 — Multi Server Provider 设计优秀

### 文件

```text
AS400ClientProviderImpl.java
```

支持：

```text
forServer(serverId)
current()
evict(serverId)
```

已经形成：

```text
                    Provider
                       │
           ┌───────────┼───────────┐
           ↓           ↓           ↓
       IBM i A      IBM i B     IBM i C
```

这是后续 Monitor Collector / Deployment Center 的正确基础。

---

# AS400-003 — `current()` 自动回退 default server

### 文件

```text
AS400ClientProviderImpl.java
```

### Line

```text
48-56
```

当前：

```text
server context exists
    ↓
forServer()

no context
    ↓
defaultClient()
```

### 最大风险

后台任务：

```text
Server B
↓
forgot serverId
↓
current()
↓
Server A
```

程序完全正常。

但是：

> 操作了错误服务器。

这是典型的 silent misrouting。

### 建议

后台任务禁止：

```text
current()
```

必须：

```text
forServer(serverId)
```

甚至可以让：

```text
current()
```

只允许 Web Request。

### P1

---

# AS400-004 — Connection Pool 参数硬编码

### 文件

```text
JTOpenConnectionState.java
```

例如：

```text
POOL_MAX_SIZE = 8
POOL_MIN_IDLE = 2
```

建议配置化：

```yaml
rxas400:
  ibmi:
    pool:
      max-size: 8
      min-idle: 2
      connection-timeout: 30s
```

未来可做到：

```text
server-specific pool
```

---

# AS400-005 — JT400 Connection Lifecycle 优秀

### 文件

```text
JTOpenConnectionState.java
```

优点：

- lazy connection
- connection lock
- pool lock
- DCL
- invalidate
- disconnect
- Hikari
- timeout
- password redaction
- connection test

这是当前项目基础设施中质量比较高的一块。

---

# AS400-006 — Job 参数校验总体较好

### 文件

```text
JobService.java
```

### Line

```text
223-236
```

已经使用：

```text
IDENTIFIER
JOB_NUMBER
```

并且：

```text
1-6 digit
```

这是正确方向。

---

# AS400-007 — Job Detail 参数校验顺序需要修正

### 文件

```text
JobService.java
```

### Method

```text
jobDetail()
```

### Line

```text
96-108
```

先：

```java
jobName.trim()
```

再统一 `requireJob()`。

如果：

```text
jobName = null
```

可能出现：

```text
NullPointerException
```

建议所有 Job API：

```text
validate first
↓
query
```

统一入口。

---

# AS400-008 — MSGW 查询失败被伪装成仿真数据

### 文件

```text
JobService.java
```

### Line

```text
162-186
```

当前：

```text
真实查询失败
↓
CPF0000
↓
“仿真”
```

这在 Mock 模式有意义。

但生产环境：

```text
IBM i SQL failed
```

最终 UI 可能看到：

```text
CPF0000
作业等待消息（仿真）
```

这会把：

```text
系统故障
```

伪装成：

```text
业务数据
```

### 建议

Mock fallback 必须明确：

```text
profile = mock
```

生产：

```text
ERROR
```

不能自动 fake。

---

# AS400-009 — SPOOL CL command 参数拼接风险

### 文件

```text
JTOpenJobClient.java
```

例如：

```text
93-120
138-151
```

存在：

```text
String.format(...)
CommandCall.run(...)
```

构造：

```text
DSPSPLF
DLTSPLF
```

虽然上层有部分校验，但 infrastructure 本身仍然依赖调用方安全。

建议：

```text
validateJobName
validateJobUser
validateJobNumber
validateSpoolName
validateOutputQueue
```

全部集中到一个：

```text
As400JobIdentifierValidator
```

---

# AS400-010 — SWITCHUSR 参数没有共享 identifier validator

### 文件

```text
JTOpenAuthClient.java
```

### Line

```text
80-87
```

当前：

```java
"SWITCHUSR USER(" + targetUser + ")"
```

没有：

```text
requireIdentifier()
```

而其它 Client 已经在使用。

### P1

---

# AS400-011 — DataArea Library 未统一 identifier validation

### 文件

```text
JTOpenDataAreaClient.java
```

例如：

```text
30
51
73
81
91
```

Library：

```java
library == null ? "QSYS" : library.trim().toUpperCase()
```

但没有统一：

```text
requireIdentifier(library)
```

而 name 已校验。

应统一：

```text
lib = requireIdentifier(library)
name = requireIdentifier(name)
```

---

# AS400-012 — DataArea value 拼 CL command

### 文件

```text
JTOpenDataAreaClient.java
```

### Line

```text
72-86
```

目前使用：

```java
replace("'", "''")
```

这是必要防护。

但更推荐 infrastructure 层提供：

```text
ClCommandBuilder
```

避免每个 Client 自己实现字符串 escaping。

---

# AS400-013 — SystemValue 修改值直接进入 CL

### 文件

```text
JTOpenSysvalClient.java
```

### Line

```text
33-40
```

同样依赖：

```text
replace("'", "''")
```

建议统一：

```text
ClStringEscaper
```

---

# AS400-014 — UserProfile STATUS 没有白名单

### 文件

```text
UserProfileServiceImpl.java
```

### Line

```text
191-193
```

：

```java
cmd.append(" STATUS(")
   .append(dto.getStatus().toUpperCase())
```

没有：

```text
enum/allowlist
```

这属于 CL 参数安全边界。

建议只允许：

```text
*ENABLED
*DISABLED
```

等系统明确支持值。

---

# AS400-015 — UserProfile Special Authority 是高危能力

### 文件

```text
UserProfileServiceImpl.java
```

### Line

```text
166-173
200-206
```

支持：

```text
*ALLOBJ
*SAVRST
...
```

虽然有 identifier 校验，但：

> identifier 合法 ≠ authorization 合法。

如果 `USER_PROFILE_MANAGE` 用户可以提交：

```text
*ALLOBJ
```

就是直接提升 IBM i 用户权限。

必须增加业务级 allowlist / approval。

例如：

```text
普通管理员:
只能 USER / PASSWORD / STATUS

安全管理员:
才能 SPCAUT

QSECOFR-level:
禁止 Web UI 直接操作
```

这是 AS400 ADM 项目最应该特别处理的一层。

---

# AS400-016 — SourceClient SQL 错误吞掉

### 文件

```text
JTOpenSourceClient.java
```

### Line

```text
89-94
112-115
135-137
```

当前：

```text
SQLException
↓
return ""
```

或者：

```text
return List.of()
```

导致：

```text
SQL ERROR
```

看起来像：

```text
No Source File
No Member
No Content
```

### 建议

统一使用：

```text
AS400_SOURCE_QUERY_FAILED
```

让 Controller 决定：

```text
error
```

而不是：

```text
empty data
```

---

# AS400-017 — IFS Path Sandbox 设计总体优秀

### 文件

```text
IfsController.java
```

### Line

```text
213-245
```

已经处理：

```text
absolute path
\
..
hidden segment
allowed root
trailing slash
```

这是非常值得肯定的安全设计。

---

# AS400-018 — IFS Sandbox 仍需要考虑 symlink escape

当前是：

```text
lexical path validation
```

但如果 IFS 中存在：

```text
/QOpenSys/rxas400/link
    ↓
/QOpenSys/usr/bin
```

那么：

```text
/QOpenSys/rxas400/link/file
```

从字符串看合法。

实际可能已经跳出 root。

### 建议

高安全模式增加：

```text
real/canonical path verification
```

或者：

```text
禁止访问 symlink
```

---

# AS400-019 — IFS 整读上限设计正确

### 文件

```text
JTOpenIfsClient.java
```

### Line

```text
30
197-203
```

：

```text
20MB
```

超过：

```text
BusinessException
```

然后要求：

```text
stream download
```

这是正确的内存保护。

---

# AS400-020 — IFS stream 下载设计正确

### 文件

```text
IfsController.java
```

### Line

```text
127-148
```

使用：

```text
StreamingResponseBody
```

避免：

```text
byte[] 100MB
```

一次性进入 Controller 内存。

很好。

---

# AS400-021 — SPOOL 临时文件应该使用 UUID

### 文件

```text
JTOpenJobClient.java
```

当前：

```text
/tmp/spool_<spoolName>_<timestamp>.txt
```

建议：

```text
/tmp/rxas400/spool/<UUID>.txt
```

业务输入不应该参与 filesystem identifier。

---

# AS400-022 — SPOOL 临时文件清理应该 finally

异常情况下：

```text
DSPSPLF
↓
read failed
↓
return
```

临时文件可能残留。

必须：

```java
try {
   ...
} finally {
   cleanup();
}
```

---

# AS400-023 — JobService MSGW 并行线程池需要生命周期管理

### 文件

```text
JobService.java
```

### Line

```text
51-65
```

当前：

```java
static final ExecutorService
```

并且：

```text
daemon thread
```

### 问题

没有：

```text
@PreDestroy
```

也没有 Spring `TaskExecutor` 管理。

建议：

```text
ThreadPoolTaskExecutor
```

由 Spring lifecycle 管理。

---

# 8. Scheduling Findings

---

# SCH-001 — JobSchedule DB 与 Quartz 状态不是原子

### 文件

```text
JobScheduleService.java
```

### create

```text
67-79
```

流程：

```text
DB insert
↓
Quartz register
```

如果：

```text
DB insert success
Quartz register failed
```

当前会：

```text
disableAfterRegisterFailure()
```

这是不错的补偿设计。

但是：

```text
DB
+
Quartz
```

本质仍不是 transaction。

---

# SCH-002 — Update 存在 DB / Quartz 状态窗口

### Line

```text
83-96
```

：

```text
DB update
↓
Quartz register
```

中间崩溃可能：

```text
DB cron = B
Quartz cron = A
```

启动恢复可以修复，但运行期间存在不一致。

建议引入：

```text
scheduler_version
```

或：

```text
reconcile job
```

---

# SCH-003 — 删除策略设计较好

### Line

```text
100-110
```

先：

```text
enabled=false
```

再：

```text
unregister
delete history
delete schedule
```

这比：

```text
delete DB first
```

安全很多。

值得保留。

---

# SCH-004 — SQL Scheduled Job 使用只读 validator

### Line

```text
147-160
```

SQL：

```text
SELECT/WITH
```

才允许执行。

这是正确设计。

---

# SCH-005 — Schedule CL 只使用 blacklist 仍然不足

保存时：

```text
S3 validator
```

执行时：

```text
S4 validator
```

虽然有双层防御：

```text
save
+
execute
```

但是 validator 本身还是 blacklist。

建议最终升级成：

```text
Command Policy Engine
```

---

# 9. Document / IFS Findings

---

# DOC-001 — Document publish 状态与 IFS 存储存在最终一致性

### 文件

```text
DocService.java
```

### Line

```text
180-196
```

流程：

```text
DB status = PUBLISHED
↓
IFS write
```

如果：

```text
DB success
IFS fail
```

文档：

```text
PUBLISHED
```

但文件：

```text
不存在
```

当前已有：

```text
resweepUnpublishedIfsDocs()
```

这是一个非常好的补偿机制。

但命名：

```text
unpublishedIfsDocs
```

实际查询的是：

```text
PUBLISHED + IFS_PATH null
```

建议增加：

```text
IFS_SYNC_STATUS
```

而不是依赖：

```text
ifsPath null
```

---

# DOC-002 — 文档删除先 IFS trash 再 DB

### Line

```text
210-223
```

当前：

```text
IFS trash
↓
DB delete
```

如果 IFS 成功、DB 失败：

```text
document still visible
but file in trash
```

应该记录：

```text
SYNC_PENDING
```

或者：

```text
outbox
```

---

# DOC-003 — purge 需要 transaction

### Line

```text
239-246
```

：

```text
delete doc
delete versions
```

应该一个 transaction。

---

# 10. BPCS Findings

BPCS 部分总体 SQL 参数化质量比较高。

尤其大量代码已经统一使用：

```text
queryListCheckedBounded()
```

这是正确方向。

但仍存在以下问题。

---

# BPCS-001 — BpcsSupplyChainServiceImpl 过大

### 文件

```text
BpcsSupplyChainServiceImpl.java
```

约：

```text
563 lines
31KB
```

同时承担：

```text
Order
Inventory
Supplier
Purchase
Shipping
Analytics
KPI
History
Alert
Mapping
Mock
```

违反 SRP。

建议拆：

```text
BpcsOrderService
BpcsInventoryService
BpcsSupplierService
BpcsPurchaseService
BpcsShippingService
BpcsAnalyticsService
```

再提供：

```text
BpcsSupplyChainFacade
```

---

# BPCS-002 — 多处 `queryListChecked()` 无显式 bounded

例如：

```text
BpcsInventoryAnalyticsServiceImpl
BpcsOrderDetailServiceImpl
BpcsOrderAnalyticsServiceImpl
```

需要逐 SQL 评估返回规模。

原则：

```text
detail query
    ↓
bounded

dashboard
    ↓
aggregate SQL

export
    ↓
stream/paging
```

不要：

```text
IBM i → unlimited rows → Java heap
```

---

# BPCS-003 — RCMX Update 存在业务并发窗口

### 文件

```text
BpcsRcmxServiceImpl.java
```

### Line

```text
116-139
```

：

```text
checkUnique
↓
deactivate
↓
update
```

两个请求并发可能：

```text
A check
B check

A update
B update
```

最终唯一性取决于 DB constraint。

必须增加：

```text
unique constraint
```

而不是仅靠业务 SELECT。

---

# BPCS-004 — WABP import 是逐行 update/insert

### 文件

```text
BpcsWabpServiceImpl.java
```

### Line

```text
170-198
```

：

```text
for each row
    select
    update/insert
```

大量 Excel：

```text
1000 rows
```

可能：

```text
2000+ DB/IBM i operations
```

建议：

```text
batch read existing
+
batch upsert
```

---

# BPCS-005 — RMA status 没有状态机

### 文件

```text
RmaServiceImpl.java
```

### Line

```text
61-68
```

任何 status：

```text
r.setStatus(status)
```

都可以写。

应该：

```text
PENDING
→ APPROVED
→ PROCESSING
→ COMPLETED
→ CANCELLED
```

并限制合法 transition。

---

# BPCS-006 — RMA No 使用 timestamp 生成

### Line

```text
47
```

：

```java
"RMA-" + System.currentTimeMillis() % 1000000
```

并发可能 collision。

必须：

```text
DB unique
+
UUID / sequence / Snowflake
```

---

# 11. API / Backend Architecture

---

# API-001 — Controller Layering 是项目优点

静态门禁：

```text
check-layering.sh
```

确认：

```text
Controller
X Mapper
X QueryWrapper
X Entity Request
```

这是非常值得保留的架构约束。

---

# API-002 — 部分 Controller 太大

例如：

```text
JobController.java
DocController.java
```

虽然没有违反 layering，但已经出现：

```text
Controller
→ many endpoints
→ many parameter conversions
→ response formatting
→ streaming
```

建议后期按 domain 拆：

```text
JobQueryController
JobCommandController
SpoolController
JobMessageController
```

---

# API-003 — Command API 使用 RequestParam

### 文件

```text
As400Controller.java
```

### Line

```text
88-91
```

：

```text
@RequestParam String command
```

对于危险执行 API，更推荐：

```json
{
  "serverId": 1,
  "command": "..."
}
```

并记录：

```text
operator
serverId
command hash
policy result
execution result
```

---

# API-004 — ExecutionService 聚合模型清晰

```text
SCHEDULE
SCRIPT
```

统一：

```text
ExecutionRecordVO
```

是好的。

但：

```text
script
```

只保存：

```text
last execution
```

而 schedule 保存：

```text
history
```

两个执行历史模型不一致。

建议最终统一：

```text
rx_execution_record
```

或者至少：

```text
CommandScriptHistory
```

---

# 12. Database / Transaction Review

---

# DB-001 — 0 Transaction Rule 是当前最大架构问题之一

文件：

```text
scripts/check-transactional.sh
```

当前强制：

```text
0 @Transactional
```

对于：

```text
User
Role
Permission
Menu
Dictionary
Document
Schedule
```

这些多表业务是不合理的。

### 正确规则

应该：

```text
Local MySQL multi-step mutation
        ↓
@Transactional
```

但是：

```text
MySQL + IBM i
```

不要 XA。

跨系统：

```text
Transaction
↓
Outbox
↓
Worker
↓
IBM i
↓
Retry / Compensation
```

---

# DB-002 — Check-then-insert 大量存在

发现于：

```text
User
Role
PermissionRequest
UserMenu
Dictionary
Region
Webhook
Rcmx
```

统一原则：

```text
Application validation
+
DB UNIQUE
```

而不是：

```text
SELECT
↓
if not exists
↓
INSERT
```

---

# DB-003 — DB Unique Constraint 应成为最后一道业务防线

重点检查：

```text
username
role_code
permission_code
webhook_name
rma_no
user_role
role_menu
permission_request
i18n(lang,key)
```

---

# DB-004 — Flyway Migration 管理优秀

项目已经到：

```text
V91
```

并且：

```text
migration consistency check
fresh DB verification
```

这是非常好的工程实践。

但后期需要：

```text
baseline/release strategy
```

否则 V1~V100+ 长期累积会增加部署复杂度。

---

# 13. Configuration Findings

---

# CFG-001 — 默认 profile = mock

### 文件

```text
application.yml
```

：

```yaml
spring:
  profiles:
    active: mock
```

这对 Demo 很方便。

但生产忘记指定：

```text
prod
```

可能启动到：

```text
MOCK
```

### 建议

生产构建明确：

```text
SPRING_PROFILES_ACTIVE=prod
```

或者：

```text
base profile 不指定 active
```

由启动环境决定。

---

# CFG-002 — base config 使用 root/root

### 文件

```text
application.yml
```

：

```yaml
username: ${MYSQL_USERNAME:root}
password: ${MYSQL_PASSWORD:root}
```

生产 profile 已经改进：

```text
RXAS400_DB_PASSWORD
```

但 base config 仍然存在危险默认值。

建议：

```text
application-local.yml
```

保存：

```text
root/root
```

base：

```text
必须提供
```

---

# CFG-003 — Production DB 使用 useSSL=false

### 文件

```text
application-prod.yml
```

当前 JDBC：

```text
useSSL=false
```

如果 MySQL 在远程服务器：

```text
DB credentials
+
data
```

可能明文传输。

建议生产：

```text
useSSL=true
verifyServerCertificate=true
```

或明确使用 TLS 配置。

---

# CFG-004 — Quartz initialize-schema=always

### 文件

```text
application.yml
```

：

```yaml
spring.quartz.jdbc.initialize-schema: always
```

生产环境不应该依赖应用启动自动初始化 Quartz schema。

建议：

```text
Flyway 管理 Quartz schema
```

然后：

```yaml
initialize-schema: never
```

---

# CFG-005 — Flyway base validate=false

当前 base：

```yaml
validate-on-migrate: false
```

prod：

```yaml
validate-on-migrate: true
```

prod 是正确的。

建议所有正式部署都：

```text
validate-on-migrate=true
```

---

# 14. Error Handling Findings

---

# ERR-001 — Infrastructure 层大量 fallback empty

例如：

```text
JTOpenSourceClient
JTOpenIfsClient
JTOpenObjectClient
JTOpenJobClient
JTOpenAuthClient
```

存在：

```text
SQLException
↓
return empty
```

问题：

```text
error
```

被转换成：

```text
empty data
```

应该统一三种语义：

```text
NOT_FOUND
SUCCESS_EMPTY
SYSTEM_ERROR
```

不能都使用：

```text
null
List.of()
""
false
```

---

# ERR-002 — CommandResult fail 与 Exception 双轨

当前：

```text
client.execute()
```

可能：

```text
CommandResult.fail()
```

也可能：

```text
Exception
```

Service 层需要统一。

推荐：

```text
AS400OperationResult
```

包括：

```text
success
errorCode
message
retryable
serverId
duration
```

---

# 15. Logging Findings

---

# LOG-001 — AS400 command logging 过于敏感

例如：

```text
IbmiSystemService.java
167
```

：

```java
log.info("... command={}", command);
```

以及：

```text
UserProfileServiceImpl
79
107
131
```

可能记录：

```text
CRTUSRPRF ... PASSWORD(...)
CHGUSRPRF ... PASSWORD(...)
```

这是非常严重的日志泄露风险。

### 必须整改。

尤其：

```text
PASSWORD(...)
```

必须 mask。

例如：

```text
CRTUSRPRF USRPRF(TEST) PASSWORD(******)
```

而不是：

```text
PASSWORD(P@ssw0rd)
```

### P1

---

# LOG-002 — Webhook URL 出现在日志

### 文件

```text
WebhookNotifier.java
```

当前日志包含：

```text
url={}
```

Webhook URL 本身可能包含：

```text
token
secret
query credential
```

建议：

```text
mask URL query
```

---

# 16. Frontend Review

---

# FE-001 — API Request Layer 设计优秀

### 文件

```text
frontend/src/api/request.ts
```

集中处理：

```text
Authorization
Accept-Language
X-AS400-Server
401
i18n error
request dedupe
```

这是前端基础设施中值得保留的部分。

---

# FE-002 — Request dedupe 是很好的设计

当前：

```text
Map<string, AbortController>
```

对：

```text
GET
HEAD
```

进行请求去重。

并且存在：

```text
noDedupe
```

用于：

```text
polling
shared URL
```

这是比较成熟的处理。

---

# FE-003 — Refresh Token 没有跨 Tab single-flight

### 文件

```text
useTokenRefresh.ts
```

以及：

```text
stores/user.ts
```

当前只在：

```text
单个 JS context
```

内避免问题。

两个浏览器 Tab：

```text
Tab A refresh R1
Tab B refresh R1
```

会同时请求。

这与后端：

```text
SEC-001
```

形成组合风险。

### 推荐：

```text
BroadcastChannel
+
cross-tab lock
+
single-flight refresh
```

---

# FE-004 — loadMenus 失败后可能保留旧 permissions

### 文件

```text
stores/user.ts
```

### Line

```text
121-130
```

异常时：

```java
this.menus = []
```

但没有：

```text
this.permissions = []
this.tabs = []
```

如果用户权限已变化：

```text
old permissions
```

可能继续留在前端状态。

虽然后端仍然负责最终授权，但 UI 状态可能错误。

建议失败：

```text
menus=[]
permissions=[]
tabs=[]
```

---

# FE-005 — 大型 Vue SFC

重点：

```text
views/job/index.vue
views/docs/index.vue
views/bpcs/controlTower/index.vue
views/bpcs/freightCost/index.vue
views/as400/userProfiles/index.vue
views/report/index.vue
views/report/ReportBuilder.vue
views/assets/index.vue
views/system/roles/index.vue
views/system/Users.vue
```

建议拆成：

```text
Toolbar
SearchForm
Table
DetailDrawer
ActionDialog
History
```

并配：

```text
useJobQuery
useJobActions
useDocument
useReportBuilder
```

---

# FE-006 — ReportBuilder 有 any

### 文件

```text
frontend/src/views/report/ReportBuilder.vue
```

### Line

```text
291
```

：

```ts
deleteDef(row: any)
```

应使用：

```ts
ReportDefinition
```

这是 TypeScript 可以直接修掉的问题。

---

# FE-007 — wangEditor 类型仍为 any

### 文件

```text
frontend/src/wangeditor.d.ts
```

：

```ts
Editor: any
Toolbar: any
```

建议补充真实类型声明。

---

# FE-008 — v-html 使用正确消毒，但仍应建立统一 Renderer

### 文件

```text
DocRenderer.vue
```

当前：

```text
marked
↓
DOMPurify
↓
v-html
```

这是正确做法。

建议统一：

```text
DocumentSanitizer
```

并严格：

```text
HTML
Markdown
TEXT
```

三种模式。

---

# FE-009 — iframe PDF preview 应增加 sandbox 策略

当前：

```vue
<iframe :src="fileUrl">
```

建议考虑：

```html
sandbox
```

尤其文档来自用户可编辑内容时。

---

# 17. Frontend Router

---

# ROUTER-001 — 前端权限只是 UX 控制

当前：

```text
router.beforeEach
↓
menuPaths
↓
authorized
```

这是正确的。

但必须明确：

> 前端 route guard 绝不能成为真正安全边界。

当前后端已经：

```text
@PreAuthorize
```

所以设计是正确的。

---

# ROUTER-002 — menuPaths fallback 策略比较合理

如果：

```text
/dashboard
```

也没有权限：

```text
不要死循环跳 dashboard
```

当前改为：

```text
first authorized menu
```

这是一次很好的边界处理。

---

# 18. Mock Architecture

---

# MOCK-001 — Mock Client Architecture 优秀

存在：

```text
MockAS400Client
MockAuthClient
MockCommandClient
MockJobClient
MockSourceClient
MockSqlClient
MockIfsClient
...
```

说明：

```text
Service
```

没有直接依赖：

```text
JT400
```

而是依赖：

```text
AS400Client
```

这极大提高了：

```text
testability
local development
demo
```

---

# MOCK-002 — Mock fallback 不应该出现在 production error path

这是当前：

```text
JobService
SourceClient
```

需要进一步收敛的地方。

规则：

```text
profile=mock
→ mock

profile=prod
→ error
```

不要：

```text
prod error
→ fake data
```

---

# 19. Performance Findings

---

# PERF-001 — ReportBuilder 10,000 rows × multiple data source

### 文件

```text
ReportBuilderService.java
```

多个数据源：

```text
setSize(10000)
```

然后：

```text
List
→ Map
→ report
```

并发时：

```text
10k × N datasets × N users
```

容易形成：

```text
Heap pressure
GC
IBM i load
```

### 推荐

```text
paged datasource
+
streaming Excel
+
query aggregation
+
execution concurrency limit
```

---

# PERF-002 — CommandScript tags 全表扫描

### 文件

```text
CommandScriptService.java
```

### Line

```text
64-75
```

：

```text
selectList(null)
```

然后 Java：

```text
split(",")
distinct
```

60 秒缓存可以缓解。

但脚本数量扩大后仍不理想。

可以增加：

```text
normalized tag table
```

或者 DB JSON/tag structure。

---

# PERF-003 — SystemHealth 全量 systems 查询

### 文件

```text
SystemHealthServiceImpl.java
```

：

```text
systemMapper.selectList(null)
```

当前服务器数量应该不会很大，所以不是 P1。

但未来：

```text
1000 IBM i systems
```

时需要分页/批量采集。

---

# PERF-004 — Notification broadcast 应异步

已经在 SYS-009 说明。

建议：

```text
Alert
↓
Event
↓
Queue
↓
WebSocket Worker
```

---

# 20. Code Quality

---

# CQ-001 — Double-brace initialization

### 文件

```text
ReportBuilderService.java
```

出现类似：

```java
new BpcsOrderListQueryDTO() {{
    setCono(cono);
}}
```

这是匿名 inner class。

不建议。

改：

```java
BpcsOrderListQueryDTO dto = new BpcsOrderListQueryDTO();
dto.setCono(cono);
```

或者：

```text
factory/builder
```

---

# CQ-002 — 巨型 Service

重点：

```text
BpcsSupplyChainServiceImpl
DocService
JobScheduleService
JobService
BpcsOrderServiceImpl
BpcsForecastServiceImpl
```

建议按 domain/application/infrastructure 分层。

---

# CQ-003 — 巨型 Controller

重点：

```text
JobController
DocController
BpcsSupplyChainController
```

应该拆 command/query。

---

# CQ-004 — Entity 直接作为部分 API Response

虽然静态 layering gate 已经禁止明显违规，但仍有部分 Service/Controller：

```text
List<Entity>
```

建议最终统一：

```text
Entity
↓
VO
↓
API
```

防止：

```text
DB schema
```

泄露到 API contract。

---

# 21. Architecture Target

建议 RXAS400ADM 最终演进成：

```text
                           Vue3
                            │
                    ┌───────┴────────┐
                    │ API Gateway/UI │
                    └───────┬────────┘
                            │
                       REST API
                            │
                  ┌─────────┴─────────┐
                  │ Application Layer │
                  └─────────┬─────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
      Security            System              AS400
        │                   │                   │
       JWT                 RBAC              Domain API
       Auth                User              │
       Audit               Role        ┌──────┼──────┐
       RateLimit            Menu        │      │      │
                           Permission  JT400  Mock   Policy
                                       │
                              ┌────────┼─────────┐
                              │        │         │
                             SQL      CL        IFS
                              │        │         │
                              └────────┬─────────┘
                                       │
                                   IBM i
```

---

# 22. Recommended AS400 Execution Architecture

这是整个项目下一阶段最值得做的架构升级。

当前：

```text
Service
   ↓
client.execute(command)
```

建议：

```text
Service
   ↓
As400OperationService
   ↓
CommandPolicy
   ↓
CommandValidator
   ↓
ServerRouter
   ↓
AS400Client
   ↓
JT400
```

统一接口：

```java
execute(
    serverId,
    operationType,
    command,
    operator
)
```

记录：

```text
serverId
operator
operationType
commandHash
commandMasked
startTime
endTime
success
errorCode
retryable
```

这样：

```text
Job
Script
Template
UserProfile
Subsystem
SystemValue
DataArea
Deployment
```

都不再自己处理安全逻辑。

---

# 23. Recommended Transaction Architecture

最终不要继续坚持：

```text
0 Transaction
```

应该变成：

```text
                    Business Operation
                           │
                    ┌──────┴──────┐
                    │             │
                MySQL State    IBM i Action
                    │             │
                Transaction     Async
                    │             │
                  COMMIT       Retry
                    │             │
                  Outbox        Compensation
                    │             │
                    └──────┬──────┘
                           ↓
                         Event
```

例如：

```text
Create User
```

应该：

```text
@Transactional
    insert user
    insert user_role
    insert audit/outbox
COMMIT

Outbox Worker
    ↓
IBM i
```

---

# 24. Recommended RBAC Architecture

当前：

```text
User
 ↓
Role
 ↓
Permission
```

已经正确。

建议进一步：

```text
User
 ├── Role
 │    └── Permission
 │
 └── Direct Permission
```

并且：

```text
Permission
```

必须是：

```text
Registry
```

而不是用户输入动态创建。

同时定义：

```text
Permission Type
----------------
READ
WRITE
EXECUTE
ADMIN
DANGEROUS
```

特别：

```text
AS400_COMMAND_EXECUTE
AS400_USER_ADMIN
AS400_SYSTEM_ADMIN
AS400_SECURITY_ADMIN
```

必须拆开。

---

# 25. Recommended IBM i Permission Model

当前：

```text
AS400_MANAGE
```

过于宽泛。

建议：

```text
AS400_VIEW
AS400_SERVER_MANAGE

AS400_COMMAND_VIEW
AS400_COMMAND_EXECUTE

AS400_JOB_VIEW
AS400_JOB_CONTROL

AS400_USER_VIEW
AS400_USER_CREATE
AS400_USER_UPDATE
AS400_USER_DELETE
AS400_USER_SPECIAL_AUTH

AS400_IFS_VIEW
AS400_IFS_WRITE
AS400_IFS_DELETE

AS400_SYSVAL_VIEW
AS400_SYSVAL_EDIT

AS400_DATAAREA_VIEW
AS400_DATAAREA_EDIT

AS400_SUBSYSTEM_VIEW
AS400_SUBSYSTEM_CONTROL
```

这样可以避免：

```text
拥有一个 AS400_MANAGE
=
几乎拥有整个 IBM i 管理权限
```

---

# 26. P0 修复顺序

第一阶段只做：

```text
1. Refresh token atomic consume
2. Permission approval transaction + CAS
3. AS400 user sync remove 5000 limit
4. User CRUD transaction
5. AS400 role assignment transaction
```

---

# 27. P1 修复顺序

第二阶段：

```text
6. Command Policy Engine
7. SWITCHUSR identifier validation
8. SPOOL parameter validation
9. UserProfile STATUS allowlist
10. UserProfile *ALLOBJ / SPCAUT security policy
11. Source SQL error semantics
12. current() silent default server fallback
13. IFS symlink escape
14. command/password logging masking
15. RMA status state machine
16. RMA number uniqueness
17. Schedule consistency/reconciliation
18. Production config hardening
```

---

# 28. P2 重构顺序

第三阶段：

```text
19. Redis distributed permission cache
20. cross-tab refresh lock
21. Async Audit
22. Async Notification
23. Async Webhook
24. AS400 operation metrics
25. Connection pool configuration
26. Report streaming
27. CommandScript tag normalization
28. BPCS batch import
29. large Service split
30. large Vue component split
```

---

# 29. Recommended Refactoring Priority

## Phase A — Security / Correctness

```text
AuthService
TokenBlacklistService
PermissionRequestService
SysUserServiceImpl
As400LoginSyncService
UserProfileServiceImpl
DangerousClCommandValidator
```

---

## Phase B — AS400 Execution

```text
AS400ClientProvider
JTOpenCommandClient
JTOpenJobClient
JTOpenAuthClient
JTOpenIfsClient
JTOpenDataAreaClient
JTOpenSysvalClient
JTOpenSubsystemClient
```

统一：

```text
validation
routing
exception
audit
metrics
```

---

## Phase C — System

```text
RoleService
UserMenuService
PermissionManageService
DictService
WebhookService
NotificationService
OperateLogAspect
```

---

## Phase D — Business

```text
BpcsSupplyChainServiceImpl
BpcsRcmxServiceImpl
BpcsWabpServiceImpl
BpcsForecastServiceImpl
BpcsOrderServiceImpl
```

---

## Phase E — Frontend

```text
request.ts
useTokenRefresh.ts
user.ts

job/index.vue
docs/index.vue
report/index.vue
ReportBuilder.vue
controlTower/index.vue
userProfiles/index.vue
```

---

# 30. Test Strategy Required

当前有：

```text
Backend test Java files ≈ 90
Frontend test files ≈ 13
```

数量不错。

但是下一阶段必须补：

## Security concurrency tests

```text
100 concurrent refresh(R1)
```

预期：

```text
1 success
99 fail
```

---

## Permission approval concurrency

```text
100 concurrent approve(requestId)
```

预期：

```text
1 APPROVED
99 rejected
```

---

## User role consistency

测试：

```text
insert user success
role insert fail
```

预期：

```text
whole transaction rollback
```

---

## IBM i routing

测试：

```text
server=A
server=B
missing server
async server
Quartz server
```

绝对不能出现：

```text
B → A
```

---

## IFS security

测试：

```text
../
..\
encoded traversal
symlink
hidden file
outside root
trash restore
```

---

## CL security

测试：

```text
CALL QSYS/QCMDEXC
RUNSQL
RUNSQLSTM
SBMJOB
DLTLIB
DLTF
DLTUSRPRF
```

---

# 31. Existing Quality Gates Evaluation

当前：

```text
check-layering.sh
```

优秀。

当前：

```text
check-frontend-slots
check-template-join
check-template-classes
check-i18n
check-migrations
```

也是非常好的工程实践。

但是：

```text
check-transactional.sh
```

需要重新设计。

不要：

```text
禁止 @Transactional
```

改成：

```text
禁止跨系统事务
允许本地事务
要求明确 transaction boundary
```

甚至可以做：

```text
check-transaction-boundary.sh
```

检查：

```text
Service multi-table write
```

是否：

```text
@Transactional
```

或者标记：

```text
@EventuallyConsistent
```

---

# 32. 项目中最值得保留的设计

以下设计不要因为重构而破坏。

## 1. AS400Client Domain Split

```text
CommandClient
SqlClient
JobClient
IfsClient
SourceClient
...
```

---

## 2. AS400ClientProvider

```text
forServer(serverId)
```

这是多 IBM i 架构的核心。

---

## 3. JT400 Connection State

连接生命周期、pool、lock 都比较成熟。

---

## 4. JWT iss/aud/jti/type

JWT 设计比普通 CRUD 项目成熟。

---

## 5. DB-backed blacklist

适合多节点。

---

## 6. Permission DB loading

避免把永久权限完全写死在 JWT。

---

## 7. SQL PreparedStatement

大量 BPCS / AS400 查询已经正确使用参数。

---

## 8. SQL ReadOnly Validator

已经考虑：

```text
QCMDEXC
IFS_WRITE
IFS_DELETE
```

说明开发者已经考虑到了 DB2 for i 特有攻击面。

---

## 9. IFS Root Sandbox

是项目安全设计的重要组成部分。

---

## 10. Frontend centralized request

是很好的 SPA 基础设施。

---

## 11. Static Quality Gates

这是整个项目工程质量的重要保障。

---

## 12. Flyway + migration checks

应该继续保持。

---

# 33. 最终整改 Backlog

建议直接创建如下 Jira/Epic：

```text
RXAS400ADM-CODE-REVIEW
```

下面建立：

```text
SEC-001 Refresh Token Atomic Rotation
SEC-002 Refresh Disabled User
SEC-003 Distributed Permission Cache

RBAC-001 Permission Approval Transaction
RBAC-002 Permission Request CAS
RBAC-003 Dynamic Permission Prevention
RBAC-004 Role Transaction

AS400-001 Server Routing Safety
AS400-002 Command Policy Engine
AS400-003 Job Command Validation
AS400-004 UserProfile Security
AS400-005 IFS Sandbox
AS400-006 Source Error Semantics
AS400-007 SPOOL Lifecycle

DB-001 Transaction Policy Redesign
DB-002 Unique Constraint Audit
DB-003 Concurrent Write Tests

SCH-001 Quartz/DB Reconciliation
SCH-002 Schedule Execution State

DOC-001 IFS Sync State
DOC-002 Document Transaction

FE-001 Cross-tab Refresh Lock
FE-002 Token Storage
FE-003 Large Component Refactor
FE-004 TypeScript any cleanup

PERF-001 Report Streaming
PERF-002 Notification Async
PERF-003 Webhook Async
PERF-004 BPCS Batch Operations

OBS-001 AS400 Operation Metrics
OBS-002 Command Audit
OBS-003 Error Classification
```

---

# 34. 最终结论

## 不建议重写

当前项目已经形成：

```text
Spring Boot
+
MyBatis Plus
+
JWT/RBAC
+
JT400
+
Quartz
+
Flyway
+
Vue3
+
TypeScript
```

整体技术路线正确。

---

## 但必须进行一次“正确性加固”

当前最危险的不是：

```text
代码风格
```

而是：

```text
Concurrency
Transaction
Cross-system consistency
AS400 command boundary
```

也就是：

```text
             当前系统
                 │
      ┌──────────┴──────────┐
      │                     │
     CRUD                 IBM i
      │                     │
      ↓                     ↓
multi-table             CL/SQL/IFS
      │                     │
      ↓                     ↓
transaction             security policy
      │                     │
      └──────────┬──────────┘
                 ↓
           correctness
```

---

# 35. 最重要的 10 个整改项

如果只能先做 10 个：

```text
1. Refresh Token Atomic Rotation
2. Permission Approval CAS + Transaction
3. Remove AS400 USER_INFO 5000 destructive limit
4. User CRUD Transaction
5. AS400 Role Assignment Transaction
6. Replace CL Blacklist with Command Policy
7. Prevent silent AS400 default-server routing
8. Mask PASSWORD/secret/command sensitive logs
9. Validate UserProfile special authorities
10. Unify AS400 infrastructure error semantics
```

完成这 10 项后，项目生产风险会明显下降。

---

# 36. 最终评分变化预测

当前：

```text
≈ 7.3 / 10
```

完成 P0：

```text
≈ 8.0
```

完成 P0 + P1：

```text
≈ 8.5
```

完成：

```text
Transaction
+
Command Policy
+
AS400 routing
+
observability
+
frontend modularization
```

可以达到：

```text
≈ 8.8 ~ 9.0
```

不需要重新设计整个项目。

---

# 37. 最终判断

### 架构：

**合理，可以继续发展。**

### Security：

**基础设施较强，但 AS400 高危操作边界需要进一步加强。**

### Backend：

**代码质量中上，但 Transaction Boundary 是最大的结构性问题。**

### AS400：

**这是项目最有价值、同时也是风险最大的模块。**

### Frontend：

**工程化基础不错，但页面级组件已经开始膨胀。**

### Database：

**Migration 管理优秀，但业务一致性仍然需要 transaction + constraint + CAS。**

### DevOps / Quality：

**静态质量门禁明显优于一般内部管理系统。**

---

# 38. 最终建议的实施路线

```text
                    RXAS400ADM
                         │
                         ▼
                ┌─────────────────┐
                │ Phase 1         │
                │ P0 Correctness  │
                └────────┬────────┘
                         │
             Token / RBAC / Sync
                         │
                         ▼
                ┌─────────────────┐
                │ Phase 2         │
                │ AS400 Security  │
                └────────┬────────┘
                         │
           Command / IFS / Job / User
                         │
                         ▼
                ┌─────────────────┐
                │ Phase 3         │
                │ Transaction     │
                └────────┬────────┘
                         │
             Local Tx / Outbox
                         │
                         ▼
                ┌─────────────────┐
                │ Phase 4         │
                │ Performance     │
                └────────┬────────┘
                         │
           Report / BPCS / Notification
                         │
                         ▼
                ┌─────────────────┐
                │ Phase 5         │
                │ Refactoring     │
                └────────┬────────┘
                         │
           Large Service / Vue SFC
                         │
                         ▼
                ┌─────────────────┐
                │ Production 9/10 │
                └─────────────────┘
```

**最终结论：RXAS400ADM 不需要推倒重建。最合理的方案是以现有架构为基础，优先修复 P0/P1 的一致性、安全边界和 IBM i 执行问题，然后再进行 Service / Vue 模块化重构。**