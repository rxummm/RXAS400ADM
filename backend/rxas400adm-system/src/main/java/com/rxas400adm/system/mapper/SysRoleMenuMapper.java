package com.rxas400adm.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rxas400adm.system.entity.SysRoleMenu;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色-菜单授权（rx_role_menu，参照旧项目 SysRoleMenuMapper）
 * SQL 已迁至 resources/mapper/SysRoleMenuMapper.xml
 */
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {

    int deleteByRoleId(@Param("roleId") Long roleId);

    int insertBatch(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);

    List<Long> selectMenuIdsByRoleIds(@Param("roleIds") List<Long> roleIds);
}