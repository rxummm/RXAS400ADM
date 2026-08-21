---
title: "十二、AS400 动态事务开关架构设计：NoOpTransactionManager 与 SQL7008 防治"
---

# 十二、AS400 动态事务开关架构设计：NoOpTransactionManager 与 SQL7008 防治

> **审计日期**：2026-08-15（第五轮专项审计 —— 动态事务管理器）
> **审计范围**：全量 `@Transactional` 注解（51 个方法）、Spring 事务管理器配置、AS400 Journaling 依赖
> **核心痛点**：项目当前**没有任何动态事务开关机制**——所有 `@Transactional` 注解无条件生效，在 AS400 未开启 Journaling 的测试环境会触发 `SQL7008` 错误导致业务崩溃。

---

### 12.1 问题背景：为什么需要动态事务开关？

#### 🛑 [严重/Critical] 项目缺少 `app.tx-enabled` 动态事务开关，AS400 多环境部署存在 SQL7008 崩溃风险

> **AAA-Remark（2026-08-15 复核）**：✅ 现状描述准确（全库确实无任何事务开关机制、无 `@EnableAsync`）。⚠️ **定性修正**：这是**面向未来 AS400 数据源的方案设计**而非当前缺陷——本项目当前跑 MySQL（无 SQL7008 概念），AS400 仅是 mock/JT400 查询连接。⚠️ **方案风险提示**：`NoOpTransactionManager` 关闭回滚意味着测试环境数据**不可回滚**（文档已自述），一旦误配到生产会造成静默数据不一致；且 `@Transactional` 切面在 `tx-enabled=false` 时语义被架空，新人易误解。建议：① 若确需支持无 Journaling 的测试环境，优先「测试环境也开 Journaling」而非关闭事务；② 开关默认值 `matchIfMissing=true`（生产模式）是对的；③ 该方案应在真正接入 AS400 主数据源时再落地，当前排期价值低。→ **状态：✅ 已修复（D1任务：§19最终决策取消全部事务控制，移除21个Service共77处@Transactional，JDBC URL配置transaction isolation=none）**

**问题定性**：属于 **"AS400 适配与动态事务开关架构设计"** 问题。

**🔍 原因分析**：

1. **AS400 Journaling 强依赖**：AS400 (IBM i) 的 DB2 for i 物理表（PF）默认**不开启 Journaling**。开启 Journaling 需要运维执行 `STRJRNPF` 命令并为每个表维护 Journal Receiver。在测试环境，运维团队通常**不愿意开启 Journaling**（增加磁盘 I/O 和管理成本），但代码中的 `@Transactional` 注解仍然会尝试提交/回滚事务，导致 `SQL7008: <table> not valid for operation` 错误。

2. **SQL7008 的破坏性**：`SQL7008` 不是一个静默的警告——它是一个**硬错误**，会导致整个数据库操作失败。用户在前端看到的是"系统内部错误"，实际是底层物理表未开启 Journaling 导致的事务提交失败。

3. **多环境矛盾**：
   - **生产环境**：已开启 Journaling，`transaction isolation = read committed`，JDBC `auto-commit = false`，事务回滚正常工作
   - **测试环境**：未开启 Journaling，如果同样配置事务，任何写操作都会触发 `SQL7008`
   - **本地开发环境**：使用 MySQL/H2，不存在 Journaling 概念，事务正常工作

4. **当前项目的硬编码风险**：`application.yml` 中 JDBC 连接串配置了 `spring.datasource.hikari.auto-commit=false`，但没有环境差异化的事务管理器配置。这意味着：
   - 切换到 AS400 数据源时，所有 `@Transactional` 方法都会尝试在 AS400 上开启事务
   - 如果运维未执行 `STRJRNPF`，任何写操作（INSERT/UPDATE/DELETE）都会触发 `SQL7008`
   - 没有机制可以在测试环境"关闭"事务回滚功能

**🛠️ 重构方案：三层架构设计**

#### 方案总览

```text
┌─────────────────────────────────────────────────────────────┐
│ app.tx-enabled 开关                                        │
│ true  → 生产环境 → DataSourceTransactionManager (正常回滚) │
│ false → 测试环境 → NoOpTransactionManager (关闭回滚)       │
└─────────────────────────────────────────────────────────────┘
```

#### 步骤一：定义配置属性类

