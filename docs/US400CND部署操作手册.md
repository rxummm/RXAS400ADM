# US400CND 部署操作手册（手把手）

> 目标：把 RXAS400ADM 平台（Spring Boot 3 后端 + Vue3 前端）部署到 IBM i 服务器 **US400CND**。
> 本文档是**操作手册**：按步骤执行、每步有验证命令；架构与配置速查见 `docs/部署到US400CND.md`（本文档为其细化版，两者配套使用）。
> 版本基线：rxas400adm-app-1.0.0-SNAPSHOT.jar；Flyway 自动建表；默认演示账号 admin/admin123（**上线后立即改密**）。

---

## 0. 总体流程（先看这张图）

```
开发机 (Windows/Git Bash)
   ① bash scripts/build.sh        —— 构建 jar + 前端 dist
   ② bash scripts/deploy-to-as400.sh —— scp 上传 + 远端重启
          │
          ▼
US400CND (IBM i，Linux LPAR 或 PASE)
   ③ 准备环境：Java 17 / MySQL 8 / 目录 / rxas400.env
   ④ 启动后端（systemd 或 nohup）→ 8080
   ⑤ Nginx 托管前端静态资源 + 反代 /api /ws → 80
   ⑥ 登录 admin → 在「资产清单」配置各服务器真实凭据（密码自动 AES-256-GCM 加密存储）
```

> ⚠️ 两个 profile：默认 `mock`（演示，MockAS400Client）；`prod`（真实 JT400 连接）。
> `start-backend.sh` 已内置 `--spring.profiles.active=prod`。**首次先用 mock 验证平台跑通，再切 prod 接真实服务器。**

---

## 1. 前置条件检查清单

| # | 项 | 检查命令（US400CND 上） | 要求 |
| --- | --- | --- | --- |
| 1 | Java 17 | `java -version` | openjdk 17+ |
| 2 | MySQL 8 | `mysql -uroot -p -e "SELECT VERSION();"` | 8.0+，可被后端访问 |
| 3 | 目录 | `ls -d /QOpenSys/opt/rxas400/{backend,frontend,logs}` | 存在（没有则 `mkdir -p`） |
| 4 | SSH | 开发机 `ssh root@US400CND echo ok` | 免密或密码登录 |
| 5 | Nginx | `nginx -v` | 有则用（推荐）；无则前端可由后端静态托管 |
| 6 | 开发机 Maven + Node | `mvn -v && node -v` | Maven 3.8+ / Node 18+ |

---

## 2. 开发机：构建（Step ①）

```bash
cd /d/vueprojects/RXAS400ADM
bash scripts/build.sh
```

产物确认：

```bash
ls -lh backend/rxas400adm-app/target/rxas400adm-app-1.0.0-SNAPSHOT.jar
ls -lh frontend/dist/index.html
```

> Windows 编码注意：所有源文件必须 UTF-8 无 BOM（构建脚本已按此约定）；如构建报错先看 `docs/项目开发步骤追踪.md` 的验证清单。

---

## 3. US400CND：环境准备（Step ③）

### 3.1 Java 17

- **Linux LPAR**：`sudo yum install -y java-17-openjdk`
- **PASE 环境**：`yum install java17`（IBM i PASE for i 的 yum 源）

验证：`java -version` 显示 17.x。

### 3.2 MySQL 8 与数据库

```bash
# 若 MySQL 未装（PASE 可用 yum install mysql 或沿用已有实例）
mysql -uroot -p <<'SQL'
CREATE DATABASE IF NOT EXISTS rxas400adm
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SQL
```

> 表结构**不要手动建**——后端首次启动时 Flyway 自动迁移（22+ 张 rx_* 表）。

### 3.3 目录

```bash
mkdir -p /QOpenSys/opt/rxas400/{backend,frontend,logs}
```

### 3.4 rxas400.env（后端生产环境变量）

在 `/QOpenSys/opt/rxas400/backend/rxas400.env` 创建：

