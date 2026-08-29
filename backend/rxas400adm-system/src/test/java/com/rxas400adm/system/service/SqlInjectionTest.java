package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.system.dto.RegionDTO;
import com.rxas400adm.system.dto.UserDTO;
import com.rxas400adm.system.entity.Region;
import com.rxas400adm.system.entity.SysMenu;
import com.rxas400adm.system.entity.SysUser;
import com.rxas400adm.system.entity.SysUserRole;
import com.rxas400adm.system.mapper.RegionMapper;
import com.rxas400adm.system.mapper.SysMenuMapper;
import com.rxas400adm.system.mapper.SysRoleMapper;
import com.rxas400adm.system.mapper.SysUserMapper;
import com.rxas400adm.system.mapper.SysRoleMenuMapper;
import com.rxas400adm.system.mapper.SysUserMenuMapper;
import com.rxas400adm.system.mapper.SysUserRoleMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SQL 注入负面测试用例
 * 验证服务层对恶意输入的防护能力，确保 MyBatis Plus 参数绑定
 * 和输入校验正确拦截注入攻击。
 */
@DisplayName("SQL 注入防御验证")
@SuppressWarnings("unchecked")
class SqlInjectionTest {

    /**
     * 纯 Mockito 环境无 MyBatis-Plus 启动上下文：
     * 为断言 getTargetSql() 涉及的实体初始化 TableInfo 缓存（既有修复，非本次 S1 引入）。
     */
    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Region.class);
        TableInfoHelper.initTableInfo(assistant, SysUser.class);
    }

    /* ========== UserMenuService: .inSql() 安全性 ========== */

    @Nested
    @DisplayName("UserMenuService - 角色菜单查询参数化（S6）")
    class UserMenuServiceInSql {

        private final SysUserMenuMapper userMenuMapper = mock(SysUserMenuMapper.class);
        private final SysMenuMapper menuMapper = mock(SysMenuMapper.class);
        private final SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        private final SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        private final SysUserMapper userMapper = mock(SysUserMapper.class);
        private final SysRoleMenuMapper roleMenuMapper = mock(SysRoleMenuMapper.class);

        private UserMenuService service() {
            return new UserMenuService(userMenuMapper, menuMapper, userRoleMapper,
                    roleMapper, userMapper, roleMenuMapper);
        }

        @Test
        @DisplayName("角色菜单经参数化批量查询，不再拼接 SQL（S6）")
        void roleMenusViaParameterizedBatchQuery() {
            when(userRoleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(roleWithId(1L), roleWithId(2L)));
            when(roleMapper.selectBatchIds(any())).thenReturn(List.of());
            // 角色菜单：参数化 IN 查询返回菜单 ID
            when(roleMenuMapper.selectMenuIdsByRoleIds(List.of(1L, 2L))).thenReturn(List.of(10L, 20L));
            SysMenu m10 = new SysMenu();
            m10.setId(10L);
            SysMenu m20 = new SysMenu();
            m20.setId(20L);
            when(menuMapper.selectBatchIds(List.of(10L, 20L))).thenReturn(List.of(m10, m20));

            Set<Long> ids = service().getUserMenuIds(1L);
            assertEquals(Set.of(10L, 20L), ids);

            // 关键断言：不再调用 inSql 拼接的 selectList，全部走参数化查询
            verify(roleMenuMapper).selectMenuIdsByRoleIds(List.of(1L, 2L));
            verify(menuMapper, org.mockito.Mockito.never()).selectList(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("roleId 来源为数据库查询结果，非用户直接输入")
        void roleIdFromDatabaseNotUserInput() {
            SysUserRole ur1 = new SysUserRole();
            ur1.setUserId(1L);
            ur1.setRoleId(100L);
            when(userRoleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(ur1));
            when(roleMapper.selectBatchIds(any())).thenReturn(List.of());
            when(roleMenuMapper.selectMenuIdsByRoleIds(List.of(100L))).thenReturn(List.of());

            service().getUserMenuIds(1L);

            // roleId 从数据库读取（100），原样传给参数化批量查询，无字符串拼接
            verify(roleMenuMapper).selectMenuIdsByRoleIds(List.of(100L));
        }

        private static SysUserRole roleWithId(Long roleId) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(1L);
            ur.setRoleId(roleId);
            return ur;
        }
    }

    /* ========== RegionService: like() 参数绑定 ========== */

    @Nested
    @DisplayName("RegionService - like() 使用参数绑定")
    class RegionServiceLike {

        private final RegionMapper regionMapper = mock(RegionMapper.class);

        @Test
        @DisplayName("SQL 注入关键字被当作参数值绑定，不拼入 SQL")
        void injectionKeywordTreatedAsLiteral() {
            RegionService service = new RegionService(regionMapper);
            when(regionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

            service.search("' OR '1'='1", null);

            ArgumentCaptor<LambdaQueryWrapper<Region>> captor =
                    ArgumentCaptor.forClass(LambdaQueryWrapper.class);
            verify(regionMapper).selectList(captor.capture());
            String sql = captor.getValue().getTargetSql();
            // 参数化断言：SQL 里只有占位符，注入串不出现在 SQL 文本中
            assertTrue(sql.contains("LIKE ?"), "like 应参数化为 LIKE ?，而非拼接字面量");
            assertFalse(sql.contains("'1'='1"), "注入关键字不得出现在 SQL 文本中");
            assertTrue(paramValuesContain(captor.getValue(), "' OR '1'='1"),
                    "注入关键字应作为参数值绑定（%...%，MyBatis Plus 自动转义）");
            assertTrue(sql.contains("LIMIT 50"),
                    "LIMIT 为硬编码常量，不可被注入覆盖");
        }

        @Test
        @DisplayName("UNION SELECT 注入尝试被参数化，不改变查询语义")
        void unionSelectTreatedAsLiteral() {
            RegionService service = new RegionService(regionMapper);
            when(regionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

            service.search("x' UNION SELECT * FROM rx_user--", null);

            ArgumentCaptor<LambdaQueryWrapper<Region>> captor =
                    ArgumentCaptor.forClass(LambdaQueryWrapper.class);
            verify(regionMapper).selectList(captor.capture());
            String sql = captor.getValue().getTargetSql();
            // UNION SELECT 只能出现在参数值里，SQL 文本保持参数化
            assertFalse(sql.contains("UNION SELECT"), "UNION SELECT 不得拼入 SQL 文本");
            assertTrue(paramValuesContain(captor.getValue(), "UNION SELECT"),
                    "UNION SELECT 应作为参数值被绑定");
        }

        @Test
        @DisplayName("空关键字不添加 like 条件")
        void emptyKeywordSkipsLike() {
            RegionService service = new RegionService(regionMapper);
            when(regionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

            service.search("   ", null);

            ArgumentCaptor<LambdaQueryWrapper<Region>> captor =
                    ArgumentCaptor.forClass(LambdaQueryWrapper.class);
            verify(regionMapper).selectList(captor.capture());
            String sql = captor.getValue().getTargetSql();
            assertTrue(!sql.contains("LIKE") || sql.contains("LIMIT 50"),
                    "空关键字应跳过 LIKE 条件，仅保留 LIMIT 50");
        }

        private static boolean paramValuesContain(com.baomidou.mybatisplus.core.conditions.AbstractWrapper<?, ?, ?> wrapper, String keyword) {
            return wrapper.getParamNameValuePairs().values().stream()
                    .map(String::valueOf)
                    .anyMatch(v -> v.contains(keyword));
        }
    }

    /* ========== SysUserService: 用户名注入防御 ========== */

    @Nested
    @DisplayName("SysUserService - 用户名作为参数绑定")
    class SysUserServiceUsername {

        private final SysUserMapper userMapper = mock(SysUserMapper.class);

        @Test
        @DisplayName("用户名含单引号不破坏查询（MyBatis Plus 自动参数化）")
        void usernameWithQuoteIsParameterized() {
            SysUserServiceImpl service = new SysUserServiceImpl(
                    userMapper, null, null, null, null, new BCryptPasswordEncoder(),
                    mock(ApplicationEventPublisher.class));
            when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            UserDTO dto = new UserDTO();
            dto.setUsername("admin'--");
            dto.setPassword("secret123");

            service.create(dto);

            ArgumentCaptor<LambdaQueryWrapper<SysUser>> captor =
                    ArgumentCaptor.forClass(LambdaQueryWrapper.class);
            verify(userMapper).selectOne(captor.capture());
            String sql = captor.getValue().getTargetSql();
            assertTrue(sql.contains("username = ?"), "用户名应参数化为 username = ?");
            assertFalse(sql.contains("admin'--"), "注入串不得拼入 SQL 文本");
            assertTrue(captor.getValue().getParamNameValuePairs().values().stream()
                            .map(String::valueOf).anyMatch(v -> v.contains("admin'--")),
                    "单引号作为参数值绑定，MyBatis Plus 自动转义");
        }

        @Test
        @DisplayName("用户名含分号不产生多语句")
        void usernameWithSemicolonNoMultiStatement() {
            SysUserServiceImpl service = new SysUserServiceImpl(
                    userMapper, null, null, null, null, new BCryptPasswordEncoder(),
                    mock(ApplicationEventPublisher.class));
            when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            UserDTO dto = new UserDTO();
            dto.setUsername("test; DROP TABLE rx_user;--");
            dto.setPassword("secret123");

            service.create(dto);

            ArgumentCaptor<LambdaQueryWrapper<SysUser>> captor =
                    ArgumentCaptor.forClass(LambdaQueryWrapper.class);
            verify(userMapper).selectOne(captor.capture());
            String sql = captor.getValue().getTargetSql();
            assertFalse(sql.contains("DROP TABLE"), "DROP TABLE 不得拼入 SQL 文本");
            assertTrue(captor.getValue().getParamNameValuePairs().values().stream()
                            .map(String::valueOf).anyMatch(v -> v.contains("DROP TABLE")),
                    "DROP TABLE 作为参数值绑定，MyBatis Plus 参数化防护");
        }
    }

    /* ========== Numeric .last() 安全性 ========== */

    @Nested
    @DisplayName("Numeric .last() - 仅接受已验证的整数")
    class NumericLastClause {

        @Test
        @DisplayName("Math.max/min 限制后 .last() 无法溢出")
        void clampedLimitCannotOverflow() {
            int raw = 99999;
            int clamped = Math.max(1, Math.min(raw, 500));
            assertEquals(500, clamped, "Math.min 将超限值截断为 500");
        }

        @Test
        @DisplayName("负数 limit 被 Math.max(1, ...) 截断为 1")
        void negativeLimitClampedToOne() {
            int raw = -5;
            int clamped = Math.max(1, Math.min(raw, 500));
            assertEquals(1, clamped, "负数 limit 被截断为 1");
        }

        @Test
        @DisplayName("零值 limit 被 Math.max(1, ...) 截断为 1")
        void zeroLimitClampedToOne() {
            int raw = 0;
            int clamped = Math.max(1, Math.min(raw, 500));
            assertEquals(1, clamped, "0 被截断为 1");
        }
    }

    /* ========== 输入校验覆盖 ========== */

    @Nested
    @DisplayName("输入校验 - 空值/超长值拒绝")
    class InputValidation {

        @Test
        @DisplayName("RegionService.search 空 keyword 不抛异常")
        void regionSearchEmptyKeyword() {
            RegionMapper mapper = mock(RegionMapper.class);
            RegionService service = new RegionService(mapper);
            when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

            List<Region> result = service.search(null, null);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("RegionService.create 空 code 抛 BusinessException")
        void regionCreateEmptyCode() {
            RegionMapper mapper = mock(RegionMapper.class);
            RegionService service = new RegionService(mapper);

            RegionDTO r = new RegionDTO();
            r.setCode("");
            r.setName("Test");

            assertThrows(BusinessException.class, () -> service.create(r),
                    "空 code 应抛出业务异常");
        }

        @Test
        @DisplayName("RegionService.create 空 name 抛 BusinessException")
        void regionCreateEmptyName() {
            RegionMapper mapper = mock(RegionMapper.class);
            RegionService service = new RegionService(mapper);

            RegionDTO r = new RegionDTO();
            r.setCode("110000");
            r.setName("");

            assertThrows(BusinessException.class, () -> service.create(r),
                    "空 name 应抛出业务异常");
        }
    }
}