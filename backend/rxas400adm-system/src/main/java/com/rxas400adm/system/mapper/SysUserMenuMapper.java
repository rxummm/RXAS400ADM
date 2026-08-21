package com.rxas400adm.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rxas400adm.system.entity.SysUserMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户-菜单直接授权 Mapper（rx_user_menu）。
 * SQL 已迁至 resources/mapper/SysUserMenuMapper.xml
 */
@Mapper
public interface SysUserMenuMapper extends BaseMapper<SysUserMenu> {

    /** 查询用户直接授权的菜单 ID 列表 */
    List<Long> selectMenuIdsByUserId(@Param("userId") Long userId);

    /** 清空用户所有直接授权 */
    void deleteByUserId(@Param("userId") Long userId);

    /** 移除用户指定菜单授权 */
    void deleteByUserIdAndMenuId(@Param("userId") Long userId, @Param("menuId") Long menuId);
}