# 部署到 AS400 服务器 US400CND

> 目标：将 RXAS400 后端（Spring Boot + MySQL）与前端（Vue3 静态资源）部署到 IBM i 服务器 **US400CND**。
> 说明：IBM i 上运行 Java 应用有两种常见方式 —— **Linux LPAR（分区）** 或 **PASE 环境（IFS 中的 AIX 兼容环境）**。本文档两种方式通用，仅启动方式略有差异（systemd vs nohup）。

---

## 1. 部署架构

```
用户浏览器
   │
   ▼
Nginx（US400CND 上，托管前端静态资源 + 反向代理 /api /ws）
   │
   ▼
RXAS400 Backend（Java 17 Spring Boot，端口 8080）
   │
   ├── MySQL 8（rxas400adm 数据库，可为本机或局域网）
   └── JT400 → IBM i 本机对象（多服务器管理：US400CND + 其他 IBM i）
```

## 2. 前置条件（US400CND 上）

| 项 | 要求 |
| --- | --- |
| Java | 17（`java -version` 验证；PASE 下可用 `yum install java17` 或 IBM i PASE for i yum） |
| MySQL 8 | 可达（本机或网络），数据库 `rxas400adm` 已创建（utf8mb4） |
| 目录 | `/QOpenSys/opt/rxas400/backend`、`/QOpenSys/opt/rxas400/frontend`、`/QOpenSys/opt/rxas400/logs` |
| SSH | 可从开发机免密登录（用于 scp/ssh 部署） |
| Nginx | 托管前端 + 反代（可选但推荐） |
| JDBC URL | **AS400 部署时必须将数据源 JDBC URL 替换为 AS400/DB2 连接串并含 `transaction isolation=none`**（见 §5.1），否则连接 AS400 时可能报 `SQL7008`（未开启 Journaling 的表在事务提交时报错）；项目已为无事务架构，需显式 `auto commit=true` |

## 3. 构建

```bash
# 开发机执行
bash scripts/build.sh
# 产物：
#   backend/rxas400adm-app/target/rxas400adm-app-1.0.0-SNAPSHOT.jar
#   frontend/dist/
```

## 4. 一键部署

`scripts/deploy-to-as400.sh` 负责**构建 + 上传 + 重启**，用法：

```bash
# 部署全部（后端 + 前端），需先设置环境变量：
export AS400_HOST=US400CND
export AS400_USER=root
bash scripts/deploy-to-as400.sh

# 或分别部署
bash scripts/deploy-to-as400.sh --backend
bash scripts/deploy-to-as400.sh --frontend
```

**脚本行为**（与本文档同步）：

| 动作 | 说明 |
| --- | --- |
| `--backend` | 本地 `mvn -DskipTests package` → `scp` 上传 jar 与 `start-backend.sh` → 远程 `start-backend.sh restart` |
| `--frontend` | 本地 `npm install + npm run build` → 上传 `frontend/dist/*` 到 `REMOTE_DIR/frontend/`（Nginx 托管） |
| `all`（默认） | 依次执行后端 + 前端 |

> 若部署目标为 PASE 环境且使用 SSH key 登录，把脚本中的 `AS400_USER` 换成实际用户即可。
> 若没有 `scp`（Windows 本机），可用 WinSCP / 或脚本改用 `sftp`。
> **生产部署建议走 CI 多环境流水线**（dev/test/prod 分环境、secrets 注入、回滚），当前为手动脚本 + 前端/后端分离部署，CI 流水线属后续迭代（见 §10 运维注意 5）。

## 5. 后端配置（环境变量）

在 `/QOpenSys/opt/rxas400/backend/rxas400.env` 中配置（systemd 通过 `EnvironmentFile` 读取；nohup 方式需在 shell 中 export）：

```bash
# 必填：JWT 密钥（≥32 字节随机串）
RXAS400_JWT_SECRET=<随机生成，如 openssl rand -base64 48>
# 数据库（默认 localhost:3306/rxas400adm）
RXAS400_DB_HOST=localhost
RXAS400_DB_PORT=3306
RXAS400_DB_NAME=rxas400adm
RXAS400_DB_USER=root
RXAS400_DB_PASSWORD=<数据库密码>
# 可选
RXAS400_PORT=8080
RXAS400_COLLECT_INTERVAL=10000
```

### 5.1 数据源 JDBC URL（部署关键）

