package com.rxas400adm.system.service;

import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.system.entity.SysMenu;
import com.rxas400adm.system.entity.SysRole;
import com.rxas400adm.system.entity.SysUser;
import com.rxas400adm.system.entity.SysUserMenu;
import com.rxas400adm.system.entity.SysUserRole;
import com.rxas400adm.system.mapper.SysMenuMapper;
import com.rxas400adm.system.mapper.SysRoleMapper;
import com.rxas400adm.system.mapper.SysRoleMenuMapper;
import com.rxas400adm.system.mapper.SysUserMapper;
import com.rxas400adm.system.mapper.SysUserMenuMapper;
import com.rxas400adm.system.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户菜单直接授权服务单测（纯 Mockito）：
 * 覆盖授权合并（角色∪直接）、ADMIN 短路、可分配树裁剪、追加幂等、移除级联子孙、替换模式。
 */
@ExtendWith(MockitoExtension.class)
class UserMenuServiceTest {

    @Mock private SysUserMenuMapper userMenuMapper;
    @Mock private SysMenuMapper menuMapper;
    @Mock private SysUserRoleMapper userRoleMapper;
    @Mock private SysRoleMapper roleMapper;
    @Mock private SysUserMapper userMapper;
    @Mock private SysRoleMenuMapper roleMenuMapper;

    private UserMenuService service;

    @BeforeEach
    void setUp() {
        service = new UserMenuService(userMenuMapper, menuMapper, userRoleMapper,
                roleMapper, userMapper, roleMenuMapper);
    }

    private SysMenu menu(long id, long parentId, int type) {
        SysMenu m = new SysMenu();
        m.setId(id);
        m.setParentId(parentId);
        m.setMenuType(type);
        m.setStatus(1);
        m.setSort(1);
        return m;
    }

    private void givenNonAdminContext(long userId) {
        SysUserRole ur = new SysUserRole();
        ur.setUserId(userId);
        ur.setRoleId(7L);
        when(userRoleMapper.selectList(any())).thenReturn(List.of(ur));
        SysRole role = new SysRole();
        role.setId(7L);
        role.setRoleCode("USER");
        when(roleMapper.selectBatchIds(any())).thenReturn(List.of(role));
    }

    // ---------------- getUserMenuIds ----------------

    @Test
    @DisplayName("getUserMenuIds → 直接授权 ∪ 角色授权")
    void getUserMenuIds_mergesDirectAndRoleGrants() {
        when(userMenuMapper.selectMenuIdsByUserId(7L)).thenReturn(List.of(5L));
        givenNonAdminContext(7L);
        when(roleMenuMapper.selectMenuIdsByRoleIds(any())).thenReturn(List.of(6L));
        SysMenu m6 = menu(6L, 0L, 2);
        when(menuMapper.selectBatchIds(any())).thenReturn(List.of(m6));

        var ids = service.getUserMenuIds(7L);

        assertTrue(ids.contains(5L));
        assertTrue(ids.contains(6L));
        assertEquals(2, ids.size());
    }

    @Test
    @DisplayName("getUserMenuIds → ADMIN 短路返回全部启用菜单")
    void getUserMenuIds_adminShortCircuitsToAllEnabled() {
        when(userMenuMapper.selectMenuIdsByUserId(1L)).thenReturn(List.of());
        SysUserRole ur = new SysUserRole();
        ur.setUserId(1L);
        ur.setRoleId(9L);
        when(userRoleMapper.selectList(any())).thenReturn(List.of(ur));
        SysRole admin = new SysRole();
        admin.setId(9L);
        admin.setRoleCode("ADMIN");
        when(roleMapper.selectBatchIds(any())).thenReturn(List.of(admin));
        when(menuMapper.selectList(any())).thenReturn(List.of(menu(100L, 0L, 2)));

        var ids = service.getUserMenuIds(1L);

        assertEquals(java.util.Set.of(100L), ids);
        verify(roleMenuMapper, never()).selectMenuIdsByRoleIds(any());
    }

    // ---------------- getManageableMenuTree ----------------