```java
// TxProperties.java —— 事务开关配置
// 放置路径：backend/rxas400adm-common/src/main/java/com/rxas400adm/common/config/TxProperties.java

package com.rxas400adm.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.tx")
public class TxProperties {

    /**
     * 事务开关：true = 启用事务回滚（生产环境，AS400 表已开启 Journaling）；
     * false = 关闭事务回滚（测试环境，AS400 表未开启 Journaling，避免 SQL7008）。
     * 默认值 true：安全优先，未配置时按生产环境处理。
     */
    private boolean enabled = true;

    /**
     * 事务隔离级别（仅 tx-enabled=true 时生效）。
     * 生产环境推荐：read_committed（对应 DB2 Cursor Stability）。
     * 取值：read_committed / read_uncommitted / repeatable_read / serializable
     */
    private String isolation = "read_committed";
}
```

#### 步骤二：实现 NoOpTransactionManager（关闭回滚时使用）

```java
// NoOpTransactionManager.java —— 无操作事务管理器
// 放置路径：backend/rxas400adm-common/src/main/java/com/rxas400adm/common/config/NoOpTransactionManager.java

package com.rxas400adm.common.config;

import org.springframework.transaction.*;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

/**
 * 无操作事务管理器：当 app.tx-enabled=false 时注入。
 * 所有 @Transactional 注解的方法仍会进入事务切面，但 commit/rollback 均为空操作，
 * 不会向 AS400 发送 COMMIT/ROLLBACK 指令，从而避免 SQL7008 错误。
 *
 * 设计要点：
 * 1. 继承 AbstractPlatformTransactionManager 而非实现 PlatformTransactionManager 接口，
 *    因为 Spring 的事务切面（TransactionInterceptor）依赖 AbstractPlatformTransactionManager
 *    的 doBegin/doCommit/doRollback 模板方法。
 * 2. getTransaction() 返回一个空的 TransactionStatus，事务切面不会报错。
 * 3. 数据库操作直接写入物理表（相当于 auto-commit 模式），无法回滚。
 */
public class NoOpTransactionManager extends AbstractPlatformTransactionManager {

    private static final Object TRANSACTION_OBJECT = new Object();

    @Override
    protected Object doGetTransaction() throws TransactionException {
        return new NoOpTransactionObject();
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
        // 空操作：不开启 AS400 事务，避免 SQL7008
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
        // 空操作：不提交（数据已在 INSERT/UPDATE/DELETE 时直接写入）
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
        // 空操作：不回滚（数据已写入物理表，无法回滚）
        // 生产环境应设置 tx-enabled=true 以避免此问题
    }

    private static class NoOpTransactionObject {
        // 空的资源持有者，满足 Spring 事务切面要求
    }
}
```

#### 步骤三：条件化事务管理器配置

```java
// TxManagerConfiguration.java —— 动态事务管理器配置
// 放置路径：backend/rxas400adm-common/src/main/java/com/rxas400adm/common/config/TxManagerConfiguration.java

package com.rxas400adm.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TxManagerConfiguration {

    private final TxProperties txProperties;

    /**
     * 生产环境事务管理器：tx-enabled=true 时注入。
     * 使用标准 DataSourceTransactionManager，支持完整的 commit/rollback。
     * 要求 AS400 物理表已开启 Journaling（STRJRNPF IMAGES(*BOTH)）。
     *
     * @Primary 确保覆盖 Spring Boot 自动配置的默认事务管理器
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.tx.enabled", havingValue = "true", matchIfMissing = true)
    public PlatformTransactionManager txManager(DataSource dataSource) {
        log.info(">>> 事务管理器：DataSourceTransactionManager（tx-enabled=true，生产模式）");
        DataSourceTransactionManager txManager = new DataSourceTransactionManager(dataSource);
        // 全局默认超时 30 秒，防止长事务锁表
        txManager.setDefaultTimeout(30);
        return txManager;
    }

    /**
     * 测试环境事务管理器：tx-enabled=false 时注入。
     * 使用 NoOpTransactionManager，所有 @Transactional 注解的方法不会触发实际的
     * commit/rollback，避免 AS400 未开 Journaling 时的 SQL7008 错误。
     *
     * 警告：此模式下数据直接写入物理表，无法回滚。仅用于测试环境！
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.tx.enabled", havingValue = "false")
    public PlatformTransactionManager noOpTxManager() {
        log.warn(">>> 事务管理器：NoOpTransactionManager（tx-enabled=false，测试模式，回滚无效！）");
        return new NoOpTransactionManager();
    }
}
```

#### 步骤四：环境差异化配置文件

```yaml
# application-as400.yml —— AS400 生产环境配置（tx-enabled=true）
spring:
  datasource:
    url: jdbc:as400://${RXAS400_DB_HOST:localhost}/${RXAS400_DB_SCHEMA:RXAS400ADM};transaction isolation=read committed;naming=system;libraries=RXAS400ADM
    driver-class-name: com.ibm.as400.access.AS400JDBCDriver
    hikari:
      auto-commit: false
      transaction-isolation: TRANSACTION_READ_COMMITTED

app:
  tx:
    enabled: true # 生产环境：AS400 表已开启 Journaling，事务回滚正常
    isolation: read_committed
```

