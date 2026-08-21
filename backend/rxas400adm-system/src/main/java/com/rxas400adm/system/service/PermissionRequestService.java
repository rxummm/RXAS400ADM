package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.event.UserPermissionGrantedEvent;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.response.PageResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rxas400adm.system.entity.PermissionRequest;
import com.rxas400adm.system.entity.SysMenu;
import com.rxas400adm.system.entity.SysPermission;
import com.rxas400adm.system.entity.SysRole;
import com.rxas400adm.system.entity.SysRolePermission;
import com.rxas400adm.system.entity.SysUserRole;
import com.rxas400adm.system.entity.SysUserMenu;
import com.rxas400adm.system.mapper.PermissionRequestMapper;
import com.rxas400adm.system.mapper.SysMenuMapper;
import com.rxas400adm.system.mapper.SysPermissionMapper;
import com.rxas400adm.system.mapper.SysRoleMapper;
import com.rxas400adm.system.mapper.SysRolePermissionMapper;
import com.rxas400adm.system.mapper.SysUserMapper;
import com.rxas400adm.system.mapper.SysUserMenuMapper;
import com.rxas400adm.system.mapper.SysUserRoleMapper;
import com.rxas400adm.system.entity.SysUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限自助申请（rx_permission_request）：用户可申请权限码或菜单/按钮（menuIds 菜单树）。
 * 通过后：菜单/按钮 → 写入 rx_user_menu 直接授权；权限码 → 绑定 REQUESTED 角色。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionRequestService implements IPermissionRequestService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final PermissionRequestMapper requestMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuMapper menuMapper;
    private final SysUserMenuMapper userMenuMapper;
    private final INotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 提交申请：支持两种模式
     * - 菜单树模式：menuIds + menuNames（推荐，参照旧项目）
     * - 权限码模式：permissionCode（旧方式兼容）
     */
    
    public PermissionRequest create(String username, String permissionCode, List<Long> menuIds,
                                    List<String> menuNames, String reason) {
        boolean menuMode = menuIds != null && !menuIds.isEmpty();
        if (!menuMode && !StringUtils.hasText(permissionCode)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要申请的菜单或填写权限码");
        }
        long dup = 0;
        if (menuMode) {
            String idsJson = toJson(menuIds);
            dup = requestMapper.selectCount(new LambdaQueryWrapper<PermissionRequest>()
                    .eq(PermissionRequest::getUsername, username)
                    .eq(PermissionRequest::getMenuIds, idsJson)
                    .eq(PermissionRequest::getStatus, "PENDING"));
        } else if (StringUtils.hasText(permissionCode)) {
            dup = requestMapper.selectCount(new LambdaQueryWrapper<PermissionRequest>()
                    .eq(PermissionRequest::getUsername, username)
                    .eq(PermissionRequest::getPermissionCode, permissionCode.trim())
                    .eq(PermissionRequest::getStatus, "PENDING"));
        }
        if (dup > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该权限已有待审批申请，请勿重复提交");
        }
        PermissionRequest request = new PermissionRequest();
        request.setUsername(username);
        if (menuMode) {
            request.setMenuIds(toJson(new LinkedHashSet<>(menuIds)));
            request.setMenuNames(toJson(menuNames));
        } else {
            request.setPermissionCode(permissionCode.trim().toUpperCase());
        }
        request.setReason(reason);
        request.setStatus("PENDING");
        request.setCreatedTime(LocalDateTime.now());
        request.setUpdatedTime(LocalDateTime.now());
        requestMapper.insert(request);
        return request;
    }

    public PageResult<PermissionRequest> mine(String username, int current, int size) {
        LambdaQueryWrapper<PermissionRequest> wrapper = new LambdaQueryWrapper<PermissionRequest>()
                .eq(PermissionRequest::getUsername, username);
        wrapper.orderByDesc(PermissionRequest::getCreatedTime);
        Page<PermissionRequest> page = requestMapper.selectPage(
                new Page<>(PageConstants.clampNum(current), PageConstants.clampSize(size)), wrapper);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public PageResult<PermissionRequest> adminPage(int current, int size, String status, String keyword) {
        LambdaQueryWrapper<PermissionRequest> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(PermissionRequest::getStatus, status.trim());
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(q -> q.like(PermissionRequest::getUsername, kw)
                    .or().like(PermissionRequest::getPermissionCode, kw));
        }
        wrapper.orderByDesc(PermissionRequest::getCreatedTime);
        Page<PermissionRequest> page = requestMapper.selectPage(
                new Page<>(PageConstants.clampNum(current), PageConstants.clampSize(size)), wrapper);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    /** 待审批数量（顶栏角标） */
    public long pendingCount() {
        return requestMapper.selectCount(new LambdaQueryWrapper<PermissionRequest>()
                .eq(PermissionRequest::getStatus, "PENDING"));
    }

    
    public PermissionRequest approve(Long id, String approver, String comment) {
        PermissionRequest request = requirePending(id);
        String grantText;
        if (StringUtils.hasText(request.getMenuIds())) {
            // 菜单树模式：写入 rx_user_menu 直接授权（含目录/按钮，自动补全子孙按钮）
            List<Long> menuIds = fromJson(request.getMenuIds(), new TypeReference<List<Long>>() {});
            grantMenus(request.getUsername(), menuIds);
            grantText = displayMenuNames(request.getMenuNames());
        } else {
            grantPermission(request.getUsername(), request.getPermissionCode());
            grantText = request.getPermissionCode();
        }
        request.setStatus("APPROVED");
        request.setApprover(approver);
        request.setApproveComment(comment);
        request.setUpdatedTime(LocalDateTime.now());
        requestMapper.updateById(request);
        notificationService.send(request.getUsername(), "PERMISSION",
                "权限申请已通过",
                "您申请的权限「" + grantText + "」已审批通过，可重新登录后使用。");
        eventPublisher.publishEvent(new UserPermissionGrantedEvent(request.getUsername()));
        return request;
    }

    
    public PermissionRequest reject(Long id, String approver, String comment) {
        PermissionRequest request = requirePending(id);
        request.setStatus("REJECTED");
        request.setApprover(approver);
        request.setApproveComment(comment);
        request.setUpdatedTime(LocalDateTime.now());
        requestMapper.updateById(request);
        notificationService.send(request.getUsername(), "PERMISSION",
                "权限申请被驳回",
                "您申请的权限「" + request.getPermissionCode() + "」被驳回"
                        + (StringUtils.hasText(comment) ? "，意见：" + comment : "") + "。");
        return request;
    }

    /**
     * 菜单树模式授权：把勾选的菜单/按钮写入用户直接授权（rx_user_menu，幂等）。
     * 若用户未精确选择按钮，则自动补全勾选菜单下的子孙按钮（参照旧项目）。
     */
    private void grantMenus(String username, List<Long> menuIds) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (user == null) return;
        List<SysMenu> allMenus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getStatus, 1));
        // 是否已精确选择按钮（menu_type=3）：先一次性收集按钮 id 集合，避免双层 anyMatch 的 O(n×m)
        Set<Long> buttonIds = allMenus.stream()
                .filter(m -> m.getMenuType() != null && m.getMenuType() == 3)
                .map(SysMenu::getId)
                .collect(Collectors.toSet());
        boolean hasButton = menuIds.stream().anyMatch(buttonIds::contains);
        Set<Long> grantIds = new LinkedHashSet<>();
        for (Long menuId : menuIds) {
            grantIds.add(menuId);
            if (!hasButton) {
                collectDescendantButtons(allMenus, menuId, grantIds);
            }
        }
        for (Long menuId : grantIds) {
            if (menuMapper.selectById(menuId) == null) continue;
            long exists = userMenuMapper.selectCount(new LambdaQueryWrapper<SysUserMenu>()
                    .eq(SysUserMenu::getUserId, user.getId())
                    .eq(SysUserMenu::getMenuId, menuId));
            if (exists == 0) {
                SysUserMenu um = new SysUserMenu();
                um.setUserId(user.getId());
                um.setMenuId(menuId);
                um.setCreatedTime(LocalDateTime.now());
                userMenuMapper.insert(um);
            }
        }
    }

    private void collectDescendantButtons(List<SysMenu> allMenus, Long parentId, Set<Long> result) {
        for (SysMenu m : allMenus) {
            if (parentId.equals(m.getParentId())) {
                if (m.getMenuType() != null && m.getMenuType() == 3) {
                    result.add(m.getId());
                }
                collectDescendantButtons(allMenus, m.getId(), result);
            }
        }
    }

    private String displayMenuNames(String menuNamesJson) {
        if (!StringUtils.hasText(menuNamesJson)) return "";
        try {
            List<String> names = OBJECT_MAPPER.readValue(menuNamesJson, new TypeReference<List<String>>() {});
            return String.join("、", names);
        } catch (Exception e) {
            return menuNamesJson;
        }
    }

    private String toJson(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "参数序列化失败");
        }
    }

    private <T> T fromJson(String json, TypeReference<T> type) {
        try {
            return OBJECT_MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "参数解析失败");
        }
    }

    /** 把权限码绑定到 REQUESTED 角色并加入申请人（幂等） */
    private void grantPermission(String username, String permissionCode) {
        // 权限码不存在则创建
        SysPermission permission = permissionMapper.selectOne(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getPermissionCode, permissionCode));
        if (permission == null) {
            permission = new SysPermission();
            permission.setPermissionCode(permissionCode);
            permission.setPermissionName(permissionCode);
            permission.setModule("REQUESTED");
            permissionMapper.insert(permission);
        }
        // REQUESTED 角色不存在则创建
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, REQUESTED_ROLE));
        if (role == null) {
            role = new SysRole();
            role.setRoleCode(REQUESTED_ROLE);
            role.setRoleName("自助申请权限");
            roleMapper.insert(role);
        }
        // 角色-权限绑定（幂等）
        long rpCount = rolePermissionMapper.selectCount(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, role.getId())
                .eq(SysRolePermission::getPermissionId, permission.getId()));
        if (rpCount == 0) {
            SysRolePermission rp = new SysRolePermission();
            rp.setRoleId(role.getId());
            rp.setPermissionId(permission.getId());
            rolePermissionMapper.insert(rp);
        }
        // 用户-角色绑定（幂等）
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (user != null) {
            long urCount = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getUserId, user.getId())
                    .eq(SysUserRole::getRoleId, role.getId()));
            if (urCount == 0) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(user.getId());
                ur.setRoleId(role.getId());
                userRoleMapper.insert(ur);
            }
        }
    }

    private PermissionRequest requirePending(Long id) {
        PermissionRequest request = requestMapper.selectById(id);
        if (request == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "申请记录不存在: " + id);
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该申请已处理，不能重复审批");
        }
        return request;
    }
}