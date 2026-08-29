package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.dto.SysPermissionDTO;
import com.rxas400adm.system.entity.SysMenu;
import com.rxas400adm.system.entity.SysPermission;
import com.rxas400adm.system.entity.SysRolePermission;
import com.rxas400adm.system.mapper.SysMenuMapper;
import com.rxas400adm.system.mapper.SysPermissionMapper;
import com.rxas400adm.system.mapper.SysRolePermissionMapper;
import com.rxas400adm.system.vo.PermissionVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionManageServiceTest {

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, SysPermission.class);
        TableInfoHelper.initTableInfo(assistant, SysMenu.class);
        TableInfoHelper.initTableInfo(assistant, SysRolePermission.class);
    }

    @Mock private SysPermissionMapper permissionMapper;
    @Mock private SysMenuMapper menuMapper;
    @Mock private SysRolePermissionMapper rolePermissionMapper;
    @Mock private ISysConfigService configService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private PermissionManageService service;

    @BeforeEach
    void setUp() {
        service = new PermissionManageService(permissionMapper, menuMapper, rolePermissionMapper, configService, objectMapper);
    }

    @Test
    @DisplayName("deriveModule → 正常提取模块前缀")
    void deriveModule_shouldExtractPrefix() {
        assertEquals("JOB", PermissionManageService.deriveModule("JOB_VIEW"));
        assertEquals("SYS_CONFIG", PermissionManageService.deriveModule("SYS_CONFIG_MANAGE"));
        assertEquals("SYSTEM", PermissionManageService.deriveModule("SINGLE"));
        assertEquals("SYSTEM", PermissionManageService.deriveModule(null));
    }

    @Test
    @DisplayName("create → 空 code 抛异常")
    void create_emptyCode_shouldThrow() {
        SysPermissionDTO dto = new SysPermissionDTO();
        dto.setPermissionCode("  ");
        assertThrows(BusinessException.class, () -> service.create(dto));
    }

    @Test
    @DisplayName("create → code 重复抛异常")
    void create_duplicateCode_shouldThrow() {
        when(permissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        SysPermissionDTO dto = new SysPermissionDTO();
        dto.setPermissionCode("JOB_VIEW");
        assertThrows(BusinessException.class, () -> service.create(dto));
    }

    @Test
    @DisplayName("create → 正常新增")
    void create_valid_shouldInsert() {
        when(permissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(permissionMapper.insert(any(SysPermission.class))).thenAnswer(inv -> {
            inv.getArgument(0, SysPermission.class).setId(1L);
            return 1;
        });

        SysPermissionDTO dto = new SysPermissionDTO();
        dto.setPermissionCode("JOB_VIEW");
        dto.setPermissionName("查看作业");
        dto.setDescription("作业查看权限");

        SysPermission result = service.create(dto);
        assertEquals("JOB_VIEW", result.getPermissionCode());
        assertEquals("JOB", result.getModule());
    }

    @Test
    @DisplayName("delete → 被菜单引用则拒绝")
    void delete_usedByMenu_shouldThrow() {
        SysPermission p = new SysPermission();
        p.setId(1L);
        p.setPermissionCode("JOB_VIEW");
        when(permissionMapper.selectById(1L)).thenReturn(p);
        when(menuMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(rolePermissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        assertThrows(BusinessException.class, () -> service.delete(1L));
    }

    @Test
    @DisplayName("delete → 未被引用则正常删除")
    void delete_notUsed_shouldDelete() {
        SysPermission p = new SysPermission();
        p.setId(1L);
        p.setPermissionCode("UNUSED_CODE");
        when(permissionMapper.selectById(1L)).thenReturn(p);
        when(menuMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(rolePermissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        service.delete(1L);
        verify(permissionMapper).deleteById(1L);
    }

    @Test
    @DisplayName("page → 分页查询")
    void page_shouldReturn() {
        Page<SysPermission> page = new Page<>(1, 10);
        page.setTotal(0);
        page.setRecords(List.of());
        when(permissionMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(rolePermissionMapper.selectList(isNull())).thenReturn(List.of());

        PageResult<PermissionVO> result = service.page(null, null, 1, 10);
        assertNotNull(result);
    }
}