package com.rxas400adm.as400;

import com.ibm.as400.access.AS400;
import com.rxas400adm.as400.model.UserProfileRow;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * JTOpen AuthClient 委托实现（AS400 用户凭据认证 / profile 查询）。
 */
@Slf4j
class JTOpenAuthClient implements AuthClient {

    private final JTOpenConnectionState state;

    JTOpenAuthClient(JTOpenConnectionState state) {
        this.state = state;
    }

    @Override
    public boolean authenticate(String username, String password) {
        if (state.host == null || state.host.isBlank() || username == null || username.isBlank()) {
            return false;
        }
        AS400 system = new AS400(state.host, username, password == null ? "" : password);
        try {
            system.validateSignon();
            return true;
        } catch (Exception e) {
            log.warn("AS400 用户认证失败(host={}, user={}): {}", state.host, username, state.redact(e.getMessage()));
            return false;
        } finally {
            system.disconnectAllServices();
        }
    }

    @Override
    public UserProfileRow userProfile(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        Map<String, Object> row = new JTOpenSqlClient(state).queryList(
                "SELECT USER_NAME, GROUP_PROFILE, STATUS FROM QSYS2.USER_INFO WHERE USER_NAME = ?",
                username.toUpperCase()).stream().findFirst().orElse(null);
        if (row == null || row.isEmpty()) {
            return null;
        }
        return new UserProfileRow(str(row, "USER_NAME"), str(row, "GROUP_PROFILE"), str(row, "STATUS"));
    }

    private static String str(Map<String, Object> r, String key) {
        Object v = r.get(key);
        return v == null ? "" : String.valueOf(v);
    }
}