package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.rxas400adm.common.event.UserPermissionGrantedEvent;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.system.dto.UserDTO;
import com.rxas400adm.system.dto.UserUpdateDTO;
import com.rxas400adm.system.entity.SysPermission;
import com.rxas400adm.system.entity.SysRole;
import com.rxas400adm.system.entity.SysRolePermission;
import com.rxas400adm.system.entity.SysUser;
import com.rxas400adm.system.entity.SysUserRole;
import com.rxas400adm.system.mapper.SysPermissionMapper;
import com.rxas400adm.system.mapper.SysRoleMapper;
import com.rxas400adm.system.mapper.SysRolePermissionMapper;
import com.rxas400adm.system.mapper.SysUserMapper;
import com.rxas400adm.system.mapper.SysUserRoleMapper;
import com.rxas400adm.system.vo.UserVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class SysUserServiceImplTest {

    private final SysUserMapper userMapper = mock(SysUserMapper.class);
    private final SysRoleMapper roleMapper = mock(SysRoleMapper.class);
    private final SysPermissionMapper permissionMapper = mock(SysPermissionMapper.class);
    private final SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
    private final SysRolePermissionMapper rolePermissionMapper = mock(SysRolePermissionMapper.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private final org.springframework.context.ApplicationEventPublisher eventPublisher =
            mock(org.springframework.context.ApplicationEventPublisher.class);

    private SysUserServiceImpl service() {
        return new SysUserServiceImpl(userMapper, roleMapper, permissionMapper,
                userRoleMapper, rolePermissionMapper, encoder, eventPublisher);
    }

    @Test
    void create_shouldEncodePassword() {
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        SysUserServiceImpl service = service();
        UserDTO dto = new UserDTO();
        dto.setUsername("dev1");
        dto.setPassword("secret123");

        UserVO created = service.create(dto);
        assertEquals("dev1", created.getUsername());
        assertEquals("PLATFORM", created.getLoginSource());

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).insert(captor.capture());
        assertNotEquals("secret123", captor.getValue().getPassword());
    }

    @Test
    void createDuplicateUsername_shouldThrow() {
        SysUser existing = new SysUser();
        existing.setUsername("admin");
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(existing);

        UserDTO dto = new UserDTO();
        dto.setUsername("admin");
        assertThrows(BusinessException.class, () -> service().create(dto));
    }

    @Test
    void listPermissions_shouldResolveThroughUserRolePermissionChain() {
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(1L);
        userRole.setRoleId(10L);
        when(userRoleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(userRole));

        SysRolePermission rp = new SysRolePermission();
        rp.setRoleId(10L);
        rp.setPermissionId(100L);
        when(rolePermissionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(rp));

        SysPermission permission = new SysPermission();
        permission.setId(100L);
        permission.setPermissionCode("JOB_VIEW");
        when(permissionMapper.selectBatchIds(List.of(100L))).thenReturn(List.of(permission));

        List<String> permissions = service().listPermissions(1L);
        assertEquals(List.of("JOB_VIEW"), permissions);
    }

    @Test
    void listPermissions_withoutRoles_shouldBeEmpty() {
        when(userRoleMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        assertEquals(List.of(), service().listPermissions(1L));
    }

    // ---------- 角色绑定（roleIds / roleCodes） ----------

    @Test
    void create_withRoleIds_shouldBindRoles() {
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        SysRole role = new SysRole();
        role.setId(10L);
        role.setRoleCode("OPERATOR");
        when(roleMapper.selectById(10L)).thenReturn(role);
        when(roleMapper.selectBatchIds(List.of(10L))).thenReturn(List.of(role));

        UserDTO dto = new UserDTO();
        dto.setUsername("op1");
        dto.setPassword("secret123");
        dto.setRoleIds(List.of(10L));

        service().create(dto);

        ArgumentCaptor<List<SysUserRole>> captor = ArgumentCaptor.forClass(List.class);
        verify(userRoleMapper).insertBatch(captor.capture());
        assertEquals(10L, captor.getValue().get(0).getRoleId());
    }

    @Test
    void create_withRoleCodes_shouldResolveAndBind() {
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        SysRole role = new SysRole();
        role.setId(10L);
        role.setRoleCode("OPERATOR");
        when(roleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(role));
        when(roleMapper.selectById(10L)).thenReturn(role);
        when(roleMapper.selectBatchIds(List.of(10L))).thenReturn(List.of(role));

        UserDTO dto = new UserDTO();
        dto.setUsername("op2");
        dto.setPassword("secret123");
        dto.setRoleCodes(List.of("OPERATOR"));

        service().create(dto);

        ArgumentCaptor<List<SysUserRole>> captor = ArgumentCaptor.forClass(List.class);
        verify(userRoleMapper).insertBatch(captor.capture());
        assertEquals(10L, captor.getValue().get(0).getRoleId());
    }

    @Test
    void create_withUnknownRoleCode_shouldThrow() {
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(roleMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        UserDTO dto = new UserDTO();
        dto.setUsername("x1");
        dto.setPassword("secret123");
        dto.setRoleCodes(List.of("NO_SUCH_ROLE"));

        assertThrows(BusinessException.class, () -> service().create(dto));
    }

    @Test
    void disableUser_shouldPublishCacheEvictEvent() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("op1");
        when(userMapper.selectById(1L)).thenReturn(user);

        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setStatus("DISABLED");

        service().update(1L, dto);

        // S1：禁用后立即失效权限缓存，无需等 60s TTL
        org.mockito.ArgumentCaptor<Object> captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals("op1", ((UserPermissionGrantedEvent) captor.getValue()).username());
    }

    @Test
    void deleteUser_shouldPublishCacheEvictEvent() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("op1");
        when(userMapper.selectById(1L)).thenReturn(user);

        service().delete(1L);

        org.mockito.ArgumentCaptor<Object> captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals("op1", ((UserPermissionGrantedEvent) captor.getValue()).username());
    }

    @Test
    void update_withRoleIds_shouldRebuildRoles() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("op1");
        when(userMapper.selectById(1L)).thenReturn(user);

        SysRole role = new SysRole();
        role.setId(10L);
        role.setRoleCode("OPERATOR");
        when(roleMapper.selectById(10L)).thenReturn(role);
        when(roleMapper.selectBatchIds(List.of(10L))).thenReturn(List.of(role));

        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setRoleIds(List.of(10L));

        service().update(1L, dto);

        // 整体重建：先删旧关联，再批量插入新角色
        verify(userRoleMapper).delete(any(Wrapper.class));
        ArgumentCaptor<List<SysUserRole>> captor = ArgumentCaptor.forClass(List.class);
        verify(userRoleMapper).insertBatch(captor.capture());
        assertEquals(10L, captor.getValue().get(0).getRoleId());
    }
}