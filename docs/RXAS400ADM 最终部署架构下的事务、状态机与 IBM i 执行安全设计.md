# RXAS400ADM 最终部署架构下的事务、状态机与 IBM i 执行安全设计

## 1. 文档目的

本文基于当前 RXAS400ADM 项目源码进行分析，并结合最终部署目标：

- RXAS400ADM 最终直接部署到 IBM i / AS400
- 不再使用 MySQL
- 数据最终使用 IBM i 上的数据库能力
- IBM i 同时作为应用运行平台和被管理平台
- 项目决定不采用 Spring `@Transactional` 作为核心业务一致性机制

重新评估当前项目中：

1. 跨步骤业务操作的一致性问题
2. 状态机应该如何设计
3. IBM i 操作如何形成统一安全边界
4. 如何处理失败、重试、恢复和补偿
5. 如何避免数据库状态和 IBM i 实际状态不一致
6. 如何针对最终 IBM i 部署架构调整原来的设计建议

---

# 2. 首先重新定义系统架构

原来的分析是：

```text
Vue
 ↓
Spring Boot
 ↓
MySQL
 ↓
JT400
 ↓
IBM i
```

这个架构不再适用于最终产品。

最终应该理解为：

```text
┌──────────────────────────────────────────┐
│                IBM i / AS400             │
│                                          │
│  ┌────────────────────────────────────┐  │
│  │          RXAS400ADM                 │  │
│  │                                    │  │
│  │  Vue3 Frontend                     │  │
│  │       ↓                            │  │
│  │  Spring Boot Backend               │  │
│  │       ↓                            │  │
│  │  Application / Domain Service      │  │
│  │       ↓                            │  │
│  │  IBM i Repository / State Store    │  │
│  │       ↓                            │  │
│  │  IBM i Gateway                     │  │
│  │       ↓                            │  │
│  │  JT400 / JDBC / CL / IFS           │  │
│  └────────────────────────────────────┘  │
│                    │                     │
│                    ↓                     │
│       ┌─────────────────────────┐        │
│       │ IBM i System Resources  │        │
│       │                         │        │
│       │ DB2 / Jobs / IFS        │        │
│       │ User Profile / OUTQ     │        │
│       │ Subsystem / Source      │        │
│       │ System Value / CL       │        │
│       └─────────────────────────┘        │
└──────────────────────────────────────────┘
```

这里产生一个非常重要的变化：

> RXAS400ADM 不再是一个“管理远程 AS400 的普通 Web 系统”，而是一个**运行在 IBM i 上的 IBM i 管理平台**。

因此，很多以前的“远程系统一致性问题”实际上会变成：

```text
RXAS400ADM Application
        ↓
IBM i Local DB2
        ↓
IBM i System Resource
```

但即使如此，**DB2 操作和 IBM i 系统操作仍然不是天然的一个业务事务**。

---

# 3. 为什么没有 MySQL 后，仍然存在“事务一致性问题”

这一点非常容易误解。

即使：

```text
MySQL ❌
DB2 for i ✅
```

仍然存在：

```text
DB2 UPDATE
+
CL Command
+
IFS
+
Job
+
User Profile
+
Spool
```

这些操作并不是天然组成一个业务事务。

例如：

```text
创建 IBM i 用户
```

可能执行：

```text
① INSERT RX_USER_OPERATION
② CRTUSRPRF
③ CHGUSRPRF
④ GRPUSRPRF
⑤ 设置 Special Authority
⑥ 写 Audit
```

即使全部运行在同一台 IBM i：

```text
① DB2 SQL
```

和：

```text
② CRTUSRPRF
```

仍然属于不同类型的操作。

所以：

```text
DB2 Transaction
```

不能简单等同于：

```text
IBM i Business Transaction
```

---

# 4. 因此“不使用事务”并不意味着“不需要一致性设计”

这是整个设计中最重要的一句话：

> **不使用 Transaction Management，只代表不依靠数据库事务解决跨步骤业务一致性；并不代表可以忽略一致性。**

应该采用：

```text
Transaction
     ↓
不作为主要一致性手段

State Machine
     ↓
作为业务过程控制中心

Operation Log
     ↓
记录每一步

Idempotency
     ↓
防止重复执行

Retry
     ↓
处理临时失败

Compensation
     ↓
处理不可恢复失败
```

整体：

```text
┌───────────────────────┐
│   Business Operation  │
└───────────┬───────────┘
            ↓
      State Machine
            ↓
       Step Execute
            ↓
     ┌──────┴──────┐
     ↓             ↓
 SUCCESS          FAILED
     ↓             ↓
 Next Step       Retry
     ↓             ↓
 COMPLETED     Compensation
```

---

# 5. 什么是这里的“状态机”

状态机不是简单增加一个：

```text
status = SUCCESS
```

而是：

> **明确记录一个业务操作当前执行到哪一步，以及下一步应该做什么。**

例如：

```text
IBM i 用户创建

REQUESTED
    ↓
CREATING
    ↓
USER_CREATED
    ↓
GROUP_BINDING
    ↓
AUTHORITY_BINDING
    ↓
COMPLETED
```

