package com.rxas400adm.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单表（rx_menu，参照旧项目 sys_menu）：驱动前端动态菜单。
 * - status: 1 启用 / 0 停用（停用=菜单隐藏，保留路由与接口）
 * - visible: 1 可见 / 0 隐藏（管理端仍可维护）
 * - admin_only: 1 仅管理员可见
 * - menu_type: 1 目录 / 2 菜单 / 3 按钮
 * - title: i18n key（前端 $t('menu.' + title) 渲染）
 * - perms: 关联权限码（菜单按权限裁剪）
 */
@Data
@TableName("rx_menu")
public class SysMenu {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String menuName;

    /** 1 目录 / 2 菜单 / 3 按钮 */
    private Integer menuType;

    /** i18n key，如 dashboard */
    private String title;

    private String path;

    /** 前端组件名（vue-router 懒加载路径，如 views/Dashboard.vue） */
    private String component;

    /** 权限码，如 MONITOR_VIEW */
    private String perms;

    private String icon;

    private Integer sort;

    /** 1 可见 / 0 隐藏 */
    private Integer visible;

    /** 1 启用 keep-alive 缓存 / 0 不缓存 */
    private Integer cached;

    /** keep-alive 组件名（覆盖前端路由 name），为空时取前端路由 name */
    private String cacheName;

    /** 1 启用 / 0 停用（停用=前端菜单隐藏） */
    private Integer status;

    /** 1 仅管理员可见 */
    private Integer adminOnly;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    /** 树形子节点（非数据库字段） */
    @TableField(exist = false)
    private List<SysMenu> children;
}
