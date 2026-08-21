# RXAS400ADM JDK 11 迁移可行性分析

> 分析日期：2026-08-21 | 当前 JDK：17 | 目标 JDK：11 | 分析者：Buffy

## 一、结论摘要

### ❌ 不建议降级到 JDK 11

| 维度 | 评估 | 说明 |
|------|------|------|
| **Spring Boot 3.x** | 🔴 **不可降级** | Spring Boot 3.0+ 最低要求 JDK 17，无法在 JDK 11 上运行 |
| **Jakarta EE** | 🔴 **不可降级** | `jakarta.*` 命名空间需要 Jakarta EE 9+，仅支持 JDK 17+ |
| **Spring Security 6** | 🔴 **不可降级** | 依赖 Spring Boot 3.x，要求 JDK 17+ |
| **Java Records** | 🔴 **99 处** | JDK 14+ 特性，无法在 JDK 11 上编译 |
| **Pattern matching instanceof** | 🟠 **10+ 处** | JDK 16+ 特性 |
| **Switch expressions** | 🟠 **42 处** | JDK 14+ 特性 |
| **Stream.toList()** | 🟠 **113 处** | JDK 16+ 不可变集合工厂方法 |

### 🟢 JDK 11 兼容的特性（无需修改）

| 特性 | 数量 | 说明 |
|------|------|------|
| `var` 关键字 | 4 处 | JDK 10+，JDK 11 支持 |
| `List.of()` / `Map.of()` / `Set.of()` | 152 处 | JDK 9+，JDK 11 支持 |
| `Optional.isEmpty()` | 0 处 | — |
| Lambda / Stream API | 广泛 | JDK 8+，JDK 11 支持 |

---

## 二、详细分析

### 2.1 Spring Boot 3.3.4 — 降级阻断项

```
Spring Boot 3.0+ (2022-11 发布)
├── 最低 JDK 要求：17
├── Jakarta EE 9+（javax.* → jakarta.* 命名空间迁移）
├── Spring Security 6.x（依赖 Jakarta Servlet 5.0+）
└── Spring Framework 6.x（要求 JDK 17+）
```

**影响范围**：整个项目 8 个模块、447 个 Java 文件、94 处 `import jakarta.*`

**降级方案**：需要回退到 Spring Boot 2.7.x（最后支持 JDK 11 的版本），但这意味着：
- `jakarta.*` → `javax.*`（94 处 import 修改）
- Spring Security 5.x → API 差异（`SecurityFilterChain` 配置方式完全不同）
- MyBatis Plus 需要切换到 `mybatis-plus-boot-starter`（非 `spring-boot3-starter`）
- Flyway 配置 API 变更
- 所有 `@PreAuthorize` / `@Valid` 注解路径可能变化

### 2.2 Records（99 处）— JDK 14+ 特性

**使用分布**：

| 模块 | 数量 | 典型用途 |
|------|------|----------|
| `rxas400adm-as400/model` | 20+ | `IfsEntry`、`GraphNode`、`SpoolRow`、`MessageRow` 等 AS400 数据模型 |
| `rxas400adm-common/response` | 5+ | `PageResult`、`ApiResponse` 等统一返回 |
| `rxas400adm-security/vo` | 8+ | `ProfileVO`、`TokenRefreshVO`、`MenuDataResponseVO` 等 |
| `rxas400adm-system/vo` | 10+ | `UnreadCountVO`、`BatchDeleteResultVO`、`FavoriteToggleVO` 等 |
| `rxas400adm-monitor/vo` | 5+ | `OverviewDataVO`、`CapacityTrendVO`、`CompareResultVO` 等 |
| `rxas400adm-app/vo` | 8+ | `CacheInfoVO`、`HealthReportVO`、`TaskInfoVO` 等 |

**降级方案**：每个 record 改写为 `@Data` Lombok 类 + 手动构造函数 + `equals()`/`hashCode()`，工作量约 2-3 天。

### 2.3 Pattern matching instanceof（10+ 处）— JDK 16+ 特性

**示例**：
```java
// 当前（JDK 16+）
if (v instanceof Number n) {
    return n.doubleValue();
}

// JDK 11 降级后
if (v instanceof Number) {
    Number n = (Number) v;
    return n.doubleValue();
}
```

**影响文件**：
- `WsAuthChannelInterceptor.java`
- `InspectionService.java`
- `ReportService.java`
- `BusinessController.java`
- `JTOpenJobClient.java`
- `JTOpenMessageFileClient.java`
- `JTOpenObjectClient.java`
- `JTOpenPfClient.java`
- `JTOpenSubsystemClient.java`

