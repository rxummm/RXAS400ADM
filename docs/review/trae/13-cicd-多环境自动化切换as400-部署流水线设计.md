---
title: "十三、CI/CD 多环境自动化切换：AS400 部署流水线设计"
---

# 十三、CI/CD 多环境自动化切换：AS400 部署流水线设计

> **审计日期**：2026-08-15（第六轮专项审计 —— CI/CD 多环境部署）  
> **审计范围**：GitHub Actions workflows、application.yml 配置外部化、多环境切换策略

---

### 13.1 当前 CI/CD 状态审计

#### [规范警告/Warning] 当前 CI/CD 流水线仅覆盖构建/测试，缺少 AS400 多环境部署阶段

**当前状态**：

| 流水线           | 阶段                                                                                  | 状态    |
| ---------------- | ------------------------------------------------------------------------------------- | ------- |
| `backend.yml`    | compile → layering-gate → v38-static → migration-structure → test → package → verify-fresh-db → upload-artifact | ✅ 完整 |
| `frontend.yml`   | install → i18n-check → test → lint → slot-gate → type-check → build → upload-artifact | ✅ 完整 |
| **AS400 部署**   | 部署到 AS400 测试/生产环境                                                            | ❌ 缺失 |
| **环境变量注入** | 根据目标环境动态切换 `app.tx.enabled`                                                 | ❌ 缺失 |

> **AAA-Remark（2026-08-15 复核）**：✅ 现状描述基本准确（AS400 部署阶段确实缺失），但上表 backend.yml 已过时——现已含 `Layering rules gate`、`V38 seed static consistency`、`Migration structure consistency` 三步静态门禁（2026-08-15 新增），已更新。§13 的 deploy-as400 方案属前瞻设计（需先有 §12 的 tx 开关才有意义），当前可暂缓；若做，注意 secrets 隔离与 production 环境审批门禁两个硬要求。

**🔍 原因分析**：

1. 当前流水线在 `upload-artifact` 后结束，JAR 包和前端 dist 目录仅作为构建产物上传，没有后续的部署步骤。
2. 没有根据目标部署环境（测试/生产）自动切换 `app.tx.enabled` 和 JDBC 隔离级别的机制。
3. AS400 部署通常通过 FTP/SFTP 将 JAR 包传输到 IFS，然后通过 CL 命令启停 Java 服务。这个过程没有集成到 CI/CD 中。

**🛠️ 重构方案：多环境部署流水线**

#### 方案 A：GitHub Actions 多环境部署 Job