```bash
# 必填：JWT 密钥（≥32 字节）。生成：openssl rand -base64 48
RXAS400_JWT_SECRET=<粘贴生成的随机串>
# 必填：AS400 连接密码 AES-256-GCM 加密密钥。生成：openssl rand -base64 32
RXAS400_CRYPTO_KEY=<粘贴生成的随机串>
# 数据库（默认 localhost:3306/rxas400adm/root）
RXAS400_DB_HOST=localhost
RXAS400_DB_PORT=3306
RXAS400_DB_NAME=rxas400adm
RXAS400_DB_USER=root
RXAS400_DB_PASSWORD=<数据库密码>
# 可选
RXAS400_PORT=8080
RXAS400_COLLECT_INTERVAL=10000
RXAS400_LOG_DIR=/QOpenSys/opt/rxas400/logs
```

> `start-backend.sh` 会自动加载同目录的 `rxas400.env`（已内置），无需手动 export。
>
> **密钥生成方法**（在开发机或任意有 openssl 的环境执行）：
> ```bash
> # JWT 密钥（48 字节 Base64）
> openssl rand -base64 48
>
> # AES 加密密钥（32 字节 Base64）
> openssl rand -base64 32
> ```
> ⚠️ **重要**：生成后务必妥善保存，密钥丢失 = 所有已加密的 AS400 密码永久无法解密，必须重新配置所有服务器密码。

---

## 4. 部署（Step ②）

### 方式 A：一键脚本（推荐）

```bash
# 开发机
export AS400_HOST=US400CND
export AS400_USER=root          # 按实际 SSH 用户改
bash scripts/deploy-to-as400.sh   # 后端 + 前端全量；可加 --backend / --frontend
```

脚本行为：
1. 构建后端 jar、上传到 `/QOpenSys/opt/rxas400/backend/`
2. 上传 `start-backend.sh`
3. 远端执行 `start-backend.sh restart`（nohup 启动，prod profile）
4. 构建前端 dist、scp 到 `/QOpenSys/opt/rxas400/frontend/`

### 方式 B：手工 scp（排查时用）

```bash
scp backend/rxas400adm-app/target/rxas400adm-app-1.0.0-SNAPSHOT.jar root@US400CND:/QOpenSys/opt/rxas400/backend/
scp scripts/start-backend.sh root@US400CND:/QOpenSys/opt/rxas400/backend/
scp -r frontend/dist/* root@US400CND:/QOpenSys/opt/rxas400/frontend/
```

---

## 5. 启动与验证后端（Step ④）

### 5.1 启动

```bash
# US400CND 上（PASE / 无 systemd 场景，脚本已自动读 rxas400.env）
bash /QOpenSys/opt/rxas400/backend/start-backend.sh start

# 查看状态与日志
bash /QOpenSys/opt/rxas400/backend/start-backend.sh status
tail -f /QOpenSys/opt/rxas400/logs/rxas400.log
```

看到 `Started Rxas400admApplication` 即成功；首次启动 Flyway 自动建表并种子数据。

> **Linux LPAR 且系统有 systemd**，可改用托管方式（可选）：
> ```bash
> sudo cp scripts/rxas400-backend.service /etc/systemd/system/
> sudo systemctl daemon-reload && sudo systemctl enable --now rxas400-backend
> journalctl -u rxas400-backend -f
> ```

### 5.2 验证

