package com.rxas400adm.system.service;

import com.rxas400adm.common.event.UserPermissionGrantedEvent;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.system.entity.PermissionRequest;
import com.rxas400adm.system.entity.SysMenu;
import com.rxas400adm.system.entity.SysPermission;
import com.rxas400adm.system.entity.SysRole;
import com.rxas400adm.system.entity.SysRolePermission;
import com.rxas400adm.system.entity.SysUser;
import com.rxas400adm.system.entity.SysUserMenu;
import com.rxas400adm.system.entity.SysUserRole;
import com.rxas400adm.system.mapper.PermissionRequestMapper;
import com.rxas400adm.system.mapper.SysMenuMapper;
import com.rxas400adm.system.mapper.SysPermissionMapper;
import com.rxas400adm.system.mapper.SysRoleMapper;
import com.rxas400adm.system.mapper.SysRolePermissionMapper;
import com.rxas400adm.system.mapper.SysUserMapper;
import com.rxas400adm.system.mapper.SysUserMenuMapper;
import com.rxas400adm.system.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 权限自助申请服务单测（纯 Mockito，不依赖 DB）：
 * 覆盖提交校验、审批幂等重入守卫、审批双管线（菜单直接授权 / 权限码绑 REQUESTED 角色）
 * 与子孙按钮自动补全、既有授权去重。
 */
@ExtendWith(MockitoExtension.class)
class PermissionRequestServiceTest {

    @Mock private PermissionRequestMapper requestMapper;
    @Mock private SysPermissionMapper permissionMapper;
    @Mock private SysRoleMapper roleMapper;
    @Mock private SysRolePermissionMapper rolePermissionMapper;
    @Mock private SysUserMapper userMapper;
    @Mock private SysUserRoleMapper userRoleMapper;
    @Mock private SysMenuMapper menuMapper;
    @Mock private SysUserMenuMapper userMenuMapper;
    @Mock private INotificationService notificationService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private PermissionRequestService service;

    @BeforeEach
    void setUp() {
        service = new PermissionRequestService(requestMapper, permissionMapper, roleMapper,
                rolePermissionMapper, userMapper, userRoleMapper, menuMapper, userMenuMapper,
                notificationService, eventPublisher);
    }

    private PermissionRequest pendingRequest(long id, String permissionCode, String menuIds, String menuNames) {
        PermissionRequest r = new PermissionRequest();
        r.setId(id);
        r.setUsername("alice");
        r.setPermissionCode(permissionCode);
        r.setMenuIds(menuIds);
        r.setMenuNames(menuNames);
        r.setStatus("PENDING");
        return r;
    }

    // ---------------- create ----------------

