package com.rxas400adm.as400;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.CommandCall;
import com.rxas400adm.as400.model.UserProfileListRow;
import com.rxas400adm.as400.model.UserProfileRow;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static com.rxas400adm.as400.JTOpenConnectionState.str;

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
            // 【E13】清理异常不得覆盖认证结果语义：disconnect 失败仅忽略
            try {
                system.disconnectAllServices();
            } catch (Exception ignored) {
                // 连接清理失败不影响 authenticate 返回值
            }
        }
    }

    @Override
    public UserProfileRow userProfile(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        Map<String, Object> row = new JTOpenSqlClient(state).queryList(
                SqlStatementRegistry.of("auth.user.query"),
                username.toUpperCase()).stream().findFirst().orElse(null);
        if (row == null || row.isEmpty()) {
            return null;
        }
        return new UserProfileRow(str(row, "USER_NAME"), str(row, "GROUP_PROFILE"), str(row, "STATUS"));
    }

    @Override
    public List<UserProfileListRow> listUserProfiles() {
        List<Map<String, Object>> rows = new JTOpenSqlClient(state).queryList(
                SqlStatementRegistry.of("auth.user.list.paged"));
        List<UserProfileListRow> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(new UserProfileListRow(
                    str(row, "USER_NAME"),
                    str(row, "STATUS"),
                    str(row, "GROUP_PROFILE"),
                    str(row, "TEXT_DESCRIPTION"),
                    str(row, "LAST_USED_DATE")));
        }
        return result;
    }

    @Override
    public CommandResult switchUser(String targetUser) {
        // AS400-010 修复：SWITCHUSR 参数必须经过 identifier 校验，防止 CL 命令注入
        JTOpenConnectionState.requireIdentifier(targetUser, "targetUser");
        try {
            AS400 system = state.connect();
            CommandCall call = new CommandCall(system);
            boolean ok = call.run("SWITCHUSR USER(" + targetUser.trim().toUpperCase() + ")");
            StringBuilder sb = new StringBuilder();
            for (var msg : call.getMessageList()) {
                sb.append(msg.getText()).append('\n');
            }
            return ok ? CommandResult.ok(sb.toString().isBlank() ? "用户态已切换" : sb.toString())
                    : CommandResult.fail(sb.toString().isBlank() ? "用户态切换失败" : sb.toString());
        } catch (Exception e) {
            log.warn("切换用户态失败(host={}, user={}): {}", state.host, targetUser, state.redact(e.getMessage()));
            return CommandResult.fail("切换失败: " + state.redact(e.getMessage()));
        }
    }

}