# RXAS400 — Enterprise IBM i Operation Platform

基于 `docs/RXAS400-分析总结.md`（由三份 ChatGPT 设计文档整理）落地的 **IBM i（AS400）运维管理平台** MVP。

> 设计文档来源：`docs/` 目录下三个 HTML 导出文件 → 详见 [docs/RXAS400-分析总结.md](docs/RXAS400-分析总结.md)

## 技术栈

| 端 | 技术 |
| --- | --- |
| 后端 | Java 17 · Spring Boot 3.3 · Spring Security 6 (JWT) · MyBatis Plus · MySQL 8 · Flyway · WebSocket (STOMP) · JT400 |
| 前端 | Vue 3 · TypeScript · Vite · Element Plus · ECharts · Pinia · vue-i18n · vue-router |

## 目录结构

```
RXAS400
├── docs/                          # 设计文档（HTML 原件 + 分析总结 md）
├── backend/                       # Spring Boot 多模块 Maven
│   ├── rxas400adm-common          # 统一返回 / 异常 / 常量
│   ├── rxas400adm-system          # 用户 / 角色 / 权限 (RBAC)
│   ├── rxas400adm-security        # 登录 / JWT / 权限过滤
│   ├── rxas400adm-as400           # IBM i 连接抽象（JT400 + Mock）
│   ├── rxas400adm-source          # Source Library / File / Member
│   ├── rxas400adm-compile         # 编译 (CRTBNDRPG 等)
│   ├── rxas400adm-deploy          # 发布流水线 / 审批 / 回滚 / WebSocket 日志
│   ├── rxas400adm-monitor         # 指标采集 / 告警 / WebSocket 推送
│   └── rxas400adm-app             # 启动模块（配置 + Flyway + 演示数据）
└── frontend/                      # Vue3 + TS + Vite
    └── src/{api,router,stores,layout,views}
```

## 快速启动

### 1. 准备数据库（MySQL 8，库名 rxas400adm）

方式 A：本机已安装 MySQL（推荐，开发环境即如此）

```sql
CREATE DATABASE IF NOT EXISTS rxas400adm DEFAULT CHARACTER SET utf8mb4;
```

方式 B：Docker（可选）

```bash
docker compose up -d
```

### 2. 启动后端（默认 mock 模式，无需真实 IBM i）

```bash
cd backend
mvn -DskipTests package
java -jar rxas400adm-app/target/rxas400adm-app-1.0.0-SNAPSHOT.jar
```

- API 地址：`http://localhost:8080/api/v1`，接口文档：`http://localhost:8080/swagger-ui.html`
- 演示账号：`admin / admin123`
- 首次启动自动建表（Flyway）并写入演示数据（4 个 IBM i 环境：PROD400/TEST400/DEV400/DR400）

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

- 页面地址：`http://localhost:5173`
- 已配置 `/api`、`/ws` 代理到后端
- 顶栏可切换语言（中/英）与当前操作的 AS400 服务器

### 接入真实 IBM i

去掉 `mock` profile 并配置连接参数：

```bash
java -jar rxas400adm-app/target/rxas400adm-app-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --RXAS400_IBMI_HOST=10.1.1.10 \
  --RXAS400_IBMI_USER=QSECOFR \
  --RXAS400_IBMI_PASSWORD=***
```

## 核心功能（MVP）

- **认证与 RBAC**：JWT 无状态登录，`@PreAuthorize` 权限码控制（DEPLOY_EXECUTE / USER_MANAGE 等），@OperateLog 审计日志
- **多 AS400 环境管理**：多实例注册（PROD/TEST/DEV/DR）、顶栏服务器切换（X-AS400-Server 头）、连接测试、CL 命令执行
- **发布中心**：Pipeline 流水线（COMPILE → SAVE → TRANSFER → RESTORE → VERIFY）、失败重试与回滚、审批流、WebSocket 实时日志
- **监控中心**：CPU / 内存采集（插件化 Collector）、定时调度、阈值告警、WebSocket 实时图表
- **源代码**：Library → File → Member 浏览与编译
- **国际化**：中/英双语 + 动态菜单

## 文档

- [分析总结](docs/RXAS400-分析总结.md) · [新旧项目对比](docs/RXAS400-vs-RXAS400ADM-对比分析.md) · [步骤追踪](docs/项目开发步骤追踪.md) · [部署 US400CND](docs/部署到US400CND.md)

## 后续路线（对应设计文档 Phase 42+）

- [ ] Job 中心（WRKACTJOB / ENDJOB）、Object 中心（DSPOBJD）
- [ ] 指标存储分层：TimescaleDB hypertable + Redis 缓存
- [ ] 高可用：调度器 Leader Election、多节点部署
- [ ] 多租户 / i18n / 时区
- [ ] CI/CD（GitHub Actions + Docker）