```yaml
# application-as400-test.yml —— AS400 测试环境配置（tx-enabled=false）
spring:
  datasource:
    url: jdbc:as400://${RXAS400_DB_HOST:test-as400}/${RXAS400_DB_SCHEMA:RXAS400ADM};transaction isolation=none;naming=system;libraries=RXAS400ADM
    driver-class-name: com.ibm.as400.access.AS400JDBCDriver
    hikari:
      auto-commit: true
      transaction-isolation: TRANSACTION_NONE

app:
  tx:
    enabled: false # 测试环境：AS400 表未开启 Journaling，关闭回滚避免 SQL7008
    isolation: none
```

#### 步骤五：CI/CD 环境变量覆盖（见第十三章）

```bash
# 生产环境部署时通过环境变量强制开启事务
export APP_TX_ENABLED=true

# 测试环境部署时通过环境变量关闭事务
export APP_TX_ENABLED=false
```

**🔍 设计原理说明**：

1. **`@Transactional(rollbackFor = Exception.class)` 注解无需修改**：当 `tx-enabled=false` 时，Spring 注入的是 `NoOpTransactionManager`，`@Transactional` 注解仍然生效（切面仍然拦截），但 `doBegin/doCommit/doRollback` 都是空操作。这意味着：
   - 代码中的 `@Transactional` 注解**不需要任何修改**
   - 事务切面正常执行，只是底层没有真正的 commit/rollback
   - AS400 不会收到 COMMIT/ROLLBACK 指令，因此不会触发 SQL7008

2. **JDBC 隔离级别同步切换**：`tx-enabled=true` 时，JDBC URL 中 `transaction isolation=read committed`；`tx-enabled=false` 时，JDBC URL 中 `transaction isolation=none`。这与事务管理器状态保持一致。

3. **安全优先原则**：`matchIfMissing = true` 确保如果运维忘记配置 `app.tx.enabled`，默认使用生产模式（`tx-enabled=true`），避免测试环境的事务管理器意外泄露到生产环境。

4. **`JTOpenAS400Client` 不受影响**：`JTOpenAS400Client` 使用独立的 `AS400JDBCDataSource` 连接，不经过 Spring 事务管理器，因此无论 `tx-enabled` 为何值，纯查询连接都不受影响。

---

### 12.2 全量 `@Transactional` 注解与动态开关兼容性验证

#### ✅ [验证通过] 所有 51 个 `@Transactional` 方法均兼容动态开关

**验证逻辑**：Spring 的 `@Transactional` 注解通过 `TransactionInterceptor` 切面工作，切面从容器中获取 `PlatformTransactionManager` bean。当 `NoOpTransactionManager` 被注入时，切面仍然正常工作（调用 `getTransaction()` → `commit()` / `rollback()`），只是底层没有实际的数据库事务操作。因此**无需修改任何现有代码**。

**注意**：`@Transactional(rollbackFor = Exception.class)` 的 `rollbackFor` 声明在 `NoOpTransactionManager` 模式下**语义上无效**（因为不会真正回滚），但**语法上完全兼容**——`NoOpTransactionManager.doRollback()` 是空操作，不会报错。

---

### 12.3 AS400 Journaling 与动态事务开关的协作关系

| 场景           | tx-enabled | AS400 Journaling     | JDBC 隔离级别    | 事务回滚              | SQL7008 风险     |
| -------------- | ---------- | -------------------- | ---------------- | --------------------- | ---------------- |
| 生产环境       | `true`     | ✅ 已开启 (STRJRNPF) | `read committed` | ✅ 正常回滚           | 无               |
| 测试环境       | `false`    | ❌ 未开启            | `none`           | ❌ 不回滚（数据直写） | 无               |
| **错误配置 A** | `true`     | ❌ 未开启            | `read committed` | ❌ 回滚失败           | 🔴 SQL7008       |
| **错误配置 B** | `false`    | ✅ 已开启            | `none`           | ❌ 不回滚（浪费日志） | 无（但浪费 I/O） |

**运维检查清单**：

```sql
-- AS400 SQL：查询当前库下所有表的 Journaling 状态
SELECT TABLE_NAME, JOURNAL_NAME, JOURNAL_STATUS
FROM QSYS2.SYSTABLES
WHERE TABLE_SCHEMA = 'RXAS400ADM'
  AND JOURNAL_NAME IS NOT NULL;

-- 预期：tx-enabled=true 的环境，28 张核心表全部出现在结果中
--       tx-enabled=false 的环境，结果为空（或仅有部分表）
```

---
