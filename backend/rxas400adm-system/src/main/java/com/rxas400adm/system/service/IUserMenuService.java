package com.rxas400adm.system.service;

import com.rxas400adm.system.entity.SysMenu;

import java.util.List;
import java.util.Set;

/**
 * 用户-菜单直接授权服务接口（rx_user_menu）。
 */
public interface IUserMenuService {

    Set<Long> getUserMenuIds(Long userId);

    Set<Long> getUserDirectMenuIds(Long userId);

    List<SysMenu> getManageableMenuTree(Long userId);

    void addUserMenus(Long userId, List<Long> menuIds);

    void removeUserMenus(Long userId, List<Long> menuIds);
}
