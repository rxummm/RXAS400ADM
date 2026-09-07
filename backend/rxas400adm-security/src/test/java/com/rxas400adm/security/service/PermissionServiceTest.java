package com.rxas400adm.security.service;

import com.rxas400adm.system.entity.SysUser;
import com.rxas400adm.system.mapper.SysRoleMapper;
import com.rxas400adm.system.mapper.SysUserRoleMapper;
import com.rxas400adm.system.service.IMenuService;
import com.rxas400adm.system.service.SysUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private SysUserService userService;

    @Mock
    private IMenuService menuService;

    @Mock
    private SysUserRoleMapper userRoleMapper;

    @Mock
    private SysRoleMapper roleMapper;

    private PermissionService service;

    @BeforeEach
    void setUp() {
        service = new PermissionService(userService, menuService, userRoleMapper, roleMapper);
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setStatus("ACTIVE");
        org.mockito.Mockito.lenient().when(userService.getByUsername("admin")).thenReturn(user);
        org.mockito.Mockito.lenient().when(userService.listPermissions(1L)).thenReturn(List.of("JOB_VIEW", "USER_MANAGE"));
        // 菜单权限合并（页面/按钮 perms）：admin 默认无（菜单未授权时返回空）
        org.mockito.Mockito.lenient().when(menuService.userMenuPerms("admin")).thenReturn(List.of());
    }

    @Test
    void loadPermissions_shouldCacheSecondCall() {
        assertEquals(2, service.loadPermissions("admin").size());
        assertEquals(2, service.loadPermissions("admin").size());
        verify(userService, times(1)).listPermissions(1L);
    }

    @Test
    void refresh_shouldReloadFromDb() {
        service.loadPermissions("admin");
        service.refresh("admin");
        verify(userService, times(2)).listPermissions(1L);
    }

    @Test
    void loadPermissions_unknownUser_shouldReturnEmpty() {
        when(userService.getByUsername("ghost")).thenReturn(null);
        assertEquals(0, service.loadPermissions("ghost").size());
    }

    @Test
    void loadPermissions_disabledUser_shouldReturnEmpty() {
        SysUser disabled = new SysUser();
        disabled.setId(2L);
        disabled.setUsername("disabled");
        disabled.setStatus("DISABLED");
        when(userService.getByUsername("disabled")).thenReturn(disabled);

        // S1：禁用用户不授予任何权限码（即便有角色绑定）
        assertEquals(0, service.loadPermissions("disabled").size());
        verify(userService, org.mockito.Mockito.never()).listPermissions(2L);
    }

    @Test
    void loadPermissions_shouldMergeMenuPerms() {
        org.mockito.Mockito.lenient().when(menuService.userMenuPerms("admin"))
                .thenReturn(List.of("SYS_PERMISSION_REQUEST"));
        assertEquals(3, service.loadPermissions("admin").size());
        org.junit.jupiter.api.Assertions.assertTrue(service.loadPermissions("admin").contains("SYS_PERMISSION_REQUEST"));
    }
}
