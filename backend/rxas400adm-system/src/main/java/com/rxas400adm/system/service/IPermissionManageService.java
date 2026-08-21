package com.rxas400adm.system.service;

import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.dto.SysPermissionDTO;
import com.rxas400adm.system.entity.SysPermission;
import com.rxas400adm.system.vo.PermissionVO;

import java.util.List;

/**
 * 权限码管理服务接口（rx_permission CRUD + 按菜单匹配的下拉建议）。
 */
public interface IPermissionManageService {

    PageResult<PermissionVO> page(String keyword, String module, long current, long size);

    List<PermissionVO> listAll();

    List<PermissionVO> suggest(String menuTitle, String keyword);

    SysPermission create(SysPermissionDTO dto);

    SysPermission update(Long id, SysPermissionDTO dto);

    void delete(Long id);
}