如果发生异常：

```text
AUTHORITY_BINDING
       ↓
     FAILED
```

然后：

```text
FAILED
  ↓
RETRYING
  ↓
AUTHORITY_BINDING
```

---

# 6. 状态机的核心不是 Status，而是 State + Step

建议不要只有：

```text
status
```

而应该至少有：

```text
operation_id
operation_type
status
current_step
target
request_data
result_data
error_code
error_message
retry_count
last_error_at
next_retry_at
created_by
created_at
updated_at
```

例如：

```text
operation_id = 202609050001

operation_type = CREATE_USER

status = RUNNING

current_step = AUTHORITY_BINDING

target = ROBIN

retry_count = 1
```

系统因此知道：

> ROBIN 用户创建操作已经完成到“权限设置”，当前正在执行权限设置。

---

# 7. 建议的统一 Operation 表

最终可以在 IBM i DB2 中建立：

```text
RX_OPERATION
```

建议字段：

```text
ID
OPERATION_TYPE
TARGET_SERVER
TARGET_OBJECT
STATUS
CURRENT_STEP
REQUEST_ID
IDEMPOTENCY_KEY
REQUEST_DATA
RESULT_DATA
ERROR_CODE
ERROR_MESSAGE
RETRY_COUNT
MAX_RETRY
NEXT_RETRY_TIME
START_TIME
END_TIME
CREATED_USER
CREATED_TIME
UPDATED_TIME
```

例如：

```text
ID              100001
TYPE            CREATE_USER
TARGET          ROBIN
STATUS          FAILED
CURRENT_STEP    AUTHORITY_BINDING
RETRY_COUNT     1
ERROR_CODE      CPF...
```

---

# 8. 再增加 Operation Step 表

建议进一步建立：

```text
RX_OPERATION_STEP
```

例如：

```text
OPERATION_ID
STEP_NO
STEP_CODE
STATUS
START_TIME
END_TIME
REQUEST_DATA
RESULT_DATA
ERROR_CODE
ERROR_MESSAGE
RETRY_COUNT
```

那么一个用户创建操作可以变成：

```text
100001
 │
 ├── 10 CREATE_USER
 │      SUCCESS
 │
 ├── 20 SET_GROUP
 │      SUCCESS
 │
 ├── 30 SET_AUTHORITY
 │      FAILED
 │
 └── 40 AUDIT
        NOT_EXECUTED
```

这比单纯：

```text
status = FAILED
```

强很多。

---

# 9. 为什么需要 Step 表

假设管理员看到：

```text
CREATE_USER = FAILED
```

是不够的。

管理员需要知道：

```text
User: ROBIN

Create User        SUCCESS
Set Group          SUCCESS
Set Authority      FAILED
Audit              SUCCESS
```

于是 UI 可以直接显示：

```text
┌──────────────────────────────┐
│ Create User Operation        │
├──────────────────────────────┤
│ ✔ Create User                │
│ ✔ Set Group                  │
│ ✘ Set Authority              │
│ ○ Complete                   │
└──────────────────────────────┘
```

这对运维系统非常重要。

---

# 10. 状态机不是任意状态跳转

不能允许：

```text
FAILED
 ↓
COMPLETED
```

也不能：

```text
REQUESTED
 ↓
COMPLETED
```

应该定义：

```text
REQUESTED
    ↓
RUNNING
    ↓
STEP_SUCCESS
    ↓
NEXT_STEP
    ↓
COMPLETED
```

失败：

```text
RUNNING
    ↓
FAILED
```

恢复：

```text
FAILED
    ↓
RETRYING
    ↓
RUNNING
```

取消：

```text
REQUESTED
RUNNING
   ↓
CANCEL_REQUESTED
   ↓
CANCELLED
```

---

# 11. 建议的统一状态

所有长流程统一使用：

```text
REQUESTED
RUNNING
WAITING
RETRYING
SUCCESS
FAILED
PARTIAL_SUCCESS
CANCEL_REQUESTED
CANCELLED
```

其中：

### REQUESTED

操作已经创建，但还没有开始。

### RUNNING

正在执行。

### WAITING

等待外部条件。

### RETRYING

正在进行失败恢复。

### SUCCESS

全部步骤成功。

### FAILED

不可继续或者达到最大重试次数。

### PARTIAL_SUCCESS

部分步骤成功，无法自动恢复。

### CANCEL_REQUESTED

用户请求取消。

### CANCELLED

操作最终取消。

---

# 12. 用户创建的完整状态机

例如：

```text
REQUESTED
   │
   ↓
CREATE_USER
   │
   ├── fail → FAILED
   │
   ↓
USER_CREATED
   │
   ↓
SET_GROUP
   │
   ├── fail → RETRYING
   │
   ↓
GROUP_SET
   │
   ↓
SET_AUTHORITY
   │
   ├── fail → RETRYING
   │
   ↓
AUTHORITY_SET
   │
   ↓
COMPLETED
```

---

