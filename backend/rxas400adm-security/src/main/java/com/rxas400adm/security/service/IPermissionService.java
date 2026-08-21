package com.rxas400adm.security.service;

import com.rxas400adm.system.entity.SysUser;

import java.util.List;

public interface IPermissionService {

    List<String> loadPermissions(String username);

    List<String> refresh(String username);

    /** 登录专用：复用已查询的 SysUser，避免重复 getByUsername */
    List<String> refresh(SysUser user);

    void evict(String username);
}