```yaml
# .github/workflows/deploy-as400.yml —— AS400 多环境部署流水线
name: Deploy to AS400

on:
  workflow_dispatch:
    inputs:
      environment:
        description: "目标部署环境"
        required: true
        type: choice
        options:
          - test
          - staging
          - production
        default: "test"
      version:
        description: "版本号（留空使用当前 commit SHA）"
        required: false
        type: string

  # 也可在 push tag 时自动触发生产部署
  push:
    tags:
      - "v*.*.*"

jobs:
  # ========== Job 1: 构建 ==========
  build:
    runs-on: ubuntu-latest
    outputs:
      version: ${{ steps.version.outputs.value }}
    steps:
      - uses: actions/checkout@v4

      - name: Determine version
        id: version
        run: |
          if [ "${{ github.event.inputs.version }}" != "" ]; then
            echo "value=${{ github.event.inputs.version }}" >> $GITHUB_OUTPUT
          elif [ "${{ github.ref_type }}" == "tag" ]; then
            echo "value=${{ github.ref_name }}" >> $GITHUB_OUTPUT
          else
            echo "value=$(git rev-parse --short HEAD)" >> $GITHUB_OUTPUT
          fi

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: "17"
          distribution: "temurin"
          cache: maven

      - name: Build backend
        run: cd backend && mvn -q -DskipTests package

      - name: Setup Node
        uses: actions/setup-node@v4
        with:
          node-version: "20"
          cache: npm
          cache-dependency-path: frontend/package-lock.json

      - name: Build frontend
        run: cd frontend && npm ci --no-audit --no-fund && npm run build

      - name: Upload backend artifact
        uses: actions/upload-artifact@v4
        with:
          name: rxas400adm-backend-${{ steps.version.outputs.value }}
          path: backend/rxas400adm-app/target/rxas400adm-app-*.jar
          retention-days: 7

      - name: Upload frontend artifact
        uses: actions/upload-artifact@v4
        with:
          name: rxas400adm-frontend-${{ steps.version.outputs.value }}
          path: frontend/dist
          retention-days: 7

  # ========== Job 2: 部署到测试环境 ==========
  deploy-test:
    needs: build
    if: github.event.inputs.environment == 'test' || github.ref_type == 'branch'
    runs-on: ubuntu-latest
    environment:
      name: test
      url: https://test-rxas400adm.example.com
    steps:
      - name: Download backend artifact
        uses: actions/download-artifact@v4
        with:
          name: rxas400adm-backend-${{ needs.build.outputs.version }}

      - name: Download frontend artifact
        uses: actions/download-artifact@v4
        with:
          name: rxas400adm-frontend-${{ needs.build.outputs.version }}

      - name: Deploy to AS400 Test
        uses: ./.github/actions/deploy-as400
        with:
          host: ${{ secrets.AS400_TEST_HOST }}
          user: ${{ secrets.AS400_TEST_USER }}
          password: ${{ secrets.AS400_TEST_PASSWORD }}
          ifs_path: /QOpenSys/rxas400adm
          # ====== 关键：测试环境 tx-enabled=false ======
          tx_enabled: "false"
          tx_isolation: "none"
          db_auto_commit: "true"
          db_url: ${{ secrets.AS400_TEST_DB_URL }}
          app_port: "8080"
          spring_profile: "as400-test"

  # ========== Job 3: 部署到生产环境（需审批） ==========
  deploy-production:
    needs: build
    if: github.event.inputs.environment == 'production' || github.ref_type == 'tag'
    runs-on: ubuntu-latest
    environment:
      name: production
      url: https://rxas400adm.example.com
    steps:
      - name: Download backend artifact
        uses: actions/download-artifact@v4
        with:
          name: rxas400adm-backend-${{ needs.build.outputs.version }}

      - name: Download frontend artifact
        uses: actions/download-artifact@v4
        with:
          name: rxas400adm-frontend-${{ needs.build.outputs.version }}

      - name: Deploy to AS400 Production
        uses: ./.github/actions/deploy-as400
        with:
          host: ${{ secrets.AS400_PROD_HOST }}
          user: ${{ secrets.AS400_PROD_USER }}
          password: ${{ secrets.AS400_PROD_PASSWORD }}
          ifs_path: /QOpenSys/rxas400adm
          # ====== 关键：生产环境 tx-enabled=true ======
          tx_enabled: "true"
          tx_isolation: "read_committed"
          db_auto_commit: "false"
          db_url: ${{ secrets.AS400_PROD_DB_URL }}
          app_port: "8080"
          spring_profile: "as400"
```

#### 方案 B：自定义 GitHub Action（deploy-as400）

