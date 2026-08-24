package com.rxas400adm.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rxas400adm.system.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 根据角色编码查询用户列表
     * @param roleCode 角色编码（如 ADMIN）
     * @return 该角色下的所有用户
     */
    @Select("""
            SELECT DISTINCT u.* FROM rx_user u
            INNER JOIN rx_user_role ur ON u.id = ur.user_id
            INNER JOIN rx_role r ON ur.role_id = r.id
            WHERE r.role_code = #{roleCode}
            AND u.status = 'ACTIVE'
            """)
    List<SysUser> selectUsersByRoleCode(@Param("roleCode") String roleCode);
}
