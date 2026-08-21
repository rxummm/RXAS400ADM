package com.rxas400adm.security.service;

import com.rxas400adm.security.dto.As400LoginRequest;
import com.rxas400adm.security.dto.LoginResponse;

import java.util.List;
import java.util.Map;

public interface IAs400LoginService {

    LoginResponse login(As400LoginRequest request);

    /** 读取 sys_config 的组 → 角色映射 JSON（供每日同步复用） */
    Map<String, String> loadGroupRoleMapping();

    /** 重建用户角色（先删后插，供每日同步复用） */
    void applyRoles(Long userId, List<String> roleCodes);
}