### 2.4 Switch expressions（42 处）— JDK 14+ 特性

**示例**：
```java
// 当前（JDK 14+）
return switch (kind) {
    case "metrics" -> reportService.metricsRows(id, 7);
    case "executions" -> reportService.executionRows(type, status);
    default -> List.of();
};

// JDK 11 降级后
switch (kind) {
    case "metrics": return reportService.metricsRows(id, 7);
    case "executions": return reportService.executionRows(type, status);
    default: return Collections.emptyList();
}
```

### 2.5 Text blocks（2 处）— JDK 15+ 特性

**影响文件**：`JobService.java`（SQL 定义）

```java
// 当前（JDK 15+）
private static final String ACTIVE_JOB_SQL = """
    SELECT JOB_NAME, ... FROM QSYS2 ...
    """;

// JDK 11 降级后
private static final String ACTIVE_JOB_SQL =
    "SELECT JOB_NAME, ... FROM QSYS2 ...";
```

### 2.6 Stream.toList()（113 处）— JDK 16+ 特性

**降级方案**：全部替换为 `.collect(Collectors.toList())`

### 2.7 jakarta.* → javax.*（94 处）

**影响**：`jakarta.validation.constraints.*`、`jakarta.servlet.*`、`jakarta.persistence.*` 等全部需要改回 `javax.*`

---

## 三、替代方案建议

### 方案 A：保持 JDK 17，AS400 上使用 OpenJDK 17 ✅ 推荐

IBM i 7.4+ / 7.5 已支持 OpenJDK 17。

**优势**：
- 零代码修改
- 保持所有现代特性
- Spring Boot 3.3 完整支持
- 性能提升（G1 GC 改进、ZGC 可选）

#### 安装方式对比

| 方式 | 隔离级别 | 适用场景 | 是否影响系统 |
|------|---------|----------|-------------|
| **YUM 系统安装** | ⚠️ 系统级 | 单项目服务器 | 是 |
| **项目内嵌 JDK** ✅ | ✅ 完全隔离 | 多项目共存/生产部署 | 否 |
| Docker 容器 | ✅ 完全隔离 | 容器化部署 | 否 |

#### 方式一：YUM 系统安装（简单但影响全局）

```bash
# IBM i 上执行
yum install openjdk-17

# 设置环境变量
echo 'export JAVA_HOME=/QOpenSys/usr/bin/openjdk-17' >> /etc/profile
source /etc/profile

# 验证
java -version
```

#### 方式二：项目内嵌 JDK ✅ 推荐（完全隔离）

将 OpenJDK 17 安装在项目部署目录内，**不影响系统其他 Java 应用**。

**目标目录结构**：
```
/QOpenSys/rxas400adm/
├── jdk/
│   └── openjdk-17/          ← 项目专用 JDK 17
│       ├── bin/
│       ├── lib/
│       └── ...
├── app/
│   └── rxas400adm-app-1.0.0-SNAPSHOT.jar
├── config/
│   └── application.yml
├── logs/
├── scripts/
│   ├── install.sh            ← 一键部署脚本
│   ├── start.sh
│   └── stop.sh
└── backup/                   ← 旧版本备份
```

**安装步骤**：

```bash
# 1. 创建项目目录
mkdir -p /QOpenSys/rxas400adm/{jdk,app,config,logs,scripts,backup}

# 2. 下载 OpenJDK 17 tar.gz（从 Adoptium）
#    地址：https://api.adoptium.net/v3/binary/latest/17/ga/linux/ppc64le/jdk/hotspot/normal/eclipse
#    或从 IBM 下载：https://www.ibm.com/support/pages/ibm-i-technology-refresh-updates
wget -O /tmp/openjdk17.tar.gz \
  "https://api.adoptium.net/v3/binary/latest/17/ga/linux/ppc64le/jdk/hotspot/normal/eclipse"

# 3. 解压到项目目录
cd /QOpenSys/rxas400adm/jdk
tar xzf /tmp/openjdk17.tar.gz
mv jdk-17* ./openjdk-17

# 4. 验证（使用项目本地 JDK）
/QOpenSys/rxas400adm/jdk/openjdk-17/bin/java -version
# 期望：openjdk version "17.x.x"

# 5. 启动时指定本地 JDK（start.sh 中）
export JAVA_HOME=/QOpenSys/rxas400adm/jdk/openjdk-17
export PATH=$JAVA_HOME/bin:$PATH
java -jar /QOpenSys/rxas400adm/app/rxas400adm-app.jar
```

