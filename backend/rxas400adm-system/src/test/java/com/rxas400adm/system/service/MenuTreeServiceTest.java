package com.rxas400adm.system.service;

import com.rxas400adm.system.entity.SysMenu;
import com.rxas400adm.system.entity.SysRole;
import com.rxas400adm.system.entity.SysUser;
import com.rxas400adm.system.entity.SysUserRole;
import com.rxas400adm.system.mapper.SysMenuMapper;
import com.rxas400adm.system.mapper.SysRoleMapper;
import com.rxas400adm.system.mapper.SysUserMapper;
import com.rxas400adm.system.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 菜单树构建与授权裁剪服务单测（纯 Mockito）：
 * 覆盖树构建、ADMIN/普通用户菜单下发、可申请树裁剪（admin_only 子树/申请入口/已拥有按钮）、
 * 权限码合并去重、用户上下文缓存与失效。
 */
@ExtendWith(MockitoExtension.class)
class MenuTreeServiceTest {

    @Mock private SysMenuMapper menuMapper;
    @Mock private SysRoleMapper roleMapper;
    @Mock private SysUserRoleMapper userRoleMapper;
    @Mock private SysUserMapper userMapper;

    private MenuTreeService service;

    @BeforeEach
    void setUp() {
        service = new MenuTreeService(menuMapper, roleMapper, userRoleMapper, userMapper);
    }

    private SysMenu menu(long id, long parentId, int type, String title, String perms) {
        SysMenu m = new SysMenu();
        m.setId(id);
        m.setParentId(parentId);
        m.setMenuType(type);
        m.setStatus(1);
        m.setSort(1);
        m.setTitle(title);
        m.setMenuName(title);
        m.setPerms(perms);
        return m;
    }

    /** 打桩用户上下文：alice + USER 角色（非 ADMIN） */
    private void givenUser(String username, long userId) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setUsername(username);
        when(userMapper.selectOne(any())).thenReturn(user);
        SysUserRole ur = new SysUserRole();
        ur.setUserId(userId);
        ur.setRoleId(7L);
        when(userRoleMapper.selectList(any())).thenReturn(List.of(ur));
        SysRole role = new SysRole();
        role.setId(7L);
        role.setRoleCode("USER");
        when(roleMapper.selectBatchIds(any())).thenReturn(List.of(role));
    }

    @Test
    @DisplayName("tree → 平铺列表构建嵌套树")
    void tree_buildsNestedStructure() {
        SysMenu dir = menu(1L, 0L, 1, "系统管理", null);
        SysMenu tab = menu(2L, 1L, 2, "用户", null);
        when(menuMapper.selectList(any())).thenReturn(List.of(dir, tab));

        List<SysMenu> roots = service.tree();

        assertEquals(1, roots.size());
        assertEquals(1L, roots.get(0).getId());
        assertEquals(2L, roots.get(0).getChildren().get(0).getId());
    }

    @Test
    @DisplayName("userMenuTree → ADMIN 返回全部启用目录/菜单；未知用户返回空")
    void userMenuTree_adminAndUnknown() {
        SysUser admin = new SysUser();
        admin.setId(1L);
        admin.setUsername("admin");
        when(userMapper.selectOne(any())).thenReturn(admin);
        SysUserRole ur = new SysUserRole();
        ur.setUserId(1L);
        ur.setRoleId(9L);
        when(userRoleMapper.selectList(any())).thenReturn(List.of(ur));
        SysRole role = new SysRole();
        role.setId(9L);
        role.setRoleCode("ADMIN");
        when(roleMapper.selectBatchIds(any())).thenReturn(List.of(role));
        when(menuMapper.selectList(any()))
                .thenReturn(List.of(menu(1L, 0L, 1, "dir", null), menu(2L, 1L, 2, "tab", null)));

        var vos = service.userMenuTree("admin");
        assertEquals(1, vos.size());
        assertEquals("dir", vos.get(0).title());
        assertEquals("tab", vos.get(0).children().get(0).title());

        when(userMapper.selectOne(any())).thenReturn(null);
        assertEquals(0, service.userMenuTree("ghost").size());
    }

    @Test
    @DisplayName("requestableMenuTree → 排除 admin_only 子树、申请入口、已拥有按钮")
    void requestableTree_filtersExclusionsAndOwnedButtons() {
        givenUser("alice", 7L);
        SysMenu adminDir = menu(1L, 0L, 1, "adminDir", null);
        adminDir.setAdminOnly(1);
        SysMenu adminBtn = menu(11L, 1L, 3, "adminBtn", "X_1");
        SysMenu normalDir = menu(2L, 0L, 2, "normalPage", null);
        SysMenu ownedBtn = menu(21L, 2L, 3, "ownedBtn", "X_2");
        SysMenu freeBtn = menu(22L, 2L, 3, "freeBtn", "X_3");
        SysMenu entry = menu(3L, 0L, 2, "permissionRequest", null);
        when(menuMapper.selectList(any()))
                .thenReturn(List.of(adminDir, adminBtn, normalDir, ownedBtn, freeBtn, entry));
        // 已授权菜单：含已拥有按钮
        when(menuMapper.selectAuthorizedMenusByUserId(7L)).thenReturn(List.of(ownedBtn));

        var vos = service.requestableMenuTree("alice");

        // 现状语义（测试固化）：admin_only/申请入口仅排除「子孙」，节点自身保留；已拥有按钮排除
        assertEquals(2, vos.size());
        assertEquals(1L, vos.get(0).id());
        assertNull(vos.get(0).children());
        assertEquals(2L, vos.get(1).id());
        assertEquals(1, vos.get(1).children().size());
        assertEquals(22L, vos.get(1).children().get(0).id());
    }

    @Test
    @DisplayName("userMenuPerms → 非 ADMIN 合并 perms 并按 admin_only 排除、去重")
    void userMenuPerms_mergesDistinctNonAdminOnly() {
        givenUser("bob", 8L);
        SysMenu m1 = menu(1L, 0L, 3, "a", "JOB_VIEW");
        SysMenu dup = menu(2L, 0L, 3, "b", "JOB_VIEW");
        SysMenu hidden = menu(3L, 0L, 3, "c", "SECRET");
        hidden.setAdminOnly(1);
        SysMenu noPerms = menu(4L, 0L, 2, "d", null);
        when(menuMapper.selectAuthorizedMenusByUserId(8L)).thenReturn(List.of(m1, dup, hidden, noPerms));

        var perms = service.userMenuPerms("bob");

        assertEquals(List.of("JOB_VIEW"), perms);
    }

    @Test
    @DisplayName("loadUserContext 缓存 → 同用户第二次调用不再查库，evict 后重新加载")
    void userContextCache_hitAndEvict() {
        givenUser("carol", 9L);

        service.userMenuPerms("carol");
        service.userMenuPerms("carol");
        verify(userMapper, times(1)).selectOne(any());

        service.evictUserContext("carol");
        service.userMenuPerms("carol");
        verify(userMapper, times(2)).selectOne(any());
    }
}
