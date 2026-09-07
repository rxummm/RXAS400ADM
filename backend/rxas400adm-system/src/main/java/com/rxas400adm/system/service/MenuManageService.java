package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.system.dto.SysMenuDTO;
import com.rxas400adm.system.entity.SysMenu;
import com.rxas400adm.system.mapper.SysMenuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 菜单管理 CRUD（从 MenuService 拆分）：新增、编辑、删除、状态切换、环检测。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuManageService {

    private final SysMenuMapper menuMapper;

    /** 新增菜单 */
    public SysMenu create(SysMenuDTO dto) {
        SysMenu menu = new SysMenu();
        menu.setParentId(dto.getParentId());
        menu.setMenuName(dto.getMenuName());
        menu.setMenuType(dto.getMenuType());
        menu.setTitle(dto.getTitle());
        menu.setPath(dto.getPath());
        menu.setComponent(dto.getComponent());
        menu.setPerms(dto.getPerms());
        menu.setIcon(dto.getIcon());
        menu.setSort(dto.getSort());
        menu.setVisible(dto.getVisible());
        menu.setStatus(dto.getStatus());
        menu.setAdminOnly(dto.getAdminOnly());
        menu.setCached(dto.getCached());
        menu.setCacheName(dto.getCacheName());
        menu.setId(null);
        if (menu.getParentId() != null && menuMapper.selectById(menu.getParentId()) == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Parent menu not found: " + menu.getParentId());
        }
        menu.setCreatedTime(LocalDateTime.now());
        menu.setUpdatedTime(LocalDateTime.now());
        menuMapper.insert(menu);
        return menu;
    }

    /** 更新菜单（字段非空才覆盖；parentId 环校验） */
    public SysMenu update(Long id, SysMenuDTO dto) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Menu not found: " + id);
        }
        if (dto.getParentId() != null && !dto.getParentId().equals(menu.getId())) {
            if (menuMapper.selectById(dto.getParentId()) == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Parent menu not found: " + dto.getParentId());
            }
            if (wouldCreateCycle(dto.getParentId(), menu.getId())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Cannot move menu under its own descendant (cycle detected)");
            }
            menu.setParentId(dto.getParentId());
        }
        if (StringUtils.hasText(dto.getMenuName())) menu.setMenuName(dto.getMenuName());
        if (dto.getMenuType() != null) menu.setMenuType(dto.getMenuType());
        if (StringUtils.hasText(dto.getTitle())) menu.setTitle(dto.getTitle());
        if (StringUtils.hasText(dto.getPath())) menu.setPath(dto.getPath());
        if (StringUtils.hasText(dto.getComponent())) menu.setComponent(dto.getComponent());
        if (StringUtils.hasText(dto.getPerms())) menu.setPerms(dto.getPerms());
        if (StringUtils.hasText(dto.getIcon())) menu.setIcon(dto.getIcon());
        if (dto.getSort() != null) menu.setSort(dto.getSort());
        if (dto.getVisible() != null) menu.setVisible(dto.getVisible());
        if (dto.getStatus() != null) menu.setStatus(dto.getStatus());
        if (dto.getAdminOnly() != null) menu.setAdminOnly(dto.getAdminOnly());
        if (dto.getCached() != null) menu.setCached(dto.getCached());
        if (dto.getCacheName() != null) menu.setCacheName(dto.getCacheName());
        menu.setUpdatedTime(LocalDateTime.now());
        menuMapper.updateById(menu);
        return menu;
    }

    /** 切换状态（1 启用 / 0 停用） */
    public SysMenu toggleStatus(Long id, Integer status) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Menu not found: " + id);
        }
        menu.setStatus(status);
        menu.setUpdatedTime(LocalDateTime.now());
        menuMapper.updateById(menu);
        return menu;
    }

    /** 删除菜单（有子节点则拒绝） */
    public void delete(Long id) {
        if (menuMapper.selectById(id) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Menu not found: " + id);
        }
        Long children = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, id));
        if (children > 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Menu has children, please delete them first");
        }
        menuMapper.deleteById(id);
    }

    /**
     * 沿 parent 链上溯检测环：新父节点若是自身或自身子孙。
     */
    private boolean wouldCreateCycle(Long candidateParentId, Long nodeId) {
        Set<Long> seen = new HashSet<>();
        Long cur = candidateParentId;
        while (cur != null) {
            if (cur.equals(nodeId)) {
                return true;
            }
            if (!seen.add(cur)) {
                return true;
            }
            SysMenu parent = menuMapper.selectById(cur);
            if (parent == null || parent.getParentId() == null) {
                break;
            }
            cur = parent.getParentId();
        }
        return false;
    }
}