    @Test
    @DisplayName("create → 无菜单且无权限码拒绝")
    void create_empty_shouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create("alice", " ", List.of(), List.of(), "理由"));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("create → 权限码 trim + 大写归一后落库，状态 PENDING")
    void create_permissionCodeNormalized() {
        when(requestMapper.selectCount(any())).thenReturn(0L);
        when(requestMapper.insert(any(PermissionRequest.class))).thenReturn(1);

        PermissionRequest created = service.create("alice", " job_view ", null, null, "需要作业查询");

        assertEquals("JOB_VIEW", created.getPermissionCode());
        assertEquals("PENDING", created.getStatus());
        verify(requestMapper).insert(any(PermissionRequest.class));
    }

    @Test
    @DisplayName("create → 同一权限已有 PENDING 申请时拒绝重复提交")
    void create_duplicatePending_shouldThrow() {
        when(requestMapper.selectCount(any())).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create("alice", "JOB_VIEW", null, null, "重复申请"));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
    }

    // ---------------- requirePending 守卫 ----------------

    @Test
    @DisplayName("approve → 记录不存在抛 NOT_FOUND")
    void approve_missing_shouldThrowNotFound() {
        when(requestMapper.selectById(9L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.approve(9L, "admin", "ok"));
        assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("approve → 已处理的申请不能重复审批（幂等重入守卫）")
    void approve_alreadyProcessed_shouldThrow() {
        PermissionRequest done = pendingRequest(1L, "JOB_VIEW", null, null);
        done.setStatus("APPROVED");
        when(requestMapper.selectById(1L)).thenReturn(done);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.approve(1L, "admin", "ok"));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
        verify(notificationService, never()).send(any(), any(), any(), any());
    }

    // ---------------- approve：权限码管线 ----------------

    @Test
    @DisplayName("approve 权限码模式 → 权限/REQUESTED 角色缺失则创建并完成两层绑定")
    void approve_permissionCode_grantsNewBinding() {
        PermissionRequest req = pendingRequest(1L, "JOB_VIEW", null, null);
        when(requestMapper.selectById(1L)).thenReturn(req);
        when(permissionMapper.selectOne(any())).thenReturn(null);
        when(permissionMapper.insert(any(SysPermission.class))).thenAnswer(inv -> {
            inv.getArgument(0, SysPermission.class).setId(100L);
            return 1;
        });
        when(roleMapper.selectOne(any())).thenReturn(null);
        when(roleMapper.insert(any(SysRole.class))).thenAnswer(inv -> {
            inv.getArgument(0, SysRole.class).setId(200L);
            return 1;
        });
        when(rolePermissionMapper.selectCount(any())).thenReturn(0L);
        SysUser alice = new SysUser();
        alice.setId(7L);
        when(userMapper.selectOne(any())).thenReturn(alice);
        when(userRoleMapper.selectCount(any())).thenReturn(0L);

        service.approve(1L, "admin", "同意");

        assertEquals("APPROVED", req.getStatus());
        assertEquals("admin", req.getApprover());
        verify(rolePermissionMapper).insert(any(SysRolePermission.class));
        verify(userRoleMapper).insert(any(SysUserRole.class));
        verify(notificationService).send(eq("alice"), eq("PERMISSION"), any(), any());
        verify(eventPublisher).publishEvent(any(UserPermissionGrantedEvent.class));
        verify(requestMapper).updateById(req);
    }

    @Test
    @DisplayName("approve 权限码模式 → 绑定已存在时不重复插入（幂等）")
    void approve_permissionCode_idempotent() {
        PermissionRequest req = pendingRequest(1L, "JOB_VIEW", null, null);
        when(requestMapper.selectById(1L)).thenReturn(req);
        SysPermission existing = new SysPermission();
        existing.setId(100L);
        when(permissionMapper.selectOne(any())).thenReturn(existing);
        SysRole existingRole = new SysRole();
        existingRole.setId(200L);
        when(roleMapper.selectOne(any())).thenReturn(existingRole);
        when(rolePermissionMapper.selectCount(any())).thenReturn(1L);
        SysUser alice = new SysUser();
        alice.setId(7L);
        when(userMapper.selectOne(any())).thenReturn(alice);
        when(userRoleMapper.selectCount(any())).thenReturn(1L);

        service.approve(1L, "admin", null);

        assertEquals("APPROVED", req.getStatus());
        verify(permissionMapper, never()).insert(any(SysPermission.class));
        verify(roleMapper, never()).insert(any(SysRole.class));
        verify(rolePermissionMapper, never()).insert(any(SysRolePermission.class));
        verify(userRoleMapper, never()).insert(any(SysUserRole.class));
    }

    // ---------------- approve：菜单树管线 ----------------

    @Test
    @DisplayName("approve 菜单模式 → 未精确选按钮时自动补全子孙按钮，既有授权去重")
    void approve_menuMode_completesDescendantButtonsAndSkipsExisting() {
        PermissionRequest req = pendingRequest(2L, null, "[1]", "[\"系统管理\"]");
        when(requestMapper.selectById(2L)).thenReturn(req);

        SysUser alice = new SysUser();
        alice.setId(7L);
        when(userMapper.selectOne(any())).thenReturn(alice);

        SysMenu dir = menu(1L, 0L, 2);
        SysMenu btnA = menu(11L, 1L, 3);
        SysMenu btnB = menu(12L, 1L, 3);
        when(menuMapper.selectList(any())).thenReturn(List.of(dir, btnA, btnB));

        // 已有授权：按钮 11 已授 → 仅应新插目录 1 与按钮 12
        SysUserMenu granted = new SysUserMenu();
        granted.setUserId(7L);
        granted.setMenuId(11L);
        when(userMenuMapper.selectList(any())).thenReturn(List.of(granted));

        service.approve(2L, "admin", "同意");

        ArgumentCaptor<SysUserMenu> captor = ArgumentCaptor.forClass(SysUserMenu.class);
        verify(userMenuMapper, times(2)).insert(captor.capture());
        List<Long> grantedIds = captor.getAllValues().stream().map(SysUserMenu::getMenuId).sorted().toList();
        assertEquals(List.of(1L, 12L), grantedIds);
        assertEquals("APPROVED", req.getStatus());
        verify(eventPublisher).publishEvent(any(UserPermissionGrantedEvent.class));
    }

    @Test
    @DisplayName("approve 菜单模式 → 用户不存在时静默跳过授权但仍闭环审批")
    void approve_menuMode_unknownUserStillApproves() {
        PermissionRequest req = pendingRequest(3L, null, "[1]", null);
        when(requestMapper.selectById(3L)).thenReturn(req);
        when(userMapper.selectOne(any())).thenReturn(null);

        service.approve(3L, "admin", null);

        assertEquals("APPROVED", req.getStatus());
        verify(userMenuMapper, never()).insert(any(SysUserMenu.class));
        verify(requestMapper).updateById(req);
    }

    // ---------------- reject ----------------

    @Test
    @DisplayName("reject → 标记 REJECTED 并通知申请人")
    void reject_marksRejectedAndNotifies() {
        PermissionRequest req = pendingRequest(4L, "JOB_VIEW", null, null);
        when(requestMapper.selectById(4L)).thenReturn(req);

        service.reject(4L, "admin", "证据不足");

        assertEquals("REJECTED", req.getStatus());
        assertEquals("admin", req.getApprover());
        verify(notificationService).send(any(), any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any(UserPermissionGrantedEvent.class));
    }

    // ---------------- 工具 ----------------

    private SysMenu menu(long id, long parentId, int type) {
        SysMenu m = new SysMenu();
        m.setId(id);
        m.setParentId(parentId);
        m.setMenuType(type);
        m.setStatus(1);
        return m;
    }
}