**优势**：
- 系统默认 Java 可以是 JDK 8/11（其他应用用）
- 只有 `start.sh` 启动时才切到 JDK 17
- 多项目可各自内嵌不同版本 JDK
- 卸载/升级只需删除 `jdk/` 目录

**步骤 3：配置环境变量**
```bash
# 编辑 /etc/profile 或用户 .profile
echo 'export JAVA_HOME=/QOpenSys/usr/bin/openjdk-17' >> /etc/profile
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> /etc/profile

# 立即生效
source /etc/profile

# 验证
java -version
echo $JAVA_HOME
```

**步骤 4：创建部署目录**
```bash
# 在 IFS 上创建应用目录
mkdir -p /QOpenSys/rxas400adm
mkdir -p /QOpenSys/rxas400adm/logs
mkdir -p /QOpenSys/rxas400adm/config

# 设置权限
chmod 755 /QOpenSys/rxas400adm
```

**步骤 5：上传应用 JAR**
```bash
# 从开发机打包
mvn clean package -DskipTests
# 产出：rxas400adm-app/target/rxas400adm-app-1.0.0-SNAPSHOT.jar

# 通过 FTP 上传到 AS400
ftp AS400_HOST
> put rxas400adm-app-1.0.0-SNAPSHOT.jar /QOpenSys/rxas400adm/
```

**步骤 6：配置 application.yml（生产环境）**
```bash
# 创建外部配置文件
cat > /QOpenSys/rxas400adm/config/application.yml << 'EOF'
spring:
  profiles:
    active: production
  datasource:
    url: jdbc:mysql://DB_HOST:3306/rxas400adm?useUnicode=true&characterEncoding=utf-8
    username: ${MYSQL_USERNAME}
    password: ${MYSQL_PASSWORD}

rxas400:
  ibmi:
    host: ${RXAS400_IBMI_HOST}
    user: ${RXAS400_IBMI_USER}
    password: ${RXAS400_IBMI_PASSWORD}
  crypto:
    key: ${RXAS400_CRYPTO_KEY}
  security:
    cors-allowed-origins: https://adm.example.com
EOF
```

**步骤 7：创建启动脚本**
```bash
cat > /QOpenSys/rxas400adm/start.sh << 'SCRIPT'
#!/bin/bash
export JAVA_HOME=/QOpenSys/usr/bin/openjdk-17
export PATH=$JAVA_HOME/bin:$PATH

APP_DIR=/QOpenSys/rxas400adm
LOG_DIR=$APP_DIR/logs
JAR=$APP_DIR/rxas400adm-app-1.0.0-SNAPSHOT.jar

# JVM 参数优化
JAVA_OPTS="-Xms256m -Xmx512m"
JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC"
JAVA_OPTS="$JAVA_OPTS -XX:MaxGCPauseMillis=200"
JAVA_OPTS="$JAVA_OPTS -Dfile.encoding=UTF-8"
JAVA_OPTS="$JAVA_OPTS -Dspring.config.additional-location=$APP_DIR/config/"
JAVA_OPTS="$JAVA_OPTS --add-opens java.base/java.lang=ALL-UNNAMED"

# 日志
LOG_FILE=$LOG_DIR/app-$(date +%Y%m%d).log

nohup java $JAVA_OPTS -jar $JAR \
  --spring.datasource.password="$MYSQL_PASSWORD" \
  > $LOG_FILE 2>&1 &

echo $! > $APP_DIR/app.pid
echo "Started with PID $(cat $APP_DIR/app.pid)"
SCRIPT
chmod +x /QOpenSys/rxas400adm/start.sh
```

**步骤 8：创建停止脚本**
```bash
cat > /QOpenSys/rxas400adm/stop.sh << 'SCRIPT'
#!/bin/bash
APP_DIR=/QOpenSys/rxas400adm
PID_FILE=$APP_DIR/app.pid

if [ -f $PID_FILE ]; then
  PID=$(cat $PID_FILE)
  kill $PID
  rm -f $PID_FILE
  echo "Stopped PID $PID"
else
  echo "No PID file found"
fi
SCRIPT
chmod +x /QOpenSys/rxas400adm/stop.sh
```