```bash
# 健康端点（无需登录）
curl -s http://localhost:8080/api/v1/health | head -c 500

# 登录（默认演示账号）
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

**首次启动后立即登录修改 admin 密码**（用户管理 → 重置/修改）。

---

## 6. Nginx 配置（Step ⑤）

在 US400CND 上编辑 `/etc/nginx/conf.d/rxas400.conf`：

```nginx
server {
    listen 80;
    server_name US400CND;

    # 前端静态资源（Vite 构建产物）
    root /QOpenSys/opt/rxas400/frontend;
    index index.html;
    location / { try_files $uri $uri/ /index.html; }

    # 后端 API
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket（监控推送 / 部署日志 / 告警实时）
    location /ws {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 3600s;
    }
}
```

生效与验证：

```bash
sudo nginx -t && sudo systemctl reload nginx
curl -s -o /dev/null -w "%{http_code}\n" http://localhost/          # 期望 200（前端首页）
curl -s http://localhost/api/v1/health | head -c 200                # 期望 JSON（反代通）
```

---

## 7. 接入真实 IBM i（prod / JT400）

### 7.1 JT400 模式切换（mock ↔ prod）

`AS400ClientProvider`（rxas400adm-as400）按 Spring profile 选择连接实现：

| Profile | 实现类 | 行为 | 用途 |
| --- | --- | --- | --- |
| `mock`（默认） | `MockAS400Client` | 不真实连接，返回演示数据 | 开发 / 演示 / 首次验证平台 |
| `prod` | `JTOpenAS400Client` | 按 `rx_ibmi_system` 逐服务器建 JT400 连接（按服务器 ID 缓存） | 生产接入 US400CND |

```bash
# start-backend.sh 默认已带 --spring.profiles.active=prod；临时演示用 mock：
java -jar rxas400adm-app-1.0.0-SNAPSHOT.jar --spring.profiles.active=mock --spring.datasource.password=xxx
# 切换必须重启：bash start-backend.sh restart
```

> 每次请求经 `X-AS400-Server: <服务器ID>` 头 → `As400ServerContextHolder`（ThreadLocal）→ Provider 按服务器取客户端。
> 顶栏服务器选择器切换即发送该头，登录页 AS400 模式登录时按所选服务器认证。

### 7.2 密码加密（AES-256-GCM，已内置）

`rx_ibmi_system.password_encrypt` 字段使用 **AES-256-GCM** 加密存储，密钥由环境变量 `RXAS400_CRYPTO_KEY` 提供。

**加密流程**：管理员在「服务器管理」页面输入密码 → 后端自动 AES-256-GCM 加密 → 密文写入 `password_encrypt`。每次加密随机生成 12 字节 IV，同一明文产生不同密文。

**解密流程**：JT400 连接时 → `AS400ClientProviderImpl` 从数据库读取密文 → AES-256-GCM 解密 → 还原明文 → 传给 JT400 连接。内存中短暂存在明文，用完即弃。

**存量兼容**：解密失败时自动降级为明文（兼容旧数据），下次编辑服务器时自动加密。

**密钥部署**（见 3.4 `rxas400.env`）：
```bash
# 生成密钥
openssl rand -base64 32
# 写入 rxas400.env
RXAS400_CRYPTO_KEY=<粘贴生成的随机串>
```

> ⚠️ **密钥丢失 = 所有已加密的 AS400 密码永久无法解密，必须重新配置所有服务器密码。请妥善保管生成的密钥。**

### 7.3 配置服务器（平台内完成，不再手改库）

登录后进入**监控中心 → 服务器管理**：新增/编辑服务器（名称/主机/端口/账号/密码/环境 PROD·TEST·DEV·DR/级别/HA 组/默认服务器/启停）、连接测试、执行 CL 命令。
**该页维护的服务器实时同步**到顶栏选择器、登录页 AS400 模式下拉、各业务页服务器下拉。

> 密码在页面上输入后自动 AES-256-GCM 加密存储，无需手动操作数据库。编辑时密码字段留空则不修改现有密码。

### 7.4 验证（重启后端后）

- 服务器管理页「连接测试」→ 全部 ONLINE
- 监控中心出现真实 CPU/MEMORY/DISK 采集；监控页 → 告警规则可**可视化调阈值**（替代改库）
- 顶栏切换服务器验证多服务器路由（`X-AS400-Server` 头）

---

## 7.5 真机联调清单（含 AS400 侧授权）

> US400CND 侧需 IT 管理员配合开通。**建议联调账号用 QSECOFR 或拥有 *ALLOBJ 的 profile**，上线后收敛为最小权限。

### A. 网络与端口（US400CND 侧防火墙）

| 端口 | 服务 | 说明 |
| --- | --- | --- |
| 449 | as-svrmap | JT400 **必需**（服务映射） |
| 8470–8476 | as-central / as-file / as-signon / as-rmtcmd 等 | 数据通道；代码默认 port=8470，联调失败改 449 再试 |
| 80 | Nginx | 对外访问前端 |
| 8080 | 后端 API | 仅内网 |

### B. AS400 用户 profile 授权（最低清单）

| 平台功能 | 需要的命令 / 对象 | 授权建议 |
| --- | --- | --- |
| 监控采集（CPU/内存/磁盘） | `QSYS2.SYSTEM_STATUS`、`QSYS2.SYSTOOL` 视图、ASP 信息 | `*SERVICE` 或 `*ALLOBJ`（联调期） |
| 作业中心（WRKACTJOB / DSPJOBLOG） | 作业列表与日志读取 | `*JOBCTL` + `*USE` on `QUSRTOOL` 相关 |
| 作业控制（HLDJOB / RLSJOB / ENDJOB） | 作业操作命令 | `*JOBCTL` |
| 消息应答（WRKMSG / MSGD） | 消息队列读写 | 对应 MSGF 的 `*CHANGE` |
| 对象搜索（DSPOBJD）与引用（DSPPGMREF） | 库/对象列表、引用数据 | 目标库 `*USE`（搜索）/ `*CHANGE`（引用分析库） |
| 数据查询（QSYS2 SQL） | 任意只读 SELECT | `QSYS2` 视图多数需 `*SERVICE`；业务库 `*USE` |
| IFS 文件 | `/tmp`、`/home` 等路径读写 | 对应目录 `*RWX` 或 5530 权限 |
| 系统服务（STR/END SBS） | 子系统启停 | `*SYSCTL` 或 `*ALLOBJ` |
| 执行 CL 命令（服务器管理页） | 任意命令 | **高危**：建议联调期使用，上线后收回或仅保留运维白名单命令 |
| 报表/审计 | 日志读取 | 只读即可 |

### C. 联调步骤（按序）

1. 确认端口可达：开发机 `telnet US400CND 449`、`telnet US400CND 8470`
2. 服务器管理页新增服务器（host=US400CND、port=8470、联调账号和密码）→ 连接测试
3. 「连接测试」→ ONLINE 后再逐页验证：监控曲线 → 作业列表 → 对象搜索 → 数据查询 → IFS → 报表手动导出
4. 验证告警：告警规则页把 CPU 阈值调到低于当前值 → 应产生告警事件（Webhook/邮件按通道配置推送）
5. 报表定时：报表中心「定时任务」建一个 1 分钟 cron（`0 * * * * ?`）→ 立即执行 → 历史有记录、收件箱有附件
6. 收敛权限：把联调账号降级为最小授权（按 B 表），复测各页功能

---

## 8. 上线验证清单（逐项打勾）

| # | 检查项 | 命令 / 操作 | 期望 |
| --- | --- | --- | --- |
| 1 | 首页可访问 | 浏览器 `http://US400CND` | 登录页渲染 |
| 2 | admin 登录 | admin/admin123 | 进入总览，菜单齐全 |
| 3 | 改默认密码 | 用户管理 → 修改密码 | 旧密码失效 |
| 4 | 多服务器路由 | 顶栏切 PROD/TEST/DEV/DR | 各页数据随服务器切换 |
| 5 | 服务器管理 | 监控中心 → 服务器管理 | 服务器 CRUD / 连接测试 / 执行命令可用 |
| 6 | 监控采集 | 监控页 | CPU/MEMORY/DISK 曲线有数据 |
| 7 | 审计落库 | `SELECT * FROM rx_audit_log ORDER BY id DESC LIMIT 5;` | 登录/操作有记录 |
| 8 | 告警规则配置 | 监控中心 → 告警规则 | 阈值/通道/启停可视化生效；触发后 Webhook/邮件/站内收到 |
| 9 | 报表中心 | 手动导出 + 定时任务 | 下载成功；定时任务按 cron 生成并邮件推送（历史可见） |
| 10 | 防火墙 | 开放 80（对外）、8080（内网）、3306（仅内网）、449/8470–8476（JT400） | 端口可达性按需 |
| 11 | 备份 | `mysqldump -uroot -p rxas400adm > rxas400-$(date +%F).sql` | 定时任务加入 crontab |

