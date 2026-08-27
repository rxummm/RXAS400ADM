package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.dto.SysPermissionDTO;
import com.rxas400adm.system.entity.SysMenu;
import com.rxas400adm.system.entity.SysPermission;
import com.rxas400adm.system.entity.SysRolePermission;
import com.rxas400adm.system.mapper.SysMenuMapper;
import com.rxas400adm.system.mapper.SysPermissionMapper;
import com.rxas400adm.system.mapper.SysRolePermissionMapper;
import com.rxas400adm.system.vo.PermissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 权限码管理（rx_permission CRUD + 按菜单匹配的下拉建议）：
 * - 权限码是后端 @PreAuthorize 与前端门控的「钥匙」，统一在此注册维护
 * - suggest(menuTitle)：按菜单业务域过滤（JOB_VIEW→JOB 模块），
 *   供菜单管理页 perms 下拉选择，杜绝手填拼写错误
 * - 删除前检查引用（rx_role_permission 绑定 / rx_menu.perms 使用），被引用则拒绝
 */
@Service
@RequiredArgsConstructor
public class PermissionManageService implements IPermissionManageService {

    /**
     * 菜单 title（i18n key）→ 业务模块前缀（与权限码 lastIndexOf('_') 前缀一致）。
     * 默认值（V25 起由 sys_config 键 permission.menuDomain 动态维护，配置缺失/解析失败时回退此表）。
     */
    private static final Map<String, List<String>> DEFAULT_MENU_DOMAIN = Map.ofEntries(
            Map.entry("jobs", List.of("JOB")),
            Map.entry("schedules", List.of("SCHEDULE")),
            Map.entry("scripts", List.of("SCRIPT")),
            Map.entry("executions", List.of("EXECUTION")),
            Map.entry("monitor", List.of("MONITOR")),
            Map.entry("health", List.of("HEALTH")),
            Map.entry("query", List.of("QUERY")),
            Map.entry("objects", List.of("OBJECT")),
            Map.entry("ifs", List.of("IFS")),
            Map.entry("subsystems", List.of("SUBSYSTEM")),
            Map.entry("pf", List.of("PF")),
            Map.entry("topology", List.of("TOPOLOGY")),
            Map.entry("reports", List.of("REPORT")),
            Map.entry("docs", List.of("DOC")),
            Map.entry("audit", List.of("AUDIT")),
            Map.entry("loginLog", List.of("AUDIT")),
            Map.entry("users", List.of("USER")),
            Map.entry("roles", List.of("ROLE")),
            Map.entry("menus", List.of("MENU")),
            Map.entry("dict", List.of("DICT")),
            Map.entry("notices", List.of("NOTICE")),
            Map.entry("notifications", List.of("NOTIFICATION")),
            Map.entry("webhooks", List.of("WEBHOOK")),
            Map.entry("config", List.of("SYS_CONFIG")),
            Map.entry("cache", List.of("SYS_CACHE")),
            Map.entry("tasks", List.of("SYS_TASK")),
            Map.entry("ipRules", List.of("SYS_IP")),
            Map.entry("permissionRequest", List.of("SYS_PERMISSION")),
            Map.entry("permissions", List.of("PERMISSION")),
            Map.entry("i18n", List.of("I18N")),
            Map.entry("region", List.of("REGION")),
            Map.entry("calendar", List.of("CALENDAR")),
            Map.entry("source", List.of("SOURCE", "COMPILE")),
            Map.entry("assets", List.of("AS400")),
            Map.entry("bizData", List.of("QUERY")),
            Map.entry("tableFields", List.of("QUERY", "PF")),
            Map.entry("messageFiles", List.of("MSGF")),
            Map.entry("sysvals", List.of("SYSVAL")),
            Map.entry("serverCompare", List.of("MONITOR")),
            Map.entry("inspection", List.of("MONITOR", "INSPECT")),
            Map.entry("jobSla", List.of("SLA", "JOB")),
            Map.entry("jobDependency", List.of("JOB")));

    private static final String MENU_DOMAIN_KEY = "permission.menuDomain";

    private final SysPermissionMapper permissionMapper;
    private final SysMenuMapper menuMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final ISysConfigService configService;
    private final ObjectMapper objectMapper;

    /** 模块 = 权限码最后一个下划线之前的部分（JOB_VIEW→JOB，SYS_CONFIG_MANAGE→SYS_CONFIG） */
    public static String deriveModule(String code) {
        if (code == null) return "SYSTEM";
        int idx = code.lastIndexOf('_');
        return idx > 0 ? code.substring(0, idx) : "SYSTEM";
    }