```yaml
# .github/actions/deploy-as400/action.yml
name: "Deploy to AS400"
description: "通过 SFTP 上传 JAR + 前端 dist 到 AS400 IFS，并执行 CL 命令启停服务"
inputs:
  host:
    description: "AS400 hostname or IP"
    required: true
  user:
    description: "AS400 user profile"
    required: true
  password:
    description: "AS400 password"
    required: true
  ifs_path:
    description: "IFS target directory"
    required: true
    default: "/QOpenSys/rxas400adm"
  tx_enabled:
    description: "app.tx.enabled (true/false)"
    required: true
    default: "true"
  tx_isolation:
    description: "JDBC transaction isolation"
    required: true
    default: "read_committed"
  db_auto_commit:
    description: "Hikari auto-commit (true/false)"
    required: true
    default: "false"
  db_url:
    description: "JDBC URL for AS400 DB2"
    required: true
  app_port:
    description: "Application port"
    required: false
    default: "8080"
  spring_profile:
    description: "Spring active profile"
    required: false
    default: "as400"

runs:
  using: "composite"
  steps:
    - name: Upload JAR to AS400 IFS
      uses: actions/checkout@v4
      # 使用 lftp 或 scp 将 JAR 上传到 AS400 IFS
      # 实际部署时替换为合适的 FTP/SFTP action

    - name: Generate application-override.yml
      shell: bash
      run: |
        cat > application-override.yml << 'YAMLEOF'
        # ====== CI/CD 自动生成的环境覆盖配置 ======
        # 生成时间：$(date -u +"%Y-%m-%dT%H:%M:%SZ")
        # 目标环境：${{ inputs.spring_profile }}
        # 构建编号：${{ github.run_id }}

        app:
          tx:
            enabled: ${{ inputs.tx_enabled }}
            isolation: ${{ inputs.tx_isolation }}

        spring:
          datasource:
            url: ${{ inputs.db_url }}
            hikari:
              auto-commit: ${{ inputs.db_auto_commit }}

        server:
          port: ${{ inputs.app_port }}
        YAMLEOF

        echo ">>> 生成的覆盖配置："
        cat application-override.yml

    - name: Execute AS400 CL commands (stop → deploy → start)
      shell: bash
      run: |
        # 以下为示意性 CL 命令，实际执行需通过 JT400 或 SSH 到 AS400
        echo ">>> 1. 停止现有 Java 服务"
        # CL: ENDJOB JOB(RXAS400ADM) OPTION(*IMMED) SPLFILE(*YES)

        echo ">>> 2. 备份旧 JAR"
        # CL: CPY OBJ('/QOpenSys/rxas400adm/app.jar') TODIR('/QOpenSys/rxas400adm/backup')

        echo ">>> 3. 部署新 JAR + 配置"
        # CL: CPY OBJ('<new jar>') TODIR('/QOpenSys/rxas400adm/app.jar')
        # CL: CPY OBJ('application-override.yml') TODIR('/QOpenSys/rxas400adm/config/')

        echo ">>> 4. 启动新 Java 服务"
        # CL: SBMJOB CMD(QSH CMD('java -jar /QOpenSys/rxas400adm/app.jar 
        #      --spring.profiles.active=${{ inputs.spring_profile }}
        #      --spring.config.additional-location=/QOpenSys/rxas400adm/config/')) 
        #      JOB(RXAS400ADM) JOBQ(QSYSNOMAX)

        echo ">>> 5. 等待服务启动（健康检查）"
        # 轮询 curl http://<host>:${{ inputs.app_port }}/actuator/health
        # 最多等待 60 秒

    - name: Health check
      shell: bash
      run: |
        # 调用项目的健康检查接口
        # curl -f http://${{ inputs.host }}:${{ inputs.app_port }}/api/health
```

#### 方案 C：GitLab CI/CD 等价配置

