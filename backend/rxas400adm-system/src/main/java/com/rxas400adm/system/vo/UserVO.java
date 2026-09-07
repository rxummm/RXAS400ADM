package com.rxas400adm.system.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户视图对象：含角色列表与登录来源标记（PLATFORM / AS400）。
 */
@Data
@Builder
public class UserVO {

    private Long id;

    private String username;

    private String email;

    /** ACTIVE / DISABLED */
    private String status;

    /** 登录来源：PLATFORM / AS400 */
    private String loginSource;

    /** AS400 登录来源的服务器 ID */
    private Long as400ServerId;

    private List<SysRoleVO> roles;

    private LocalDateTime createdTime;
}
