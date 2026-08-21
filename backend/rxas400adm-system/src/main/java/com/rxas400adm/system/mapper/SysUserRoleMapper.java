package com.rxas400adm.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rxas400adm.system.entity.SysUserRole;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    int insertBatch(@Param("list") List<SysUserRole> userRoles);
}
