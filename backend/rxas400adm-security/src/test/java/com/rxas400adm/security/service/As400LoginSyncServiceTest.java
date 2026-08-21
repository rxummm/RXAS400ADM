package com.rxas400adm.security.service;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.entity.IbmiSystem;
import com.rxas400adm.as400.mapper.IbmiSystemMapper;
import com.rxas400adm.system.entity.SysUser;
import com.rxas400adm.system.mapper.SysUserMapper;
import com.rxas400adm.system.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class As400LoginSyncServiceTest {

    @Mock
    private IbmiSystemMapper systemMapper;

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private SysUserRoleMapper userRoleMapper;

    @Mock
    private AS400ClientProvider clientProvider;

    @Mock
    private AS400Client client;

    @Mock
    private As400LoginService as400LoginService;

    @Mock
    private PermissionService permissionService;

    private As400LoginSyncService service;

    @BeforeEach
    void setUp() {
        service = new As400LoginSyncService(systemMapper, userMapper, userRoleMapper,
                clientProvider, as400LoginService, permissionService);
    }

    private IbmiSystem system() {
        IbmiSystem s = new IbmiSystem();
        s.setId(1L);
        s.setName("US400CND");
        s.setEnabled(true);
        return s;
    }

    private SysUser user() {
        SysUser u = new SysUser();
        u.setId(7L);
        u.setUsername("as400user");
        u.setLoginSource("AS400");
        u.setAs400ServerId(1L);
        return u;
    }

    @Test
    void dailySync_mockProfile_shouldSkip() {
        ReflectionTestUtils.setField(service, "activeProfile", "mock");
        service.dailySync();
        verify(systemMapper, never()).selectList(any());
    }

    @Test
    void dailySync_staleUser_shouldDeleteWithRoles() {
        ReflectionTestUtils.setField(service, "activeProfile", "prod");
        when(systemMapper.selectList(any())).thenReturn(List.of(system()));
        when(clientProvider.forServer(1L)).thenReturn(client);
        // 两名本地用户：stale 在 IBM i 已删除（profile 为空），alive 仍存在
        // → 部分缺失（非整机故障），正常清理失效账号
        SysUser stale = user();
        SysUser alive = new SysUser();
        alive.setId(8L);
        alive.setUsername("as400alive");
        alive.setLoginSource("AS400");
        alive.setAs400ServerId(1L);
        when(userMapper.selectList(any())).thenReturn(List.of(stale, alive));
        when(client.userProfile("as400user")).thenReturn(null);
        when(client.userProfile("as400alive")).thenReturn(
                new com.rxas400adm.as400.model.UserProfileRow("AS400ALIVE", "GRPDEV", "*ENABLED"));
        when(as400LoginService.loadGroupRoleMapping()).thenReturn(Map.of("GRPDEV", "DEVELOPER"));

        service.dailySync();

        verify(userRoleMapper).delete(any());
        verify(userMapper).deleteById(Long.valueOf(7L));
        verify(permissionService).evict("as400user");
        verify(as400LoginService, never()).applyRoles(org.mockito.ArgumentMatchers.eq(Long.valueOf(7L)), any());
        verify(as400LoginService).applyRoles(Long.valueOf(8L), List.of("DEVELOPER"));
    }

    @Test
    void dailySync_allMissing_firstRound_shouldNotDelete() {
        ReflectionTestUtils.setField(service, "activeProfile", "prod");
        when(systemMapper.selectList(any())).thenReturn(List.of(system()));
        when(clientProvider.forServer(1L)).thenReturn(client);
        when(userMapper.selectList(any())).thenReturn(List.of(user()));
        when(client.userProfile("as400user")).thenReturn(null);

        service.dailySync();

        // M2：整机疑似故障——首轮全空仅计数（streak=1 < LIMIT=2），暂缓清理
        verify(userRoleMapper, never()).delete(any());
        verify(userMapper, never()).deleteById(org.mockito.ArgumentMatchers.<java.io.Serializable>any());
    }

    @Test
    void dailySync_allMissing_secondRound_shouldDelete() {
        ReflectionTestUtils.setField(service, "activeProfile", "prod");
        when(systemMapper.selectList(any())).thenReturn(List.of(system()));
        when(clientProvider.forServer(1L)).thenReturn(client);
        when(userMapper.selectList(any())).thenReturn(List.of(user()));
        when(client.userProfile("as400user")).thenReturn(null);

        service.dailySync(); // 第 1 轮：streak=1，暂缓
        service.dailySync(); // 第 2 轮：streak=2 = LIMIT，按失效清理

        verify(userRoleMapper).delete(any());
        verify(userMapper).deleteById(Long.valueOf(7L));
        verify(permissionService).evict("as400user");
    }

    @Test
    void dailySync_groupConverge_shouldApplyMappedRole() {
        ReflectionTestUtils.setField(service, "activeProfile", "prod");
        when(systemMapper.selectList(any())).thenReturn(List.of(system()));
        when(clientProvider.forServer(1L)).thenReturn(client);
        when(userMapper.selectList(any())).thenReturn(List.of(user()));
        when(client.userProfile("as400user")).thenReturn(
                new com.rxas400adm.as400.model.UserProfileRow("AS400USER", "GRPDEV", "*ENABLED"));
        when(as400LoginService.loadGroupRoleMapping()).thenReturn(Map.of("GRPDEV", "DEVELOPER"));

        service.dailySync();

        verify(as400LoginService).applyRoles(Long.valueOf(7L), List.of("DEVELOPER"));
        verify(permissionService).evict("as400user");
        verify(userMapper, never()).deleteById(org.mockito.ArgumentMatchers.<java.io.Serializable>any());
    }

    @Test
    void dailySync_serverFailure_shouldSkipThatServer() {
        ReflectionTestUtils.setField(service, "activeProfile", "prod");
        when(systemMapper.selectList(any())).thenReturn(List.of(system()));
        when(clientProvider.forServer(1L)).thenThrow(new RuntimeException("connect failed"));

        service.dailySync();

        verify(userMapper, never()).selectList(any());
    }
}