# 13. Retry 时最重要的问题：不能重新执行已经成功的步骤

这是状态机最大的价值之一。

第一次：

```text
CRTUSRPRF ROBIN
```

成功。

但是：

```text
GRPUSRPRF ROBIN
```

失败。

如果管理员点击 Retry：

错误做法：

```text
CRTUSRPRF ROBIN
GRPUSRPRF ROBIN
...
```

因为：

```text
CRTUSRPRF
```

已经成功。

应该：

```text
current_step = SET_GROUP
```

Retry：

```text
SET_GROUP
```

直接继续。

---

# 14. 这就要求每一步具备幂等性

所谓幂等：

```text
执行一次
```

和：

```text
执行两次
```

最终结果一样。

例如：

```text
SET_GROUP ROBIN DEV
```

如果已经是：

```text
ROBIN → DEV
```

再次执行不应该产生严重副作用。

---

# 15. 对无法天然幂等的 IBM i 操作要特别处理

例如：

```text
CRTUSRPRF
```

不是天然幂等。

第一次：

```text
用户不存在
→ 创建
```

第二次：

```text
用户已经存在
→ CPF...
```

因此应该：

```text
Before Check
```

例如：

```text
IF user exists
    verify desired state
ELSE
    create
```

也就是说：

```text
CREATE_USER
```

实际逻辑应该是：

```text
查询 ROBIN

不存在
 ↓
CRTUSRPRF

存在
 ↓
检查是否就是当前 Operation 创建的用户
 ↓
如果符合预期
 → 认为 STEP SUCCESS
```

---

# 16. Operation 必须具有唯一 Request ID

例如：

```text
REQUEST_ID = 202609050000123
```

或者：

```text
IDEMPOTENCY_KEY = UUID
```

前端因为网络问题连续提交：

```text
Create User
Create User
```

后端收到：

```text
request A
request B
```

不能创建两个 Operation。

应该：

```text
IDEMPOTENCY_KEY
       ↓
查询 RX_OPERATION
       ↓
存在？
 ┌─────┴─────┐
 ↓           ↓
YES         NO
 ↓           ↓
返回已有    创建
Operation   Operation
```

---

# 17. 这对 RXAS400ADM 特别重要

因为管理员操作经常是：

```text
点击按钮
 ↓
等待
 ↓
浏览器 timeout
```

实际上 IBM i 操作可能已经成功。

管理员再点击：

```text
Retry
```

如果没有 Idempotency：

```text
第一次成功
第二次再次执行
```

风险很高。

---

# 18. DocService 应该如何修改

当前文档发布存在：

```text
DB = PUBLISHED
IFS = 写入失败
```

的问题。

在新的设计中应该：

```text
DRAFT
 ↓
PUBLISH_REQUESTED
 ↓
PUBLISHING
 ↓
IFS_WRITE
 ↓
VERIFY
 ↓
PUBLISHED
```

失败：

```text
IFS_WRITE
   ↓
FAILED
```

管理员：

```text
Retry
```

继续：

```text
IFS_WRITE
```

而不是重新把整个文档发布流程全部执行一次。

---

# 19. 文档发布状态

建议：

```text
DRAFT
PUBLISH_REQUESTED
PUBLISHING
PUBLISHED
PUBLISH_FAILED
UNPUBLISHING
UNPUBLISHED
DELETE_REQUESTED
DELETED
```

例如：

```text
DRAFT
 ↓
PUBLISHING
 ↓
写 IFS
 ↓
验证文件
 ↓
PUBLISHED
```

这里尤其建议增加：

```text
VERIFY
```

因为：

```text
writeFile()
返回成功
```

不一定意味着：

```text
目标文件最终正确
```

可以进一步检查：

```text
文件存在
文件大小
checksum
版本
```

---

# 20. 删除操作也应该状态化

当前类似：

```text
IFS delete/move
 ↓
DB delete
```

可能出现：

```text
IFS 成功
DB 失败
```

于是：

```text
文件已经删除
数据库仍然显示存在
```

应该：

```text
DELETE_REQUESTED
 ↓
DELETING
 ↓
IFS_DELETE
 ↓
DB_MARK_DELETED
 ↓
DELETED
```

---

# 21. Restore 同样如此

```text
RESTORE_REQUESTED
 ↓
RESTORING
 ↓
IFS_RESTORE
 ↓
DB_UPDATE
 ↓
RESTORED
```

失败：

```text
RESTORE_FAILED
```

---

# 22. Purge 更需要状态机

永久删除属于高风险操作。

建议：

```text
PURGE_REQUESTED
 ↓
CONFIRMED
 ↓
PURGING
 ↓
IFS_DELETE
 ↓
VERSION_DELETE
 ↓
DOCUMENT_DELETE
 ↓
PURGED
```

而不是一次请求：

```text
delete DB
delete versions
delete IFS
```

---

# 23. Job 操作也适合状态机

例如：

```text
END_JOB
```

可以：

```text
REQUESTED
 ↓
VALIDATING
 ↓
ENDING
 ↓
VERIFYING
 ↓
COMPLETED
```

验证：

