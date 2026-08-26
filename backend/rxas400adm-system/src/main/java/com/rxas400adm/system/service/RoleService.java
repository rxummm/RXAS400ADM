package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.dto.SysRoleDTO;
import com.rxas400adm.system.entity.SysMenu;
import com.rxas400adm.system.entity.SysRole;
import com.rxas400adm.system.entity.SysUserRole;
import com.rxas400adm.system.mapper.SysMenuMapper;
import com.rxas400adm.system.mapper.SysRoleMapper;
import com.rxas400adm.system.entity.SysRoleMenu;
import com.rxas400adm.system.mapper.SysRoleMenuMapper;
import com.rxas400adm.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 角色管理（参照旧项目 SysRoleService）：
 * - 角色 CRUD（含排序/启用停用）
 * - 角色-菜单授权（rx_role_menu）：菜单变更后清权限缓存，非 admin 用户下次拉菜单即时生效
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService implements IRoleService {

    private static final String ADMIN_CODE = "ADMIN";

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuMapper menuMapper;

    /** 角色列表（含各角色已授权菜单 ID，供前端回显） */
    public List<SysRole> listAll() {
        List<SysRole> roles = roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getSort));
        fillMenuIds(roles);
        return roles;
    }

    /** 分页查询（管理页） */
    public PageResult<SysRole> page(long current, long size, String keyword) {
        LambdaQueryWrapper<SysRole> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(SysRole::getRoleName, keyword).or().like(SysRole::getRoleCode, keyword));
        }
        qw.orderByAsc(SysRole::getSort);
        // 分页边界统一（PageConstants）：避免 size 超大查询拖垮数据库
        var page = roleMapper.selectPage(
                new Page<>(PageConstants.clampNum(current), PageConstants.clampSize(size)), qw);
        fillMenuIds(page.getRecords());
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    /** P2-11：角色菜单授权批量加载（一次 IN 查询 + 分组，替代逐角色 1 次查询的 N+1） */
    private void fillMenuIds(List<SysRole> roles) {
        if (roles.isEmpty()) return;
        List<Long> roleIds = roles.stream().map(SysRole::getId).toList();
        Map<Long, List<Long>> grouped = roleMenuMapper.selectList(
                        new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds))
                .stream()
                .collect(Collectors.groupingBy(
                        SysRoleMenu::getRoleId,
                        Collectors.mapping(SysRoleMenu::getMenuId,
                                Collectors.toList())));
        roles.forEach(r -> r.setMenuIds(grouped.getOrDefault(r.getId(), List.of())));
    }

    
    public SysRole create(SysRoleDTO dto) {
        SysRole role = new SysRole();
        role.setRoleCode(dto.getRoleCode());
        role.setRoleName(dto.getRoleName());
        role.setDescription(dto.getDescription());
        role.setSort(dto.getSort());
        role.setStatus(dto.getStatus());
        role.setMenuIds(dto.getMenuIds());
        long exists = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, role.getRoleCode()));
        if (exists > 0) {
            throw new BusinessException(ErrorCode.ROLE_CODE_EXISTS, "角色编码已存在: " + role.getRoleCode());
        }
        if (role.getSort() == null) role.setSort(0);
        if (role.getStatus() == null) role.setStatus(1);
        // T1：先校验后写——menuIds 含不存在项在插入角色前即拒绝，杜绝「删旧成功、插新失败」的权限丢失
        List<Long> validMenuIds = validatedMenuIds(role.getMenuIds());
        role.setId(null);
        roleMapper.insert(role);
        saveMenuIds(role.getId(), validMenuIds);
        return role;
    }

    
    public SysRole update(Long id, SysRoleDTO dto) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND, "角色不存在: " + id);
        }
        // ADMIN 角色不可停用（防止把自己锁死）
        if (ADMIN_CODE.equals(role.getRoleCode()) && dto.getStatus() != null && dto.getStatus() == 0) {
            throw new BusinessException(ErrorCode.ROLE_ADMIN_PROTECTED, "内置 ADMIN 角色不可停用");
        }
        if (StringUtils.hasText(dto.getRoleName())) role.setRoleName(dto.getRoleName());
        if (StringUtils.hasText(dto.getDescription())) role.setDescription(dto.getDescription());
        if (dto.getSort() != null) role.setSort(dto.getSort());
        if (dto.getStatus() != null) role.setStatus(dto.getStatus());
        roleMapper.updateById(role);
        // 菜单与权限（menuIds 传 null 时跳过重建，空数组=清空该角色权限）
        // 菜单查询实时查库，授权变更用户下次请求 /auth/menu 即时生效
        if (dto.getMenuIds() != null) {
            // T1：校验前置于删除——insertBatch 中途失败不再导致该角色授权全部丢失
            List<Long> validMenuIds = validatedMenuIds(dto.getMenuIds());
            roleMenuMapper.deleteByRoleId(id);
            saveMenuIds(id, validMenuIds);
        }
        return role;
    }

    
    public void delete(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND, "角色不存在: " + id);
        }
        if (ADMIN_CODE.equals(role.getRoleCode())) {
            throw new BusinessException(ErrorCode.ROLE_ADMIN_PROTECTED, "内置 ADMIN 角色不可删除");
        }
        roleMenuMapper.deleteByRoleId(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
        roleMapper.deleteById(id);
    }

    /** 批量删除（跳过内置 ADMIN，任一含 ADMIN 即整体拒绝） */
    
    public int batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        List<Long> distinctIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            return 0;
        }
        long adminCount = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getId, distinctIds).eq(SysRole::getRoleCode, ADMIN_CODE));
        if (adminCount > 0) {
            throw new BusinessException(ErrorCode.ROLE_ADMIN_PROTECTED, "内置 ADMIN 角色不可删除");
        }
        // B7：批量删除改为 IN 条件，消除逐条 delete 的 N 次 DB 往返
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, distinctIds));
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getRoleId, distinctIds));
        roleMapper.delete(new LambdaQueryWrapper<SysRole>().in(SysRole::getId, distinctIds));
        return distinctIds.size();
    }

    private void saveMenuIds(Long roleId, List<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        roleMenuMapper.insertBatch(roleId, menuIds.stream().filter(Objects::nonNull).distinct().toList());
    }

    /**
     * T1：menuIds 存在性校验——含不存在项抛 BAD_REQUEST 并列出非法 id。
     * 必须在任何 delete/insert 之前调用（无事务架构下删除不可回滚）。
     */
    private List<Long> validatedMenuIds(List<Long> menuIds) {
        List<Long> ids = menuIds == null ? List.of()
                : menuIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return ids;
        }
        List<SysMenu> existing = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .select(SysMenu::getId)
                .in(SysMenu::getId, ids));
        java.util.Set<Long> existingIds = existing.stream().map(SysMenu::getId).collect(Collectors.toSet());
        List<Long> invalid = ids.stream().filter(i -> !existingIds.contains(i)).toList();
        if (!invalid.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "菜单不存在: " + invalid);
        }
        return ids;
    }
}