---

## 9. 升级与回滚

**升级**：
```bash
# 开发机改代码 → 构建 → 部署（同 Step ②）
bash scripts/deploy-to-as400.sh --backend --frontend
# 新版本 Flyway 迁移会自动执行；升级前先备份数据库
```

**回滚**（保留上一版 jar）：
```bash
# US400CND 上
cd /QOpenSys/opt/rxas400/backend
cp rxas400adm-app-1.0.0-SNAPSHOT.jar.jar.bak rxas400adm-app-1.0.0-SNAPSHOT.jar   # 示例：覆盖旧版
bash start-backend.sh restart
```

> ⚠️ 若新版本含 Flyway 迁移，回滚到旧版可能因 schema 超前失败——升级前 `mysqldump` 备份可整库恢复。

---

## 10. 运维日常

- **日志**：`/QOpenSys/opt/rxas400/logs/rxas400.log`（systemd 方式另见 `journalctl -u rxas400-backend`）
- **指标保留**：默认 90 天自动清理（`rxas400.metric.retention-days`，可在 env 覆盖）
- **告警配置**：监控中心 → 告警规则页（阈值/通道/启停，按服务器维度）；SMTP 收件人/Webhook 在 系统管理 → 参数维护（`alert.email.*`、`alert.webhook.*`）
- **每日同步**：凌晨 2 点 AS400 账号同步（组 profile→角色映射、清理失效账号）
- **备份策略**：MySQL 每日 mysqldump + 每周保留；`rx_metric` 高频表按月归档