```text
JOB 是否仍存在？
```

如果：

```text
ENDJOB
```

已经成功，但是 HTTP 请求 timeout：

Retry 时：

```text
查询 Job
 ↓
不存在
 ↓
认为 END_JOB 已经成功
```

而不是再次执行：

```text
ENDJOB
```

---

# 24. Subsystem END 更应该如此

因为：

```text
END SUBSYSTEM OPTION(*IMMED)
```

属于高风险操作。

建议：

```text
REQUESTED
 ↓
CONFIRM_REQUIRED
 ↓
CONFIRMED
 ↓
EXECUTING
 ↓
VERIFYING
 ↓
COMPLETED
```

这样 UI 可以显示：

> You are about to immediately end subsystem XXXX. This may terminate active jobs.

用户必须明确确认。

---

# 25. IBM i 用户 Profile 操作应该状态化

例如：

```text
CREATE USER
```

状态：

```text
REQUESTED
 ↓
VALIDATING
 ↓
CREATING
 ↓
USER_CREATED
 ↓
CONFIGURING
 ↓
AUTHORITY_SETTING
 ↓
VERIFYING
 ↓
COMPLETED
```

Verification：

```text
DSPUSRPRF / QSYS2
```

确认：

```text
User exists
Group correct
Status correct
Authorities correct
```

之后才：

```text
COMPLETED
```

---

# 26. 为什么“Verify”非常重要

不能认为：

```java
client.execute(command);
```

没有抛异常：

> 就一定完成。

更可靠的是：

```text
Execute
 ↓
Query
 ↓
Compare desired state
 ↓
SUCCESS
```

例如：

```text
要求：
ROBIN → DEV
```

执行：

```text
GRPUSRPRF
```

然后：

```text
Query User Profile
```

确认：

```text
ROBIN primary group = DEV
```

才认为：

```text
STEP SUCCESS
```

---

# 27. 这会形成一个非常适合 RXAS400ADM 的模型

```text
Desired State
      ↓
Operation
      ↓
Execute
      ↓
Actual State
      ↓
Compare
      ↓
SUCCESS / FAILED
```

这其实比简单的事务更适合 IBM i 管理平台。

---

# 28. IBM i 执行边界应该重新设计

第二个核心问题是：

> 当前项目有很多 IBM i 操作，但安全策略分散在不同 Service / Client 中。

例如：

```text
JobService
UserProfileService
SubsystemService
IfsService
SystemValueService
DataAreaService
CommandService
```

各自做：

```text
参数校验
权限检查
危险命令检查
```

容易出现：

```text
A API 安全
B API 忘记检查
C API 检查不完整
```

---

# 29. 应该建立统一的 IBM i Operation

例如：

```text
IbmiOperation
```

定义：

```text
operationCode
riskLevel
permission
targetType
validationPolicy
auditPolicy
confirmationPolicy
```

---

# 30. Operation 示例

```text
JOB_LIST
JOB_DETAIL
JOB_END
JOB_HOLD
JOB_RELEASE

USER_LIST
USER_CREATE
USER_UPDATE
USER_DELETE
USER_AUTHORITY_CHANGE

IFS_READ
IFS_WRITE
IFS_DELETE
IFS_RESTORE

SPOOL_LIST
SPOOL_READ
SPOOL_DELETE

SUBSYSTEM_LIST
SUBSYSTEM_END

SYSTEM_VALUE_READ
SYSTEM_VALUE_CHANGE

RAW_CL
```

---

# 31. 每一个 Operation 定义风险等级

例如：

```text
READ
WRITE
DESTRUCTIVE
CRITICAL
BREAK_GLASS
```

对应：

| Operation | Risk |
|---|---|
| JOB_LIST | READ |
| USER_LIST | READ |
| IFS_READ | READ |
| USER_CREATE | WRITE |
| IFS_WRITE | WRITE |
| SPOOL_DELETE | DESTRUCTIVE |
| JOB_END | DESTRUCTIVE |
| USER_DELETE | CRITICAL |
| CHGSYSVAL | CRITICAL |
| END_SUBSYSTEM_IMMED | CRITICAL |
| RAW_CL | BREAK_GLASS |

---

# 32. 权限不能再只有“大权限”

例如不要只：

```text
USER_MANAGE
```

而应该进一步：

```text
USER_VIEW
USER_CREATE
USER_UPDATE
USER_DELETE
USER_AUTHORITY
USER_ALLOBJ
```

这样：

```text
USER_CREATE
```

不代表：

```text
USER_ALLOBJ
```

---

# 33. 特别是 *ALLOBJ

当前项目允许类似：

```text
*ALLOBJ
*SAVRST
```

作为 User Profile 特殊权限。

这是非常高风险的。

应该至少区分：

```text
USER_AUTHORITY
```

和：

```text
USER_CRITICAL_AUTHORITY
```

例如：

```text
*ALLOBJ
*SECADM
*IOSYSCFG
*SAVSYS
*JOBCTL
```

不能仅仅因为：

```text
USER_MANAGE
```

就全部允许。

---

