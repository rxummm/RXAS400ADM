# Phase 0-2 编码规范违规记录

## 概述

在实现 Phase 0-2（操作驱动架构脚手架 + 执行器集成）过程中，发现并修复了以下编码规范违规项。

## 已修复的违规项

### 1. Controller 使用 VO 返回类型（规则 2.1.3）

**违规代码**（OperationController.java）：
```java
// ❌ 违规：Controller 返回 Entity
@OperateLog(module = "OPERATION", operation = "Get operation detail")
public ApiResponse<Operation> getOperation(@PathVariable Long id)
```

**修复后**：
```java
// ✅ 合规：Controller 返回 VO
@OperateLog(module = "OPERATION", operation = "Get operation detail")
public ApiResponse<OperationVO> getOperation(@PathVariable Long id)
```

**说明**：创建了 `OperationVO` record 类，使用 `OperationVO.from()` 静态方法转换 Entity → VO。禁止直接暴露 Entity 给前端。

---

### 2. Controller 使用 FQN 注解（规则 4.2）

**违规代码**（OperationController.java）：
```java
// ❌ 违规：使用全限定类名注解
@com.rxas400adm.common.annotation.OperateLog(module = "OPERATION", operation = "Create operation")
```

**修复后**：
```java
// ✅ 合规：顶部 import + 简名引用
import com.rxas400adm.common.annotation.OperateLog;
@OperateLog(module = "OPERATION", operation = "Create operation")
```

**说明**：Java 禁止内联全限定类名，一律顶部 import。

---

### 3. 使用 EntityUtil.require()（规则 10.3）

**违规代码**（OperationMapperImpl.java）：
```java
// ❌ 违规：手写 null 检查
Operation op = operationMapper.selectById(id);
if (op == null) {
    throw new RxBusinessException(ErrorCode.OPERATION_NOT_FOUND);
}
```

**修复后**：
```java
// ✅ 合规：使用 EntityUtil.require()
Operation op = EntityUtil.require(operationMapper.selectById(id), ErrorCode.OPERATION_NOT_FOUND);
```

**说明**：`EntityUtil.require()` 是项目统一的 null 检查工具方法，必须优先使用。

---

### 4. 实体类使用 @Data 注解（规则 6.4）

**违规代码**（Operation.java, OperationStep.java）：
```java
// ❌ 违规：实体类使用 @Data，会生成 toString() 泄漏敏感信息
@Data
@TableName("rx_operation")
public class Operation {
```

**修复后**：
```java
// ✅ 合规：使用 @Getter @Setter 替代 @Data
@Getter
@Setter
@TableName("rx_operation")
public class Operation {
```

**说明**：CODING_STANDARDS 6.4 禁止实体类使用 `@Data`，因为 `toString()` 可能暴露敏感信息（如密码、token）。必须使用 `@Getter @Setter` 替代。

---

## 未修复的遗留项

### 1. 风险等级常量未提取（规则 6.5）

**现状**：`RiskLevel` 枚举类定义在 `domain` 包，但 `UserPolicy`、`RiskPolicy` 等类直接引用 `RiskLevel.CRITICAL`。

**评估**：符合规范，无需修复。`RiskLevel` 是域枚举，不是魔法数字。

---

### ~~2. Operation 类未使用 @Data @Builder（规则 6.4）~~ ✅ 已修复

**现状**：`Operation` 类已修复为使用 `@Getter @Setter`，符合规范。

---

## 验证结果

- **编译**：`mvn -q -DskipTests compile` ✅
- **测试**：`mvn test` — 494 个测试全部通过 ✅
- **编码规范检查**：
  - `@Transactional` 零容忍 ✅（operation 模块 0 处）
  - 分层准绳 ✅（Controller 无 Mapper 注入、无 QueryWrapper）
  - FQN 内联 ✅（operation 模块 0 处）
  - 实体类 @Data ✅（已修复为 @Getter @Setter）
  - OperationVO 返回类型 ✅（Controller 返回 VO 而非 Entity）
  - SpoolRow record 类型 ✅（使用 record 字段访问器）

---

## 结论

Phase 0-2 实现已完全符合 CODING_STANDARDS.md 要求。所有违规项已在提交前修复。
