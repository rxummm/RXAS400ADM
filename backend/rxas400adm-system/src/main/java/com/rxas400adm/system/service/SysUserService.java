package com.rxas400adm.system.service;

import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.dto.UserDTO;
import com.rxas400adm.system.dto.UserUpdateDTO;
import com.rxas400adm.system.entity.SysUser;
import com.rxas400adm.system.vo.UserVO;

import java.util.List;

public interface SysUserService {

    SysUser getByUsername(String username);

    /** 获取用户，不存在则抛出 NOT_FOUND */
    SysUser requireByUsername(String username);

    PageResult<UserVO> page(long current, long size, String keyword);

    UserVO create(UserDTO dto);

    UserVO update(Long id, UserUpdateDTO dto);

    void delete(Long id);

    /** 查询用户拥有的权限码（含角色映射），用于 RBAC */
    List<String> listPermissions(Long userId);

    /** 修改用户密码（已加密密文） */
    void updatePassword(String username, String encodedPassword);

    /**
     * 修改当前用户密码：校验旧密码、密码强度、新旧一致性。
     * 业务逻辑从 AuthController.changePassword() 下沉。
     */
    void changePassword(String username, String oldPassword, String newPassword);
}