# 34. Raw CL 必须成为特殊边界

当前：

```text
executeCommand()
```

属于非常强的能力。

建议架构：

```text
Normal API
     ↓
Typed Operation
     ↓
Validation
     ↓
Policy
     ↓
JT400
```

Raw CL：

```text
Raw CL
  ↓
BREAK_GLASS
  ↓
Strong Permission
  ↓
Confirmation
  ↓
Reason
  ↓
Audit
  ↓
JT400
```

---

# 35. Blacklist 不能作为唯一安全边界

当前项目：

```text
DangerousClCommandValidator
```

这个设计有价值，但是：

> Blacklist 只能作为第二道防线。

不能认为：

```text
不包含 DELETE
```

就：

```text
安全
```

更合理的是：

```text
Allowlist
```

例如正常业务只允许：

```text
DSPJOB
ENDJOB
WRKACTJOB
DSPUSRPRF
...
```

对于 Raw CL：

```text
Break-glass
```

再进行更严格的命令策略。

---

# 36. JTOpen Client 不应该成为安全边界

例如：

```text
Controller
 ↓
Service
 ↓
JTOpenClient
```

不能认为：

> 因为 Controller 有 `@PreAuthorize`，所以 Client 可以相信所有参数。

因为以后可能出现：

```text
另一个 Service
 ↓
JTOpenClient
```

直接调用。

因此应该：

```text
Controller
 ↓
Application Service
 ↓
Operation Policy
 ↓
IBM i Gateway
 ↓
JTOpen
```

---

# 37. IBM i Gateway 应该成为统一执行入口

例如：

```java
ibmiGateway.execute(operationContext);
```

OperationContext：

```text
operationCode
server
user
target
parameters
requestId
operationId
```

Gateway 执行前：

```text
① Permission
② Risk
③ Parameter
④ Target
⑤ Audit
⑥ Confirmation
```

然后：

```text
Execute
```

最后：

```text
Audit Result
```

---

# 38. 推荐的完整 IBM i 执行链

```text
HTTP Request
     ↓
Controller
     ↓
Application Service
     ↓
Create Operation
     ↓
State = REQUESTED
     ↓
Operation Policy
     ├── Permission
     ├── Risk
     ├── Parameter
     ├── Confirmation
     └── Audit
     ↓
IBM i Gateway
     ↓
JTOpen
     ↓
IBM i
     ↓
Verify
     ↓
Update Operation State
     ↓
Audit
```

---

# 39. 一个完整的 CREATE USER 示例

用户点击：

```text
Create User
```

提交：

```json
{
  "user": "ROBIN",
  "group": "DEV",
  "authorities": ["*ALLOBJ"]
}
```

系统首先：

```text
POST /users
```

不是直接：

```text
CRTUSRPRF
```

而是：

```text
① 创建 Operation
```

```text
Operation ID = 100001
Status = REQUESTED
```

---

# 40. Policy 检查

系统发现：

```text
authorities = *ALLOBJ
```

于是：

```text
Risk = CRITICAL
```

检查：

```text
USER_CREATE
```

有权限：

```text
YES
```

但是：

```text
USER_CRITICAL_AUTHORITY
```

没有：

```text
NO
```

直接：

```text
REJECTED
```

而不是执行：

```text
CRTUSRPRF
```

---

# 41. 如果用户拥有高风险权限

则：

```text
REQUESTED
 ↓
CONFIRM_REQUIRED
```

UI：

```text
WARNING

You are granting *ALLOBJ authority.

This gives the user extensive IBM i authority.

Confirm?
```

用户确认：

```text
CONFIRMED
```

然后：

```text
RUNNING
```

---

# 42. Step 1

```text
CHECK_USER
```

查询：

```text
ROBIN exists?
```

不存在：

```text
SUCCESS
```

---

# 43. Step 2

```text
CREATE_USER
```

执行：

```text
CRTUSRPRF
```

成功：

```text
USER_CREATED
```

---

# 44. Step 3

```text
SET_GROUP
```

执行：

```text
GRPUSRPRF
```

然后 Verify：

```text
ROBIN primary group = DEV
```

成功：

```text
GROUP_SET
```

---

# 45. Step 4

```text
SET_AUTHORITY
```

执行：

```text
CHGUSRPRF
```

然后：

```text
Query user authority
```

确认：

```text
*ALLOBJ
```

存在。

成功：

```text
AUTHORITY_SET
```

---

# 46. 最终

```text
VERIFY
```

检查：

```text
User exists
Group correct
Status correct
Authority correct
```

全部正确：

```text
COMPLETED
```

---

# 47. 如果 Step 4 失败

状态：

```text
RUNNING
 ↓
SET_AUTHORITY
 ↓
FAILED
```

记录：

```text
ERROR_CODE
ERROR_MESSAGE
```

例如：

```text
CPFxxxx
Authority update failed
```

管理员看到：

```text
Create User       SUCCESS
Set Group         SUCCESS
Set Authority     FAILED
Verify            NOT_EXECUTED
```

---

# 48. Retry

管理员：