> **当前项目 MySQL 环境已配置 `useServerPrepStmts=true&auto-commit=true`**（2026-08-16 修正，原无效参数 `transactions=none` 已移除）。无事务架构下正确配置如下：

- **MySQL 环境**（开发/测试，等效无事务）：URL 已含 `useServerPrepStmts=true&auto-commit=true`（`application.yml` / `application-prod.yml`）
- **AS400/DB2 for i 环境**（生产，US400CND）：替换为 DB2 JDBC 驱动连接串，必须含 `transaction isolation=none` 与 `auto commit=true`，参考格式：

```
jdbc:as400://<host>/<schema>;transaction isolation=none;naming=system;libraries=RXAS400ADM;auto commit=true
```

> 本平台**无 `@Transactional`（零容忍门禁）**，无需 DB2 表开启 Journaling；但连接串漏配 `transaction isolation=none` 时，DB2 默认会尝试事务隔离，未开 Journal 的表报 `SQL7008`。若未来 AS400 成为写数据源并恢复事务，须先对全量业务表执行 `STRJRNPF IMAGES(*BOTH)`（见 AGENTS.md「事务与回滚」§11.2 清单）。

## 6. 启动后端

### 方式 A：Linux LPAR（推荐，systemd 托管）

```bash
sudo cp scripts/rxas400-backend.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now rxas400-backend
sudo systemctl status rxas400-backend
```

### 方式 B：PASE / 无 systemd（nohup 脚本）

```bash
bash /QOpenSys/opt/rxas400/backend/start-backend.sh start
bash /QOpenSys/opt/rxas400/backend/start-backend.sh status
bash /QOpenSys/opt/rxas400/backend/start-backend.sh stop
```

> 首次启动会自动执行 Flyway 迁移建表并写入初始数据（admin/admin123），请登录后立即修改密码。

## 7. 配置 Nginx（托管前端 + 反代）

```nginx
server {
    listen 80;
    server_name US400CND;

    # 前端静态资源
    root /QOpenSys/opt/rxas400/frontend;
    index index.html;
    location / { try_files $uri $uri/ /index.html; }

    # 后端 API
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # WebSocket（发布日志 / 监控推送）
    location /ws {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

## 8. 多 AS400 环境管理（重点）

本平台支持一个平台管理多台 IBM i：

| 环境 | 用途 | 示例 |
| --- | --- | --- |
| PROD | 生产（Critical） | US400CND（默认服务器） |
| TEST | 测试 | TEST400 |
| DEV | 开发 | DEV400 |
| DR | 灾备 | DR400 |

- 登录后在**顶栏服务器选择器**切换当前操作的服务器（`X-AS400-Server` 请求头）
- 未选择时默认使用 `default_server=1` 的服务器（PROD400）
- 服务器注册在 `rx_ibmi_system` 表，可在管理界面增删改与连接测试
- 生产模式（`--spring.profiles.active=prod`）下，后端按服务器配置创建 **JT400 连接**（每服务器缓存）
- 部署后建议在生产界面中：① 修改服务器密码为真实凭据；② 将 `password_encrypt` 以 AES-256 加密存储（后续版本内置）

## 9. 验证

```bash
# 后端健康
curl http://US400CND:8080/v3/api-docs | head

# 登录
curl -X POST http://US400CND:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 指定服务器查询
curl http://US400CND:8080/api/v1/source/libraries \
  -H "Authorization: Bearer <token>" \
  -H "X-AS400-Server: <serverId>"
```

## 10. 运维注意事项

1. **防火墙**：开放 80（Nginx）、8080（API，如直连）、3306（仅内网访问 MySQL）
2. **数据备份**：定期备份 MySQL `rxas400adm`（`mysqldump`）；`rx_metric` 高频表按时间归档
3. **日志**：`/QOpenSys/opt/rxas400/logs/rxas400.log`，配合 `journalctl -u rxas400-backend`（systemd 方式）
4. **回滚**：保留上一版 jar，`cp` 覆盖后重启即回滚
5. **CI 多环境流水线（暂缓项）**：当前为手动 `deploy-to-as400.sh`。后续迭代建议建 dev/test/prod 三环境流水线：分环境注入 secrets（`RXAS400_JWT_SECRET`/`RXAS400_DB_PASSWORD`）、构建产物按环境打 tag、滚动发布与一键回滚、健康检查（`curl /v3/api-docs`）通过后才切换流量。依赖 AS400 主数据源立项后一并落地。