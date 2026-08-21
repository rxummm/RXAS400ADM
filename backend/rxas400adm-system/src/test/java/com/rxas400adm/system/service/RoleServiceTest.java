package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.dto.SysRoleDTO;
import com.rxas400adm.system.entity.SysRole;
import com.rxas400adm.system.entity.SysRoleMenu;
import com.rxas400adm.system.entity.SysUserRole;
import com.rxas400adm.system.mapper.SysRoleMapper;
import com.rxas400adm.system.mapper.SysRoleMenuMapper;
import com.rxas400adm.system.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private SysRoleMenuMapper roleMenuMapper;
    @Mock
    private SysUserRoleMapper userRoleMapper;

    private RoleService service;

    @BeforeEach
    void setUp() {
        service = new RoleService(roleMapper, roleMenuMapper, userRoleMapper);
    }

    @Test
    @DisplayName("新增角色 → 插入成功")
    void create_shouldInsert() {
        when(roleMapper.insert(any(SysRole.class))).thenReturn(1);

        SysRoleDTO dto = new SysRoleDTO();
        dto.setRoleCode("TEST_ROLE");
        dto.setRoleName("测试角色");
        dto.setSort(1);

        SysRole created = service.create(dto);

        assertEquals("TEST_ROLE", created.getRoleCode());
        assertEquals("测试角色", created.getRoleName());
        verify(roleMapper).insert(any(SysRole.class));
    }

    @Test
    @DisplayName("新增角色 → roleCode 重复则拒绝")
    void create_duplicateCode_shouldThrow() {
        when(roleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        SysRoleDTO dto = new SysRoleDTO();
        dto.setRoleCode("ADMIN");
        dto.setRoleName("重复角色");

        assertThrows(BusinessException.class, () -> service.create(dto));
    }

    @Test
    @DisplayName("删除 ADMIN 角色 → 拒绝")
    void delete_adminRole_shouldThrow() {
        SysRole admin = new SysRole();
        admin.setId(1L);
        admin.setRoleCode("ADMIN");
        when(roleMapper.selectById(1L)).thenReturn(admin);

        assertThrows(BusinessException.class, () -> service.delete(1L));
    }

    @Test
    @DisplayName("删除普通角色 → 清理关联 + 删除")
    void delete_normalRole_shouldCleanup() {
        SysRole role = new SysRole();
        role.setId(2L);
        role.setRoleCode("TEST");
        when(roleMapper.selectById(2L)).thenReturn(role);
        when(userRoleMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(roleMapper.deleteById(2L)).thenReturn(1);

        service.delete(2L);

        verify(roleMenuMapper).deleteByRoleId(2L);
        verify(userRoleMapper).delete(any(LambdaQueryWrapper.class));
        verify(roleMapper).deleteById(2L);
    }

    @Test
    @DisplayName("分页查询 → 委托 Mapper")
    void page_shouldReturnPagedResult() {
        Page<SysRole> page = new Page<>(1, 10);
        SysRole role = new SysRole();
        role.setId(1L);
        role.setRoleCode("TEST");
        page.setRecords(List.of(role));
        page.setTotal(1);
        when(roleMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResult<SysRole> result = service.page(1, 10, "TEST");

        assertEquals(1, result.getTotal());
        assertEquals("TEST", result.getRecords().get(0).getRoleCode());
    }
}
