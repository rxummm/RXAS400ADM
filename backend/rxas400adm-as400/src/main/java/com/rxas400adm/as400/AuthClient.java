package com.rxas400adm.as400;

import com.rxas400adm.as400.model.UserProfileListRow;
import com.rxas400adm.as400.model.UserProfileRow;

import java.util.List;

/**
 * 认证域：AS400 user profile 登录验证、组归属查询、用户列表、用户态切换。
 */
public interface AuthClient {

    /**
     * 用用户提供的凭据认证（AS400 user profile 登录用）。
     * Mock 模式返回 true；JT400 模式用给定账号真实登录验证。
     * 认证失败不抛异常，返回 false。
     */
    boolean authenticate(String username, String password);

    /**
     * 查询用户 profile 信息（组归属等，用于组→角色映射与每日同步）。
     * 用户不存在返回 null。
     */
    UserProfileRow userProfile(String username);

    /**
     * 用户 profile 列表（QSYS2.USER_INFO）。
     */
    default List<UserProfileListRow> listUserProfiles() {
        return List.of();
    }

    /**
     * 切换用户态（SWITCHUSR CL 命令）。
     */
    default CommandResult switchUser(String targetUser) {
        return CommandResult.fail("用户态切换未实现");
    }
}
