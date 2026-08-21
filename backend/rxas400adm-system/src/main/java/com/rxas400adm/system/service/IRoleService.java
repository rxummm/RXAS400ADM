package com.rxas400adm.system.service;

import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.dto.SysRoleDTO;
import com.rxas400adm.system.entity.SysRole;

import java.util.List;

/**
 * 角色服务接口。
 */
public interface IRoleService {

    List<SysRole> listAll();

    PageResult<SysRole> page(long current, long size, String keyword);

    SysRole create(SysRoleDTO role);

    SysRole update(Long id, SysRoleDTO dto);

    void delete(Long id);

    int batchDelete(List<Long> ids);
}