**步骤 9：IBM i 服务注册（开机自启）**
```bash
# 方法一：通过 WRKSRVAG 命令注册为受管服务
# CL 命令：
# ADDSRVAG SERVICE(RXAS400ADM) TYPE(*INTERNAL) AUTOSTART(*YES)
#   SVRJOB(QUSER/QZLSOINIT) TEXT('RXAS400 ADM Platform')

# 方法二：通过 /etc/rc.local 添加启动命令
echo '/QOpenSys/rxas400adm/start.sh' >> /etc/rc.local
```

**步骤 10：验证部署**
```bash
# 检查进程
ps -ef | grep java

# 检查端口
netstat -an | grep 8080

# 健康检查
curl http://localhost:8080/actuator/health
# 期望：{"status":"UP"}

# 登录测试
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
```

**步骤 11：防火墙配置**
```bash
# 如果 AS400 有防火墙，开放 8080 端口
# 或通过反向代理（nginx）转发
# STRTCPSVR SERVER(*HTTP) HTTPSVR( rxas400adm )
```

**步骤 12：监控配置**
```bash
# Prometheus 端点（需认证）
curl -u admin:admin123 http://localhost:8080/actuator/prometheus

# 在 Prometheus 中添加 target:
# scrape_configs:
#   - job_name: 'rxas400adm'
#     metrics_path: '/actuator/prometheus'
#     basic_auth:
#       username: admin
#       password: admin123
#     static_configs:
#       - targets: ['AS400_HOST:8080']
```

**常见问题**：

| 问题 | 解决方案 |
|------|----------|
| `java.lang.UnsatisfiedLinkError` | JT400 需要 native library，设置 `LD_LIBRARY_PATH=/QOpenSys/usr/lib` |
| `Port 8080 already in use` | 修改 `server.port` 或停止占用进程 |
| `Connection refused to MySQL` | 检查 MySQL 服务器网络可达性，IBM i 需配置 TCP/IP 路由 |
| `OutOfMemoryError` | 增大 `-Xmx` 或优化连接池大小 |
| `Permission denied` | 检查 IFS 目录权限 `chmod -R 755 /QOpenSys/rxas400adm` |

### 方案 B：降级到 Spring Boot 2.7.x + JDK 11 ⚠️ 高成本

| 工作项 | 工作量 | 风险 |
|--------|--------|------|
| Spring Boot 3.3 → 2.7 | 2-3 周 | 高（API 差异大） |
| Jakarta → javax | 3-5 天 | 中（94 处 import） |
| 99 个 Records → Lombok 类 | 2-3 天 | 中（机械替换） |
| Switch expressions → 传统 switch | 1 天 | 低 |
| Stream.toList() → Collectors.toList() | 半天 | 低 |
| Pattern matching instanceof → 传统 instanceof | 半天 | 低 |
| Security 6 → Security 5 配置 | 3-5 天 | 高（配置方式完全不同） |
| MyBatis Plus 3.5.7 → 3.5.3 | 1 天 | 低 |
| 测试回归 | 3-5 天 | 高 |
| **合计** | **4-6 周** | — |

**不推荐理由**：
1. Spring Boot 2.7 已于 2023-11 EOL，不再接收安全补丁
2. 工作量相当于重写 30% 的后端代码
3. 失去 JDK 17+ 的性能优化（G1 GC 改进约 15-20%）
4. 失去 Records、Pattern matching 等提高可读性的特性

### 方案 C：GraalVM Native Image（JDK 17 + AOT 编译）

如果担心 AS400 上的启动时间或内存占用，可以考虑 GraalVM Native Image：

```xml
<plugin>
    <groupId>org.graalvm.buildtools</groupId>
    <artifactId>native-maven-plugin</artifactId>
</plugin>
```

启动时间从 5-10s 降到 <1s，内存占用从 256MB 降到 50MB。

---

## 四、如果必须降级的最小改动清单

> ⚠️ 仅作为参考，**强烈不推荐执行**

### Phase 1：框架降级（2-3 周）

1. `pom.xml`：Spring Boot 3.3.4 → 2.7.18
2. `pom.xml`：`mybatis-plus-spring-boot3-starter` → `mybatis-plus-boot-starter`
3. 所有 `import jakarta.*` → `import javax.*`（94 处）
4. `SecurityConfig.java`：`SecurityFilterChain` Bean → `WebSecurityConfigurerAdapter`
5. `application.yml`：Spring Boot 2.7 配置格式适配

### Phase 2：语法降级（3-5 天）