```text
Retry
```

系统：

```text
operationId = 100001
currentStep = SET_AUTHORITY
```

所以：

```text
不会重新 CRTUSRPRF
不会重新 GRPUSRPRF
```

只执行：

```text
SET_AUTHORITY
```

---

# 49. 如果 Retry 仍然失败

```text
retry_count = 3
```

达到：

```text
max_retry = 3
```

则：

```text
FAILED
```

管理员可以：

```text
Manual Retry
```

或者：

```text
Compensation
```

---

# 50. 什么是 Compensation

例如：

```text
Create User
成功

Set Group
成功

Set Authority
失败
```

如果最终无法恢复，而业务要求“完全失败”，可以：

```text
DELETE USER
```

进行补偿。

即：

```text
CRTUSRPRF
     ↓
成功
     ↓
GRPUSRPRF
     ↓
成功
     ↓
AUTHORITY
     ↓
失败
     ↓
COMPENSATE
     ↓
DLTUSRPRF
```

---

# 51. 但是 Compensation 不能默认使用

因为：

```text
Delete User
```

本身也是高风险操作。

而且：

```text
这个 User
```

可能已经被其他系统使用。

因此应该区分：

```text
AUTO_COMPENSATABLE
```

和：

```text
MANUAL_COMPENSATION
```

例如：

```text
临时创建目录
```

可以自动删除。

但是：

```text
创建 User Profile
```

通常建议：

```text
MANUAL REVIEW
```

而不是自动删除。

---

# 52. 状态机设计原则

建议遵循：

### 原则 1：每个状态都有明确含义

不能：

```text
FAILED
```

却不知道哪一步失败。

---

### 原则 2：状态只能按照合法路径转换

不能：

```text
FAILED → SUCCESS
```

---

### 原则 3：每一步都有唯一 Step Code

例如：

```text
CREATE_USER
SET_GROUP
SET_AUTHORITY
VERIFY_USER
```

---

### 原则 4：Retry 从失败步骤继续

---

### 原则 5：每一步必须可审计

---

### 原则 6：高风险操作必须二次确认

---

### 原则 7：尽量实现幂等

---

# 53. 状态机不应该全部写在 Controller

错误：

```java
@PostMapping
public void create() {

    if (...) ...
    if (...) ...
    if (...) ...
}
```

最终 Controller 会非常复杂。

应该：

```text
Controller
 ↓
Application Service
 ↓
Operation Service
 ↓
State Machine
 ↓
Step Executor
```

---

# 54. 推荐代码结构

```text
com.rxas400adm
│
├── operation
│   ├── controller
│   ├── service
│   ├── domain
│   │   ├── Operation
│   │   ├── OperationStep
│   │   ├── OperationStatus
│   │   └── OperationType
│   ├── repository
│   └── executor
│
├── security
│   ├── policy
│   │   ├── OperationPolicy
│   │   ├── RiskPolicy
│   │   └── ConfirmationPolicy
│
└── as400
    ├── gateway
    │   ├── JobGateway
    │   ├── UserGateway
    │   ├── IfsGateway
    │   ├── SpoolGateway
    │   └── SystemGateway
    │
    └── jtopena
```

---

# 55. Operation Executor

可以抽象：

```java
public interface OperationExecutor {

    String operationType();

    void execute(OperationContext context);
}
```

例如：

```text
CreateUserExecutor
EndJobExecutor
DeleteSpoolExecutor
PublishDocumentExecutor
ChangeSystemValueExecutor
```

这样每一个复杂业务流程都有自己的 Step。

---

# 56. Step Executor

进一步：

```java
public interface OperationStepExecutor {

    String stepCode();

    StepResult execute(OperationContext context);
}
```

例如：

```text
CREATE_USER
SET_GROUP
SET_AUTHORITY
VERIFY_USER
```

这样状态机本身不关心：

```text
CRTUSRPRF
```

它只负责：

```text
当前 Step
 ↓
调用 Executor
 ↓
SUCCESS / FAILED
 ↓
Next State
```

---

# 57. DB2 for i 只负责保存状态

最终可以理解为：

```text
DB2
 =
State Store
```

而不是：

```text
DB2
 =
唯一的一致性机制
```

DB2 保存：

```text
Operation
Step
Audit
Desired State
Actual State
```

IBM i 系统资源保存：

```text
真实系统状态
```

---

# 58. “Desired State / Actual State”是非常值得引入的概念

例如用户：

```text
Desired:

User = ROBIN
Group = DEV
Status = ENABLED
Authority = *ALLOBJ
```

IBM i 查询：

```text
Actual:

User = ROBIN
Group = DEV
Status = ENABLED
Authority = *ALLOBJ
```

比较：

```text
Desired == Actual
```

则：

```text
COMPLETED
```

否则：

```text
DRIFT
```

---

# 59. 这甚至可以发展成 IBM i Configuration Management

例如：

```text
RXAS400ADM
      ↓
Desired Configuration
      ↓
IBM i
      ↓
Actual Configuration
      ↓
Compare
```

发现：

```text
DRIFT
```

