package com.rxas400adm.system.service;

import com.rxas400adm.system.dto.SysMenuDTO;
import com.rxas400adm.system.entity.SysMenu;
import com.rxas400adm.system.vo.MenuVO;
import com.rxas400adm.system.vo.RequestableMenuVO;
import com.rxas400adm.system.vo.UserMenuDataVO;

import java.util.List;

/**
 * 菜单服务接口。
 */
public interface IMenuService {

    List<SysMenu> tree();

    List<MenuVO> enabledMenuTree();

    List<MenuVO> userMenuTree(String username);

    List<RequestableMenuVO> requestableMenuTree(String username);

    UserMenuDataVO userMenuData(String username);

    List<String> userMenuPerms(String username);

    SysMenu create(SysMenuDTO menu);

    SysMenu update(Long id, SysMenuDTO dto);

    SysMenu toggleStatus(Long id, Integer status);

    void delete(Long id);
}