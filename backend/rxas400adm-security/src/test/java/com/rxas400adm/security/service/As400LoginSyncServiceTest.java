package com.rxas400adm.security.service;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.entity.IbmiSystem;
import com.rxas400adm.as400.mapper.IbmiSystemMapper;
import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.system.entity.SysUser;
import com.rxas400adm.system.mapper.SysUserMapper;
import com.rxas400adm.system.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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

    @Mock
    private ProfileResolver profileResolver;

    private As400LoginSyncService service;

    @BeforeEach
    void setUp() {
        service = new As400LoginSyncService(systemMapper, userMapper, userRoleMapper,
                clientProvider, as400LoginService, permissionService, profileResolver);
    }

    private void setMockMode(boolean mock) {
        when(profileResolver.isMockMode()).thenReturn(mock);
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
        setMockMode(true);
        service.dailySync();
        verify(systemMapper, never()).selectList(any());
    }

    @Test
    void dailySync_staleUser_shouldDeleteWithRoles() {
        setMockMode(false);
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
        // P15：批量探测改为 stub 单条 USER_INFO 查询——stale 不在 IBM i 全库结果中，alive 命中
        when(client.queryListCheckedBounded(anyString(), anyInt())).thenReturn(List.of(
                Map.of("AUTHORIZATION_NAME", "AS400ALIVE", "GROUP_PROFILE_NAME", "GRPDEV")));
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
        setMockMode(false);
        when(systemMapper.selectList(any())).thenReturn(List.of(system()));
        when(clientProvider.forServer(1L)).thenReturn(client);
        when(userMapper.selectList(any())).thenReturn(List.of(user()));
        // P15：全库查询 0 行 → 该账号在 IBM i 上不存在
        when(client.queryListCheckedBounded(anyString(), anyInt())).thenReturn(List.of());

        service.dailySync();

        // M2：整机疑似故障——首轮全空仅计数（streak=1 < LIMIT=2），暂缓清理
        verify(userRoleMapper, never()).delete(any());
        verify(userMapper, never()).deleteById(org.mockito.ArgumentMatchers.<java.io.Serializable>any());
    }

    @Test
    void dailySync_allMissing_secondRound_shouldDelete() {
        setMockMode(false);
        when(systemMapper.selectList(any())).thenReturn(List.of(system()));
        when(clientProvider.forServer(1L)).thenReturn(client);
        when(userMapper.selectList(any())).thenReturn(List.of(user()));
        // P15：全库查询 0 行 → 该账号在 IBM i 上不存在
        when(client.queryListCheckedBounded(anyString(), anyInt())).thenReturn(List.of());

        service.dailySync(); // 第 1 轮：streak=1，暂缓
        service.dailySync(); // 第 2 轮：streak=2 = LIMIT，按失效清理

        verify(userRoleMapper).delete(any());
        verify(userMapper).deleteById(Long.valueOf(7L));
        verify(permissionService).evict("as400user");
    }

    @Test
    void dailySync_groupConverge_shouldApplyMappedRole() {
        setMockMode(false);
        when(systemMapper.selectList(any())).thenReturn(List.of(system()));
        when(clientProvider.forServer(1L)).thenReturn(client);
        when(userMapper.selectList(any())).thenReturn(List.of(user()));
        // P15：全库查询命中该账号（大写 AUTHORIZATION_NAME + 组 profile）
        when(client.queryListCheckedBounded(anyString(), anyInt())).thenReturn(List.of(
                Map.of("AUTHORIZATION_NAME", "AS400USER", "GROUP_PROFILE_NAME", "GRPDEV")));
        when(as400LoginService.loadGroupRoleMapping()).thenReturn(Map.of("GRPDEV", "DEVELOPER"));

        service.dailySync();

        verify(as400LoginService).applyRoles(Long.valueOf(7L), List.of("DEVELOPER"));
        verify(permissionService).evict("as400user");
        verify(userMapper, never()).deleteById(org.mockito.ArgumentMatchers.<java.io.Serializable>any());
    }

    @Test
    void dailySync_serverFailure_shouldSkipThatServer() {
        setMockMode(false);
        when(systemMapper.selectList(any())).thenReturn(List.of(system()));
        when(clientProvider.forServer(1L)).thenThrow(new RuntimeException("connect failed"));

        service.dailySync();

        verify(userMapper, never()).selectList(any());
    }
}
