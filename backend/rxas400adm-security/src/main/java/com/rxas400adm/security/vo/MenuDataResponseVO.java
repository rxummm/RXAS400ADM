package com.rxas400adm.security.vo;

import com.rxas400adm.system.vo.MenuVO;
import com.rxas400adm.system.vo.TabVO;

import java.util.List;
import java.util.Set;

/**
 * 菜单权限数据响应（AuthController.menu 返回）。
 */
public record MenuDataResponseVO(
        List<MenuVO> menus,
        Set<String> perms,
        List<TabVO> tabs
) {
}
