---
title: "三、数据访问与业务分层（MyBatis-Plus 特性审计）"
---

# 三、数据访问与业务分层（MyBatis-Plus 特性审计）

### 3.1 QueryWrapper 滥用诊断

#### [阻碍理解/Blocker] Service 层 `QueryWrapper` 泛滥

**统计**：项目中 **0 个 Mapper XML 文件**，所有 SQL（包括复杂 CTE 递归）均通过 `@Select` 注解或 `LambdaQueryWrapper` 实现。`LambdaQueryWrapper` 在以下 Service 中出现：

| 文件                                                                                                                                                                 | LambdaQueryWrapper 出现次数 | 问题严重度         |
| -------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------- | ------------------ |
| [MenuService.java](file:///d:/vueprojects/RXAS400ADM/backend/rxas400adm-system/src/main/java/com/rxas400adm/system/service/MenuService.java)                         | 14 次                       | [阻碍理解/Blocker] |
| [SysUserServiceImpl.java](file:///d:/vueprojects/RXAS400ADM/backend/rxas400adm-system/src/main/java/com/rxas400adm/system/service/SysUserServiceImpl.java)           | 8 次                        | [规范警告/Warning] |
| [PermissionManageService.java](file:///d:/vueprojects/RXAS400ADM/backend/rxas400adm-system/src/main/java/com/rxas400adm/system/service/PermissionManageService.java) | 6 次                        | [规范警告/Warning] |
| [RoleService.java](file:///d:/vueprojects/RXAS400ADM/backend/rxas400adm-system/src/main/java/com/rxas400adm/system/service/RoleService.java)                         | 7 次                        | [规范警告/Warning] |
| [DocService.java](file:///d:/vueprojects/RXAS400ADM/backend/rxas400adm-system/src/main/java/com/rxas400adm/system/service/DocService.java)                           | 6 次                        | [规范警告/Warning] |
| [WebhookService.java](file:///d:/vueprojects/RXAS400ADM/backend/rxas400adm-system/src/main/java/com/rxas400adm/system/service/WebhookService.java)                   | 5 次                        | [重构建议/Info]    |

> **AAA-Remark（2026-08-15 复核）**：✅ 问题真实存在——全库 43 个 Java 文件、`QueryWrapper`/`LambdaQueryWrapper` 共 212 处（system 模块 service 层 17 个文件）。⚠️ 上表数字已过时/有误：MenuService 实际 **17** 次（文档 14）、SysUserServiceImpl **9**（文档 8）、RoleService **8**（文档 7）、PermissionManageService **7**（文档 6），且**漏列 `PermissionRequestService` 14 次**（全库第 2 多）。分层准绳 R1 已落地为 `scripts/check-layering.sh` 门禁（新增 Controller 层 QueryWrapper 即失败），存量收敛仍待排期。→ **状态：Controller 层 ✅ 门禁已闭环（check-layering.sh R1）；Service 层存量 ⏳ 未收敛**

**核心问题**：`LambdaQueryWrapper` 本身不是问题——问题在于**是否形成复杂条件拼接**。简单 `eq` 可以接受，但以下场景是重灾区：

```java
// 原晦涩代码（MenuService.java L88-L126）：userMenuData() 方法
// 同一方法中出现 5 个 LambdaQueryWrapper，且夹杂 admin 判断、流式过滤、Map 构建
// 新人需要理解 RBAC 授权模型 + CTE 递归 + admin_only 逻辑 + Tree 构建 + Tab 模型
// 才能定位"为什么某用户看不到某个菜单"
public Map<String, Object> userMenuData(String username) {
    SysUser user = userMapperRef.selectOne(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getUsername, username));
    // ... 30+ 行业务逻辑与 5 个 QueryWrapper 交织 ...
    return Map.of("menus", menuTree, "perms", perms, "tabs", tabList);
}
```

**重构策略**：将可复用的查询条件封装为 Mapper 方法。

```java
// 易读重构：将查询逻辑下沉到 Mapper 接口
// SysMenuMapper.java
@Select("SELECT * FROM rx_menu WHERE status = 1 AND menu_type IN (1,2) ORDER BY sort ASC")
List<SysMenu> selectEnabledMenus();

// MenuService.java 变为
public List<MenuVO> userMenuTree(String username) {
    SysUser user = userService.getByUsername(username);
    List<SysRole> roles = userService.getRolesByUserId(user.getId());
    boolean isAdmin = roles.stream().anyMatch(r -> "ADMIN".equals(r.getRoleCode()));
    List<SysMenu> menus = isAdmin
        ? menuMapper.selectEnabledMenus()
        : menuMapper.selectAuthorizedMenusByUserId(user.getId());
    return toMenuVOList(menus);
}
```

### 3.2 多表联查与单表查询边界审计

#### [规范警告/Warning] `SysMenuMapper.selectAuthorizedMenusByUserId` 的 CTE 递归 SQL 写在 `@Select` 注解中

> **AAA-Remark（2026-08-15 复核）**：✅ 真实存在——`SysMenuMapper.java` L19 起 `@Select` 内嵌 `WITH RECURSIVE` CTE，项目 Mapper XML 确实为 0 个。⚠️ 迁移到 XML 需启用 MyBatis-Plus 的 `mapper-locations`（当前未配置）并补充 resultMap；属低风险重构，可排期（§7 Action Item 3）。

**文件**：[SysMenuMapper.java](file:///d:/vueprojects/RXAS400ADM/backend/rxas400adm-system/src/main/java/com/rxas400adm/system/mapper/SysMenuMapper.java#L19-L34)

```java
// 原代码：CTE 递归 SQL 直接写在注解中，无缩进高亮，无法格式化
@Select("<script>" +
    "WITH RECURSIVE menu_tree AS (" +
    "  SELECT m.* FROM rx_menu m " +
    "  JOIN rx_role_menu rm ON m.id = rm.menu_id " +
    "  JOIN rx_user_role ur ON rm.role_id = ur.role_id " +
    "  WHERE ur.user_id = #{userId} AND m.status = 1 " +
    "  UNION " +
    "  SELECT m.* FROM rx_menu m " +
    "  JOIN rx_user_menu um ON m.id = um.menu_id " +
    "  WHERE um.user_id = #{userId} AND m.status = 1 " +
    "  UNION " +
    "  SELECT p.* FROM rx_menu p " +
    "  JOIN menu_tree mt ON p.id = mt.parent_id " +
    "  WHERE p.status = 1" +
    ") SELECT DISTINCT * FROM menu_tree ORDER BY sort ASC" +
    "</script>")
List<SysMenu> selectAuthorizedMenusByUserId(@Param("userId") Long userId);
```

**影响**：新人调试 SQL 时需要从字符串拼接还原 SQL，无法使用 IDE 的 SQL 格式化/语法高亮，也无法直接复制到数据库工具测试。

**重构建议**：创建 `SysMenuMapper.xml` 文件。

```xml
<!-- 易读重构：SysMenuMapper.xml -->
<select id="selectAuthorizedMenusByUserId" resultMap="BaseResultMap">
    WITH RECURSIVE menu_tree AS (
        SELECT m.* FROM rx_menu m
        JOIN rx_role_menu rm ON m.id = rm.menu_id
        JOIN rx_user_role ur ON rm.role_id = ur.role_id
        WHERE ur.user_id = #{userId} AND m.status = 1
        UNION
        SELECT m.* FROM rx_menu m
        JOIN rx_user_menu um ON m.id = um.menu_id
        WHERE um.user_id = #{userId} AND m.status = 1
        UNION
        SELECT p.* FROM rx_menu p
        JOIN menu_tree mt ON p.id = mt.parent_id
        WHERE p.status = 1
    )
    SELECT DISTINCT * FROM menu_tree ORDER BY sort ASC
</select>
```

---
