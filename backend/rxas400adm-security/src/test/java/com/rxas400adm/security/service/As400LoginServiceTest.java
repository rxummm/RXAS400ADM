package com.rxas400adm.security.service;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.security.dto.As400LoginRequest;
import com.rxas400adm.security.dto.LoginResponse;
import com.rxas400adm.security.jwt.JwtUtil;
import com.rxas400adm.system.entity.SysRole;
import com.rxas400adm.system.entity.SysUser;
import com.rxas400adm.system.entity.SysUserRole;
import com.rxas400adm.system.mapper.SysRoleMapper;
import com.rxas400adm.system.mapper.SysUserMapper;
import com.rxas400adm.system.mapper.SysUserRoleMapper;
import com.rxas400adm.system.service.ISysConfigService;
import com.rxas400adm.system.service.SysUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class As400LoginServiceTest {

    @Mock
    private AS400ClientProvider clientProvider;

    @Mock
    private AS400Client client;

    @Mock
    private SysUserService userService;

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private SysRoleMapper roleMapper;

    @Mock
    private SysUserRoleMapper userRoleMapper;

    @Mock
    private ISysConfigService sysConfigService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PermissionService permissionService;

    @Mock
    private JwtUtil jwtUtil;

    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private As400LoginService service;

    @BeforeEach
    void setUp() {
        service = new As400LoginService(clientProvider, userService, userMapper,
                roleMapper, userRoleMapper, sysConfigService, objectMapper,
                permissionService, jwtUtil, passwordEncoder);
        when(clientProvider.forServer(1L)).thenReturn(client);
        lenient().when(client.userProfile(anyString())).thenReturn(null);
        lenient().when(jwtUtil.generateToken(anyString(), any())).thenReturn("jwt-token");
    }

    private As400LoginRequest request() {
        As400LoginRequest req = new As400LoginRequest();
        req.setServerId(1L);
        req.setUsername("as400user");
        req.setPassword("secret");
        return req;
    }

    @Test
    void login_authenticationFailed_shouldThrow() {
        when(client.authenticate("as400user", "secret")).thenReturn(false);
        assertThrows(BusinessException.class, () -> service.login(request()));
        verify(userService, never()).getByUsername(anyString());
    }

    @Test
    void login_existingUser_shouldReturnToken() {
        when(client.authenticate("as400user", "secret")).thenReturn(true);
        SysUser existing = new SysUser();
        existing.setUsername("as400user");
        existing.setStatus("ACTIVE");
        when(userService.getByUsername("as400user")).thenReturn(existing);
        when(permissionService.refresh("as400user")).thenReturn(List.of("JOB_VIEW"));

        LoginResponse response = service.login(request());
        assertEquals("as400user", response.getUsername());
        assertEquals("jwt-token", response.getToken());
        verify(userMapper, never()).insert(any(SysUser.class));
    }

    @Test
    void login_disabledLocalUser_shouldThrow() {
        when(client.authenticate("as400user", "secret")).thenReturn(true);
        SysUser existing = new SysUser();
        existing.setUsername("as400user");
        existing.setStatus("DISABLED");
        when(userService.getByUsername("as400user")).thenReturn(existing);

        // S1：平台侧已禁用，即使 AS400 认证通过也拒绝
        BusinessException ex = assertThrows(BusinessException.class, () -> service.login(request()));
        assertEquals(403, ex.getCode());
        verify(userMapper, never()).insert(any(SysUser.class));
        verify(userMapper, never()).updateById(any(SysUser.class));
    }

    @Test
    void login_newUser_shouldAutoCreateWithViewerRole() {
        when(client.authenticate("as400user", "secret")).thenReturn(true);
        when(userService.getByUsername("as400user")).thenReturn(null);
        when(permissionService.refresh("as400user")).thenReturn(List.of());
        SysRole viewer = new SysRole();
        viewer.setId(10L);
        viewer.setRoleCode("VIEWER");
        when(roleMapper.selectOne(any())).thenReturn(viewer);

        service.login(request());

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).insert(captor.capture());
        assertEquals("AS400", captor.getValue().getLoginSource());
        assertEquals(1L, captor.getValue().getAs400ServerId());
        verify(userRoleMapper).insert(org.mockito.ArgumentMatchers.<SysUserRole>any());
    }

    @Test
    void login_groupRoleMapping_shouldAssignMappedRole() {
        when(client.authenticate("as400user", "secret")).thenReturn(true);
        when(client.userProfile("as400user")).thenReturn(
                new com.rxas400adm.as400.model.UserProfileRow("AS400USER", "GRPDEV", "*ENABLED"));
        when(sysConfigService.get(As400LoginService.GROUP_ROLE_MAPPING_KEY, ""))
                .thenReturn("{\"GRPDEV\":\"DEVELOPER\"}");
        when(userService.getByUsername("as400user")).thenReturn(null);
        when(permissionService.refresh("as400user")).thenReturn(List.of());
        SysRole dev = new SysRole();
        dev.setId(20L);
        dev.setRoleCode("DEVELOPER");
        when(roleMapper.selectOne(any())).thenReturn(dev);

        service.login(request());

        ArgumentCaptor<SysUserRole> urCaptor = ArgumentCaptor.forClass(SysUserRole.class);
        verify(userRoleMapper).insert(urCaptor.capture());
        assertEquals(20L, urCaptor.getValue().getRoleId());
    }
}