```yaml
# .gitlab-ci.yml —— GitLab CI/CD 多环境部署（AS400）
stages:
  - build
  - deploy-test
  - deploy-production

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"

# ========== 构建阶段 ==========
build:
  stage: build
  image: maven:3.9-eclipse-temurin-17
  script:
    - cd backend && mvn -q -DskipTests package
    - cd ../frontend && npm ci --no-audit --no-fund && npm run build
  artifacts:
    paths:
      - backend/rxas400adm-app/target/rxas400adm-app-*.jar
      - frontend/dist
    expire_in: 7 days

# ========== 测试环境部署 ==========
deploy-test:
  stage: deploy-test
  image: alpine:latest
  environment:
    name: test
  variables:
    # ====== 测试环境：tx-enabled=false ======
    APP_TX_ENABLED: "false"
    APP_TX_ISOLATION: "none"
    DB_AUTO_COMMIT: "true"
    SPRING_PROFILES_ACTIVE: "as400-test"
  script:
    - apk add --no-cache openssh-client sshpass
    - |
      echo "spring.datasource.url=${AS400_TEST_DB_URL}" > override.properties
      echo "app.tx.enabled=${APP_TX_ENABLED}" >> override.properties
      echo "app.tx.isolation=${APP_TX_ISOLATION}" >> override.properties
      echo "spring.datasource.hikari.auto-commit=${DB_AUTO_COMMIT}" >> override.properties
    - sshpass -p "${AS400_TEST_PASSWORD}" scp backend/rxas400adm-app/target/*.jar ${AS400_TEST_USER}@${AS400_TEST_HOST}:/QOpenSys/rxas400adm/app.jar
    - sshpass -p "${AS400_TEST_PASSWORD}" scp override.properties ${AS400_TEST_USER}@${AS400_TEST_HOST}:/QOpenSys/rxas400adm/config/override.properties
    # 启停服务命令...
  only:
    - develop

# ========== 生产环境部署（手动触发 + 审批） ==========
deploy-production:
  stage: deploy-production
  image: alpine:latest
  environment:
    name: production
  variables:
    # ====== 生产环境：tx-enabled=true ======
    APP_TX_ENABLED: "true"
    APP_TX_ISOLATION: "read_committed"
    DB_AUTO_COMMIT: "false"
    SPRING_PROFILES_ACTIVE: "as400"
  script:
    - apk add --no-cache openssh-client sshpass
    - |
      echo "spring.datasource.url=${AS400_PROD_DB_URL}" > override.properties
      echo "app.tx.enabled=${APP_TX_ENABLED}" >> override.properties
      echo "app.tx.isolation=${APP_TX_ISOLATION}" >> override.properties
      echo "spring.datasource.hikari.auto-commit=${DB_AUTO_COMMIT}" >> override.properties
    - sshpass -p "${AS400_PROD_PASSWORD}" scp backend/rxas400adm-app/target/*.jar ${AS400_PROD_USER}@${AS400_PROD_HOST}:/QOpenSys/rxas400adm/app.jar
    - sshpass -p "${AS400_PROD_PASSWORD}" scp override.properties ${AS400_PROD_USER}@${AS400_PROD_HOST}:/QOpenSys/rxas400adm/config/override.properties
  when: manual
  only:
    - main
```

#### 方案 D：GitHub Actions 环境变量 + Secrets 配置清单

| Secret 名称          | 用途              | 测试环境值示例                                                      | 生产环境值示例                                                                |
| -------------------- | ----------------- | ------------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| `AS400_TEST_HOST`    | 测试 AS400 主机   | `test-as400.example.com`                                            | —                                                                             |
| `AS400_TEST_DB_URL`  | 测试 DB2 JDBC URL | `jdbc:as400://test-as400/RXAS400ADM;transaction isolation=none;...` | —                                                                             |
| `AS400_PROD_HOST`    | 生产 AS400 主机   | —                                                                   | `prod-as400.example.com`                                                      |
| `AS400_PROD_DB_URL`  | 生产 DB2 JDBC URL | —                                                                   | `jdbc:as400://prod-as400/RXAS400ADM;transaction isolation=read committed;...` |
| `AS400_TEST_USER`    | 测试 AS400 用户   | `rxadmtest`                                                         | —                                                                             |
| `AS400_PROD_USER`    | 生产 AS400 用户   | —                                                                   | `rxadmprod`                                                                   |
| `RXAS400_JWT_SECRET` | JWT 签名密钥      | `test-secret-xxx`                                                   | `prod-secret-xxx` (≥32字节)                                                   |
| `RXAS400_CRYPTO_KEY` | AES 加密密钥      | `test-key-xxx`                                                      | `prod-key-xxx`                                                                |

**🔍 关键设计原则**：

1. **环境变量优先级链**：`command-line args > application-override.yml > application-${profile}.yml > application.yml`
2. **`app.tx.enabled` 绝不硬编码**：始终通过 Spring 外部化配置（环境变量或 profile 文件）注入，CI/CD 流水线根据目标环境自动设置
3. **生产部署必须有审批门禁**：GitHub Actions 使用 `environment: production` + protection rules；GitLab 使用 `when: manual`
4. **Secrets 隔离**：测试和生产使用不同的 GitHub Secrets，互不交叉

---
