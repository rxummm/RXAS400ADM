package com.rxas400adm.as400;

import com.rxas400adm.as400.model.UserProfileListRow;
import com.rxas400adm.as400.model.UserProfileRow;

import java.util.List;

/**
 * Mock AuthClient 委托实现（认证仿真 / 用户 profile / 用户列表 / 用户态切换）。
 */
class MockAuthClient implements AuthClient {

    MockAuthClient(MockState state) {
    }

    @Override
    public boolean authenticate(String username, String password) {
        return username != null && !username.isBlank();
    }

    @Override
    public UserProfileRow userProfile(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        String upper = username.trim().toUpperCase();
        if (upper.startsWith("GHOST")) {
            return null;
        }
        String group = switch (upper) {
            case "ADMIN", "QSECOFR" -> "GRPADM";
            case "DEVELOPER" -> "GRPDEV";
            case "OPERATOR" -> "GRPOPR";
            default -> "GRPDEV";
        };
        return new UserProfileRow(upper, group, "*ENABLED");
    }

    @Override
    public List<UserProfileListRow> listUserProfiles() {
        return List.of(
                new UserProfileListRow("QSECOFR", "*ENABLED", "GRPADM", "Security Officer", "2026-08-27"),
                new UserProfileListRow("ADMIN", "*ENABLED", "GRPADM", "Administrator", "2026-08-27"),
                new UserProfileListRow("DEVELOPER", "*ENABLED", "GRPDEV", "Developer", "2026-08-26"),
                new UserProfileListRow("OPERATOR", "*ENABLED", "GRPOPR", "Operator", "2026-08-25"),
                new UserProfileListRow("BATCH01", "*ENABLED", "GRPOPR", "Batch User", "2026-08-24")
        );
    }

    @Override
    public CommandResult switchUser(String targetUser) {
        if (targetUser == null || targetUser.isBlank()) {
            return CommandResult.fail("目标用户不能为空");
        }
        return CommandResult.ok("[Mock] 用户态已切换至: " + targetUser.trim().toUpperCase());
    }
}