1. 99 个 Records → `@Data` Lombok 类
2. 42 处 Switch expressions → 传统 switch
3. 113 处 `Stream.toList()` → `.collect(Collectors.toList())`
4. 10+ 处 Pattern matching instanceof → 传统 instanceof + 强制转换
5. 2 处 Text blocks → 字符串拼接

### Phase 3：测试回归（3-5 天）

1. 所有 `@SpringBootTest` 适配 Spring Boot 2.7
2. Security 切片测试适配
3. 端到端回归测试

---

## 五、总结

| 维度 | JDK 17（当前） | JDK 11（目标） |
|------|----------------|----------------|
| Spring Boot | 3.3.4 ✅ | 2.7.18 ⚠️（EOL） |
| 安全补丁 | 持续接收 ✅ | 已停止 ❌ |
| Records | 99 处 ✅ | 需全部改写 ❌ |
| Switch expressions | 42 处 ✅ | 需全部改写 ❌ |
| Stream.toList() | 113 处 ✅ | 需全部改写 ❌ |
| Jakarta EE | 完整支持 ✅ | 不支持 ❌ |
| 工作量 | 0 | 4-6 周 |
| 风险 | — | 高 |

### 🎯 最终建议

**保持 JDK 17**，在 AS400 上安装 OpenJDK 17（IBM i 7.4+ 官方支持）。这是零成本、零风险、零代码修改的最优方案。如果 AS400 硬件确实只支持 JDK 11（如老旧的 IBM i 7.2），建议评估升级 IBM i OS 版本的可行性，而非降级应用代码。

---

## 附录：AS400 一键部署脚本 install.sh

> 完整的自动化部署脚本，覆盖 JDK 检查/下载/部署/服务注册/健康检查。保存为 `scripts/install.sh`，在 AS400 Qshell 中执行。