    @Test
    @DisplayName("可分配树 → 排除 admin_only 子孙/申请入口子孙/已拥有页，保留目录与按钮占位")
    void manageableTree_filtersExclusions() {
        SysMenu d1 = menu(10L, 0L, 1);
        SysMenu p11 = menu(11L, 10L, 2);
        SysMenu p12 = menu(12L, 10L, 2);
        SysMenu b13 = menu(13L, 10L, 3);
        SysMenu a20 = menu(20L, 0L, 1);
        a20.setAdminOnly(1);
        SysMenu b21 = menu(21L, 20L, 3);
        SysMenu pr = menu(30L, 0L, 2);
        pr.setTitle("permissionRequest");
        SysMenu x31 = menu(31L, 30L, 3);
        List<SysMenu> all = List.of(d1, p11, p12, b13, a20, b21, pr, x31);

        when(userMenuMapper.selectMenuIdsByUserId(7L)).thenReturn(List.of(12L));
        givenNonAdminContext(7L);
        when(roleMenuMapper.selectMenuIdsByRoleIds(any())).thenReturn(List.of(12L));
        when(menuMapper.selectBatchIds(any())).thenReturn(List.of(p12));
        // 第一次调用：全量启用菜单；第二次：admin_only 菜单（doReturn 链避免 varargs 泛型数组警告）
        doReturn(all).doReturn(List.of(a20)).when(menuMapper).selectList(any());

        var tree = service.getManageableMenuTree(7L);

        // 现状语义（测试固化）：排除的是 admin_only/入口的「子孙」，节点自身作为目录/页面占位保留
        assertEquals(3, tree.size());
        assertEquals(10L, tree.get(0).getId());
        var childIds = tree.get(0).getChildren().stream().map(SysMenu::getId).toList();
        assertEquals(List.of(11L, 13L), childIds);
        // A20 的按钮 21、PR 的按钮 31 已被级联排除
        assertTrue(tree.get(1).getChildren() == null || tree.get(1).getChildren().isEmpty());
        assertTrue(tree.get(2).getChildren() == null || tree.get(2).getChildren().isEmpty());
    }

    // ---------------- addUserMenus / removeUserMenus ----------------

    @Test
    @DisplayName("addUserMenus → 批量幂等：已存在授权不重复插入；剩余一次 insertBatch")
    void addUserMenus_batchInsertSkipsGranted() {
        when(userMapper.selectById(7L)).thenReturn(new SysUser());
        when(menuMapper.selectBatchIds(any())).thenReturn(List.of(menu(11L, 0L, 2), menu(12L, 0L, 3)));
        SysUserMenu granted = new SysUserMenu();
        granted.setUserId(7L);
        granted.setMenuId(11L);
        when(userMenuMapper.selectList(any())).thenReturn(List.of(granted));

        service.addUserMenus(7L, List.of(11L, 12L, 11L));

        verify(userMenuMapper).insertBatch(argThat(list ->
                list != null && list.size() == 1
                        && Long.valueOf(12L).equals(list.get(0).getMenuId())
                        && Long.valueOf(7L).equals(list.get(0).getUserId())));
    }

    @Test
    @DisplayName("addUserMenus → 不存在的菜单抛 BAD_REQUEST 且不写入")
    void addUserMenus_missingMenuThrowsBadRequest() {
        when(userMapper.selectById(7L)).thenReturn(new SysUser());
        when(menuMapper.selectBatchIds(any())).thenReturn(List.of(menu(11L, 0L, 2)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.addUserMenus(7L, List.of(11L, 99L)));

        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
        verify(userMenuMapper, never()).insertBatch(any());
    }

    @Test
    @DisplayName("removeUserMenus → 目录级联删除子孙；按钮仅删自身；一次 IN 批量删除")
    void removeUserMenus_cascadesDirectoryDescendants() {
        when(userMapper.selectById(7L)).thenReturn(new SysUser());
        SysMenu dir = menu(10L, 0L, 1);
        SysMenu page = menu(11L, 10L, 2);
        SysMenu btn = menu(12L, 10L, 3);
        SysMenu btnDirect = menu(13L, 0L, 3);
        when(menuMapper.selectList(any())).thenReturn(List.of(dir, page, btn, btnDirect));

        service.removeUserMenus(7L, List.of(10L, 13L));

        verify(userMenuMapper).deleteByUserIdAndMenuIds(eq(7L), argThat(ids -> ids != null
                && List.of(10L, 11L, 12L, 13L).equals(ids.stream().sorted().toList())));
    }

    @Test
    @DisplayName("操作不存在用户 → NOT_FOUND")
    void operations_unknownUserThrowNotFound() {
        when(userMapper.selectById(404L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.addUserMenus(404L, List.of(1L)));
        assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
    }
}
