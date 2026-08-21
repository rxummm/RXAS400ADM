# 开发约定与 Skills 分析（借鉴旧项目）

> 分析对象：`D:\vueprojects\RXAS400`（旧项目，862 个 Java 文件 + 完整前端）
> 分析维度：① 旧项目使用的 Skills 清单　② 旧项目开发约定　③ 对 RXAS400ADM 的借鉴结论与推荐
> 更新日期：2026-08-12

---

## 1. 旧项目使用的 Skills 清单

旧项目在 `.opencode/skills/`（项目级）与用户级 skills 目录中各有一批 skill：

| Skill | 类别 | 作用 | 适用性 |
| --- | --- | --- | --- |
| `rx-admin-dev` | 项目专用 | RX Admin 开发规范：新增 CRUD 模块 14 步流程、@Local/@Remote 数据源、权限码、前端布局硬性要求 | **直接相关**（需按本项目技术栈改写） |
| `git-safety-check` | 通用元技能 | git 操作前安全检查（status/diff、防 force-push、有意义的 commit message） | 通用，直接可用 |
| `lint-gate` | 通用元技能 | 每次编辑后跑 lint/格式/类型检查质量门 | 通用，直接可用 |
| `break-edit-loop` | 通用元技能 | 同一修复失败 ≥2 次时停止编辑、先分析再动手 | 通用，直接可用 |
| `emergency-confusion-reset` | 通用元技能 | 陷入混乱时重置思路 | 通用，直接可用 |
| `frontend-design` | 前端设计 | 前端页面/样式设计规范 | 部分适用（旧项目 JS，本项目 TS） |
| `github-integration` | 协作 | GitHub 集成（PR/issue 等） | 视是否上 GitHub 而定 |
| `skill-creator` / `opencode-skill-creation` | 元技能 | 创建/管理自定义 skill | 创建项目专用 skill 时用 |
| `obra-superpowers` | 通用 | 通用能力增强包 | 可选 |

## 2. 旧项目开发约定（AGENTS.md 要点）

| 约定 | 内容 | 对 RXAS400ADM 的借鉴结论 |
| --- | --- | --- |
| MySQL UTF-8 编码 | `mysql --default-character-set=utf8mb4`，避免 GBK 双编码乱码 | ✅ **已落地**（AGENTS.md「编码警告」节） |
| VS Code 终端乱码 | `chcp 65001` + `-Dfile.encoding=UTF-8` | ✅ 已吸收进 AGENTS.md 运行说明 |
| Playwright 需用户确认 | 页面渲染验证必须显式确认，优先 SQL/API/代码分析 | ✅ 已吸收（本项目以 curl 端到端验证为主） |
| 编译工作流 | 避免 `mvn clean package` 全量重建，用增量 `mvn compile` | ✅ **已落地**（AGENTS.md「编译/运行工作流」节） |
| 14 步新增模块流程 | DDL→Entity→Mapper→DTO→VO→Convert→Service→Impl→Controller→@OperateLog→菜单→前端 API→页面→映射 | ✅ **已落地**（精简为适合本项目 9 模块结构的版本） |
| 构造器注入 | `@RequiredArgsConstructor`，禁止 `@Autowired` 字段注入 | ✅ 本项目已遵循 |
| Entity 不暴露 | Controller 出入参一律 DTO/VO | ✅ 本项目已遵循 |
| 权限码 | `{module}:{entity}:{action}` 每端点必配 | 🔄 本项目沿用设计文档 `{MODULE}_{ACTION}` 风格 |
| 写操作审计 | `@Transactional` + `@OperateLog` | ✅ 本项目已实现 AOP 审计注解 |
| i18n 无硬编码 | 前端文案必须 `$t()` | ✅ 本项目已实现双语 |
| ESLint/Prettier | 前端 lint 质量门 | ⬜ 本项目暂未配置 ESLint（可后续补，见 §4） |

