package com.rxas400adm.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rxas400adm.system.entity.SysUserMenu;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 用户-菜单直接授权 Mapper（rx_user_menu）。
 * SQL 已迁至 resources/mapper/SysUserMenuMapper.xml
 */
public interface SysUserMenuMapper extends BaseMapper<SysUserMenu> {

    /** 查询用户直接授权的菜单 ID 列表 */
    List<Long> selectMenuIdsByUserId(@Param("userId") Long userId);

    /** 清空用户所有直接授权 */
    void deleteByUserId(@Param("userId") Long userId);

    /** P6 批量插入用户-菜单直接授权（多值 INSERT，一次 DB 往返） */
    int insertBatch(@Param("list") List<SysUserMenu> list);

    /** P6 批量移除用户指定菜单授权（IN 条件一次删除，替代逐条 delete） */
    int deleteByUserIdAndMenuIds(@Param("userId") Long userId, @Param("menuIds") Collection<Long> menuIds);
}