就可以：

```text
Remediate
```

这个方向实际上非常适合 RXAS400ADM。

---

# 60. Audit 和 Operation 必须区分

Operation：

> “这次业务操作是什么状态？”

Audit：

> “谁在什么时间对 IBM i 做了什么操作？”

例如：

```text
Operation
100001
CREATE_USER
COMPLETED
```

Audit：

```text
ROBIN
2026-09-05 17:30
CREATE_USER
TARGET=ROBIN
RESULT=SUCCESS
```

两者不要混成一张表。

---

# 61. 高风险操作 Audit 必须记录更多内容

例如：

```text
operation
user
server
target
operation_type
risk_level
request_data
before_state
after_state
result
error
timestamp
```

但是：

```text
PASSWORD
TOKEN
SECRET
```

必须：

```text
MASK / NEVER STORE
```

特别是当前项目发现的：

```java
log.info("执行创建用户Profile命令: {}", command);
```

如果 command 包含：

```text
PASSWORD(...)
```

属于必须修改的问题。

---

# 62. 最终推荐的统一安全模型

```text
             User
              ↓
         Permission
              ↓
       Operation Policy
              ↓
        Risk Evaluation
              ↓
       Parameter Validation
              ↓
        Confirmation
              ↓
            Audit
              ↓
        Operation State
              ↓
        IBM i Gateway
              ↓
            JT400
              ↓
             IBM i
              ↓
            Verify
              ↓
        Operation State
```

---

# 63. 风险等级对应策略

## READ

例如：

```text
LIST_JOB
READ_IFS
QUERY_SOURCE
```

要求：

```text
Permission
Audit
```

---

## WRITE

例如：

```text
CREATE_USER
WRITE_IFS
CHANGE_DATA_AREA
```

要求：

```text
Permission
Validation
Audit
```

---

## DESTRUCTIVE

例如：

```text
DELETE_SPOOL
DELETE_IFS
END_JOB
```

要求：

```text
Permission
Validation
Confirmation
Audit
```

---

## CRITICAL

例如：

```text
CHANGE_SYSTEM_VALUE
END_SUBSYSTEM_IMMED
DELETE_USER
SPECIAL_AUTHORITY
```

要求：

```text
Special Permission
Confirmation
Reason
Audit
```

必要时：

```text
Approval
MFA
```

---

## BREAK_GLASS

例如：

```text
RAW_CL
```

要求：

```text
Break-glass Permission
Explicit Confirmation
Reason
Audit
Command Policy
Possibly Approval
```

---

# 64. 当前项目应该重点修改的地方

按照新的最终部署模型，我会把优先级重新调整。

## P0

### 1. 去掉“必须使用 @Transactional”的设计目标

不是说：

```text
必须补 @Transactional
```

而是：

```text
明确系统不依赖 Transaction Management
```

---

### 2. 建立统一 Operation / State Machine

这是现在最重要的架构增强。

---

### 3. User Profile 创建流程状态化

特别是：

```text
CRTUSRPRF
GROUP
AUTHORITY
VERIFY
```

---

### 4. Doc / IFS 发布状态化

解决：

```text
DB 状态 ≠ IFS 状态
```

---

### 5. Raw CL 建立 Break-glass 边界

---

### 6. *ALLOBJ 等 Special Authority 单独授权

---

### 7. 禁止记录 PASSWORD CL

---

# 65. P1

### Job

增加：

```text
END_JOB
HOLD_JOB
RELEASE_JOB
```

的 Operation 状态。

---

### IFS

增加：

```text
WRITE
DELETE
RESTORE
PURGE
```

的状态管理。

---

### Spool

增加：

```text
DELETE_SPOOL
```

状态。

---

### Subsystem

增加：

```text
END_SUBSYSTEM
```

状态。

---

### System Value

增加：

```text
CHANGE_SYSTEM_VALUE
```

高风险策略。

---

# 66. P2

进一步建立：

```text
Desired State
Actual State
Drift Detection
Remediation
```

最终形成：

```text
IBM i Configuration Management
```

能力。

---

# 67. 最终推荐架构

```text
┌───────────────────────────────────────────────┐
│                    Vue3                      │
│                                               │
│ Dashboard / Jobs / Users / IFS / Source      │
└──────────────────────┬────────────────────────┘
                       ↓
┌───────────────────────────────────────────────┐
│                REST Controller                │
└──────────────────────┬────────────────────────┘
                       ↓
┌───────────────────────────────────────────────┐
│             Application Service              │
└──────────────────────┬────────────────────────┘
                       ↓
             ┌─────────┴─────────┐
             ↓                   ↓
      Operation Service      Query Service
             ↓
      ┌───────────────┐
      │ State Machine │
      └───────┬───────┘
              ↓
       Operation Policy
              │
      ┌───────┼────────┐
      ↓       ↓        ↓
 Permission  Risk    Audit
      │       │        │
      └───────┼────────┘
              ↓
        Step Executor
              ↓
        IBM i Gateway
              ↓
      ┌───────┼────────────┐
      ↓       ↓            ↓
    Job     User          IFS
  Gateway  Gateway       Gateway
      ↓       ↓            ↓
             JT400
              ↓
┌───────────────────────────────────────────────┐
│                    IBM i                     │
│                                               │
│ DB2 / Jobs / User Profile / IFS / OUTQ       │
│ Subsystem / Source / System Value / CL       │
└───────────────────────────────────────────────┘
```