**不借鉴项**（旧项目有但本项目按设计文档已更优或不同）：
- Sa-Token → 保留 **Spring Security 6 + JWT**
- 单模块单体 → 保留 **9 模块 Maven 多模块**
- 前端 JS → 保留 **TypeScript**
- 平台侧 MySQL `rx_admin` → 本项目库名 **`rxas400adm`**、包名 **`com.rxas400adm`**

## 3. 对 RXAS400ADM 的推荐

### 3.1 已落地（本次完成）

| 产出 | 位置 | 说明 |
| --- | --- | --- |
| 项目开发约定 | 根目录 `AGENTS.md` | 快速开始、编码警告、工作流、14 步模块流程、架构 must-knows、验证清单 |
| CI/CD 骨架 | `.github/workflows/backend.yml` + `frontend.yml` | 对应设计文档 Phase 41 §15（mvn test / npm build / 产物上传） |

### 3.2 推荐引入的通用 Skill（复制旧项目即可用）

1. **`git-safety-check`** —— 本项目多代理协作（他人可能同时编辑），git 安全检查价值高
2. **`lint-gate`** —— 与 AGENTS.md 验证清单配合，形成"改完必验"习惯
3. **`break-edit-loop`** —— 通用调试纪律，适合所有项目
4. **`frontend-design`** —— 若后续频繁开发新页面可引入（按 TS + Element Plus 微调）

### 3.3 推荐新建的项目专用 Skill（RxAS400ADM 特有）

| 推荐 Skill | 内容要点 | 价值 |
| --- | --- | --- |
| `rxas400adm-dev` ✅ 已落地（`.opencode/skills/rxas400adm-dev/SKILL.md`） | 从 AGENTS.md 提炼：14 步模块流程、`com.rxas400adm` 包约束、Mapper 包名必须以 `.mapper` 结尾（@MapperScan 约束）、X-AS400-Server 多服务器路由、@OperateLog 审计、i18n 双语、发布流水线端到端验证、本仓库已踩过的坑 | 复刻旧项目 `rx-admin-dev` 的价值，替代通用性不足 |
| `ibmi-cl-command` | IBM i CL 命令知识库：CRTBNDRPG/CRTSQLRPGI/CRTPGM/SAVOBJ/RSTOBJ/DSPOBJD/DSPPGMREF/ENDJOB、QSYS2 视图（SYSTEM_STATUS_INFO/ACTIVE_JOB_INFO/ASP_INFO/SUBSYSTEM_INFO） | 发布/监控/Job 模块开发时减少查资料 |
| `flyway-migration` | 本项目 Flyway 规范：新表用 `V{n}__*.sql`、MySQL 方言、`rx_` 前缀、中文列注释 UTF-8、不手动改已迁移表 | 数据库演进纪律 |

> 注：创建 skill 可用 `skill-creator` / `opencode-skill-creation` 生成模板，放到 `.opencode/skills/`（项目级）。

### 3.4 建议补充的工程化（后续）

| 项 | 说明 | 优先级 |
| --- | --- | --- |
| 前端 ESLint + Prettier | 参照旧项目 `lint-gate`（`no-console`、`no-unused-vars`、`vue/no-v-html`） | P2 |
| `.gitignore` 补 `.opencode/` 等 | 避免工具目录入库 | P1 |
| LICENSE | 设计文档 Phase 41 §1 提及 | P3 |

---

## 4. 结论

- 旧项目最有价值的可迁移资产是**开发约定**（编码陷阱、增量编译工作流、14 步模块流程）——已浓缩进本项目 `AGENTS.md`；
- 通用元技能（git-safety-check / lint-gate / break-edit-loop）**直接推荐引入**；
- 项目专用 skill 建议新建 `rxas400adm-dev`（核心）+ `ibmi-cl-command` + `flyway-migration`（辅助），覆盖本项目与旧项目最不同的三块：多模块 Maven 约束、IBM i 领域知识、Flyway 演进规范。