```bash
#!/bin/bash
# ============================================================
# RXAS400ADM AS400 一键部署脚本
# 用途：自动完成 JDK 检查、目录创建、JAR 部署、服务注册、健康检查
# 兼容：IBM i 7.4+ / 7.5 (Qshell / SSH)
# 使用：chmod +x install.sh && ./install.sh
# ============================================================
set -e

# ======================== 配置区 ========================
APP_NAME="rxas400adm"
APP_VERSION="1.0.0-SNAPSHOT"
APP_BASE="/QOpenSys/${APP_NAME}"
JDK_DIR="${APP_BASE}/jdk/openjdk-17"
JAR_NAME="${APP_NAME}-app-${APP_VERSION}.jar"
JAR_PATH="${APP_BASE}/app/${JAR_NAME}"
CONFIG_DIR="${APP_BASE}/config"
LOG_DIR="${APP_BASE}/logs"
BACKUP_DIR="${APP_BASE}/backup"
SCRIPTS_DIR="${APP_BASE}/scripts"
PID_FILE="${APP_BASE}/app.pid"

# JDK 下载地址（Adoptium OpenJDK 17 for Linux ppc64le）
JDK_URL="https://api.adoptium.net/v3/binary/latest/17/ga/linux/ppc64le/jdk/hotspot/normal/eclipse"
JDK_TARBALL="/tmp/openjdk17.tar.gz"

# JVM 参数
JVM_OPTS="-Xms256m -Xmx512m"
JVM_OPTS="${JVM_OPTS} -XX:+UseG1GC"
JVM_OPTS="${JVM_OPTS} -XX:MaxGCPauseMillis=200"
JVM_OPTS="${JVM_OPTS} -Dfile.encoding=UTF-8"
JVM_OPTS="${JVM_OPTS} --add-opens java.base/java.lang=ALL-UNNAMED"
JVM_OPTS="${JVM_OPTS} --add-opens java.base/java.util=ALL-UNNAMED"

# 环境变量（生产环境通过 .env 文件或系统环境变量注入）
ENV_FILE="${APP_BASE}/.env"

# ======================== 工具函数 ========================
log_info()  { echo "[INFO]  $(date '+%Y-%m-%d %H:%M:%S') $*"; }
log_warn()  { echo "[WARN]  $(date '+%Y-%m-%d %H:%M:%S') $*"; }
log_error() { echo "[ERROR] $(date '+%Y-%m-%d %H:%M:%S') $*"; }

# ======================== 1. 环境检查 ========================
check_environment() {
    log_info "=== 步骤 1/7: 环境检查 ==="

    # 检查 IBM i 版本
    if command -v system &>/dev/null; then
        OS_VERSION=$(system --version 2>/dev/null || echo "unknown")
        log_info "IBM i 版本: ${OS_VERSION}"
    fi

    # 检查当前 Java
    if command -v java &>/dev/null; then
        CURRENT_JAVA=$(java -version 2>&1 | head -1)
        log_info "当前 Java: ${CURRENT_JAVA}"
    else
        log_warn "系统未安装 Java"
    fi

    # 检查磁盘空间（至少 500MB）
    AVAILABLE_KB=$(df -k "$(dirname ${APP_BASE})" 2>/dev/null | awk 'NR==2{print $4}' || echo 0)
    AVAILABLE_MB=$((AVAILABLE_KB / 1024))
    if [ "${AVAILABLE_MB}" -lt 500 ]; then
        log_error "磁盘空间不足: ${AVAILABLE_MB}MB < 500MB"
        exit 1
    fi
    log_info "可用磁盘空间: ${AVAILABLE_MB}MB"
}

# ======================== 2. 创建目录结构 ========================
create_directories() {
    log_info "=== 步骤 2/7: 创建目录结构 ==="
    mkdir -p "${APP_BASE}"/{jdk,app,config,logs,scripts,backup}
    chmod -R 755 "${APP_BASE}"
    log_info "目录已创建: ${APP_BASE}"
}

# ======================== 3. 安装/更新 JDK ========================
install_jdk() {
    log_info "=== 步骤 3/7: 检查/安装 JDK 17 ==="

    # 如果项目内嵌 JDK 已存在且版本正确，跳过安装
    if [ -x "${JDK_DIR}/bin/java" ]; then
        EXISTING_VERSION=$("${JDK_DIR}/bin/java" -version 2>&1 | head -1)
        if echo "${EXISTING_VERSION}" | grep -q "17"; then
            log_info "项目内嵌 JDK 17 已存在，跳过安装: ${EXISTING_VERSION}"
            return 0
        fi
        log_warn "现有 JDK 版本不匹配，将重新安装"
    fi

    # 检查 YUM 是否可用
    if command -v yum &>/dev/null; then
        log_info "YUM 可用，尝试通过 YUM 安装到项目目录"
        yum install --installroot="${APP_BASE}/jdk" openjdk-17 2>/dev/null && {
            log_info "YUM 安装成功"
            return 0
        }
        log_warn "YUM 安装失败，回退到 tar.gz 方式"
    fi

    # 下载 tar.gz（如果未下载过）
    if [ ! -f "${JDK_TARBALL}" ]; then
        log_info "下载 OpenJDK 17..."
        if command -v wget &>/dev/null; then
            wget -q -O "${JDK_TARBALL}" "${JDK_URL}"
        elif command -v curl &>/dev/null; then
            curl -sL -o "${JDK_TARBALL}" "${JDK_URL}"
        else
            log_error "未找到 wget 或 curl，请手动下载 JDK 到 ${JDK_TARBALL}"
            exit 1
        fi
    fi

    # 解压
    log_info "解压 JDK..."
    cd "${APP_BASE}/jdk"
    tar xzf "${JDK_TARBALL}" 2>/dev/null
    mv jdk-17* ./openjdk-17 2>/dev/null || true

    # 验证
    if [ -x "${JDK_DIR}/bin/java" ]; then
        log_info "JDK 安装成功: $("${JDK_DIR}/bin/java" -version 2>&1 | head -1)"
    else
        log_error "JDK 安装失败，请检查下载文件是否完整"
        exit 1
    fi
}

# ======================== 4. 部署应用 JAR ========================
deploy_jar() {
    log_info "=== 步骤 4/7: 部署应用 JAR ==="

    # 检查 JAR 是否存在于上传目录
    UPLOAD_JAR="${APP_BASE}/upload/${JAR_NAME}"
    if [ ! -f "${UPLOAD_JAR}" ]; then
        # 也检查当前目录
        if [ -f "./${JAR_NAME}" ]; then
            UPLOAD_JAR="./${JAR_NAME}"
        elif [ -f "./target/${JAR_NAME}" ]; then
            UPLOAD_JAR="./target/${JAR_NAME}"
        else
            log_error "未找到 JAR 文件: ${JAR_NAME}"
            log_info "请先将 JAR 上传到 ${APP_BASE}/upload/ 或当前目录"
            exit 1
        fi
    fi

    # 备份旧版本
    if [ -f "${JAR_PATH}" ]; then
        BACKUP_NAME="${BACKUP_DIR}/${APP_NAME}-$(date +%Y%m%d_%H%M%S).jar"
        cp "${JAR_PATH}" "${BACKUP_NAME}"
        log_info "旧版本已备份: ${BACKUP_NAME}"
    fi

    # 部署新版本
    cp "${UPLOAD_JAR}" "${JAR_PATH}"
    log_info "JAR 已部署: ${JAR_PATH}"
}

# ======================== 5. 生成配置文件 ========================
generate_config() {
    log_info "=== 步骤 5/7: 生成配置文件 ==="

    # 如果 .env 不存在，生成模板
    if [ ! -f "${ENV_FILE}" ]; then
        cat > "${ENV_FILE}" << 'ENVEOF'
# RXAS400ADM 生产环境配置
# 数据库
MYSQL_USERNAME=root
MYSQL_PASSWORD=changeme

# AS400 连接
RXAS400_IBMI_HOST=your-as400-host
RXAS400_IBMI_USER=QSECOFR
RXAS400_IBMI_PASSWORD=changeme

# 加密密钥（openssl rand -base64 32 生成）
RXAS400_CRYPTO_KEY=changeme

# CORS
RXAS400_CORS_ALLOWED_ORIGINS=https://adm.example.com

# 端口
SERVER_PORT=8080
ENVEOF
        chmod 600 "${ENV_FILE}"
        log_warn "已生成 .env 模板，请编辑 ${ENV_FILE} 填入真实配置"
    else
        log_info ".env 配置文件已存在"
    fi

    # 生成 application.yml（如果不存在）
    if [ ! -f "${CONFIG_DIR}/application.yml" ]; then
        cat > "${CONFIG_DIR}/application.yml" << 'YMLEOF'
spring:
  application:
    name: rxas400adm
  profiles:
    active: production
  datasource:
    url: jdbc:mysql://localhost:3306/rxas400adm?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: ${MYSQL_USERNAME}
    password: ${MYSQL_PASSWORD}
  flyway:
    enabled: true

server:
  port: ${SERVER_PORT:8080}

logging:
  level:
    com.rxas400adm: info
YMLEOF
        log_info "已生成 application.yml"
    fi
}

# ======================== 6. 生成启停脚本 ========================
generate_scripts() {
    log_info "=== 步骤 6/7: 生成启停脚本 ==="

    # start.sh
    cat > "${SCRIPTS_DIR}/start.sh" << 'STARTEOF'
#!/bin/bash
# RXAS400ADM 启动脚本
APP_BASE="/QOpenSys/rxas400adm"
JDK_DIR="${APP_BASE}/jdk/openjdk-17"
export JAVA_HOME="${JDK_DIR}"
export PATH="${JAVA_HOME}/bin:${PATH}"

# 加载环境变量
[ -f "${APP_BASE}/.env" ] && export $(grep -v '^#' "${APP_BASE}/.env" | xargs)

JAR="${APP_BASE}/app/rxas400adm-app-1.0.0-SNAPSHOT.jar"
LOG="${APP_BASE}/logs/app-$(date +%Y%m%d_%H%M%S).log"
PID_FILE="${APP_BASE}/app.pid"

# 检查是否已在运行
if [ -f "${PID_FILE}" ] && kill -0 $(cat "${PID_FILE}") 2>/dev/null; then
    echo "Application is already running (PID $(cat ${PID_FILE}))"
    exit 1
fi

echo "Starting RXAS400ADM..."
nohup java \
    -Xms256m -Xmx512m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -Dfile.encoding=UTF-8 \
    --add-opens java.base/java.lang=ALL-UNNAMED \
    --add-opens java.base/java.util=ALL-UNNAMED \
    -jar "${JAR}" \
    --spring.config.additional-location="${APP_BASE}/config/" \
    > "${LOG}" 2>&1 &

echo $! > "${PID_FILE}"
echo "Started with PID $(cat ${PID_FILE})"
echo "Log: ${LOG}"
STARTEOF
    chmod +x "${SCRIPTS_DIR}/start.sh"

    # stop.sh
    cat > "${SCRIPTS_DIR}/stop.sh" << 'STOPEOF'
#!/bin/bash
# RXAS400ADM 停止脚本
APP_BASE="/QOpenSys/rxas400adm"
PID_FILE="${APP_BASE}/app.pid"

if [ ! -f "${PID_FILE}" ]; then
    echo "No PID file found. Application may not be running."
    exit 0
fi

PID=$(cat "${PID_FILE}")
if kill -0 "${PID}" 2>/dev/null; then
    echo "Stopping PID ${PID}..."
    kill "${PID}"
    # 等待最多 30 秒
    for i in $(seq 1 30); do
        if ! kill -0 "${PID}" 2>/dev/null; then
            echo "Stopped."
            rm -f "${PID_FILE}"
            exit 0
        fi
        sleep 1
    done
    echo "Force killing PID ${PID}..."
    kill -9 "${PID}"
    rm -f "${PID_FILE}"
    echo "Force stopped."
else
    echo "Process ${PID} is not running."
    rm -f "${PID_FILE}"
fi
STOPEOF
    chmod +x "${SCRIPTS_DIR}/stop.sh"

    # status.sh
    cat > "${SCRIPTS_DIR}/status.sh" << 'STATUSEOF'
#!/bin/bash
APP_BASE="/QOpenSys/rxas400adm"
PID_FILE="${APP_BASE}/app.pid"

if [ -f "${PID_FILE}" ] && kill -0 $(cat "${PID_FILE}") 2>/dev/null; then
    PID=$(cat "${PID_FILE}")
    echo "Running (PID ${PID})"
    # 健康检查
    HEALTH=$(curl -s http://localhost:8080/actuator/health 2>/dev/null)
    if echo "${HEALTH}" | grep -q '"status":"UP"'; then
        echo "Health: UP"
    else
        echo "Health: DOWN or unreachable"
    fi
else
    echo "Not running"
fi
STATUSEOF
    chmod +x "${SCRIPTS_DIR}/status.sh"

    log_info "启停脚本已生成: ${SCRIPTS_DIR}/{start,stop,status}.sh"
}

# ======================== 7. 健康检查 ========================
health_check() {
    log_info "=== 步骤 7/7: 启动并健康检查 ==="

    # 启动应用
    "${SCRIPTS_DIR}/start.sh"

    # 等待启动（最多 60 秒）
    log_info "等待应用启动..."
    for i in $(seq 1 60); do
        HEALTH=$(curl -s http://localhost:8080/actuator/health 2>/dev/null)
        if echo "${HEALTH}" | grep -q '"status":"UP"'; then
            log_info "✅ 应用启动成功！"
            log_info "健康检查: ${HEALTH}"
            log_info "API 文档: http://localhost:8080/swagger-ui.html"
            log_info "Prometheus: http://localhost:8080/actuator/prometheus"
            return 0
        fi
        sleep 2
    done

    log_warn "应用启动超时（60s），请检查日志: ${LOG_DIR}/"
    return 1
}

# ======================== 主流程 ========================
main() {
    echo "========================================"
    echo "  RXAS400ADM AS400 一键部署"
    echo "  版本: ${APP_VERSION}"
    echo "========================================"
    echo ""

    check_environment
    create_directories
    install_jdk
    deploy_jar
    generate_config
    generate_scripts
    health_check

    echo ""
    echo "========================================"
    echo "  部署完成！"
    echo "  启动: ${SCRIPTS_DIR}/start.sh"
    echo "  停止: ${SCRIPTS_DIR}/stop.sh"
    echo "  状态: ${SCRIPTS_DIR}/status.sh"
    echo "  配置: ${ENV_FILE}"
    echo "  日志: ${LOG_DIR}/"
    echo "========================================"
}

main "$@"
```