---

## 11. 常见问题排查

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| 启动报 `Could not resolve placeholder 'RXAS400_JWT_SECRET'` | env 未生效 | 确认 `rxas400.env` 在 backend 目录、`start-backend.sh` 已自动加载 |
| 启动报 `RXAS400_CRYPTO_KEY` 相关警告 | 加密密钥未设置 | 生产环境必须设置 `RXAS400_CRYPTO_KEY`（见 3.4）；开发/演示可忽略，密码将以明文存储 |
| 连接测试失败（ONLINE → OFFLINE） | 服务器凭据错误或密码加密密钥不匹配 | 重新在服务器管理页输入密码；确认 `RXAS400_CRYPTO_KEY` 与加密时一致 |
| 启动报 Flyway 迁移失败 | 迁移文件损坏/占位符 | 查日志具体 SQL；`{title}` 类花括号内容避开 `${}` 语法；必要时 `validate-on-migrate=false`（已默认） |
| 前端白屏 | Nginx root 指向错误 / dist 未更新 | `ls /QOpenSys/opt/rxas400/frontend` 确认 index.html；重新部署前端 |
| /api 请求 404/502 | Nginx 反代未生效 | `nginx -t`、`curl localhost/api/v1/health` |
| WebSocket 连不上 | 反代缺 upgrade 头 / 后端未启 ws | 检查 Nginx `proxy_set_header Connection "upgrade"`；后端日志 `MessageBroker-*` |
| 监控无数据 | 服务器凭据错误或 mock 模式 | 资产清单连接测试；确认 prod profile + rx_ibmi_system 凭据正确 |
| 中文乱码 | 客户端/DB 编码 | MySQL 用 utf8mb4（建库语句见 3.2）；源文件 UTF-8 无 BOM |
| 8080 被占用 | 旧进程未停 | `bash start-backend.sh stop` 或 `pkill -f rxas400adm` 后重启 |

---

## 12. 交付后建议（可后续做）

- [ ] ~~服务器密码 AES-256 加密存储~~ **已内置**（7.2），无需额外操作
- [ ] 配置 SSL/HTTPS（Nginx 证书）
- [ ] 监控页接入 WebSocket 实时刷新（目前轮询）
- [ ] 把部署命令固化为 crontab 定时备份
- [ ] 告警规则支持聚合指标（如连续 N 次超阈值才告警，当前按单点值）