---

# 68. 最终建议的数据模型

```text
RX_OPERATION
       │
       ├──────── RX_OPERATION_STEP
       │
       ├──────── RX_OPERATION_AUDIT
       │
       └──────── RX_OPERATION_RESULT
```

同时：

```text
RX_OPERATION
       │
       ↓
Desired State
       │
       ↓
IBM i Actual State
       │
       ↓
Verification
```

---

# 69. 最终业务流程

统一变成：

```text
Request
   ↓
Create Operation
   ↓
REQUESTED
   ↓
Policy Check
   ↓
Permission
   ↓
Risk
   ↓
Validation
   ↓
Confirmation
   ↓
RUNNING
   ↓
Step 1
   ↓
Verify
   ↓
Step 2
   ↓
Verify
   ↓
Step N
   ↓
Verify
   ↓
COMPLETED
```

失败：

```text
             ┌──────────────┐
             ↓              │
           FAILED → RETRYING
             │              │
             ↓              │
      Manual Compensation   │
             │              │
             └──────────────┘
```

---

# 70. 与原来的“事务方案”的最终区别

原方案思路：

```text
Business
   ↓
@Transactional
   ↓
DB rollback
```

这个方案适合：

```text
单数据库
```

而 RXAS400ADM 最终更适合：

```text
Business
   ↓
Operation
   ↓
State Machine
   ↓
Step
   ↓
IBM i
   ↓
Verify
   ↓
State
```

也就是说：

> **从“数据库事务一致性”转向“业务过程一致性”。**

---

# 71. 为什么我认为这个方案更适合 RXAS400ADM

因为 RXAS400ADM 本质上不是普通 CRUD 系统。

它管理的是：

```text
IBM i User
IBM i Job
IBM i IFS
IBM i Spool
IBM i Source
IBM i Subsystem
IBM i System Value
IBM i CL
```

这些操作很多都属于：

```text
Command
+
System Resource
+
External State
```

它们天然不是简单的：

```text
INSERT
UPDATE
DELETE
```

所以最适合的不是：

```text
Everything = Transaction
```

而是：

```text
Everything = Operation
```

---

# 72. 最终可以把 RXAS400ADM 定义成“Operation-driven Architecture”

核心思想：

```text
                  RXAS400ADM
                       │
                       ↓
                  Operation
                       │
        ┌──────────────┼──────────────┐
        ↓              ↓              ↓
     Security       State           Audit
        │              │              │
        ↓              ↓              ↓
     Policy        Workflow        History
                       │
                       ↓
                     Step
                       │
                       ↓
                 IBM i Gateway
                       │
                       ↓
                    IBM i
                       │
                       ↓
                   Verify
```

这比单纯的：

```text
Controller → Service → DAO
```

更加符合这个项目的定位。

---

# 73. 最终结论

在“最终部署到 AS400、没有 MySQL、并且项目决定不使用事务管理”的前提下，我建议正式调整之前的架构结论：

### 不再把“缺少 @Transactional”作为主要问题。

真正需要解决的是：

```text
跨步骤业务没有过程状态
        ↓
失败后不知道做到哪一步
        ↓
无法安全 Retry
        ↓
容易重复执行 IBM i 操作
        ↓
DB/IFS/Job/User Profile 状态可能不一致
```

因此核心解决方案应该升级为：

```text
              State Machine
                    +
              Operation Log
                    +
               Idempotency
                    +
                 Retry
                    +
             Verification
                    +
        Compensation / Manual Recovery
```

与此同时，IBM i 执行必须形成统一边界：

```text
              Operation Policy
                    │
        ┌───────────┼───────────┐
        ↓           ↓           ↓
    Permission     Risk       Validation
        │           │           │
        └───────────┼───────────┘
                    ↓
                Confirmation
                    ↓
                  Audit
                    ↓
              IBM i Gateway
                    ↓
                  JT400
                    ↓
                  IBM i
```

**最终目标不是让 RXAS400ADM “事务化”，而是让它成为一个可恢复、可审计、可重试、可验证、具有统一 IBM i 安全边界的 Operation-driven IBM i 管理平台。**

尤其建议下一阶段优先落地以下 5 个基础设施：

1. `RX_OPERATION`
2. `RX_OPERATION_STEP`
3. `OperationStateMachine`
4. `IbmiOperationPolicy`
5. `IbmiGateway`

然后把 **User Profile、Job、IFS/Document、Spool、Subsystem、System Value、Deployment** 逐步迁移到这个模型。

这样后面新增任何 IBM i 管理功能，都不需要重新发明一套“异常处理 + 权限 + 审计 + 重试 + 状态”的机制，而是直接接入统一 Operation Framework。