    /** 分页查询（含被引用次数：菜单 perms + 角色绑定） */
    public PageResult<PermissionVO> page(String keyword, String module, long current, long size) {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<SysPermission>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(SysPermission::getPermissionCode, keyword)
                        .or().like(SysPermission::getPermissionName, keyword)
                        .or().like(SysPermission::getDescription, keyword))
                .like(StringUtils.hasText(module), SysPermission::getModule, module)
                .orderByAsc(SysPermission::getModule, SysPermission::getPermissionCode);
        Page<SysPermission> page = permissionMapper.selectPage(
                new Page<>(PageConstants.clampNum(current), PageConstants.clampSize(size)), wrapper);
        UsageMap usage = loadUsageMaps();
        List<PermissionVO> records = page.getRecords().stream()
                .map(p -> toVO(p, usage)).toList();
        return new PageResult<>(page.getTotal(), records);
    }

    /** 全部权限码（下拉/字典用） */
    public List<PermissionVO> listAll() {
        UsageMap usage = loadUsageMaps();
        return permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                        .orderByAsc(SysPermission::getModule, SysPermission::getPermissionCode))
                .stream().map(p -> toVO(p, usage)).toList();
    }

    /**
     * 菜单业务域映射：优先读 sys_config（permission.menuDomain，JSON），失败/缺失回退默认表。
     * 每次读取（下拉建议场景低频，无需缓存）；在系统配置页可动态增删页面→模块映射。
     */
    private Map<String, List<String>> menuDomain() {
        String json = configService.get(MENU_DOMAIN_KEY, null);
        if (StringUtils.hasText(json)) {
            try {
                return objectMapper.readValue(json, new TypeReference<Map<String, List<String>>>() {
                });
            } catch (Exception ignored) {
                // 配置损坏时回退默认表，不阻塞下拉建议
            }
        }
        return DEFAULT_MENU_DOMAIN;
    }

    /**
     * 按菜单业务域过滤的建议码（菜单管理页 perms 下拉用）：
     * - menuTitle 命中映射 → 仅返回该域权限码；未命中 → 全部（兜底）
     * - keyword 可选模糊过滤
     */
    public List<PermissionVO> suggest(String menuTitle, String keyword) {
        List<String> modules = StringUtils.hasText(menuTitle)
                ? menuDomain().getOrDefault(menuTitle, List.of()) : List.of();
        UsageMap usage = loadUsageMaps();
        return permissionMapper.selectList(null).stream()
                .filter(p -> modules.isEmpty() || modules.contains(deriveModule(p.getPermissionCode())))
                .filter(p -> !StringUtils.hasText(keyword)
                        || p.getPermissionCode().toLowerCase().contains(keyword.toLowerCase())
                        || (p.getPermissionName() != null
                        && p.getPermissionName().toLowerCase().contains(keyword.toLowerCase())))
                .map(p -> toVO(p, usage))
                .toList();
    }

    /** 新增（权限码唯一） */
    public SysPermission create(SysPermissionDTO dto) {
        if (!StringUtils.hasText(dto.getPermissionCode())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "权限码不能为空");
        }
        String code = dto.getPermissionCode().trim().toUpperCase();
        long exists = permissionMapper.selectCount(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getPermissionCode, code));
        if (exists > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "权限码已存在: " + code);
        }
        SysPermission p = new SysPermission();
        p.setPermissionCode(code);
        p.setPermissionName(StringUtils.hasText(dto.getPermissionName()) ? dto.getPermissionName() : code);
        p.setModule(StringUtils.hasText(dto.getModule()) ? dto.getModule() : deriveModule(code));
        p.setDescription(dto.getDescription());
        permissionMapper.insert(p);
        return p;
    }

    public SysPermission update(Long id, SysPermissionDTO dto) {
        SysPermission p = permissionMapper.selectById(id);
        if (p == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "权限码不存在: " + id);
        }
        if (StringUtils.hasText(dto.getPermissionName())) p.setPermissionName(dto.getPermissionName());
        if (StringUtils.hasText(dto.getModule())) p.setModule(dto.getModule());
        p.setDescription(dto.getDescription());
        permissionMapper.updateById(p);
        return p;
    }

    /** 删除（被菜单/角色绑定引用则拒绝） */
    public void delete(Long id) {
        SysPermission p = permissionMapper.selectById(id);
        if (p == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "权限码不存在: " + id);
        }
        long menuUsed = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getPerms, p.getPermissionCode()));
        long roleUsed = rolePermissionMapper.selectCount(
                new LambdaQueryWrapper<SysRolePermission>()
                        .eq(SysRolePermission::getPermissionId, id));
        if (menuUsed > 0 || roleUsed > 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "权限码 " + p.getPermissionCode() + " 正在被使用（菜单 " + menuUsed + " 处 / 角色绑定 " + roleUsed + " 处），不能删除");
        }
        permissionMapper.deleteById(id);
    }

    /** P2-11：引用计数批量加载（一次查出全表分组，避免逐权限码 2 次 count 的 N+1） */
    private record UsageMap(Map<String, Long> menuUsage, Map<Long, Long> roleUsage) {
    }

    private UsageMap loadUsageMaps() {
        // 菜单 perms 引用：rx_menu 全量很小（90 行），按 perms 分组计数
        Map<String, Long> menuUsage = menuMapper.selectList(
                        new LambdaQueryWrapper<SysMenu>().isNotNull(SysMenu::getPerms))
                .stream()
                .filter(m -> m.getPerms() != null && !m.getPerms().isBlank())
                .collect(Collectors.groupingBy(SysMenu::getPerms, Collectors.counting()));
        // 角色绑定引用：rx_role_permission 全量很小，按 permission_id 分组计数
        Map<Long, Long> roleUsage = rolePermissionMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(
                        SysRolePermission::getPermissionId,
                        Collectors.counting()));
        return new UsageMap(menuUsage, roleUsage);
    }

    private PermissionVO toVO(SysPermission p, UsageMap usage) {
        return new PermissionVO(
                p.getId(),
                p.getPermissionCode(),
                p.getPermissionName(),
                p.getModule(),
                p.getDescription(),
                usage.menuUsage().getOrDefault(p.getPermissionCode(), 0L),
                usage.roleUsage().getOrDefault(p.getId(), 0L));
    }
}