**使用方式**：

```bash
# 1. 在开发机打包
mvn clean package -DskipTests

# 2. 将 JAR 和脚本上传到 AS400
ftp AS400_HOST
> put target/rxas400adm-app-1.0.0-SNAPSHOT.jar /QOpenSys/rxas400adm/upload/
> put scripts/install.sh /QOpenSys/rxas400adm/scripts/

# 3. 在 AS400 上执行
ssh AS400_HOST
> cd /QOpenSys/rxas400adm/scripts
> chmod +x install.sh
> ./install.sh
```

**脚本功能总结**：

| 步骤 | 功能 | 说明 |
|------|------|------|
| 1 | 环境检查 | IBM i 版本、Java 版本、磁盘空间 |
| 2 | 创建目录 | 完整目录结构 + 权限设置 |
| 3 | 安装 JDK | 优先 YUM，回退 tar.gz，项目内嵌 |
| 4 | 部署 JAR | 自动备份旧版本 + 部署新版本 |
| 5 | 生成配置 | .env 模板 + application.yml |
| 6 | 生成脚本 | start.sh / stop.sh / status.sh |
| 7 | 健康检查 | 启动 + 最多 60s 等待 + curl 验证 |

---

*Generated on 2026-08-21 | Updated with install.sh | Analyzer: Buffy (Codebuff)*
