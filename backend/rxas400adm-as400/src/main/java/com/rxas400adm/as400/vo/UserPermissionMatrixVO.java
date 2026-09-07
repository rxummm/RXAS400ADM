package com.rxas400adm.as400.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A3 用户权限矩阵 VO
 */
@Data
public class UserPermissionMatrixVO {

    /** 用户名 */
    private String userName;

    /** 用户状态 */
    private String status;

    /** 角色列表 */
    private List<String> roles;

    /** 权限列表 */
    private List<String> permissions;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 密码过期天数 */
    private Integer passwordExpireDays;

    /** 权限变更记录数 */
    private Long changeCount;
}
